package com.toolkit.rules.workflow;

import com.toolkit.mock.MockIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DynamicApprovalRoutingTest {

    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        config = buildTestConfig();
    }

    @Test
    void testLowRiskRequestRoutesToManager() {
        DynamicApprovalRouting rule = new DynamicApprovalRouting(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 20);

        DynamicApprovalRouting.RoutingResult result =
                rule.execute(identity, "Email", "Email Access");

        assertEquals("manager", result.getApprover());
        assertEquals(1, result.getApprovalLevels());
    }

    @Test
    void testHighRiskScoreRoutesToDirector() {
        DynamicApprovalRouting rule = new DynamicApprovalRouting(config);

        MockIdentity identity = new MockIdentity("risky")
                .setAttribute("riskScore", 85);

        DynamicApprovalRouting.RoutingResult result =
                rule.execute(identity, "Active Directory", "Domain User");

        assertEquals("high", result.getRiskLevel());
        assertEquals("director", result.getApprover());
        assertEquals(2, result.getApprovalLevels());
    }

    @Test
    void testCriticalRiskRoutesToSecurityTeam() {
        DynamicApprovalRouting rule = new DynamicApprovalRouting(config);

        MockIdentity identity = new MockIdentity("admin")
                .setAttribute("riskScore", 96);

        DynamicApprovalRouting.RoutingResult result =
                rule.execute(identity, "Active Directory", "Domain Admin");

        assertEquals("critical", result.getRiskLevel());
        assertEquals("securityTeam", result.getApprover());
        assertEquals(3, result.getApprovalLevels());
    }

    @Test
    void testApplicationSensitivityOverridesScore() {
        DynamicApprovalRouting rule = new DynamicApprovalRouting(config);

        // Low risk score but financial system is critical
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 10);

        DynamicApprovalRouting.RoutingResult result =
                rule.execute(identity, "Financial Systems", "Finance Access");

        assertEquals("critical", result.getRiskLevel());
        assertEquals("securityTeam", result.getApprover());
        assertEquals(3, result.getApprovalLevels());
    }

    @Test
    void testHigherRiskLevelWins() {
        DynamicApprovalRouting rule = new DynamicApprovalRouting(config);

        // Medium risk score, but HR Systems override to high
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 65);

        DynamicApprovalRouting.RoutingResult result =
                rule.execute(identity, "HR Systems", "HR Admin");

        // Score maps to medium, app maps to high -> high wins
        assertEquals("high", result.getRiskLevel());
        assertEquals("director", result.getApprover());
    }

    @Test
    void testMediumRiskRoutesToManager() {
        DynamicApprovalRouting rule = new DynamicApprovalRouting(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 65);

        DynamicApprovalRouting.RoutingResult result =
                rule.execute(identity, "Email", "Email Access");

        assertEquals("medium", result.getRiskLevel());
        assertEquals("manager", result.getApprover());
        assertEquals(1, result.getApprovalLevels());
    }

    @Test
    void testResultContainsRequestDetails() {
        DynamicApprovalRouting rule = new DynamicApprovalRouting(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 50);

        DynamicApprovalRouting.RoutingResult result =
                rule.execute(identity, "Active Directory", "VPN Access");

        assertEquals("jdoe", result.getIdentityName());
        assertEquals("Active Directory", result.getApplicationName());
        assertEquals("VPN Access", result.getRequestedRole());
    }

    @Test
    void testNoRiskScore() {
        DynamicApprovalRouting rule = new DynamicApprovalRouting(config);
        MockIdentity identity = new MockIdentity("newuser");

        DynamicApprovalRouting.RoutingResult result =
                rule.execute(identity, "Email", "Email Access");

        // Default risk score of 0 maps to default level
        assertNotNull(result.getApprover());
    }

    @Test
    void testDetermineRiskLevels() {
        DynamicApprovalRouting rule = new DynamicApprovalRouting(config);

        assertEquals("medium", rule.determineRiskLevel(0)); // Below low threshold
        assertEquals("low", rule.determineRiskLevel(35));
        assertEquals("medium", rule.determineRiskLevel(65));
        assertEquals("high", rule.determineRiskLevel(85));
        assertEquals("critical", rule.determineRiskLevel(96));
    }

    @Test
    void testEmptyConfigDefaults() {
        DynamicApprovalRouting rule = new DynamicApprovalRouting(Collections.emptyMap());

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 50);

        DynamicApprovalRouting.RoutingResult result =
                rule.execute(identity, "SomeApp", "SomeRole");

        assertEquals("manager", result.getApprover()); // Default approver
        assertEquals(1, result.getApprovalLevels());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        Map<String, Object> routing = new HashMap<>();

        Map<String, Object> thresholds = new HashMap<>();
        thresholds.put("low", 30);
        thresholds.put("medium", 60);
        thresholds.put("high", 80);
        thresholds.put("critical", 95);
        routing.put("riskThresholds", thresholds);

        Map<String, Object> rules = new HashMap<>();
        rules.put("low", createRoutingRule("manager", 1));
        rules.put("medium", createRoutingRule("manager", 1));
        rules.put("high", createRoutingRule("director", 2));
        rules.put("critical", createRoutingRule("securityTeam", 3));
        routing.put("routingRules", rules);

        Map<String, Object> appSensitivity = new HashMap<>();
        appSensitivity.put("Financial Systems", "critical");
        appSensitivity.put("HR Systems", "high");
        appSensitivity.put("Active Directory", "medium");
        appSensitivity.put("Email", "low");
        routing.put("applicationSensitivity", appSensitivity);

        routing.put("defaultRiskLevel", "medium");

        cfg.put("dynamicRouting", routing);
        return cfg;
    }

    private Map<String, Object> createRoutingRule(String approver, int levels) {
        Map<String, Object> rule = new HashMap<>();
        rule.put("approver", approver);
        rule.put("approvalLevels", levels);
        return rule;
    }
}
