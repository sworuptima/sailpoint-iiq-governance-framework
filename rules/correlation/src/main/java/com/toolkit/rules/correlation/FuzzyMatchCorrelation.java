package com.toolkit.rules.correlation;

import com.toolkit.mock.MockIdentity;
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
 * Correlates accounts to identities using fuzzy string matching.
 *
 * Exact-match correlation fails when source data has typos, name variations,
 * or inconsistent formatting. "Jon" vs "John", "O'Brien" vs "OBrien",
 * "Smith-Jones" vs "SmithJones" — these are the same person but different
 * strings.
 *
 * This rule uses Levenshtein edit distance to compute a similarity score
 * between attribute values. It checks exact-match attributes first (like
 * employeeId) for a guaranteed match, then falls back to fuzzy matching
 * on configurable attributes. The candidate with the highest combined
 * similarity above the minimum score threshold wins.
 *
 * Configuration keys:
 *   fuzzyMatch.maxEditDistance       - maximum allowed edit distance for a fuzzy match
 *   fuzzyMatch.minimumScore         - minimum similarity score (0.0 to 1.0) to accept
 *   fuzzyMatch.attributes           - list of attributes to fuzzy-match on
 *   fuzzyMatch.exactMatchAttributes - attributes that must match exactly (checked first)
 */
public class FuzzyMatchCorrelation {

    private static final String RULE_NAME = "FuzzyMatchCorrelation";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public FuzzyMatchCorrelation(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public FuzzyMatchCorrelation() {
        this(loadDefaultConfig());
    }

    /**
     * Finds the best fuzzy match for an account across all candidate identities.
     *
     * @param context      the SailPoint context containing candidate identities
     * @param accountAttrs the account attributes to match against
     * @return the best matching identity with similarity scores
     */
    public FuzzyResult execute(MockSailPointContext context, Map<String, Object> accountAttrs) {
        LoggingUtils.logAction(LOG, RULE_NAME, "correlation", "start",
                "Fuzzy correlating with " + (accountAttrs != null ? accountAttrs.size() : 0) + " attributes");

        if (accountAttrs == null || accountAttrs.isEmpty()) {
            return new FuzzyResult(null, 0.0, Collections.emptyMap());
        }

        List<String> exactAttrs = getExactMatchAttributes();
        List<String> fuzzyAttrs = getFuzzyAttributes();
        double minScore = getMinimumScore();

        // First pass: check exact match attributes
        for (MockIdentity candidate : context.getAllIdentities()) {
            boolean allExactMatch = true;
            boolean hasExactAttr = false;

            for (String attr : exactAttrs) {
                Object accountVal = accountAttrs.get(attr);
                Object identityVal = SafeAttributeUtils.getAttribute(candidate, attr);

                if (accountVal != null && identityVal != null) {
                    hasExactAttr = true;
                    if (!accountVal.toString().equalsIgnoreCase(identityVal.toString())) {
                        allExactMatch = false;
                        break;
                    }
                } else if (accountVal != null || identityVal != null) {
                    allExactMatch = false;
                    break;
                }
            }

            if (hasExactAttr && allExactMatch) {
                Map<String, Double> breakdown = new HashMap<>();
                for (String attr : exactAttrs) {
                    breakdown.put(attr, 1.0);
                }
                LoggingUtils.logAction(LOG, RULE_NAME, "correlation", "exactMatch",
                        "Exact match on " + exactAttrs + " to " + candidate.getName());
                return new FuzzyResult(candidate, 1.0, breakdown);
            }
        }

        // Second pass: fuzzy matching
        MockIdentity bestMatch = null;
        double bestScore = 0.0;
        Map<String, Double> bestBreakdown = Collections.emptyMap();

        for (MockIdentity candidate : context.getAllIdentities()) {
            Map<String, Double> breakdown = new HashMap<>();
            double totalScore = 0.0;
            int matchedAttrs = 0;

            for (String attr : fuzzyAttrs) {
                Object accountVal = accountAttrs.get(attr);
                Object identityVal = SafeAttributeUtils.getAttribute(candidate, attr);

                if (accountVal != null && identityVal != null) {
                    double similarity = computeSimilarity(
                            accountVal.toString().toLowerCase(),
                            identityVal.toString().toLowerCase());
                    breakdown.put(attr, similarity);
                    totalScore += similarity;
                    matchedAttrs++;
                }
            }

            double averageScore = matchedAttrs > 0 ? totalScore / matchedAttrs : 0.0;

            if (averageScore > bestScore) {
                bestScore = averageScore;
                bestMatch = candidate;
                bestBreakdown = breakdown;
            }
        }

        if (bestScore < minScore) {
            LoggingUtils.logAction(LOG, RULE_NAME, "correlation", "noMatch",
                    String.format("Best score %.2f below threshold %.2f", bestScore, minScore));
            return new FuzzyResult(null, bestScore, bestBreakdown);
        }

        LoggingUtils.logAction(LOG, RULE_NAME, "correlation", "fuzzyMatch",
                String.format("Matched to %s with score %.2f", bestMatch.getName(), bestScore));
        return new FuzzyResult(bestMatch, bestScore, bestBreakdown);
    }

    /**
     * Computes the Levenshtein edit distance between two strings.
     * This is the minimum number of single-character edits (insertions,
     * deletions, or substitutions) needed to transform one string into the other.
     */
    static int levenshteinDistance(String a, String b) {
        if (a == null || b == null) {
            return a == null && b == null ? 0 : Integer.MAX_VALUE;
        }

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[a.length()][b.length()];
    }

    /**
     * Computes a similarity score between 0.0 and 1.0 based on Levenshtein distance.
     * 1.0 means identical strings, 0.0 means completely different.
     */
    static double computeSimilarity(String a, String b) {
        if (a == null || b == null) {
            return 0.0;
        }
        if (a.equals(b)) {
            return 1.0;
        }
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 1.0;
        }
        int distance = levenshteinDistance(a, b);
        return 1.0 - ((double) distance / maxLen);
    }

    @SuppressWarnings("unchecked")
    private List<String> getFuzzyAttributes() {
        Object fuzzy = config.get("fuzzyMatch");
        if (fuzzy instanceof Map) {
            Object attrs = ((Map<String, Object>) fuzzy).get("attributes");
            if (attrs instanceof List) {
                return (List<String>) attrs;
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> getExactMatchAttributes() {
        Object fuzzy = config.get("fuzzyMatch");
        if (fuzzy instanceof Map) {
            Object attrs = ((Map<String, Object>) fuzzy).get("exactMatchAttributes");
            if (attrs instanceof List) {
                return (List<String>) attrs;
            }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private double getMinimumScore() {
        Object fuzzy = config.get("fuzzyMatch");
        if (fuzzy instanceof Map) {
            Object score = ((Map<String, Object>) fuzzy).get("minimumScore");
            if (score instanceof Number) {
                return ((Number) score).doubleValue();
            }
        }
        return 0.75;
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                FuzzyMatchCorrelation.class.getResourceAsStream("/correlation-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // --- Result container ---

    public static class FuzzyResult {
        private final MockIdentity identity;
        private final double score;
        private final Map<String, Double> attributeScores;

        public FuzzyResult(MockIdentity identity, double score, Map<String, Double> attributeScores) {
            this.identity = identity;
            this.score = score;
            this.attributeScores = attributeScores;
        }

        public MockIdentity getIdentity() { return identity; }
        public double getScore() { return score; }
        public Map<String, Double> getAttributeScores() { return attributeScores; }
        public boolean isMatch() { return identity != null; }
    }
}
