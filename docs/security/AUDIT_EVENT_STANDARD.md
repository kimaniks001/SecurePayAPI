# Audit Event Standard

**Status:** Current architectural decision (Phase 3 implementation)  
**Phase:** 3 — database, audit, idempotency foundation  
**Branch:** `phase-03-database-audit-idempotency-foundation`  
**Module:** `shared/platform-persistence`

## Purpose

Define the structure, categories, and safety rules for immutable audit events stored in `audit.audit_events`. Audit events support security investigations, operational forensics, and future compliance reporting.

## Immutability

| Layer | Enforcement |
| --- | --- |
| Database | Triggers block `UPDATE` and `DELETE` on `audit.audit_events` |
| Application | `AuditEventWriter.append` inserts only; no mutation API |
| Control Centre | No direct database access; future read APIs only ([ADR-0005](../decisions/ADR-0005-CONTROL-CENTRE-NO-DIRECT-DATABASE-ACCESS.md)) |

## Write API

```java
AuditEventWriter.append(
    AuditCategory category,
    String eventType,
    ActorContext actor,
    String resourceType,
    String resourceId,
    String action,
    Map<String, Object> previousState,
    Map<String, Object> newState,
    String reason,
    Map<String, Object> metadata)
```

Transactional participation: see [Transaction Boundary Standard](../architecture/TRANSACTION_BOUNDARY_STANDARD.md).

## Phase 3 categories and event types

| Category | Value | Event type (Phase 3) |
| --- | --- | --- |
| Platform technical | `platform.technical` | `platform.technical.test.created` |

Business categories (identity, financial, governance, security) are **not** emitted in Phase 3.

## Actor requirements (Phase 3)

Audit events record `actor_type` and `actor_id` from `ActorContext`. Phase 3 allows only:

- `system` / `system`
- `service` / `test-actor` (TEST actor)

See [Actor Context Standard](ACTOR_CONTEXT_STANDARD.md).

## Required columns

| Column | Description |
| --- | --- |
| `event_id` | Unique business identifier (Phase 3: generated via `IdentifierRules.newRequestId()`) |
| `category` | High-level grouping |
| `event_type` | Specific action classification |
| `resource_type` | Entity type (Phase 3: `technical_test`) |
| `resource_id` | Entity identifier (Phase 3: record key) |
| `action` | Verb (Phase 3: `create`) |
| `request_id` | HTTP or command request ID |
| `correlation_id` | Business correlation ID |
| `source_service` | Originating service name |
| `occurred_at` | When the action occurred (UTC) |
| `created_at` | When the row was persisted (UTC) |

## State capture

| Field | Usage |
| --- | --- |
| `previous_state` | JSON before change; `null` on create |
| `new_state` | JSON after change |
| `reason` | Human-readable justification |
| `metadata` | Supplementary non-state context |

Phase 3 example: `previous_state = null`, `new_state = payload`, `reason = "Phase 3 technical test flow"`.

## Payload safety

`AuditPayloadValidator.sanitize` runs on `previous_state`, `new_state`, and `metadata` before insert.

**Forbidden keys** (case-insensitive on field names):

- `password`, `secret`, `token`, `api_key`, `apikey`, `authorization`, `otp`, `pin`

Nested objects and arrays are validated recursively. Violations throw `IllegalArgumentException` and abort the transaction.

## Integrity fields

| Column | Phase 3 | Future |
| --- | --- | --- |
| `integrity_version` | `1` | Version of hash algorithm |
| `integrity_hash` | `NULL` | Hash chain or HMAC digest |

Integrity verification tooling is **pending** security workstream approval.

## Temporal rules

Check constraint: `occurred_at <= created_at`.

Clock skew between application and database must remain within operational tolerance; both timestamps stored as `TIMESTAMPTZ` in UTC.

## Query patterns

Indexes support investigation by:

- `event_id` (unique lookup)
- `(resource_type, resource_id)` (entity history)
- `(actor_type, actor_id)` (actor activity)
- `correlation_id` (distributed trace)
- `occurred_at` (time range reports)

## Metrics and logging

| Signal | Behaviour |
| --- | --- |
| `audit.events.appended` | Incremented on successful insert |
| Failure | Log `audit_append_failed` without payload contents |

## Retention

Audit event retention period: **pending legal/operational confirmation**.

Until policy is approved, audit data is retained indefinitely in non-production and subject to infrastructure backup policies in production.

## Related documents

- [ADR-0010 Immutable audit events](../decisions/ADR-0010-IMMUTABLE-AUDIT-EVENTS.md)
- [Actor Context Standard](ACTOR_CONTEXT_STANDARD.md)
- [Database Access Control Standard](DATABASE_ACCESS_CONTROL_STANDARD.md)
- [Logging and Redaction Standard](LOGGING_AND_REDACTION_STANDARD.md)
- [Data Retention and Partitioning Standard](../operations/DATA_RETENTION_AND_PARTITIONING_STANDARD.md)
