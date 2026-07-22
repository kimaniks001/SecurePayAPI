# Unresolved Items Register

**Status:** Living register  
**Phase:** 1–4 foundation (living register)

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
| UR-23 | Identity lifecycle vs doctrine statuses | Phase 4 uses PENDING/ACTIVE/SUSPENDED/CLOSED; doctrine lists DECEASED, DISSOLVED, etc. | Product + architecture | 4 |
| UR-24 | Identity display_name PII | Encryption, masking, retention for optional display names | Legal + compliance | 4 |
| UR-25 | Public identity HTTP API | Issuance, lookup, alias endpoints and auth model | Platform engineering | 5+ |
| UR-26 | Trusted actor propagation | End-user and partner actors beyond SYSTEM/TEST | Platform engineering | 5+ |
| UR-27 | Sequence gap monitoring | Ops metrics comparing sequence last_value vs committed rows | Platform + ops | 4+ |
| UR-28 | Alias moderation workflow | Operator approve/reject/dispute for memorable aliases | Product | 4+ |
| UR-29 | Retired alias reuse | Cooling period before normalized_alias may be reassigned | Product + legal | 4+ |
| UR-30 | Primary display alias uniqueness | At most one `is_primary_display_alias` per identity | Product | 4+ |
| UR-31 | Expanded alias reserved terms | Legal/brand review of blocklist beyond Phase 4 code list | Legal + brand | 4+ |
| UR-32 | Auto-activation on verification | Whether PENDING → ACTIVE is automatic on first verification event | Product | 5+ |
| UR-33 | Identity-alias suspension cascade | Whether identity SUSPENDED forces alias SUSPENDED | Architecture | 4+ |
| UR-34 | Identity closure banking hooks | Choice account closure when identity reaches CLOSED | Product + Choice | 4+ |
| UR-35 | Alias resolution for inactive aliases | Query behaviour when alias status is not ACTIVE | Architecture | 4+ |
| UR-36 | Internationalized aliases | Unicode, homoglyph, and locale policy | Product | 4+ |
| UR-37 | Alias creation rate limiting | Abuse prevention when public APIs exist | Security engineering | 5+ |
| UR-38 | Offensive-term alias screening | Automated moderation beyond reserved-term blocklist | Compliance | 4+ |

## Phase 4 note

Phase 4 implements KS Number identity (issuance, aliases, lifecycle) as service-layer domain logic only. Public APIs, authentication, alias self-service moderation, and Choice Bank account provisioning on issuance remain unresolved. See [Phase 04 Completion Report](PHASE_04_COMPLETION_REPORT.md) and [KS Alias Security Standard](../security/KS_ALIAS_SECURITY_STANDARD.md).

**Permanent duplicate-issuance safeguard:** `identity.ks_identities.issuance_request_key` uniqueness plus `issuance_request_hash` permanently preserve issuance ownership. Canonical KS Numbers and issuance request keys are never reused.

**Unresolved legal retention:** Idempotency replay record retention for `identity.ks-number.issue` (including `IDENTITY_ISSUE_REPLAY_STORAGE_EXPIRY` = 90 days as provisional operational replay storage) is **not** a legal retention decision. No invented legal or permanent retention period is encoded. Deleting or expiring idempotency replay data cannot authorize a second identity when an identity row already owns the issuance key.

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
- [Phase 03 Completion Report](PHASE_03_COMPLETION_REPORT.md)
- [Phase 04 Completion Report](PHASE_04_COMPLETION_REPORT.md)
