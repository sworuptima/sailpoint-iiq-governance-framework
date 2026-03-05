package com.toolkit.rules.policy;

import com.toolkit.mock.MockBundle;
import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockSailPointContext;
import com.toolkit.utils.LoggingUtils;
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
 * Evaluates role combinations against a configurable conflict matrix to
 * detect Separation of Duties violations.
 *
 * SoD is a core control in identity governance. In financial services,
 * the person who initiates a wire transfer cannot also approve it. In IT,
 * a database administrator should not also have application deployment
 * rights. These combinations create fraud and error risk.
 *
 * This rule checks every identity in the context (or a single identity)
 * against a conflict matrix. It categorizes violations by severity level
 * and produces a report suitable for compliance dashboards.
 *
 * Configuration keys:
 *   sodViolation.conflictMatrix  - role name -> list of conflicting role names
 *   sodViolation.severityLevels  - severity -> list of conflicting role pairs
 */
public class SoDViolationDetector {

    private static final String RULE_NAME = "SoDViolationDetector";
    private static final Logger LOG = LoggingUtils.getLogger(RULE_NAME);

    private final Map<String, Object> config;

    public SoDViolationDetector(Map<String, Object> config) {
        this.config = config != null ? config : Collections.emptyMap();
    }

    public SoDViolationDetector() {
        this(loadDefaultConfig());
    }

    /**
     * Checks a single identity for SoD violations.
     *
     * @param identity the identity to evaluate
     * @return a ViolationReport containing all detected violations
     */
    public ViolationReport execute(MockIdentity identity) {
        LoggingUtils.logRuleEntry(LOG, RULE_NAME, identity.getName());

        Map<String, List<String>> conflictMatrix = getConflictMatrix();
        Map<String, List<List<String>>> severityMap = getSeverityLevels();

        Set<String> roles = new HashSet<>();
        for (MockBundle bundle : identity.getBundles()) {
            roles.add(bundle.getName());
        }

        List<Violation> violations = new ArrayList<>();
        Set<String> checkedPairs = new HashSet<>();

        List<String> roleList = new ArrayList<>(roles);
        for (int i = 0; i < roleList.size(); i++) {
            String roleA = roleList.get(i);

            for (int j = i + 1; j < roleList.size(); j++) {
                String roleB = roleList.get(j);

                // Avoid duplicate pairs
                String pairKey = roleA.compareTo(roleB) < 0
                        ? roleA + "|" + roleB : roleB + "|" + roleA;
                if (checkedPairs.contains(pairKey)) {
                    continue;
                }

                // Check both directions in the conflict matrix
                List<String> conflictsA = conflictMatrix.get(roleA);
                List<String> conflictsB = conflictMatrix.get(roleB);
                boolean isConflict = (conflictsA != null && conflictsA.contains(roleB))
                        || (conflictsB != null && conflictsB.contains(roleA));

                if (isConflict) {
                    checkedPairs.add(pairKey);
                    String severity = determineSeverity(roleA, roleB, severityMap);
                    violations.add(new Violation(roleA, roleB, severity));
                    LoggingUtils.logWarning(LOG, RULE_NAME, identity.getName(),
                            "violation", roleA + " <-> " + roleB + " [" + severity + "]");
                }
            }
        }

        LoggingUtils.logRuleExit(LOG, RULE_NAME, identity.getName(),
                violations.size() + " violations detected");
        return new ViolationReport(identity.getName(), violations);
    }

    /**
     * Scans all identities in the context for SoD violations.
     *
     * @param context the SailPoint context containing identities to scan
     * @return a map of identity name -> ViolationReport
     */
    public Map<String, ViolationReport> scanAll(MockSailPointContext context) {
        Map<String, ViolationReport> results = new HashMap<>();
        for (MockIdentity identity : context.getAllIdentities()) {
            ViolationReport report = execute(identity);
            if (!report.getViolations().isEmpty()) {
                results.put(identity.getName(), report);
            }
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private String determineSeverity(String roleA, String roleB,
                                      Map<String, List<List<String>>> severityMap) {
        for (Map.Entry<String, List<List<String>>> entry : severityMap.entrySet()) {
            for (List<String> pair : entry.getValue()) {
                if (pair.size() == 2) {
                    if ((pair.get(0).equals(roleA) && pair.get(1).equals(roleB)) ||
                            (pair.get(0).equals(roleB) && pair.get(1).equals(roleA))) {
                        return entry.getKey();
                    }
                }
            }
        }
        return "medium";
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> getConflictMatrix() {
        Object sod = config.get("sodViolation");
        if (sod instanceof Map) {
            Object matrix = ((Map<String, Object>) sod).get("conflictMatrix");
            if (matrix instanceof Map) {
                return (Map<String, List<String>>) matrix;
            }
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<List<String>>> getSeverityLevels() {
        Object sod = config.get("sodViolation");
        if (sod instanceof Map) {
            Object levels = ((Map<String, Object>) sod).get("severityLevels");
            if (levels instanceof Map) {
                return (Map<String, List<List<String>>>) levels;
            }
        }
        return Collections.emptyMap();
    }

    private static Map<String, Object> loadDefaultConfig() {
        try (Reader reader = new InputStreamReader(
                SoDViolationDetector.class.getResourceAsStream("/policy-config.json"),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return new Gson().fromJson(reader, type);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    // --- Result containers ---

    public static class Violation {
        private final String roleA;
        private final String roleB;
        private final String severity;

        public Violation(String roleA, String roleB, String severity) {
            this.roleA = roleA;
            this.roleB = roleB;
            this.severity = severity;
        }

        public String getRoleA() { return roleA; }
        public String getRoleB() { return roleB; }
        public String getSeverity() { return severity; }

        @Override
        public String toString() {
            return roleA + " <-> " + roleB + " [" + severity + "]";
        }
    }

    public static class ViolationReport {
        private final String identityName;
        private final List<Violation> violations;

        public ViolationReport(String identityName, List<Violation> violations) {
            this.identityName = identityName;
            this.violations = violations;
        }

        public String getIdentityName() { return identityName; }
        public List<Violation> getViolations() { return violations; }
        public boolean hasViolations() { return !violations.isEmpty(); }
        public int getViolationCount() { return violations.size(); }

        public List<Violation> getViolationsBySeverity(String severity) {
            List<Violation> filtered = new ArrayList<>();
            for (Violation v : violations) {
                if (severity.equals(v.getSeverity())) {
                    filtered.add(v);
                }
            }
            return filtered;
        }
    }
}
