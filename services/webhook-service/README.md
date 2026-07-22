# webhook-service

**Phase 2:** Compiling skeleton module — no webhook delivery.

## Ownership

Future **signed partner webhook delivery**.

## Locked responsibilities (future)

- Signature generation and retry semantics
- Dead-letter handling for failed deliveries

## Prohibited

- **No webhook delivery** in Phase 2
- **No unverified outbound payloads**

## Future health endpoints

Will expose `/health/*` with queue and delivery backlog metrics.

## Related documents

- [Security Baseline](../../docs/security/SECUREPAY_SECURITY_BASELINE.md)
