# choice-bank-connector

**Phase 2:** Compiling skeleton module — no Choice API calls.

## Ownership

Future **sole Choice Bank BaaS integration boundary**.

## Locked responsibilities (future)

- Request signing and response verification
- Idempotent callback handling
- Provider-neutral command interface toward SecurePay Core

## Prohibited

- **No credentials committed** to version control
- **No actual Choice HTTP calls** in Phase 2
- **No credential exposure** to other modules or frontends

## Future health endpoints

Will expose standard `/health/*` including Choice connectivity checks when implemented.

## Related documents

- [Choice Bank Integration Boundary](../../docs/banking/CHOICE_BANK_INTEGRATION_BOUNDARY.md)
- [ADR-0004 Choice Bank adapter boundary](../../docs/decisions/ADR-0004-CHOICE-BANK-ADAPTER-BOUNDARY.md)
