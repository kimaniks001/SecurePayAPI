# ADR-0010: Immutable Audit Events

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-22 |
| **Phase** | 3 |
| **Branch** | `phase-03-database-audit-idempotency-foundation` |

## Context

SecurePay security baseline mandates audit logging for identity, agreement, governance, review, security, and financial actions, with audit history that must not be deletable via Control Centre. Phase 3 introduces the audit persistence layer before business domains exist, proving append-only storage and payload safety at the database and application layers.

Regulatory and operational investigations require trustworthy, tamper-evident audit trails. Application-level "do not delete" policies alone are insufficient against privileged database access or application bugs.

## Decision

Store audit events in PostgreSQL table `audit.audit_events` as **append-only immutable records**:

1. **Database enforcement:** Triggers `trg_audit_events_no_update` and `trg_audit_events_no_delete` invoke `audit.prevent_audit_mutation()`, raising an exception on any `UPDATE` or `DELETE`.
2. **Application API:** `AuditEventWriter.append` performs `INSERT` only via `AuditEventRepository`; no update or delete methods exist in Phase 3.
3. **Payload safety:** `AuditPayloadValidator` rejects forbidden keys (`password`, `secret`, `token`, `api_key`, `otp`, etc.) in `previous_state`, `new_state`, and `metadata`.
4. **Correlation:** Every event carries `request_id`, `correlation_id`, and `source_service` from `ActorContext`.
5. **Integrity placeholders:** Columns `integrity_version` and `integrity_hash` reserved for future hash-chain or HMAC schemes; Phase 3 sets version `1` and hash `NULL`.
6. **Temporal constraint:** `occurred_at <= created_at` check constraint prevents backdated insertion beyond clock skew tolerance at write time.
7. **Phase 3 scope:** Category `platform.technical` only (`AuditCategory.PLATFORM_TECHNICAL`); actor types `SYSTEM` and `TEST` only.

Persistence uses **Spring Data JDBC**—not JPA.

## Consequences

### Positive

- Tamper attempts at the SQL layer fail even if application code regresses.
- Uniform audit envelope supports future Control Centre read APIs and compliance export.
- Sanitization prevents accidental secret persistence in audit JSON.

### Negative

- Corrections to erroneous audit entries require compensating append events—not mutation (correct by design).
- Table growth is monotonic until archival/partitioning policies apply.
- Hash-chain integrity not yet active; tamper detection relies on DB permissions and triggers only.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Mutable audit with admin edit | Violates security baseline and regulatory expectations |
| External SIEM as sole audit store | Does not satisfy transactional adjacency with business commits |
| Application-only immutability | Insufficient against direct SQL or compromised credentials |
| WORM object storage only | Poor query patterns for resource and actor investigations |

## Security impact

- Future `audit_writer` role: `INSERT` and `SELECT` on `audit.audit_events` only—no `UPDATE`/`DELETE`.
- Future `audit_reader` role: `SELECT` only for reporting and Control Centre APIs.
- Audit rows may reference resource identifiers; access controlled by role separation from runtime writers.
- Integrity hash implementation deferred but schema prepared.

## Operational impact

- Index coverage: `event_id` (unique), `(resource_type, resource_id)`, `(actor_type, actor_id)`, `correlation_id`, `occurred_at`.
- Metric: `audit.events.appended` counter on successful append.
- Backup strategy must treat `audit` schema as compliance-critical (see retention standard).

## Migration impact

- Migration creates schema `audit`, table `audit.audit_events`, indexes, trigger function, and triggers.
- No historical audit data to migrate in Phase 3.
- Trigger function replacements require new forward migration; cannot alter accepted migration in place.

## Unresolved matters

- Integrity hash algorithm and verification tooling — Phase 4+ security workstream.
- Audit export format for regulators — **pending legal/operational confirmation**.
- Whether read replicas may serve audit queries — infrastructure phase.
- Business audit categories (identity, financial, governance) — defined when domains land.

## Related documents

- [Audit Event Standard](../security/AUDIT_EVENT_STANDARD.md)
- [Actor Context Standard](../security/ACTOR_CONTEXT_STANDARD.md)
- [Database Access Control Standard](../security/DATABASE_ACCESS_CONTROL_STANDARD.md)
- [Data Retention and Partitioning Standard](../operations/DATA_RETENTION_AND_PARTITIONING_STANDARD.md)
