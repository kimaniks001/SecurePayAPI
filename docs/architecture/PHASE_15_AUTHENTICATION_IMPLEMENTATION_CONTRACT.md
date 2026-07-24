# Phase 15 Authentication Implementation Contract

**Status:** Binding implementation contract
**Precondition:** Phase 14.5 alignment merged

## Phase objective

Implement KSNumber-first password authentication and secure session lifecycle infrastructure without violating the locked requirement that an authenticated session is issued only after successful MFA completion.

## Required capabilities

Phase 15 must implement:

1. strict canonical KSNumber login input;
2. safe credential and identity eligibility checks;
3. adaptive password verification;
4. generic invalid-credential responses;
5. persistent pending MFA challenges;
6. trusted authentication-completion proof boundary;
7. session activation only after valid completion proof;
8. opaque access and refresh token issuance;
9. digest-only token persistence;
10. access-token expiry;
11. refresh-token rotation;
12. refresh replay detection and committed compromise revocation;
13. logout;
14. explicit session revocation;
15. actor-wide revocation;
16. password change with optimistic locking;
17. immutable audit events;
18. transactional outbox events;
19. trusted actor propagation foundation;
20. PostgreSQL integration tests for replay, expiry and revocation.

## Explicit exclusions

Phase 15 does not implement:

- production OTP delivery;
- recovery;
- lockout;
- rate limiting;
- delegated authority;
- organization roles;
- agreement signing;
- payments or regulated-account mappings.

Those capabilities must be supported by the model but are implemented in later phases.

## Mandatory tests

The exit gate requires proof that:

- password success alone cannot issue an active session;
- a valid completion proof can activate one session and cannot be replayed;
- suspended and closed identities cannot authenticate;
- inactive credentials cannot authenticate;
- public errors do not reveal identity existence;
- access tokens expire;
- refresh tokens rotate;
- replay of a rotated refresh token commits session revocation;
- logout revokes session access;
- password change revokes all actor sessions and refresh tokens;
- raw secrets are absent from persistence, audit and outbox payloads;
- existing Phase 1–14 tests continue to pass.
