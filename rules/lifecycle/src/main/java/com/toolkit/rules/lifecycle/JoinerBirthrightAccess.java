package com.toolkit.rules.lifecycle;

import com.toolkit.mock.MockBundle;
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
 * Assigns birthright roles to newly detected identities based on their
 * department and title attributes.
 *
 * When a new employee is aggregated into IdentityIQ, this rule determines
 * which roles they should receive automatically (without a request or approval).
 * Birthright roles represent the minimum access needed to perform a job function.
 *
 * The mapping from department/title to roles is driven entirely by configuration,
 * making it easy to update access policies without code changes.
 *
 * Configuration keys:
 *   globalBirthrightRoles   - roles assigned to every new identity
 *   departmentRoleMappings  - department name -> list of role names
 *   titleRoleMappings       - title -> list of additional role names
 */
public class JoinerBirthrightAccess {

    private static final String RULE_NAME = "JoinerBirthrightAccess";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public JoinerBirthrightAccess(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    /**
     * Creates an instance using the default config from the classpath.
     */
    public JoinerBirthrightAccess() {
        this(loadDefaultConfig());
    }

    /**
     * Evaluates the identity and builds a provisioning plan assigning
     * appropriate birthright roles.
     *
     * @param context  the SailPoint context (used to verify role existence)
     * @param identity the newly detected identity
     * @return a provisioning plan with role assignments, or an empty plan if none apply
     */
    public MockProvisioningPlan execute(MockSailPointContext context, MockIdentity identity) {
        LoggingUtils.logRuleEntry(LOG, RULE_NAME, identity.getName());

        MockProvisioningPlan plan = new MockProvisioningPlan(identity);
        List<String> rolesToAssign = resolveRoles(identity);

        // Filter out roles the identity already has
        List<String> newRoles = new ArrayList<>();
        for (String roleName : rolesToAssign) {
            if (!identity.hasBundle(roleName)) {
                newRoles.add(roleName);
            } else {
                LoggingUtils.logAction(LOG, RULE_NAME, identity.getName(),
                        "skipRole", roleName + " (already assigned)");
            }
        }

        if (!newRoles.isEmpty()) {
            AccountRequest acctReq = new AccountRequest("IIQ", Operation.Modify);
            acctReq.setNativeIdentity(identity.getName());

            for (String roleName : newRoles) {
                acctReq.add(new AttributeRequest("assignedRoles", Operation.Add, roleName));
                LoggingUtils.logAction(LOG, RULE_NAME, identity.getName(),
                        "assignRole", roleName);
            }

            plan.addAccountRequest(acctReq);
        }

        LoggingUtils.logRuleExit(LOG, RULE_NAME, identity.getName(),
                newRoles.size() + " roles assigned");
        return plan;
    }

    /**
     * Resolves the complete list of birthright roles for an identity based on
     * global config, department, and title.
     */
    List<String> resolveRoles(MockIdentity identity) {
        Set<String> roles = new HashSet<>();

        // Global birthright roles
        List<String> globalRoles = getConfigList("globalBirthrightRoles");
        roles.addAll(globalRoles);

        // Department-based roles
        String department = SafeAttributeUtils.getStringAttribute(identity, "department");
        if (department != null) {
            Map<String, List<String>> deptMappings = getConfigMap("departmentRoleMappings");
            List<String> deptRoles = deptMappings.get(department);
            if (deptRoles != null) {
                roles.addAll(deptRoles);
            }
        }

        // Title-based roles
        String title = SafeAttributeUtils.getStringAttribute(identity, "title");
        if (title != null) {
            Map<String, List<String>> titleMappings = getConfigMap("titleRoleMappings");
            List<String> titleRoles = titleMappings.get(title);
            if (titleRoles != null) {
                roles.addAll(titleRoles);
            }
        }

        return new ArrayList<>(roles);
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

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                JoinerBirthrightAccess.class.getResourceAsStream("/joiner-birthright-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
