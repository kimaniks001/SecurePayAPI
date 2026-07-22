# KS Number Alias Standard

**Status:** Current architectural decision (Phase 4 implementation)  
**Phase:** 4 — KS Number identity issuance  
**Branch:** `phase-04-ksnumber-identity-issuance`  
**Module:** `shared/platform-identity`

## Purpose

Define the alias data model, normalization rules, lifecycle, and resolution behaviour for memorable KS Number aliases.

## Alias model

Aliases are **secondary identifiers** bound to exactly one `identity.ks_identities` row.

| Principle | Rule |
| --- | --- |
| Canonical authority | Canonical `KS###` number remains authoritative for financial and audit records |
| Sequence independence | Creating an alias does not call `ks_number_sequence` |
| Global uniqueness | `normalized_alias` is unique across the platform |
| Case insensitivity | Lookup uses normalized lowercase form |
| No canonical impersonation | Aliases cannot match `^ks[0-9]{3,}$` |

## Normalization (`AliasNormalizer`)

### Input rules

| Rule | Requirement |
| --- | --- |
| Null | Rejected |
| Surrounding whitespace | Rejected (must match trimmed input) |
| Length | 3–32 characters inclusive |
| Control characters | Rejected |
| Charset (after lowercasing) | `[a-z0-9._-]+` only |

### Rejection patterns

| Pattern | Example | Reason |
| --- | --- | --- |
| Canonical KS format | `KS001`, `ks9999` | Impersonation |
| Email-like | `user@example.com` | Misleading contact format |
| URL scheme | `https://evil` | Phishing |
| Phone-like | `+254712345678` | Misleading contact format |
| Reserved term (exact) | `admin`, `securepay`, … | Platform impersonation |
| Substring `admin` or `official` | `myadminshop` | Misleading administrative term |

### Normalization output

```java
NormalizedAlias(String rawAlias, String normalizedAlias)
// normalizedAlias = trimmed.toLowerCase(Locale.ROOT)
```

Stored in `identity.ks_number_aliases`:

- `alias` = raw (validated) value
- `normalized_alias` = lookup key

## Alias types

| Type | Intended use (Phase 4) |
| --- | --- |
| `MEMORABLE` | Human-chosen memorable names |
| `LEGACY` | Imported or migrated aliases |
| `SYSTEM` | Platform-assigned aliases |

## Lifecycle

```
RESERVED → ACTIVE | RETIRED
ACTIVE → SUSPENDED | RETIRED
SUSPENDED → ACTIVE | RETIRED
```

| Status | Meaning |
| --- | --- |
| `RESERVED` | Created; not yet active for resolution |
| `ACTIVE` | Eligible for resolution via query service |
| `SUSPENDED` | Temporarily blocked |
| `RETIRED` | Released; `released_at` set; row retained |

**Service:** `KsAliasService.createAlias` / `transitionAlias`  
**Initial status on create:** `RESERVED`

## Resolution

`KsIdentityQueryService.findByNormalizedAlias(String)`:

1. Lowercase input
2. Lookup alias by `normalized_alias`
3. Load owning identity by `identity_id`

Phase 4 does not filter by alias `ACTIVE` status in query — resolution behaviour for non-active aliases is **unresolved** (UR-35).

## Primary display alias

`is_primary_display_alias` flag on create (`CreateAliasCommand.primaryDisplayAlias`). Phase 4 does **not** enforce at most one primary per identity.

## Audit and outbox events

| Event | Trigger |
| --- | --- |
| `identity.alias.created` | `createAlias` |
| `identity.alias.status.changed` | `transitionAlias` |

## Database constraints

- `UNIQUE (normalized_alias)`
- `normalized_alias !~ '^ks[0-9]{3,}$'` (case-insensitive pattern on stored lowercase value)
- FK `identity_id` → `ks_identities(id)`

## Metrics

| Metric | When |
| --- | --- |
| `securepay.identity.alias.created` | Successful create |
| `securepay.identity.alias.conflict` | Duplicate `normalized_alias` |

## Phase 4 exclusions

- No public alias registration API
- No alias transfer between identities
- No alias reuse after `RETIRED`
- No full moderation queue

## Related documents

- [ADR-0014 KS Number alias model](../decisions/ADR-0014-KS-NUMBER-ALIAS-MODEL.md)
- [KS Alias Security Standard](../security/KS_ALIAS_SECURITY_STANDARD.md)
- [KS Identity Domain Standard](KS_IDENTITY_DOMAIN_STANDARD.md)
