package com.toolkit.rules.aggregation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NestedGroupFlattenerTest {

    private Map<String, Object> config;
    private Map<String, List<String>> groupHierarchy;

    @BeforeEach
    void setUp() {
        config = buildTestConfig();

        // Build a hierarchy: Dev -> Engineering -> All-Employees
        groupHierarchy = new HashMap<>();
        groupHierarchy.put("Engineering-Dev", Arrays.asList("Engineering"));
        groupHierarchy.put("Engineering-QA", Arrays.asList("Engineering"));
        groupHierarchy.put("Engineering", Arrays.asList("All-Employees"));
        groupHierarchy.put("Sales-East", Arrays.asList("Sales"));
        groupHierarchy.put("Sales", Arrays.asList("All-Employees"));
    }

    @Test
    void testFlattenDirectAndParentGroups() {
        NestedGroupFlattener rule = new NestedGroupFlattener(config);
        List<String> directGroups = Arrays.asList("Engineering-Dev");

        NestedGroupFlattener.FlattenResult result = rule.execute(directGroups, groupHierarchy);

        assertTrue(result.getAllGroups().contains("Engineering-Dev"));
        assertTrue(result.getAllGroups().contains("Engineering"));
        assertTrue(result.getAllGroups().contains("All-Employees"));
        assertEquals(3, result.getTotalCount());
        assertEquals(1, result.getDirectCount());
        assertEquals(2, result.getResolvedCount());
    }

    @Test
    void testFlattenMultipleDirectGroups() {
        NestedGroupFlattener rule = new NestedGroupFlattener(config);
        List<String> directGroups = Arrays.asList("Engineering-Dev", "Sales-East");

        NestedGroupFlattener.FlattenResult result = rule.execute(directGroups, groupHierarchy);

        assertTrue(result.getAllGroups().contains("Engineering-Dev"));
        assertTrue(result.getAllGroups().contains("Engineering"));
        assertTrue(result.getAllGroups().contains("Sales-East"));
        assertTrue(result.getAllGroups().contains("Sales"));
        assertTrue(result.getAllGroups().contains("All-Employees"));
        assertEquals(5, result.getTotalCount());
    }

    @Test
    void testDeduplication() {
        NestedGroupFlattener rule = new NestedGroupFlattener(config);
        // Both Engineering-Dev and Engineering-QA resolve to Engineering -> All-Employees
        List<String> directGroups = Arrays.asList("Engineering-Dev", "Engineering-QA");

        NestedGroupFlattener.FlattenResult result = rule.execute(directGroups, groupHierarchy);

        assertTrue(result.getAllGroups().contains("Engineering-Dev"));
        assertTrue(result.getAllGroups().contains("Engineering-QA"));
        assertTrue(result.getAllGroups().contains("Engineering"));
        assertTrue(result.getAllGroups().contains("All-Employees"));
        assertEquals(4, result.getTotalCount());
    }

    @Test
    void testMaxDepthPreventsInfiniteRecursion() {
        Map<String, Object> shallowConfig = new HashMap<>();
        Map<String, Object> nestedGroupConfig = new HashMap<>();
        nestedGroupConfig.put("maxDepth", 1);
        nestedGroupConfig.put("includeParentGroups", true);
        shallowConfig.put("nestedGroupConfig", nestedGroupConfig);

        NestedGroupFlattener rule = new NestedGroupFlattener(shallowConfig);
        List<String> directGroups = Arrays.asList("Engineering-Dev");

        NestedGroupFlattener.FlattenResult result = rule.execute(directGroups, groupHierarchy);

        // At depth 1, should get Engineering but NOT All-Employees
        assertTrue(result.getAllGroups().contains("Engineering-Dev"));
        assertTrue(result.getAllGroups().contains("Engineering"));
        assertFalse(result.getAllGroups().contains("All-Employees"));
    }

    @Test
    void testCircularGroupReference() {
        // Create circular: A -> B -> C -> A
        Map<String, List<String>> circular = new HashMap<>();
        circular.put("GroupA", Arrays.asList("GroupB"));
        circular.put("GroupB", Arrays.asList("GroupC"));
        circular.put("GroupC", Arrays.asList("GroupA"));

        NestedGroupFlattener rule = new NestedGroupFlattener(config);
        List<String> directGroups = Arrays.asList("GroupA");

        NestedGroupFlattener.FlattenResult result = rule.execute(directGroups, circular);

        // Should handle circular reference without infinite loop
        assertTrue(result.getAllGroups().contains("GroupA"));
        assertTrue(result.getAllGroups().contains("GroupB"));
        assertTrue(result.getAllGroups().contains("GroupC"));
        assertEquals(3, result.getTotalCount());
    }

    @Test
    void testDisableParentGroupResolution() {
        Map<String, Object> noParentConfig = new HashMap<>();
        Map<String, Object> nestedGroupConfig = new HashMap<>();
        nestedGroupConfig.put("maxDepth", 5);
        nestedGroupConfig.put("includeParentGroups", false);
        noParentConfig.put("nestedGroupConfig", nestedGroupConfig);

        NestedGroupFlattener rule = new NestedGroupFlattener(noParentConfig);
        List<String> directGroups = Arrays.asList("Engineering-Dev");

        NestedGroupFlattener.FlattenResult result = rule.execute(directGroups, groupHierarchy);

        assertEquals(1, result.getTotalCount());
        assertTrue(result.getAllGroups().contains("Engineering-Dev"));
    }

    @Test
    void testNullDirectGroups() {
        NestedGroupFlattener rule = new NestedGroupFlattener(config);
        NestedGroupFlattener.FlattenResult result = rule.execute((List<String>) null, groupHierarchy);
        assertEquals(0, result.getTotalCount());
    }

    @Test
    void testDelimitedStringInput() {
        NestedGroupFlattener rule = new NestedGroupFlattener(config);
        String groupString = "Engineering-Dev;Sales-East";

        NestedGroupFlattener.FlattenResult result = rule.execute(groupString, ";", groupHierarchy);

        assertTrue(result.getAllGroups().contains("Engineering-Dev"));
        assertTrue(result.getAllGroups().contains("Engineering"));
        assertTrue(result.getAllGroups().contains("Sales-East"));
        assertTrue(result.getAllGroups().contains("Sales"));
        assertTrue(result.getAllGroups().contains("All-Employees"));
    }

    @Test
    void testEmptyGroupString() {
        NestedGroupFlattener rule = new NestedGroupFlattener(config);
        NestedGroupFlattener.FlattenResult result = rule.execute("", ";", groupHierarchy);
        assertEquals(0, result.getTotalCount());
    }

    @Test
    void testNoHierarchyAvailable() {
        NestedGroupFlattener rule = new NestedGroupFlattener(config);
        List<String> directGroups = Arrays.asList("Engineering-Dev", "Sales-East");

        NestedGroupFlattener.FlattenResult result = rule.execute(directGroups, null);

        assertEquals(2, result.getTotalCount());
        assertEquals(0, result.getResolvedCount());
    }

    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();
        Map<String, Object> nestedGroupConfig = new HashMap<>();
        nestedGroupConfig.put("maxDepth", 5);
        nestedGroupConfig.put("includeParentGroups", true);
        nestedGroupConfig.put("groupAttribute", "memberOf");
        cfg.put("nestedGroupConfig", nestedGroupConfig);
        cfg.put("groupDelimiter", ";");
        return cfg;
    }
}
