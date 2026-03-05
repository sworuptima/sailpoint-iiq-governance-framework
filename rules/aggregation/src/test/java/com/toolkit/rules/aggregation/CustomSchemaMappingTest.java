package com.toolkit.rules.aggregation;

import com.toolkit.mock.MockLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomSchemaMappingTest {

    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        config = buildTestConfig();
    }

    @Test
    void testActiveDirectoryMapping() {
        CustomSchemaMapping rule = new CustomSchemaMapping(config);

        Map<String, Object> rawAttrs = new HashMap<>();
        rawAttrs.put("sAMAccountName", "jdoe");
        rawAttrs.put("givenName", "John");
        rawAttrs.put("sn", "Doe");
        rawAttrs.put("mail", "john.doe@company.com");
        rawAttrs.put("department", "Engineering");

        Map<String, Object> mapped = rule.execute("Active Directory", rawAttrs);

        assertEquals("jdoe", mapped.get("accountName"));
        assertEquals("John", mapped.get("firstname"));
        assertEquals("Doe", mapped.get("lastname"));
        assertEquals("john.doe@company.com", mapped.get("email"));
        assertEquals("Engineering", mapped.get("department"));
    }

    @Test
    void testHRSystemMapping() {
        CustomSchemaMapping rule = new CustomSchemaMapping(config);

        Map<String, Object> rawAttrs = new HashMap<>();
        rawAttrs.put("EMPLOYEE_ID", "EMP001");
        rawAttrs.put("FIRST_NAME", "Jane");
        rawAttrs.put("LAST_NAME", "Smith");
        rawAttrs.put("DEPT_CODE", "Sales");

        Map<String, Object> mapped = rule.execute("HR System", rawAttrs);

        assertEquals("EMP001", mapped.get("employeeId"));
        assertEquals("Jane", mapped.get("firstname"));
        assertEquals("Smith", mapped.get("lastname"));
        assertEquals("Sales", mapped.get("department"));
    }

    @Test
    void testUnknownApplicationUsesDefaults() {
        CustomSchemaMapping rule = new CustomSchemaMapping(config);

        Map<String, Object> rawAttrs = new HashMap<>();
        rawAttrs.put("username", "jdoe");
        rawAttrs.put("first_name", "John");
        rawAttrs.put("customField", "customValue");

        Map<String, Object> mapped = rule.execute("Unknown App", rawAttrs);

        assertEquals("jdoe", mapped.get("accountName"));
        assertEquals("John", mapped.get("firstname"));
        // Unmapped attributes pass through
        assertEquals("customValue", mapped.get("customField"));
    }

    @Test
    void testNullAttributes() {
        CustomSchemaMapping rule = new CustomSchemaMapping(config);
        Map<String, Object> mapped = rule.execute("Active Directory", null);
        assertTrue(mapped.isEmpty());
    }

    @Test
    void testEmptyAttributes() {
        CustomSchemaMapping rule = new CustomSchemaMapping(config);
        Map<String, Object> mapped = rule.execute("Active Directory", Collections.emptyMap());
        assertTrue(mapped.isEmpty());
    }

    @Test
    void testEmptyConfig() {
        CustomSchemaMapping rule = new CustomSchemaMapping(Collections.emptyMap());

        Map<String, Object> rawAttrs = new HashMap<>();
        rawAttrs.put("sAMAccountName", "jdoe");

        Map<String, Object> mapped = rule.execute("Active Directory", rawAttrs);
        // With no mappings, attributes pass through unchanged
        assertEquals("jdoe", mapped.get("sAMAccountName"));
    }

    @Test
    void testApplyToLink() {
        CustomSchemaMapping rule = new CustomSchemaMapping(config);

        MockLink link = new MockLink("Active Directory", "jdoe");
        link.setAttribute("sAMAccountName", "jdoe");
        link.setAttribute("givenName", "John");
        link.setAttribute("sn", "Doe");

        MockLink result = rule.applyToLink(link);

        assertSame(link, result);
        assertEquals("John", link.getAttribute("firstname"));
        assertEquals("Doe", link.getAttribute("lastname"));
        assertEquals("jdoe", link.getAttribute("accountName"));
    }

    @Test
    void testApplyToNullLink() {
        CustomSchemaMapping rule = new CustomSchemaMapping(config);
        assertNull(rule.applyToLink(null));
    }

    @Test
    void testResolveMappingsAppSpecificOverridesDefault() {
        CustomSchemaMapping rule = new CustomSchemaMapping(config);

        Map<String, String> adMappings = rule.resolveMappings("Active Directory");
        // AD-specific mapping should be present
        assertEquals("accountName", adMappings.get("sAMAccountName"));
        // Default mapping key should also be present
        assertEquals("accountName", adMappings.get("username"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        Map<String, Object> schemaMappings = new HashMap<>();

        Map<String, String> adMap = new HashMap<>();
        adMap.put("sAMAccountName", "accountName");
        adMap.put("givenName", "firstname");
        adMap.put("sn", "lastname");
        adMap.put("mail", "email");
        adMap.put("department", "department");
        schemaMappings.put("Active Directory", adMap);

        Map<String, String> hrMap = new HashMap<>();
        hrMap.put("EMPLOYEE_ID", "employeeId");
        hrMap.put("FIRST_NAME", "firstname");
        hrMap.put("LAST_NAME", "lastname");
        hrMap.put("DEPT_CODE", "department");
        schemaMappings.put("HR System", hrMap);

        cfg.put("schemaMappings", schemaMappings);

        Map<String, String> defaults = new HashMap<>();
        defaults.put("username", "accountName");
        defaults.put("first_name", "firstname");
        cfg.put("defaultMappings", defaults);

        return cfg;
    }
}
