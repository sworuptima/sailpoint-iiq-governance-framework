package com.toolkit.rules.correlation;

import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockLink;
import com.toolkit.mock.MockSailPointContext;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Correlates accounts to identities using weighted multi-attribute scoring.
 *
 * Simple correlation rules match on a single attribute (usually employeeId
 * or username). That works until it doesn't — data quality issues, missing
 * fields, or merging identity data across systems can make single-attribute
 * correlation unreliable.
 *
 * This rule scores each candidate identity across multiple attributes, each
 * with a configurable weight. An employeeId match might score 100 points,
 * an email match 80, a last name match 30, and a first name match 20. The
 * candidate with the highest total score above the threshold wins.
 *
 * Configuration keys:
 *   weightedCorrelation.attributes     - attribute name -> weight (integer)
 *   weightedCorrelation.matchThreshold - minimum score to accept a match
 *   weightedCorrelation.caseSensitive  - whether string comparisons are case-sensitive
 */
public class WeightedMultiAttributeCorrelation {

    private static final String RULE_NAME = "WeightedMultiAttributeCorrelation";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public WeightedMultiAttributeCorrelation(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public WeightedMultiAttributeCorrelation() {
        this(loadDefaultConfig());
    }

    /**
     * Finds the best matching identity for an account based on weighted
     * attribute scoring.
     *
     * @param context       the SailPoint context containing candidate identities
     * @param accountAttrs  the account attributes to match against
     * @return a CorrelationResult with the matched identity (or null) and score details
     */
    public CorrelationResult execute(MockSailPointContext context, Map<String, Object> accountAttrs) {
        LoggingUtils.logAction(LOG, RULE_NAME, "correlation", "start",
                "Correlating account with " + (accountAttrs != null ? accountAttrs.size() : 0) + " attributes");

        if (accountAttrs == null || accountAttrs.isEmpty()) {
            return new CorrelationResult(null, 0, Collections.emptyMap());
        }

        Map<String, Integer> weights = getAttributeWeights();
        int threshold = getMatchThreshold();
        boolean caseSensitive = getCaseSensitive();

        MockIdentity bestMatch = null;
        int bestScore = 0;
        Map<String, Integer> bestBreakdown = Collections.emptyMap();

        for (MockIdentity candidate : context.getAllIdentities()) {
            Map<String, Integer> breakdown = new HashMap<>();
            int score = 0;

            for (Map.Entry<String, Integer> weightEntry : weights.entrySet()) {
                String attrName = weightEntry.getKey();
                int weight = weightEntry.getValue();

                Object accountValue = accountAttrs.get(attrName);
                Object identityValue = SafeAttributeUtils.getAttribute(candidate, attrName);

                if (accountValue != null && identityValue != null) {
                    boolean match;
                    if (caseSensitive) {
                        match = accountValue.toString().equals(identityValue.toString());
                    } else {
                        match = accountValue.toString().equalsIgnoreCase(identityValue.toString());
                    }

                    if (match) {
                        score += weight;
                        breakdown.put(attrName, weight);
                    }
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
                bestBreakdown = breakdown;
            }
        }

        if (bestScore < threshold) {
            LoggingUtils.logAction(LOG, RULE_NAME, "correlation", "noMatch",
                    "Best score " + bestScore + " below threshold " + threshold);
            return new CorrelationResult(null, bestScore, bestBreakdown);
        }

        LoggingUtils.logAction(LOG, RULE_NAME, "correlation", "matched",
                "Matched to " + bestMatch.getName() + " with score " + bestScore);
        return new CorrelationResult(bestMatch, bestScore, bestBreakdown);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> getAttributeWeights() {
        Object weighted = config.get("weightedCorrelation");
        if (weighted instanceof Map) {
            Object attrs = ((Map<String, Object>) weighted).get("attributes");
            if (attrs instanceof Map) {
                Map<String, Integer> result = new HashMap<>();
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) attrs).entrySet()) {
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
    private int getMatchThreshold() {
        Object weighted = config.get("weightedCorrelation");
        if (weighted instanceof Map) {
            Object threshold = ((Map<String, Object>) weighted).get("matchThreshold");
            if (threshold instanceof Number) {
                return ((Number) threshold).intValue();
            }
        }
        return 80;
    }

    @SuppressWarnings("unchecked")
    private boolean getCaseSensitive() {
        Object weighted = config.get("weightedCorrelation");
        if (weighted instanceof Map) {
            Object cs = ((Map<String, Object>) weighted).get("caseSensitive");
            if (cs instanceof Boolean) {
                return (Boolean) cs;
            }
        }
        return false;
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                WeightedMultiAttributeCorrelation.class.getResourceAsStream("/correlation-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // --- Result container ---

    public static class CorrelationResult {
        private final MockIdentity identity;
        private final int score;
        private final Map<String, Integer> scoreBreakdown;

        public CorrelationResult(MockIdentity identity, int score, Map<String, Integer> scoreBreakdown) {
            this.identity = identity;
            this.score = score;
            this.scoreBreakdown = scoreBreakdown;
        }

        public MockIdentity getIdentity() { return identity; }
        public int getScore() { return score; }
        public Map<String, Integer> getScoreBreakdown() { return scoreBreakdown; }
        public boolean isMatch() { return identity != null; }
    }
}
