# Local Development Guide

**Phase:** 2 — executable platform skeleton

## Required software

| Tool | Version |
| --- | --- |
| Java | 21 (Temurin recommended) |
| Docker | Latest stable with Compose v2 |
| Git | Latest stable |

Gradle is **not** required globally — use the Gradle Wrapper (`./gradlew`).

## Quick start

```bash
cp .env.example .env
docker compose --env-file .env.example up --build
```

Health endpoints (default):

| Endpoint | URL |
| --- | --- |
| Liveness | http://localhost:8080/health/live |
| Readiness | http://localhost:8080/health/ready |
| Dependencies | http://localhost:8080/health/dependencies |

Internal Actuator (management port): http://localhost:8081/internal/actuator

## Build without Docker

```bash
# Start dependencies only
docker compose --env-file .env.example up -d postgres redis

# Run application locally
./gradlew :services:securepay-core:bootRun --args='--spring.profiles.active=local'
```

## Test

```bash
./gradlew test
./gradlew integrationTest
./gradlew doctrineTest
```

| Task | Purpose |
| --- | --- |
| `test` | Unit tests only |
| `integrationTest` | Testcontainers PostgreSQL + Redis tests |
| `doctrineTest` | ArchUnit architecture rules |

Set `SECUREPAY_REQUIRE_TESTCONTAINERS=true` to fail when Docker is unavailable (CI always sets this). Locally without Docker, `integrationTest` skips with a reported reason.

## Validate

```bash
bash scripts/run_all_validations.sh
docker compose --env-file .env.example config --quiet
```

## Stop

```bash
docker compose --env-file .env.example down
```

## Docker usage (Phase 2)

Redis is configured for **temporary operational data only**. It is **not** authoritative for balances, agreement state, KS Numbers, audit history, or reconciliation.

### Dependency health status (public JSON)

| Value | Meaning |
| --- | --- |
| `healthy` | Dependency reachable |
| `degraded` | Partial or in-progress (e.g. application startup) |
| `unavailable` | Dependency not reachable |

Internal enums use `HEALTHY`, `DEGRADED`, `UNAVAILABLE`. The label `unhealthy` is not used in public responses.

## Secrets

Never commit `.env` or real credentials. `.env.example` contains fake local placeholders only.

## Related documents

- [README](../../README.md)
- [Database Migration Standard](DATABASE_MIGRATION_STANDARD.md)
- [Request Correlation ID Standard](../architecture/REQUEST_CORRELATION_ID_STANDARD.md)
