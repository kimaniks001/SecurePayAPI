# evidence-service

**Phase 2:** Compiling skeleton module — no upload logic.

## Ownership

Future **evidence metadata and object-storage boundary**.

## Locked responsibilities (future)

- Evidence metadata in PostgreSQL
- Binary objects in approved object storage
- Malware scanning before processing

## Prohibited

- **No binary evidence storage in PostgreSQL**
- **No upload endpoints** in Phase 2
- **No permanent local disk storage**

## Future health endpoints

Will expose `/health/*` with object-storage dependency checks.

## Related documents

- [Security Baseline](../../docs/security/SECUREPAY_SECURITY_BASELINE.md)
