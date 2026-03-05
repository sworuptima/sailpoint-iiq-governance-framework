package com.toolkit.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simulates a SailPoint Link object for testing purposes.
 * In SailPoint IIQ, a Link represents an account on a target application
 * that is correlated to an identity. Each Link holds the native identity
 * (the username on the target system) and a map of account attributes
 * read during aggregation.
 */
public class MockLink {

    private String id;
    private String applicationName;
    private String nativeIdentity;
    private MockIdentity identity;
    private boolean disabled;
    private final Map<String, Object> attributes;

    public MockLink(String applicationName, String nativeIdentity) {
        this.applicationName = applicationName;
        this.nativeIdentity = nativeIdentity;
        this.disabled = false;
        this.attributes = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public MockLink setId(String id) {
        this.id = id;
        return this;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public MockLink setApplicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    public String getNativeIdentity() {
        return nativeIdentity;
    }

    public MockLink setNativeIdentity(String nativeIdentity) {
        this.nativeIdentity = nativeIdentity;
        return this;
    }

    public MockIdentity getIdentity() {
        return identity;
    }

    public MockLink setIdentity(MockIdentity identity) {
        this.identity = identity;
        return this;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public MockLink setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public MockLink setAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public String toString() {
        return "MockLink{app='" + applicationName + "', nativeIdentity='" + nativeIdentity + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockLink mockLink = (MockLink) o;
        if (applicationName != null ? !applicationName.equals(mockLink.applicationName) : mockLink.applicationName != null)
            return false;
        return nativeIdentity != null ? nativeIdentity.equals(mockLink.nativeIdentity) : mockLink.nativeIdentity == null;
    }

    @Override
    public int hashCode() {
        int result = applicationName != null ? applicationName.hashCode() : 0;
        result = 31 * result + (nativeIdentity != null ? nativeIdentity.hashCode() : 0);
        return result;
    }
}
