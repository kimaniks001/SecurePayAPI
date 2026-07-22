# Phase 2: Java, Spring Boot, and Gradle Kotlin DSL

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-22 |
| **Phase** | 2 |

## Context

Phase 1 established doctrine, contracts, and validation without executable services. Phase 2 requires a professional, horizontally scalable backend foundation with strong ecosystem support for transactions, security, observability, and testing.

The platform must support modular deployable services (`securepay-core`, `financial-ledger`, `choice-bank-connector`, etc.) while sharing technical primitives without uncontrolled coupling.

## Decision

Adopt the following **locked implementation stack** for SecurePay backend services:

| Component | Choice |
| --- | --- |
| Language | **Java 21** (LTS) |
| Framework | **Spring Boot 3.4.x** |
| Build | **Gradle** with **Kotlin DSL** and version catalog |
| System of record | **PostgreSQL 16** (ADR-0003) |
| Migrations | **Flyway** |
| Cache | **Redis-compatible** via Spring Data Redis |
| JSON | **Jackson** |
| Validation | **Jakarta Bean Validation** |
| Observability | **Spring Boot Actuator** (restricted), structured JSON logging |
| Testing | **JUnit 5**, **Testcontainers**, **ArchUnit** |
| Contracts | OpenAPI 3.1 under `contracts/openapi` |
| Local execution | **Docker** and **Docker Compose** |

Dependency versions are pinned in `gradle/libs.versions.toml`. Snapshot, milestone, RC, and unpinned versions are prohibited.

## Rationale

### Java 21

- Modern LTS with performance and language improvements
- Strong ecosystem for financial and enterprise systems
- Aligns with Spring Boot 3.x baseline

### Spring Boot

- Mature transaction management for PostgreSQL
- Integrated Flyway, Redis, Actuator, validation, and graceful shutdown
- Security ecosystem for future authentication phases
- Horizontal scaling via stateless service design

### Gradle Kotlin DSL

- Type-safe build scripts for multi-module monorepo
- Version catalog centralizes pinned dependencies
- Gradle Wrapper ensures reproducible CI and local builds

### Modular architecture compatibility

- `shared/platform-*` modules provide approved cross-cutting primitives
- Service modules compile independently with explicit dependency rules
- `financial-ledger` and `choice-bank-connector` remain isolated per ADR-0002 and ADR-0004

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Node.js / TypeScript | Weaker transactional accounting ecosystem for ledger phase |
| Kotlin + Spring | Valid; Java 21 chosen for broader enterprise familiarity and doctrine-test simplicity in Phase 2 |
| Maven | Acceptable; Gradle Kotlin DSL preferred for multi-module version catalog |
| Micronaut / Quarkus | Smaller SecurePay team familiarity; Spring ecosystem match for Choice integration patterns |

## Consequences

### Positive

- Single coherent stack for all backend services
- Testcontainers enables repeatable PostgreSQL/Redis integration tests in CI
- ArchUnit enforces module boundaries early

### Negative

- JVM memory footprint higher than lightweight runtimes
- Spring Boot startup time requires readiness probes and graceful shutdown discipline

## Security impact

- Pinned dependencies reduce supply-chain drift
- Actuator restricted to internal management port
- No Spring Security business authentication in Phase 2 (health endpoints only)

## Operational impact

- Docker multi-stage builds for `securepay-core`
- JSON structured logs with request/correlation IDs
- Flyway forward-only migrations (see `DATABASE_MIGRATION_STANDARD.md`)

## Unresolved matters

- Whether future `notification-service` adopts reactive stack for provider SDKs — deferred to implementation phase
- Exact production JVM container base image hardening — infrastructure phase input

## Related documents

- [Master Architecture](../architecture/SECUREPAY_MASTER_ARCHITECTURE.md)
- [ADR-0002 Modular platform boundaries](ADR-0002-MODULAR-PLATFORM-BOUNDARIES.md)
- [ADR-0003 PostgreSQL system of record](ADR-0003-POSTGRESQL-SYSTEM-OF-RECORD.md)
- [Phase 02 Completion Report](../operations/PHASE_02_COMPLETION_REPORT.md)
