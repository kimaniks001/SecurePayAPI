# securepay-core

**Phase 2:** Executable Spring Boot application (platform skeleton only).

## Runnable service

```bash
./gradlew :services:securepay-core:bootRun --args='--spring.profiles.active=local'
```

Or via Docker Compose: `docker compose --env-file .env.example up --build`

## Phase 2 HTTP endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/health/live` | Process liveness |
| GET | `/health/ready` | Readiness (PostgreSQL + Redis + startup) |
| GET | `/health/dependencies` | Safe dependency summary |

No business endpoints are implemented in Phase 2.

## Dependencies

- PostgreSQL 16 (Flyway migrations from `database/migrations/`)
- Redis-compatible cache (connectivity only — no business caching)

## Ports

| Port | Purpose |
| --- | --- |
| 8080 | Public API (health only in Phase 2) |
| 8081 | Internal Actuator (`/internal/actuator`) |

## Related documents

- [Local Development Guide](../../docs/operations/LOCAL_DEVELOPMENT_GUIDE.md)
- [Application–Infrastructure Contract](../../docs/handover/APPLICATION_INFRASTRUCTURE_CONTRACT.md)
- [ADR-0006 Java Spring Boot Gradle](../../docs/decisions/ADR-0006-JAVA-SPRING-BOOT-GRADLE.md)
