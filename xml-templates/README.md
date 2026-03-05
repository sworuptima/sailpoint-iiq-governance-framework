# XML Templates

IIQ-importable XML rule definitions that can be deployed directly into a SailPoint IdentityIQ instance.

## What Are XML Templates?

SailPoint IdentityIQ stores rules, workflows, and configuration as XML objects in its database. To deploy a rule into IIQ, you import an XML file that contains the rule's metadata (name, type, description) and its BeanShell source code.

These templates provide ready-to-import XML for the rules implemented in this toolkit.

## How to Import

### Via IIQ Console

```bash
> import /path/to/joiner-birthright-rule.xml
```

### Via IIQ API

```java
Importer importer = new Importer(context);
importer.importFile("/path/to/joiner-birthright-rule.xml");
```

### Via SSB/SSD Build

Copy the XML files into your SSB `config/Rule/` directory and run the build/deploy process.

## Templates

| File | Rule Type | Description |
|------|-----------|-------------|
| `rule-template.xml` | Generic | Blank rule skeleton — starting point for new rules |
| `joiner-birthright-rule.xml` | Workflow | Assigns birthright roles to new identities |
| `mover-rebalance-rule.xml` | Workflow | Rebalances access on department change |
| `custom-schema-mapping-rule.xml` | BuildMap | Maps non-standard source attributes to normalized IIQ schema |
| `weighted-correlation-rule.xml` | Correlation | Scores candidate identities across weighted attributes |
| `before-provisioning-sod-check-rule.xml` | BeforeProvisioning | Validates SoD compliance before provisioning executes |
| `service-account-exclusion-rule.xml` | CertificationExclusion | Excludes service/system accounts from certifications |
| `sod-violation-detector-rule.xml` | Policy | Scans identities for Separation of Duties violations |
| `approval-escalation-rule.xml` | Workflow | Escalates pending approvals through a tiered chain |

## BeanShell Notes

IIQ rules use BeanShell, which is essentially Java without generics. Key differences from standard Java:

- No generic type parameters (`List` instead of `List<String>`)
- No diamond operator
- No lambda expressions
- Explicit casts required
- Variables available in scope depend on rule type (check IIQ documentation for each type)

## Customization

Before importing, review and update:

1. **Rule name** — must be unique within your IIQ instance
2. **Department/role mappings** — adjust to match your organization
3. **Application names** — update to match your configured applications
4. **Custom object references** — if using externalized config, ensure the Custom object exists
