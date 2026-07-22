# financial-ledger

**Phase 2:** Compiling skeleton module — no ledger logic.

## Ownership

Future **authoritative financial source of truth** for SecurePay.

## Locked responsibilities (future)

- Double-entry journal enforcement
- Immutable posted entries with reversal/compensation only
- Idempotent financial commands

## Prohibited

- **No direct external-client access** — ledger commands via internal APIs only
- **No direct administrator balance editing**
- **No dependency on `securepay-core`** (ArchUnit enforced)

## Future health endpoints

Will expose standard `/health/*` when deployed as an independent service.

## Related documents

- [Financial Ledger Doctrine](../../docs/domains/FINANCIAL_LEDGER_DOCTRINE.md)
- [ADR-0002 Modular platform boundaries](../../docs/decisions/ADR-0002-MODULAR-PLATFORM-BOUNDARIES.md)
