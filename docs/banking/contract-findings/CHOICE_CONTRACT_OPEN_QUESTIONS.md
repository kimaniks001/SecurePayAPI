# Choice Contract — Open Questions

**Status:** Pending external confirmation  
**Phase:** 1 — unresolved matters register (Choice-specific)

Do not encode assumptions below as doctrine or implementation.

| ID | Matter | Competing interpretations / notes | Who must confirm |
| --- | --- | --- | --- |
| CQ-01 | Exact account type for each SecurePay user | Wallet vs current vs SME vs VA/sub-account models | Choice account management + Keyman product |
| CQ-02 | Automatic account creation | On KS Number issuance vs explicit onboarding flow | Choice + Keyman product |
| CQ-03 | KS Number as VA/sub-account name | Public docs do not confirm KS Number as account label | Choice technical + legal |
| CQ-04 | Enabled Choice products | VA, sub-account, IMT, PesaLink sponsorship | Choice account management |
| CQ-05 | Production transaction limits | Per rail and per account type | Choice operations |
| CQ-06 | Callback configuration | URLs, types, signing, retry policy | Choice technical integration |
| CQ-07 | Settlement and reconciliation structure | Pay-in/pay-out timing vs ledger posting | Choice operations + SecurePay finance |
| CQ-08 | KYC exception ownership | Rejected/incomplete onboarding handling | Choice compliance + Keyman support |
| CQ-09 | Incomplete or rejected onboarding | User messaging and retry authority | Keyman product + Choice |
| CQ-10 | Retention and deletion | KYC and transaction record retention split | Legal + Choice compliance |
| CQ-11 | API change control | Notice period and deprecation process | Choice + Keyman integration lead |
| CQ-12 | Commercial figures | Fees, revenue share, minimums | Commercial leadership |
| CQ-13 | Certification process | Sandbox-to-production checklist | Choice account management |
| CQ-14 | Production base URL and credentials issuance | Separate from sandbox pilot URL | Choice account management |
| CQ-15 | OTP requirements for transfers | Default vs OTP-exempted enablement | Choice technical |

## Process

1. Document new questions here when ambiguity is discovered.
2. Mark status in [Unresolved Items Register](../../operations/UNRESOLVED_ITEMS_REGISTER.md).
3. Do not implement production behavior based on unresolved items.

## Related documents

- [Confirmed Requirements](CHOICE_CONTRACT_CONFIRMED_REQUIREMENTS.md)
- [Choice Bank Source Register](../CHOICE_BANK_SOURCE_REGISTER.md)
- [Integration Boundary](../CHOICE_BANK_INTEGRATION_BOUNDARY.md)
