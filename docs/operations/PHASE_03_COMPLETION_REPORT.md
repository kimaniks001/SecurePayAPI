# Phase 03 Completion Report

**Objective:** Establish permanent technical persistence controls (audit, idempotency, transactional outbox, actor context, schema ownership) before business domains are implemented.

**Branch:** `phase-03-database-audit-idempotency-foundation`  
**Date:** 2026-07-23  
**Status:** Implementation complete — awaiting commit approval

## Persistence approach

**Spring Data JDBC** (consistent with Phase 2). Flyway owns all schema changes. No Hibernate/JPA or auto-DDL.

## Database schemas

| Schema | Purpose |
| --- | --- |
| `platform` | Platform metadata and technical test scaffolding |
| `audit` | Append-only immutable audit events |
| `events` | Transactional outbox |
| `idempotency` | Persistent idempotency records |

## Migration

| File | Description |
| --- | --- |
| `database/migrations/V20260723090000__phase_03_technical_foundations.sql` | Creates schemas, tables, indexes, check constraints, audit immutability triggers; relocates `platform_metadata` |

## Technical tables

| Table | Key features |
| --- | --- |
| `platform.platform_metadata` | Platform phase metadata |
| `platform.technical_test_records` | Transactional test state for outbox tests |
| `audit.audit_events` | Append-only; DB triggers block UPDATE/DELETE |
| `idempotency.idempotency_records` | Unique scope index; optimistic locking (`version`) |
| `events.outbox_events` | Event envelope payload; status lifecycle; optimistic locking |

## Constraints and indexes

- Unique: `audit.audit_events.event_id`, `events.outbox_events.event_id`, `idempotency` scope (`uq_idempotency_scope`)
- Check constraints: idempotency/outbox status enums, non-negative versions and attempt counts, timestamp ordering
- Indexes: audit (event_id, resource, actor, correlation_id, occurred_at); outbox (status+available_at, created_at, correlation_id); idempotency (status, expires_at, created_at)
- Triggers: `audit.prevent_audit_mutation()` on UPDATE/DELETE

## Module structure

New: `shared/platform-persistence` — actor context, audit writer, idempotency service, outbox writer, domain event envelope, technical foundation service, metrics.

## Implementation summary

| Area | Implementation |
| --- | --- |
| Actor context | `ActorType`, `ActorContext`, `ActorContextFactory` — SYSTEM and TEST only |
| Audit | `AuditWriter` + `AuditEventRepository` (append only); `AuditPayloadValidator` |
| Idempotency | `IdempotencyService` + `IdempotencyRepository`; SHA-256 request hash; replay and conflict handling |
| Outbox | `OutboxService` + `OutboxRepository`; `NoOpOutboxPublisher`; schema-compatible envelope |
| Optimistic locking | `version` column on idempotency and outbox records; `OptimisticLockException` |
| Transaction boundaries | `@Transactional` on writers and `TechnicalFoundationService` |
| Metrics | `securepay.audit.events.appended`, idempotency counters, outbox pending gauge |

## Tests

| Suite | Location | Coverage |
| --- | --- | --- |
| Unit | `shared/platform-persistence/src/test` | Audit payload validation, event envelope JSON Schema contract |
| Integration | `services/securepay-core/src/integrationTest/.../persistence/` | Flyway migration, audit immutability, idempotency, outbox transactions |
| Integration (relocation) | `FlywayMigrationIntegrationTest.platformMetadataWasRelocatedFromPublicToPlatformSchema` | Queries `information_schema` to prove `platform.platform_metadata` exists, `public.platform_metadata` does not, and `platform_phase` is `phase-03-database-audit-idempotency-foundation` |
| Doctrine | `testing/doctrine` | Extended ArchUnit rules (17) + migration doctrine test |

## Exact validation commands

```bash
./gradlew clean test --no-daemon
./gradlew integrationTest --no-daemon
./gradlew doctrineTest --no-daemon
bash scripts/run_all_validations.sh
docker compose --env-file .env.example config --quiet
python3 scripts/scan_secrets.py
```

With Docker:

```bash
SECUREPAY_REQUIRE_TESTCONTAINERS=true ./gradlew integrationTest --no-daemon
docker compose --env-file .env.example up --build -d
curl --fail http://localhost:8080/health/live
curl --fail http://localhost:8080/health/ready
curl --fail http://localhost:8080/health/dependencies
docker compose --env-file .env.example down --volumes
```

## Validation results (local agent environment)

| Check | Result |
| --- | --- |
| `./gradlew clean test` | **PASS** — unit + contract tests |
| `./gradlew integrationTest` (no Docker) | **SKIPPED** — 29 tests with clear reason (includes explicit `platform_metadata` relocation assertion) |
| `SECUREPAY_REQUIRE_TESTCONTAINERS=true ./gradlew integrationTest` | **FAIL** (expected without Docker) |
| `./gradlew doctrineTest` | **PASS** — 18 doctrine tests |
| `bash scripts/run_all_validations.sh` | **PASS** |
| `docker compose config` | **PASS** |
| `python3 scripts/scan_secrets.py` | **PASS** |
| Compose runtime | **NOT RUN** — Docker daemon unavailable locally |

## CI changes

[`.github/workflows/phase-3-validation.yml`](../../.github/workflows/phase-3-validation.yml):

- Phase 1 validation suite
- Gradle wrapper, unit tests, mandatory integration tests (`SECUREPAY_REQUIRE_TESTCONTAINERS=true`)
- Doctrine tests, secret scan, Compose config, Compose runtime health validation

## Security review

- No secrets committed
- No public audit/idempotency/outbox endpoints
- Audit repository has no update/delete methods; DB triggers enforce immutability
- No business-domain tables
- No Choice Bank HTTP client
- Actor identity not accepted from untrusted headers
- Idempotency keys validated (length/format)
- Event payloads size-controlled via JSONB and validation
- Sensitive keys rejected in audit payloads

## Risks

| Risk | Mitigation |
| --- | --- |
| Local dev without Docker | Documented skip; CI enforces Testcontainers |
| Idempotency response encryption unresolved | Documented in UR-14; plain JSONB for Phase 3 technical responses only |
| No external broker in Phase 3 | NoOp publisher; outbox persisted only |

## Unresolved matters

See [UNRESOLVED_ITEMS_REGISTER.md](UNRESOLVED_ITEMS_REGISTER.md) UR-12 through UR-22.

## Assumptions

- Single local PostgreSQL user acceptable for development; production roles documented only
- Technical test operation `platform.technical.test` sufficient for idempotency validation
- JSON Schema contract validation uses `event-envelope-v1.schema.json` with ISO-8601 timestamps

## Exclusions (confirmed)

Phase 3 does **not** implement KS Numbers, users, authentication, SecureLinks, ledger business logic, payments, Choice API calls, settlements, or Control Centre business functions.

## Recommended Phase 4 scope

1. Authentication session model and trusted actor propagation
2. KS Number schema design (no issuance until approved)
3. First domain-owned schema with ADR approval
4. Outbox publisher integration design

## Confirmations

- **No business domains were implemented.**
- **No Choice Bank API calls exist.**
- **No secrets were committed.**
- **Phase 4 has not started.**
