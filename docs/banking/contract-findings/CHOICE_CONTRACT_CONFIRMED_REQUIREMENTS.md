# Choice Contract — Confirmed Requirements

**Status:** Confirmed contractual facts (summarized — no confidential text reproduced)  
**Phase:** 1 — foundation documentation  
**Governing law:** Kenyan law (confirmed contractual fact)

## Classification

This document records **confirmed contractual facts** derived from executed agreements between Choice Microfinance Bank Limited and Keyman Oak Limited. It does not reproduce confidential contract text, signatures, personal contacts, or full commercial schedules.

## Confirmed requirements

| # | Confirmed contractual fact |
| --- | --- |
| 1 | Choice Microfinance Bank Limited is the regulated banking provider. |
| 2 | Keyman Oak operates the partner platform and customer-facing frontend. |
| 3 | Choice provides APIs supporting banking-system capabilities. |
| 4 | Choice supports end-user onboarding and account-opening processes. |
| 5 | Keyman Oak collects and submits onboarding information and documents. |
| 6 | Choice determines KYC requirements and stores KYC data internally. |
| 7 | Choice performs due diligence before account opening. |
| 8 | Onboarding outcomes are communicated to the partner. |
| 9 | Choice processes customer-account transactions including deposits, withdrawals, transfers, and payments. |
| 10 | Choice provides transaction and account updates as contractually supported. |
| 11 | Choice carries regulated banking, AML/CFT, regulatory-reporting, and banking-continuity responsibilities, working with Keyman Oak where required. |
| 12 | Keyman Oak is responsible for customer acquisition. |
| 13 | Keyman Oak is responsible for frontline customer support. |
| 14 | Keyman Oak is responsible for the customer-facing platform. |
| 15 | Keyman Oak is responsible for integration and maintenance. |
| 16 | Keyman Oak must maintain integration when Choice updates supported APIs or services. |
| 17 | Keyman Oak has data-security and privacy obligations. |
| 18 | Choice safeguards customer funds within the regulated structure described by the agreement. |
| 19 | Commercial charges and revenue-sharing arrangements exist. |
| 20 | The agreement contains confidentiality and data-protection obligations. |
| 21 | Kenyan law governs the agreement. |

## SecurePay implications

| Classification | Implication |
| --- | --- |
| **Current architectural decision** | SecurePay platform code is part of Keyman Oak's integration responsibility. |
| **Locked doctrine** | Choice Bank accessed only via `choice-bank-connector`; partners never hold Choice credentials. |
| **Locked doctrine** | KYC data storage at Choice — SecurePay must not assume local custody of full KYC payloads without explicit design. |

## Related documents

- [Technical Obligations](CHOICE_CONTRACT_TECHNICAL_OBLIGATIONS.md)
- [Commercial Rules Register](CHOICE_CONTRACT_COMMERCIAL_RULES_REGISTER.md)
- [Open Questions](CHOICE_CONTRACT_OPEN_QUESTIONS.md)
