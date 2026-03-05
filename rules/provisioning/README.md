# Provisioning Rules

Rules that execute before or after provisioning operations — when IdentityIQ creates, modifies, or deletes accounts on target systems.

## Business Context

Provisioning is where governance decisions become real changes. Before an account is created or a group membership is added, provisioning rules can:

- **Validate** that the change doesn't violate policy (SoD check before granting access)
- **Transform** the request (compute a home directory path, generate a username)
- **Enrich** the request (add default attributes, set expiration dates)
- **Audit** the change (create tickets, send notifications, log for compliance)

After provisioning completes, rules can verify the result, create audit records, or trigger follow-up actions.

## Implemented Rules

### AttributeDrivenGroupAssignment

Computes target system group memberships from identity attributes. Maps departments to AD groups, locations to office groups, and adds global groups. Builds the provisioning plan automatically.

**Config:** `groupAssignment.departmentGroupMappings`, `locationGroupMappings`, `globalGroups`, `targetApplication`

**Tests:** 7 tests — department groups, location groups, unknown department, empty config, deduplication

### BeforeProvisioningSoDCheck

Validates Separation of Duties compliance before provisioning executes. Collects current + requested roles, checks against a conflict matrix, and either blocks or flags violations. Supports exempt roles.

**Config:** `sodCheck.conflictMatrix`, `blockOnViolation`, `exemptRoles`

**Tests:** 9 tests — no violation, single/multiple violations, block vs flag, exempt roles, existing conflicts

## Future Rules

| Rule | Type | Description |
|------|------|-------------|
| `HomeDirectoryProvisioning` | FieldValue | Generates home directory paths based on department and username |
| `AfterProvisioningTicketCreation` | AfterProvisioning | Creates ITSM tickets for audit trail after provisioning completes |

## Status

**Ready** — Core rules implemented with full test coverage. Contributions welcome. See [CONTRIBUTING.md](../../CONTRIBUTING.md).
