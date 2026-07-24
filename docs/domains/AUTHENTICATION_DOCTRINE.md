# Authentication Doctrine

**Status:** Locked doctrine  
**Phase:** Locked in Phase 1; implementation alignment approved in Phase 14.5

## Normal login sequence

```
KS Number → password → OTP → authenticated session
```

| Step | Classification | Requirement |
| --- | --- | --- |
| 1. KS Number | **Locked doctrine** | Primary login identifier |
| 2. Password | **Locked doctrine** | Verified with modern adaptive hash |
| 3. OTP | **Locked doctrine** | Delivered via verified phone or email channel |
| 4. Session | **Locked doctrine** | Issued only after successful multi-factor completion |

## Identifier rules

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | KS Number is the primary login identifier. |
| **Locked doctrine** | Repeated login helps users remember their KS Number. |
| **Locked doctrine** | Phone and email support OTP, recovery, verification, and notifications. |
| **Locked doctrine** | Phone and email do not replace the KS Number as the standard login identifier. |
| **Current architectural decision** | "Find my KS Number" may use verified phone, email, and identity checks. |

## Credential storage

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Passwords use a modern adaptive hash (e.g. Argon2id or bcrypt with appropriate work factor). |
| **Locked doctrine** | Passwords, OTPs, access tokens, and refresh tokens are never stored in plaintext. |
| **Locked doctrine** | OTPs expire and have attempt limits. |

## Abuse controls

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Login attempts are rate-limited by identity, application, device, and source. |
| **Locked doctrine** | Login errors do not reveal whether a KS Number exists. |
| **Locked doctrine** | Sensitive actions require fresh step-up authentication. |

## Sessions and administration

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Sessions are revocable. |
| **Locked doctrine** | Administrators require stronger and separately audited authentication. |
| **Locked doctrine** | Recovery events are audited. |

## Session activation boundary

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Password verification alone never creates an active authenticated session. |
| **Locked doctrine** | Password success creates or advances a pending MFA challenge. |
| **Locked doctrine** | Session activation requires trusted proof of password and approved second-factor completion. |
| **Locked doctrine** | Completion proof is actor-bound, challenge-bound, expiry-bound and single-use. |
| **Locked doctrine** | No production password-only bypass may be enabled by configuration. |

## Identity and credential eligibility

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Normal authentication requires both an ACTIVE identity and an active credential. |
| **Locked doctrine** | PENDING identities cannot activate normal authenticated sessions. |
| **Locked doctrine** | SUSPENDED and CLOSED identities cannot authenticate. |
| **Locked doctrine** | Suspension, closure, credential deactivation, password change or security-version invalidation revokes or invalidates affected sessions. |

## Credential enrolment and recovery

| Classification | Rule |
| --- | --- |
| **Current architectural decision** | Initial password creation is credential enrolment and requires trusted identity-ownership proof. |
| **Locked doctrine** | Knowledge of a KS Number alone is never sufficient to create or reset its credential. |
| **Current architectural decision** | Public recovery and forgotten-password controls are implemented with MFA abuse protections in Phase 16. |

## Session and token security

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Access and refresh tokens are opaque high-entropy values; only cryptographic digests are persisted. |
| **Locked doctrine** | Refresh tokens rotate after successful use. |
| **Locked doctrine** | Replay of a rotated refresh token revokes the compromised session and active descendants. |
| **Locked doctrine** | Replay-triggered revocation must commit even when the request returns an invalid-session error. |
| **Locked doctrine** | Password change revokes all sessions and refresh tokens belonging to the identity. |

## Actor propagation

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Authenticated actor identity is derived from a validated session, never from an untrusted actor header. |
| **Current architectural decision** | The temporary SYSTEM actor must not represent ordinary user activity after authenticated actor propagation exists. |
| **Current architectural decision** | Roles, scopes, organization membership and delegated authority are resolved dynamically rather than treated as permanently valid token claims. |

## Partner and API access

| Classification | Statement |
| --- | --- |
| **Future implementation requirement** | Partner applications authenticate via client credentials or OAuth with scoped access |
| **Locked doctrine** | Valid credentials do not automatically make an application trusted — scopes and object-level authorization still apply |

## Phase 1 scope

**Confirmed:** No authentication services, session stores, OTP delivery, or credential tables are implemented in Phase 1.

## Related documents

- [KS Number Doctrine](KS_NUMBER_DOCTRINE.md)
- [Security Baseline](../security/SECUREPAY_SECURITY_BASELINE.md)
- [Unresolved Items Register](../operations/UNRESOLVED_ITEMS_REGISTER.md)
- [MFA-Gated Session Activation ADR](../decisions/ADR-0016-MFA-GATED-SESSION-ACTIVATION.md)
- [Authentication and Session Security Standard](../security/AUTHENTICATION_AND_SESSION_SECURITY_STANDARD.md)
- [Identity Endpoint Exposure Standard](../architecture/IDENTITY_ENDPOINT_EXPOSURE_STANDARD.md)
- [Phase 15 Authentication Implementation Contract](../architecture/PHASE_15_AUTHENTICATION_IMPLEMENTATION_CONTRACT.md)
