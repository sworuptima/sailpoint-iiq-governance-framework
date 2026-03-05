# Workflow Rules

Library rules that support workflow execution — approval routing, escalation logic, retry handling, and audit logging.

## Business Context

Workflows in SailPoint IIQ orchestrate multi-step governance processes: access requests flow through approvals, provisioning operations retry on failure, and audit events get logged for compliance. Workflow library rules provide reusable building blocks:

- **Approval routing**: Determining who should approve a request based on the request type, risk level, and organizational hierarchy
- **Escalation**: Automatically escalating stale approvals to the next level
- **Error handling**: Retrying failed provisioning operations with backoff
- **Audit**: Creating detailed records of every governance decision

## Implemented Rules

### ApprovalEscalation

Escalates pending approvals through a tiered chain: manager → director → security team. Configurable timeout periods at each level. Optional auto-approve after maximum wait period and reminder notifications.

**Config:** `approvalEscalation.escalationLevels`, `autoApproveAfterDays`, `sendReminders`, `reminderIntervalDays`

**Tests:** 11 tests — no action, reminder, tiered escalation (3/5/7 days), auto-approve, work item tracking

### DynamicApprovalRouting

Routes approval requests based on risk score and application sensitivity. Low-risk requests go to the manager with single approval; critical requests require the security team with three approval levels.

**Config:** `dynamicRouting.riskThresholds`, `routingRules`, `applicationSensitivity`, `defaultRiskLevel`

**Tests:** 11 tests — risk level routing, application override, higher-risk-wins, request details, empty config defaults

## Future Rules

| Rule | Type | Description |
|------|------|-------------|
| `RetryWrapper` | WorkflowLibrary | Wraps provisioning steps with configurable retry logic and failure handling |
| `AuditEventLogger` | WorkflowLibrary | Creates standardized audit events for governance actions |

## Status

**Ready** — Core rules implemented with full test coverage. Contributions welcome. See [CONTRIBUTING.md](../../CONTRIBUTING.md).
