# Authentication and Session Security Standard

**Status:** Approved architecture standard
**Applies from:** Phase 14.5

## KSNumber-first identity

The canonical KS Number is the standard human-facing login identity across SecurePay.

The normal sign-in sequence is:

```text
KS Number → password → approved second factor → authenticated session
```

Phone and email support recovery, verification, notification, and second-factor delivery. They do not replace the KS Number as the normal login identifier.

## Identity and credential eligibility

Authentication may proceed only when both are true:

```text
identity.status = ACTIVE
credential.active = true
```

PENDING, SUSPENDED, and CLOSED identities cannot activate normal authenticated sessions. Suspension, closure, credential deactivation, password change, or security-version invalidation must revoke or invalidate affected sessions.

## Credential enrolment

Creating the first password is credential enrolment, not login and not password recovery.

A password may be enrolled only after a trusted onboarding or identity-ownership proof. Knowledge of a publicly known KS Number is never sufficient proof.

## Session and token security

- Password verification alone must never create an active session.
- Access and refresh tokens are opaque high-entropy values.
- Only cryptographic digests are persisted.
- Refresh tokens rotate on successful use.
- Replay of a rotated refresh token revokes the compromised session and active descendants.
- Replay-triggered revocation must commit even when the request returns an invalid-session error.

## Password change

A successful password change must verify the current password, hash the new password, perform an optimistic compare-and-set update, increment the credential security version, revoke all active sessions and refresh tokens, and record audit and outbox security events.

## Safe errors

Public authentication responses must not distinguish among unknown KS Number, wrong password, inactive credential, suspended identity, closed identity, or missing credential.

## Actor trust boundary

Authenticated actor identity is derived from a validated session or a trusted internal execution boundary.

Clients cannot establish trusted actor identity using request headers or request-body actor IDs.

The temporary SYSTEM actor used before authentication must not represent ordinary user activity after Phase 15.

## Future compatibility

The design must support Phase 16 MFA and recovery, Phase 17 roles, organizations, scopes and delegated authority, step-up authentication, application-specific session control, and agreement signing evidence.
