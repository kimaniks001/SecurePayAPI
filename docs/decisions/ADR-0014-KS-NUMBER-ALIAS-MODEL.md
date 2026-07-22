# ADR-0014: KS Number Alias Model

| Field | Value |
| --- | --- |
| **Status** | Accepted |
| **Date** | 2026-07-23 |
| **Phase** | 4 |
| **Branch** | `phase-04-ksnumber-identity-issuance` |

## Context

KS Number doctrine defines memorable aliases (e.g. `KSKEYMAN` → `KS0457`) as case-insensitive overlays on canonical numbers. Aliases must not disturb the canonical sequence, must remain unique platform-wide, and must not impersonate canonical KS Number formats.

Phase 4 introduces `identity.ks_number_aliases` and the `KsAliasService` for alias creation and lifecycle transitions. Normalization and reserved-term policy must be enforced before insert because uniqueness is keyed on `normalized_alias`.

## Decision

Implement aliases as **first-class rows** linked to `identity.ks_identities`:

| Column / rule | Value |
| --- | --- |
| Table | `identity.ks_number_aliases` |
| Uniqueness | Global unique on `normalized_alias` |
| Canonical impersonation | DB check: `normalized_alias !~ '^ks[0-9]{3,}$'`; application `AliasNormalizer` rejects canonical patterns |
| Normalization | Trim; lowercase (`Locale.ROOT`); allowed charset `[a-z0-9._-]` |
| Length | 3–32 characters |
| Alias type | `MEMORABLE`, `LEGACY`, `SYSTEM` |
| Initial status | `RESERVED` on create |
| Lifecycle | `RESERVED` → `ACTIVE` \| `RETIRED`; `ACTIVE` → `SUSPENDED` \| `RETIRED`; `SUSPENDED` → `ACTIVE` \| `RETIRED` |
| Primary display flag | `is_primary_display_alias` (boolean; uniqueness enforcement deferred) |
| Resolution | `KsIdentityQueryService.findByNormalizedAlias` joins alias → identity |

**Rejected alias patterns** (application layer via `AliasNormalizer`):

- Canonical KS Number format (`KS001`, `ks9999`, …)
- Email-like strings (`*@*.*`)
- URL schemes (`http://`, `ftp://`, …)
- Phone-like digit strings
- Control characters or surrounding whitespace
- Reserved terms: `admin`, `administrator`, `securepay`, `support`, `system`, `root`, `official`, `helpdesk`, `billing`, `security`, `api`, `internal`, `staff`, `moderator`
- Substrings `admin` or `official` anywhere in normalized alias

**Audit and outbox:**

- Create: `identity.alias.created`
- Status change: `identity.alias.status.changed`

## Consequences

### Positive

- Global alias uniqueness prevents routing ambiguity.
- Normalization ensures case-insensitive lookup without functional indexes on `LOWER(alias)`.
- Aliases are auditable and emit outbox events for downstream index rebuilds.

### Negative

- Reserved-term list is static in code; moderation workflow for disputed aliases is not implemented.
- `is_primary_display_alias` is stored but not constrained to one per identity in Phase 4.
- Alias creation does not validate identity `ACTIVE` status — reserved aliases may exist while identity is `PENDING`.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Store aliases in JSON on `ks_identities` | Poor uniqueness enforcement; harder audit per alias |
| Case-sensitive uniqueness | Violates doctrine |
| Allow canonical-format aliases | Impersonation and payment-routing risk |
| Separate `alias` schema | Unnecessary split; aliases are identity domain |
| Redis alias cache as source of truth | Violates PostgreSQL authority |

## Security impact

- Reserved terms reduce phishing and impersonation of platform roles.
- Alias squatting on high-value names requires future moderation and dispute process (UR-28).
- No public alias registration API in Phase 4.

## Operational impact

- Monitor `securepay.identity.alias.created` and `securepay.identity.alias.conflict` (duplicate normalized alias).
- Operators cannot release aliases via Control Centre in Phase 4 — service-layer only.
- Retired aliases set `released_at`; normalized value remains unique (no reuse in Phase 4).

## Migration impact

- `identity.ks_number_aliases` created with FK to `ks_identities`, unique on `normalized_alias`, optimistic locking `version`.
- No seed aliases in migration.

## Concurrency impact

- Duplicate alias inserts race to unique constraint on `normalized_alias`; loser receives `DuplicateKeyException` and conflict metric.
- Alias lifecycle updates use optimistic locking on `version`.
- No sequence involvement — aliases do not affect `ks_number_sequence`.

## Unresolved matters

- Full alias moderation workflow (approval, appeals, operator override) — UR-28.
- Whether retired aliases may be reassigned after cooling period — UR-29.
- Enforcement of single `is_primary_display_alias` per identity — UR-30.
- Expanded reserved-term list from product/legal — UR-31.
- Public alias availability check API — UR-25 (shared with identity API phase).

## Related documents

- [KS Number Alias Standard](../architecture/KS_NUMBER_ALIAS_STANDARD.md)
- [KS Alias Security Standard](../security/KS_ALIAS_SECURITY_STANDARD.md)
- [ADR-0012 KS Number identity model](ADR-0012-KS-NUMBER-IDENTITY-MODEL.md)
- [KS Number Doctrine](../domains/KS_NUMBER_DOCTRINE.md)
