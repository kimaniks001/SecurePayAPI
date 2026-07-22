# KS Number Doctrine

**Status:** Locked doctrine  
**Phase:** 1 — documentation only (no KS Number tables in this phase)

## Purpose

The KS Number is SecurePay's canonical human-facing identity identifier. It is permanent, sequential, and centrally allocated.

## Canonical numbering rules

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Canonical KS Numbers begin at `KS001`. |
| **Locked doctrine** | Numbers grow sequentially without gaps in allocation order. |
| **Locked doctrine** | Numbers below 1000 use at least three digits (`KS001` … `KS999`). |
| **Locked doctrine** | After `KS999`, continue naturally: `KS1000`, `KS1001`, and onward. |
| **Locked doctrine** | Internal database identity uses UUID. The sequence number is not the primary key. |
| **Locked doctrine** | Issued canonical KS Numbers are permanent and never reused. |
| **Locked doctrine** | Closed, suspended, deceased, or dissolved identities retain their historical number. |

## Allocation authority

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Only the central KS Number domain may allocate the next number. |
| **Locked doctrine** | Applications may not calculate, reserve, or issue KS Numbers. |
| **Locked doctrine** | Choice Bank may not control the KS Number sequence. |

## Aliases

Special memorable identities are aliases over existing canonical numbers.

| Classification | Rule |
| --- | --- |
| **Locked doctrine** | Example: `KSKEYMAN` resolves to `KS0457`. |
| **Locked doctrine** | Aliases do not disturb the sequence. |
| **Locked doctrine** | Aliases do not replace the canonical number. |
| **Locked doctrine** | Aliases are case-insensitive after normalization. |
| **Locked doctrine** | Aliases must be unique. |
| **Locked doctrine** | Financial and audit records retain the canonical KS Number. |

## Conceptual statuses

These statuses describe identity lifecycle. Final transition rules will be implemented in a later phase.

| Status | Meaning (conceptual) |
| --- | --- |
| `RESERVED` | Number held pending issuance |
| `ISSUED` | Number assigned to an identity record |
| `PENDING_VERIFICATION` | Identity verification in progress |
| `PENDING_ACTIVATION` | Awaiting activation steps |
| `ACTIVE` | Identity active for platform use |
| `INACTIVE` | Temporarily inactive |
| `RESTRICTED` | Operational restrictions apply |
| `SUSPENDED` | Suspended by policy or compliance |
| `CLOSED` | Identity closed; number retained |
| `DECEASED` | Individual deceased; number retained |
| `DISSOLVED` | Organization dissolved; number retained |

## Mapping to banking

| Classification | Statement |
| --- | --- |
| **Pending external confirmation** | Whether KS Numbers may be used as Choice virtual account or sub-account names |
| **Current architectural decision** | Canonical KS Numbers map to provider references in the connector; aliases do not replace canonical mappings |

See [Choice Bank Integration Boundary](../banking/CHOICE_BANK_INTEGRATION_BOUNDARY.md) and [Unresolved Items Register](../operations/UNRESOLVED_ITEMS_REGISTER.md).

## Phase 1 scope

**Confirmed:** No KS Number database tables, issuance APIs, or allocation logic are implemented in Phase 1.

## Related documents

- [Authentication Doctrine](AUTHENTICATION_DOCTRINE.md)
- [Financial Ledger Doctrine](FINANCIAL_LEDGER_DOCTRINE.md)
- [ADR-0001 API-first, domain-first](../decisions/ADR-0001-API-FIRST-DOMAIN-FIRST.md)
