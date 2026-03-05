package com.toolkit.rules.workflow;

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
import java.util.Collections;
import java.util.Map;

/**
 * Routes approval requests based on risk score, application sensitivity,
 * and organizational hierarchy.
 *
 * Not every access request should follow the same approval path. A request
 * for email access shouldn't require the same scrutiny as a request for
 * database admin privileges. Routing all requests through the same approval
 * chain either bottlenecks high-risk requests or under-reviews low-risk ones.
 *
 * This rule evaluates the risk level of a request based on the identity's
 * risk score and the sensitivity of the target application, then determines
 * who should approve it and how many approval levels are required.
 *
 * Configuration keys:
 *   dynamicRouting.riskThresholds         - risk level -> score threshold
 *   dynamicRouting.routingRules           - risk level -> approver and levels
 *   dynamicRouting.applicationSensitivity - application -> risk level override
 *   dynamicRouting.defaultRiskLevel       - fallback risk level
 */
public class DynamicApprovalRouting {

    private static final String RULE_NAME = "DynamicApprovalRouting";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public DynamicApprovalRouting(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public DynamicApprovalRouting() {
        this(loadDefaultConfig());
    }

    /**
     * Determines the approval routing for an access request.
     *
     * @param identity       the identity requesting access
     * @param applicationName the target application
     * @param requestedRole   the role being requested
     * @return a RoutingResult with the approval path
     */
    public RoutingResult execute(MockIdentity identity, String applicationName,
                                  String requestedRole) {
        LoggingUtils.logRuleEntry(LOG, RULE_NAME, identity.getName());

        // Determine risk level from identity risk score
        int riskScore = SafeAttributeUtils.getIntAttribute(identity, "riskScore", 0);
        String scoreBasedLevel = determineRiskLevel(riskScore);

        // Check application sensitivity override
        String appLevel = getApplicationSensitivity(applicationName);

        // Use the higher risk level
        String effectiveLevel = getHigherRiskLevel(scoreBasedLevel, appLevel);

        // Get routing rules for this risk level
        String approver = getApprover(effectiveLevel);
        int approvalLevels = getApprovalLevels(effectiveLevel);

        String summary = String.format(
                "Request by %s for %s on %s: risk=%s, approver=%s, levels=%d",
                identity.getName(), requestedRole, applicationName,
                effectiveLevel, approver, approvalLevels);

        LoggingUtils.logAction(LOG, RULE_NAME, identity.getName(), "route", summary);
        LoggingUtils.logRuleExit(LOG, RULE_NAME, identity.getName(), summary);

        return new RoutingResult(identity.getName(), applicationName, requestedRole,
                effectiveLevel, approver, approvalLevels, summary);
    }

    /**
     * Maps a numeric risk score to a risk level string.
     */
    String determineRiskLevel(int riskScore) {
        Map<String, Integer> thresholds = getRiskThresholds();
        String defaultLevel = getDefaultRiskLevel();

        // Check from highest to lowest
        if (riskScore >= thresholds.getOrDefault("critical", 95)) {
            return "critical";
        }
        if (riskScore >= thresholds.getOrDefault("high", 80)) {
            return "high";
        }
        if (riskScore >= thresholds.getOrDefault("medium", 60)) {
            return "medium";
        }
        if (riskScore >= thresholds.getOrDefault("low", 30)) {
            return "low";
        }
        return defaultLevel;
    }

    private String getHigherRiskLevel(String levelA, String levelB) {
        if (levelA == null) return levelB != null ? levelB : getDefaultRiskLevel();
        if (levelB == null) return levelA;

        int rankA = riskRank(levelA);
        int rankB = riskRank(levelB);
        return rankA >= rankB ? levelA : levelB;
    }

    private int riskRank(String level) {
        switch (level) {
            case "critical": return 4;
            case "high": return 3;
            case "medium": return 2;
            case "low": return 1;
            default: return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> getRiskThresholds() {
        Object routing = config.get("dynamicRouting");
        if (routing instanceof Map) {
            Object thresholds = ((Map<String, Object>) routing).get("riskThresholds");
            if (thresholds instanceof Map) {
                Map<String, Integer> result = new java.util.HashMap<>();
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) thresholds).entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        result.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                    }
                }
                return result;
            }
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private String getApplicationSensitivity(String applicationName) {
        Object routing = config.get("dynamicRouting");
        if (routing instanceof Map) {
            Object appSens = ((Map<String, Object>) routing).get("applicationSensitivity");
            if (appSens instanceof Map) {
                Object level = ((Map<String, Object>) appSens).get(applicationName);
                if (level instanceof String) {
                    return (String) level;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String getApprover(String riskLevel) {
        Object routing = config.get("dynamicRouting");
        if (routing instanceof Map) {
            Object rules = ((Map<String, Object>) routing).get("routingRules");
            if (rules instanceof Map) {
                Object levelRule = ((Map<String, Object>) rules).get(riskLevel);
                if (levelRule instanceof Map) {
                    Object approver = ((Map<String, Object>) levelRule).get("approver");
                    if (approver instanceof String) {
                        return (String) approver;
                    }
                }
            }
        }
        return "manager";
    }

    @SuppressWarnings("unchecked")
    private int getApprovalLevels(String riskLevel) {
        Object routing = config.get("dynamicRouting");
        if (routing instanceof Map) {
            Object rules = ((Map<String, Object>) routing).get("routingRules");
            if (rules instanceof Map) {
                Object levelRule = ((Map<String, Object>) rules).get(riskLevel);
                if (levelRule instanceof Map) {
                    Object levels = ((Map<String, Object>) levelRule).get("approvalLevels");
                    if (levels instanceof Number) {
                        return ((Number) levels).intValue();
                    }
                }
            }
        }
        return 1;
    }

    @SuppressWarnings("unchecked")
    private String getDefaultRiskLevel() {
        Object routing = config.get("dynamicRouting");
        if (routing instanceof Map) {
            Object level = ((Map<String, Object>) routing).get("defaultRiskLevel");
            if (level instanceof String) {
                return (String) level;
            }
        }
        return "medium";
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                DynamicApprovalRouting.class.getResourceAsStream("/workflow-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // --- Result container ---

    public static class RoutingResult {
        private final String identityName;
        private final String applicationName;
        private final String requestedRole;
        private final String riskLevel;
        private final String approver;
        private final int approvalLevels;
        private final String summary;

        public RoutingResult(String identityName, String applicationName,
                             String requestedRole, String riskLevel,
                             String approver, int approvalLevels, String summary) {
            this.identityName = identityName;
            this.applicationName = applicationName;
            this.requestedRole = requestedRole;
            this.riskLevel = riskLevel;
            this.approver = approver;
            this.approvalLevels = approvalLevels;
            this.summary = summary;
        }

        public String getIdentityName() { return identityName; }
        public String getApplicationName() { return applicationName; }
        public String getRequestedRole() { return requestedRole; }
        public String getRiskLevel() { return riskLevel; }
        public String getApprover() { return approver; }
        public int getApprovalLevels() { return approvalLevels; }
        public String getSummary() { return summary; }
    }
}
