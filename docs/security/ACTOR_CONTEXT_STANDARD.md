# Actor Context Standard

**Status:** Current architectural decision (Phase 3 implementation)  
**Phase:** 3 — database, audit, idempotency foundation  
**Branch:** `phase-03-database-audit-idempotency-foundation`  
**Module:** `shared/platform-persistence`

## Purpose

Define the trusted actor and request context propagated into audit events, outbox records, and idempotency scope. Actor context represents **server-side truth** about who initiated an operation—not unvalidated client claims alone.

## ActorContext record

```java
ActorContext(
    ActorType actorType,
    String actorId,
    String actorKsNumber,
    String applicationId,
    boolean authenticated,
    String authenticationMethod,
    String requestId,
    String correlationId,
    String sourceService,
    String sourceIpHash,
    String deviceId
)
```

All persistence writers require non-null: `actorType`, `actorId`, `requestId`, `correlationId`, `sourceService`.

## Phase 3 actor types

Phase 3 **uses only** `SYSTEM` and `TEST`. Other enum values exist for forward compatibility but must not be emitted in production Phase 3 flows.

| Actor type | Enum | API / DB value | Factory method | `actor_id` |
| --- | --- | --- | --- | --- |
| System | `ActorType.SYSTEM` | `system` | `ActorContextFactory.system(sourceService)` | `system` |
| Test | `ActorType.TEST` | `service` | `ActorContextFactory.test(sourceService)` | `test-actor` |

**Note:** `ActorType.TEST` maps to API value `service` for envelope compatibility; audit and outbox store this string in `actor_type`.

### Phase 3 exclusions

The following actor types are **not used** in Phase 3:

- `USER`
- `PARTNER_APPLICATION`
- `ADMINISTRATOR`
- `COMPLIANCE_OFFICER`
- `BANK_PROVIDER`

Authentication-derived fields (`authenticated`, `authenticationMethod`, `actorKsNumber`, `applicationId`) remain `false` or `null` in Phase 3 factory methods.

## Request and correlation IDs

| Field | Source (Phase 3) |
| --- | --- |
| `requestId` | `CorrelationContext.requestId()` or `IdentifierRules.newRequestId()` |
| `correlationId` | `CorrelationContext.correlationId()` or `IdentifierRules.newCorrelationId()` |

When HTTP filters populate `CorrelationContext`, persistence inherits client-traceable IDs per [Request Correlation ID Standard](../architecture/REQUEST_CORRELATION_ID_STANDARD.md).

Override for tests: `ActorContextFactory.withCorrelation(base, requestId, correlationId)`.

## Propagation map

| Field | audit.audit_events | events.outbox_events | idempotency.idempotency_records |
| --- | --- | --- | --- |
| `actor_type` | ✓ | ✓ | — |
| `actor_id` | ✓ | ✓ | ✓ (scope) |
| `actor_ks_number` | ✓ | — | — |
| `application_id` | ✓ | — | ✓ (scope) |
| `request_id` | ✓ | — | — |
| `correlation_id` | ✓ | ✓ | — |
| `source_service` | ✓ | ✓ | — |

## Trust boundary (future phases)

When authentication is implemented:

1. Actor context must be assembled **after** authentication and authorization filters
2. Clients must not supply trusted `actor_id` or `actor_type` via request body
3. Partner applications populate `applicationId` from verified token claims
4. KS Number populates `actorKsNumber` only for authenticated users

Phase 3 integration tests use factory methods directly—acceptable for technical scaffolding only.

## Source service

`sourceService` identifies the deploying component (e.g. `securepay-core`). Must match service identity in structured logs for trace correlation.

## Sensitive fields

| Field | Phase 3 | Future |
| --- | --- | --- |
| `sourceIpHash` | `null` | Hashed client IP when privacy policy allows |
| `deviceId` | `null` | Device binding for step-up auth flows |

Never store raw IP addresses in audit metadata without ADR approval.

## Related documents

- [Audit Event Standard](AUDIT_EVENT_STANDARD.md)
- [Idempotency Standard](../architecture/IDEMPOTENCY_STANDARD.md)
- [Request Correlation ID Standard](../architecture/REQUEST_CORRELATION_ID_STANDARD.md)
- [SecurePay Security Baseline](SECUREPAY_SECURITY_BASELINE.md)
