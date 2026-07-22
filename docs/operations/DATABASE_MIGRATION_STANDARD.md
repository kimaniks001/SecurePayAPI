# Database Migration Standard

**Status:** Locked doctrine
**Phase:** 2 — foundation

## Ownership

| Area | Owner module |
| --- | --- |
| Platform foundation metadata | `securepay-core` (Phase 2) |
| Ledger schema | `financial-ledger` (future) |
| Evidence metadata | `evidence-service` (future) |

Each service owns migrations for tables it alone writes. Shared tables require ADR approval.

## Naming convention

```
V{UTC_TIMESTAMP}__{snake_case_description}.sql
```

Example: `V20260722100000__platform_foundation.sql`

## Rules

| Rule | Classification |
| --- | --- |
| Forward-only in production | **Locked doctrine** |
| No manual production table edits | **Locked doctrine** |
| No destructive amendment of applied migrations | **Locked doctrine** |
| Corrections via new forward migration | **Locked doctrine** |
| Migrations immutable after acceptance | **Locked doctrine** |
| Schema changes via `database/migrations` only | **Locked doctrine** |

## Rollback strategy

Production rollback is **application rollback**, not migration deletion. Data corrections use **new forward migrations** or compensating scripts reviewed under change control.

## Review requirements

1. Migration reviewed in pull request
2. Doctrine tests and integration tests pass
3. No business-domain tables without phase approval
4. Documented in phase completion report

## Execution expectations

| Environment | Execution |
| --- | --- |
| Local | Flyway on `securepay-core` startup against Docker PostgreSQL |
| Test | Testcontainers PostgreSQL per test suite |
| Staging | Controlled deployment pipeline before production |
| Production | Pipeline-managed; least-privilege DB role |

## UTC and timestamps

All `TIMESTAMPTZ` columns use UTC storage. Client display conversion is a client responsibility.

## Phase 2 migration

`V20260722100000__platform_foundation.sql` creates `platform_metadata` only. No business tables.

## Related documents

- [ADR-0003 PostgreSQL system of record](../decisions/ADR-0003-POSTGRESQL-SYSTEM-OF-RECORD.md)
- [Financial Ledger Doctrine](../domains/FINANCIAL_LEDGER_DOCTRINE.md)
