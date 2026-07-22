# ADR-0013: Sequential KS Number Issuance

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-23 |
| **Phase** | 4 |
| **Branch** | `phase-04-ksnumber-identity-issuance` |

## Context

Locked doctrine requires that canonical KS Numbers grow sequentially and that only the central KS Number domain may allocate the next number. Phase 4 must issue the first production identity numbers (`KS001` onward) safely under concurrent load from horizontally scaled `securepay-core` instances.

Alternatives include application-level counters, `MAX(sequence_number)+1` queries, and advisory locks. Each has failure modes under contention or partial transaction rollback.

Phase 3 idempotency infrastructure must wrap issuance so client retries do not consume additional sequence values or create duplicate identities.

## Decision

Allocate canonical sequence numbers using a **PostgreSQL sequence** and a single issuance service:

| Item | Value |
| --- | --- |
| Sequence | `identity.ks_number_sequence` |
| Start value | `1` (first formatted canonical: `KS001`) |
| Allocator | `KsNumberSequenceAllocator.allocateNext()` → `SELECT nextval('identity.ks_number_sequence')` |
| Formatter | `KsNumber.fromSequence(sequenceNumber)` → `KsNumberFormatter.format` |
| Issuance service | `KsIdentityIssuanceService` / `DefaultKsIdentityIssuanceService` |
| Idempotency operation | `identity.ks-number.issue` |
| Idempotency key | `issuance_request_key` on command and `ks_identities` unique constraint |

**Permanent duplicate-issuance safeguard:** `identity.ks_identities.issuance_request_key` is unique forever. Before `nextval`, `DefaultKsIdentityIssuanceService` checks for an existing identity with the same key and matching `issuance_request_hash`. Replay returns the original identity without sequence allocation, audit issuance, or outbox issuance. Idempotency records (`identity.ks-number.issue`) support replay and concurrency only; their expiry or deletion cannot authorize a second identity for the same key.

**Idempotency replay storage:** `IDENTITY_ISSUE_REPLAY_STORAGE_EXPIRY` = 90 days — provisional operational TTL for idempotency records only. **Not** the permanent uniqueness guarantee and **not** a legal retention period (legal retention remains unresolved).

### Issuance transaction order

1. **Permanent ownership check** — lookup `issuance_request_key`; if found, verify `issuance_request_hash` and return original identity (no `nextval`, no issuance audit/outbox)
2. Idempotency acquire or replay (`IdempotencyService.execute`)
3. **Race-safe ownership re-check** before `nextval`
4. `nextval` on `identity.ks_number_sequence`
5. Format canonical KS Number
6. Insert `identity.ks_identities` with `status = PENDING` and `issuance_request_hash`
7. Append audit event `identity.ks-number.issued`
8. Append outbox event `identity.ks-number.issued` with causation link to audit `event_id`
9. Complete idempotency record with JSON response body

**Gap policy:** Sequence gaps may occur after transaction rollback or failed issuance after `nextval`. Gaps are **acceptable**; numbers are **never reused**. This is documented on the sequence object in migration SQL.

## Consequences

### Positive

- `nextval` is atomic and scales across concurrent application instances without application-level locks.
- Unique constraints on `sequence_number` and `canonical_ks_number` provide defence in depth.
- Idempotent replays return the original identity without consuming a new sequence value, even when idempotency replay records have expired or been removed.

### Negative

- Doctrine states numbers grow "without gaps in allocation order" for **committed** identities; database sequence gaps from rollbacks are visible only as missing sequence values, not as reused numbers.
- Each failed attempt after `nextval` but before commit consumes a sequence value.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| `SELECT MAX(sequence_number)+1` | Race conditions under concurrency; table scans |
| Pessimistic lock on counter row | Hot-row contention; unnecessary with PostgreSQL sequences |
| Pre-reserve blocks of numbers in application memory | Violates central allocation; complex recovery on crash |
| Redis `INCR` | Not authoritative; violates PostgreSQL system-of-record decision |
| UUID-based external numbers | Violates KS Number doctrine |

## Security impact

- Sequence allocation is not exposed to clients; only `DefaultKsIdentityIssuanceService` invokes the allocator.
- Idempotency scope prevents duplicate identity creation from replay attacks using the same issuance key with identical payload.
- Permanent `issuance_request_key` uniqueness on `identity.ks_identities` prevents a second identity even if idempotency data is unavailable.
- Mismatched payload for the same key raises `IssuanceOwnershipConflictException` and increments `securepay.identity.issuance.conflict`.

## Operational impact

- Monitor `securepay.identity.issued`, `securepay.identity.issuance.replayed`, and `securepay.identity.issuance.conflict`.
- Alert on sudden drops in issuance rate or elevated conflict counts.
- Fresh environments: verify `identity.ks_number_sequence` starts at `1` via `IdentityFreshDatabaseIntegrationTest`.

## Migration impact

- `CREATE SEQUENCE identity.ks_number_sequence START WITH 1 INCREMENT BY 1 MINVALUE 1 NO MAXVALUE NO CYCLE`.
- `ks_identities.sequence_number` unique + positive check constraint.
- No backfill required — greenfield sequence for Phase 4 acceptance.

## Concurrency impact

- **Primary mechanism:** PostgreSQL `nextval` serializes sequence allocation across sessions.
- **Integration validation:** `KsIdentityIssuanceConcurrencyIntegrationTest` issues 12 parallel unique requests and asserts distinct canonical numbers and monotonic sequence values.
- **Idempotency:** Same `issuance_request_key` under parallel calls resolves to one identity via idempotency record locking.
- **Not used for issuance:** optimistic locking on sequence (sequence is append-only via `nextval`).

## Unresolved matters

- Whether to expose sequence gap metrics for operations dashboards — engineering assumption pending ops input (UR-27).
- Maximum sustainable issuance throughput before sequence becomes a bottleneck — not measured in Phase 4; BIGINT sequence is sufficient for platform lifetime.
- Reconciliation job to compare `MAX(sequence_number)` vs `last_value` of sequence — future ops tooling.

## Related documents

- [KS Number Issuance Standard](../architecture/KS_NUMBER_ISSUANCE_STANDARD.md)
- [Idempotency Standard](../architecture/IDEMPOTENCY_STANDARD.md)
- [ADR-0012 KS Number identity model](ADR-0012-KS-NUMBER-IDENTITY-MODEL.md)
- [KS Number Doctrine](../domains/KS_NUMBER_DOCTRINE.md)
