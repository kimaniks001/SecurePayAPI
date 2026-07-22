# Phase 02 Completion Report

**Objective:** Turn Phase 1 foundation into a runnable, testable backend platform skeleton.

**Branch:** `phase-02-executable-platform-skeleton`
**Date:** 2026-07-22
**Status:** Committed — Phase 2 executable platform skeleton

## Implementation stack

| Component | Version |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.4.4 |
| Gradle | 8.12.1 (Kotlin DSL) |
| PostgreSQL | 16 |
| Flyway | 10.20.1 |
| Redis | 7 (Spring Data Redis) |
| Testcontainers | 1.20.6 |
| ArchUnit | 1.4.0 |

## Health dependency vocabulary (corrected)

| Internal enum | Public JSON |
| --- | --- |
| `HEALTHY` | `healthy` |
| `DEGRADED` | `degraded` |
| `UNAVAILABLE` | `unavailable` |

The deprecated public label `unhealthy` was removed from OpenAPI, DTO mappings, tests, and documentation. Contract test `DependencyStatusContractTest` enforces the vocabulary.

## Modules created

### Shared

| Module | Purpose |
| --- | --- |
| `shared/platform-common` | IDs, time, foundation error codes |
| `shared/platform-web` | Request/correlation filters, envelopes, exception handling |
| `shared/platform-observability` | Correlation context, log fields, redaction |
| `shared/platform-testing` | Testcontainers helpers + `SECUREPAY_REQUIRE_TESTCONTAINERS` policy |

### Services

| Module | Phase 2 status |
| --- | --- |
| `services/securepay-core` | Executable Spring Boot application |
| `services/financial-ledger` | Compiling skeleton |
| `services/choice-bank-connector` | Compiling skeleton |
| `services/evidence-service` | Compiling skeleton |
| `services/notification-service` | Compiling skeleton |
| `services/webhook-service` | Compiling skeleton |

### Other

| Module | Purpose |
| --- | --- |
| `applications/control-centre` | Non-executable placeholder |
| `testing/doctrine` | ArchUnit doctrine tests |

## Testcontainers policy

| Environment | Behaviour |
| --- | --- |
| Local (no Docker) | `integrationTest` **skips** with clear reason |
| Local (`SECUREPAY_REQUIRE_TESTCONTAINERS=true`) | **Fails** if Docker unavailable |
| GitHub Actions | `SECUREPAY_REQUIRE_TESTCONTAINERS=true` — integration tests **never silently skip** |

Gradle tasks (explicit in CI):

```bash
./gradlew test
./gradlew integrationTest
./gradlew doctrineTest
```

Integration tests live in `services/securepay-core/src/integrationTest/java`.

## Architecture decisions

- [ADR-0006 Java Spring Boot Gradle](../decisions/ADR-0006-JAVA-SPRING-BOOT-GRADLE.md) — locked implementation stack

## Application behaviour

- Typed configuration with startup validation (`SecurePayProperties`)
- PostgreSQL via HikariCP + Flyway
- Redis connectivity check (not authoritative for business state)
- Structured JSON logging with request/correlation IDs
- Graceful shutdown (30s timeout)
- Actuator on port 8081 under `/internal/actuator` (restricted exposure)

## Endpoints implemented

| Method | Path | Description |
| --- | --- | --- |
| GET | `/health/live` | Process alive |
| GET | `/health/ready` | PostgreSQL + Redis + startup ready |
| GET | `/health/dependencies` | Safe dependency summary (`healthy` / `degraded` / `unavailable`) |

## Database migration

| File | Purpose |
| --- | --- |
| `database/migrations/V20260722100000__platform_foundation.sql` | `platform_metadata` table only |

## Testing performed

| Suite | Task | Tests |
| --- | --- | --- |
| Unit | `test` | `DependencyStatusContractTest`, `SecurepayCoreApplicationTest`, `SecurePayPropertiesBindingTest` |
| Integration | `integrationTest` | `SecurepayCoreContextTest`, `HealthEndpointIntegrationTest` (PostgreSQL + Redis Testcontainers) |
| Doctrine | `doctrineTest` | `ArchitectureDoctrineTest` (6 rules) |
| Skeleton | `test` | `LedgerModuleTest`, `ChoiceConnectorModuleTest` |

## Exact commands (local)

```bash
./gradlew clean test
./gradlew integrationTest
./gradlew doctrineTest
bash scripts/run_all_validations.sh
docker compose --env-file .env.example config --quiet
```

