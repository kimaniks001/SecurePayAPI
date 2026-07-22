# KS Alias Security Standard

**Status:** Current architectural decision (Phase 4 implementation)  
**Phase:** 4 — KS Number identity issuance  
**Branch:** `phase-04-ksnumber-identity-issuance`  
**Module:** `shared/platform-identity`

## Purpose

Define security controls for KS Number alias creation, reserved terms, impersonation prevention, and known unresolved moderation requirements.

## Threat model (Phase 4)

| Threat | Mitigation (Phase 4) |
| --- | --- |
| Impersonating canonical KS Numbers | `AliasNormalizer` + DB check constraint |
| Squatting platform role names | Reserved term blocklist |
| Phishing via URL/email/phone aliases | Pattern rejection in `AliasNormalizer` |
| Unauthorized alias creation | Service-layer only; trusted `ActorContext` |
| Alias hijack via race | Unique constraint on `normalized_alias` |

## Reserved terms

Phase 4 blocklist in `AliasNormalizer.RESERVED_TERMS` (exact match on normalized alias):

| Term |
| --- |
| `admin` |
| `administrator` |
| `securepay` |
| `support` |
| `system` |
| `root` |
| `official` |
| `helpdesk` |
| `billing` |
| `security` |
| `api` |
| `internal` |
| `staff` |
| `moderator` |

**Additional substring rules:** normalized alias must not contain `admin` or `official` as substrings.

**Classification:** Engineering assumption — list requires product, legal, and brand review before production alias self-service.

## Canonical impersonation prevention

| Layer | Control |
| --- | --- |
| Application | `CANONICAL_KS_PATTERN`: `^ks[0-9]{3,}$` (case-insensitive on input) |
| Database | `ks_number_aliases_not_canonical_format` check on `normalized_alias` |

Attackers cannot register `KS001` or `kskeyman123` as aliases when digits satisfy canonical width rules.

## Character set restriction

Allowed after normalization: `a-z`, `0-9`, `.`, `_`, `-`

Prevents Unicode homoglyph attacks and unexpected normalization behaviour in Phase 4. Internationalized aliases are **unresolved** (UR-36).

## Audit and investigation

All alias creates and status changes emit:

- Immutable audit events (`AuditCategory.IDENTITY`)
- Outbox events with `correlation_id` and actor fields

Operators investigating alias abuse should query `audit.audit_events` by `resource_type = identity_alias` or `correlation_id`.

## Access control

| Actor (Phase 4) | Alias operations |
| --- | --- |
| `SYSTEM` | Allowed via service layer |
| `TEST` | Allowed in integration tests |
| End-user / partner | **Not supported** — no authenticated actor types |

Database role grants for `identity` schema: see [Database Access Control Standard](DATABASE_ACCESS_CONTROL_STANDARD.md).

## Logging

- Do not log full alias lists in bulk export without redaction policy approval.
- Alias conflict metrics (`securepay.identity.alias.conflict`) may be alerted without logging the contested alias value in production info logs.

## Unresolved moderation matters

The following are **not implemented** in Phase 4 and remain open:

| ID | Matter | Status |
| --- | --- | --- |
| UR-28 | Operator moderation workflow (approve/reject/dispute) | Pending product |
| UR-29 | Cooling period before retired alias reuse | Pending policy |
| UR-31 | Expanded reserved-term list from legal/brand | Pending external confirmation |
| UR-35 | Resolution behaviour for non-`ACTIVE` aliases | Pending architecture |
| UR-36 | Internationalized alias policy | Pending product |
| UR-37 | Rate limiting on alias creation | Pending API phase |
| UR-38 | Automated offensive-term screening | Pending compliance |

**Do not** implement production self-service alias registration until moderation ADR is accepted.

## Phase 4 confirmations

- No public alias HTTP endpoints
- No end-user authentication for alias claims
- No integration with external trademark databases

## Related documents

- [KS Number Alias Standard](../architecture/KS_NUMBER_ALIAS_STANDARD.md)
- [ADR-0014 KS Number alias model](../decisions/ADR-0014-KS-NUMBER-ALIAS-MODEL.md)
- [Audit Event Standard](AUDIT_EVENT_STANDARD.md)
- [Unresolved Items Register](../operations/UNRESOLVED_ITEMS_REGISTER.md)
