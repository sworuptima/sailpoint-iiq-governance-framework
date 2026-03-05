# IAM & SailPoint Glossary

Plain-language definitions of Identity and Access Management (IAM) and SailPoint IdentityIQ terminology. If you're new to IAM or SailPoint, start here.

---

### Aggregation
The process of reading accounts, entitlements, and group memberships from a target system (Active Directory, LDAP, a database, etc.) into IdentityIQ. Think of it as "pulling a snapshot" of who has what access on each system. Aggregation runs on a schedule and feeds data into the correlation engine.

### Authoritative Source
The system of record for identity data — typically an HR system or HCM platform. When the authoritative source says someone's department is "Engineering," that becomes the truth in IIQ. Attributes from the authoritative source drive lifecycle events and birthright access.

### Birthright Access
The minimum set of roles and entitlements automatically assigned to someone based on who they are (department, title, location), not what they've requested. When a new engineer joins, they get "Engineering Tools" and "GitHub Access" without asking — that's birthright access.

### Bundle
SailPoint's internal object representing a role or entitlement group. Bundles can be business roles (high-level, like "Sales Representative"), IT roles (technical, like "AD-Sales-Group"), or entitlements (individual permissions). The term "bundle" and "role" are often used interchangeably.

### Certification Campaign
A periodic access review where managers or application owners verify that the people under their responsibility still need the access they have. Reviewers approve, revoke, or reassign each access item. Also called "access review" or "attestation."

### Correlation
The process of matching an account on a target system to an identity in IIQ. When aggregation finds an AD account "jdoe," correlation determines that it belongs to the identity "John Doe" using matching rules (employee ID, email, username patterns).

### Entitlement
A specific permission or group membership on a target system. Examples: an Active Directory group membership, a database role, an application permission. Entitlements are the atomic units of access.

### Entitlement Creep
The gradual accumulation of access rights over time, especially when someone changes roles without losing old access. A person who started in Sales, moved to Finance, and then to Engineering might still have CRM access, budget approval rights, and source code access — far more than their current role requires.

### Identity
A person record in IdentityIQ representing an employee, contractor, partner, or other user. An identity aggregates information from multiple sources: HR data (name, department, manager), account data (AD account, LDAP entry), and role assignments.

### Identity Cube
SailPoint's term for the unified view of an identity — all their attributes, accounts, entitlements, roles, certifications, and policy violations in one place. The identity cube is what makes governance possible: you can see everything a person has across all systems.

### Identity Lifecycle
The stages an identity goes through in an organization: Joiner (new hire), Mover (role change), and Leaver (departure). Each stage triggers different governance actions — birthright provisioning, access rebalancing, or account deprovisioning.

### Joiner
A new identity entering the organization. The joiner process typically involves creating accounts on target systems, assigning birthright roles, and notifying the manager. Without automation, new hires wait days for the access they need to work.

### LDAP (Lightweight Directory Access Protocol)
A protocol for accessing directory services like Active Directory or OpenLDAP. In IAM, LDAP directories are common target systems that store user accounts and group memberships.

### Leaver
An identity departing the organization (termination, resignation, contract end). The leaver process disables or deletes accounts, revokes access, and may preserve data for compliance. Delayed leaver processing is one of the biggest security risks in IAM.

### Link
A SailPoint object representing an account on a target system, linked (correlated) to an identity. If John Doe has an AD account and an LDAP account, his identity has two links. Each link holds the native identity (username on that system) and the account's attributes.

### Mover
An identity changing roles, departments, or locations within the organization. Movers are the most complex lifecycle event because they require removing old access, adding new access, and verifying that the transition doesn't create policy violations.

### Orphan Account
An account on a target system that isn't correlated to any identity in IIQ. This usually means someone left the organization but their account was never disabled, or an account was created outside of the governance process. Orphan accounts are security risks and audit findings.

### Policy
A governance rule that defines what access combinations are allowed or prohibited. The most common policy type is Separation of Duties (SoD) — for example, "no one should have both 'Create Purchase Order' and 'Approve Purchase Order.'"

### Provisioning
The act of creating, modifying, disabling, or deleting accounts and entitlements on target systems. Provisioning can be automatic (triggered by a rule) or manual (initiated by a request). A ProvisioningPlan describes what changes to make.

### ProvisioningPlan
A SailPoint object that describes a set of changes to be made to target systems. It contains AccountRequests (which application and what operation) and AttributeRequests (which attributes to set, add, or remove). Rules build plans; the provisioning engine executes them.

### RBAC (Role-Based Access Control)
An access model where permissions are assigned to roles, and roles are assigned to people. Instead of giving John direct access to 47 individual permissions, you assign him the "Senior Developer" role, which includes all the permissions he needs. RBAC simplifies administration and auditing.

### Rule
BeanShell or Java code that runs within IdentityIQ to implement business logic. Rules are attached to specific extension points in the IIQ lifecycle — aggregation rules transform data, correlation rules match accounts, provisioning rules prepare changes, certification rules filter reviews.

### SailPointContext
The API entry point for interacting with IdentityIQ objects programmatically. Through the context, rules can look up identities, search for objects, save changes, and trigger workflows. Every IIQ rule receives a `context` variable.

### SCIM (System for Cross-domain Identity Management)
A standard protocol for automating user provisioning across systems. SCIM provides a REST API for creating, reading, updating, and deleting user accounts. Many SaaS applications support SCIM for automated provisioning from IIQ.

### Separation of Duties (SoD)
A governance principle that prevents a single person from holding conflicting access rights. Classic example: the person who creates invoices should not also approve payments. SoD policies in IIQ detect violations and can block or flag them for review.

### Target System
An application, directory, database, or service that IdentityIQ manages. Examples: Active Directory, ServiceNow, Salesforce, Oracle Database, AWS IAM. IIQ connects to target systems through connectors to aggregate data and provision changes.

### Workflow
An orchestrated series of steps in IdentityIQ that can include approvals, notifications, provisioning, and custom logic. Workflows coordinate complex processes like access requests (submit → manager approval → security review → provisioning → notification).
