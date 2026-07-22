# Application–Infrastructure Contract

**Status:** Current architectural decision — reusable handover template  
**Phase:** 1 — template and conventions

## Purpose

Define the contract between SecurePay application services and infrastructure teams. Every deployable service must have a completed instance of this template before production deployment.

**Phase:** 2 — `securepay-core` contract populated

## Completed service contract: `securepay-core`

| Field | Value |
| --- | --- |
| **Purpose** | Platform skeleton; home of KS identity domain (Phase 4); future agreement and governance APIs |
| **Container image** | `services/securepay-core/Dockerfile` (multi-stage, Java 21, non-root) |
| **Ports** | `8080` (public API), `8081` (internal management) |
| **Public exposure** | Health endpoints only in Phase 2 — future APIs via gateway |
| **Private exposure** | Internal VPC / mesh |
| **Liveness endpoint** | `GET /health/live` |
| **Readiness endpoint** | `GET /health/ready` |
| **Dependency-health endpoint** | `GET /health/dependencies` |

#### Environment variables (non-secret)

| Variable | Required | Description |
| --- | --- | --- |
| `ENVIRONMENT` | yes | `local`, `test`, `staging`, `production` |
| `LOG_LEVEL` | yes | Logging verbosity |
| `APP_PORT` | no | HTTP port (default `8080`) |
| `MANAGEMENT_PORT` | no | Actuator port (default `8081`) |
| `POSTGRES_HOST` | yes | PostgreSQL hostname |
| `POSTGRES_PORT` | yes | PostgreSQL port |
| `POSTGRES_DB` | yes | Database name |
| `POSTGRES_USER` | yes | Database user |
| `REDIS_HOST` | yes | Redis hostname |
| `REDIS_PORT` | yes | Redis port |
| `SPRING_PROFILES_ACTIVE` | yes | Active Spring profile |

#### Secrets (never in Git)

| Secret | Required | Description |
| --- | --- | --- |
| `POSTGRES_PASSWORD` | yes | Database password (secrets manager in production) |

#### Data stores

| Store | Required | Ownership |
| --- | --- | --- |
| PostgreSQL | yes | `securepay-core` — schemas `platform`, `audit`, `events`, `idempotency` (Phase 3); `identity` (Phase 4) |
| Redis-compatible cache | yes | Ephemeral operational data only — **not authoritative for idempotency** |
| Object storage | no | Not used in Phase 3 |
| Message queue | no | Outbox persisted in PostgreSQL; external broker deferred |

#### External dependencies

| Dependency | Required | Failure behavior |
| --- | --- | --- |
| Choice Bank BaaS | no (Phase 2) | N/A |
| PostgreSQL | yes | Readiness `503`; no corrupt state |
| Redis | yes | Readiness `503`; no corrupt state |

#### Data classification

| Data type | Classification | Storage |
| --- | --- | --- |
| Platform metadata | Internal | PostgreSQL `platform` schema |
| Audit events | Restricted | PostgreSQL `audit` schema (append-only) |
| Idempotency records | Internal | PostgreSQL `idempotency` schema |
| Outbox events | Internal | PostgreSQL `events` schema |
| KS identities | Internal | PostgreSQL `identity.ks_identities` |
| KS aliases | Internal | PostgreSQL `identity.ks_number_aliases` |
| PII | Confidential | `display_name` optional on identity — classification pending (UR-24) |
| Financial records | Restricted | Not stored in Phase 3 |

#### Scaling and traffic

| Field | Value |
| --- | --- |
| Minimum instances (production input) | ≥ 2 (future infrastructure decision) |
| Horizontal scaling | Stateless HTTP; scale on CPU/RPS |
| Traffic assumptions | Phase 2 health checks only |
| Target RPS | Thousands RPS capability required at platform maturity — not measured in Phase 2 |

#### Resilience

| Field | Value |
| --- | --- |
| Graceful shutdown timeout | 30 seconds (`securepay.shutdown.timeout-seconds`) |
| Retry policy | Idempotent commands only (future financial APIs) |
| Circuit breaker | Provider calls in connector (future) |
| Provider-outage behavior | Must not corrupt SecurePay state |

#### Observability

