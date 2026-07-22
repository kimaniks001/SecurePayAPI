# Financial Ledger Doctrine

**Status:** Locked doctrine  
**Phase:** 1 — documentation only (no ledger tables in this phase)

## Purpose

The financial ledger is SecurePay's authoritative financial source of truth. Agreement status, provider callbacks, and frontend displays are not substitutes for ledger position.

## Core accounting rules

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Double-entry accounting — every posted journal balances. |
| **Locked doctrine** | Ledger accounts have explicit owners, purposes, and currencies. |
| **Locked doctrine** | Posted entries are immutable. |
| **Locked doctrine** | Corrections use reversals or compensating entries — never destructive edits. |
| **Locked doctrine** | Every financial command requires idempotency. |

## Authority hierarchy

| Source | Authoritative for money? |
| --- | --- |
| Financial ledger | **Yes** |
| Choice Bank provider status | No — reconciled against ledger |
| SecureLink workflow status | No — informs but does not replace ledger |
| Frontend or partner balance display | No — read models only |

## Idempotency and provider interaction

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Duplicate callbacks and retries cannot duplicate money movement. |
| **Locked doctrine** | Failed provider operations must not create false completed positions. |
| **Locked doctrine** | Provider success does not replace ledger postings. |
| **Current architectural decision** | Reconciliation compares SecurePay ledger with provider and Choice Bank records. |

## Holds

Financial holds may support:

- agreement escrow requirements
- Agreement Reviews
- compliance restrictions
- refunds
- settlement staging

Hold semantics will be specified in a later phase. Holds must be reflected in ledger accounts, not ad hoc flags alone.

## Access and administration

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | No direct ledger-table editing through the Control Centre. |
| **Locked doctrine** | No arbitrary administrator balance adjustment. |
| **Locked doctrine** | No frontend or partner may directly edit balances. |
| **Locked doctrine** | Ledger access follows least privilege. |

## Audit and correlation

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Every posting has correlation and audit references. |
| **Locked doctrine** | Fees and referral rewards use ledger entries. |

## Core financial doctrine protection

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Core financial doctrine must not be switchable through ordinary environment variables. |

## Phase 1 scope

**Confirmed:** No ledger tables, posting APIs, or reconciliation jobs are implemented in Phase 1.

## Related documents

- [Payment Ready Doctrine](PAYMENT_READY_DOCTRINE.md)
- [Choice Bank Integration Boundary](../banking/CHOICE_BANK_INTEGRATION_BOUNDARY.md)
- [ADR-0003 PostgreSQL system of record](../decisions/ADR-0003-POSTGRESQL-SYSTEM-OF-RECORD.md)
- [Security Baseline](../security/SECUREPAY_SECURITY_BASELINE.md)
