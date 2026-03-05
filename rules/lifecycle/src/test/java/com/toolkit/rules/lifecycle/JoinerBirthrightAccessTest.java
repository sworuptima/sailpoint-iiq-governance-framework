package com.toolkit.rules.lifecycle;

import com.toolkit.mock.MockBundle;
import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockProvisioningPlan;
import com.toolkit.mock.MockProvisioningPlan.AccountRequest;
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

class JoinerBirthrightAccessTest {

    private MockSailPointContext context;
    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        context = new MockSailPointContext();
        config = buildTestConfig();

        // Register bundles in context
        for (String role : Arrays.asList("Base Access", "Email", "Company Intranet",
                "Engineering Tools", "GitHub Access", "CRM Access", "Sales Tools",
                "Manager Dashboard", "Approval Authority")) {
            context.addBundle(new MockBundle(role));
        }
    }

    @Test
    void testNewEngineer() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("department", "Engineering")
                .setAttribute("title", "Developer");
        context.addIdentity(identity);

        JoinerBirthrightAccess rule = new JoinerBirthrightAccess(config);
        MockProvisioningPlan plan = rule.execute(context, identity);

        assertFalse(plan.isEmpty());
        List<String> assignedRoles = extractAddedRoles(plan);

        assertTrue(assignedRoles.contains("Base Access"));
        assertTrue(assignedRoles.contains("Email"));
        assertTrue(assignedRoles.contains("Company Intranet"));
        assertTrue(assignedRoles.contains("Engineering Tools"));
        assertTrue(assignedRoles.contains("GitHub Access"));
        assertFalse(assignedRoles.contains("CRM Access"));
    }

    @Test
    void testNewSalesManager() {
        MockIdentity identity = new MockIdentity("asmith")
                .setAttribute("department", "Sales")
                .setAttribute("title", "Manager");
        context.addIdentity(identity);

        JoinerBirthrightAccess rule = new JoinerBirthrightAccess(config);
        MockProvisioningPlan plan = rule.execute(context, identity);

        List<String> assignedRoles = extractAddedRoles(plan);

        // Global
        assertTrue(assignedRoles.contains("Base Access"));
        // Department
        assertTrue(assignedRoles.contains("CRM Access"));
        assertTrue(assignedRoles.contains("Sales Tools"));
        // Title
        assertTrue(assignedRoles.contains("Manager Dashboard"));
        assertTrue(assignedRoles.contains("Approval Authority"));
        // Not engineering
        assertFalse(assignedRoles.contains("Engineering Tools"));
    }

    @Test
    void testUnknownDepartment() {
        MockIdentity identity = new MockIdentity("newbie")
                .setAttribute("department", "Research")
                .setAttribute("title", "Intern");
        context.addIdentity(identity);

        JoinerBirthrightAccess rule = new JoinerBirthrightAccess(config);
        MockProvisioningPlan plan = rule.execute(context, identity);

        List<String> assignedRoles = extractAddedRoles(plan);

        // Only global roles
        assertTrue(assignedRoles.contains("Base Access"));
        assertTrue(assignedRoles.contains("Email"));
        assertTrue(assignedRoles.contains("Company Intranet"));
        assertEquals(3, assignedRoles.size());
    }

    @Test
    void testNullDepartment() {
        MockIdentity identity = new MockIdentity("contractor1");
        context.addIdentity(identity);

        JoinerBirthrightAccess rule = new JoinerBirthrightAccess(config);
        MockProvisioningPlan plan = rule.execute(context, identity);

        List<String> assignedRoles = extractAddedRoles(plan);
        assertEquals(3, assignedRoles.size()); // global only
    }

    @Test
    void testDuplicateRolesNotAdded() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("department", "Engineering")
                .addBundle(new MockBundle("Base Access"))
                .addBundle(new MockBundle("Engineering Tools"));
        context.addIdentity(identity);

        JoinerBirthrightAccess rule = new JoinerBirthrightAccess(config);
        MockProvisioningPlan plan = rule.execute(context, identity);

        List<String> assignedRoles = extractAddedRoles(plan);
        assertFalse(assignedRoles.contains("Base Access"), "Should not re-add existing role");
        assertFalse(assignedRoles.contains("Engineering Tools"), "Should not re-add existing role");
        assertTrue(assignedRoles.contains("Email"));
        assertTrue(assignedRoles.contains("GitHub Access"));
    }

    @Test
    void testPlanStructure() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("department", "Engineering");
        context.addIdentity(identity);

        JoinerBirthrightAccess rule = new JoinerBirthrightAccess(config);
        MockProvisioningPlan plan = rule.execute(context, identity);

        assertEquals(1, plan.getAccountRequests().size());
        AccountRequest acctReq = plan.getAccountRequests().get(0);
        assertEquals("IIQ", acctReq.getApplicationName());
        assertEquals(MockProvisioningPlan.Operation.Modify, acctReq.getOperation());
        assertEquals("jdoe", acctReq.getNativeIdentity());

        for (AttributeRequest attrReq : acctReq.getAttributeRequests()) {
            assertEquals("assignedRoles", attrReq.getName());
            assertEquals(MockProvisioningPlan.Operation.Add, attrReq.getOperation());
        }
    }

    @Test
    void testEmptyConfig() {
        JoinerBirthrightAccess rule = new JoinerBirthrightAccess(Collections.emptyMap());
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("department", "Engineering");

        MockProvisioningPlan plan = rule.execute(context, identity);
        assertTrue(plan.isEmpty(), "Empty config should produce empty plan");
    }

    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        cfg.put("globalBirthrightRoles", Arrays.asList("Base Access", "Email", "Company Intranet"));

        Map<String, List<String>> deptMap = new HashMap<>();
        deptMap.put("Engineering", Arrays.asList("Engineering Tools", "GitHub Access"));
        deptMap.put("Sales", Arrays.asList("CRM Access", "Sales Tools"));
        cfg.put("departmentRoleMappings", deptMap);

        Map<String, List<String>> titleMap = new HashMap<>();
        titleMap.put("Manager", Arrays.asList("Manager Dashboard", "Approval Authority"));
        cfg.put("titleRoleMappings", titleMap);

        return cfg;
    }

    private List<String> extractAddedRoles(MockProvisioningPlan plan) {
        return plan.getAccountRequests().stream()
                .flatMap(ar -> ar.getAttributeRequests().stream())
                .filter(attr -> MockProvisioningPlan.Operation.Add.equals(attr.getOperation()))
                .map(attr -> attr.getValue().toString())
                .collect(Collectors.toList());
    }
}