## Exact commands (GitHub Actions — Phase 2 workflow)

```bash
bash scripts/run_all_validations.sh
./gradlew wrapper --gradle-version 8.12.1 --dry-run
./gradlew clean test --no-daemon
SECUREPAY_REQUIRE_TESTCONTAINERS=true ./gradlew integrationTest --no-daemon
./gradlew doctrineTest --no-daemon
docker compose --env-file .env.example config --quiet
docker compose --env-file .env.example up --build -d
curl --fail http://localhost:8080/health/live
curl --fail http://localhost:8080/health/ready
curl --fail http://localhost:8080/health/dependencies
docker compose --env-file .env.example down --volumes
```

## Validation results (local agent environment — 2026-07-22 revalidation)

| Check | Result |
| --- | --- |
| `./gradlew clean test` | **PASS** — 4 unit tests + 6 skeleton tests (securepay-core + service skeletons) |
| `./gradlew integrationTest` (no Docker) | **SKIPPED** — 7 tests skipped with reason: *Docker is not available; skipping integration tests…* |
| `SECUREPAY_REQUIRE_TESTCONTAINERS=true ./gradlew integrationTest` (no Docker) | **FAIL** (expected) — 2 initialization errors: *SECUREPAY_REQUIRE_TESTCONTAINERS=true but Docker is not available* |
| `./gradlew doctrineTest` | **PASS** — 6 ArchUnit rules |
| `bash scripts/run_all_validations.sh` | **PASS** — required files, markdown links, doctrine, secret scan, OpenAPI, JSON Schema, Compose config |
| `docker compose --env-file .env.example config --quiet` | **PASS** |
| `docker compose --env-file .env.example up --build` | **NOT RUN** — Docker daemon/socket unavailable locally (`/var/run/docker.sock` missing) |
| `python3 scripts/scan_secrets.py` | **PASS** — no suspicious patterns |

### Warnings

- Gradle reports deprecated features incompatible with Gradle 9.0 (existing plugin usage; non-blocking).
- Parallel `./gradlew doctrineTest` immediately after `./gradlew clean test` can race on compile stash files; run sequentially in CI (workflow already does).

## CI responsibilities

GitHub Actions is responsible for:

1. **Mandatory Testcontainers** — `SECUREPAY_REQUIRE_TESTCONTAINERS=true` on `integrationTest`
2. **Compose runtime validation** — `docker compose up --build`, curl health endpoints, verify `healthy`/`degraded`/`unavailable` vocabulary, cleanup with `down --volumes`
3. On failure: `docker compose ps` and `docker compose logs --no-color` (no secrets in compose file)

Local development without Docker may skip integration tests only with a clear reported reason. **CI must never silently skip.**

## CI changes

[`.github/workflows/phase-2-validation.yml`](../../.github/workflows/phase-2-validation.yml):

- Phase 1 validation suite
- `./gradlew clean test`
- `./gradlew integrationTest` with `SECUREPAY_REQUIRE_TESTCONTAINERS=true`
- `./gradlew doctrineTest`
- Docker Compose config validation
- Compose runtime health validation with bounded retry and cleanup

## Security review

- No secrets in source or tests
- Pinned dependencies via version catalog
- Non-root Docker runtime user
- Actuator not exposed on public port
- Sensitive values redacted in logging helpers

## Docker results

| Item | Status |
| --- | --- |
| `services/securepay-core/Dockerfile` | Multi-stage Java 21, non-root user |
| `docker-compose.yml` | PostgreSQL, Redis, securepay-core |
| Local runtime validation | **Unavailable** — Docker daemon not running in agent environment |
| CI runtime validation | **Mandatory** in Phase 2 workflow |

## Risks

| Risk | Mitigation |
| --- | --- |
| Local dev without Docker | Documented skip behaviour; CI enforces Testcontainers |
| Compose startup timing | Bounded 30-attempt retry loop in CI |

## Exclusions (confirmed)

Phase 2 does **not** implement business domains, Choice API calls, or production infrastructure.

## Recommended Phase 3 scope

1. Authentication session model design
2. KS Number schema design (no issuance until approved)
3. Partner credential model
4. Expand doctrine tests as domains are added

## Confirmations

- **No business domains were implemented.**
- **No Choice API calls exist.**
- **No secrets were committed.**
- **Public health vocabulary uses `healthy`, `degraded`, `unavailable` only.**
