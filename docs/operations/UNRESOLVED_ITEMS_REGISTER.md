# Unresolved Items Register

**Status:** Living register  
**Phase:** 1–2 foundation (living register)

## Purpose

Track ambiguity, competing interpretations, and matters requiring external confirmation. **Do not encode assumptions as doctrine.**

## Active unresolved matters

| ID | Topic | Competing interpretations / gap | Owner to confirm | Phase |
| --- | --- | --- | --- | --- |
| UR-01 | KS Number as Choice account label | May or may not be permitted as VA/sub-account name | Choice technical + legal | 2+ |
| UR-02 | SecurePay user account type | Wallet vs current vs SME per persona | Product + Choice | 2+ |
| UR-03 | Automatic bank account on KS issuance | Immediate vs onboarding-gated | Product + Choice | 2+ |
| UR-04 | Choice BaaS model for SecurePay | VA vs sub-account vs IMT | Commercial + Choice | 2+ |
| UR-05 | Production callback registration | URL approval and signing details | Choice integration | 2+ |
| UR-06 | Commercial fee schedule | Exists contractually but not in repo | Commercial leadership | 2+ |
| UR-07 | Production certification checklist | Sandbox-to-prod gates | Choice account manager | 2+ |
| UR-08 | OTP policy for outbound transfers | Default OTP vs exempted flows | Choice technical | 3+ |
| UR-09 | Evidence retention period | Legal vs operational minimums | Legal + compliance | 3+ |
| UR-10 | Agreement Review authority model | Internal vs external reviewers | Product + legal | 3+ |
| UR-11 | Spring Security for health-only Phase 2 | Permit health without fake auth vs add minimal filter | Platform engineering | 2 |

## Phase 2 note

Executable `securepay-core` health endpoints are intentionally public in local development. Production exposure rules remain a future infrastructure input (see Application–Infrastructure Contract).

## Choice-specific open questions

Full Choice contract open questions: [CHOICE_CONTRACT_OPEN_QUESTIONS.md](../banking/contract-findings/CHOICE_CONTRACT_OPEN_QUESTIONS.md)

## Resolution process

1. Add item with ID, owner, and phase target.
2. Link supporting documents.
3. When confirmed, move to ADR or doctrine with classification label.
4. Remove only after documented confirmation — never silent deletion.

## Related documents

- [Operating Doctrine](../doctrine/SECUREPAY_OPERATING_DOCTRINE.md)
- [Phase 02 Completion Report](PHASE_02_COMPLETION_REPORT.md)
