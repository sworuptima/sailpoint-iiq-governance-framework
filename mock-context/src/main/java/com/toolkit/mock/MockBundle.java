package com.toolkit.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulates a SailPoint Bundle (role or entitlement group) for testing purposes.
 * In SailPoint IIQ, a Bundle represents a role that can be assigned to identities.
 * Bundles can have requirements (other bundles that must also be assigned) and
 * can be typed as business roles, IT roles, or entitlements.
 */
public class MockBundle {

    private String id;
    private String name;
    private String displayName;
    private String type;
    private boolean disabled;
    private final Map<String, Object> attributes;
    private final List<MockBundle> requirements;
    private final List<MockBundle> permits;

    public MockBundle(String name) {
        this.name = name;
        this.displayName = name;
        this.type = "it";
        this.disabled = false;
        this.attributes = new HashMap<>();
        this.requirements = new ArrayList<>();
        this.permits = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public MockBundle setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public MockBundle setName(String name) {
        this.name = name;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public MockBundle setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getType() {
        return type;
    }

    public MockBundle setType(String type) {
        this.type = type;
        return this;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public MockBundle setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public MockBundle setAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public List<MockBundle> getRequirements() {
        return Collections.unmodifiableList(requirements);
    }

    public MockBundle addRequirement(MockBundle bundle) {
        requirements.add(bundle);
        return this;
    }

    public MockBundle removeRequirement(MockBundle bundle) {
        requirements.remove(bundle);
        return this;
    }

    public List<MockBundle> getPermits() {
        return Collections.unmodifiableList(permits);
    }

    public MockBundle addPermit(MockBundle bundle) {
        permits.add(bundle);
        return this;
    }

    @Override
    public String toString() {
        return "MockBundle{name='" + name + "', type='" + type + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockBundle that = (MockBundle) o;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
