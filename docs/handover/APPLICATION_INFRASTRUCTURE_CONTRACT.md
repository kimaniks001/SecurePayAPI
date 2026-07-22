# Application–Infrastructure Contract

**Status:** Current architectural decision — reusable handover template  
**Phase:** 1 — template and conventions

## Purpose

Define the contract between SecurePay application services and infrastructure teams. Every deployable service must have a completed instance of this template before production deployment.

## Service contract template

Copy this section for each service.

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

## Service inventory (scaffold)

| Service | Phase 1 contract status |
| --- | --- |
| `securepay-core` | Template only — to be completed Phase 2 |
| `financial-ledger` | Template only |
| `choice-bank-connector` | Template only |
| `evidence-service` | Template only |
| `notification-service` | Template only |
| `webhook-service` | Template only |
| `securepay-control-centre` | Template only — UI deferred |

## Related documents

- [Master Architecture](../architecture/SECUREPAY_MASTER_ARCHITECTURE.md)
- [OpenAPI Foundation](../../contracts/openapi/securepay-api-v1.yaml)
- [Infrastructure runbooks](../../infrastructure/runbooks/.gitkeep)
