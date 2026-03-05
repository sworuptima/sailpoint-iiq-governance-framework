package com.toolkit.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockContextTest {

    private MockSailPointContext context;

    @BeforeEach
    void setUp() {
        context = new MockSailPointContext();
    }

    @Test
    void testAddAndRetrieveIdentityByName() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setFirstname("John")
                .setLastname("Doe");

        context.addIdentity(identity);

        MockIdentity retrieved = context.getIdentityByName("jdoe");
        assertNotNull(retrieved);
        assertEquals("John", retrieved.getFirstname());
        assertEquals("Doe", retrieved.getLastname());
    }

    @Test
    void testAddAndRetrieveIdentityById() {
        MockIdentity identity = new MockIdentity("jdoe");
        context.addIdentity(identity);

        String id = identity.getId();
        assertNotNull(id);

        MockIdentity retrieved = context.getIdentityById(id);
        assertNotNull(retrieved);
        assertEquals("jdoe", retrieved.getName());
    }

    @Test
    void testIdentityAttributes() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("department", "Engineering")
                .setAttribute("title", "Senior Developer")
                .setEmail("jdoe@company.com");

        assertEquals("Engineering", identity.getAttribute("department"));
        assertEquals("Senior Developer", identity.getAttribute("title"));
        assertEquals("jdoe@company.com", identity.getAttribute("email"));
        assertEquals("Engineering", identity.getDepartment());
        assertEquals("Senior Developer", identity.getTitle());
    }

    @Test
    void testIdentityStringAttribute() {
        MockIdentity identity = new MockIdentity("jdoe")
                .setAttribute("employeeId", 12345);

        assertEquals("12345", identity.getStringAttribute("employeeId"));
        assertNull(identity.getStringAttribute("nonexistent"));
    }

    @Test
    void testAddAndRemoveLinks() {
        MockIdentity identity = new MockIdentity("jdoe");
        MockLink adLink = new MockLink("Active Directory", "CN=jdoe,OU=Users,DC=corp");
        MockLink ldapLink = new MockLink("Corporate LDAP", "uid=jdoe,ou=people,dc=corp");

        identity.addLink(adLink);
        identity.addLink(ldapLink);

        assertEquals(2, identity.getLinks().size());
        assertNotNull(identity.getLink("Active Directory"));
        assertEquals("CN=jdoe,OU=Users,DC=corp", identity.getLink("Active Directory").getNativeIdentity());

        identity.removeLink(adLink);
        assertEquals(1, identity.getLinks().size());
        assertNull(identity.getLink("Active Directory"));
    }

    @Test
    void testLinkAttributes() {
        MockLink link = new MockLink("Active Directory", "CN=jdoe,OU=Users,DC=corp")
                .setAttribute("memberOf", "CN=Developers,OU=Groups,DC=corp")
                .setAttribute("accountDisabled", false);

        assertEquals("CN=Developers,OU=Groups,DC=corp", link.getAttribute("memberOf"));
        assertEquals(false, link.getAttribute("accountDisabled"));
    }

    @Test
    void testAddAndRemoveBundles() {
        MockIdentity identity = new MockIdentity("jdoe");
        MockBundle baseAccess = new MockBundle("Base Access");
        MockBundle engTools = new MockBundle("Engineering Tools");

        identity.addBundle(baseAccess);
        identity.addBundle(engTools);

        assertEquals(2, identity.getBundles().size());
        assertTrue(identity.hasBundle("Base Access"));
        assertTrue(identity.hasBundle("Engineering Tools"));
        assertFalse(identity.hasBundle("Finance Systems"));

        identity.removeBundle(baseAccess);
        assertEquals(1, identity.getBundles().size());
        assertFalse(identity.hasBundle("Base Access"));
    }

    @Test
    void testDuplicateBundleNotAdded() {
        MockIdentity identity = new MockIdentity("jdoe");
        MockBundle baseAccess = new MockBundle("Base Access");

        identity.addBundle(baseAccess);
        identity.addBundle(baseAccess);

        assertEquals(1, identity.getBundles().size());
    }

    @Test
    void testBundleWithRequirements() {
        MockBundle managerDashboard = new MockBundle("Manager Dashboard");
        MockBundle baseAccess = new MockBundle("Base Access");

        managerDashboard.addRequirement(baseAccess);

        assertEquals(1, managerDashboard.getRequirements().size());
        assertEquals("Base Access", managerDashboard.getRequirements().get(0).getName());
    }

    @Test
    void testProvisioningPlanConstruction() {
        MockIdentity identity = new MockIdentity("jdoe");

        MockProvisioningPlan plan = new MockProvisioningPlan(identity);

        MockProvisioningPlan.AccountRequest acctReq =
                new MockProvisioningPlan.AccountRequest("Active Directory", MockProvisioningPlan.Operation.Create);
        acctReq.setNativeIdentity("CN=jdoe,OU=Users,DC=corp");
        acctReq.add(new MockProvisioningPlan.AttributeRequest(
                "memberOf", MockProvisioningPlan.Operation.Add, "CN=Developers,OU=Groups,DC=corp"));
        acctReq.add(new MockProvisioningPlan.AttributeRequest(
                "homeDirectory", MockProvisioningPlan.Operation.Set, "/home/jdoe"));

        plan.addAccountRequest(acctReq);

        assertFalse(plan.isEmpty());
        assertEquals(1, plan.getAccountRequests().size());

        MockProvisioningPlan.AccountRequest retrieved = plan.getAccountRequests().get(0);
        assertEquals("Active Directory", retrieved.getApplicationName());
        assertEquals(MockProvisioningPlan.Operation.Create, retrieved.getOperation());
        assertEquals(2, retrieved.getAttributeRequests().size());

        MockProvisioningPlan.AttributeRequest memberOfReq = retrieved.getAttributeRequests().get(0);
        assertEquals("memberOf", memberOfReq.getName());
        assertEquals(MockProvisioningPlan.Operation.Add, memberOfReq.getOperation());
    }

    @Test
    void testProvisioningPlanSummary() {
        MockIdentity identity = new MockIdentity("jdoe");
        MockProvisioningPlan plan = new MockProvisioningPlan(identity);

        MockProvisioningPlan.AccountRequest acctReq =
                new MockProvisioningPlan.AccountRequest("Active Directory", MockProvisioningPlan.Operation.Modify);
        acctReq.add(new MockProvisioningPlan.AttributeRequest(
                "assignedRoles", MockProvisioningPlan.Operation.Add, "Engineering Tools"));
        plan.addAccountRequest(acctReq);

        String summary = plan.toSummaryString();
        assertTrue(summary.contains("jdoe"));
        assertTrue(summary.contains("Active Directory"));
        assertTrue(summary.contains("Engineering Tools"));
    }

    @Test
    void testContextSearchIdentities() {
        context.addIdentity(new MockIdentity("jdoe")
                .setAttribute("department", "Engineering"));
        context.addIdentity(new MockIdentity("asmith")
                .setAttribute("department", "Sales"));
        context.addIdentity(new MockIdentity("bwilson")
                .setAttribute("department", "Engineering"));

        Map<String, Object> filter = new HashMap<>();
        filter.put("department", "Engineering");

        List<MockIdentity> results = context.searchIdentities(filter);
        assertEquals(2, results.size());
    }

    @Test
    void testContextGenericObjectAccess() {
        MockIdentity identity = new MockIdentity("jdoe");
        MockBundle bundle = new MockBundle("Base Access");

        context.addIdentity(identity);
        context.addBundle(bundle);

        MockIdentity retrievedIdentity = context.getObjectByName(MockIdentity.class, "jdoe");
        MockBundle retrievedBundle = context.getObjectByName(MockBundle.class, "Base Access");

        assertNotNull(retrievedIdentity);
        assertNotNull(retrievedBundle);
        assertEquals("jdoe", retrievedIdentity.getName());
        assertEquals("Base Access", retrievedBundle.getName());
    }

    @Test
    void testContextSaveAndRemoveObject() {
        MockIdentity identity = new MockIdentity("jdoe");

        context.saveObject(identity);
        assertNotNull(context.getIdentityByName("jdoe"));

        context.removeObject(identity);
        assertNull(context.getIdentityByName("jdoe"));
    }

    @Test
    void testContextReset() {
        context.addIdentity(new MockIdentity("jdoe"));
        context.addBundle(new MockBundle("Base Access"));

        context.reset();

        assertTrue(context.getAllIdentities().isEmpty());
        assertTrue(context.getAllBundles().isEmpty());
    }

    @Test
    void testIdentityManagerRelationship() {
        MockIdentity manager = new MockIdentity("msmith")
                .setFirstname("Mary")
                .setLastname("Smith");
        MockIdentity employee = new MockIdentity("jdoe")
                .setFirstname("John")
                .setLastname("Doe")
                .setManager(manager);

        context.addIdentity(manager);
        context.addIdentity(employee);

        MockIdentity retrieved = context.getIdentityByName("jdoe");
        assertNotNull(retrieved.getManager());
        assertEquals("msmith", retrieved.getManager().getName());
    }

    @Test
    void testNonexistentObjectReturnsNull() {
        assertNull(context.getIdentityByName("nobody"));
        assertNull(context.getBundleByName("nothing"));
        assertNull(context.getObjectByName(MockIdentity.class, "nobody"));
        assertNull(context.getObjectById(MockBundle.class, "fakeid"));
    }
}
