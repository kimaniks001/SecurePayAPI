# Doctrine Tests

**Phase:** 2 foundation

Doctrine tests protect locked SecurePay rules at the architecture and repository level before business logic is implemented.

## Location

- **Gradle module:** `testing/doctrine`
- **Run:** `./gradlew doctrineTest`

## What is enforced (Phase 2)

| Rule | Mechanism |
| --- | --- |
| `financial-ledger` does not depend on `securepay-core` | ArchUnit |
| Shared modules do not depend on services | ArchUnit |
| No Choice Bank HTTP client in connector skeleton | ArchUnit |
| No Payment Ready override classes | ArchUnit |
| Only `HealthController` exposes REST endpoints in core | ArchUnit |
| Choice connector remains skeleton-only | ArchUnit |
| Phase 1 secret scan and doctrine scripts | `scripts/validate_doctrine.py`, `scripts/scan_secrets.py` |

## Future phases

Extend this suite to verify:

- No manual Payment Ready assignment APIs
- No ledger posting outside `financial-ledger`
- No direct Choice credentials outside `choice-bank-connector`
- No business endpoints beyond approved OpenAPI operations

Integration tests are in `services/securepay-core/src/integrationTest/java` and run via `./gradlew integrationTest`. When `SECUREPAY_REQUIRE_TESTCONTAINERS=true` (CI), Docker must be available and PostgreSQL/Redis Testcontainer tests must run.

## Policy

**No code may bypass a failing doctrine test.** Fix the violation or escalate doctrine change through ADR and explicit review — never disable tests to merge.

## Related documents

- [AGENTS.md](../../AGENTS.md)
- [Operating Doctrine](../../docs/doctrine/SECUREPAY_OPERATING_DOCTRINE.md)
