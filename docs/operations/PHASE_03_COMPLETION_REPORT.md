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
| Integration (infrastructure) | `IntegrationTestInfrastructureTest` | Verifies single PostgreSQL/Redis Testcontainers, datasource/Redis connectivity, and classpath event schema availability |
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

## CI correction (post-initial PR #3 failure)

### Original CI failures (run 29914667506)

| Failure | Symptom |
| --- | --- |
| Duplicate Spring bean | `BeanDefinitionOverrideException` during integration-test context startup (`IllegalStateException` in all persistence/health tests) |
| Event schema path | `NoSuchFileException` in `OutboxIntegrationTest` `@BeforeAll` when loading `contracts/events/event-envelope-v1.schema.json` from the process working directory |
| Workflow visibility | Doctrine, secret scan, and Compose checks were skipped after integration-test failure in a single job |

### Root cause: duplicate `clockProvider` bean

| Field | Value |
| --- | --- |
| Duplicate bean name | `clockProvider` |
| First definition | `PlatformWebAutoConfiguration.clockProvider()` (`shared/platform-web`) |
| Second definition | `PlatformPersistenceAutoConfiguration.clockProvider()` (`shared/platform-persistence`, added in Phase 3) |
| Imported by integration tests | All `@SecurePayIntegrationTest` classes load the full `securepay-core` application context, which activates both auto-configurations via `spring.factories` / `AutoConfiguration.imports` |
| Why integration context only | Phase 3 added `platform-persistence` to `securepay-core`; both `platform-web` and `platform-persistence` auto-configurations register a `clockProvider` `@Bean`. Spring Boot rejects the override by default. Unit tests that do not start the full application context were unaffected. |

**Correction:** Removed the duplicate `clockProvider` bean from `PlatformPersistenceAutoConfiguration`. The `clock` bean now depends on the existing `ClockProvider` supplied by `PlatformWebAutoConfiguration`.

### Root cause: event schema working-directory dependency

`OutboxIntegrationTest` and `DomainEventEnvelopeContractTest` used `Files.readString(Path.of("contracts/events/..."))`, which only worked when the Gradle working directory was the repository root. CI executes from the module context, so the file was not found.

**Correction:**

- Added `EventEnvelopeSchemaSupport` in `shared/platform-testing` to load `contracts/events/event-envelope-v1.schema.json` from the test classpath via `ClassPathResource`.
- Gradle `processTestResources` / `processIntegrationTestResources` copy the authoritative contract file from `contracts/events/event-envelope-v1.schema.json` into test resources at build time (no manually maintained duplicate).
- `IntegrationTestInfrastructureTest.eventEnvelopeSchemaIsAvailableOnClasspath` fails clearly if the resource is missing.

### Testcontainers configuration consolidation

| Before | After |
| --- | --- |
| Each integration test class repeated `@SpringBootTest`, `@ActiveProfiles("test")`, `@Import(IntegrationTestContainersConfig.class)` | `@SecurePayIntegrationTest` meta-annotation imports `IntegrationTestContainersConfig` exactly once per class |
| No infrastructure assertion | `IntegrationTestInfrastructureTest` asserts one `postgresContainer`, one `redisContainer`, working `DataSource`, and `RedisConnectionFactory` |

`IntegrationTestContainersConfig` remains the single source for PostgreSQL 16 and Redis 7 `@ServiceConnection` beans.

### CI workflow structure (updated)

[`.github/workflows/phase-3-validation.yml`](../../.github/workflows/phase-3-validation.yml) now runs independent jobs:

| Job | Purpose |
| --- | --- |
| `foundation-validation` | Phase 1 validation suite + Gradle wrapper check |
| `unit-tests` | `./gradlew clean test` |
| `integration-tests` | Mandatory Testcontainers (`SECUREPAY_REQUIRE_TESTCONTAINERS=true`); uploads reports on failure |
| `doctrine-tests` | `./gradlew doctrineTest` |
| `secret-scan` | `python3 scripts/scan_secrets.py` |
| `compose-config` | `docker compose config` |
| `compose-runtime` | Compose health validation (depends on `unit-tests` for build) |
| `phase-3-complete` | Gate job requiring all mandatory jobs to pass |

Doctrine, secret scan, and Compose config now report independently even when integration tests fail.

### Integration test inventory (corrected)

| Metric | Count |
| --- | --- |
| Test classes | 7 |
| Test methods | 32 |
| Prior CI executed count (24) | Lower because Spring context failed before most test methods ran; `OutboxIntegrationTest` failed in `@BeforeAll` (1 error, 5 methods not reached) |

## Validation results (local agent environment)

| Check | Result |
| --- | --- |
| `./gradlew clean test` | **PASS** — unit + contract tests (classpath schema loading verified) |
| `./gradlew integrationTest` (no Docker) | **SKIPPED** — 32 tests with clear reason |
| `SECUREPAY_REQUIRE_TESTCONTAINERS=true ./gradlew integrationTest` | **FAIL** (expected without Docker — 7 class `initializationError`s from mandatory policy) |
| `./gradlew doctrineTest` | **PASS** — 18 doctrine tests |
| `bash scripts/run_all_validations.sh` | **PASS** |
| `docker compose config` | **PASS** |
| `python3 scripts/scan_secrets.py` | **PASS** |
| Compose runtime | **NOT RUN** locally — delegated to GitHub Actions `compose-runtime` job |
| Integration scenarios (Flyway, relocation, audit, idempotency, outbox) | **Delegated to GitHub Actions** — requires Docker |

## CI changes

[`.github/workflows/phase-3-validation.yml`](../../.github/workflows/phase-3-validation.yml):

- Independent parallel jobs for foundation validation, unit tests, integration tests, doctrine tests, secret scan, Compose config, and Compose runtime
- Mandatory integration tests (`SECUREPAY_REQUIRE_TESTCONTAINERS=true`) with failure artifact upload
- Final `phase-3-complete` gate requiring all jobs to pass

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
