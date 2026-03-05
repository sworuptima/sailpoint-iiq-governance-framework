# Correlation Rules

Rules that match accounts from target systems to identities in IdentityIQ.

## Business Context

When IIQ aggregates an account like `jdoe` from Active Directory, it needs to determine which identity that account belongs to. This sounds simple, but in practice:

- Usernames aren't consistent across systems (`jdoe` in AD, `john.doe` in LDAP, `12345` in the HR system)
- People change names (marriage, legal name change) but keep old accounts
- Contractors may not exist in the HR system at all
- Shared or service accounts don't map to a single person

Correlation rules implement the logic to match accounts to identities accurately, reducing orphan accounts and miscorrelation.

## Implemented Rules

### WeightedMultiAttributeCorrelation

Scores candidate identities across multiple attributes with configurable weights. EmployeeId match = 100 points, email = 80, last name = 30. Best score above the threshold wins. Case-insensitive by default.

**Config:** `weightedCorrelation.attributes` (name → weight), `matchThreshold`, `caseSensitive`

**Tests:** 9 tests — exact match, multi-attribute scoring, threshold filtering, case insensitivity

### FuzzyMatchCorrelation

Uses Levenshtein edit distance for approximate string matching. Handles typos ("Jon" vs "John"), name variations, and inconsistent formatting. Checks exact-match attributes first (employeeId), falls back to fuzzy scoring.

**Config:** `fuzzyMatch.attributes`, `minimumScore`, `maxEditDistance`, `exactMatchAttributes`

**Tests:** 11 tests — exact match, fuzzy matching, Levenshtein distance, similarity scoring, threshold behavior

## Future Rules

| Rule | Description |
|------|-------------|
| `ContractorCorrelation` | Handles correlation for contractors who may lack standard HR attributes |

## Status

**Ready** — Core rules implemented with full test coverage. Contributions welcome. See [CONTRIBUTING.md](../../CONTRIBUTING.md).
