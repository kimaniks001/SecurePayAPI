# Choice Bank Source Register

**Status:** Living register — Phase 1 public documentation review  
**Official source:** https://choice-bank.gitbook.io/choice-bank  
**Date reviewed:** 2026-07-22  
**Reviewer:** SecurePay Phase 1 foundation agent

## Classification legend

| Label | Meaning |
| --- | --- |
| **Confirmed technical-documentation fact** | Explicitly stated in official GitBook |
| **Matters requiring contract confirmation** | Public docs insufficient or silent |
| **Security observation** | Security-relevant note from documentation review |
| **Future implementation phase** | When SecurePay should implement |

Public documentation does **not** prove legal account ownership, production approval, commercial charges, safeguarding structure, KYC allocation, automatic account creation, or approval to use KS Number as a bank account name.

---

## Overview

| Field | Value |
| --- | --- |
| Source name | Choice BaaS API Documentation — Overview |
| Source type | Official technical documentation (GitBook) |
| Source URL | https://choice-bank.gitbook.io/choice-bank |
| Date reviewed | 2026-07-22 |
| Sections reviewed | Introduction, message templates, terminologies |
| Future implementation phase | Phase 2+ (connector scaffolding) |

### Confirmed technical capabilities

- **Confirmed technical-documentation fact:** Choice BaaS is a Banking-as-a-Service platform exposing JSON APIs for partners to launch financial products.
- **Confirmed technical-documentation fact:** Partners typically operate customer front-end and back-end; transactions call Choice BaaS APIs.
- **Confirmed technical-documentation fact:** Integration requires a **Sender ID** and **Private Key** obtained from an account manager.
- **Confirmed technical-documentation fact:** Sandbox integration precedes production Sender ID and private key issuance.
- **Confirmed technical-documentation fact:** Request template fields: `requestId`, `sender`, `locale`, `timestamp`, `salt`, `signature`, `params`.
- **Confirmed technical-documentation fact:** Response template includes `code`, `msg`, `requestId`, `sender`, `locale`, `timestamp`, `salt`, `signature`, `data`.
- **Confirmed technical-documentation fact:** Success response code is `"00000"`.
- **Confirmed technical-documentation fact:** Default locale documented as `en_KE`.

### Matters requiring contract confirmation

- Production base URL and environment-specific endpoints
- Commercial charges and transaction limits
- Exact products enabled for Keyman Oak / SecurePay

### Security observations

- Private keys must be stored in approved secrets manager — never in Git.
- `requestId` must be unique per API call (replay consideration).

---

## FAQs

| Field | Value |
| --- | --- |
| Source URL | https://choice-bank.gitbook.io/choice-bank/getting-started/faqs.md |
| Date reviewed | 2026-07-22 |
| Future implementation phase | Reference for all phases |

### Confirmed technical capabilities

- **Confirmed technical-documentation fact:** Most documented APIs work with API-onboarded accounts: wallets, current, and SME accounts (excluding Merchant section and certain statement APIs per FAQ note).
- **Confirmed technical-documentation fact:** BaaS models include Virtual Account, Sub-account, and IMT.
- **Confirmed technical-documentation fact:** Wallet accounts for individuals have transactional limits; current accounts require additional KYC (KRA PIN).
- **Confirmed technical-documentation fact:** SME accounts are for registered business entities onboarded via API.
- **Confirmed technical-documentation fact:** Merchant accounts are onboarded via accounts/operations team, not API — Merchant ≠ SME.
- **Confirmed technical-documentation fact:** API signing uses plain SHA-256.
- **Confirmed technical-documentation fact:** Choice Bank code: 46; SWIFT code documented as 82.
- **Confirmed technical-documentation fact:** Typical sandbox integration timeline cited as 1–2 months (partner-dependent).
- **Confirmed technical-documentation fact:** Bank support hours documented as 8am–4:30pm EAT with off-hours support for serious downtimes.

### Matters requiring contract confirmation

