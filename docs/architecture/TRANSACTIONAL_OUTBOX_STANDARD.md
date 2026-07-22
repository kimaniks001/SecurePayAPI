# Transactional Outbox Standard

**Status:** Current architectural decision (Phase 3 implementation)  
**Phase:** 3 — database, audit, idempotency foundation  
**Branch:** `phase-03-database-audit-idempotency-foundation`  
**Module:** `shared/platform-persistence`

## Purpose

Ensure domain events are durably recorded in the same database transaction as the state change that produced them, enabling reliable asynchronous delivery to downstream consumers without dual-write inconsistency.

## Storage

| Item | Value |
| --- | --- |
| Schema | `events` |
| Table | `events.outbox_events` |
| Writer | `OutboxWriter` |
| Repository | `OutboxRepository` (Spring Data JDBC) |
| Publisher interface | `OutboxPublisher` (Phase 3: `NoOpOutboxPublisher`) |

## Event envelope

Outbox `payload` JSONB stores a serialized `DomainEventEnvelope` conforming to:

- Java record: `ke.securepay.platform.persistence.events.DomainEventEnvelope`
- JSON Schema: `contracts/events/event-envelope-v1.schema.json`

Required envelope fields propagated to outbox columns where indexed:

| Envelope field | Outbox column |
| --- | --- |
| `event_id` | `event_id` (unique) |
| `event_type` | `event_type` |
| `event_version` | `event_version` |
| `correlation_id` | `correlation_id` |
| `causation_id` | `causation_id` |
| `actor.type` | `actor_type` |
| `actor.id` | `actor_id` |
| `source_service` | `source_service` |

Aggregate routing uses `aggregate_type` and `aggregate_id` columns (Phase 3: `technical_test` / record key).

## Status lifecycle

```
PENDING → PROCESSING → PUBLISHED
                    → FAILED → (retry) → PROCESSING
                    → DEAD_LETTER
```

| Status | Meaning |
| --- | --- |
| `PENDING` | Awaiting worker pickup |
| `PROCESSING` | Worker claimed row; `attempt_count` incremented |
| `PUBLISHED` | Successfully dispatched; `published_at` set |
| `FAILED` | Transient failure; may become available again |
| `DEAD_LETTER` | Exhausted retries; requires manual intervention |

Check constraint on `status` enforces allowed values.

## Write pattern (Phase 3)

Within `@Transactional`:

1. Insert business or technical state (e.g. `platform.technical_test_records`)
2. Append audit event (optional but demonstrated in Phase 3)
3. Insert outbox row with `status = PENDING` via `OutboxWriter.appendTechnicalTestCreated`

Reference implementation: `TechnicalFoundationService.recordTechnicalCreation`.

## Worker pattern (future)

Not implemented in Phase 3. Planned behaviour:

1. Select `PENDING` rows where `available_at <= now()` ordered by `available_at`
2. `markProcessing(id, version)` — optimistic lock
3. Invoke `OutboxPublisher.publish(record)`
4. `markPublished(id, version)` or mark failed/dead letter

Worker identity will use PostgreSQL role `outbox_worker` (see access control standard).

## Optimistic locking

All status transitions increment `version`. See [Optimistic Locking Standard](OPTIMISTIC_LOCKING_STANDARD.md).

## Phase 3 event types

| Event type | Constant | Purpose |
| --- | --- | --- |
| `platform.test.created` | `TechnicalEventTypes.PLATFORM_TEST_CREATED` | Integration validation |

No business-domain event types in Phase 3.

## Causation chain

Phase 3 sets outbox `causation_id` to the audit event's `event_id` when both are written in the same flow, linking audit append to event emission.

## Observability

| Signal | Source |
| --- | --- |
| Row counts by status | `OutboxRepository.countByStatus` |
| Index | `idx_outbox_status_available_at` for worker queries |
| Correlation tracing | `correlation_id` index |

## Retention

Published and dead-letter outbox row retention: **pending legal/operational confirmation**.

`idx_outbox_created_at` supports time-based archival when policy is approved.

## Related documents

- [ADR-0008 Transactional outbox](../decisions/ADR-0008-TRANSACTIONAL-OUTBOX.md)
- [Transaction Boundary Standard](TRANSACTION_BOUNDARY_STANDARD.md)
- [Optimistic Locking Standard](OPTIMISTIC_LOCKING_STANDARD.md)
- [Database Schema Ownership Standard](DATABASE_SCHEMA_OWNERSHIP_STANDARD.md)
- [Data Retention and Partitioning Standard](../operations/DATA_RETENTION_AND_PARTITIONING_STANDARD.md)
