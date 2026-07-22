# Database Access Control Standard

**Status:** Current architectural decision (Phase 4 design; roles not yet provisioned in local Docker)
**Phase:** 4 — KS Number identity issuance
**Branch:** `phase-04-ksnumber-identity-issuance`

## Purpose

Define least-privilege PostgreSQL roles for SecurePay so that application components, migration tooling, workers, and reporting can access only the schemas and operations they require.

Phase 3 documents the target role model. Production and staging role creation is a **future implementation requirement** coordinated with infrastructure.

## Role registry

| Role | Purpose | Phase 3 status |
| --- | --- | --- |
| `migration_owner` | Flyway DDL execution | Documented; used implicitly via superuser in local Docker |
| `securepay_core_runtime` | Application read/write for platform operations | Documented; local uses single DB user |
| `audit_writer` | Append-only audit inserts | Documented |
| `audit_reader` | Audit investigation and reporting reads | Documented |
| `outbox_worker` | Outbox claim and status updates | Documented |
| `reporting_reader` | Read-only analytics across approved schemas | Documented |

## Privilege matrix (target)

Legend: `C` = CREATE (schema objects), `R` = SELECT, `I` = INSERT, `U` = UPDATE, `D` = DELETE, `—` = no access

### Schema: `identity`

| Role | `ks_identities` | `ks_number_aliases` | `ks_number_sequence` |
| --- | --- | --- | --- |
| `migration_owner` | CRUD + DDL | CRUD + DDL | USAGE, SELECT |
| `securepay_core_runtime` | R, I, U | R, I, U | USAGE, SELECT (`nextval`) |
| `audit_writer` | — | — | — |
| `audit_reader` | — | — | — |
| `outbox_worker` | — | — | — |
| `reporting_reader` | R | R | R (sequence metadata only) |

**Explicit:** `securepay_core_runtime` requires `USAGE` on schema `identity` and `SELECT` + `nextval` on `identity.ks_number_sequence`.

### Schema: `platform`

| Role | `platform_metadata` | `technical_test_records` |
| --- | --- | --- |
| `migration_owner` | CRUD + DDL | CRUD + DDL |
| `securepay_core_runtime` | R, I, U | R, I |
| `audit_writer` | — | — |
| `audit_reader` | — | — |
| `outbox_worker` | — | — |
| `reporting_reader` | R | R |

### Schema: `audit`

| Role | `audit_events` |
| --- | --- |
| `migration_owner` | CRUD + DDL |
| `securepay_core_runtime` | R, I (via `AuditEventWriter`) |
| `audit_writer` | R, I |
| `audit_reader` | R |
| `outbox_worker` | — |
| `reporting_reader` | R (if approved for compliance reports) |

**Explicit:** No role except `migration_owner` receives `UPDATE` or `DELETE` on `audit.audit_events`. Triggers provide defence in depth.

### Schema: `events`

| Role | `outbox_events` |
| --- | --- |
| `migration_owner` | CRUD + DDL |
| `securepay_core_runtime` | R, I |
| `audit_writer` | — |
| `audit_reader` | — |
| `outbox_worker` | R, U (status transitions only) |
| `reporting_reader` | R |

### Schema: `idempotency`

| Role | `idempotency_records` |
| --- | --- |
| `migration_owner` | CRUD + DDL |
| `securepay_core_runtime` | R, I, U |
| `audit_writer` | — |
| `audit_reader` | — |
| `outbox_worker` | — |
| `reporting_reader` | — (unless operational reporting approved) |

## Role descriptions

### migration_owner

- Executes Flyway migrations at deploy time
- Holds DDL privileges on all SecurePay schemas
- **Not** used by running application pods
- Credentials stored in deployment pipeline secrets; short-lived where platform allows

### securepay_core_runtime

- Primary application role for `securepay-core`
- Writes platform technical data, appends audit events, inserts outbox and idempotency rows
- Cannot mutate audit history or run DDL

### audit_writer

- Dedicated insert path if audit writing is split to a sidecar or async processor in future architectures
- Phase 3: same process as `securepay_core_runtime`; separate role reserved for split deployment

### audit_reader

- Control Centre audit API and compliance export jobs (future)
- SELECT on `audit.audit_events` only
- No write access to any schema

### outbox_worker

- Future dedicated outbox dispatcher service or job
- SELECT and UPDATE on `events.outbox_events` only
- No INSERT (application creates events) unless worker design changes via ADR

### reporting_reader

- Read replicas and BI tools
- SELECT on explicitly granted schemas/tables
- Default Phase 3 grant: `platform.*`, `audit.audit_events`, `events.outbox_events` (read-only)
- Phase 4 addition: `identity.ks_identities`, `identity.ks_number_aliases` (read-only) when approved for compliance reports
- Excludes `idempotency` unless fraud/ops ADR approves

## Connection and secrets

| Rule | Requirement |
| --- | --- |
| Production secrets | Approved secrets manager only |
| Git | No credentials in repository |
| Control Centre | No database credentials ([ADR-0005](../decisions/ADR-0005-CONTROL-CENTRE-NO-DIRECT-DATABASE-ACCESS.md)) |
| Rotation | Role passwords rotated on compromise and per infra policy |

## Local development (Phase 3)

Docker Compose PostgreSQL typically uses a single superuser for developer velocity. **Do not** treat local configuration as the production security model.

## Implementation checklist (future)

1. Create roles in staging/production via infrastructure-as-code
2. Grant schema usage: `GRANT USAGE ON SCHEMA {schema} TO {role}`
3. Grant table privileges per matrix above
4. Set `ALTER DEFAULT PRIVILEGES` for migration-created objects
5. Configure `securepay-core` datasource with `securepay_core_runtime`
6. Configure outbox worker datasource with `outbox_worker`
7. Verify audit triggers block mutation for all non-owner roles

## Related documents

- [ADR-0007 Database schema ownership](../decisions/ADR-0007-DATABASE-SCHEMA-OWNERSHIP.md)
- [Database Schema Ownership Standard](../architecture/DATABASE_SCHEMA_OWNERSHIP_STANDARD.md)
- [Audit Event Standard](AUDIT_EVENT_STANDARD.md)
- [SecurePay Security Baseline](SECUREPAY_SECURITY_BASELINE.md)
- [Application–Infrastructure Contract](../handover/APPLICATION_INFRASTRUCTURE_CONTRACT.md)
