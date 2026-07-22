# SecurePay Security Baseline

**Status:** Locked doctrine + current architectural decision  
**Phase:** 1 — foundation

## Zero trust and access control

| Control | Requirement | Classification |
| --- | --- | --- |
| Zero-trust assumptions | Verify identity, authorization, and intent on every request | **Locked doctrine** |
| Least privilege | Services, databases, and admins receive minimum required access | **Locked doctrine** |
| Object-level authorization | Possession of a token does not imply access to all resources | **Locked doctrine** |
| API scopes | Partner and user tokens carry explicit scopes | **Current architectural decision** |
| Administrator controls | Stronger authentication, separate audit trail | **Locked doctrine** |
| Break-glass access | Emergency access is logged, time-bound, and reviewed | **Current architectural decision** |

## Authentication and sessions

| Control | Requirement |
| --- | --- |
| Strong authentication | KS Number + password + OTP for standard users |
| Step-up authentication | Required for sensitive actions |
| Password hashing | Modern adaptive hash — never plaintext |
| OTP controls | Expiry, attempt limits, rate limiting |
| Session revocation | All active sessions revocable |
| Login rate limiting | By identity, application, device, and source |
| Account enumeration | Login errors do not reveal KS Number existence |

**Explicit:** no plaintext passwords, OTPs, or tokens in storage or logs.

## API security

| Control | Requirement |
| --- | --- |
| Idempotency | Required for mutating financial and agreement commands |
| Replay protection | Unique request IDs, nonce/salt patterns where applicable |
| Signature verification | Provider callbacks and partner webhooks verified before trust |
| Rate limiting | Per identity, partner, endpoint, and IP |
| Request-size limits | Enforced at gateway and application layers |

**Explicit:** no callback is trusted before verification.

**Explicit:** valid credentials do not automatically make an application trusted.

## Secrets and configuration

| Control | Requirement |
| --- | --- |
| Secrets management | Production secrets in approved secrets manager only |
| Git hygiene | no production secrets in GitHub |
| Environment variables | Core financial doctrine must not be disabled via ordinary env vars |
| Provider isolation | Choice credentials only in `choice-bank-connector` |
| Frontend banking credentials | no frontend banking credentials — ever |

## Encryption

| Control | Requirement |
| --- | --- |
| Encryption in transit | TLS 1.2+ for all external and inter-service traffic |
| Encryption at rest | Database, object storage, and backups encrypted |
| Sensitive payloads | Minimize storage; encrypt where retention required |

## Logging and audit

| Control | Requirement |
| --- | --- |
| Secure logging | Structured logs without secrets or full PAN/account data |
| Redaction | Provider payloads and PII redacted in logs |
| Audit logging | Identity, agreement, governance, review, security, and financial actions |
| Audit immutability | Audit history must not be deletable via Control Centre |

## Evidence and uploads

| Control | Requirement |
| --- | --- |
| Secure uploads | Authenticated, authorized, size-limited, type-validated |
| Malware scanning | Required before evidence processing |
| Object storage | Permanent files not on local application disks |

## Supply chain and change control

| Control | Requirement |
| --- | --- |
| Dependency scanning | CI and scheduled scans for known vulnerabilities |
| Code review | Required for production changes |
| Migration controls | Schema changes via reviewed migrations only |

## Financial and administrative boundaries

| Control | Requirement |
| --- | --- |
| Ledger integrity | no direct Control Centre ledger editing |
| Payment Ready | not permit manual Payment Ready assignment |
| Balance authority | no frontend or partner direct balance edits |
| Provider isolation | no frontend or partner direct Choice Bank access |

## Backup and incident response

| Control | Requirement |
| --- | --- |
| Backup security | Encrypted backups with access controls |
| Incident response | Documented runbooks, escalation, and post-incident review |

## Phase 1 scope

**Confirmed:** Security controls are documented and validated in CI; production enforcement is implemented in later phases.

## Related documents

- [Authentication Doctrine](../domains/AUTHENTICATION_DOCTRINE.md)
- [Financial Ledger Doctrine](../domains/FINANCIAL_LEDGER_DOCTRINE.md)
- [Control Centre Requirements](../operations/CONTROL_CENTRE_REQUIREMENTS.md)
- [ADR-0005 Control Centre no direct database access](../decisions/ADR-0005-CONTROL-CENTRE-NO-DIRECT-DATABASE-ACCESS.md)
