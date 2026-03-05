package com.toolkit.rules.workflow;

import com.toolkit.utils.LoggingUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Escalates pending approval work items after configurable timeout periods.
 *
 * Access requests that sit in someone's approval queue for days represent
 * a governance gap. The employee can't work effectively without the access,
 * and the longer the request sits, the more likely it is to get rubber-stamped.
 *
 * This rule implements a tiered escalation chain: after 3 days, escalate to
 * the approver's manager. After 5, to the director. After 7, to the security
 * team. Optionally, auto-approve after 10 days to prevent permanent blocking.
 *
 * Configuration keys:
 *   approvalEscalation.escalationLevels    - ordered list of escalation tiers
 *   approvalEscalation.autoApproveAfterDays - auto-approve threshold (0 = disabled)
 *   approvalEscalation.sendReminders       - whether to send reminder notifications
 *   approvalEscalation.reminderIntervalDays - days between reminders
 */
public class ApprovalEscalation {

    private static final String RULE_NAME = "ApprovalEscalation";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public ApprovalEscalation(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public ApprovalEscalation() {
        this(loadDefaultConfig());
    }

    /**
     * Evaluates a pending approval and determines the appropriate escalation action.
     *
     * @param workItemId      the ID of the pending work item
     * @param currentApprover the current owner of the approval
     * @param daysPending     how many days the approval has been pending
     * @return an EscalationResult with the recommended action
     */
    public EscalationResult execute(String workItemId, String currentApprover, int daysPending) {
        LoggingUtils.logAction(LOG, RULE_NAME, currentApprover, "evaluate",
                "Work item " + workItemId + " pending " + daysPending + " days");

        int autoApproveDays = getAutoApproveAfterDays();
        List<EscalationLevel> levels = getEscalationLevels();
        boolean sendReminders = getSendReminders();
        int reminderInterval = getReminderIntervalDays();

        // Check for auto-approve
        if (autoApproveDays > 0 && daysPending >= autoApproveDays) {
            LoggingUtils.logAction(LOG, RULE_NAME, currentApprover, "autoApprove",
                    "Work item " + workItemId + " auto-approved after " + daysPending + " days");
            return new EscalationResult(workItemId, "AUTO_APPROVE", null,
                    daysPending, "Auto-approved after " + autoApproveDays + " days");
        }

        // Find the appropriate escalation level
        EscalationLevel applicableLevel = null;
        for (int i = levels.size() - 1; i >= 0; i--) {
            if (daysPending >= levels.get(i).getDaysUntilEscalation()) {
                applicableLevel = levels.get(i);
                break;
            }
        }

        if (applicableLevel != null) {
            LoggingUtils.logAction(LOG, RULE_NAME, currentApprover, "escalate",
                    "Escalating to " + applicableLevel.getEscalateTo());
            return new EscalationResult(workItemId, "ESCALATE",
                    applicableLevel.getEscalateTo(), daysPending,
                    "Escalated after " + daysPending + " days to " + applicableLevel.getEscalateTo());
        }

        // Check for reminder
        if (sendReminders && daysPending > 0 && daysPending % reminderInterval == 0) {
            LoggingUtils.logAction(LOG, RULE_NAME, currentApprover, "remind",
                    "Sending reminder for work item " + workItemId);
            return new EscalationResult(workItemId, "REMIND", currentApprover,
                    daysPending, "Reminder sent to " + currentApprover);
        }

        return new EscalationResult(workItemId, "NO_ACTION", currentApprover,
                daysPending, "No action needed");
    }

    @SuppressWarnings("unchecked")
    private List<EscalationLevel> getEscalationLevels() {
        Object esc = config.get("approvalEscalation");
        if (esc instanceof Map) {
            Object levels = ((Map<String, Object>) esc).get("escalationLevels");
            if (levels instanceof List) {
                List<EscalationLevel> result = new ArrayList<>();
                for (Object item : (List<?>) levels) {
                    if (item instanceof Map) {
                        Map<String, Object> levelMap = (Map<String, Object>) item;
                        int days = ((Number) levelMap.get("daysUntilEscalation")).intValue();
                        String target = (String) levelMap.get("escalateTo");
                        result.add(new EscalationLevel(days, target));
                    }
                }
                return result;
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private int getAutoApproveAfterDays() {
        Object esc = config.get("approvalEscalation");
        if (esc instanceof Map) {
            Object val = ((Map<String, Object>) esc).get("autoApproveAfterDays");
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private boolean getSendReminders() {
        Object esc = config.get("approvalEscalation");
        if (esc instanceof Map) {
            Object val = ((Map<String, Object>) esc).get("sendReminders");
            if (val instanceof Boolean) {
                return (Boolean) val;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private int getReminderIntervalDays() {
        Object esc = config.get("approvalEscalation");
        if (esc instanceof Map) {
            Object val = ((Map<String, Object>) esc).get("reminderIntervalDays");
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
        }
        return 1;
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                ApprovalEscalation.class.getResourceAsStream("/workflow-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // --- Supporting classes ---

    static class EscalationLevel {
        private final int daysUntilEscalation;
        private final String escalateTo;

        EscalationLevel(int daysUntilEscalation, String escalateTo) {
            this.daysUntilEscalation = daysUntilEscalation;
            this.escalateTo = escalateTo;
        }

        public int getDaysUntilEscalation() { return daysUntilEscalation; }
        public String getEscalateTo() { return escalateTo; }
    }

    public static class EscalationResult {
        private final String workItemId;
        private final String action;
        private final String targetApprover;
        private final int daysPending;
        private final String summary;

        public EscalationResult(String workItemId, String action,
                                String targetApprover, int daysPending, String summary) {
            this.workItemId = workItemId;
            this.action = action;
            this.targetApprover = targetApprover;
            this.daysPending = daysPending;
            this.summary = summary;
        }

        public String getWorkItemId() { return workItemId; }
        public String getAction() { return action; }
        public String getTargetApprover() { return targetApprover; }
        public int getDaysPending() { return daysPending; }
        public String getSummary() { return summary; }
    }
}