| Field | Value |
| --- | --- |
| Logs | Structured JSON to stdout |
| Metrics | Micrometer-compatible (Actuator internal); Phase 4 identity counters `securepay.identity.*` |
| Traces | OpenTelemetry (future) |

#### Storage policy

**No permanent local file storage.** Evidence and exports use object storage in future phases.

#### Shared modules (classpath)

| Module | Phase | Responsibility |
| --- | --- | --- |
| `shared/platform-persistence` | 3 | Audit, idempotency, outbox, actor context |
| `shared/platform-identity` | 4 | KS Number issuance, aliases, lifecycle, identity outbox writer |

---

## Service contract template

Copy this section for services not yet deployed.

---

### Service: `<service-name>`

| Field | Value |
| --- | --- |
| **Purpose** | |
| **Container image** | |
| **Ports** | |
| **Public exposure** | `yes` / `no` — if yes, via API gateway only |
| **Private exposure** | Internal service mesh / VPC |
| **Liveness endpoint** | `GET /health/live` |
| **Readiness endpoint** | `GET /health/ready` |
| **Dependency-health endpoint** | `GET /health/dependencies` |

#### Environment variables (non-secret)

| Variable | Required | Description |
| --- | --- | --- |
| `ENVIRONMENT` | yes | `local`, `staging`, `production` |
| `LOG_LEVEL` | yes | Logging verbosity |

#### Secrets (never in Git)

| Secret | Required | Description |
| --- | --- | --- |
| | | |

#### Data stores

| Store | Required | Ownership |
| --- | --- | --- |
| PostgreSQL | | Schema owner: |
| Redis-compatible cache | | |
| Object storage | | |
| Message queue | | |

#### External dependencies

| Dependency | Required | Failure behavior |
| --- | --- | --- |
| Choice Bank BaaS | connector only | Degrade per runbook |
| | | |

#### Data classification

| Data type | Classification | Storage |
| --- | --- | --- |
| PII | Confidential | Encrypted at rest |
| Financial records | Restricted | Ledger service only |
| Evidence | Confidential | Object storage |

#### Scaling and traffic

| Field | Value |
| --- | --- |
| Minimum instances | |
| Horizontal scaling | CPU/request based |
| Traffic assumptions | |
| Target RPS | |

#### Resilience

| Field | Value |
| --- | --- |
| Request timeout | |
| Retry policy | Idempotent commands only |
| Circuit breaker | Provider calls |
| Provider-outage behavior | No corrupt state; queue/retry |

#### Observability

| Field | Value |
| --- | --- |
| Logs | Structured JSON to aggregator |
| Metrics | Prometheus-compatible |
| Traces | OpenTelemetry |
| Alert thresholds | |

#### Deployment

| Field | Value |
| --- | --- |
| Deployment order | After migrations for owned schemas |
| Migration order | Per `database/migrations` ownership |
| Rollback strategy | Blue/green or rolling with migration reversibility |
| Graceful shutdown | Drain connections; complete in-flight idempotent ops |

---

## Platform-wide defaults (Phase 1)

| Classification | Default |
| --- | --- |
| **Current architectural decision** | All services expose `/health/live`, `/health/ready`, `/health/dependencies` per OpenAPI foundation |
| **Locked doctrine** | Production secrets in approved secrets manager |
| **Locked doctrine** | No permanent local disk storage for user files |
| **Locked doctrine** | Database schema changes via migrations only |
| **Current architectural decision** | Local development uses `docker-compose.yml` PostgreSQL and Redis |

## Service inventory (Phase 2)

| Service | Contract status |
| --- | --- |
| `securepay-core` | **Populated** — see above |
| `financial-ledger` | Template pending |
| `choice-bank-connector` | Template pending |
| `evidence-service` | Template pending |
| `notification-service` | Template pending |
| `webhook-service` | Template pending |
| `securepay-control-centre` | Template pending — UI deferred |

## Related documents

- [Master Architecture](../architecture/SECUREPAY_MASTER_ARCHITECTURE.md)
- [OpenAPI Foundation](../../contracts/openapi/securepay-api-v1.yaml)
- [Infrastructure runbooks](../../infrastructure/runbooks/.gitkeep)
