package com.toolkit.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulates a SailPoint Identity object for testing purposes.
 * In SailPoint IIQ, an Identity represents a person (employee, contractor, etc.)
 * and holds their attributes, linked accounts (Links), assigned roles (Bundles),
 * and organizational relationships (manager).
 */
public class MockIdentity {

    private String id;
    private String name;
    private String firstname;
    private String lastname;
    private String email;
    private boolean inactive;
    private MockIdentity manager;
    private final Map<String, Object> attributes;
    private final List<MockLink> links;
    private final List<MockBundle> bundles;

    public MockIdentity(String name) {
        this.name = name;
        this.inactive = false;
        this.attributes = new HashMap<>();
        this.links = new ArrayList<>();
        this.bundles = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public MockIdentity setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public MockIdentity setName(String name) {
        this.name = name;
        return this;
    }

    public String getFirstname() {
        return firstname;
    }

    public MockIdentity setFirstname(String firstname) {
        this.firstname = firstname;
        return this;
    }

    public String getLastname() {
        return lastname;
    }

    public MockIdentity setLastname(String lastname) {
        this.lastname = lastname;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public MockIdentity setEmail(String email) {
        this.email = email;
        return this;
    }

    public boolean isInactive() {
        return inactive;
    }

    public MockIdentity setInactive(boolean inactive) {
        this.inactive = inactive;
        return this;
    }

    public MockIdentity getManager() {
        return manager;
    }

    public MockIdentity setManager(MockIdentity manager) {
        this.manager = manager;
        return this;
    }

    // --- Attribute access ---

    public Object getAttribute(String name) {
        switch (name) {
            case "firstname": return firstname;
            case "lastname": return lastname;
            case "email": return email;
            case "inactive": return inactive;
            case "manager": return manager;
            default: return attributes.get(name);
        }
    }

    public MockIdentity setAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    public String getStringAttribute(String name) {
        Object val = getAttribute(name);
        return val != null ? val.toString() : null;
    }

    public Map<String, Object> getAttributes() {
        Map<String, Object> all = new HashMap<>(attributes);
        all.put("firstname", firstname);
        all.put("lastname", lastname);
        all.put("email", email);
        all.put("inactive", inactive);
        return Collections.unmodifiableMap(all);
    }

    // --- Link management ---

    public List<MockLink> getLinks() {
        return Collections.unmodifiableList(links);
    }

    public MockIdentity addLink(MockLink link) {
        link.setIdentity(this);
        links.add(link);
        return this;
    }

    public MockIdentity removeLink(MockLink link) {
        links.remove(link);
        return this;
    }

    public MockLink getLink(String applicationName) {
        for (MockLink link : links) {
            if (applicationName.equals(link.getApplicationName())) {
                return link;
            }
        }
        return null;
    }

    // --- Bundle (role) management ---

    public List<MockBundle> getBundles() {
        return Collections.unmodifiableList(bundles);
    }

    public MockIdentity addBundle(MockBundle bundle) {
        if (!bundles.contains(bundle)) {
            bundles.add(bundle);
        }
        return this;
    }

    public MockIdentity removeBundle(MockBundle bundle) {
        bundles.remove(bundle);
        return this;
    }

    public boolean hasBundle(String bundleName) {
        for (MockBundle bundle : bundles) {
            if (bundleName.equals(bundle.getName())) {
                return true;
            }
        }
        return false;
    }

    // --- Convenience attribute shortcuts ---

    public String getDepartment() {
        return getStringAttribute("department");
    }

    public MockIdentity setDepartment(String department) {
        return setAttribute("department", department);
    }

    public String getTitle() {
        return getStringAttribute("title");
    }

    public MockIdentity setTitle(String title) {
        return setAttribute("title", title);
    }

    public String getLocation() {
        return getStringAttribute("location");
    }

    public MockIdentity setLocation(String location) {
        return setAttribute("location", location);
    }

    @Override
    public String toString() {
        return "MockIdentity{name='" + name + "', department='" + getDepartment() + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockIdentity that = (MockIdentity) o;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
