package com.toolkit.rules.correlation;

import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockSailPointContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FuzzyMatchCorrelationTest {

    private MockSailPointContext context;
    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        context = new MockSailPointContext();
        config = buildTestConfig();

        context.addIdentity(new MockIdentity("jdoe")
                .setAttribute("employeeId", "EMP001")
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john.doe@company.com"));

        context.addIdentity(new MockIdentity("asmith")
                .setAttribute("employeeId", "EMP002")
                .setFirstname("Alice")
                .setLastname("Smith")
                .setEmail("alice.smith@company.com"));
    }

    @Test
    void testExactMatchOnEmployeeId() {
        FuzzyMatchCorrelation rule = new FuzzyMatchCorrelation(config);

        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("employeeId", "EMP001");

        FuzzyMatchCorrelation.FuzzyResult result = rule.execute(context, accountAttrs);

        assertTrue(result.isMatch());
        assertEquals("jdoe", result.getIdentity().getName());
        assertEquals(1.0, result.getScore(), 0.001);
    }

    @Test
    void testFuzzyMatchOnFirstname() {
        FuzzyMatchCorrelation rule = new FuzzyMatchCorrelation(config);

        // "Jon" is one edit away from "John"
        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("firstname", "Jon");
        accountAttrs.put("lastname", "Doe");

        FuzzyMatchCorrelation.FuzzyResult result = rule.execute(context, accountAttrs);

        assertTrue(result.isMatch());
        assertEquals("jdoe", result.getIdentity().getName());
    }

    @Test
    void testFuzzyMatchWithTypo() {
        FuzzyMatchCorrelation rule = new FuzzyMatchCorrelation(config);

        // "Smit" is one edit away from "Smith"
        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("firstname", "Alice");
        accountAttrs.put("lastname", "Smit");

        FuzzyMatchCorrelation.FuzzyResult result = rule.execute(context, accountAttrs);

        assertTrue(result.isMatch());
        assertEquals("asmith", result.getIdentity().getName());
    }

    @Test
    void testNoMatchBelowThreshold() {
        FuzzyMatchCorrelation rule = new FuzzyMatchCorrelation(config);

        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("firstname", "Xavier");
        accountAttrs.put("lastname", "Williams");

        FuzzyMatchCorrelation.FuzzyResult result = rule.execute(context, accountAttrs);

        assertFalse(result.isMatch());
    }

    @Test
    void testLevenshteinDistanceIdentical() {
        assertEquals(0, FuzzyMatchCorrelation.levenshteinDistance("hello", "hello"));
    }

    @Test
    void testLevenshteinDistanceOneEdit() {
        assertEquals(1, FuzzyMatchCorrelation.levenshteinDistance("cat", "bat"));
        assertEquals(1, FuzzyMatchCorrelation.levenshteinDistance("cat", "cats"));
        assertEquals(1, FuzzyMatchCorrelation.levenshteinDistance("cat", "at"));
    }

    @Test
    void testLevenshteinDistanceMultipleEdits() {
        assertEquals(3, FuzzyMatchCorrelation.levenshteinDistance("kitten", "sitting"));
    }

    @Test
    void testSimilarityScore() {
        assertEquals(1.0, FuzzyMatchCorrelation.computeSimilarity("John", "John"), 0.001);
        assertTrue(FuzzyMatchCorrelation.computeSimilarity("John", "Jon") > 0.7);
        assertTrue(FuzzyMatchCorrelation.computeSimilarity("Smith", "Smyth") > 0.6);
    }

    @Test
    void testNullInputs() {
        FuzzyMatchCorrelation rule = new FuzzyMatchCorrelation(config);
        FuzzyMatchCorrelation.FuzzyResult result = rule.execute(context, null);
        assertFalse(result.isMatch());
    }

    @Test
    void testEmptyContext() {
        MockSailPointContext emptyCtx = new MockSailPointContext();
        FuzzyMatchCorrelation rule = new FuzzyMatchCorrelation(config);

        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("firstname", "John");
        accountAttrs.put("lastname", "Doe");

        FuzzyMatchCorrelation.FuzzyResult result = rule.execute(emptyCtx, accountAttrs);
        assertFalse(result.isMatch());
    }

    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        Map<String, Object> fuzzy = new HashMap<>();
        fuzzy.put("maxEditDistance", 2);
        fuzzy.put("minimumScore", 0.75);
        fuzzy.put("attributes", Arrays.asList("firstname", "lastname", "email"));
        fuzzy.put("exactMatchAttributes", Arrays.asList("employeeId"));

        cfg.put("fuzzyMatch", fuzzy);
        return cfg;
    }
}
