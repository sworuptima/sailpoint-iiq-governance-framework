# Mock SailPoint Context

A lightweight Java library that stubs out core SailPoint IdentityIQ API classes, enabling rule development and testing without a live IIQ instance.

## Purpose

SailPoint IdentityIQ rules are typically developed and tested directly inside the IIQ console, which requires a licensed installation, a running application server, and a configured database. This creates a slow feedback loop and makes it difficult to write unit tests.

The mock context module provides in-memory implementations of the key SailPoint object types so that rule logic can be tested with standard JUnit, using `mvn test` from the command line.

## What's Included

| Class | Simulates | Key Features |
|-------|-----------|--------------|
| `MockSailPointContext` | `sailpoint.api.SailPointContext` | In-memory object store, lookup by name/ID, attribute-based search |
| `MockIdentity` | `sailpoint.object.Identity` | Attributes, links, bundles, manager relationship, fluent builder |
| `MockLink` | `sailpoint.object.Link` | Application account with native identity and attributes |
| `MockBundle` | `sailpoint.object.Bundle` | Role/entitlement with type, requirements, and permits |
| `MockProvisioningPlan` | `sailpoint.object.ProvisioningPlan` | AccountRequest/AttributeRequest hierarchy with operations |

## Usage

```java
// Set up context with test data
MockSailPointContext context = new MockSailPointContext();

MockBundle baseAccess = new MockBundle("Base Access");
context.addBundle(baseAccess);

MockIdentity identity = new MockIdentity("jdoe")
    .setFirstname("John")
    .setLastname("Doe")
    .setAttribute("department", "Engineering")
    .setAttribute("title", "Senior Developer")
    .addBundle(baseAccess);
context.addIdentity(identity);

// Build a provisioning plan
MockProvisioningPlan plan = new MockProvisioningPlan(identity);
MockProvisioningPlan.AccountRequest acctReq =
    new MockProvisioningPlan.AccountRequest("Active Directory", MockProvisioningPlan.Operation.Modify);
acctReq.add(new MockProvisioningPlan.AttributeRequest(
    "assignedRoles", MockProvisioningPlan.Operation.Add, "Engineering Tools"));
plan.addAccountRequest(acctReq);

System.out.println(plan.toSummaryString());
```

## Limitations

This mock layer is intentionally simplified. It does not replicate:

- QueryOptions or Filter-based searching (uses simple attribute map matching)
- Persistent storage or transactions
- Event model or change listeners
- Full SailPoint XML serialization
- TaskResult, AuditEvent, or other secondary object types

These limitations are acceptable for unit testing rule business logic. Integration testing against a live IIQ instance is still recommended before production deployment.
