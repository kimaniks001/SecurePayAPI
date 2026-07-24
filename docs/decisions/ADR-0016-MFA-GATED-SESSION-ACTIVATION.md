# ADR-0016: MFA-Gated Session Activation

**Status:** Accepted
**Date:** 2026-07-24
**Applies from:** Phase 15

## Decision

SecurePay authentication remains:

```text
KS Number → password → OTP → authenticated session
```

Password verification alone must never create an active authenticated session.

Phase 15 may verify the KS Number and password, create a pending MFA challenge, and prepare session infrastructure. An active session is issued only after a trusted, single-use completion proof confirms successful password and approved second-factor completion.

Phone and email remain supporting channels for OTP, recovery, verification, and notifications. They do not replace the KS Number as the normal login identifier.

Suspended or closed identities cannot authenticate, and no production password-only bypass is permitted.
