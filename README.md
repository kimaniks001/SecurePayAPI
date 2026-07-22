# SecurePay API

**Money should follow the agreement.**

SecurePay is an API-first, domain-first agreement and financial platform built by Keyman Oak. It enables versioned agreements (SecureLinks), deterministic Payment Ready evaluation, authoritative ledger accounting, and regulated settlement — accessed by SecurePay clients, Keyman business solutions, institutional partners, and approved external developers.

## Implementation stack (Phase 2–3)

| Component | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.4.x |
| Build | Gradle (Kotlin DSL) + version catalog |
| Database | PostgreSQL 16 + Flyway |
| Persistence | Spring Data JDBC (Flyway-owned schema) |
| Cache | Redis-compatible (Spring Data Redis) |
| Testing | JUnit 5, Testcontainers, ArchUnit |

See [ADR-0006](docs/decisions/ADR-0006-JAVA-SPRING-BOOT-GRADLE.md).

## Phase 3 technical foundations

Phase 3 adds permanent persistence controls in PostgreSQL (not Redis):

- Schema ownership (`platform`, `audit`, `events`, `idempotency`)
- Immutable audit events
- Persistent idempotency records
- Transactional outbox
- Technical actor context (SYSTEM and TEST only)
- Optimistic locking on mutable technical records

Module: `shared/platform-persistence`. See [Phase 3 completion report](docs/operations/PHASE_03_COMPLETION_REPORT.md).

## Phase 4 KS Number identity

Phase 4 implements the first business domain — canonical KS Number issuance and aliases:

- `identity` schema (`ks_identities`, `ks_number_aliases`, `ks_number_sequence`)
- Sequential issuance starting at `KS001` via PostgreSQL sequence
- Idempotent operation `identity.ks-number.issue`
- Identity and alias lifecycle with audit and outbox events
- Service-layer only — no public identity HTTP endpoints

Module: `shared/platform-identity`. See [Phase 4 completion report](docs/operations/PHASE_04_COMPLETION_REPORT.md).

## Why API-first?

The public API is the stable contract. Web apps, mobile apps, partner platforms, and the Control Centre are **replaceable clients**.

## Planned deployable components

| Service | Phase 2 status |
| --- | --- |
| `securepay-core` | **Executable** — health endpoints only; Phase 3 persistence foundations |
| `financial-ledger` | Compiling skeleton |
| `choice-bank-connector` | Compiling skeleton |
| `evidence-service` | Compiling skeleton |
| `notification-service` | Compiling skeleton |
| `webhook-service` | Compiling skeleton |
| `securepay-control-centre` | Non-executable placeholder |

**Phase 3 does not implement production business domains.**

## Repository structure

```
securepayAPI/
├── build.gradle.kts          # Root Gradle build
├── gradle/libs.versions.toml   # Pinned dependency versions
├── shared/                     # platform-common, platform-web, platform-persistence, etc.
├── services/                   # securepay-core (executable) + skeletons
├── applications/control-centre/  # Placeholder
├── database/migrations/        # Flyway SQL (canonical location)
├── contracts/                    # OpenAPI, events, errors
├── testing/doctrine/             # ArchUnit doctrine tests
└── docs/                         # Doctrine and architecture
```

## Required local software

- **Java 21**
- **Docker** with Compose v2 (for PostgreSQL, Redis, and optional full stack)
- **Python 3.12+** (for Phase 1 validation scripts)

Gradle is provided via `./gradlew` — do not rely on a global Gradle install.

## Build

```bash
./gradlew clean build
```

## Run locally

```bash
cp .env.example .env
docker compose --env-file .env.example up --build
```

Health endpoints:

- http://localhost:8080/health/live
- http://localhost:8080/health/ready
- http://localhost:8080/health/dependencies

See [Local Development Guide](docs/operations/LOCAL_DEVELOPMENT_GUIDE.md).

## Test

```bash
./gradlew test
./gradlew integrationTest
./gradlew doctrineTest
```

Integration tests run via `./gradlew integrationTest` and require Docker when `SECUREPAY_REQUIRE_TESTCONTAINERS=true` (always in CI). Without Docker locally, integration tests skip with a clear reason unless that variable is set.

## Validate

```bash
python3 -m pip install -r scripts/requirements-validation.txt
bash scripts/run_all_validations.sh
docker compose --env-file .env.example config --quiet
```

## GitHub Actions

- [Phase 1 validation](.github/workflows/phase-1-validation.yml) — doctrine, OpenAPI, secrets, compose config
- [Phase 2 validation](.github/workflows/phase-2-validation.yml) — Phase 1 + Gradle build + doctrine tests
- [Phase 3 validation](.github/workflows/phase-3-validation.yml) — Phase 1 + persistence integration tests + compose runtime
- [Phase 4 validation](.github/workflows/phase-4-validation.yml) — Phase 1 + identity integration tests + doctrine + compose runtime

## Branch and PR expectations

- Feature branches per phase; **no automatic merge**
- Doctrine tests and CI must pass
- Update completion report and unresolved register each phase

## Phased build strategy

| Phase | Scope |
| --- | --- |
| Phase 1 | Doctrine, contracts, validation foundation |
| Phase 2 | Executable platform skeleton — health endpoints only |
| Phase 3 | Database, audit, idempotency, outbox foundations |
| **Phase 4** (current) | KS Number identity — issuance, aliases, lifecycle |
| Phase 5+ | Authentication, agreements, ledger per ADRs |

## Secrets policy

Never commit production secrets, Choice private keys, or real `.env` files.

## License

Proprietary — Keyman Oak Limited.
