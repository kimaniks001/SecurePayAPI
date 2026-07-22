# Choice Contract — Technical Obligations

**Status:** Confirmed contractual facts + derived technical obligations  
**Phase:** 1 — foundation documentation

## Keyman Oak / SecurePay technical obligations

Derived from confirmed contractual facts. Wording is obligation-focused; full contract text is not reproduced.

| # | Obligation | Classification |
| --- | --- | --- |
| T1 | Maintain SecurePay integration with Choice BaaS APIs | **Confirmed contractual fact** (integration and maintenance responsibility) |
| T2 | Collect and submit onboarding information and documents to Choice | **Confirmed contractual fact** |
| T3 | Handle onboarding outcome notifications from Choice | **Confirmed contractual fact** |
| T4 | Maintain integration when Choice updates supported APIs or services | **Confirmed contractual fact** |
| T5 | Implement data-security and privacy controls for platform data | **Confirmed contractual fact** |
| T6 | Operate customer-facing platform without bypassing Choice for regulated banking functions | **Current architectural decision** derived from division of responsibilities |
| T7 | Route all Choice API calls through `choice-bank-connector` | **Locked doctrine** |
| T8 | Implement idempotent financial commands and callback handling | **Locked doctrine** |
| T9 | Reconcile provider state with SecurePay ledger | **Locked doctrine** |
| T10 | Never commit Choice Sender ID, private keys, or production secrets | **Locked doctrine** |

## Choice technical obligations (summary)

| # | Obligation | Classification |
| --- | --- | --- |
| C1 | Provide banking-system APIs | **Confirmed contractual fact** |
| C2 | Determine KYC requirements and store KYC data internally | **Confirmed contractual fact** |
| C3 | Perform due diligence before account opening | **Confirmed contractual fact** |
| C4 | Process deposits, withdrawals, transfers, and payments | **Confirmed contractual fact** |
| C5 | Provide transaction and account updates as contractually supported | **Confirmed contractual fact** |
| C6 | Maintain regulated banking, AML/CFT, reporting, and continuity responsibilities | **Confirmed contractual fact** |

## Pending external confirmation

See [Open Questions](CHOICE_CONTRACT_OPEN_QUESTIONS.md) for matters not fully specified in public documentation or Phase 1 summaries.

## Related documents

- [Confirmed Requirements](CHOICE_CONTRACT_CONFIRMED_REQUIREMENTS.md)
- [Integration Boundary](../CHOICE_BANK_INTEGRATION_BOUNDARY.md)
- [Source Register](../CHOICE_BANK_SOURCE_REGISTER.md)
