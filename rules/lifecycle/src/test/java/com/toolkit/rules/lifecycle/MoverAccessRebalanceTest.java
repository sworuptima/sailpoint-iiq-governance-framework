package com.toolkit.rules.lifecycle;

import com.toolkit.mock.MockBundle;
import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockProvisioningPlan;
import com.toolkit.mock.MockProvisioningPlan.AttributeRequest;
import com.toolkit.mock.MockSailPointContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MoverAccessRebalanceTest {

    private MockSailPointContext context;
    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        context = new MockSailPointContext();
        config = buildTestConfig();
    }

    @Test
    void testEngineeringToSales() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("department", "Sales")
                .addBundle(new MockBundle("Base Access"))
                .addBundle(new MockBundle("Email"))
                .addBundle(new MockBundle("Engineering Tools"))
                .addBundle(new MockBundle("GitHub Access"));
        context.addIdentity(identity);

        MoverAccessRebalance rule = new MoverAccessRebalance(config);
        MoverAccessRebalance.MoverResult result = rule.execute(
                context, identity, "Engineering", "Sales");

        assertTrue(result.getRolesRemoved().contains("Engineering Tools"));
        assertTrue(result.getRolesRemoved().contains("GitHub Access"));
        assertTrue(result.getRolesAdded().contains("CRM Access"));
        assertTrue(result.getRolesAdded().contains("Sales Tools"));

        // Global roles should NOT be removed
        assertFalse(result.getRolesRemoved().contains("Base Access"));
        assertFalse(result.getRolesRemoved().contains("Email"));
    }

    @Test
    void testSameDepartmentNoChanges() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("department", "Engineering")
                .addBundle(new MockBundle("Engineering Tools"))
                .addBundle(new MockBundle("GitHub Access"));
        context.addIdentity(identity);

        MoverAccessRebalance rule = new MoverAccessRebalance(config);
        MoverAccessRebalance.MoverResult result = rule.execute(
                context, identity, "Engineering", "Engineering");

        assertTrue(result.getRolesRemoved().isEmpty());
        assertTrue(result.getRolesAdded().isEmpty());
        assertTrue(result.getPlan().isEmpty());
    }

    @Test
    void testGlobalRolesRetained() {
        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Base Access"))
                .addBundle(new MockBundle("Email"))
                .addBundle(new MockBundle("Company Intranet"))
                .addBundle(new MockBundle("Engineering Tools"));
        context.addIdentity(identity);

        MoverAccessRebalance rule = new MoverAccessRebalance(config);
        MoverAccessRebalance.MoverResult result = rule.execute(
                context, identity, "Engineering", "Sales");

        assertFalse(result.getRolesRemoved().contains("Base Access"));
        assertFalse(result.getRolesRemoved().contains("Email"));
        assertFalse(result.getRolesRemoved().contains("Company Intranet"));
    }

    @Test
    void testCertificationFlaggedForSensitiveRoles() {
        MockIdentity identity = new MockIdentity("fuser")
                .addBundle(new MockBundle("Finance Systems"))
                .addBundle(new MockBundle("Reporting Tools"));
        context.addIdentity(identity);

        // Config with alwaysCertify=false to test sensitive role detection
        Map<String, Object> customConfig = buildTestConfig();
        customConfig.put("alwaysCertifyOnDepartmentChange", false);

        MoverAccessRebalance rule = new MoverAccessRebalance(customConfig);
        MoverAccessRebalance.MoverResult result = rule.execute(
                context, identity, "Finance", "Sales");

        assertTrue(result.isCertificationRequired(),
                "Should flag cert when sensitive role (Finance Systems) is removed");
    }

    @Test
    void testCertificationNotFlaggedForNonSensitiveRoles() {
        MockIdentity identity = new MockIdentity("suser")
                .addBundle(new MockBundle("CRM Access"))
                .addBundle(new MockBundle("Sales Tools"));
        context.addIdentity(identity);

        Map<String, Object> customConfig = buildTestConfig();
        customConfig.put("alwaysCertifyOnDepartmentChange", false);

        MoverAccessRebalance rule = new MoverAccessRebalance(customConfig);
        MoverAccessRebalance.MoverResult result = rule.execute(
                context, identity, "Sales", "Marketing");

        assertFalse(result.isCertificationRequired(),
                "Should not flag cert when no sensitive roles are removed");
    }

    @Test
    void testNullPreviousDepartment() {
        MockIdentity identity = new MockIdentity("newuser");
        context.addIdentity(identity);

        MoverAccessRebalance rule = new MoverAccessRebalance(config);
        MoverAccessRebalance.MoverResult result = rule.execute(
                context, identity, null, "Engineering");

        assertTrue(result.getRolesRemoved().isEmpty());
        assertTrue(result.getRolesAdded().contains("Engineering Tools"));
        assertTrue(result.getRolesAdded().contains("GitHub Access"));
    }

    @Test
    void testRoleOverlap() {
        // Engineering and IT share no roles in our config, but let's test
        // with a config where they share "Monitoring Tools"
        Map<String, Object> overlapConfig = buildTestConfig();
        @SuppressWarnings("unchecked")
        Map<String, List<String>> deptMap =
                (Map<String, List<String>>) overlapConfig.get("departmentRoleMappings");
        deptMap.put("Engineering", Arrays.asList("Engineering Tools", "Monitoring Tools"));
        deptMap.put("IT", Arrays.asList("IT Service Desk", "Monitoring Tools"));

        MockIdentity identity = new MockIdentity("techuser")
                .addBundle(new MockBundle("Engineering Tools"))
                .addBundle(new MockBundle("Monitoring Tools"));
        context.addIdentity(identity);

        MoverAccessRebalance rule = new MoverAccessRebalance(overlapConfig);
        MoverAccessRebalance.MoverResult result = rule.execute(
                context, identity, "Engineering", "IT");

        // Monitoring Tools is in both departments — should NOT be removed
        assertFalse(result.getRolesRemoved().contains("Monitoring Tools"));
        assertTrue(result.getRolesRemoved().contains("Engineering Tools"));
        assertTrue(result.getRolesAdded().contains("IT Service Desk"));
    }

    @Test
    void testMoverResultSummary() {
        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Engineering Tools"))
                .addBundle(new MockBundle("GitHub Access"));
        context.addIdentity(identity);

        MoverAccessRebalance rule = new MoverAccessRebalance(config);
        MoverAccessRebalance.MoverResult result = rule.execute(
                context, identity, "Engineering", "Sales");

        String summary = result.getSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("jdoe"));
        assertTrue(summary.contains("Engineering"));
        assertTrue(summary.contains("Sales"));
    }

    @Test
    void testPlanAttributeRequestOperations() {
        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Engineering Tools"));
        context.addIdentity(identity);

        MoverAccessRebalance rule = new MoverAccessRebalance(config);
        MoverAccessRebalance.MoverResult result = rule.execute(
                context, identity, "Engineering", "Sales");

        MockProvisioningPlan plan = result.getPlan();
        assertFalse(plan.isEmpty());

        List<AttributeRequest> removals = plan.getAccountRequests().stream()
                .flatMap(ar -> ar.getAttributeRequests().stream())
                .filter(attr -> MockProvisioningPlan.Operation.Remove.equals(attr.getOperation()))
                .collect(Collectors.toList());

        List<AttributeRequest> additions = plan.getAccountRequests().stream()
                .flatMap(ar -> ar.getAttributeRequests().stream())
                .filter(attr -> MockProvisioningPlan.Operation.Add.equals(attr.getOperation()))
                .collect(Collectors.toList());

        assertFalse(removals.isEmpty(), "Should have removal requests");
        assertFalse(additions.isEmpty(), "Should have addition requests");
    }

    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        Map<String, List<String>> deptMap = new HashMap<>();
        deptMap.put("Engineering", Arrays.asList("Engineering Tools", "GitHub Access"));
        deptMap.put("Sales", Arrays.asList("CRM Access", "Sales Tools"));
        deptMap.put("Finance", Arrays.asList("Finance Systems", "Reporting Tools"));
        deptMap.put("Marketing", Arrays.asList("Marketing Platform", "Analytics Dashboard"));
        cfg.put("departmentRoleMappings", deptMap);

        cfg.put("sensitiveRoles", Arrays.asList("Finance Systems", "HR Systems", "Budget Access"));
        cfg.put("globalRoles", Arrays.asList("Base Access", "Email", "Company Intranet"));
        cfg.put("alwaysCertifyOnDepartmentChange", true);
        cfg.put("retainGlobalRoles", true);

        return cfg;
    }
}
