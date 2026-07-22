# Optimistic Locking Standard

**Status:** Current architectural decision (Phase 3 implementation)  
**Phase:** 3 — database, audit, idempotency foundation  
**Branch:** `phase-03-database-audit-idempotency-foundation`  
**Module:** `shared/platform-persistence`

## Purpose

Provide a single, predictable concurrency control model for mutable rows accessed by concurrent HTTP requests and future background workers.

## Applicability

| Table | Uses `version` | Rationale |
| --- | --- | --- |
| `idempotency.idempotency_records` | Yes | Concurrent retries and completion updates |
| `events.outbox_events` | Yes | Multi-instance outbox workers |
| `audit.audit_events` | No | Append-only; DB triggers block updates |
| `platform.technical_test_records` | No | Insert-only in Phase 3 |
| `platform.platform_metadata` | No | Simple key-value; versioning deferred |

New mutable tables that support concurrent updates **must** include `version BIGINT NOT NULL DEFAULT 0` unless an ADR documents an alternative (e.g. append-only log).

## Column specification

```sql
version BIGINT NOT NULL DEFAULT 0,
CONSTRAINT {table}_version_non_negative CHECK (version >= 0)
```

## Update contract

1. **Read** row including current `version`.
2. **Update** with predicate `WHERE id = :id AND version = :expectedVersion`.
3. **Increment** `version = version + 1` in the same `UPDATE`.
4. If affected row count is `0`, throw `OptimisticLockException`.

### Reference implementation (idempotency)

```java
int updated = jdbcTemplate.update(
    """
    UPDATE idempotency.idempotency_records
    SET processing_status = 'COMPLETED', ..., version = version + 1
    WHERE id = :id AND version = :expectedVersion
    """, params);
if (updated == 0) {
    throw new OptimisticLockException("Idempotency record version conflict");
}
```

Same pattern appears in `OutboxRepository.markProcessing`, `markPublished`, and `markDeadLetter`.

## Exception handling

| Layer | Phase 3 behaviour |
| --- | --- |
| Repository | Throws `ke.securepay.platform.persistence.exception.OptimisticLockException` |
| Service | Propagates or retries read-modify-write where safe |
| HTTP API (future) | Map to 409 Conflict or retryable 503 per API standards |

Do not swallow `OptimisticLockException` silently.

## Prohibited patterns

| Pattern | Reason |
| --- | --- |
| JPA `@Version` on shared persistence entities | Standard is JDBC explicit SQL |
| `UPDATE` without version predicate on concurrent tables | Lost updates |
| Decrementing `version` | Breaks monotonic conflict detection |
| Pessimistic lock as default for outbox polling | Contention across worker instances |

## Testing requirements

Integration tests must cover:

- Successful update increments version
- Stale version throws `OptimisticLockException`

See `IdempotencyIntegrationTest` and `OutboxIntegrationTest` in `securepay-core`.

## Related documents

- [ADR-0011 Optimistic locking standard](../decisions/ADR-0011-OPTIMISTIC-LOCKING-STANDARD.md)
- [Idempotency Standard](IDEMPOTENCY_STANDARD.md)
- [Transactional Outbox Standard](TRANSACTIONAL_OUTBOX_STANDARD.md)
