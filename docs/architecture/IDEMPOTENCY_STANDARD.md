# Idempotency Standard

**Status:** Current architectural decision (Phase 4 implementation)
**Phase:** 4 — KS Number identity issuance
**Branch:** `phase-04-ksnumber-identity-issuance`
**Module:** `shared/platform-persistence`, `shared/platform-identity`

## Purpose

Define how SecurePay stores and replays idempotent command execution so client retries do not duplicate side effects. Phase 3 implements the persistence layer; business API operation codes arrive in later phases.

## Storage

| Item | Value |
| --- | --- |
| Schema | `idempotency` |
| Table | `idempotency.idempotency_records` |
| Access | `IdempotencyRepository`, `IdempotencyService` (Spring Data JDBC) |

## Scope key

A record is uniquely identified by:

```
(COALESCE(application_id, ''), COALESCE(actor_id, ''), operation_code, idempotency_key)
```

Enforced by unique index `uq_idempotency_scope`.

| Dimension | Phase 3–4 behaviour |
| --- | --- |
| `application_id` | `NULL` for `SYSTEM` and `TEST` actors via `ActorContextFactory` |
| `actor_id` | `system` or `test-actor` |
| `operation_code` | `platform.technical.test`, `identity.ks-number.issue` |
| `idempotency_key` | Client-supplied; normalized by `IdempotencyKeyValidator` |

## Idempotency key rules

| Rule | Requirement |
| --- | --- |
| Normalization | Trim whitespace; reject empty or invalid keys |
| Request hash | SHA-256 of normalized request body |
| Hash mismatch | `IdempotencyConflictException` — same key, different payload |
| Expiry | Default 24 hours (`expires_at`); expired records not replayable |

## Processing status lifecycle

```
IN_PROGRESS → COMPLETED
            → FAILED_RETRYABLE (future API use)
            → FAILED_FINAL
            → EXPIRED (operational classification / future job)
```

| Status | Client behaviour |
| --- | --- |
| `COMPLETED` | Return stored `response_status` and `response_body`; increment replay metric |
| `IN_PROGRESS` (lock active) | Conflict — retry after delay |
| `IN_PROGRESS` (lock expired) | Conflict with stale message — safe retry may acquire refreshed lock in future phases |
| Other | Conflict — do not assume success |

## Locking and optimistic concurrency

| Field | Default | Purpose |
| --- | --- | --- |
| `locked_until` | Now + 5 minutes on create | Prevents parallel execution |
| `version` | Starts at 0 | Optimistic lock on updates |

Updates use `WHERE id = :id AND version = :expectedVersion`. Conflicts raise `OptimisticLockException`.

## Phase 4 identity issuance

Permanent duplicate protection is enforced by `identity.ks_identities.issuance_request_key` (unique forever) and `issuance_request_hash`. Idempotency records are replay-storage only.

```java
IdempotencyService.execute(
    actor,
    IdempotencyService.IDENTITY_ISSUE_OPERATION,  // "identity.ks-number.issue"
    "identity",
    issuanceRequestKey,
    requestBody,
    "application/json",
    IdempotencyService.DEFAULT_LOCK,
    IdempotencyService.IDENTITY_ISSUE_REPLAY_STORAGE_EXPIRY,  // 90 days — replay storage only
    issuanceAction)
```

`DefaultKsIdentityIssuanceService.issue` checks permanent identity ownership **before** idempotency and **before** `nextval`. Request body JSON includes `issuance_request_key`, `identity_type`, `display_name`. Fingerprint stored as `issuance_request_hash`.

| Safeguard | Survives idempotency expiry/deletion? |
| --- | --- |
| `identity.ks_identities.issuance_request_key` UNIQUE | **Yes** — permanent |
| `issuance_request_hash` on identity row | **Yes** — permanent |
| Idempotency replay record | No — operational replay storage only |

No environment variable may disable identity issuance idempotency or permanent duplicate protection.

## Phase 3 entry point

```java
IdempotencyService.executeTechnical(actor, idempotencyKey, requestBody, contentType, action)
```

- Wraps technical test side effects in a single `@Transactional` boundary
- On success: marks record `COMPLETED` with HTTP 200 JSON response body
- Returns `IdempotencyExecutionResult` with `replayed` flag

## Response replay

Completed records store:

- `response_status` (integer HTTP status)
- `response_content_type`
- `response_body` (JSONB)

Callers must ensure response bodies contain no secrets before persistence.

## Metrics

| Metric | Meaning |
| --- | --- |
| `idempotency.created` | New record inserted |
| `idempotency.replayed` | Completed record returned without re-execution |
| `idempotency.conflicts` | Key/hash/lock conflicts |

## Retention

Idempotency record retention period: **pending legal/operational confirmation** — no invented legal retention period is encoded in Phase 4.

For identity issuance, **legal retention remains unresolved**. The permanent duplicate-issuance guarantee is `identity.ks_identities.issuance_request_key` uniqueness plus `issuance_request_hash`. Deleting or expiring idempotency replay data cannot authorize another identity for the same key.

`IDENTITY_ISSUE_REPLAY_STORAGE_EXPIRY` (90 days) is a provisional operational replay-storage setting only.

Phase 3 stores records until manual cleanup or future purge job. Index `idx_idempotency_expires_at` supports expiry-based deletion when policy is approved.

## Future operation codes

Business phases will register operation codes such as agreement or payment commands. Each code must:

1. Be documented in OpenAPI and internal registries
2. Use the same scope and hash rules
3. Map HTTP idempotency headers to `idempotency_key`
4. Participate in the same transaction boundary as domain writes when applicable

## Related documents

- [ADR-0009 Idempotency persistence](../decisions/ADR-0009-IDEMPOTENCY-PERSISTENCE.md)
- [Optimistic Locking Standard](OPTIMISTIC_LOCKING_STANDARD.md)
- [Transaction Boundary Standard](TRANSACTION_BOUNDARY_STANDARD.md)
- [Actor Context Standard](../security/ACTOR_CONTEXT_STANDARD.md)
- [Data Retention and Partitioning Standard](../operations/DATA_RETENTION_AND_PARTITIONING_STANDARD.md)
