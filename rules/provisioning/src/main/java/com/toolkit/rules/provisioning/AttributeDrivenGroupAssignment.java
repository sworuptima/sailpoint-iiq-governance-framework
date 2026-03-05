package com.toolkit.rules.provisioning;

import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockProvisioningPlan;
import com.toolkit.mock.MockProvisioningPlan.AccountRequest;
import com.toolkit.mock.MockProvisioningPlan.AttributeRequest;
import com.toolkit.mock.MockProvisioningPlan.Operation;
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
 * Computes target system group memberships based on identity attributes.
 *
 * In most environments, Active Directory group membership mirrors the
 * organizational structure: engineers go in the Engineering group, sales
 * reps in the CRM-Users group, NYC employees in the NYC-Office group.
 * Doing this by hand per user is error-prone and doesn't scale.
 *
 * This FieldValue rule reads the identity's department, location, and other
 * attributes, resolves the corresponding groups from configuration, and
 * builds the provisioning plan to add those group memberships on the target
 * application.
 *
 * Configuration keys:
 *   groupAssignment.departmentGroupMappings - department -> list of group DNs
 *   groupAssignment.locationGroupMappings   - location -> list of group DNs
 *   groupAssignment.globalGroups            - groups assigned to every identity
 *   groupAssignment.targetApplication       - the application name for the account request
 */
public class AttributeDrivenGroupAssignment {

    private static final String RULE_NAME = "AttributeDrivenGroupAssignment";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public AttributeDrivenGroupAssignment(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public AttributeDrivenGroupAssignment() {
        this(loadDefaultConfig());
    }

    /**
     * Resolves group memberships for an identity and builds a provisioning plan.
     *
     * @param identity the identity to compute groups for
     * @return a provisioning plan with group Add operations
     */
    public MockProvisioningPlan execute(MockIdentity identity) {
        LoggingUtils.logRuleEntry(LOG, RULE_NAME, identity.getName());

        Set<String> groups = resolveGroups(identity);
        MockProvisioningPlan plan = new MockProvisioningPlan(identity);

        if (!groups.isEmpty()) {
            String targetApp = getTargetApplication();
            AccountRequest acctReq = new AccountRequest(targetApp, Operation.Modify);
            acctReq.setNativeIdentity(identity.getName());

            for (String group : groups) {
                acctReq.add(new AttributeRequest("memberOf", Operation.Add, group));
                LoggingUtils.logAction(LOG, RULE_NAME, identity.getName(),
                        "addGroup", group);
            }

            plan.addAccountRequest(acctReq);
        }

        LoggingUtils.logRuleExit(LOG, RULE_NAME, identity.getName(),
                groups.size() + " groups computed");
        return plan;
    }

    /**
     * Resolves all groups for an identity based on department, location,
     * and global configuration.
     */
    Set<String> resolveGroups(MockIdentity identity) {
        Set<String> groups = new HashSet<>();

        // Global groups
        groups.addAll(getGroupList("globalGroups"));

        // Department-based groups
        String department = SafeAttributeUtils.getStringAttribute(identity, "department");
        if (department != null) {
            Map<String, List<String>> deptMappings = getGroupMappings("departmentGroupMappings");
            List<String> deptGroups = deptMappings.get(department);
            if (deptGroups != null) {
                groups.addAll(deptGroups);
            }
        }

        // Location-based groups
        String location = SafeAttributeUtils.getStringAttribute(identity, "location");
        if (location != null) {
            Map<String, List<String>> locMappings = getGroupMappings("locationGroupMappings");
            List<String> locGroups = locMappings.get(location);
            if (locGroups != null) {
                groups.addAll(locGroups);
            }
        }

        return groups;
    }

    @SuppressWarnings("unchecked")
    private List<String> getGroupList(String key) {
        Object ga = config.get("groupAssignment");
        if (ga instanceof Map) {
            Object val = ((Map<String, Object>) ga).get(key);
            if (val instanceof List) {
                return (List<String>) val;
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> getGroupMappings(String key) {
        Object ga = config.get("groupAssignment");
        if (ga instanceof Map) {
            Object val = ((Map<String, Object>) ga).get(key);
            if (val instanceof Map) {
                return (Map<String, List<String>>) val;
            }
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private String getTargetApplication() {
        Object ga = config.get("groupAssignment");
        if (ga instanceof Map) {
            Object val = ((Map<String, Object>) ga).get("targetApplication");
            if (val instanceof String) {
                return (String) val;
            }
        }
        return "Active Directory";
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                AttributeDrivenGroupAssignment.class.getResourceAsStream("/provisioning-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
