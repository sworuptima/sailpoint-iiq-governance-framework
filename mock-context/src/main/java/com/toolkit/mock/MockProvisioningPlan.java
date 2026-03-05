package com.toolkit.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simulates a SailPoint ProvisioningPlan for testing purposes.
 * A ProvisioningPlan describes what changes should be made to target systems
 * for a given identity. It contains one or more AccountRequests, each targeting
 * a specific application. Each AccountRequest contains AttributeRequests that
 * specify individual attribute-level changes (set, add, remove).
 */
public class MockProvisioningPlan {

    private MockIdentity identity;
    private final List<AccountRequest> accountRequests;

    public MockProvisioningPlan() {
        this.accountRequests = new ArrayList<>();
    }

    public MockProvisioningPlan(MockIdentity identity) {
        this();
        this.identity = identity;
    }

    public MockIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(MockIdentity identity) {
        this.identity = identity;
    }

    public List<AccountRequest> getAccountRequests() {
        return Collections.unmodifiableList(accountRequests);
    }

    public MockProvisioningPlan addAccountRequest(AccountRequest request) {
        accountRequests.add(request);
        return this;
    }

    public boolean isEmpty() {
        return accountRequests.isEmpty();
    }

    /**
     * Returns a human-readable summary of this provisioning plan,
     * useful for logging and test assertions.
     */
    public String toSummaryString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ProvisioningPlan for: ")
          .append(identity != null ? identity.getName() : "unknown")
          .append("\n");

        for (AccountRequest acctReq : accountRequests) {
            sb.append("  Account: ").append(acctReq.getApplicationName())
              .append(" [").append(acctReq.getOperation()).append("]\n");

            for (AttributeRequest attrReq : acctReq.getAttributeRequests()) {
                sb.append("    ").append(attrReq.getOperation())
                  .append(" ").append(attrReq.getName())
                  .append(" = ").append(attrReq.getValue())
                  .append("\n");
            }
        }
        return sb.toString();
    }

    // --- Operation Enum ---

    public enum Operation {
        Create,
        Modify,
        Delete,
        Disable,
        Enable,
        Set,
        Add,
        Remove
    }

    // --- AccountRequest ---

    public static class AccountRequest {

        private String applicationName;
        private String nativeIdentity;
        private Operation operation;
        private final List<AttributeRequest> attributeRequests;

        public AccountRequest() {
            this.attributeRequests = new ArrayList<>();
        }

        public AccountRequest(String applicationName, Operation operation) {
            this();
            this.applicationName = applicationName;
            this.operation = operation;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public AccountRequest setApplicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        public String getNativeIdentity() {
            return nativeIdentity;
        }

        public AccountRequest setNativeIdentity(String nativeIdentity) {
            this.nativeIdentity = nativeIdentity;
            return this;
        }

        public Operation getOperation() {
            return operation;
        }

        public AccountRequest setOperation(Operation operation) {
            this.operation = operation;
            return this;
        }

        public List<AttributeRequest> getAttributeRequests() {
            return Collections.unmodifiableList(attributeRequests);
        }

        public AccountRequest add(AttributeRequest attributeRequest) {
            attributeRequests.add(attributeRequest);
            return this;
        }
    }

    // --- AttributeRequest ---

    public static class AttributeRequest {

        private String name;
        private Operation operation;
        private Object value;

        public AttributeRequest() {
        }

        public AttributeRequest(String name, Operation operation, Object value) {
            this.name = name;
            this.operation = operation;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public AttributeRequest setName(String name) {
            this.name = name;
            return this;
        }

        public Operation getOperation() {
            return operation;
        }

        public AttributeRequest setOperation(Operation operation) {
            this.operation = operation;
            return this;
        }

        public Object getValue() {
            return value;
        }

        public AttributeRequest setValue(Object value) {
            this.value = value;
            return this;
        }

        @Override
        public String toString() {
            return "AttributeRequest{" + operation + " " + name + "=" + value + "}";
        }
    }
}
