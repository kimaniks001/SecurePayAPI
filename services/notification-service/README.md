# notification-service

**Phase 2:** Compiling skeleton module — no provider calls.

## Ownership

Future **SMS, email, and approved notification-channel delivery**.

## Locked responsibilities (future)

- OTP and notification orchestration
- Rate limiting and audit of delivery attempts

## Prohibited

- **No SMS/email provider calls** in Phase 2
- **No plaintext OTP storage**

## Future health endpoints

Will expose `/health/*` with provider connectivity summaries.

## Related documents

- [Authentication Doctrine](../../docs/domains/AUTHENTICATION_DOCTRINE.md)
