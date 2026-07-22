# KS Number Issuance Standard

**Status:** Current architectural decision (Phase 4 implementation)  
**Phase:** 4 — KS Number identity issuance  
**Branch:** `phase-04-ksnumber-identity-issuance`  
**Module:** `shared/platform-identity`

## Purpose

Define the canonical KS Number issuance transaction, permanent duplicate protection, idempotency contract, sequence allocation, and observability for Phase 4.

## Canonical model

| Item | Rule |
| --- | --- |
| First issued number | `KS001` (sequence value `1`) |
| Format | `KS` + decimal digits; minimum width 3 for values < 1000 |
| Examples | `KS001`, `KS042`, `KS999`, `KS1000` |
| Permanence | Never reused after issuance |
| Internal key | UUID `identity_id` — not the sequence |

Formatting is implemented in `KsNumberFormatter.format(long sequenceNumber)` and exposed via `KsNumber.fromSequence`.

## Permanent duplicate-issuance safeguard

| Mechanism | Role |
| --- | --- |
| `identity.ks_identities.issuance_request_key` UNIQUE | **Permanent** guarantee — one identity per issuance key forever |
| `identity.ks_identities.issuance_request_hash` | Immutable SHA-256 fingerprint of issuance command body |
| Pre-`nextval` ownership check | Returns original identity on replay; rejects hash mismatch |
| Database unique constraints | `canonical_ks_number`, `sequence_number` never reused |

**Legal retention of idempotency replay records remains unresolved.** Expiry, archival, or deletion of idempotency data **cannot** authorize a second identity when an `identity.ks_identities` row already owns the issuance key.

## Sequence allocation

```sql
SELECT nextval('identity.ks_number_sequence');
```

| Property | Value |
| --- | --- |
| Object | `identity.ks_number_sequence` |
| Start | `1` |
| Increment | `1` |
| Allocator class | `KsNumberSequenceAllocator` |
| Gap policy | Gaps allowed after rollback; numbers not reused |

**Concurrency:** PostgreSQL `nextval` is atomic. Parallel issuers receive distinct sequence values. Validated by `KsIdentityIssuanceConcurrencyIntegrationTest`.

**Replay:** `nextval` is **not** called when an existing identity owns the issuance request key.

## Issuance service

**Interface:** `KsIdentityIssuanceService`  
**Implementation:** `DefaultKsIdentityIssuanceService`  
**Command:** `IssueKsIdentityCommand(issuanceRequestKey, identityType, displayName, actorContext)`  
**Result:** `IssuedKsIdentityResult(identityId, canonicalKsNumber, sequenceNumber, identityType, status, replayed)`

### Validation

| Field | Rule |
| --- | --- |
| `issuanceRequestKey` | Required; non-blank |
| `identityType` | Required; `INDIVIDUAL`, `SYSTEM`, or `TEST` |
| `actorContext` | Required |
| `displayName` | Optional; max 128 characters |

`INDIVIDUAL` type is accepted at the domain layer; public onboarding is **out of scope** for Phase 4.

## Idempotency (replay and concurrency support)

| Item | Value |
| --- | --- |
| Operation code | `identity.ks-number.issue` |
| Scope resource | `identity` |
| Idempotency key | `issuance_request_key` |
| Request body hash | SHA-256 via `IssuanceRequestFingerprint` (same algorithm as idempotency) |
| Replay storage expiry | 90 days (`IdempotencyService.IDENTITY_ISSUE_REPLAY_STORAGE_EXPIRY`) — **provisional operational setting only** |
| Lock duration | 5 minutes (`IdempotencyService.DEFAULT_LOCK`) |

Idempotency records support in-flight concurrency and fast replay. They are **not** the permanent duplicate-issuance guarantee.

### Execution flow

```
Client/service call
  → resolvePermanentIssuanceOwnership(issuance_request_key, hash)
    → [found + matching hash] return original identity (replayed=true)
      • no nextval
      • no issuance audit event
      • no issuance outbox event
    → [found + hash mismatch] IssuanceOwnershipConflictException
  → IdempotencyService.execute(...)  // replay storage only
    → [replay] return stored response
    → [new] createNewIdentity:
        1. race-safe ownership re-check
        2. sequenceAllocator.allocateNext()
        3. KsNumber.fromSequence(sequence)
        4. identityRepository.insert(PENDING, issuance_request_hash)
        5. auditWriter.append(identity.ks-number.issued)
        6. outboxWriter.append(identity.ks-number.issued)
        7. return response map → idempotency COMPLETED
```

### Response body (stored for idempotency replay)

```json
{
  "identity_id": "<uuid>",
  "canonical_ks_number": "KS001",
  "sequence_number": 1,
  "identity_type": "TEST",
  "status": "PENDING"
}
```

### Conflict behaviour

| Condition | Result |
| --- | --- |
| Same key + same body (identity exists) | Return original identity via ownership check |
| Same key + same body (idempotency replay) | Return stored response |
| Same key + different body | `IssuanceOwnershipConflictException` |
| Parallel in-progress | Idempotency lock conflict |

## Transaction boundary

Entire new issuance (sequence, insert, audit, outbox, idempotency completion) runs inside a single `@Transactional` boundary. Ownership replay short-circuits before sequence allocation and side effects. Rollback after `nextval` may leave a sequence gap — acceptable per ADR-0013.

## Audit and outbox

| Event type | Category | Resource |
| --- | --- | --- |
| `identity.ks-number.issued` | `identity` | `identity` / `identity_id` |

Issuance audit and outbox events are written **once** per identity. Replay via ownership check does not append duplicate issuance events.

## Metrics

| Metric | When incremented |
| --- | --- |
| `securepay.identity.issued` | Successful new issuance |
| `securepay.identity.issuance.replayed` | Ownership or idempotency replay |
| `securepay.identity.issuance.conflict` | Hash mismatch on same issuance key |

## Integration tests

| Test | Validates |
| --- | --- |
| `KsIdentityIssuanceIntegrationTest` | Sequential distinct numbers, audit/outbox, replay without sequence advance, expired/deleted idempotency with permanent ownership |
| `KsIdentityIssuanceConcurrencyIntegrationTest` | Parallel distinct numbers, idempotent parallel same-key |
| `IdentityFreshDatabaseIntegrationTest` | Sequence starts at 1 → `KS001` |

## Phase 4 exclusions

- No HTTP `POST /identities` endpoint
- No environment variable to disable issuance idempotency or permanent duplicate protection
- No Choice Bank virtual account creation on issuance
- No automatic `PENDING` → `ACTIVE` transition

## Related documents

- [ADR-0013 Sequential KS Number issuance](../decisions/ADR-0013-SEQUENTIAL-KS-NUMBER-ISSUANCE.md)
- [Idempotency Standard](IDEMPOTENCY_STANDARD.md)
- [KS Identity Domain Standard](KS_IDENTITY_DOMAIN_STANDARD.md)
- [Transaction Boundary Standard](TRANSACTION_BOUNDARY_STANDARD.md)
