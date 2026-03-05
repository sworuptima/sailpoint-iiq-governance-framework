# Certification Rules

Rules that control access certification campaigns — periodic reviews where managers verify that their reports still need the access they have.

## Business Context

Certification campaigns are a core compliance requirement. Regulations like SOX, HIPAA, and SOC 2 require periodic access reviews. Without customization, certification campaigns can be overwhelming:

- A manager with 50 direct reports, each with 20 entitlements, faces 1,000 review decisions
- Service accounts and system accounts clutter the review with irrelevant items
- Low-risk access gets the same attention as high-risk access
- Reviewers rubber-stamp approvals because the volume is unmanageable

Certification rules solve this by scoping campaigns intelligently — excluding noise, focusing on risk, and automating decisions where policy allows.

## Implemented Rules

### ServiceAccountExclusion

Excludes service and system accounts from manager certifications. Matches name patterns (`svc_*`, `system_*`), checks identity type attributes, filters inactive identities and disabled accounts.

**Config:** `serviceAccountExclusion.serviceAccountPatterns`, `serviceAccountAttribute`, `serviceAccountValues`, `excludeInactive`, `excludeDisabledAccounts`

**Tests:** 11 tests — name patterns, identity type, inactive exclusion, disabled accounts, mixed accounts, multiple reasons

### RiskBasedCertScoping

Scopes certification campaigns to high-risk entitlements only. Evaluates risk scores against thresholds, checks for high-risk role assignments, and detects privilege-indicating attributes.

**Config:** `riskBasedScoping.riskAttribute`, `highRiskThreshold`, `highRiskRoles`, `alwaysIncludeAttributes`

**Tests:** 10 tests — risk score thresholds, high-risk roles, privilege attributes, empty config defaults

## Future Rules

| Rule | Type | Description |
|------|------|-------------|
| `AutoRevocationRule` | CertificationAutomaticClosing | Automatically revokes access that hasn't been used in N days |
| `CertPreDelegation` | CertificationPreDelegation | Routes certification items to the right reviewer based on application ownership |

## Status

**Ready** — Core rules implemented with full test coverage. Contributions welcome. See [CONTRIBUTING.md](../../CONTRIBUTING.md).
