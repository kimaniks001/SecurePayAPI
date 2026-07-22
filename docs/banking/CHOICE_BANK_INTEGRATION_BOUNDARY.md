# Choice Bank Integration Boundary

**Status:** Locked doctrine + current architectural decision  
**Phase:** 1 — boundary documentation only (no Choice API calls in this phase)

## Boundary rule

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Choice Bank is accessed only through the `choice-bank-connector` service. |
| **Locked doctrine** | Frontend clients never call Choice directly. |
| **Locked doctrine** | Partner applications never use SecurePay's Choice credentials. |

## Command model

| Classification | Rule |
| --- | --- |
| **Current architectural decision** | SecurePay Core issues provider-neutral banking commands to the connector. |
| **Current architectural decision** | Public APIs should not expose Choice-specific details unnecessarily. |
| **Locked doctrine** | Canonical KS Numbers map to provider references; KS aliases do not replace canonical mappings. |

## Security and credentials

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Choice credentials are never committed to version control. |
| **Locked doctrine** | Sandbox and production credentials are separate. |
| **Locked doctrine** | Signing and verification remain inside the connector. |
| **Confirmed technical-documentation fact** | Choice BaaS requires request signing with a partner private key and response signature verification (plain SHA-256). |
| **Locked doctrine** | Sensitive provider payloads are encrypted or minimized in storage. |
| **Locked doctrine** | Logs redact sensitive provider information. |

## Callbacks and webhooks

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Webhooks and provider callbacks are signature-verified before processing. |
| **Locked doctrine** | Callbacks are idempotent and replay-resistant. |
| **Locked doctrine** | No callback is trusted before verification. |
| **Confirmed technical-documentation fact** | Choice documents callback notifications for asynchronous transaction and onboarding outcomes. |

## Reliability

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Timeouts and retries must not corrupt SecurePay state. |
| **Locked doctrine** | Provider outages must not corrupt SecurePay state. |
| **Current architectural decision** | Provider responses are reconciled against ledger commands — provider success does not replace ledger postings. |

## Provider replacement

| Classification | Rule |
| --- | --- |
| **Current architectural decision** | The adapter boundary supports future banking-provider replacement without changing public SecurePay doctrine. |
| **Engineering assumption** | A future non-Choice provider would require new ADRs and contractual review. |

## Capability classification

When documenting Choice capabilities, distinguish:

| Label | Meaning |
| --- | --- |
| **Confirmed contractual fact** | Stated in executed agreements |
| **Confirmed technical-documentation fact** | Explicit in official GitBook |
| **Sandbox-confirmed behavior** | Observed in sandbox during integration testing |
| **Production-certified behavior** | Confirmed by Choice for production go-live |
| **Engineering assumption** | Placeholder until confirmed |

**Phase 1 rule:** Unconfirmed Choice capabilities remain adapter placeholders. Do not encode them as production behavior.

## Phase 1 scope

**Confirmed:** No Choice HTTP clients, credential storage, or callback handlers are implemented in Phase 1.

## Related documents

- [Choice Bank Source Register](CHOICE_BANK_SOURCE_REGISTER.md)
- [Contract Confirmed Requirements](contract-findings/CHOICE_CONTRACT_CONFIRMED_REQUIREMENTS.md)
- [Contract Open Questions](contract-findings/CHOICE_CONTRACT_OPEN_QUESTIONS.md)
- [ADR-0004 Choice Bank adapter boundary](../decisions/ADR-0004-CHOICE-BANK-ADAPTER-BOUNDARY.md)
