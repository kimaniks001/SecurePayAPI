# SecurePay API

**Money should follow the agreement.**

SecurePay is an API-first, domain-first agreement and financial platform built by Keyman Oak. It enables versioned agreements (SecureLinks), deterministic Payment Ready evaluation, authoritative ledger accounting, and regulated settlement — accessed by SecurePay clients, Keyman business solutions, institutional partners, and approved external developers.

## Implementation stack (Phase 2+)

| Component | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.4.x |
| Build | Gradle (Kotlin DSL) + version catalog |
| Database | PostgreSQL 16 + Flyway |
| Cache | Redis-compatible (Spring Data Redis) |
| Testing | JUnit 5, Testcontainers, ArchUnit |

See [ADR-0006](docs/decisions/ADR-0006-JAVA-SPRING-BOOT-GRADLE.md).

## Why API-first?

The public API is the stable contract. Web apps, mobile apps, partner platforms, and the Control Centre are **replaceable clients**.

## Planned deployable components

| Service | Phase 2 status |
| --- | --- |
| `securepay-core` | **Executable** — health endpoints only |
| `financial-ledger` | Compiling skeleton |
| `choice-bank-connector` | Compiling skeleton |
| `evidence-service` | Compiling skeleton |
| `notification-service` | Compiling skeleton |
| `webhook-service` | Compiling skeleton |
| `securepay-control-centre` | Non-executable placeholder |

**Phase 2 does not implement production business domains.**

## Repository structure

```
securepayAPI/
├── build.gradle.kts          # Root Gradle build
├── gradle/libs.versions.toml   # Pinned dependency versions
├── shared/                     # platform-common, platform-web, etc.
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

## Branch and PR expectations

- Feature branches per phase; **no automatic merge**
- Doctrine tests and CI must pass
- Update completion report and unresolved register each phase

## Phased build strategy

| Phase | Scope |
| --- | --- |
| Phase 1 | Doctrine, contracts, validation foundation |
| **Phase 2** (current) | Executable platform skeleton — health endpoints only |
| Phase 3+ | Domain implementation per ADRs |

## Secrets policy

Never commit production secrets, Choice private keys, or real `.env` files.

## License

Proprietary — Keyman Oak Limited.
