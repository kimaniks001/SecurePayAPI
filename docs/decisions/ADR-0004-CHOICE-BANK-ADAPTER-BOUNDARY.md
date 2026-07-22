# ADR-0004: Choice Bank Adapter Boundary

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-22 |
| **Phase** | 1 |

## Context

Choice Bank BaaS requires private key signing, callback verification, and regulated handling of banking operations. Partners and frontends must never hold Choice credentials.

## Decision

All Choice Bank communication occurs exclusively through **`choice-bank-connector`**:

- Request signing and response verification inside connector
- Provider-neutral commands from SecurePay Core and ledger
- Idempotent callback processing with reconciliation
- Sandbox and production credential separation

Unconfirmed Choice capabilities remain placeholders until sandbox- or production-certified.

## Consequences

### Positive

- Single audit point for banking integration.
- Provider replacement possible with new adapter implementation.

### Negative

- Connector becomes critical path; requires high availability design.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Direct Choice calls from core | Violates credential isolation and doctrine |
| Frontend-initiated banking | Unacceptable security and compliance risk |

## Security impact

Credentials never in client apps or Git. Logs redact provider secrets.

## Operational impact

Choice outages handled with queued retries without corrupting ledger state.

## Unresolved matters

See [Choice Contract Open Questions](../banking/contract-findings/CHOICE_CONTRACT_OPEN_QUESTIONS.md).
