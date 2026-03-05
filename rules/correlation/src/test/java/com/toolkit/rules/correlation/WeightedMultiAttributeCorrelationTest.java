package com.toolkit.rules.correlation;

import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockSailPointContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WeightedMultiAttributeCorrelationTest {

    private MockSailPointContext context;
    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        context = new MockSailPointContext();
        config = buildTestConfig();

        // Seed identities
        context.addIdentity(new MockIdentity("jdoe")
                .setAttribute("employeeId", "EMP001")
                .setEmail("john.doe@company.com")
                .setFirstname("John")
                .setLastname("Doe"));

        context.addIdentity(new MockIdentity("asmith")
                .setAttribute("employeeId", "EMP002")
                .setEmail("alice.smith@company.com")
                .setFirstname("Alice")
                .setLastname("Smith"));

        context.addIdentity(new MockIdentity("bjones")
                .setAttribute("employeeId", "EMP003")
                .setEmail("bob.jones@company.com")
                .setFirstname("Bob")
                .setLastname("Jones"));
    }

    @Test
    void testExactEmployeeIdMatch() {
        WeightedMultiAttributeCorrelation rule = new WeightedMultiAttributeCorrelation(config);

        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("employeeId", "EMP001");

        WeightedMultiAttributeCorrelation.CorrelationResult result = rule.execute(context, accountAttrs);

        assertTrue(result.isMatch());
        assertEquals("jdoe", result.getIdentity().getName());
        assertEquals(100, result.getScore());
    }

    @Test
    void testEmailMatch() {
        WeightedMultiAttributeCorrelation rule = new WeightedMultiAttributeCorrelation(config);

        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("email", "alice.smith@company.com");

        WeightedMultiAttributeCorrelation.CorrelationResult result = rule.execute(context, accountAttrs);

        assertTrue(result.isMatch());
        assertEquals("asmith", result.getIdentity().getName());
        assertEquals(80, result.getScore());
    }

    @Test
    void testMultiAttributeScoring() {
        WeightedMultiAttributeCorrelation rule = new WeightedMultiAttributeCorrelation(config);

        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("employeeId", "EMP003");
        accountAttrs.put("email", "bob.jones@company.com");
        accountAttrs.put("lastname", "Jones");

        WeightedMultiAttributeCorrelation.CorrelationResult result = rule.execute(context, accountAttrs);

        assertTrue(result.isMatch());
        assertEquals("bjones", result.getIdentity().getName());
        // 100 (employeeId) + 80 (email) + 30 (lastname) = 210
        assertEquals(210, result.getScore());
        assertEquals(3, result.getScoreBreakdown().size());
    }

    @Test
    void testBelowThreshold() {
        WeightedMultiAttributeCorrelation rule = new WeightedMultiAttributeCorrelation(config);

        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("firstname", "John"); // Only 20 points, below 80 threshold

        WeightedMultiAttributeCorrelation.CorrelationResult result = rule.execute(context, accountAttrs);

        assertFalse(result.isMatch());
        assertNull(result.getIdentity());
    }

    @Test
    void testCaseInsensitiveMatch() {
        WeightedMultiAttributeCorrelation rule = new WeightedMultiAttributeCorrelation(config);

        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("email", "JOHN.DOE@COMPANY.COM");

        WeightedMultiAttributeCorrelation.CorrelationResult result = rule.execute(context, accountAttrs);

        assertTrue(result.isMatch());
        assertEquals("jdoe", result.getIdentity().getName());
    }

    @Test
    void testNoMatchingIdentity() {
        WeightedMultiAttributeCorrelation rule = new WeightedMultiAttributeCorrelation(config);

        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("employeeId", "EMP999");
        accountAttrs.put("email", "unknown@nowhere.com");

        WeightedMultiAttributeCorrelation.CorrelationResult result = rule.execute(context, accountAttrs);

        assertFalse(result.isMatch());
    }

    @Test
    void testNullAttributes() {
        WeightedMultiAttributeCorrelation rule = new WeightedMultiAttributeCorrelation(config);
        WeightedMultiAttributeCorrelation.CorrelationResult result = rule.execute(context, null);
        assertFalse(result.isMatch());
    }

    @Test
    void testEmptyConfig() {
        WeightedMultiAttributeCorrelation rule = new WeightedMultiAttributeCorrelation(Collections.emptyMap());

        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("employeeId", "EMP001");

        WeightedMultiAttributeCorrelation.CorrelationResult result = rule.execute(context, accountAttrs);
        assertFalse(result.isMatch());
    }

    @Test
    void testBestMatchWinsWithMultipleCandidates() {
        WeightedMultiAttributeCorrelation rule = new WeightedMultiAttributeCorrelation(config);

        // Both jdoe and asmith have matching lastnames (different ones),
        // but only jdoe matches on employeeId
        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("employeeId", "EMP001");
        accountAttrs.put("lastname", "Doe");

        WeightedMultiAttributeCorrelation.CorrelationResult result = rule.execute(context, accountAttrs);

        assertTrue(result.isMatch());
        assertEquals("jdoe", result.getIdentity().getName());
        assertEquals(130, result.getScore()); // 100 + 30
    }

    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        Map<String, Object> weighted = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("employeeId", 100);
        attributes.put("email", 80);
        attributes.put("lastname", 30);
        attributes.put("firstname", 20);
        weighted.put("attributes", attributes);
        weighted.put("matchThreshold", 80);
        weighted.put("caseSensitive", false);

        cfg.put("weightedCorrelation", weighted);
        return cfg;
    }
}
