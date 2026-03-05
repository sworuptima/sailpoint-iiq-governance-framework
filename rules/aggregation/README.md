# Aggregation Rules

Rules that execute during the aggregation phase — when IdentityIQ reads accounts and entitlements from target systems.

## Business Context

Aggregation is the foundation of identity governance. Before IIQ can govern access, it needs an accurate picture of what access exists. Aggregation rules customize how that data is read, transformed, and stored.

Common challenges aggregation rules solve:
- **Schema mismatches**: Source systems store data in different formats (date strings, nested groups, multi-valued attributes)
- **Data quality**: Missing fields, inconsistent naming, duplicate records
- **Multi-source correlation**: The same person may exist in multiple systems with different identifiers
- **Performance**: Large directories with millions of accounts need efficient processing

## Implemented Rules

### CustomSchemaMapping

Maps non-standard source system attributes into a normalized IIQ schema. Each application gets its own attribute translation table configured in JSON. Unmapped attributes pass through unchanged.

**Config:** `schemaMappings` (per-application maps), `defaultMappings` (fallback translations)

**Tests:** 9 tests — AD mapping, HR mapping, unknown app defaults, null handling, link application

### NestedGroupFlattener

Flattens hierarchical Active Directory group memberships into a single deduplicated list. Resolves parent groups recursively with configurable depth limits and circular reference protection.

**Config:** `nestedGroupConfig.maxDepth`, `nestedGroupConfig.includeParentGroups`, `groupDelimiter`

**Tests:** 11 tests — hierarchy resolution, deduplication, circular references, depth limits, delimiter parsing

## Future Rules

| Rule | Type | Description |
|------|------|-------------|
| `MultiSourceCorrelationAggregation` | ResourceObjectCustomization | Normalizes account data from multiple sources for consistent correlation |
| `PreIterationCleanup` | PreIterate | Cleans up stale data before aggregation begins |

## Status

**Ready** — Core rules implemented with full test coverage. Contributions welcome. See [CONTRIBUTING.md](../../CONTRIBUTING.md).
