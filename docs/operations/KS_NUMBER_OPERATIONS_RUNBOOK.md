# KS Number Operations Runbook

**Status:** Current architectural decision (Phase 4)  
**Phase:** 4 — KS Number identity issuance  
**Branch:** `phase-04-ksnumber-identity-issuance`

## Purpose

Operational guidance for monitoring, investigating, and supporting the KS identity domain in non-production and production environments.

## Scope

| In scope (Phase 4) | Out of scope |
| --- | --- |
| Identity issuance metrics and idempotency replay | Public identity HTTP APIs |
| Audit/outbox investigation | Choice Bank account provisioning |
| Sequence and alias integrity checks | Control Centre identity UI |
| Integration test validation patterns | Payment or ledger operations |

## Key artefacts

| Artefact | Location |
| --- | --- |
| Migration | `database/migrations/V20260723130000__ks_identity_foundation.sql` |
| Module | `shared/platform-identity` |
| Integration tests | `services/securepay-core/src/integrationTest/.../identity/` |
| CI workflow | `.github/workflows/phase-4-validation.yml` |

## Health checks

Phase 4 does not add public identity endpoints. Use existing health endpoints:

```bash
curl --fail http://localhost:8080/health/live
curl --fail http://localhost:8080/health/ready
curl --fail http://localhost:8080/health/dependencies
```

Identity functionality is validated via service-layer integration tests, not HTTP probes.

## Metrics to monitor

| Metric | Meaning | Suggested action |
| --- | --- | --- |
| `securepay.identity.issued` | New canonical numbers issued | Baseline traffic; alert on unexpected zero in prod |
| `securepay.identity.issuance.replayed` | Idempotent replays | Elevated rate may indicate client retry storm |
| `securepay.identity.issuance.conflict` | Idempotency key/body conflicts | Investigate client bug or abuse |
| `securepay.identity.status.changed` | Lifecycle transitions | Review audit for unauthorized changes |
| `securepay.identity.alias.created` | New aliases | Monitor for squatting when APIs exist |
| `securepay.identity.alias.conflict` | Duplicate alias attempts | Expected on races; spike may indicate abuse |

Also monitor Phase 3 signals: outbox `PENDING` backlog, idempotency conflicts globally, audit append failures.

## Database queries (read-only investigation)

### Current platform phase

```sql
SELECT metadata_value
FROM platform.platform_metadata
WHERE metadata_key = 'platform_phase';
-- Expected: phase-04-ksnumber-identity-issuance
```

### Sequence position

```sql
SELECT last_value, is_called
FROM identity.ks_number_sequence;
```

### Recent identities

```sql
SELECT id, canonical_ks_number, sequence_number, identity_type, status, created_at
FROM identity.ks_identities
ORDER BY sequence_number DESC
LIMIT 20;
```

### Issuance idempotency record

```sql
SELECT id, idempotency_key, status, created_at, completed_at
FROM idempotency.idempotency_records
WHERE operation_code = 'identity.ks-number.issue'
ORDER BY created_at DESC
LIMIT 20;
```

### Audit trail for an identity

```sql
SELECT event_id, event_type, action, occurred_at, new_state
FROM audit.audit_events
WHERE resource_type = 'identity'
  AND resource_id = '<identity-uuid>'
ORDER BY occurred_at;
```

### Pending identity outbox events

```sql
SELECT event_id, event_type, status, created_at, available_at
FROM events.outbox_events
WHERE aggregate_type = 'identity'
  AND status IN ('PENDING', 'PROCESSING', 'FAILED')
ORDER BY created_at;
```

## Common scenarios

### Client reports duplicate issuance on retry

1. Confirm client sends stable `issuance_request_key`.
2. Query `identity.ks_identities` for the issuance key — **this is the permanent ownership record**.
3. If a row exists with matching `issuance_request_hash`, replay is correct — same `identity_id` and `canonical_ks_number` returned without a new sequence value or duplicate issuance audit/outbox events.
4. If hash mismatch, `IssuanceOwnershipConflictException` — client changed payload under same key.
5. Idempotency record (`identity.ks-number.issue`) is replay-storage only. Expired or deleted idempotency data **cannot** authorize a second identity when step 2 finds an existing row.

### Idempotency replay record expired or deleted

**Expected safe behaviour.** `DefaultKsIdentityIssuanceService` checks `identity.ks_identities.issuance_request_key` before `nextval`. Replay succeeds from the identity row even when idempotency data is unavailable. Legal retention of idempotency replay records remains unresolved (UR-12).

### Unexpected gap in sequence numbers

**Expected behaviour.** Gaps occur when a transaction rolls back after `nextval`. Verify no duplicate `canonical_ks_number` or `sequence_number` rows exist. Do not attempt to "fill" gaps.

### Alias creation fails with duplicate key

1. Query `identity.ks_number_aliases` for `normalized_alias`.
2. If legitimate dispute, escalate via unresolved moderation process (UR-28) — no operator override API in Phase 4.

### Identity stuck in PENDING

Phase 4 does not auto-activate. Transition to `ACTIVE` requires `KsIdentityLifecycleService.transition` with trusted actor — typically a future onboarding or admin API.

### Optimistic lock failure on lifecycle update

Concurrent status updates on the same identity. Retry with fresh read of `version` column.

## Validation commands

```bash
./gradlew clean test --no-daemon
./gradlew doctrineTest --no-daemon
bash scripts/run_all_validations.sh
python3 scripts/scan_secrets.py
```

With Docker (CI / local full validation):

```bash
SECUREPAY_REQUIRE_TESTCONTAINERS=true ./gradlew integrationTest --no-daemon
```

## CI pipeline

[Phase 4 validation](../../.github/workflows/phase-4-validation.yml) runs:

1. Foundation validation (`run_all_validations.sh`)
2. Unit tests
3. Integration tests (Testcontainers required)
4. Doctrine tests (includes `Phase4MigrationDoctrineTest`)
5. Secret scan
6. Compose config + runtime health

## Incident response notes

| Severity | Example | Response |
| --- | --- | --- |
| High | Duplicate `canonical_ks_number` rows | Stop issuance; schema integrity breach — escalate engineering |
| Medium | Growing identity outbox `PENDING` backlog | Phase 4 uses `NoOpOutboxPublisher` — expected until broker worker exists |
| Low | Elevated idempotency replays | Client retry tuning |

## Phase 4 limitations

- No operator UI for identity management
- No automated alias moderation queue
- No runbook steps for Choice Bank account linkage on issuance
- Production PostgreSQL role separation documented but not provisioned in local Docker

## Related documents

- [Phase 04 Completion Report](PHASE_04_COMPLETION_REPORT.md)
- [KS Identity Domain Standard](../architecture/KS_IDENTITY_DOMAIN_STANDARD.md)
- [KS Number Issuance Standard](../architecture/KS_NUMBER_ISSUANCE_STANDARD.md)
- [Unresolved Items Register](UNRESOLVED_ITEMS_REGISTER.md)
