package com.toolkit.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Null-safe attribute access utilities for SailPoint identity objects.
 *
 * Uses reflection to call getAttribute(String) so that the same utility
 * works with both mock objects (for testing) and real SailPoint objects
 * (in production) without a compile-time dependency on the SailPoint JAR.
 */
public final class SafeAttributeUtils {

    private SafeAttributeUtils() {
        // Static utility class
    }

    /**
     * Retrieves an attribute value from an object using reflection.
     * Returns null if the object is null, the attribute name is null,
     * or the method call fails.
     */
    public static Object getAttribute(Object object, String attributeName) {
        if (object == null || attributeName == null) {
            return null;
        }
        try {
            Method method = object.getClass().getMethod("getAttribute", String.class);
            return method.invoke(object, attributeName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieves an attribute as a String. Returns null if the attribute
     * is null or the object does not support getAttribute.
     */
    public static String getStringAttribute(Object object, String attributeName) {
        Object value = getAttribute(object, attributeName);
        return value != null ? value.toString() : null;
    }

    /**
     * Retrieves an attribute as a String with a default fallback.
     */
    public static String getStringAttribute(Object object, String attributeName, String defaultValue) {
        String value = getStringAttribute(object, attributeName);
        return value != null ? value : defaultValue;
    }

    /**
     * Retrieves an attribute as a boolean. Returns the default value if
     * the attribute is null or cannot be parsed.
     */
    public static boolean getBooleanAttribute(Object object, String attributeName, boolean defaultValue) {
        Object value = getAttribute(object, attributeName);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String str = value.toString().trim().toLowerCase();
        if ("true".equals(str) || "yes".equals(str) || "1".equals(str)) {
            return true;
        }
        if ("false".equals(str) || "no".equals(str) || "0".equals(str)) {
            return false;
        }
        return defaultValue;
    }

    /**
     * Retrieves an attribute as an int. Returns the default value if
     * the attribute is null or cannot be parsed.
     */
    public static int getIntAttribute(Object object, String attributeName, int defaultValue) {
        Object value = getAttribute(object, attributeName);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Retrieves an attribute as a List of Strings. Handles multiple input types:
     * - null returns an empty list
     * - String is split by comma
     * - Collection is converted element-wise
     * - Single non-string object is wrapped in a list
     */
    @SuppressWarnings("unchecked")
    public static List<String> getListAttribute(Object object, String attributeName) {
        Object value = getAttribute(object, attributeName);
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        if (value instanceof Collection) {
            List<String> result = new ArrayList<>();
            for (Object item : (Collection<?>) value) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) {
                return Collections.emptyList();
            }
            return new ArrayList<>(Arrays.asList(str.split("\\s*,\\s*")));
        }
        return Collections.singletonList(value.toString());
    }

    /**
     * Checks whether a value is null or effectively empty.
     * Handles String (empty/whitespace), Collection (empty), and null.
     */
    public static boolean isNullOrEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        return false;
    }
}
