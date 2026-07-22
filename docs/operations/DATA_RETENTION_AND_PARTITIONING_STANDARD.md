# Data Retention and Partitioning Standard

**Status:** Current architectural decision (Phase 3 scaffolding; retention periods pending confirmation)  
**Phase:** 3 — database, audit, idempotency foundation  
**Branch:** `phase-03-database-audit-idempotency-foundation`

## Purpose

Establish how SecurePay manages growth of technical persistence tables—audit events, outbox events, and idempotency records—through retention policies, archival, and future partitioning strategies.

Phase 3 creates the tables and indexes that support time-based lifecycle operations but **does not implement automated purge or partition management jobs**.

## Classification

| Label | Meaning in this document |
| --- | --- |
| **Pending legal/operational confirmation** | Retention duration not yet approved by legal, compliance, or operations |
| **Future implementation requirement** | Approved approach documented; automation not built in Phase 3 |
| **Phase 3 default** | Engineering default for non-production and integration testing only |

## Table lifecycle summary

| Schema | Table | Growth pattern | Partitioning (Phase 3) | Retention period |
| --- | --- | --- | --- | --- |
| `platform` | `platform_metadata` | Bounded keys | None | Indefinite (configuration) |
| `platform` | `technical_test_records` | Test-driven | None | **Phase 3 default:** manual truncate in dev/test |
| `audit` | `audit_events` | Append-only monotonic | None (indexes on `occurred_at`) | **Pending legal/operational confirmation** |
| `events` | `outbox_events` | Insert + status updates | None (indexes on `created_at`) | **Pending legal/operational confirmation** |
| `idempotency` | `idempotency_records` | Insert + updates until terminal | None (indexes on `expires_at`, `created_at`) | **Pending legal/operational confirmation** |

**Explicit:** No retention period in this document is approved for production compliance until legal and operations sign off.

## Retention design principles

1. **Audit data** is assumed long-lived until legal specifies minimum retention and permitted deletion/archival windows.
2. **Idempotency data** serves replay protection; retention must exceed maximum client retry window (Phase 3 engineering default: 24 hours expiry on new records).
3. **Outbox published events** may be purged or archived after successful downstream delivery confirmation once broker idempotency is proven.
4. **Dead-letter outbox rows** retain until manual resolution; retention **pending operational runbook confirmation**.
5. Purge jobs must **never** delete `audit.audit_events` rows unless explicit legal approval and compensating archive exist.

## Phase 3 engineering defaults (non-production)

| Data | Default handling |
| --- | --- |
| `technical_test_records` | Safe to truncate in local/integration environments between test runs |
| Idempotency records | Expire after 24 hours (`expires_at`); no automated purge job |
| Outbox events | Accumulate; `NoOpOutboxPublisher` leaves rows in `PENDING` for inspection |
| Audit events | Accumulate; append-only |

These defaults are **not** production retention commitments.

## Index support for future purge

Phase 3 migration creates indexes to support time-scoped operations:

| Table | Index | Future use |
| --- | --- | --- |
| `audit.audit_events` | `idx_audit_events_occurred_at` | Time-range archive/export |
| `events.outbox_events` | `idx_outbox_created_at` | Purge published rows older than threshold |
| `idempotency.idempotency_records` | `idx_idempotency_expires_at` | Delete expired records |
| `idempotency.idempotency_records` | `idx_idempotency_created_at` | Operational reporting |

## Partitioning strategy (future)

**Status:** Future implementation requirement — **pending legal/operational confirmation** for audit; engineering evaluation for outbox and idempotency.

Recommended direction when volume warrants:

| Table | Partition key candidate | Strategy |
| --- | --- | --- |
| `audit.audit_events` | `occurred_at` | Monthly range partitions |
| `events.outbox_events` | `created_at` | Monthly range partitions; detach published partitions for archive |
| `idempotency.idempotency_records` | `created_at` | Monthly range partitions with post-expiry drop |

Partitioning requires:

1. ADR approval
2. Migration to convert existing table or create partitioned successor
3. Query updates to include partition key in hot paths where required
4. Coordinated purge/archive jobs

Phase 3 tables are **not** partitioned.

## Archival

Before any production purge of audit or financial-adjacent data:

1. Legal approval of retention schedule
2. Encrypted archive to approved object storage
3. Manifest with row counts and checksums
4. Verification restore test on archive sample

Archive format and storage location: **pending legal/operational confirmation**.

## Operational monitoring (future)

| Metric | Alert threshold |
| --- | --- |
| `audit.audit_events` row count / disk | Infrastructure-defined |
| `events.outbox_events` where `status = PENDING` | Backlog growth |
| `events.outbox_events` where `status = DEAD_LETTER` | Any sustained increase |
| `idempotency.idempotency_records` where `expires_at < now()` | Pre-purge backlog |

## Environment differences

| Environment | Expectation |
| --- | --- |
| Local Docker | Developers may reset volumes freely |
| CI Testcontainers | Ephemeral; no retention |
| Staging | Mirror production policy when approved |
| Production | No purge until retention ADR and runbooks approved |

## Related documents

- [ADR-0010 Immutable audit events](../decisions/ADR-0010-IMMUTABLE-AUDIT-EVENTS.md)
- [ADR-0008 Transactional outbox](../decisions/ADR-0008-TRANSACTIONAL-OUTBOX.md)
- [ADR-0009 Idempotency persistence](../decisions/ADR-0009-IDEMPOTENCY-PERSISTENCE.md)
- [Audit Event Standard](../security/AUDIT_EVENT_STANDARD.md)
- [Transactional Outbox Standard](../architecture/TRANSACTIONAL_OUTBOX_STANDARD.md)
- [Idempotency Standard](../architecture/IDEMPOTENCY_STANDARD.md)
- [Database Access Control Standard](../security/DATABASE_ACCESS_CONTROL_STANDARD.md)
