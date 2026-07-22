# Payment Ready Doctrine

**Status:** Locked doctrine  
**Phase:** 1 — documentation only (no Payment Ready engine in this phase)

## Definition

**Payment Ready** is a deterministic and explainable backend evaluation result indicating whether a SecureLink may proceed toward release/settlement under recorded agreement terms.

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Payment Ready must be calculated by backend domain logic. |
| **Locked doctrine** | Payment Ready may never be directly assigned by a user, frontend, partner application, or administrator. |
| **Locked doctrine** | No manual Payment Ready override is allowed. |

An authorized administrator may correct **source data** through an audited process, after which the engine **reevaluates**. The administrator does not set Payment Ready directly.

## Inputs (non-exhaustive)

Payment Ready evaluation may depend on:

| Input | Classification |
| --- | --- |
| Confirmed funding | Locked doctrine |
| Accepted agreement version | Locked doctrine |
| Conditions and milestones | Locked doctrine |
| Evidence submitted | Locked doctrine |
| Evidence verification (where required) | Locked doctrine |
| Approvals | Locked doctrine |
| Time requirements | Locked doctrine |
| Agreement Reviews | Locked doctrine |
| Compliance restrictions | Locked doctrine |
| Legal restrictions | Locked doctrine |
| Valid settlement destination | Locked doctrine |
| Sufficient authoritative ledger position | Locked doctrine |

## Conceptual outcomes

| Outcome | Meaning |
| --- | --- |
| `NOT_READY` | One or more requirements unsatisfied |
| `READY` | All requirements satisfied for configured release |
| `PARTIALLY_READY` | Some release tranches ready; others blocked |
| `BLOCKED` | Hard block (review, compliance, legal, or operational) |

## Required explanation payload

Every evaluation must explain:

| Field | Requirement |
| --- | --- |
| Satisfied requirements | What passed |
| Outstanding requirements | What remains |
| Responsible person or authority | Who must act |
| Blocking reviews | Active Agreement Reviews |
| Compliance or legal blocks | Active restrictions |
| Agreement version evaluated | Exact version reference |
| Evaluation time | UTC timestamp |

## Relationship to other concepts

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Funding does not automatically imply `READY`. |
| **Locked doctrine** | `READY` does not guarantee settlement success. |
| **Locked doctrine** | Provider success does not replace ledger truth. |

## Phase 1 scope

**Confirmed:** No Payment Ready engine, APIs, or persistence are implemented in Phase 1.

## Related documents

- [SecureLink State Machine](SECURELINK_STATE_MACHINE.md)
- [Financial Ledger Doctrine](FINANCIAL_LEDGER_DOCTRINE.md)
- [Control Centre Requirements](../operations/CONTROL_CENTRE_REQUIREMENTS.md)
