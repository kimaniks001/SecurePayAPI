# ADR-0002: Modular Platform Boundaries

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-22 |
| **Phase** | 1 |

## Context

SecurePay spans agreements, money, evidence, notifications, webhooks, and banking integration. Some concerns require strong isolation; unnecessary microservices add operational cost.

## Decision

Adopt a **modular architecture** with selected isolated services:

- `securepay-core` — agreement and identity domains
- `financial-ledger` — authoritative money record
- `choice-bank-connector` — sole Choice Bank boundary
- `evidence-service`, `notification-service`, `webhook-service` — isolated supporting services
- `securepay-control-centre` — administration UI (API client only)

Do not create unnecessary microservices. Logical domain ownership is documented separately from deployment topology.

## Consequences

### Positive

- Ledger and banking credentials isolated from general application code.
- Teams can scale and secure high-risk components independently.

### Negative

- Distributed tracing and deployment coordination required.
- Inter-service contracts must be maintained.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Single monolith for everything | Insufficient isolation for ledger and banking credentials |
| Full microservices per domain | Excessive operational overhead for early phases |

## Security impact

Least-privilege database and credential access per service. Choice keys confined to connector.

## Operational impact

Each service requires application–infrastructure contract completion before production.

## Unresolved matters

- Exact service split timing for evidence vs core — may start combined with clear module boundaries.
