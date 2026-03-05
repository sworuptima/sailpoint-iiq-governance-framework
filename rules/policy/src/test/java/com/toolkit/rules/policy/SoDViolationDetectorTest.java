package com.toolkit.rules.policy;

import com.toolkit.mock.MockBundle;
import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockSailPointContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SoDViolationDetectorTest {

    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        config = buildTestConfig();
    }

    @Test
    void testNoViolation() {
        SoDViolationDetector rule = new SoDViolationDetector(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("CRM Access"))
                .addBundle(new MockBundle("Email"));

        SoDViolationDetector.ViolationReport report = rule.execute(identity);
        assertFalse(report.hasViolations());
        assertEquals(0, report.getViolationCount());
    }

    @Test
    void testSingleViolation() {
        SoDViolationDetector rule = new SoDViolationDetector(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Accounts Payable"))
                .addBundle(new MockBundle("Payment Approval"));

        SoDViolationDetector.ViolationReport report = rule.execute(identity);

        assertTrue(report.hasViolations());
        assertEquals(1, report.getViolationCount());
    }

    @Test
    void testCriticalSeverity() {
        SoDViolationDetector rule = new SoDViolationDetector(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Accounts Payable"))
                .addBundle(new MockBundle("Payment Approval"));

        SoDViolationDetector.ViolationReport report = rule.execute(identity);

        List<SoDViolationDetector.Violation> critical =
                report.getViolationsBySeverity("critical");
        assertEquals(1, critical.size());
    }

    @Test
    void testHighSeverity() {
        SoDViolationDetector rule = new SoDViolationDetector(config);

        MockIdentity identity = new MockIdentity("admin")
                .addBundle(new MockBundle("User Administration"))
                .addBundle(new MockBundle("Security Audit"));

        SoDViolationDetector.ViolationReport report = rule.execute(identity);

        List<SoDViolationDetector.Violation> high =
                report.getViolationsBySeverity("high");
        assertEquals(1, high.size());
    }

    @Test
    void testMultipleViolations() {
        SoDViolationDetector rule = new SoDViolationDetector(config);

        MockIdentity identity = new MockIdentity("risky")
                .addBundle(new MockBundle("Accounts Payable"))
                .addBundle(new MockBundle("Payment Approval"))
                .addBundle(new MockBundle("User Administration"))
                .addBundle(new MockBundle("Security Audit"));

        SoDViolationDetector.ViolationReport report = rule.execute(identity);

        assertTrue(report.hasViolations());
        assertEquals(2, report.getViolationCount());
    }

    @Test
    void testScanAllIdentities() {
        SoDViolationDetector rule = new SoDViolationDetector(config);

        MockSailPointContext context = new MockSailPointContext();

        // Clean user
        context.addIdentity(new MockIdentity("clean")
                .addBundle(new MockBundle("Email")));

        // Violating user
        context.addIdentity(new MockIdentity("risky")
                .addBundle(new MockBundle("Accounts Payable"))
                .addBundle(new MockBundle("Accounts Receivable")));

        Map<String, SoDViolationDetector.ViolationReport> results = rule.scanAll(context);

        assertEquals(1, results.size());
        assertTrue(results.containsKey("risky"));
        assertFalse(results.containsKey("clean"));
    }

    @Test
    void testNoRoles() {
        SoDViolationDetector rule = new SoDViolationDetector(config);

        MockIdentity identity = new MockIdentity("newuser");
        SoDViolationDetector.ViolationReport report = rule.execute(identity);

        assertFalse(report.hasViolations());
    }

    @Test
    void testEmptyConfig() {
        SoDViolationDetector rule = new SoDViolationDetector(Collections.emptyMap());

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Accounts Payable"))
                .addBundle(new MockBundle("Payment Approval"));

        SoDViolationDetector.ViolationReport report = rule.execute(identity);
        assertFalse(report.hasViolations()); // No conflict matrix = no violations
    }

    @Test
    void testViolationReportIdentityName() {
        SoDViolationDetector rule = new SoDViolationDetector(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Database Admin"))
                .addBundle(new MockBundle("Application Developer"));

        SoDViolationDetector.ViolationReport report = rule.execute(identity);
        assertEquals("jdoe", report.getIdentityName());
        assertTrue(report.hasViolations());
    }

    @Test
    void testBidirectionalConflictNotDuplicated() {
        SoDViolationDetector rule = new SoDViolationDetector(config);

        // Both Accounts Payable -> Payment Approval AND
        // Payment Approval -> Accounts Payable exist in config.
        // Should only produce one violation, not two.
        MockIdentity identity = new MockIdentity("jdoe")
                .addBundle(new MockBundle("Accounts Payable"))
                .addBundle(new MockBundle("Payment Approval"));

        SoDViolationDetector.ViolationReport report = rule.execute(identity);
        assertEquals(1, report.getViolationCount());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        Map<String, Object> sod = new HashMap<>();

        Map<String, List<String>> matrix = new HashMap<>();
        matrix.put("Accounts Payable", Arrays.asList("Accounts Receivable", "Payment Approval"));
        matrix.put("Payment Approval", Arrays.asList("Accounts Payable", "Vendor Management"));
        matrix.put("User Administration", Arrays.asList("Security Audit"));
        matrix.put("Database Admin", Arrays.asList("Application Developer"));
        sod.put("conflictMatrix", matrix);

        Map<String, List<List<String>>> severity = new HashMap<>();
        severity.put("critical", Arrays.asList(
                Arrays.asList("Accounts Payable", "Payment Approval")));
        severity.put("high", Arrays.asList(
                Arrays.asList("User Administration", "Security Audit"),
                Arrays.asList("Database Admin", "Application Developer")));
        sod.put("severityLevels", severity);

        cfg.put("sodViolation", sod);
        return cfg;
    }
}