- Which BaaS model applies to SecurePay (VA, sub-account, or IMT)
- Production SLA and support escalation for SecurePay
- PesaLink sponsorship applicability

---

## Authentication

| Field | Value |
| --- | --- |
| Source URL | https://choice-bank.gitbook.io/choice-bank/getting-started/authentication.md |
| Date reviewed | 2026-07-22 |
| Future implementation phase | Choice connector — signing module |

### Confirmed technical capabilities

- **Confirmed technical-documentation fact:** All requests must be signed; invalid signatures are rejected.
- **Confirmed technical-documentation fact:** Signing: add `salt`, add `senderKey` (private key) temporarily, flatten JSON to sorted `key=value` pairs joined by `&`, hash with plain SHA-256, set `signature`, remove `senderKey` before send.
- **Confirmed technical-documentation fact:** Response verification mirrors request signing using partner private key.
- **Confirmed technical-documentation fact:** Reference implementations provided for Java, Node.js, Ruby, Python.

### Security observations

- Signing and verification must remain inside `choice-bank-connector`.
- Connector must never log `senderKey` or full signed payloads containing secrets.

---

## Sandbox Environment

| Field | Value |
| --- | --- |
| Source URL | https://choice-bank.gitbook.io/choice-bank/getting-started/sandbox-environment.md |
| Date reviewed | 2026-07-22 |
| Future implementation phase | Connector sandbox configuration |

### Confirmed technical capabilities

- **Confirmed technical-documentation fact:** Sandbox base URL: `https://baas-pilot.choicebankapi.com/`
- **Confirmed technical-documentation fact:** Sandbox private key obtained from account manager.
- **Confirmed technical-documentation fact:** `POST /account/closeSandBoxAccount` closes test accounts without OTP or manual review; result sent via callback type 0019.

### Matters requiring contract confirmation

- Production base URL
- Sandbox account provisioning process for Keyman Oak

---

## Account types (Current Account overview via sitemap)

| Field | Value |
| --- | --- |
| Source URL | https://choice-bank.gitbook.io/choice-bank/sitemap.md (Current Account, Wallet, SME sections) |
| Date reviewed | 2026-07-22 |
| Future implementation phase | Onboarding design (Phase 3+) |

### Confirmed technical capabilities

- **Confirmed technical-documentation fact:** Personal accounts: wallet and current accounts.
- **Confirmed technical-documentation fact:** Business accounts: SME and joint accounts.
- **Confirmed technical-documentation fact:** Onboarding APIs exist for individual and SME accounts.
- **Confirmed technical-documentation fact:** SME supports adding accounts / creating VAs.
- **Confirmed technical-documentation fact:** Query onboarding info APIs exist for individual and SME onboarding attempts.

### Matters requiring contract confirmation

- Exact account type for each SecurePay user persona
- Whether KS Number may label a VA or sub-account
- Automatic account creation on KS Number issuance

---

## Account management

| Field | Value |
| --- | --- |
| Source URL | Sitemap — Account Management section |
| Date reviewed | 2026-07-22 |
| Future implementation phase | Post-onboarding operations |

### Confirmed technical capabilities (from sitemap — pages not individually fetched in Phase 1)

- **Confirmed technical-documentation fact:** APIs documented for: add personal accounts, generate statements, change contact details, confirm phone number change.

---

## Deposits

| Field | Value |
| --- | --- |
| Source URL | Sitemap — Deposit from M-PESA, Deposit from Banks |
| Date reviewed | 2026-07-22 |
| Future implementation phase | Funding rails (later phase) |

### Confirmed technical capabilities

- **Confirmed technical-documentation fact:** M-PESA deposit APIs documented.
- **Confirmed technical-documentation fact:** Bank deposit APIs documented.
- **Confirmed technical-documentation fact:** Terminology includes STK Push, Paybill, Till/BuyGoods.

### Matters requiring contract confirmation

- Enabled deposit channels for SecurePay production
- Settlement and reconciliation structure for pay-ins

---

## Transfers

