package com.toolkit.rules.provisioning;

import com.toolkit.mock.MockBundle;
import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockProvisioningPlan;
import com.toolkit.mock.MockProvisioningPlan.AccountRequest;
import com.toolkit.mock.MockProvisioningPlan.AttributeRequest;
import com.toolkit.mock.MockProvisioningPlan.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BeforeProvisioningSoDCheckTest {

    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        config = buildTestConfig();
    }

    @Test
    void testNoViolation() {
        BeforeProvisioningSoDCheck rule = new BeforeProvisioningSoDCheck(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Accounts Payable"));

        MockProvisioningPlan plan = buildPlanAddingRole(identity, "CRM Access");
        BeforeProvisioningSoDCheck.SoDResult result = rule.execute(identity, plan);

        assertFalse(result.hasViolations());
        assertFalse(result.isBlocked());
    }

    @Test
    void testViolationDetected() {
        BeforeProvisioningSoDCheck rule = new BeforeProvisioningSoDCheck(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Accounts Payable"));

        // Adding Accounts Receivable conflicts with existing Accounts Payable
        MockProvisioningPlan plan = buildPlanAddingRole(identity, "Accounts Receivable");
        BeforeProvisioningSoDCheck.SoDResult result = rule.execute(identity, plan);

        assertTrue(result.hasViolations());
        assertEquals(1, result.getViolations().size());

        BeforeProvisioningSoDCheck.SoDViolation violation = result.getViolations().get(0);
        assertTrue(violation.isIntroducedByPlan());
    }

    @Test
    void testMultipleViolations() {
        BeforeProvisioningSoDCheck rule = new BeforeProvisioningSoDCheck(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Accounts Payable"))
                .addBundle(new MockBundle("User Administration"));

        // Adding both a conflicting role for each existing role
        MockProvisioningPlan plan = new MockProvisioningPlan(identity);
        AccountRequest acctReq = new AccountRequest("IIQ", Operation.Modify);
        acctReq.add(new AttributeRequest("assignedRoles", Operation.Add, "Accounts Receivable"));
        acctReq.add(new AttributeRequest("assignedRoles", Operation.Add, "Security Audit"));
        plan.addAccountRequest(acctReq);

        BeforeProvisioningSoDCheck.SoDResult result = rule.execute(identity, plan);

        assertTrue(result.hasViolations());
        assertEquals(2, result.getViolations().size());
    }

    @Test
    void testBlockOnViolation() {
        Map<String, Object> blockConfig = buildTestConfig();
        @SuppressWarnings("unchecked")
        Map<String, Object> sodConfig = (Map<String, Object>) blockConfig.get("sodCheck");
        sodConfig.put("blockOnViolation", true);

        BeforeProvisioningSoDCheck rule = new BeforeProvisioningSoDCheck(blockConfig);

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Accounts Payable"));

        MockProvisioningPlan plan = buildPlanAddingRole(identity, "Payment Approval");
        BeforeProvisioningSoDCheck.SoDResult result = rule.execute(identity, plan);

        assertTrue(result.hasViolations());
        assertTrue(result.isBlocked());
        assertTrue(result.getSummary().contains("BLOCKED"));
    }

    @Test
    void testFlagWithoutBlocking() {
        BeforeProvisioningSoDCheck rule = new BeforeProvisioningSoDCheck(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Accounts Payable"));

        MockProvisioningPlan plan = buildPlanAddingRole(identity, "Payment Approval");
        BeforeProvisioningSoDCheck.SoDResult result = rule.execute(identity, plan);

        assertTrue(result.hasViolations());
        assertFalse(result.isBlocked());
        assertTrue(result.getSummary().contains("FLAGGED"));
    }

    @Test
    void testExemptRolesSkipped() {
        BeforeProvisioningSoDCheck rule = new BeforeProvisioningSoDCheck(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Emergency Access"));

        // Emergency Access is exempt, so even if it were in the conflict matrix,
        // it wouldn't trigger a violation
        MockProvisioningPlan plan = buildPlanAddingRole(identity, "Accounts Payable");
        BeforeProvisioningSoDCheck.SoDResult result = rule.execute(identity, plan);

        assertFalse(result.hasViolations());
    }

    @Test
    void testEmptyPlan() {
        BeforeProvisioningSoDCheck rule = new BeforeProvisioningSoDCheck(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Accounts Payable"));

        MockProvisioningPlan plan = new MockProvisioningPlan(identity);
        BeforeProvisioningSoDCheck.SoDResult result = rule.execute(identity, plan);

        assertFalse(result.hasViolations());
    }

    @Test
    void testNoConflictingRoles() {
        BeforeProvisioningSoDCheck rule = new BeforeProvisioningSoDCheck(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("CRM Access"))
                .addBundle(new MockBundle("Email"));

        MockProvisioningPlan plan = buildPlanAddingRole(identity, "Base Access");
        BeforeProvisioningSoDCheck.SoDResult result = rule.execute(identity, plan);

        assertFalse(result.hasViolations());
    }

    @Test
    void testExistingViolationNotIntroducedByPlan() {
        BeforeProvisioningSoDCheck rule = new BeforeProvisioningSoDCheck(config);

        // Identity already has conflicting roles
        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Accounts Payable"))
                .addBundle(new MockBundle("Accounts Receivable"));

        // Plan adds an unrelated role
        MockProvisioningPlan plan = buildPlanAddingRole(identity, "CRM Access");
        BeforeProvisioningSoDCheck.SoDResult result = rule.execute(identity, plan);

        assertTrue(result.hasViolations());
        assertFalse(result.getViolations().get(0).isIntroducedByPlan());
    }

    private MockProvisioningPlan buildPlanAddingRole(MockIdentity identity, String roleName) {
        MockProvisioningPlan plan = new MockProvisioningPlan(identity);
        AccountRequest acctReq = new AccountRequest("IIQ", Operation.Modify);
        acctReq.add(new AttributeRequest("assignedRoles", Operation.Add, roleName));
        plan.addAccountRequest(acctReq);
        return plan;
    }

    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        Map<String, Object> sod = new HashMap<>();

        Map<String, List<String>> matrix = new HashMap<>();
        matrix.put("Accounts Payable", Arrays.asList("Accounts Receivable", "Payment Approval"));
        matrix.put("Payment Approval", Arrays.asList("Accounts Payable", "Vendor Management"));
        matrix.put("User Administration", Arrays.asList("Security Audit"));
        matrix.put("Database Admin", Arrays.asList("Application Developer"));
        sod.put("conflictMatrix", matrix);

        sod.put("blockOnViolation", false);
        sod.put("exemptRoles", Arrays.asList("Emergency Access"));

        cfg.put("sodCheck", sod);
        return cfg;
    }
}
