# KS Identity Domain Standard

**Status:** Current architectural decision (Phase 4 implementation)  
**Phase:** 4 — KS Number identity issuance  
**Branch:** `phase-04-ksnumber-identity-issuance`  
**Module:** `shared/platform-identity`

## Purpose

Define schema ownership, table inventory, domain rules, and service boundaries for the KS identity domain — canonical KS Number issuance, lifecycle management, and aliases.

## Schema ownership

| Item | Value |
| --- | --- |
| Schema | `identity` |
| Owner | `securepay-core` (DDL via Flyway) |
| Application module | `shared/platform-identity` |
| Migration | `database/migrations/V20260723130000__ks_identity_foundation.sql` |
| ADR | [ADR-0012](../decisions/ADR-0012-KS-NUMBER-IDENTITY-MODEL.md) |

Only the identity domain module may write to `identity.*` tables. Other domains reference identities by UUID or canonical KS Number through services — not direct SQL.

## Tables

### `identity.ks_identities`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | UUID PK | Internal identity identifier |
| `canonical_ks_number` | VARCHAR(32) UNIQUE | Formatted canonical value (`KS001`, …) |
| `sequence_number` | BIGINT UNIQUE | Allocator sequence value; > 0 |
| `identity_type` | VARCHAR(32) | `INDIVIDUAL`, `SYSTEM`, `TEST` |
| `status` | VARCHAR(32) | `PENDING`, `ACTIVE`, `SUSPENDED`, `CLOSED` |
| `display_name` | VARCHAR(128) | Optional; max 128 chars |
| `issuance_request_key` | VARCHAR(128) UNIQUE | Permanent issuance ownership key; never reused |
| `issuance_request_hash` | VARCHAR(128) | SHA-256 fingerprint of issuance command; immutable |
| `created_by_actor_type` | VARCHAR(64) | From `ActorContext` |
| `created_by_actor_id` | VARCHAR(128) | From `ActorContext` |
| `request_id` | VARCHAR(128) | Correlation to HTTP/command |
| `correlation_id` | VARCHAR(128) | Business correlation |
| `created_at` | TIMESTAMPTZ | UTC |
| `updated_at` | TIMESTAMPTZ | UTC |
| `suspended_at` | TIMESTAMPTZ | Set on `SUSPENDED` |
| `closed_at` | TIMESTAMPTZ | Set on `CLOSED` |
| `version` | BIGINT | Optimistic locking; starts at 0 |

**Indexes:** `status`, `identity_type`, `correlation_id`

**Check constraints:**

- `canonical_ks_number ~ '^KS[0-9]{3,}$'`
- Status and type enums
- Timestamp ordering
- `version >= 0`

### `identity.ks_number_aliases`

| Column | Type | Notes |
| --- | --- | --- |
| `id` | UUID PK | Alias identifier |
| `identity_id` | UUID FK → `ks_identities` | Owning identity |
| `alias` | VARCHAR(64) | Raw alias as supplied |
| `normalized_alias` | VARCHAR(64) UNIQUE | Lowercase normalized lookup key |
| `alias_type` | VARCHAR(32) | `MEMORABLE`, `LEGACY`, `SYSTEM` |
| `status` | VARCHAR(32) | `RESERVED`, `ACTIVE`, `SUSPENDED`, `RETIRED` |
| `is_primary_display_alias` | BOOLEAN | Default false |
| Actor / correlation columns | | Same pattern as identities |
| `released_at` | TIMESTAMPTZ | Set on `RETIRED` |
| `version` | BIGINT | Optimistic locking |

**Indexes:** `identity_id`, `status`

### `identity.ks_number_sequence`

PostgreSQL sequence starting at `1`. See [KS Number Issuance Standard](KS_NUMBER_ISSUANCE_STANDARD.md).

## Domain rules

| Rule | Classification | Enforcement |
| --- | --- | --- |
| Only central identity domain allocates KS Numbers | Locked doctrine | `KsIdentityIssuanceService` + sequence |
| Canonical numbers start at `KS001` | Locked doctrine | Sequence `START WITH 1` + formatter |
| Issued numbers never reused | Locked doctrine | No delete API; unique constraints |
| UUID is internal PK | Locked doctrine | `id` column |
| Aliases do not affect sequence | Locked doctrine | Separate table; no sequence reference |
| Aliases globally unique after normalization | Locked doctrine | Unique on `normalized_alias` |
| Financial/audit records use canonical number | Locked doctrine | Outbox/audit payloads include `canonical_ks_number` |
| Phase 4: no public HTTP identity APIs | Current architectural decision | Service-layer only |
| Phase 4: SYSTEM/TEST actors for issuance | Current architectural decision | `ActorContextFactory` |

## Services (`ke.securepay.platform.identity.service`)

| Service | Responsibility |
| --- | --- |
| `KsIdentityIssuanceService` | Idempotent canonical number issuance |
| `KsIdentityLifecycleService` | Identity status transitions |
| `KsAliasService` | Alias create and lifecycle |
| `KsIdentityQueryService` | Lookup by id, canonical number, or normalized alias |

## Value objects and utilities

| Type | Package | Purpose |
| --- | --- | --- |
| `KsNumber` | `ksnumber` | Immutable canonical number |
| `KsNumberFormatter` / `KsNumberParser` | `ksnumber` | Format and parse |
| `AliasNormalizer` | `alias` | Normalize and validate aliases |

## Cross-cutting integration

| Concern | Integration |
| --- | --- |
| Audit | `AuditCategory.IDENTITY`; see [Audit Event Standard](../security/AUDIT_EVENT_STANDARD.md) |
| Outbox | `IdentityOutboxWriter`; aggregate type `identity` |
| Idempotency | Operation `identity.ks-number.issue` |
| Metrics | `securepay.identity.*` via `IdentityMetricNames` |

## Auto-configuration

`PlatformIdentityAutoConfiguration` registers repositories and services when `shared/platform-identity` is on the classpath. Imported by `securepay-core`.

## Phase 4 exclusions

- No authentication or end-user actor types
- No public REST controllers for identity
- No Choice Bank account provisioning on issuance
- No payment or ledger coupling
- No Control Centre identity screens

## Related documents

- [KS Number Issuance Standard](KS_NUMBER_ISSUANCE_STANDARD.md)
- [KS Number Alias Standard](KS_NUMBER_ALIAS_STANDARD.md)
- [Database Schema Ownership Standard](DATABASE_SCHEMA_OWNERSHIP_STANDARD.md)
- [KS Number Doctrine](../domains/KS_NUMBER_DOCTRINE.md)
- [ADR-0012](../decisions/ADR-0012-KS-NUMBER-IDENTITY-MODEL.md)
- [ADR-0015](../decisions/ADR-0015-KS-IDENTITY-LIFECYCLE.md)
