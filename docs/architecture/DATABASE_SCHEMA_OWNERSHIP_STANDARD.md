# Database Schema Ownership Standard

**Status:** Current architectural decision (Phase 4 implementation)
**Phase:** 4 — KS Number identity issuance
**Branch:** `phase-04-ksnumber-identity-issuance`
**Module:** `shared/platform-persistence`, `shared/platform-identity`

## Purpose

Define how PostgreSQL schemas and tables are owned, migrated, and accessed so that cross-cutting persistence foundations remain maintainable as business domains are added in later phases.

## Schema registry (Phase 3–4)

| Schema | Owner | Migration authority | Application module |
| --- | --- | --- | --- |
| `platform` | `securepay-core` | `database/migrations` via Flyway on `securepay-core` startup | `shared/platform-persistence` |
| `audit` | `securepay-core` | Same | `shared/platform-persistence` |
| `events` | `securepay-core` | Same | `shared/platform-persistence` |
| `idempotency` | `securepay-core` | Same | `shared/platform-persistence` |
| `identity` | `securepay-core` | Same | `shared/platform-identity` |

Future business schemas (e.g. `ledger`, `evidence`) require a dedicated ADR before any migration merges.

## Table inventory (Phase 3–4)

| Schema | Table | Mutability | Purpose |
| --- | --- | --- | --- |
| `platform` | `platform_metadata` | Update allowed | Platform phase and configuration keys |
| `platform` | `technical_test_records` | Insert-only (Phase 3) | Integration test scaffolding |
| `audit` | `audit_events` | Append-only | Immutable audit trail |
| `events` | `outbox_events` | Insert + status updates | Transactional outbox |
| `idempotency` | `idempotency_records` | Insert + status updates | Command idempotency |
| `identity` | `ks_identities` | Insert + status updates | Canonical KS Number identities |
| `identity` | `ks_number_aliases` | Insert + status updates | Memorable aliases |

**Phase 4 migration:** `V20260723130000__ks_identity_foundation.sql`

**Explicit:** Phase 3 introduced no business-domain tables; Phase 4 adds identity only.

## Persistence technology

All access in `shared/platform-persistence` uses **Spring Data JDBC**:

- `NamedParameterJdbcTemplate` for explicit SQL
- Java records for row mapping (`AuditEventRecord`, `OutboxEventRecord`, `IdempotencyRecord`)
- `@Repository` and `@Service` components—**not** JPA entities or `@Entity` mappings

Services depend on `platform-persistence` auto-configuration (`PlatformPersistenceAutoConfiguration`) rather than embedding SQL.

## Migration rules

| Rule | Requirement |
| --- | --- |
| Location | `database/migrations/` only |
| Naming | `V{UTC_TIMESTAMP}__{snake_case_description}.sql` |
| Phase 3 migration | `V20260723090000__phase_03_technical_foundations.sql` |
| Phase 4 migration | `V20260723130000__ks_identity_foundation.sql` |
| Immutability | Accepted migrations are never edited |
| Phase 2 relocation | `public.platform_metadata` → `platform.platform_metadata` when present |

See [Database Migration Standard](../operations/DATABASE_MIGRATION_STANDARD.md).

## Cross-schema transactions

Phase 3 demonstrates a single transaction spanning `platform`, `audit`, and `events` in `TechnicalFoundationService.recordTechnicalCreation`. Any service combining business writes with audit or outbox must follow [Transaction Boundary Standard](TRANSACTION_BOUNDARY_STANDARD.md).

## Naming conventions

| Object | Convention | Example |
| --- | --- | --- |
| Schema | Lowercase singular concern | `audit`, `events` |
| Table | Lowercase plural snake_case | `audit_events`, `outbox_events` |
| Index | `idx_{table}_{columns}` | `idx_audit_events_correlation_id` |
| Unique index | `uq_{purpose}` | `uq_idempotency_scope` |
| Trigger | `trg_{table}_{purpose}` | `trg_audit_events_no_delete` |

## Adding a new schema or table

1. Propose ADR with owner, mutability model, and role grants.
2. Add forward migration; do not alter accepted migrations.
3. Implement repository in owning module or approved shared module.
4. Add integration tests (Flyway + Testcontainers pattern in `securepay-core`).
5. Update this standard and phase completion report.

## Phase 3–4 exclusions

- No agreement, ledger, or evidence tables (identity excepted in Phase 4)
- No Control Centre direct database access ([ADR-0005](../decisions/ADR-0005-CONTROL-CENTRE-NO-DIRECT-DATABASE-ACCESS.md))
- No PostgreSQL role provisioning in local Docker (documented for future environments)

## Related documents

- [ADR-0007 Database schema ownership](../decisions/ADR-0007-DATABASE-SCHEMA-OWNERSHIP.md)
- [Database Access Control Standard](../security/DATABASE_ACCESS_CONTROL_STANDARD.md)
- [Transaction Boundary Standard](TRANSACTION_BOUNDARY_STANDARD.md)
- [KS Identity Domain Standard](KS_IDENTITY_DOMAIN_STANDARD.md)
- [ADR-0012 KS Number identity model](../decisions/ADR-0012-KS-NUMBER-IDENTITY-MODEL.md)
