# ADR-0005: Control Centre — No Direct Database Access

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-22 |
| **Phase** | 1 |

## Context

Administrative interfaces pose high risk for unauthorized financial changes, Payment Ready overrides, and audit tampering.

## Decision

The **SecurePay Control Centre** must:

- Use secured administration APIs only
- Never directly access production database tables
- Never directly edit ledger records or assign Payment Ready
- Use RBAC, step-up authentication, and immutable audit for sensitive changes

## Consequences

### Positive

- All administrative actions pass through same authorization and audit pipeline as automation.
- Doctrine tests can enforce API-level prohibitions.

### Negative

- Administration APIs must be built before Control Centre features.
- Some operational queries may require read APIs rather than ad hoc SQL.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Direct DB admin console | Bypasses Payment Ready and ledger immutability |
| Read-only SQL access for ops | Credential leakage risk; inconsistent audit |

## Security impact

Eliminates a major class of insider and compromise blast-radius scenarios.

## Operational impact

Platform metrics and partner management exposed via dedicated admin APIs.

## Unresolved matters

- Break-glass read-only diagnostic access policy — security + operations to define.
