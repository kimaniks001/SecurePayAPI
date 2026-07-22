# ADR-0015: KS Identity Lifecycle

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-23 |
| **Phase** | 4 |
| **Branch** | `phase-04-ksnumber-identity-issuance` |

## Context

KS Number doctrine describes a rich set of conceptual identity statuses (`ACTIVE`, `SUSPENDED`, `CLOSED`, `DECEASED`, `DISSOLVED`, etc.) and requires that closed or suspended identities retain their canonical numbers permanently.

Phase 4 must implement a **minimal enforceable lifecycle** for platform operations and integration tests without prematurely encoding verification, banking, or compliance workflows. Issuance creates identities in `PENDING`; activation and suspension are required before downstream domains (agreements, ledger adjacency) can rely on identity state.

## Decision

Adopt a **four-state identity lifecycle** for Phase 4:

```
PENDING → ACTIVE
ACTIVE → SUSPENDED | CLOSED
SUSPENDED → ACTIVE | CLOSED
CLOSED → (terminal)
```

| Status | Meaning (Phase 4) |
| --- | --- |
| `PENDING` | Canonical number issued; not yet active for platform use |
| `ACTIVE` | Identity available for downstream domain references |
| `SUSPENDED` | Temporarily blocked; canonical number retained |
| `CLOSED` | Terminal; canonical number retained; `closed_at` set |

**Implementation:**

- Service: `KsIdentityLifecycleService` / `DefaultKsIdentityLifecycleService`
- Command: `LifecycleTransitionCommand` with `targetStatus`, `reason`, `actorContext`
- Persistence: `KsIdentityRepository.updateStatus` with optimistic locking
- Timestamps: `suspended_at` set on transition to `SUSPENDED`; `closed_at` on `CLOSED`
- Audit: `identity.status.changed` with `previous_state` / `new_state`
- Outbox: `identity.status.changed` with `previous_status` / `new_status`
- Metric: `securepay.identity.status.changed`

**Alias lifecycle** (separate state machine on `ks_number_aliases`):

```
RESERVED → ACTIVE | RETIRED
ACTIVE → SUSPENDED | RETIRED
SUSPENDED → ACTIVE | RETIRED
```

Managed by `KsAliasService.transitionAlias` with `AliasLifecycleTransitionCommand`.

**Issuance default:** New identities are created with `status = PENDING` inside `DefaultKsIdentityIssuanceService`.

## Consequences

### Positive

- Clear, testable transitions with explicit illegal-transition errors (`IdentityLifecycleException`).
- Terminal `CLOSED` state preserves canonical numbers per doctrine.
- Audit and outbox provide traceability for compliance investigations.

### Negative

- Doctrine statuses `DECEASED`, `DISSOLVED`, `RESTRICTED`, `INACTIVE`, etc. are not modelled — mapping deferred.
- No automatic cascade from identity `SUSPENDED` to alias `SUSPENDED` in Phase 4.
- `PENDING` is not the doctrine term `ISSUED` / `RESERVED` — naming alignment pending.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Single `ACTIVE` state only | Insufficient for suspension and closure workflows |
| Full doctrine status enum in Phase 4 | Premature without verification and banking flows |
| Soft-delete identities | Violates permanent number retention |
| Lifecycle via direct SQL updates | Bypasses audit, outbox, and optimistic locking |
| Event-sourced identity only | Phase 3 pattern is relational SoR + outbox |

## Security impact

- Lifecycle transitions require trusted `ActorContext`; no public API in Phase 4.
- Suspension and closure events are audited for forensic review.
- Re-activation from `SUSPENDED` requires explicit transition — no implicit auto-activation.

## Operational impact

- Operators will eventually manage lifecycle via Control Centre APIs (future); Phase 4 is service-layer only.
- `reason` field on transition commands is persisted in audit `reason` column.
- Query by `status` supported via `idx_ks_identities_status`.

## Migration impact

- Check constraint `ks_identities_status_check` enforces allowed status values.
- Timestamp ordering constraints: `suspended_at`, `closed_at`, `updated_at` >= `created_at`.
- No data migration — new table population begins at issuance.

## Concurrency impact

- Lifecycle updates use `version` optimistic locking; concurrent transitions on same identity produce `OptimisticLockException` from repository layer.
- Status transitions are low-frequency compared to issuance; no special hot-row mitigation required in Phase 4.

## Unresolved matters

- Map doctrine statuses (`DECEASED`, `DISSOLVED`, `RESTRICTED`, …) to Phase 4 states or extensions — UR-23.
- Whether `PENDING` should auto-transition to `ACTIVE` on first verification event — UR-32.
- Cascade rules: identity `SUSPENDED` → alias `SUSPENDED` — UR-33.
- Banking/account closure hooks on `CLOSED` — UR-03, UR-34.
- Whether `CLOSED` identities may be reopened — doctrine suggests no; confirm via ADR amendment.

## Related documents

- [KS Identity Domain Standard](../architecture/KS_IDENTITY_DOMAIN_STANDARD.md)
- [ADR-0012 KS Number identity model](ADR-0012-KS-NUMBER-IDENTITY-MODEL.md)
- [ADR-0014 KS Number alias model](ADR-0014-KS-NUMBER-ALIAS-MODEL.md)
- [Audit Event Standard](../security/AUDIT_EVENT_STANDARD.md)
- [Transactional Outbox Standard](../architecture/TRANSACTIONAL_OUTBOX_STANDARD.md)
