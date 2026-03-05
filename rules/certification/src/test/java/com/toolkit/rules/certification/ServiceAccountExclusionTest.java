package com.toolkit.rules.certification;

import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAccountExclusionTest {

    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        config = buildTestConfig();
    }

    @Test
    void testRegularUserNotExcluded() {
        ServiceAccountExclusion rule = new ServiceAccountExclusion(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("identityType", "employee");

        ServiceAccountExclusion.ExclusionResult result = rule.execute(identity);
        assertFalse(result.isExcluded());
        assertTrue(result.getReasons().isEmpty());
    }

    @Test
    void testServiceAccountByNamePattern() {
        ServiceAccountExclusion rule = new ServiceAccountExclusion(config);

        MockIdentity identity = new MockIdentity("svc_ldap")
                .setAttribute("identityType", "employee");

        ServiceAccountExclusion.ExclusionResult result = rule.execute(identity);
        assertTrue(result.isExcluded());
        assertTrue(result.getReasons().get(0).contains("pattern"));
    }

    @Test
    void testServiceAccountByPrefix() {
        ServiceAccountExclusion rule = new ServiceAccountExclusion(config);

        MockIdentity identity = new MockIdentity("SVC-backup");

        ServiceAccountExclusion.ExclusionResult result = rule.execute(identity);
        assertTrue(result.isExcluded());
    }

    @Test
    void testServiceAccountByType() {
        ServiceAccountExclusion rule = new ServiceAccountExclusion(config);

        MockIdentity identity = new MockIdentity("monitoring-bot")
                .setAttribute("identityType", "service");

        ServiceAccountExclusion.ExclusionResult result = rule.execute(identity);
        assertTrue(result.isExcluded());
        assertTrue(result.getReasons().stream()
                .anyMatch(r -> r.contains("service/system")));
    }

    @Test
    void testRPAAccountExcluded() {
        ServiceAccountExclusion rule = new ServiceAccountExclusion(config);

        MockIdentity identity = new MockIdentity("invoice-processor")
                .setAttribute("identityType", "rpa");

        ServiceAccountExclusion.ExclusionResult result = rule.execute(identity);
        assertTrue(result.isExcluded());
    }

    @Test
    void testInactiveIdentityExcluded() {
        ServiceAccountExclusion rule = new ServiceAccountExclusion(config);

        MockIdentity identity = new MockIdentity("jdoe")
                .setInactive(true);

        ServiceAccountExclusion.ExclusionResult result = rule.execute(identity);
        assertTrue(result.isExcluded());
        assertTrue(result.getReasons().stream()
                .anyMatch(r -> r.contains("inactive")));
    }

    @Test
    void testAllDisabledAccountsExcluded() {
        ServiceAccountExclusion rule = new ServiceAccountExclusion(config);

        MockIdentity identity = new MockIdentity("former_employee");
        identity.addLink(new MockLink("Active Directory", "jdoe").setDisabled(true));
        identity.addLink(new MockLink("LDAP", "jdoe").setDisabled(true));

        ServiceAccountExclusion.ExclusionResult result = rule.execute(identity);
        assertTrue(result.isExcluded());
        assertTrue(result.getReasons().stream()
                .anyMatch(r -> r.contains("disabled")));
    }

    @Test
    void testMixedAccountsNotExcluded() {
        ServiceAccountExclusion rule = new ServiceAccountExclusion(config);

        MockIdentity identity = new MockIdentity("jdoe");
        identity.addLink(new MockLink("Active Directory", "jdoe").setDisabled(false));
        identity.addLink(new MockLink("LDAP", "jdoe").setDisabled(true));

        ServiceAccountExclusion.ExclusionResult result = rule.execute(identity);
        assertFalse(result.isExcluded());
    }

    @Test
    void testNoAccountsNotExcluded() {
        ServiceAccountExclusion rule = new ServiceAccountExclusion(config);

        MockIdentity identity = new MockIdentity("newuser");

        ServiceAccountExclusion.ExclusionResult result = rule.execute(identity);
        assertFalse(result.isExcluded());
    }

    @Test
    void testMultipleExclusionReasons() {
        ServiceAccountExclusion rule = new ServiceAccountExclusion(config);

        MockIdentity identity = new MockIdentity("svc_monitoring")
                .setAttribute("identityType", "service")
                .setInactive(true);

        ServiceAccountExclusion.ExclusionResult result = rule.execute(identity);
        assertTrue(result.isExcluded());
        assertTrue(result.getReasons().size() >= 2);
    }

    @Test
    void testWildcardPatternMatching() {
        ServiceAccountExclusion rule = new ServiceAccountExclusion(config);
        assertTrue(rule.matchesServiceAccountPattern("svc_ldap"));
        assertTrue(rule.matchesServiceAccountPattern("SVC-backup"));
        assertTrue(rule.matchesServiceAccountPattern("system_cron"));
        assertTrue(rule.matchesServiceAccountPattern("admin_portal"));
        assertFalse(rule.matchesServiceAccountPattern("jdoe"));
        assertFalse(rule.matchesServiceAccountPattern("alice.smith"));
    }

    private Map<String, Object> buildTestConfig() {
        Map<String, Object> cfg = new HashMap<>();

        Map<String, Object> excl = new HashMap<>();
        excl.put("serviceAccountPatterns", Arrays.asList("svc_*", "SVC-*", "system_*", "admin_*"));
        excl.put("serviceAccountAttribute", "identityType");
        excl.put("serviceAccountValues", Arrays.asList("service", "system", "rpa"));
        excl.put("excludeInactive", true);
        excl.put("excludeDisabledAccounts", true);

        cfg.put("serviceAccountExclusion", excl);
        return cfg;
    }
}
