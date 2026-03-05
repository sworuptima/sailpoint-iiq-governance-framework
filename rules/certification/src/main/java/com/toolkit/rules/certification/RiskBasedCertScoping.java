package com.toolkit.rules.certification;

import com.toolkit.mock.MockBundle;
import com.toolkit.mock.MockIdentity;
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
 * Scopes certification campaigns to include only high-risk entitlements.
 *
 * Certifying every entitlement for every user creates review fatigue.
 * A manager with 50 reports and 20 entitlements each faces 1,000 decisions.
 * Most of that access is low-risk — email, intranet, basic tools. The items
 * that actually matter — admin access, financial systems, privileged roles —
 * get the same 3-second glance as everything else.
 *
 * This rule focuses certification on what matters: identities with risk scores
 * above a threshold, identities holding roles from a high-risk list, or
 * identities with specific privilege-indicating attributes.
 *
 * Configuration keys:
 *   riskBasedScoping.riskAttribute         - the identity attribute holding risk score
 *   riskBasedScoping.highRiskThreshold     - numeric threshold for high risk
 *   riskBasedScoping.highRiskRoles         - role names that always warrant review
 *   riskBasedScoping.alwaysIncludeAttributes - attributes that force inclusion
 *   riskBasedScoping.alwaysIncludeValue    - value of the attribute that triggers inclusion
 */
public class RiskBasedCertScoping {

    private static final String RULE_NAME = "RiskBasedCertScoping";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public RiskBasedCertScoping(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public RiskBasedCertScoping() {
        this(loadDefaultConfig());
    }

    /**
     * Evaluates whether an identity should be included in a risk-focused
     * certification campaign.
     *
     * @param identity the identity to evaluate
     * @return a ScopingResult indicating inclusion/exclusion and reasons
     */
    public ScopingResult execute(MockIdentity identity) {
        LoggingUtils.logRuleEntry(LOG, RULE_NAME, identity.getName());

        List<String> reasons = new ArrayList<>();

        // Check risk score
        if (isAboveRiskThreshold(identity)) {
            reasons.add("Risk score above threshold");
        }

        // Check for high-risk roles
        List<String> highRiskRolesHeld = getHighRiskRolesHeld(identity);
        if (!highRiskRolesHeld.isEmpty()) {
            reasons.add("Holds high-risk roles: " + String.join(", ", highRiskRolesHeld));
        }

        // Check for privilege-indicating attributes
        if (hasPrivilegeAttribute(identity)) {
            reasons.add("Has privileged access attribute");
        }

        boolean included = !reasons.isEmpty();

        LoggingUtils.logAction(LOG, RULE_NAME, identity.getName(),
                included ? "include" : "exclude",
                included ? String.join("; ", reasons) : "Below risk threshold, no high-risk roles");

        LoggingUtils.logRuleExit(LOG, RULE_NAME, identity.getName(),
                included ? "IN SCOPE" : "OUT OF SCOPE");
        return new ScopingResult(included, reasons, highRiskRolesHeld);
    }

    private boolean isAboveRiskThreshold(MockIdentity identity) {
        String riskAttr = getRiskAttribute();
        int threshold = getHighRiskThreshold();

        int riskScore = SafeAttributeUtils.getIntAttribute(identity, riskAttr, 0);
        return riskScore >= threshold;
    }

    List<String> getHighRiskRolesHeld(MockIdentity identity) {
        List<String> highRiskRoles = getHighRiskRoles();
        List<String> held = new ArrayList<>();

        for (MockBundle bundle : identity.getBundles()) {
            if (highRiskRoles.contains(bundle.getName())) {
                held.add(bundle.getName());
            }
        }
        return held;
    }

    private boolean hasPrivilegeAttribute(MockIdentity identity) {
        List<String> attrs = getAlwaysIncludeAttributes();
        String expectedValue = getAlwaysIncludeValue();

        for (String attr : attrs) {
            String value = SafeAttributeUtils.getStringAttribute(identity, attr);
            if (expectedValue.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private String getRiskAttribute() {
        Object scope = config.get("riskBasedScoping");
        if (scope instanceof Map) {
            Object attr = ((Map<String, Object>) scope).get("riskAttribute");
            if (attr instanceof String) {
                return (String) attr;
            }
        }
        return "riskScore";
    }

    @SuppressWarnings("unchecked")
    private int getHighRiskThreshold() {
        Object scope = config.get("riskBasedScoping");
        if (scope instanceof Map) {
            Object threshold = ((Map<String, Object>) scope).get("highRiskThreshold");
            if (threshold instanceof Number) {
                return ((Number) threshold).intValue();
            }
        }
        return 70;
    }

    @SuppressWarnings("unchecked")
    private List<String> getHighRiskRoles() {
        Object scope = config.get("riskBasedScoping");
        if (scope instanceof Map) {
            Object roles = ((Map<String, Object>) scope).get("highRiskRoles");
            if (roles instanceof List) {
                return (List<String>) roles;
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> getAlwaysIncludeAttributes() {
        Object scope = config.get("riskBasedScoping");
        if (scope instanceof Map) {
            Object attrs = ((Map<String, Object>) scope).get("alwaysIncludeAttributes");
            if (attrs instanceof List) {
                return (List<String>) attrs;
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private String getAlwaysIncludeValue() {
        Object scope = config.get("riskBasedScoping");
        if (scope instanceof Map) {
            Object val = ((Map<String, Object>) scope).get("alwaysIncludeValue");
            if (val instanceof String) {
                return (String) val;
            }
        }
        return "true";
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                RiskBasedCertScoping.class.getResourceAsStream("/certification-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // --- Result container ---

    public static class ScopingResult {
        private final boolean included;
        private final List<String> reasons;
        private final List<String> highRiskRolesHeld;

        public ScopingResult(boolean included, List<String> reasons, List<String> highRiskRolesHeld) {
            this.included = included;
            this.reasons = reasons;
            this.highRiskRolesHeld = highRiskRolesHeld;
        }

        public boolean isIncluded() { return included; }
        public List<String> getReasons() { return reasons; }
        public List<String> getHighRiskRolesHeld() { return highRiskRolesHeld; }
    }
}
