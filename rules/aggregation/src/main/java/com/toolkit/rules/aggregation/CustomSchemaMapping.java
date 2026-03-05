package com.toolkit.rules.aggregation;

import com.toolkit.mock.MockLink;
import com.toolkit.utils.LoggingUtils;
import com.toolkit.utils.SafeAttributeUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps non-standard source system attributes into a normalized IIQ schema.
 *
 * Every target system stores identity data differently. Active Directory uses
 * sAMAccountName, LDAP uses uid, HR systems use EMPLOYEE_ID — but IIQ needs
 * them all mapped to a consistent set of attribute names for correlation and
 * governance rules to work.
 *
 * This rule reads a schema mapping configuration that defines per-application
 * attribute translations. During aggregation, it transforms raw account data
 * into the canonical format before IIQ stores it.
 *
 * Configuration keys:
 *   schemaMappings  - application name -> map of source attribute -> target attribute
 *   defaultMappings - fallback mappings when no application-specific mapping exists
 */
public class CustomSchemaMapping {

    private static final String RULE_NAME = "CustomSchemaMapping";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public CustomSchemaMapping(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public CustomSchemaMapping() {
        this(loadDefaultConfig());
    }

    /**
     * Transforms a raw account record from a source system into the normalized
     * IIQ schema based on the application-specific mapping configuration.
     *
     * @param applicationName the source application name
     * @param rawAttributes   the raw attributes as read from the source system
     * @return a new map with attribute names translated to the IIQ schema
     */
    public Map<String, Object> execute(String applicationName, Map<String, Object> rawAttributes) {
        LoggingUtils.logAction(LOG, RULE_NAME, applicationName, "mapSchema",
                "Processing " + (rawAttributes != null ? rawAttributes.size() : 0) + " attributes");

        if (rawAttributes == null || rawAttributes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> mapping = resolveMappings(applicationName);
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : rawAttributes.entrySet()) {
            String sourceAttr = entry.getKey();
            String targetAttr = mapping.get(sourceAttr);

            if (targetAttr != null) {
                result.put(targetAttr, entry.getValue());
                LoggingUtils.logAction(LOG, RULE_NAME, applicationName, "mapAttribute",
                        sourceAttr + " -> " + targetAttr);
            } else {
                // Pass through unmapped attributes with their original names
                result.put(sourceAttr, entry.getValue());
            }
        }

        LoggingUtils.logAction(LOG, RULE_NAME, applicationName, "mapComplete",
                result.size() + " attributes mapped");
        return result;
    }

    /**
     * Applies the schema mapping directly to a MockLink, replacing its
     * attributes with the normalized versions.
     *
     * @param link the account link to transform
     * @return the same link with transformed attributes
     */
    public MockLink applyToLink(MockLink link) {
        if (link == null) {
            return null;
        }

        Map<String, Object> rawAttributes = new HashMap<>(link.getAttributes());
        Map<String, Object> mapped = execute(link.getApplicationName(), rawAttributes);

        // Clear existing and set mapped attributes
        for (Map.Entry<String, Object> entry : mapped.entrySet()) {
            link.setAttribute(entry.getKey(), entry.getValue());
        }

        return link;
    }

    /**
     * Resolves the mapping for a given application, falling back to default
     * mappings for any attributes not explicitly mapped.
     */
    @SuppressWarnings("unchecked")
    Map<String, String> resolveMappings(String applicationName) {
        Map<String, String> resolved = new HashMap<>();

        // Start with default mappings
        Object defaults = config.get("defaultMappings");
        if (defaults instanceof Map) {
            resolved.putAll((Map<String, String>) defaults);
        }

        // Override with application-specific mappings
        Object schemaMappings = config.get("schemaMappings");
        if (schemaMappings instanceof Map) {
            Map<String, Object> appMappings = (Map<String, Object>) schemaMappings;
            Object appSpecific = appMappings.get(applicationName);
            if (appSpecific instanceof Map) {
                resolved.putAll((Map<String, String>) appSpecific);
            }
        }

        return resolved;
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                CustomSchemaMapping.class.getResourceAsStream("/aggregation-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
