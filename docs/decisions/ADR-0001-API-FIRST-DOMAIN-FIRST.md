# ADR-0001: API-First, Domain-First Platform

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-22 |
| **Phase** | 1 |

## Context

SecurePay must serve multiple clients (web, mobile, partner platforms, future Keyman solutions) while preserving financial and agreement integrity. Frontend journeys inform usability but must not become the source of truth for money movement, ledger structure, or Payment Ready.

## Decision

SecurePay is **API-first** and **domain-first**:

- Public OpenAPI contracts define the stable integration surface.
- Domain logic owns state transitions, Payment Ready, and financial commands.
- Frontends and partner apps are replaceable clients.

## Consequences

### Positive

- Multiple clients can evolve independently.
- Doctrine can be tested against domain logic without UI coupling.
- Partner integrations remain stable across UI redesigns.

### Negative

- Requires upfront contract and doctrine investment before UI features.
- Domain modeling discipline required across all phases.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Frontend-driven design | Risks bypassing ledger and Payment Ready controls |
| Monolithic UI-embedded rules | Prevents partner API parity and auditability |

## Security impact

Centralized domain authorization reduces risk of client-side bypass. All sensitive operations flow through authenticated APIs with object-level checks.

## Operational impact

API versioning, contract validation in CI, and doctrine tests become mandatory gates.

## Unresolved matters

- Final partner authentication grant type (OAuth vs client credentials) — see [Unresolved Items Register](../operations/UNRESOLVED_ITEMS_REGISTER.md).
