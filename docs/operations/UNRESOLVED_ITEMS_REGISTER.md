# Unresolved Items Register

**Status:** Living register  
**Phase:** 1–3 foundation (living register)

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
| UR-12 | Legal retention periods | Audit, idempotency, outbox, financial records | Legal + compliance | 3+ |
| UR-13 | Production database role provisioning | Least-privilege PostgreSQL roles | Platform + DBA | 3+ |
| UR-14 | Idempotency response encryption | Replayable response storage at rest | Security engineering | 3+ |
| UR-15 | Outbox broker selection | Kafka, RabbitMQ, SNS/SQS, or other | Platform engineering | 3+ |
| UR-16 | Outbox polling vs CDC | Delivery mechanism trade-offs | Platform engineering | 3+ |
| UR-17 | Event ordering guarantees | Per-aggregate vs global ordering | Architecture | 3+ |
| UR-18 | Audit integrity chaining | Hash chain or external notarization requirements | Security + compliance | 3+ |
| UR-19 | Partitioning thresholds | When to partition audit/outbox/idempotency tables | Platform + DBA | 3+ |
| UR-20 | Production archival location | Long-term audit and event storage | Operations + legal | 3+ |
| UR-21 | Regulator audit export format | Partner/regulator export requirements | Compliance | 3+ |
| UR-22 | Reporting store design | Read models vs direct reporting queries | Architecture | 4+ |

## Phase 3 note

Phase 3 establishes technical persistence foundations only. Retention durations, production database roles, broker selection, and encryption of replayable idempotency responses remain unresolved and documented in [DATA_RETENTION_AND_PARTITIONING_STANDARD.md](DATA_RETENTION_AND_PARTITIONING_STANDARD.md) and related ADRs.

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
