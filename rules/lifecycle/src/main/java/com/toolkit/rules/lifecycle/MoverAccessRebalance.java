package com.toolkit.rules.lifecycle;

import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockProvisioningPlan;
import com.toolkit.mock.MockProvisioningPlan.AccountRequest;
import com.toolkit.mock.MockProvisioningPlan.AttributeRequest;
import com.toolkit.mock.MockProvisioningPlan.Operation;
import com.toolkit.mock.MockSailPointContext;
import com.toolkit.utils.LoggingUtils;
import com.toolkit.utils.SafeAttributeUtils;
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
 * Detects department changes and rebalances role assignments accordingly.
 *
 * When an employee transfers between departments, they often retain access
 * from their previous role — a pattern known as "entitlement creep." This rule
 * compares the old and new department role mappings, removes roles that are
 * specific to the old department, adds roles for the new department, and
 * flags the identity for certification review if sensitive roles are involved.
 *
 * Configuration keys:
 *   departmentRoleMappings           - department name -> list of role names
 *   globalRoles                      - roles that are never removed during rebalance
 *   sensitiveRoles                   - roles that trigger mandatory certification
 *   alwaysCertifyOnDepartmentChange  - if true, every department change triggers cert
 *   retainGlobalRoles                - if true, global roles survive rebalance
 *   gracePeriodDays                  - days before removed access is actually revoked
 */
public class MoverAccessRebalance {

    private static final String RULE_NAME = "MoverAccessRebalance";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public MoverAccessRebalance(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public MoverAccessRebalance() {
        this(loadDefaultConfig());
    }

    /**
     * Evaluates a department change and builds a provisioning plan that
     * removes old department roles and adds new ones.
     *
     * @param context            the SailPoint context
     * @param identity           the identity that moved
     * @param previousDepartment the department before the move (may be null for new assignments)
     * @param newDepartment      the department after the move
     * @return a MoverResult containing the plan, certification flag, and summary
     */
    public MoverResult execute(MockSailPointContext context, MockIdentity identity,
                               String previousDepartment, String newDepartment) {
        LoggingUtils.logRuleEntry(LOG, RULE_NAME, identity.getName());

        Map<String, List<String>> deptMappings = getConfigMap("departmentRoleMappings");
        List<String> globalRoles = getConfigList("globalRoles");
        List<String> sensitiveRoles = getConfigList("sensitiveRoles");
        boolean alwaysCertify = getConfigBoolean("alwaysCertifyOnDepartmentChange", true);
        boolean retainGlobal = getConfigBoolean("retainGlobalRoles", true);

        Set<String> oldRoles = new HashSet<>(
                deptMappings.getOrDefault(previousDepartment, Collections.emptyList()));
        Set<String> newRoles = new HashSet<>(
                deptMappings.getOrDefault(newDepartment, Collections.emptyList()));

        // Roles to remove: in old set but not in new set
        Set<String> toRemove = new HashSet<>(oldRoles);
        toRemove.removeAll(newRoles);

        // If retaining global roles, never remove them
        if (retainGlobal) {
            toRemove.removeAll(globalRoles);
        }

        // Only remove roles the identity actually has
        List<String> actualRemovals = new ArrayList<>();
        for (String role : toRemove) {
            if (identity.hasBundle(role)) {
                actualRemovals.add(role);
            }
        }

        // Roles to add: in new set but not already assigned
        List<String> actualAdditions = new ArrayList<>();
        for (String role : newRoles) {
            if (!identity.hasBundle(role)) {
                actualAdditions.add(role);
            }
        }

        // Build provisioning plan
        MockProvisioningPlan plan = new MockProvisioningPlan(identity);

        if (!actualRemovals.isEmpty() || !actualAdditions.isEmpty()) {
            AccountRequest acctReq = new AccountRequest("IIQ", Operation.Modify);
            acctReq.setNativeIdentity(identity.getName());

            for (String role : actualRemovals) {
                acctReq.add(new AttributeRequest("assignedRoles", Operation.Remove, role));
                LoggingUtils.logAction(LOG, RULE_NAME, identity.getName(),
                        "removeRole", role + " (old department: " + previousDepartment + ")");
            }

            for (String role : actualAdditions) {
                acctReq.add(new AttributeRequest("assignedRoles", Operation.Add, role));
                LoggingUtils.logAction(LOG, RULE_NAME, identity.getName(),
                        "addRole", role + " (new department: " + newDepartment + ")");
            }

            plan.addAccountRequest(acctReq);
        }

        // Determine certification requirement
        boolean certRequired = alwaysCertify;
        if (!certRequired) {
            certRequired = requiresCertification(actualRemovals, sensitiveRoles);
        }

        if (certRequired) {
            LoggingUtils.logAction(LOG, RULE_NAME, identity.getName(),
                    "flagCertification", "department change from " +
                            previousDepartment + " to " + newDepartment);
        }

        // Build summary
        String summary = buildSummary(identity.getName(), previousDepartment, newDepartment,
                actualRemovals, actualAdditions, certRequired);

        LoggingUtils.logRuleExit(LOG, RULE_NAME, identity.getName(), summary);

        return new MoverResult(plan, certRequired, actualAdditions, actualRemovals, summary);
    }

    /**
     * Determines whether a certification campaign should be triggered based on
     * whether any sensitive roles are being removed.
     */
    boolean requiresCertification(List<String> removedRoles, List<String> sensitiveRoles) {
        for (String role : removedRoles) {
            if (sensitiveRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    private String buildSummary(String identityName, String prevDept, String newDept,
                                List<String> removed, List<String> added, boolean certRequired) {
        return String.format("%s moved %s -> %s: +%d roles, -%d roles, cert=%s",
                identityName, prevDept, newDept, added.size(), removed.size(), certRequired);
    }

    @SuppressWarnings("unchecked")
    private List<String> getConfigList(String key) {
        Object value = config.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> getConfigMap(String key) {
        Object value = config.get(key);
        if (value instanceof Map) {
            return (Map<String, List<String>>) value;
        }
        return Collections.emptyMap();
    }

    private boolean getConfigBoolean(String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                MoverAccessRebalance.class.getResourceAsStream("/mover-rebalance-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // --- Result container ---

    public static class MoverResult {
        private final MockProvisioningPlan plan;
        private final boolean certificationRequired;
        private final List<String> rolesAdded;
        private final List<String> rolesRemoved;
        private final String summary;

        public MoverResult(MockProvisioningPlan plan, boolean certificationRequired,
                           List<String> rolesAdded, List<String> rolesRemoved, String summary) {
            this.plan = plan;
            this.certificationRequired = certificationRequired;
            this.rolesAdded = rolesAdded;
            this.rolesRemoved = rolesRemoved;
            this.summary = summary;
        }

        public MockProvisioningPlan getPlan() { return plan; }
        public boolean isCertificationRequired() { return certificationRequired; }
        public List<String> getRolesAdded() { return rolesAdded; }
        public List<String> getRolesRemoved() { return rolesRemoved; }
        public String getSummary() { return summary; }
    }
}
