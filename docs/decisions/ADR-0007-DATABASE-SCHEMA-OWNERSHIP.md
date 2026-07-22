# ADR-0007: Database Schema Ownership

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-22 |
| **Phase** | 3 |
| **Branch** | `phase-03-database-audit-idempotency-foundation` |

## Context

Phase 2 introduced a single `platform_metadata` table in the default `public` schema, owned by `securepay-core` migrations. Phase 3 adds cross-cutting persistence foundations—audit, transactional outbox, and idempotency—that multiple services will eventually share.

Without explicit schema ownership, teams risk:

- Unclear migration authority and conflicting DDL changes
- Over-broad database roles granting write access to unrelated tables
- Difficulty enforcing append-only audit and least-privilege access at the PostgreSQL layer

SecurePay requires a durable convention before business-domain tables (identity, agreements, ledger) are introduced in later phases.

## Decision

Adopt **schema-level ownership** in PostgreSQL with four Phase 3 technical schemas:

| Schema | Purpose | Primary writer (Phase 3) |
| --- | --- | --- |
| `platform` | Platform technical metadata and test scaffolding | `securepay-core` via `shared/platform-persistence` |
| `audit` | Append-only immutable audit events | `securepay-core` via `AuditEventWriter` |
| `events` | Transactional outbox for domain events | `securepay-core` via `OutboxWriter` |
| `idempotency` | Persistent idempotency records | `securepay-core` via `IdempotencyService` |

**Rules:**

1. Each schema has a documented owning module or service. Only the owner may introduce forward migrations that create, alter, or drop objects in that schema.
2. Shared persistence primitives live in `shared/platform-persistence` and are consumed by services; schema DDL remains in `database/migrations` executed by `securepay-core` until a dedicated migration runner is approved.
3. Phase 3 creates **no business-domain schemas or tables** (no identity, agreements, ledger, or evidence tables).
4. Schema names are stable identifiers. Business domains added in future phases receive their own schemas (e.g. `identity`, `ledger`) via new ADRs.
5. Application access uses **Spring Data JDBC** (`NamedParameterJdbcTemplate`) with explicit SQL—**not JPA/Hibernate**—in `shared/platform-persistence`.

Phase 3 tables:

- `platform.platform_metadata`
- `platform.technical_test_records`
- `audit.audit_events`
- `events.outbox_events`
- `idempotency.idempotency_records`

## Consequences

### Positive

- Clear migration review scope: changes to `audit.*` are auditable infrastructure changes, not ad hoc application DDL.
- PostgreSQL roles can be scoped per schema in staging and production.
- Shared module code (`platform-persistence`) can evolve without each service duplicating SQL or ownership rules.

### Negative

- Cross-schema transactions require explicit transaction boundary discipline (see `TRANSACTION_BOUNDARY_STANDARD.md`).
- Relocating Phase 2 `public.platform_metadata` into `platform.platform_metadata` requires a one-time migration step for existing environments.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Single `public` schema for all tables | Cannot grant least-privilege roles; audit immutability harder to enforce |
| Schema per microservice from Phase 3 | Premature operational complexity before business domains exist |
| JPA entity-per-table mapping | Conflicts with explicit SQL, optimistic locking control, and append-only audit triggers; ADR-0006 stack uses JDBC for persistence foundations |
| Separate database per concern | Operational overhead; weak cross-table transactional guarantees for outbox and business writes |

## Security impact

- Schema separation enables future PostgreSQL roles with `INSERT`-only on `audit.audit_events` and no `UPDATE`/`DELETE` grants.
- Control Centre and reporting consumers receive read-only roles scoped to approved schemas only (see `DATABASE_ACCESS_CONTROL_STANDARD.md`).
- No business PII is stored in Phase 3 tables; ownership rules apply before sensitive domain data arrives.

## Operational impact

- Flyway migrations remain forward-only; Phase 3 migration `V20260723090000__phase_03_technical_foundations.sql` is immutable after acceptance.
- Backup and restore procedures must include all four schemas.
- Monitoring should tag persistence metrics by schema concern (audit append rate, outbox backlog, idempotency conflicts).

## Migration impact

- **New environments:** Migration creates four schemas and five tables.
- **Existing Phase 2 environments:** Migration relocates `public.platform_metadata` to `platform.platform_metadata` when present, then ensures Phase 3 objects exist.
- **Rollback:** Application rollback only; no migration deletion. Corrections require new forward migrations per `DATABASE_MIGRATION_STANDARD.md`.
- **Downstream services:** No schema access for `financial-ledger`, `choice-bank-connector`, or Control Centre in Phase 3.

## Unresolved matters

- Whether `securepay-core` remains the sole Flyway executor when `financial-ledger` introduces the `ledger` schema — pending Phase 4+ ADR.
- Exact PostgreSQL role creation timing in managed environments — documented as future roles in `DATABASE_ACCESS_CONTROL_STANDARD.md`; not applied in local Docker Compose Phase 3.
- Schema naming for evidence and notification metadata — deferred to respective service phases.

## Related documents

- [Database Schema Ownership Standard](../architecture/DATABASE_SCHEMA_OWNERSHIP_STANDARD.md)
- [Database Migration Standard](../operations/DATABASE_MIGRATION_STANDARD.md)
- [Database Access Control Standard](../security/DATABASE_ACCESS_CONTROL_STANDARD.md)
- [ADR-0003 PostgreSQL system of record](ADR-0003-POSTGRESQL-SYSTEM-OF-RECORD.md)
