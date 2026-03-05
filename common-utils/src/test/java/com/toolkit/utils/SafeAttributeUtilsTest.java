package com.toolkit.utils;

import com.toolkit.mock.MockIdentity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SafeAttributeUtilsTest {

    @Test
    void testNullObjectReturnsNull() {
        assertNull(SafeAttributeUtils.getAttribute(null, "department"));
        assertNull(SafeAttributeUtils.getStringAttribute(null, "department"));
    }

    @Test
    void testNullAttributeNameReturnsNull() {
        MockIdentity identity = new MockIdentity("jdoe");
        assertNull(SafeAttributeUtils.getAttribute(identity, null));
    }

    @Test
    void testGetStringAttribute() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("department", "Engineering");

        assertEquals("Engineering", SafeAttributeUtils.getStringAttribute(identity, "department"));
    }

    @Test
    void testGetStringAttributeWithDefault() {
        MockIdentity identity = new MockIdentity("jdoe");

        assertEquals("Unknown", SafeAttributeUtils.getStringAttribute(identity, "department", "Unknown"));
        identity.setAttribute("department", "Engineering");
        assertEquals("Engineering", SafeAttributeUtils.getStringAttribute(identity, "department", "Unknown"));
    }

    @Test
    void testGetStringAttributeFromNonStringValue() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("employeeId", 12345);

        assertEquals("12345", SafeAttributeUtils.getStringAttribute(identity, "employeeId"));
    }

    @Test
    void testGetBooleanAttribute() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("active", true)
                .setAttribute("vip", "yes")
                .setAttribute("disabled", "false")
                .setAttribute("flagged", "1");

        assertTrue(SafeAttributeUtils.getBooleanAttribute(identity, "active", false));
        assertTrue(SafeAttributeUtils.getBooleanAttribute(identity, "vip", false));
        assertFalse(SafeAttributeUtils.getBooleanAttribute(identity, "disabled", true));
        assertTrue(SafeAttributeUtils.getBooleanAttribute(identity, "flagged", false));
    }

    @Test
    void testGetBooleanAttributeDefaultOnMissing() {
        MockIdentity identity = new MockIdentity("jdoe");

        assertTrue(SafeAttributeUtils.getBooleanAttribute(identity, "nonexistent", true));
        assertFalse(SafeAttributeUtils.getBooleanAttribute(identity, "nonexistent", false));
    }

    @Test
    void testGetIntAttribute() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("level", 5)
                .setAttribute("score", "42");

        assertEquals(5, SafeAttributeUtils.getIntAttribute(identity, "level", 0));
        assertEquals(42, SafeAttributeUtils.getIntAttribute(identity, "score", 0));
        assertEquals(-1, SafeAttributeUtils.getIntAttribute(identity, "nonexistent", -1));
    }

    @Test
    void testGetIntAttributeInvalidReturnsDefault() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("badNumber", "not-a-number");

        assertEquals(0, SafeAttributeUtils.getIntAttribute(identity, "badNumber", 0));
    }

    @Test
    void testGetListAttributeFromList() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("groups", Arrays.asList("Admin", "Users", "Developers"));

        List<String> result = SafeAttributeUtils.getListAttribute(identity, "groups");
        assertEquals(3, result.size());
        assertTrue(result.contains("Admin"));
    }

    @Test
    void testGetListAttributeFromCsvString() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("groups", "Admin, Users, Developers");

        List<String> result = SafeAttributeUtils.getListAttribute(identity, "groups");
        assertEquals(3, result.size());
        assertEquals("Admin", result.get(0));
        assertEquals("Users", result.get(1));
    }

    @Test
    void testGetListAttributeFromNull() {
        MockIdentity identity = new MockIdentity("jdoe");

        List<String> result = SafeAttributeUtils.getListAttribute(identity, "groups");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetListAttributeFromEmptyString() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("groups", "");

        List<String> result = SafeAttributeUtils.getListAttribute(identity, "groups");
        assertTrue(result.isEmpty());
    }

    @Test
    void testIsNullOrEmpty() {
        assertTrue(SafeAttributeUtils.isNullOrEmpty(null));
        assertTrue(SafeAttributeUtils.isNullOrEmpty(""));
        assertTrue(SafeAttributeUtils.isNullOrEmpty("   "));
        assertTrue(SafeAttributeUtils.isNullOrEmpty(Collections.emptyList()));

        assertFalse(SafeAttributeUtils.isNullOrEmpty("hello"));
        assertFalse(SafeAttributeUtils.isNullOrEmpty(42));
        assertFalse(SafeAttributeUtils.isNullOrEmpty(Arrays.asList("a")));
    }

    @Test
    void testObjectWithoutGetAttributeReturnsNull() {
        String plainObject = "not an identity";
        assertNull(SafeAttributeUtils.getAttribute(plainObject, "anything"));
    }
}
