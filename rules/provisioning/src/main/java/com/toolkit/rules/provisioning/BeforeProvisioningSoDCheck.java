package com.toolkit.rules.provisioning;

import com.toolkit.mock.MockBundle;
import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockProvisioningPlan;
import com.toolkit.mock.MockProvisioningPlan.AccountRequest;
import com.toolkit.mock.MockProvisioningPlan.AttributeRequest;
import com.toolkit.mock.MockProvisioningPlan.Operation;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates Separation of Duties compliance before provisioning executes.
 *
 * SoD is a fundamental security control. The person who creates purchase
 * orders must not also approve them. The person who administers user accounts
 * must not also perform security audits. These conflicting entitlements, if
 * held by one person, create fraud risk.
 *
 * This rule evaluates a provisioning plan against a configurable conflict
 * matrix. It collects the identity's current roles, adds the roles being
 * provisioned, and checks every pair against the matrix. If a conflict is
 * found, the rule can either block the provisioning or flag it for review.
 *
 * Configuration keys:
 *   sodCheck.conflictMatrix     - role name -> list of conflicting role names
 *   sodCheck.blockOnViolation   - if true, reject the plan; if false, flag only
 *   sodCheck.exemptRoles        - roles that bypass SoD checks (e.g., emergency access)
 */
public class BeforeProvisioningSoDCheck {

    private static final String RULE_NAME = "BeforeProvisioningSoDCheck";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public BeforeProvisioningSoDCheck(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public BeforeProvisioningSoDCheck() {
        this(loadDefaultConfig());
    }

    /**
     * Checks a provisioning plan for SoD violations.
     *
     * @param identity the identity the plan applies to
     * @param plan     the provisioning plan to validate
     * @return a SoDResult indicating whether violations were found
     */
    public SoDResult execute(MockIdentity identity, MockProvisioningPlan plan) {
        LoggingUtils.logRuleEntry(LOG, RULE_NAME, identity.getName());

        Map<String, List<String>> conflictMatrix = getConflictMatrix();
        boolean blockOnViolation = getBlockOnViolation();
        List<String> exemptRoles = getExemptRoles();

        // Collect current roles
        Set<String> currentRoles = new HashSet<>();
        for (MockBundle bundle : identity.getBundles()) {
            currentRoles.add(bundle.getName());
        }

        // Collect roles being added by the plan
        Set<String> rolesBeingAdded = new HashSet<>();
        for (AccountRequest acctReq : plan.getAccountRequests()) {
            for (AttributeRequest attrReq : acctReq.getAttributeRequests()) {
                if ("assignedRoles".equals(attrReq.getName())
                        && Operation.Add.equals(attrReq.getOperation())
                        && attrReq.getValue() != null) {
                    rolesBeingAdded.add(attrReq.getValue().toString());
                }
            }
        }

        // Combined role set after provisioning
        Set<String> allRoles = new HashSet<>(currentRoles);
        allRoles.addAll(rolesBeingAdded);

        // Remove exempt roles from checking
        Set<String> checkableRoles = new HashSet<>(allRoles);
        checkableRoles.removeAll(exemptRoles);

        // Check for conflicts
        List<SoDViolation> violations = new ArrayList<>();
        List<String> roleList = new ArrayList<>(checkableRoles);

        Set<String> checkedPairs = new HashSet<>();

        for (int i = 0; i < roleList.size(); i++) {
            String roleA = roleList.get(i);

            for (int j = i + 1; j < roleList.size(); j++) {
                String roleB = roleList.get(j);

                String pairKey = roleA.compareTo(roleB) < 0
                        ? roleA + "|" + roleB : roleB + "|" + roleA;
                if (checkedPairs.contains(pairKey)) {
                    continue;
                }

                // Check both directions in the conflict matrix
                List<String> conflictsA = conflictMatrix.get(roleA);
                List<String> conflictsB = conflictMatrix.get(roleB);
                boolean isConflict = (conflictsA != null && conflictsA.contains(roleB))
                        || (conflictsB != null && conflictsB.contains(roleA));

                if (isConflict) {
                    checkedPairs.add(pairKey);
                    boolean isNewConflict = rolesBeingAdded.contains(roleA) || rolesBeingAdded.contains(roleB);
                    violations.add(new SoDViolation(roleA, roleB, isNewConflict));
                    LoggingUtils.logWarning(LOG, RULE_NAME, identity.getName(),
                            "sodViolation", roleA + " conflicts with " + roleB);
                }
            }
        }

        boolean blocked = blockOnViolation && !violations.isEmpty();
        String summary = buildSummary(identity.getName(), violations, blocked);

        LoggingUtils.logRuleExit(LOG, RULE_NAME, identity.getName(), summary);
        return new SoDResult(violations, blocked, summary);
    }

    private String buildSummary(String identityName, List<SoDViolation> violations, boolean blocked) {
        if (violations.isEmpty()) {
            return identityName + ": no SoD violations detected";
        }
        return String.format("%s: %d SoD violation(s) found, action=%s",
                identityName, violations.size(), blocked ? "BLOCKED" : "FLAGGED");
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> getConflictMatrix() {
        Object sod = config.get("sodCheck");
        if (sod instanceof Map) {
            Object matrix = ((Map<String, Object>) sod).get("conflictMatrix");
            if (matrix instanceof Map) {
                return (Map<String, List<String>>) matrix;
            }
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private boolean getBlockOnViolation() {
        Object sod = config.get("sodCheck");
        if (sod instanceof Map) {
            Object block = ((Map<String, Object>) sod).get("blockOnViolation");
            if (block instanceof Boolean) {
                return (Boolean) block;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<String> getExemptRoles() {
        Object sod = config.get("sodCheck");
        if (sod instanceof Map) {
            Object exempt = ((Map<String, Object>) sod).get("exemptRoles");
            if (exempt instanceof List) {
                return (List<String>) exempt;
            }
        }
        return Collections.emptyList();
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                BeforeProvisioningSoDCheck.class.getResourceAsStream("/provisioning-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // --- Result containers ---

    public static class SoDViolation {
        private final String roleA;
        private final String roleB;
        private final boolean introducedByPlan;

        public SoDViolation(String roleA, String roleB, boolean introducedByPlan) {
            this.roleA = roleA;
            this.roleB = roleB;
            this.introducedByPlan = introducedByPlan;
        }

        public String getRoleA() { return roleA; }
        public String getRoleB() { return roleB; }
        public boolean isIntroducedByPlan() { return introducedByPlan; }

        @Override
        public String toString() {
            return roleA + " <-> " + roleB + (introducedByPlan ? " (new)" : " (existing)");
        }
    }

    public static class SoDResult {
        private final List<SoDViolation> violations;
        private final boolean blocked;
        private final String summary;

        public SoDResult(List<SoDViolation> violations, boolean blocked, String summary) {
            this.violations = violations;
            this.blocked = blocked;
            this.summary = summary;
        }

        public List<SoDViolation> getViolations() { return violations; }
        public boolean isBlocked() { return blocked; }
        public boolean hasViolations() { return !violations.isEmpty(); }
        public String getSummary() { return summary; }
    }
}
