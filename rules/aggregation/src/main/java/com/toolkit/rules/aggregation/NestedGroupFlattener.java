package com.toolkit.rules.aggregation;

import com.toolkit.utils.LoggingUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Flattens nested Active Directory group memberships into a single list.
 *
 * In Active Directory, group membership is hierarchical. A user might be a
 * direct member of "Engineering-Dev" which is nested inside "Engineering"
 * which is nested inside "All-Employees". IIQ sees only the direct membership
 * during aggregation unless a rule walks the group hierarchy.
 *
 * This rule takes a group hierarchy map and an account's direct group list,
 * then resolves all parent groups recursively up to a configurable depth
 * limit to prevent infinite loops in circular group structures.
 *
 * Configuration keys:
 *   nestedGroupConfig.maxDepth           - maximum recursion depth (default 5)
 *   nestedGroupConfig.includeParentGroups - whether to include parent groups in the result
 *   nestedGroupConfig.groupAttribute     - the attribute name holding group memberships
 */
public class NestedGroupFlattener {

    private static final String RULE_NAME = "NestedGroupFlattener";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public NestedGroupFlattener(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public NestedGroupFlattener() {
        this(loadDefaultConfig());
    }

    /**
     * Flattens a set of direct group memberships by resolving parent groups
     * from the provided hierarchy map.
     *
     * @param directGroups   the groups the account is a direct member of
     * @param groupHierarchy a map of child group -> list of parent groups
     * @return a deduplicated list of all groups (direct + resolved parents)
     */
    public FlattenResult execute(List<String> directGroups, Map<String, List<String>> groupHierarchy) {
        if (directGroups == null || directGroups.isEmpty()) {
            return new FlattenResult(Collections.emptyList(), 0, 0);
        }

        int maxDepth = getMaxDepth();
        boolean includeParents = getIncludeParentGroups();

        Set<String> allGroups = new HashSet<>(directGroups);
        int resolvedCount = 0;

        if (includeParents && groupHierarchy != null) {
            for (String group : directGroups) {
                Set<String> visited = new HashSet<>();
                visited.add(group);
                resolvedCount += resolveParents(group, groupHierarchy, allGroups, visited, 0, maxDepth);
            }
        }

        List<String> result = new ArrayList<>(allGroups);
        Collections.sort(result);

        LoggingUtils.logAction(LOG, RULE_NAME, "aggregation", "flatten",
                "Direct: " + directGroups.size() + ", resolved: " + resolvedCount +
                        ", total: " + result.size());

        return new FlattenResult(result, directGroups.size(), resolvedCount);
    }

    /**
     * Convenience method that accepts a delimited string of groups instead of a list.
     *
     * @param groupString    delimited string of group names
     * @param delimiter      the delimiter (e.g., ";", ",")
     * @param groupHierarchy parent group mappings
     * @return flattened result
     */
    public FlattenResult execute(String groupString, String delimiter,
                                  Map<String, List<String>> groupHierarchy) {
        if (groupString == null || groupString.trim().isEmpty()) {
            return new FlattenResult(Collections.emptyList(), 0, 0);
        }

        String delim = delimiter != null ? delimiter : getConfiguredDelimiter();
        String[] parts = groupString.split("\\s*" + escapeRegex(delim) + "\\s*");
        List<String> groups = new ArrayList<>(Arrays.asList(parts));
        groups.removeIf(String::isEmpty);

        return execute(groups, groupHierarchy);
    }

    /**
     * Recursively resolves parent groups up to the configured depth limit.
     */
    private int resolveParents(String group, Map<String, List<String>> hierarchy,
                               Set<String> allGroups, Set<String> visited,
                               int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth) {
            LoggingUtils.logWarning(LOG, RULE_NAME, "aggregation", "maxDepthReached",
                    "Stopping at depth " + maxDepth + " for group: " + group);
            return 0;
        }

        List<String> parents = hierarchy.get(group);
        if (parents == null || parents.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (String parent : parents) {
            if (!visited.contains(parent)) {
                visited.add(parent);
                allGroups.add(parent);
                count++;
                count += resolveParents(parent, hierarchy, allGroups, visited,
                        currentDepth + 1, maxDepth);
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private int getMaxDepth() {
        Object nested = config.get("nestedGroupConfig");
        if (nested instanceof Map) {
            Object depth = ((Map<String, Object>) nested).get("maxDepth");
            if (depth instanceof Number) {
                return ((Number) depth).intValue();
            }
        }
        return 5;
    }

    @SuppressWarnings("unchecked")
    private boolean getIncludeParentGroups() {
        Object nested = config.get("nestedGroupConfig");
        if (nested instanceof Map) {
            Object include = ((Map<String, Object>) nested).get("includeParentGroups");
            if (include instanceof Boolean) {
                return (Boolean) include;
            }
        }
        return true;
    }

    private String getConfiguredDelimiter() {
        Object delim = config.get("groupDelimiter");
        return delim instanceof String ? (String) delim : ";";
    }

    private String escapeRegex(String literal) {
        return literal.replaceAll("([\\\\\\[\\]{}()*+?.^$|])", "\\\\$1");
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                NestedGroupFlattener.class.getResourceAsStream("/aggregation-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // --- Result container ---

    public static class FlattenResult {
        private final List<String> allGroups;
        private final int directCount;
        private final int resolvedCount;

        public FlattenResult(List<String> allGroups, int directCount, int resolvedCount) {
            this.allGroups = allGroups;
            this.directCount = directCount;
            this.resolvedCount = resolvedCount;
        }

        public List<String> getAllGroups() { return allGroups; }
        public int getDirectCount() { return directCount; }
        public int getResolvedCount() { return resolvedCount; }
        public int getTotalCount() { return allGroups.size(); }
    }
}
