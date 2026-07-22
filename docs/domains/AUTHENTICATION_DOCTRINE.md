# Authentication Doctrine

**Status:** Locked doctrine  
**Phase:** 1 — documentation only (no authentication implementation in this phase)

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
