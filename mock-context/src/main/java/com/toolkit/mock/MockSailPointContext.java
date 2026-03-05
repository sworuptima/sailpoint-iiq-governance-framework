package com.toolkit.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulates the SailPoint SailPointContext API for testing purposes.
 * In production IIQ, SailPointContext is the primary interface for accessing
 * and manipulating objects in the IdentityIQ data store. This mock provides
 * an in-memory implementation that supports identity and bundle storage,
 * retrieval by name or ID, and simple attribute-based searching.
 */
public class MockSailPointContext {

    private final Map<String, MockIdentity> identitiesByName;
    private final Map<String, MockIdentity> identitiesById;
    private final Map<String, MockBundle> bundlesByName;
    private final Map<String, MockBundle> bundlesById;
    private int idCounter;

    public MockSailPointContext() {
        this.identitiesByName = new HashMap<>();
        this.identitiesById = new HashMap<>();
        this.bundlesByName = new HashMap<>();
        this.bundlesById = new HashMap<>();
        this.idCounter = 1;
    }

    // --- Identity operations ---

    public MockSailPointContext addIdentity(MockIdentity identity) {
        if (identity.getId() == null) {
            identity.setId(generateId());
        }
        identitiesByName.put(identity.getName(), identity);
        identitiesById.put(identity.getId(), identity);
        return this;
    }

    public MockIdentity getIdentityByName(String name) {
        return identitiesByName.get(name);
    }

    public MockIdentity getIdentityById(String id) {
        return identitiesById.get(id);
    }

    public List<MockIdentity> getAllIdentities() {
        return new ArrayList<>(identitiesByName.values());
    }

    public void removeIdentity(MockIdentity identity) {
        identitiesByName.remove(identity.getName());
        if (identity.getId() != null) {
            identitiesById.remove(identity.getId());
        }
    }

    // --- Bundle operations ---

    public MockSailPointContext addBundle(MockBundle bundle) {
        if (bundle.getId() == null) {
            bundle.setId(generateId());
        }
        bundlesByName.put(bundle.getName(), bundle);
        bundlesById.put(bundle.getId(), bundle);
        return this;
    }

    public MockBundle getBundleByName(String name) {
        return bundlesByName.get(name);
    }

    public MockBundle getBundleById(String id) {
        return bundlesById.get(id);
    }

    public List<MockBundle> getAllBundles() {
        return new ArrayList<>(bundlesByName.values());
    }

    public void removeBundle(MockBundle bundle) {
        bundlesByName.remove(bundle.getName());
        if (bundle.getId() != null) {
            bundlesById.remove(bundle.getId());
        }
    }

    // --- Generic object access (mirrors SailPoint API patterns) ---

    @SuppressWarnings("unchecked")
    public <T> T getObjectByName(Class<T> clazz, String name) {
        if (MockIdentity.class.isAssignableFrom(clazz)) {
            return (T) getIdentityByName(name);
        } else if (MockBundle.class.isAssignableFrom(clazz)) {
            return (T) getBundleByName(name);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getObjectById(Class<T> clazz, String id) {
        if (MockIdentity.class.isAssignableFrom(clazz)) {
            return (T) getIdentityById(id);
        } else if (MockBundle.class.isAssignableFrom(clazz)) {
            return (T) getBundleById(id);
        }
        return null;
    }

    /**
     * Searches identities by matching a set of attribute name-value pairs.
     * All specified attributes must match for an identity to be included.
     */
    public List<MockIdentity> searchIdentities(Map<String, Object> filter) {
        List<MockIdentity> results = new ArrayList<>();
        for (MockIdentity identity : identitiesByName.values()) {
            boolean matches = true;
            for (Map.Entry<String, Object> entry : filter.entrySet()) {
                Object actual = identity.getAttribute(entry.getKey());
                if (actual == null || !actual.equals(entry.getValue())) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                results.add(identity);
            }
        }
        return results;
    }

    /**
     * Saves an object to the context store. Supports MockIdentity and MockBundle.
     */
    public void saveObject(Object obj) {
        if (obj instanceof MockIdentity) {
            addIdentity((MockIdentity) obj);
        } else if (obj instanceof MockBundle) {
            addBundle((MockBundle) obj);
        }
    }

    /**
     * Removes an object from the context store. Supports MockIdentity and MockBundle.
     */
    public void removeObject(Object obj) {
        if (obj instanceof MockIdentity) {
            removeIdentity((MockIdentity) obj);
        } else if (obj instanceof MockBundle) {
            removeBundle((MockBundle) obj);
        }
    }

    /**
     * Clears all stored objects and resets the ID counter.
     */
    public void reset() {
        identitiesByName.clear();
        identitiesById.clear();
        bundlesByName.clear();
        bundlesById.clear();
        idCounter = 1;
    }

    private String generateId() {
        return String.format("%032d", idCounter++);
    }
}
