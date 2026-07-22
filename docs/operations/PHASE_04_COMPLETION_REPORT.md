# Phase 04 Completion Report

**Objective:** Implement the KS Number identity domain — canonical sequential issuance, idempotent commands, aliases, lifecycle, audit, and transactional outbox — as the first business domain on Phase 3 persistence foundations.

**Branch:** `phase-04-ksnumber-identity-issuance`  
**Date:** 2026-07-23  
**Status:** Implementation complete — awaiting commit approval

## Persistence approach

**Spring Data JDBC** (consistent with Phases 2–3). Flyway owns all schema changes. No Hibernate/JPA or auto-DDL.

## Migration

| File | Description |
| --- | --- |
| `database/migrations/V20260723130000__ks_identity_foundation.sql` | Creates `identity` schema, `ks_identities`, `ks_number_aliases`, `ks_number_sequence`; updates `platform_phase` metadata |

## Database schema

| Schema | Purpose |
| --- | --- |
| `identity` | KS Number identity domain |

### Tables

| Table | Key features |
| --- | --- |
| `identity.ks_identities` | Canonical KS Number, sequence, type, lifecycle status, `issuance_request_key`, `issuance_request_hash`, optimistic locking |
| `identity.ks_number_aliases` | Memorable aliases, normalized uniqueness, lifecycle, FK to identity |

### Sequence

| Object | Configuration |
| --- | --- |
| `identity.ks_number_sequence` | `START WITH 1`, `INCREMENT BY 1`, `MINVALUE 1`, `NO MAXVALUE`, `NO CYCLE` |

First committed issuance produces canonical number **`KS001`**.

## Canonical KS Number model

| Rule | Implementation |
| --- | --- |
| Format | `KS` + digits; minimum width 3 (`KsNumberFormatter`) |
| Allocation | PostgreSQL `nextval('identity.ks_number_sequence')` |
| Permanence | Unique `canonical_ks_number` and `sequence_number`; no delete API |
| Internal ID | UUID `id` column |

## Module structure

| Module | Responsibility |
| --- | --- |
| `shared/platform-identity` | Identity domain services, repositories, value objects, outbox writer |
| `shared/platform-persistence` | Extended idempotency operation `identity.ks-number.issue` |
| `services/securepay-core` | Wires modules; hosts integration tests |

### Services (`ke.securepay.platform.identity.service`)

| Service | Role |
| --- | --- |
| `KsIdentityIssuanceService` | Idempotent canonical issuance |
| `KsIdentityLifecycleService` | `PENDING` → `ACTIVE` → `SUSPENDED` / `CLOSED` |
| `KsAliasService` | Alias create and lifecycle |
| `KsIdentityQueryService` | Lookup by id, canonical number, normalized alias |

## Idempotency and permanent duplicate protection

| Item | Value |
| --- | --- |
| Operation | `identity.ks-number.issue` |
| Idempotency key | `issuance_request_key` |
| Permanent safeguard | `identity.ks_identities.issuance_request_key` UNIQUE + `issuance_request_hash` |
| Replay storage expiry | 90 days (`IDENTITY_ISSUE_REPLAY_STORAGE_EXPIRY`) — operational only; **not** legal retention |
| Replay metric | `securepay.identity.issuance.replayed` |

Legal retention of idempotency replay records remains **unresolved**. The identity row permanently preserves issuance ownership. Expired or deleted idempotency data cannot authorize a second identity for the same issuance key.

## Concurrency

| Mechanism | Usage |
| --- | --- |
| PostgreSQL sequence | Atomic `nextval` for distinct sequence numbers under parallel issuers |
| Idempotency lock | Prevents duplicate identity for same issuance key |
| Optimistic locking (`version`) | Identity and alias lifecycle updates |

Validated by `KsIdentityIssuanceConcurrencyIntegrationTest` (12 parallel threads).

## Aliases

- Table: `identity.ks_number_aliases`
- Normalization: `AliasNormalizer` (lowercase, charset, reserved terms, anti-impersonation)
- Types: `MEMORABLE`, `LEGACY`, `SYSTEM`
- Status lifecycle: `RESERVED` → `ACTIVE` / `RETIRED`, etc.
- See [KS Number Alias Standard](../architecture/KS_NUMBER_ALIAS_STANDARD.md)

## Lifecycle

| Entity | States (Phase 4) |
| --- | --- |
| Identity | `PENDING`, `ACTIVE`, `SUSPENDED`, `CLOSED` |
| Alias | `RESERVED`, `ACTIVE`, `SUSPENDED`, `RETIRED` |

Issuance creates identities in `PENDING`. See [ADR-0015](../decisions/ADR-0015-KS-IDENTITY-LIFECYCLE.md).

## Audit events

| Category | Event types |
| --- | --- |
| `identity` | `identity.ks-number.issued`, `identity.status.changed`, `identity.alias.created`, `identity.alias.status.changed` |

Append-only via `AuditWriter`; `AuditCategory.IDENTITY` added in `platform-persistence`.

## Outbox events

| Event type | Aggregate |
| --- | --- |
| `identity.ks-number.issued` | `identity` / `identity_id` |
| `identity.status.changed` | `identity` / `identity_id` |
| `identity.alias.created` | `identity` / `identity_id` |
| `identity.alias.status.changed` | `identity` / `identity_id` |

Written via `IdentityOutboxWriter`; `causation_id` links to audit `event_id`. Publisher remains `NoOpOutboxPublisher` (Phase 3).

## Metrics

