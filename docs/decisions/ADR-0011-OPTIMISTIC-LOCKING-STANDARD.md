# ADR-0011: Optimistic Locking Standard

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-22 |
| **Phase** | 3 |
| **Branch** | `phase-03-database-audit-idempotency-foundation` |

## Context

Concurrent requests, retry storms, and future horizontally scaled workers will update the same rows in `idempotency.idempotency_records` and `events.outbox_events`. Pessimistic row locking (`SELECT FOR UPDATE`) increases contention and deadlock risk for high-throughput outbox polling.

Phase 3 establishes a single optimistic locking pattern across mutable platform tables, implemented with Spring Data JDBC explicit SQL—not JPA `@Version`.

## Decision

Adopt **optimistic locking via monotonic `version BIGINT` columns** on all Phase 3 mutable tables:

| Table | Mutable operations |
| --- | --- |
| `idempotency.idempotency_records` | Complete, fail, refresh lock |
| `events.outbox_events` | Mark processing, published, dead letter |

**Standard update pattern:**

```sql
UPDATE {schema}.{table}
SET ..., version = version + 1
WHERE id = :id AND version = :expectedVersion
```

If zero rows are updated, repositories throw `OptimisticLockException` with a descriptive message. Callers treat this as a concurrency conflict: retry with fresh read, or abort with a safe client response.

**Rules:**

1. `version` starts at `0` on insert; never decrement.
2. Application passes the version observed at read time as `expectedVersion`.
3. Do not use JPA optimistic locking annotations in `shared/platform-persistence`.
4. Append-only tables (`audit.audit_events`) do **not** use optimistic locking—they have no updates.
5. `platform.technical_test_records` is insert-only in Phase 3; no version column required.

## Consequences

### Positive

- Consistent concurrency semantics across idempotency and outbox workers.
- Clear exception type for integration tests and HTTP error mapping in future API layers.
- Avoids long-held database locks during external I/O (future outbox publish).

### Negative

- Callers must handle `OptimisticLockException` and retry logic explicitly.
- Hot-row contention on a single idempotency record still serializes at the application level.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Pessimistic locking for all updates | Poor fit for outbox pollers competing across instances |
| JPA `@Version` | Phase 3 persistence standard is JDBC; mixed models increase complexity |
| Last-write-wins without version | Lost updates on concurrent status transitions |
| Timestamp-based optimistic lock | Clock skew and coarser granularity vs integer version |

## Security impact

- Prevents accidental or malicious stale updates from overwriting newer worker state when credentials are scoped correctly.
- No direct security boundary; complements least-privilege roles by reducing inconsistent state after partial failures.

## Operational impact

- Log and metric optimistic lock conflicts at appropriate levels (warn for idempotency races, info/debug for outbox worker contention).
- Load tests should validate outbox poller behaviour under multi-instance deployment (future).

## Migration impact

- `version BIGINT NOT NULL DEFAULT 0` added to `idempotency.idempotency_records` and `events.outbox_events` in Phase 3 migration.
- Check constraints enforce `version >= 0`.
- New mutable tables in future phases must include `version` per `OPTIMISTIC_LOCKING_STANDARD.md` unless ADR exempts append-only design.

## Unresolved matters

- Standard HTTP status mapping for `OptimisticLockException` (409 vs 503) — API phase decision.
- Maximum retry count for outbox worker on version conflict — worker implementation phase.
- Whether `platform_metadata` updates need versioning when business logic mutates it — evaluate in domain phases.

## Related documents

- [Optimistic Locking Standard](../architecture/OPTIMISTIC_LOCKING_STANDARD.md)
- [Transactional Outbox Standard](../architecture/TRANSACTIONAL_OUTBOX_STANDARD.md)
- [Idempotency Standard](../architecture/IDEMPOTENCY_STANDARD.md)