| Field | Value |
| --- | --- |
| Source URL | Sitemap — General Transfer, Quick Transfer, B2B, RTGS/EFT, SWIFT, Bulk |
| Date reviewed | 2026-07-22 |
| Future implementation phase | Release/settlement (later phase) |

### Confirmed technical capabilities

- **Confirmed technical-documentation fact:** General Transfer APIs documented.
- **Confirmed technical-documentation fact:** Quick Transfer documented.
- **Confirmed technical-documentation fact:** M-PESA PayBill/Till (B2B) documented.
- **Confirmed technical-documentation fact:** Bank Transfer (RTGS/EFT) documented — RTGS for high-value immediate settlement; EFT batch/overnight per terminology.
- **Confirmed technical-documentation fact:** International Transfer (SWIFT) documented.
- **Confirmed technical-documentation fact:** SME Bulk Transfer documented.
- **Confirmed technical-documentation fact:** OTP-exempted transfers documented as a feature area.
- **Confirmed technical-documentation fact:** PesaLink described as Kenya interbank real-time transfers.

### Matters requiring contract confirmation

- Production transaction limits per rail
- OTP requirements for SecurePay-initiated transfers

---

## OTP confirmation

| Field | Value |
| --- | --- |
| Source URL | Sitemap — OTP-exempted transfers; Authentication/FAQs terminology |
| Date reviewed | 2026-07-22 |
| Future implementation phase | Connector + notification coordination |

### Confirmed technical capabilities

- **Confirmed technical-documentation fact:** OTP defined as time-sensitive single-use code via SMS or app for high-risk transactions.
- **Confirmed technical-documentation fact:** OTP-exempted transfer capability documented (subject to enablement).

---

## Callbacks

| Field | Value |
| --- | --- |
| Source URL | Sitemap — Callback Notifications, Scenarios (pay-ins/-outs) |
| Date reviewed | 2026-07-22 |
| Future implementation phase | Webhook-service + connector |

### Confirmed technical capabilities

- **Confirmed technical-documentation fact:** Callback URL is partner-provided endpoint for asynchronous notifications.
- **Confirmed technical-documentation fact:** Webhook notifications described as HTTP callbacks with structured JSON/XML payloads.
- **Confirmed technical-documentation fact:** Sandbox account close uses callback notification type 0019.

### Matters requiring contract confirmation

- Production callback URL registration process
- Full callback type catalog and signature scheme for inbound webhooks

---

## Enumerations, IDs, codes, and types

| Field | Value |
| --- | --- |
| Source URL | Sitemap — Lookup APIs, Type/Status IDs, Error Codes |
| Date reviewed | 2026-07-22 |
| Future implementation phase | Connector reference data module |

### Confirmed technical capabilities

- **Confirmed technical-documentation fact:** Lookup APIs return lists when called with null parameters.
- **Confirmed technical-documentation fact:** Type/Status IDs and Error Codes sections exist in documentation.

### Matters requiring contract confirmation

- Production error code handling requirements for partner-facing messages

---

## Error codes

| Field | Value |
| --- | --- |
| Source URL | https://choice-bank.gitbook.io/choice-bank/appendix/error-codes.md (referenced in sitemap) |
| Date reviewed | 2026-07-22 — index only; full appendix not exhaustively copied |
| Future implementation phase | Connector error mapping |

### Confirmed technical capabilities

- **Confirmed technical-documentation fact:** Non-`00000` codes indicate failure; dedicated Error Codes appendix exists.

---

## Phase 1 actions taken

- Recorded only facts supported by official documentation or sitemap structure.
- Did not invent endpoint paths beyond those explicitly retrieved (sandbox close account, message templates).
- Deferred sandbox-confirmed and production-certified behavior to integration phases.

## Related documents

- [Integration Boundary](CHOICE_BANK_INTEGRATION_BOUNDARY.md)
- [Contract Open Questions](contract-findings/CHOICE_CONTRACT_OPEN_QUESTIONS.md)
- [Unresolved Items Register](../operations/UNRESOLVED_ITEMS_REGISTER.md)
