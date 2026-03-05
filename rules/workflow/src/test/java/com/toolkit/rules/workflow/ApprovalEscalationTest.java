package com.toolkit.rules.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApprovalEscalationTest {

    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        config = buildTestConfig();
    }

    @Test
    void testNoActionForNewRequest() {
        ApprovalEscalation rule = new ApprovalEscalation(config);

        ApprovalEscalation.EscalationResult result =
                rule.execute("WI-001", "jdoe", 0);

        assertEquals("NO_ACTION", result.getAction());
        assertEquals("jdoe", result.getTargetApprover());
    }

    @Test
    void testReminderOnDay1() {
        ApprovalEscalation rule = new ApprovalEscalation(config);

        ApprovalEscalation.EscalationResult result =
                rule.execute("WI-001", "jdoe", 1);

        assertEquals("REMIND", result.getAction());
        assertEquals("jdoe", result.getTargetApprover());
    }

    @Test
    void testEscalateToManagerAfter3Days() {
        ApprovalEscalation rule = new ApprovalEscalation(config);

        ApprovalEscalation.EscalationResult result =
                rule.execute("WI-001", "jdoe", 3);

        assertEquals("ESCALATE", result.getAction());
        assertEquals("manager", result.getTargetApprover());
    }

    @Test
    void testEscalateToDirectorAfter5Days() {
        ApprovalEscalation rule = new ApprovalEscalation(config);

        ApprovalEscalation.EscalationResult result =
                rule.execute("WI-001", "jdoe", 5);

        assertEquals("ESCALATE", result.getAction());
        assertEquals("director", result.getTargetApprover());
    }

    @Test
    void testEscalateToSecurityAfter7Days() {
        ApprovalEscalation rule = new ApprovalEscalation(config);

        ApprovalEscalation.EscalationResult result =
                rule.execute("WI-001", "jdoe", 7);

        assertEquals("ESCALATE", result.getAction());
        assertEquals("securityTeam", result.getTargetApprover());
    }

    @Test
    void testAutoApproveAfter10Days() {
        ApprovalEscalation rule = new ApprovalEscalation(config);

        ApprovalEscalation.EscalationResult result =
                rule.execute("WI-001", "jdoe", 10);

        assertEquals("AUTO_APPROVE", result.getAction());
        assertTrue(result.getSummary().contains("Auto-approved"));
    }

    @Test
    void testAutoApproveAfterMoreThan10Days() {
        ApprovalEscalation rule = new ApprovalEscalation(config);

        ApprovalEscalation.EscalationResult result =
                rule.execute("WI-001", "jdoe", 15);

        assertEquals("AUTO_APPROVE", result.getAction());
    }

    @Test
    void testDay4EscalatesToManager() {
        ApprovalEscalation rule = new ApprovalEscalation(config);

        // Day 4 is between manager (3) and director (5) levels
        ApprovalEscalation.EscalationResult result =
                rule.execute("WI-001", "jdoe", 4);

        assertEquals("ESCALATE", result.getAction());
        assertEquals("manager", result.getTargetApprover());
    }

    @Test
    void testWorkItemIdPreserved() {
        ApprovalEscalation rule = new ApprovalEscalation(config);

        ApprovalEscalation.EscalationResult result =
                rule.execute("WI-12345", "jdoe", 3);

        assertEquals("WI-12345", result.getWorkItemId());
    }

    @Test
    void testDaysPendingPreserved() {
        ApprovalEscalation rule = new ApprovalEscalation(config);

        ApprovalEscalation.EscalationResult result =
                rule.execute("WI-001", "jdoe", 6);

        assertEquals(6, result.getDaysPending());
    }

    @Test
    void testEmptyConfigNoAction() {
        ApprovalEscalation rule = new ApprovalEscalation(Collections.emptyMap());

        ApprovalEscalation.EscalationResult result =
                rule.execute("WI-001", "jdoe", 5);

        // No escalation levels, no auto-approve = reminder or no action
        assertNotEquals("ESCALATE", result.getAction());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        Map<String, Object> esc = new HashMap<>();

        List<Map<String, Object>> levels = Arrays.asList(
                createLevel(3, "manager"),
                createLevel(5, "director"),
                createLevel(7, "securityTeam")
        );
        esc.put("escalationLevels", levels);
        esc.put("autoApproveAfterDays", 10);
        esc.put("sendReminders", true);
        esc.put("reminderIntervalDays", 1);

        cfg.put("approvalEscalation", esc);
        return cfg;
    }

    private Map<String, Object> createLevel(int days, String target) {
        Map<String, Object> level = new HashMap<>();
        level.put("daysUntilEscalation", days);
        level.put("escalateTo", target);
        return level;
    }
}
