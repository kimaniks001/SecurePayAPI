# Transaction Boundary Standard

**Status:** Current architectural decision (Phase 3 implementation)  
**Phase:** 3 — database, audit, idempotency foundation  
**Branch:** `phase-03-database-audit-idempotency-foundation`  
**Module:** `shared/platform-persistence`

## Purpose

Define when and how database transactions span multiple persistence operations so that business state, audit events, and outbox messages remain consistent.

## Core principle

**If an operation must be atomic with its audit trail or outbox event, it must share one transaction boundary.**

Partial success (state committed without audit or outbox) is a defect for mutating platform and future business flows.

## Phase 3 reference flow

`TechnicalFoundationService.recordTechnicalCreation` demonstrates the canonical pattern:

```
@Transactional
├── INSERT platform.technical_test_records
├── AuditEventWriter.append → INSERT audit.audit_events
└── OutboxWriter.appendTechnicalTestCreated → INSERT events.outbox_events
```

All three succeed or all roll back.

## Transaction demarcation rules

| Rule | Requirement |
| --- | --- |
| Entry point | `@Transactional` on service method orchestrating the flow |
| Repository methods | No `@Transactional` on repositories; participate in caller transaction |
| Read-only queries | `@Transactional(readOnly = true)` when a service method only reads |
| Propagation | Default `REQUIRED` unless ADR specifies otherwise |
| Rollback | Unchecked exceptions roll back; do not catch-and-commit partial work |

## Idempotency and transactions

`IdempotencyService.executeTechnical` is `@Transactional`:

1. Insert or resolve idempotency record
2. Execute supplied action (may include further persistence)
3. Mark idempotency record completed

The idempotency record and side effects must commit together. If the action throws, the transaction rolls back including the `IN_PROGRESS` insert unless explicitly handled in a future design (not Phase 3).

## Outbox publisher boundary

`OutboxPublisher.publish` must **not** run inside the same transaction as the outbox insert in production worker design:

| Phase | Behaviour |
| --- | --- |
| Phase 3 write path | Insert only; `NoOpOutboxPublisher` |
| Future worker | Short transaction: claim row → commit → publish externally → new transaction mark published |

External message broker I/O must not hold database transactions open.

## Cross-schema scope

PostgreSQL supports cross-schema transactions in a single database. Phase 3 schemas (`platform`, `audit`, `events`, `idempotency`) participate in one connection transaction via Spring's `DataSourceTransactionManager`.

## Isolation level

Default: PostgreSQL `READ COMMITTED` (Spring Boot default). Raise isolation only with ADR justification (e.g. serializable for specific financial invariants in ledger phase).

## Anti-patterns

| Anti-pattern | Risk |
| --- | --- |
| Audit append in separate transaction after commit | Audit missing when downstream fails |
| Outbox insert after async `@Async` | Not atomic with state change |
| Manual commit in repository | Bypasses Spring rollback semantics |
| Long-running external API inside `@Transactional` | Connection pool exhaustion |

## Future business commands

When agreement or payment commands arrive:

- Domain row update + audit + outbox + idempotency completion share one transaction where operation scope requires it
- Ledger postings may require distributed transaction patterns (outbox to ledger service)—separate ADR

## Testing

Integration tests use `@Transactional` rollback or dedicated Testcontainers database per test class. `AuditEventIntegrationTest`, `OutboxIntegrationTest`, and idempotency tests assert cross-table consistency.

## Related documents

- [Transactional Outbox Standard](TRANSACTIONAL_OUTBOX_STANDARD.md)
- [Idempotency Standard](IDEMPOTENCY_STANDARD.md)
- [Database Schema Ownership Standard](DATABASE_SCHEMA_OWNERSHIP_STANDARD.md)
- [Audit Event Standard](../security/AUDIT_EVENT_STANDARD.md)
