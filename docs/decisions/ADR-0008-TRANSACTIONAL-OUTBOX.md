# ADR-0008: Transactional Outbox

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-22 |
| **Phase** | 3 |
| **Branch** | `phase-03-database-audit-idempotency-foundation` |

## Context

SecurePay must publish domain events to downstream consumers (webhooks, notifications, analytics, ledger adjacency) without losing events when the application crashes after a database commit or when message brokers are temporarily unavailable.

Dual-write patterns (write DB then publish to broker) create consistency gaps. Phase 3 establishes the outbox infrastructure before business-domain aggregates exist, using a technical test flow to prove atomic write + outbox append in a single transaction.

## Decision

Adopt the **transactional outbox pattern** with PostgreSQL table `events.outbox_events`, implemented in `shared/platform-persistence`:

1. **Atomic append:** Business or technical state changes and outbox row insertion occur in the same `@Transactional` boundary (demonstrated by `TechnicalFoundationService.recordTechnicalCreation`).
2. **Envelope contract:** Outbox payloads conform to `DomainEventEnvelope` and the JSON schema at `contracts/events/event-envelope-v1.schema.json`.
3. **Status lifecycle:** `PENDING` → `PROCESSING` → `PUBLISHED` | `FAILED` | `DEAD_LETTER`, enforced by check constraint and `OutboxStatus` enum.
4. **Optimistic locking:** `version` column on `events.outbox_events`; worker transitions use `WHERE id = :id AND version = :expectedVersion` (see `OPTIMISTIC_LOCKING_STANDARD.md`).
5. **Phase 3 publisher:** `OutboxPublisher` interface with `NoOpOutboxPublisher` default—events are persisted but not externally published until a worker phase enables dispatch.
6. **Technical event only:** Phase 3 emits `platform.test.created` (`TechnicalEventTypes.PLATFORM_TEST_CREATED`) for integration validation; no business-domain event types.

Persistence uses **Spring Data JDBC** (`OutboxRepository`, `NamedParameterJdbcTemplate`)—not JPA.

## Consequences

### Positive

- Guaranteed at-least-once internal durability of events relative to committing transactions.
- Outbox table supports replay, backlog inspection, and dead-letter investigation.
- Correlation and causation IDs propagate from `ActorContext` into outbox rows for distributed tracing.

### Negative

- Requires a future outbox worker process and broker integration (not built in Phase 3).
- Table growth until published rows are archived or purged per retention policy.
- Consumers must be idempotent; duplicate delivery is possible after retries.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Publish-then-commit (broker first) | Message loss if DB commit fails after publish |
| Change Data Capture (CDC) only | Higher operational complexity; deferred as optional complement |
| Kafka transaction + DB (XA) | Heavier ops; PostgreSQL outbox sufficient for SecurePay scale targets |
| In-memory event bus | Not durable; unsuitable for financial adjacency |

## Security impact

- Outbox payloads pass through the same forbidden-key validation patterns as audit (no secrets in JSON bodies).
- Future `outbox_worker` role receives `UPDATE` on `events.outbox_events` only—not business tables.
- Event payloads may contain operational metadata; access restricted by database role and service identity.

## Operational impact

- Monitor `events.outbox_events` counts by `status`; alert on growing `PENDING` or `DEAD_LETTER` backlogs in production (Phase 4+).
- `OutboxRepository.countByStatus` supports integration tests and future metrics.
- `available_at` supports delayed retry scheduling when the worker is implemented.

## Migration impact

- Migration `V20260723090000__phase_03_technical_foundations.sql` creates schema `events` and table `events.outbox_events` with indexes on `(status, available_at)`, `created_at`, and `correlation_id`.
- No data backfill required; table starts empty.
- Existing Phase 2 deployments unaffected except shared Flyway execution order.

## Unresolved matters

- Target message broker (Redis streams, SNS/SQS, Kafka, or managed equivalent) — Phase 4+ infrastructure decision.
- Outbox worker deployment topology (embedded poller vs dedicated service) — pending load analysis.
- Maximum retry policy and dead-letter operational runbook — pending SRE input.
- Whether CDC will supplement polling for analytics — under evaluation.

## Related documents

- [Transactional Outbox Standard](../architecture/TRANSACTIONAL_OUTBOX_STANDARD.md)
- [Transaction Boundary Standard](../architecture/TRANSACTION_BOUNDARY_STANDARD.md)
- [Optimistic Locking Standard](../architecture/OPTIMISTIC_LOCKING_STANDARD.md)
- [Data Retention and Partitioning Standard](../operations/DATA_RETENTION_AND_PARTITIONING_STANDARD.md)
