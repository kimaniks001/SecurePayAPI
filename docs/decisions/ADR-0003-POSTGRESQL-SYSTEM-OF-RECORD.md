# ADR-0003: PostgreSQL as System of Record

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-22 |
| **Phase** | 1 |

## Context

SecurePay requires ACID transactions, relational integrity for agreements and ledger adjacency, and mature migration tooling. Identity uses UUID internally; KS Numbers are sequential business identifiers.

## Decision

**PostgreSQL** is the system of record for SecurePay operational and financial metadata (ledger postings also in PostgreSQL under `financial-ledger` ownership).

- Schema changes via `database/migrations` only.
- Local development uses PostgreSQL 16 via Docker Compose.

## Consequences

### Positive

- Strong consistency for agreement and financial adjacency.
- Well-understood backup, replication, and migration ecosystem.

### Negative

- Horizontal write scaling requires careful partitioning later.
- Connection pool management at scale.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Document store primary | Weaker transactional guarantees for ledger adjacency |
| Multiple primary databases | Reconciliation complexity |

## Security impact

Encryption at rest, least-privilege DB roles per service, no Control Centre direct access.

## Operational impact

Migration order documented in application–infrastructure contract. Managed PostgreSQL in staging/production.

## Unresolved matters

- Read replica strategy for reporting — Phase 4+.
