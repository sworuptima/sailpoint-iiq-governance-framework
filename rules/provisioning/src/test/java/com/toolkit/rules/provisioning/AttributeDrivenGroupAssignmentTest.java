package com.toolkit.rules.provisioning;

import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockProvisioningPlan;
import com.toolkit.mock.MockProvisioningPlan.AccountRequest;
import com.toolkit.mock.MockProvisioningPlan.AttributeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AttributeDrivenGroupAssignmentTest {

    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        config = buildTestConfig();
    }

    @Test
    void testEngineerInNewYork() {
        AttributeDrivenGroupAssignment rule = new AttributeDrivenGroupAssignment(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("department", "Engineering")
                .setAttribute("location", "New York");

        MockProvisioningPlan plan = rule.execute(identity);
        List<String> groups = extractAddedGroups(plan);

        assertTrue(groups.contains("cn=All-Employees,ou=Groups,dc=company,dc=com"));
        assertTrue(groups.contains("cn=Engineering,ou=Groups,dc=company,dc=com"));
        assertTrue(groups.contains("cn=NYC-Office,ou=Groups,dc=company,dc=com"));
    }

    @Test
    void testSalesInLondon() {
        AttributeDrivenGroupAssignment rule = new AttributeDrivenGroupAssignment(config);

        MockIdentity identity = new MockIdentity("asmith")
                .setAttribute("department", "Sales")
                .setAttribute("location", "London");

        MockProvisioningPlan plan = rule.execute(identity);
        List<String> groups = extractAddedGroups(plan);

        assertTrue(groups.contains("cn=All-Employees,ou=Groups,dc=company,dc=com"));
        assertTrue(groups.contains("cn=Sales,ou=Groups,dc=company,dc=com"));
        assertTrue(groups.contains("cn=CRM-Users,ou=Groups,dc=company,dc=com"));
        assertTrue(groups.contains("cn=LON-Office,ou=Groups,dc=company,dc=com"));
        assertFalse(groups.contains("cn=Engineering,ou=Groups,dc=company,dc=com"));
    }

    @Test
    void testUnknownDepartmentGetsGlobalOnly() {
        AttributeDrivenGroupAssignment rule = new AttributeDrivenGroupAssignment(config);

        MockIdentity identity = new MockIdentity("contractor1")
                .setAttribute("department", "Research");

        MockProvisioningPlan plan = rule.execute(identity);
        List<String> groups = extractAddedGroups(plan);

        assertTrue(groups.contains("cn=All-Employees,ou=Groups,dc=company,dc=com"));
        assertEquals(1, groups.size());
    }

    @Test
    void testNoDepartmentOrLocation() {
        AttributeDrivenGroupAssignment rule = new AttributeDrivenGroupAssignment(config);
        MockIdentity identity = new MockIdentity("temp1");

        MockProvisioningPlan plan = rule.execute(identity);
        List<String> groups = extractAddedGroups(plan);

        // Should still get global groups
        assertTrue(groups.contains("cn=All-Employees,ou=Groups,dc=company,dc=com"));
        assertEquals(1, groups.size());
    }

    @Test
    void testPlanTargetsCorrectApplication() {
        AttributeDrivenGroupAssignment rule = new AttributeDrivenGroupAssignment(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("department", "Engineering");

        MockProvisioningPlan plan = rule.execute(identity);

        assertFalse(plan.isEmpty());
        AccountRequest acctReq = plan.getAccountRequests().get(0);
        assertEquals("Active Directory", acctReq.getApplicationName());
        assertEquals(MockProvisioningPlan.Operation.Modify, acctReq.getOperation());
    }

    @Test
    void testGroupDeduplication() {
        AttributeDrivenGroupAssignment rule = new AttributeDrivenGroupAssignment(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("department", "Engineering")
                .setAttribute("location", "New York");

        Set<String> groups = rule.resolveGroups(identity);

        // All-Employees should appear only once despite being in global groups
        long allEmployeesCount = groups.stream()
                .filter(g -> g.contains("All-Employees"))
                .count();
        assertEquals(1, allEmployeesCount);
    }

    @Test
    void testEmptyConfig() {
        AttributeDrivenGroupAssignment rule = new AttributeDrivenGroupAssignment(Collections.emptyMap());
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("department", "Engineering");

        MockProvisioningPlan plan = rule.execute(identity);
        assertTrue(plan.isEmpty());
    }

    private List<String> extractAddedGroups(MockProvisioningPlan plan) {
        return plan.getAccountRequests().stream()
                .flatMap(ar -> ar.getAttributeRequests().stream())
                .filter(attr -> MockProvisioningPlan.Operation.Add.equals(attr.getOperation()))
                .map(attr -> attr.getValue().toString())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        Map<String, Object> ga = new HashMap<>();

        Map<String, List<String>> deptMap = new HashMap<>();
        deptMap.put("Engineering", Arrays.asList("cn=Engineering,ou=Groups,dc=company,dc=com"));
        deptMap.put("Sales", Arrays.asList("cn=Sales,ou=Groups,dc=company,dc=com", "cn=CRM-Users,ou=Groups,dc=company,dc=com"));
        ga.put("departmentGroupMappings", deptMap);

        Map<String, List<String>> locMap = new HashMap<>();
        locMap.put("New York", Arrays.asList("cn=NYC-Office,ou=Groups,dc=company,dc=com"));
        locMap.put("London", Arrays.asList("cn=LON-Office,ou=Groups,dc=company,dc=com"));
        ga.put("locationGroupMappings", locMap);

        ga.put("globalGroups", Arrays.asList("cn=All-Employees,ou=Groups,dc=company,dc=com"));
        ga.put("targetApplication", "Active Directory");

        cfg.put("groupAssignment", ga);
        return cfg;
    }
}
