# ADR-0012: KS Number Identity Model

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-23 |
| **Phase** | 4 |
| **Branch** | `phase-04-ksnumber-identity-issuance` |

## Context

SecurePay doctrine defines the KS Number as the canonical human-facing identity identifier. It is permanent, sequential, and centrally allocated. Phase 3 established cross-cutting persistence (audit, idempotency, outbox) but intentionally excluded business-domain tables.

Phase 4 introduces the first business domain schema: `identity`, owned by `shared/platform-identity`. The model must reconcile locked doctrine ([KS Number Doctrine](../domains/KS_NUMBER_DOCTRINE.md)) with operational needs:

- Internal primary keys remain UUIDs; canonical KS Numbers are separate immutable identifiers.
- Canonical numbers are allocated only by the central identity domain — applications must not calculate or reserve numbers.
- Aliases are distinct from canonical numbers and must not impersonate them.
- Identity lifecycle, audit, and outbox events must participate in the same transactional boundaries established in Phase 3.

## Decision

Adopt a **dual-identifier identity model** in PostgreSQL schema `identity`:

| Concept | Storage | Rules |
| --- | --- | --- |
| Internal identity | `identity.ks_identities.id` (UUID) | Primary key; never exposed as the human-facing identifier |
| Canonical KS Number | `canonical_ks_number` + `sequence_number` | Immutable after issuance; globally unique; formatted `KS` + zero-padded digits (minimum width 3) |
| Issuance idempotency key | `issuance_request_key` | Unique per identity; permanent duplicate-issuance safeguard |
| Issuance request fingerprint | `issuance_request_hash` | SHA-256 of issuance command body; immutable after insert |
| Identity type | `identity_type` | `INDIVIDUAL`, `SYSTEM`, `TEST` in Phase 4 |
| Lifecycle status | `status` | `PENDING`, `ACTIVE`, `SUSPENDED`, `CLOSED` in Phase 4 |

**Canonical formatting rules** (implemented in `KsNumberFormatter` / `KsNumber`):

1. Sequence starts at `1`; first issued canonical number is `KS001`.
2. Numbers below 1000 use at least three digits (`KS001` … `KS999`).
3. After `KS999`, formatting continues naturally (`KS1000`, `KS1001`, …).
4. Issued canonical numbers are never reused, even when an identity is closed or suspended.

**Module ownership:**

- Schema DDL: `database/migrations/V20260723130000__ks_identity_foundation.sql`
- Application logic: `shared/platform-identity` (`ke.securepay.platform.identity`)
- Consumption: `securepay-core` wires auto-configuration; no public HTTP identity APIs in Phase 4

## Consequences

### Positive

- Aligns database model with locked KS Number doctrine.
- Clear separation between internal UUID and external canonical identifier supports audit, ledger adjacency, and future API design.
- Identity domain is isolated in its own schema per [ADR-0007](ADR-0007-DATABASE-SCHEMA-OWNERSHIP.md).

### Negative

- Doctrine conceptual statuses (`DECEASED`, `DISSOLVED`, `PENDING_VERIFICATION`, etc.) are not yet modelled — Phase 4 uses a reduced status set.
- `INDIVIDUAL` type is stored but public onboarding flows are out of scope; type alone does not imply verified end-user identity.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Use `sequence_number` as primary key | Violates doctrine; UUID required for internal identity |
| Store canonical number only (no sequence column) | Loses efficient ordering and sequence integrity checks |
| Embed identity in `platform` schema | Violates schema ownership; identity is a business domain |
| Client-supplied KS Numbers | Violates allocation authority doctrine |
| Single `identities` table in `public` | No schema-level ownership or least-privilege boundaries |

## Security impact

- Canonical numbers are not secrets but are sensitive identifiers; audit and outbox payloads include them with actor context.
- No public issuance endpoint in Phase 4 reduces attack surface; service-layer calls require trusted `ActorContext`.
- `INDIVIDUAL` issuance without authentication is deferred — premature public APIs would create impersonation risk.

## Operational impact

- Operators monitor `securepay.identity.issued` and issuance replay/conflict metrics.
- Identity rows include `correlation_id` and `request_id` for incident investigation.
- Fresh-database deployments start sequence at `KS001` via `identity.ks_number_sequence`.

## Migration impact

- New schema `identity` with tables `ks_identities`, `ks_number_aliases`, and sequence `ks_number_sequence`.
- `platform.platform_metadata.platform_phase` updated to `phase-04-ksnumber-identity-issuance`.
- No changes to Phase 3 schemas beyond shared idempotency operation registration.
- Migration is immutable after acceptance per [Database Migration Standard](../operations/DATABASE_MIGRATION_STANDARD.md).

## Concurrency impact

- Identity inserts are independent per issuance; uniqueness enforced by database constraints on `canonical_ks_number`, `sequence_number`, and `issuance_request_key`.
- Lifecycle updates use optimistic locking (`version` column) consistent with Phase 3.
- Sequence allocation concurrency is addressed in [ADR-0013](ADR-0013-SEQUENTIAL-KS-NUMBER-ISSUANCE.md).

## Unresolved matters

- Mapping doctrine statuses (`DECEASED`, `DISSOLVED`, `RESTRICTED`, etc.) to Phase 4 `PENDING`/`ACTIVE`/`SUSPENDED`/`CLOSED` — see [UNRESOLVED_ITEMS_REGISTER](../operations/UNRESOLVED_ITEMS_REGISTER.md) UR-23.
- Whether `display_name` is PII and requires encryption or masking at rest — pending legal/compliance (UR-24).
- Public HTTP API shape for identity lookup and issuance — deferred to API phase (UR-25).
- End-user and partner `ActorContext` propagation — requires authentication phase (UR-26).

## Related documents

- [KS Identity Domain Standard](../architecture/KS_IDENTITY_DOMAIN_STANDARD.md)
- [KS Number Issuance Standard](../architecture/KS_NUMBER_ISSUANCE_STANDARD.md)
- [KS Number Doctrine](../domains/KS_NUMBER_DOCTRINE.md)
- [ADR-0013 Sequential KS Number issuance](ADR-0013-SEQUENTIAL-KS-NUMBER-ISSUANCE.md)
- [ADR-0015 KS identity lifecycle](ADR-0015-KS-IDENTITY-LIFECYCLE.md)
