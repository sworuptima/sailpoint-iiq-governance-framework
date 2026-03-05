package com.toolkit.rules.policy;

import com.toolkit.mock.MockBundle;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Identifies identities with significantly more access than their peers.
 *
 * Entitlement creep is the gradual accumulation of permissions over time.
 * When someone transfers departments, they often keep old access while
 * gaining new access. After a few moves, they have far more entitlements
 * than anyone else in their current role.
 *
 * This rule groups identities by a configurable peer attribute (typically
 * department), computes the average number of non-global roles per group,
 * and flags identities whose role count exceeds the peer average by a
 * configurable multiplier.
 *
 * Configuration keys:
 *   entitlementCreep.peerGroupAttribute        - attribute to group peers by
 *   entitlementCreep.creepThresholdMultiplier   - how many times above average triggers detection
 *   entitlementCreep.minimumPeerGroupSize       - minimum peers needed for a valid comparison
 *   entitlementCreep.excludeGlobalRoles         - whether to exclude global roles from counting
 *   entitlementCreep.globalRoles                - list of global role names to exclude
 */
public class EntitlementCreepDetector {

    private static final String RULE_NAME = "EntitlementCreepDetector";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public EntitlementCreepDetector(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public EntitlementCreepDetector() {
        this(loadDefaultConfig());
    }

    /**
     * Scans all identities in the context for entitlement creep.
     *
     * @param context the SailPoint context with identities to evaluate
     * @return a list of CreepResult for identities exceeding the threshold
     */
    public List<CreepResult> execute(MockSailPointContext context) {
        LoggingUtils.logAction(LOG, RULE_NAME, "scan", "start",
                "Scanning " + context.getAllIdentities().size() + " identities");

        String peerAttr = getPeerGroupAttribute();
        double multiplier = getCreepThresholdMultiplier();
        int minGroupSize = getMinimumPeerGroupSize();
        Set<String> globalRoles = getGlobalRoles();
        boolean excludeGlobal = getExcludeGlobalRoles();

        // Group identities by peer attribute
        Map<String, List<MockIdentity>> peerGroups = new HashMap<>();
        for (MockIdentity identity : context.getAllIdentities()) {
            String groupKey = SafeAttributeUtils.getStringAttribute(identity, peerAttr);
            if (groupKey == null) {
                groupKey = "Ungrouped";
            }
            peerGroups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(identity);
        }

        // Compute averages and detect creep
        List<CreepResult> results = new ArrayList<>();

        for (Map.Entry<String, List<MockIdentity>> entry : peerGroups.entrySet()) {
            String groupName = entry.getKey();
            List<MockIdentity> peers = entry.getValue();

            if (peers.size() < minGroupSize) {
                LoggingUtils.logAction(LOG, RULE_NAME, groupName, "skipGroup",
                        "Only " + peers.size() + " peers, minimum is " + minGroupSize);
                continue;
            }

            // Compute role counts
            Map<MockIdentity, Integer> roleCounts = new HashMap<>();
            int totalRoles = 0;

            for (MockIdentity peer : peers) {
                int count = countRoles(peer, excludeGlobal, globalRoles);
                roleCounts.put(peer, count);
                totalRoles += count;
            }

            double average = (double) totalRoles / peers.size();
            double threshold = average * multiplier;

            for (MockIdentity peer : peers) {
                int count = roleCounts.get(peer);
                if (count > threshold && count > average) {
                    CreepResult result = new CreepResult(
                            peer.getName(), groupName, count, average, threshold);
                    results.add(result);

                    LoggingUtils.logWarning(LOG, RULE_NAME, peer.getName(),
                            "creepDetected",
                            String.format("Roles: %d, avg: %.1f, threshold: %.1f",
                                    count, average, threshold));
                }
            }
        }

        LoggingUtils.logAction(LOG, RULE_NAME, "scan", "complete",
                results.size() + " identities flagged for entitlement creep");
        return results;
    }

    /**
     * Evaluates a single identity against its peer group.
     *
     * @param context  the context containing all identities
     * @param identity the identity to evaluate
     * @return a CreepResult if creep is detected, null otherwise
     */
    public CreepResult evaluateIdentity(MockSailPointContext context, MockIdentity identity) {
        String peerAttr = getPeerGroupAttribute();
        double multiplier = getCreepThresholdMultiplier();
        int minGroupSize = getMinimumPeerGroupSize();
        Set<String> globalRoles = getGlobalRoles();
        boolean excludeGlobal = getExcludeGlobalRoles();

        String groupKey = SafeAttributeUtils.getStringAttribute(identity, peerAttr);
        if (groupKey == null) {
            return null;
        }

        // Find peers
        List<MockIdentity> peers = new ArrayList<>();
        for (MockIdentity candidate : context.getAllIdentities()) {
            String candidateGroup = SafeAttributeUtils.getStringAttribute(candidate, peerAttr);
            if (groupKey.equals(candidateGroup)) {
                peers.add(candidate);
            }
        }

        if (peers.size() < minGroupSize) {
            return null;
        }

        // Compute average
        int totalRoles = 0;
        for (MockIdentity peer : peers) {
            totalRoles += countRoles(peer, excludeGlobal, globalRoles);
        }

        double average = (double) totalRoles / peers.size();
        double threshold = average * multiplier;
        int identityCount = countRoles(identity, excludeGlobal, globalRoles);

        if (identityCount > threshold && identityCount > average) {
            return new CreepResult(identity.getName(), groupKey,
                    identityCount, average, threshold);
        }

        return null;
    }

    int countRoles(MockIdentity identity, boolean excludeGlobal, Set<String> globalRoles) {
        int count = 0;
        for (MockBundle bundle : identity.getBundles()) {
            if (excludeGlobal && globalRoles.contains(bundle.getName())) {
                continue;
            }
            count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private String getPeerGroupAttribute() {
        Object creep = config.get("entitlementCreep");
        if (creep instanceof Map) {
            Object attr = ((Map<String, Object>) creep).get("peerGroupAttribute");
            if (attr instanceof String) {
                return (String) attr;
            }
        }
        return "department";
    }

    @SuppressWarnings("unchecked")
    private double getCreepThresholdMultiplier() {
        Object creep = config.get("entitlementCreep");
        if (creep instanceof Map) {
            Object mult = ((Map<String, Object>) creep).get("creepThresholdMultiplier");
            if (mult instanceof Number) {
                return ((Number) mult).doubleValue();
            }
        }
        return 1.5;
    }

    @SuppressWarnings("unchecked")
    private int getMinimumPeerGroupSize() {
        Object creep = config.get("entitlementCreep");
        if (creep instanceof Map) {
            Object size = ((Map<String, Object>) creep).get("minimumPeerGroupSize");
            if (size instanceof Number) {
                return ((Number) size).intValue();
            }
        }
        return 3;
    }

    @SuppressWarnings("unchecked")
    private boolean getExcludeGlobalRoles() {
        Object creep = config.get("entitlementCreep");
        if (creep instanceof Map) {
            Object exclude = ((Map<String, Object>) creep).get("excludeGlobalRoles");
            if (exclude instanceof Boolean) {
                return (Boolean) exclude;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getGlobalRoles() {
        Object creep = config.get("entitlementCreep");
        if (creep instanceof Map) {
            Object roles = ((Map<String, Object>) creep).get("globalRoles");
            if (roles instanceof List) {
                return new HashSet<>((List<String>) roles);
            }
        }
        return Collections.emptySet();
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                EntitlementCreepDetector.class.getResourceAsStream("/policy-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // --- Result container ---

    public static class CreepResult {
        private final String identityName;
        private final String peerGroup;
        private final int roleCount;
        private final double peerAverage;
        private final double threshold;

        public CreepResult(String identityName, String peerGroup,
                           int roleCount, double peerAverage, double threshold) {
            this.identityName = identityName;
            this.peerGroup = peerGroup;
            this.roleCount = roleCount;
            this.peerAverage = peerAverage;
            this.threshold = threshold;
        }

        public String getIdentityName() { return identityName; }
        public String getPeerGroup() { return peerGroup; }
        public int getRoleCount() { return roleCount; }
        public double getPeerAverage() { return peerAverage; }
        public double getThreshold() { return threshold; }

        @Override
        public String toString() {
            return String.format("%s: %d roles (peer avg: %.1f, threshold: %.1f) in %s",
                    identityName, roleCount, peerAverage, threshold, peerGroup);
        }
    }
}
