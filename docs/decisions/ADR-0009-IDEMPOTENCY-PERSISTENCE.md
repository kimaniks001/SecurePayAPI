# ADR-0009: Idempotency Persistence

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-22 |
| **Phase** | 3 |
| **Branch** | `phase-03-database-audit-idempotency-foundation` |

## Context

SecurePay security baseline requires idempotency for mutating financial and agreement commands. Clients and partners may retry requests on timeouts or network failures. Without server-side idempotency records, retries can duplicate side effects—a critical failure mode for payments and ledger adjacency.

Phase 3 implements persistent idempotency infrastructure before business APIs exist, using a technical operation code to validate scope, hashing, replay, and conflict detection.

## Decision

Persist idempotency state in PostgreSQL table `idempotency.idempotency_records`, accessed via **Spring Data JDBC** in `shared/platform-persistence`:

1. **Scope key:** Unique on `(COALESCE(application_id,''), COALESCE(actor_id,''), operation_code, idempotency_key)` via partial unique index `uq_idempotency_scope`.
2. **Request integrity:** SHA-256 hash of normalized request body (`IdempotencyKeyValidator.hashRequest`); reuse of the same key with a different hash raises `IdempotencyConflictException`.
3. **Status model:** `IN_PROGRESS`, `COMPLETED`, `FAILED_RETRYABLE`, `FAILED_FINAL`, `EXPIRED`—aligned with check constraint and `IdempotencyStatus` enum.
4. **Locking:** `locked_until` prevents concurrent duplicate execution while `IN_PROGRESS`; default lock duration 5 minutes.
5. **Expiry:** `expires_at` default 24 hours from creation; expired records are not replayable.
6. **Optimistic locking:** `version` column incremented on every state transition update.
7. **Phase 3 operation:** Single technical operation `platform.technical.test` via `IdempotencyService.executeTechnical`; actor types limited to `SYSTEM` and `TEST` through `ActorContextFactory`.

Idempotency records store serialized response bodies (`response_body JSONB`) for safe replay of completed operations.

## Consequences

### Positive

- Retries return the original response for completed operations without re-executing side effects.
- Conflicting reuse of keys is detected and metered (`PersistenceMetricNames.IDEMPOTENCY_CONFLICTS`).
- Foundation ready for financial command operation codes in later phases without schema redesign.

### Negative

- Storage growth until purge jobs run per retention policy.
- Clients must supply stable idempotency keys within the documented format.
- In-progress staleness requires client retry semantics when lock expires.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Redis-only idempotency | Not durable across Redis eviction or restart; insufficient for financial commands |
| Idempotency keyed by HTTP method + path only | Insufficient scope for partner applications and actor-specific commands |
| No response body storage | Cannot faithfully replay HTTP responses on retry |
| JPA `@Version` entities | Team standard is explicit JDBC updates with `OptimisticLockException` |

## Security impact

- Idempotency keys validated and normalized before persistence (`IdempotencyKeyValidator`).
- Scope includes `application_id` and `actor_id` to prevent cross-tenant replay when those dimensions are populated in future phases.
- Response bodies must not contain secrets; callers responsible for sanitization before storage.
- Database role for runtime service: `INSERT`/`UPDATE`/`SELECT` on `idempotency.idempotency_records` only.

## Operational impact

- Metrics: `idempotency.created`, `idempotency.replayed`, `idempotency.conflicts` via Micrometer.
- Expired and completed record cleanup deferred to retention job (see `DATA_RETENTION_AND_PARTITIONING_STANDARD.md`).
- Support teams can inspect records by `idempotency_key` and `operation_code` for dispute resolution (future).

## Migration impact

- Migration creates schema `idempotency` and table `idempotency.idempotency_records` with indexes on `processing_status`, `expires_at`, and `created_at`.
- No migration from prior idempotency storage (none existed).
- Forward-only; status enum changes require new migration and application deployment coordination.

## Unresolved matters

- Default TTL for production financial operations — **pending legal/operational confirmation** (Phase 3 uses 24 hours for technical tests).
- Whether `FAILED_RETRYABLE` automatic retry is server-driven or client-only — deferred to API layer Phase 4+.
- Idempotency key header name and OpenAPI requirement timing — contracts phase.
- Purge automation schedule — operations phase.

## Related documents

- [Idempotency Standard](../architecture/IDEMPOTENCY_STANDARD.md)
- [Optimistic Locking Standard](../architecture/OPTIMISTIC_LOCKING_STANDARD.md)
- [Actor Context Standard](../security/ACTOR_CONTEXT_STANDARD.md)
- [SecurePay Security Baseline](../security/SECUREPAY_SECURITY_BASELINE.md)
