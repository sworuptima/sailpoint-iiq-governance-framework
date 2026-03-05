package com.toolkit.rules.policy;

import com.toolkit.mock.MockBundle;
import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockSailPointContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntitlementCreepDetectorTest {

    private Map<String, Object> config;
    private MockSailPointContext context;

    @BeforeEach
    void setUp() {
        config = buildTestConfig();
        context = new MockSailPointContext();

        // Engineering team: 4 people, each with 2 non-global roles on average
        context.addIdentity(new MockIdentity("eng1")
                .setAttribute("department", "Engineering")
                .addBundle(new MockBundle("Base Access"))
                .addBundle(new MockBundle("Engineering Tools"))
                .addBundle(new MockBundle("GitHub Access")));

        context.addIdentity(new MockIdentity("eng2")
                .setAttribute("department", "Engineering")
                .addBundle(new MockBundle("Base Access"))
                .addBundle(new MockBundle("Engineering Tools")));

        context.addIdentity(new MockIdentity("eng3")
                .setAttribute("department", "Engineering")
                .addBundle(new MockBundle("Base Access"))
                .addBundle(new MockBundle("Engineering Tools"))
                .addBundle(new MockBundle("GitHub Access")));

        // This engineer has way more roles than peers (creep!)
        context.addIdentity(new MockIdentity("eng-creep")
                .setAttribute("department", "Engineering")
                .addBundle(new MockBundle("Base Access"))
                .addBundle(new MockBundle("Engineering Tools"))
                .addBundle(new MockBundle("GitHub Access"))
                .addBundle(new MockBundle("CRM Access"))
                .addBundle(new MockBundle("Sales Tools"))
                .addBundle(new MockBundle("Finance Systems"))
                .addBundle(new MockBundle("HR Systems")));
    }

    @Test
    void testDetectsCreep() {
        EntitlementCreepDetector rule = new EntitlementCreepDetector(config);
        List<EntitlementCreepDetector.CreepResult> results = rule.execute(context);

        assertFalse(results.isEmpty());
        assertTrue(results.stream()
                .anyMatch(r -> "eng-creep".equals(r.getIdentityName())));
    }

    @Test
    void testNormalUsersNotFlagged() {
        EntitlementCreepDetector rule = new EntitlementCreepDetector(config);
        List<EntitlementCreepDetector.CreepResult> results = rule.execute(context);

        assertFalse(results.stream()
                .anyMatch(r -> "eng1".equals(r.getIdentityName())));
        assertFalse(results.stream()
                .anyMatch(r -> "eng2".equals(r.getIdentityName())));
    }

    @Test
    void testCreepResultContainsPeerInfo() {
        EntitlementCreepDetector rule = new EntitlementCreepDetector(config);
        List<EntitlementCreepDetector.CreepResult> results = rule.execute(context);

        EntitlementCreepDetector.CreepResult creepResult = results.stream()
                .filter(r -> "eng-creep".equals(r.getIdentityName()))
                .findFirst().orElse(null);

        assertNotNull(creepResult);
        assertEquals("Engineering", creepResult.getPeerGroup());
        assertTrue(creepResult.getRoleCount() > creepResult.getPeerAverage());
        assertTrue(creepResult.getRoleCount() > creepResult.getThreshold());
    }

    @Test
    void testGlobalRolesExcluded() {
        EntitlementCreepDetector rule = new EntitlementCreepDetector(config);
        Set<String> globalRoles = new HashSet<>(Arrays.asList("Base Access", "Email", "Company Intranet"));

        MockIdentity identity = new MockIdentity("test")
                .addBundle(new MockBundle("Base Access"))
                .addBundle(new MockBundle("Email"))
                .addBundle(new MockBundle("Engineering Tools"));

        int count = rule.countRoles(identity, true, globalRoles);
        assertEquals(1, count); // Only Engineering Tools should count
    }

    @Test
    void testSmallPeerGroupSkipped() {
        MockSailPointContext smallContext = new MockSailPointContext();
        // Only 2 people in the group (below minimum of 3)
        smallContext.addIdentity(new MockIdentity("solo1")
                .setAttribute("department", "Research")
                .addBundle(new MockBundle("Base Access"))
                .addBundle(new MockBundle("Research Tools")));

        smallContext.addIdentity(new MockIdentity("solo2")
                .setAttribute("department", "Research")
                .addBundle(new MockBundle("Base Access")));

        EntitlementCreepDetector rule = new EntitlementCreepDetector(config);
        List<EntitlementCreepDetector.CreepResult> results = rule.execute(smallContext);

        assertTrue(results.isEmpty());
    }

    @Test
    void testEvaluateSingleIdentity() {
        EntitlementCreepDetector rule = new EntitlementCreepDetector(config);

        MockIdentity creepyUser = context.getIdentityByName("eng-creep");
        EntitlementCreepDetector.CreepResult result =
                rule.evaluateIdentity(context, creepyUser);

        assertNotNull(result);
        assertEquals("eng-creep", result.getIdentityName());
    }

    @Test
    void testEvaluateNormalIdentity() {
        EntitlementCreepDetector rule = new EntitlementCreepDetector(config);

        MockIdentity normalUser = context.getIdentityByName("eng1");
        EntitlementCreepDetector.CreepResult result =
                rule.evaluateIdentity(context, normalUser);

        assertNull(result);
    }

    @Test
    void testNoDepartmentAttribute() {
        EntitlementCreepDetector rule = new EntitlementCreepDetector(config);

        MockIdentity noDept = new MockIdentity("nodept")
                .addBundle(new MockBundle("Everything"));

        EntitlementCreepDetector.CreepResult result =
                rule.evaluateIdentity(context, noDept);

        assertNull(result); // No department means no peer group
    }

    @Test
    void testEmptyContext() {
        EntitlementCreepDetector rule = new EntitlementCreepDetector(config);
        MockSailPointContext emptyCtx = new MockSailPointContext();

        List<EntitlementCreepDetector.CreepResult> results = rule.execute(emptyCtx);
        assertTrue(results.isEmpty());
    }

    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        Map<String, Object> creep = new HashMap<>();
        creep.put("peerGroupAttribute", "department");
        creep.put("creepThresholdMultiplier", 1.5);
        creep.put("minimumPeerGroupSize", 3);
        creep.put("excludeGlobalRoles", true);
        creep.put("globalRoles", Arrays.asList("Base Access", "Email", "Company Intranet"));

        cfg.put("entitlementCreep", creep);
        return cfg;
    }
}
