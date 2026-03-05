package com.toolkit.rules.certification;

import com.toolkit.mock.MockIdentity;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Excludes service and system accounts from manager certification campaigns.
 *
 * A certification campaign that includes svc_ldap, system_backup, and
 * admin_monitoring alongside real employees creates noise. Managers don't
 * own service accounts and can't make informed decisions about them. These
 * accounts should be certified through a separate, application-owner-driven
 * process.
 *
 * This rule evaluates each identity against configurable patterns and
 * attributes to determine if it should be excluded from the certification.
 * It supports name pattern matching, identity type attribute checking,
 * inactive identity exclusion, and disabled account filtering.
 *
 * Configuration keys:
 *   serviceAccountExclusion.serviceAccountPatterns   - wildcard patterns for service account names
 *   serviceAccountExclusion.serviceAccountAttribute  - attribute name to check for account type
 *   serviceAccountExclusion.serviceAccountValues     - values that indicate a service account
 *   serviceAccountExclusion.excludeInactive          - whether to exclude inactive identities
 *   serviceAccountExclusion.excludeDisabledAccounts  - whether to exclude identities with only disabled links
 */
public class ServiceAccountExclusion {

    private static final String RULE_NAME = "ServiceAccountExclusion";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public ServiceAccountExclusion(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public ServiceAccountExclusion() {
        this(loadDefaultConfig());
    }

    /**
     * Determines whether an identity should be excluded from a certification campaign.
     *
     * @param identity the identity to evaluate
     * @return an ExclusionResult indicating whether to exclude and why
     */
    public ExclusionResult execute(MockIdentity identity) {
        LoggingUtils.logRuleEntry(LOG, RULE_NAME, identity.getName());

        List<String> reasons = new ArrayList<>();

        // Check name patterns
        if (matchesServiceAccountPattern(identity.getName())) {
            reasons.add("Name matches service account pattern");
        }

        // Check identity type attribute
        if (isServiceAccountType(identity)) {
            reasons.add("Identity type is service/system account");
        }

        // Check inactive status
        if (shouldExcludeInactive() && identity.isInactive()) {
            reasons.add("Identity is inactive");
        }

        // Check if all accounts are disabled
        if (shouldExcludeDisabled() && hasOnlyDisabledAccounts(identity)) {
            reasons.add("All linked accounts are disabled");
        }

        boolean excluded = !reasons.isEmpty();

        if (excluded) {
            LoggingUtils.logAction(LOG, RULE_NAME, identity.getName(),
                    "exclude", String.join("; ", reasons));
        }

        LoggingUtils.logRuleExit(LOG, RULE_NAME, identity.getName(),
                excluded ? "EXCLUDED" : "INCLUDED");
        return new ExclusionResult(excluded, reasons);
    }

    /**
     * Checks if an identity name matches any of the configured service account patterns.
     * Supports simple wildcard matching with * at the start or end of the pattern.
     */
    boolean matchesServiceAccountPattern(String name) {
        if (name == null) {
            return false;
        }

        for (String pattern : getServiceAccountPatterns()) {
            if (matchesWildcard(name, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesWildcard(String value, String pattern) {
        if (pattern.startsWith("*") && pattern.endsWith("*")) {
            return value.contains(pattern.substring(1, pattern.length() - 1));
        } else if (pattern.endsWith("*")) {
            return value.startsWith(pattern.substring(0, pattern.length() - 1));
        } else if (pattern.startsWith("*")) {
            return value.endsWith(pattern.substring(1));
        }
        return value.equals(pattern);
    }

    private boolean isServiceAccountType(MockIdentity identity) {
        String attrName = getServiceAccountAttribute();
        if (attrName == null) {
            return false;
        }

        String value = SafeAttributeUtils.getStringAttribute(identity, attrName);
        if (value == null) {
            return false;
        }

        for (String svcValue : getServiceAccountValues()) {
            if (svcValue.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOnlyDisabledAccounts(MockIdentity identity) {
        List<MockLink> links = identity.getLinks();
        if (links.isEmpty()) {
            return false; // No accounts isn't the same as all disabled
        }

        for (MockLink link : links) {
            if (!link.isDisabled()) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<String> getServiceAccountPatterns() {
        Object excl = config.get("serviceAccountExclusion");
        if (excl instanceof Map) {
            Object patterns = ((Map<String, Object>) excl).get("serviceAccountPatterns");
            if (patterns instanceof List) {
                return (List<String>) patterns;
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private String getServiceAccountAttribute() {
        Object excl = config.get("serviceAccountExclusion");
        if (excl instanceof Map) {
            Object attr = ((Map<String, Object>) excl).get("serviceAccountAttribute");
            if (attr instanceof String) {
                return (String) attr;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getServiceAccountValues() {
        Object excl = config.get("serviceAccountExclusion");
        if (excl instanceof Map) {
            Object vals = ((Map<String, Object>) excl).get("serviceAccountValues");
            if (vals instanceof List) {
                return (List<String>) vals;
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private boolean shouldExcludeInactive() {
        Object excl = config.get("serviceAccountExclusion");
        if (excl instanceof Map) {
            Object val = ((Map<String, Object>) excl).get("excludeInactive");
            if (val instanceof Boolean) {
                return (Boolean) val;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean shouldExcludeDisabled() {
        Object excl = config.get("serviceAccountExclusion");
        if (excl instanceof Map) {
            Object val = ((Map<String, Object>) excl).get("excludeDisabledAccounts");
            if (val instanceof Boolean) {
                return (Boolean) val;
            }
        }
        return true;
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                ServiceAccountExclusion.class.getResourceAsStream("/certification-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // --- Result container ---

    public static class ExclusionResult {
        private final boolean excluded;
        private final List<String> reasons;

        public ExclusionResult(boolean excluded, List<String> reasons) {
            this.excluded = excluded;
            this.reasons = reasons;
        }

        public boolean isExcluded() { return excluded; }
        public List<String> getReasons() { return reasons; }
    }
}