| Metric | Description |
| --- | --- |
| `securepay.identity.issued` | Successful issuance |
| `securepay.identity.issuance.replayed` | Idempotent replay |
| `securepay.identity.issuance.conflict` | Issuance idempotency conflict |
| `securepay.identity.status.changed` | Lifecycle transition |
| `securepay.identity.alias.created` | Alias created |
| `securepay.identity.alias.conflict` | Duplicate normalized alias |

## Security

- No public identity HTTP endpoints in Phase 4
- Alias reserved-term blocklist and canonical impersonation checks
- Service-layer operations require `ActorContext` (SYSTEM/TEST only)
- No secrets committed; `scripts/scan_secrets.py` in CI

See [KS Alias Security Standard](../security/KS_ALIAS_SECURITY_STANDARD.md).

## Tests

| Suite | Location | Coverage |
| --- | --- | --- |
| Unit | `shared/platform-identity/src/test` | `KsNumberFormatter`, `KsNumberParser`, `AliasNormalizer` |
| Integration | `services/securepay-core/src/integrationTest/.../identity/` | Issuance, idempotency, audit/outbox, concurrency, lifecycle, fresh DB |
| Doctrine | `testing/doctrine` | `Phase4MigrationDoctrineTest` — identity schema only, no prohibited domain tables |

## CI

Workflow: [`.github/workflows/phase-4-validation.yml`](../../.github/workflows/phase-4-validation.yml)

Jobs: foundation validation, unit tests, integration tests (Testcontainers), doctrine tests, secret scan, compose config, compose runtime health, phase-4-complete gate.

## ADRs and standards (new)

| Document | Topic |
| --- | --- |
| [ADR-0012](../decisions/ADR-0012-KS-NUMBER-IDENTITY-MODEL.md) | Identity model |
| [ADR-0013](../decisions/ADR-0013-SEQUENTIAL-KS-NUMBER-ISSUANCE.md) | Sequential issuance |
| [ADR-0014](../decisions/ADR-0014-KS-NUMBER-ALIAS-MODEL.md) | Alias model |
| [ADR-0015](../decisions/ADR-0015-KS-IDENTITY-LIFECYCLE.md) | Lifecycle |
| [KS Identity Domain Standard](../architecture/KS_IDENTITY_DOMAIN_STANDARD.md) | Schema ownership |
| [KS Number Issuance Standard](../architecture/KS_NUMBER_ISSUANCE_STANDARD.md) | Issuance transaction |
| [KS Number Alias Standard](../architecture/KS_NUMBER_ALIAS_STANDARD.md) | Normalization |
| [KS Alias Security Standard](../security/KS_ALIAS_SECURITY_STANDARD.md) | Reserved terms |
| [KS Number Operations Runbook](KS_NUMBER_OPERATIONS_RUNBOOK.md) | Operations |

## Local validation results

| Check | Result |
| --- | --- |
| `./gradlew test` | **PASS** |
| `./gradlew doctrineTest` | **PASS** |
| `bash scripts/run_all_validations.sh` | **PASS** |
| `./gradlew integrationTest` | **Skipped locally** — Docker unavailable in cloud agent environment; CI runs with `SECUREPAY_REQUIRE_TESTCONTAINERS=true` |

## Unresolved items (spec section 32)

Phase 4 defers the following to the [Unresolved Items Register](UNRESOLVED_ITEMS_REGISTER.md):

| ID | Topic |
| --- | --- |
| UR-23 | Doctrine lifecycle statuses (`DECEASED`, `DISSOLVED`, …) vs Phase 4 four-state model |
| UR-24 | `display_name` PII classification and storage controls |
| UR-25 | Public HTTP API for identity issuance and lookup |
| UR-26 | End-user and partner `ActorContext` (requires authentication phase) |
| UR-27 | Sequence gap monitoring and reconciliation tooling |
| UR-28 | Alias moderation workflow (approve/reject/dispute) |
| UR-29 | Retired alias reuse cooling period |
| UR-30 | Single `is_primary_display_alias` enforcement |
| UR-31 | Expanded reserved-term list (legal/brand) |
| UR-32 | Auto `PENDING` → `ACTIVE` on verification |
| UR-33 | Identity suspension cascade to aliases |
| UR-34 | Banking account closure hooks on `CLOSED` |
| UR-35 | Alias resolution for non-`ACTIVE` statuses |
| UR-36 | Internationalized alias policy |
| UR-37 | Alias creation rate limiting |
| UR-38 | Automated offensive-term screening |

Prior unresolved items remain open: UR-01 (KS Number as Choice account label), UR-03 (automatic bank account on issuance), UR-13 (production DB roles), UR-14 (idempotency response encryption), UR-15/16 (outbox broker/delivery).

## Exclusions (confirmed)

Phase 4 does **not** implement:

- Authentication or session management
- Payment, ledger, or settlement logic
- Choice Bank API calls or account provisioning
- Public identity or alias HTTP endpoints
- Control Centre identity administration UI
- End-user onboarding for `INDIVIDUAL` identities

## Confirmations

- **No authentication** was implemented.
- **No payments** or financial commands were implemented.
- **No Choice Bank API** integration was added.
- **No secrets** were committed.
- **Phase 4 documentation** complete; **no git commit** performed per instruction.

## Related documents

- [Phase 03 Completion Report](PHASE_03_COMPLETION_REPORT.md)
- [KS Number Doctrine](../domains/KS_NUMBER_DOCTRINE.md)
- [SecurePay Master Architecture](../architecture/SECUREPAY_MASTER_ARCHITECTURE.md)
