package com.toolkit.rules.certification;

import com.toolkit.mock.MockBundle;
import com.toolkit.mock.MockIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RiskBasedCertScopingTest {

    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        config = buildTestConfig();
    }

    @Test
    void testHighRiskScoreIncluded() {
        RiskBasedCertScoping rule = new RiskBasedCertScoping(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 85);

        RiskBasedCertScoping.ScopingResult result = rule.execute(identity);

        assertTrue(result.isIncluded());
        assertTrue(result.getReasons().stream()
                .anyMatch(r -> r.contains("Risk score")));
    }

    @Test
    void testLowRiskScoreExcluded() {
        RiskBasedCertScoping rule = new RiskBasedCertScoping(config);

        MockIdentity identity = new MockIdentity("asmith")
                .setAttribute("riskScore", 20);

        RiskBasedCertScoping.ScopingResult result = rule.execute(identity);
        assertFalse(result.isIncluded());
    }

    @Test
    void testHighRiskRoleIncluded() {
        RiskBasedCertScoping rule = new RiskBasedCertScoping(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 10) // Low risk score
                .addBundle(new MockBundle("Database Admin"));

        RiskBasedCertScoping.ScopingResult result = rule.execute(identity);

        assertTrue(result.isIncluded());
        assertEquals(1, result.getHighRiskRolesHeld().size());
        assertTrue(result.getHighRiskRolesHeld().contains("Database Admin"));
    }

    @Test
    void testMultipleHighRiskRoles() {
        RiskBasedCertScoping rule = new RiskBasedCertScoping(config);

        MockIdentity identity = new MockIdentity("admin")
                .addBundle(new MockBundle("Domain Admin"))
                .addBundle(new MockBundle("Security Admin"))
                .addBundle(new MockBundle("Base Access")); // Not high-risk

        RiskBasedCertScoping.ScopingResult result = rule.execute(identity);

        assertTrue(result.isIncluded());
        assertEquals(2, result.getHighRiskRolesHeld().size());
    }

    @Test
    void testPrivilegeAttributeIncluded() {
        RiskBasedCertScoping rule = new RiskBasedCertScoping(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 10)
                .setAttribute("privilegedAccess", "true");

        RiskBasedCertScoping.ScopingResult result = rule.execute(identity);

        assertTrue(result.isIncluded());
        assertTrue(result.getReasons().stream()
                .anyMatch(r -> r.contains("privileged")));
    }

    @Test
    void testNoRiskFactorsExcluded() {
        RiskBasedCertScoping rule = new RiskBasedCertScoping(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 10)
                .addBundle(new MockBundle("Email"))
                .addBundle(new MockBundle("Base Access"));

        RiskBasedCertScoping.ScopingResult result = rule.execute(identity);
        assertFalse(result.isIncluded());
    }

    @Test
    void testExactThresholdIncluded() {
        RiskBasedCertScoping rule = new RiskBasedCertScoping(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 70); // Exactly at threshold

        RiskBasedCertScoping.ScopingResult result = rule.execute(identity);
        assertTrue(result.isIncluded());
    }

    @Test
    void testJustBelowThresholdExcluded() {
        RiskBasedCertScoping rule = new RiskBasedCertScoping(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 69);

        RiskBasedCertScoping.ScopingResult result = rule.execute(identity);
        assertFalse(result.isIncluded());
    }

    @Test
    void testNoRiskScoreAttribute() {
        RiskBasedCertScoping rule = new RiskBasedCertScoping(config);

        MockIdentity identity = new MockIdentity("jdoe");
        // No risk score set — defaults to 0

        RiskBasedCertScoping.ScopingResult result = rule.execute(identity);
        assertFalse(result.isIncluded());
    }

    @Test
    void testEmptyConfig() {
        RiskBasedCertScoping rule = new RiskBasedCertScoping(Collections.emptyMap());
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("riskScore", 85);

        // With default threshold of 70, should still include
        RiskBasedCertScoping.ScopingResult result = rule.execute(identity);
        assertTrue(result.isIncluded());
    }

    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        Map<String, Object> scope = new HashMap<>();
        scope.put("riskAttribute", "riskScore");
        scope.put("highRiskThreshold", 70);
        scope.put("highRiskRoles", Arrays.asList(
                "Domain Admin", "Database Admin", "Payment Approval",
                "Security Admin", "Root Access"));
        scope.put("alwaysIncludeAttributes", Arrays.asList("privilegedAccess"));
        scope.put("alwaysIncludeValue", "true");

        cfg.put("riskBasedScoping", scope);
        return cfg;
    }
}
