# Policy Rules

Rules that detect governance policy violations — Separation of Duties conflicts, entitlement creep, orphan accounts, and non-compliant configurations.

## Business Context

Policies are the guardrails of identity governance. They define what access combinations are allowed, what constitutes excessive access, and what accounts should not exist. Without policy enforcement:

- A single person accumulates conflicting permissions (the person who creates purchase orders also approves them)
- Employees collect access over years of role changes, far exceeding what they need today
- Accounts exist on target systems with no corresponding identity in IIQ — nobody is responsible, nobody reviews them
- Password policies vary across systems, creating weak links in the security chain

Policy rules detect these conditions and can block, flag, or remediate them automatically.

## Implemented Rules

### SoDViolationDetector

Scans identities for Separation of Duties violations against a configurable conflict matrix. Categorizes violations by severity (critical, high, medium). Supports single-identity and bulk scanning modes.

**Config:** `sodViolation.conflictMatrix`, `severityLevels`

**Tests:** 10 tests — single/multiple violations, severity levels, bulk scanning, bidirectional dedup, empty config

### EntitlementCreepDetector

Identifies identities with significantly more access than their peers. Groups by department, computes average role count, and flags outliers exceeding a configurable multiplier. Excludes global roles from comparison.

**Config:** `entitlementCreep.peerGroupAttribute`, `creepThresholdMultiplier`, `minimumPeerGroupSize`, `globalRoles`

**Tests:** 9 tests — creep detection, normal users not flagged, peer info, global role exclusion, small groups, single identity evaluation

## Future Rules

| Rule | Description |
|------|-------------|
| `OrphanAccountIdentifier` | Finds accounts on target systems that are not correlated to any identity |
| `PasswordPolicyEnforcement` | Validates password configurations against organizational standards |

## Status

**Ready** — Core rules implemented with full test coverage. Contributions welcome. See [CONTRIBUTING.md](../../CONTRIBUTING.md).
