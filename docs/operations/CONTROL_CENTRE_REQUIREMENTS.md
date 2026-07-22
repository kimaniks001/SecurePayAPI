# SecurePay Control Centre Requirements

**Status:** Future implementation requirement (requirements locked; UI not built in Phase 1)  
**Phase:** 1 — requirements documentation only

## Purpose

The SecurePay Control Centre is the operational administration interface for platform and partner management. It is a **replaceable client** of secured administration APIs.

## Architectural constraints

| Classification | Requirement |
| --- | --- |
| **Locked doctrine** | Must use secured administration APIs — never directly access production tables. |
| **Locked doctrine** | Must use role-based permissions. |
| **Locked doctrine** | Must use step-up authentication for sensitive actions. |
| **Locked doctrine** | Must record who changed what, why, when, and previous/new values. |
| **Locked doctrine** | Must distinguish operational settings from protected doctrine. |
| **Locked doctrine** | Must not permit direct ledger changes. |
| **Locked doctrine** | Must not permit manual Payment Ready assignment. |
| **Locked doctrine** | Must not permit release without conditions. |
| **Locked doctrine** | Must not permit audit-history deletion. |

See [ADR-0005](../decisions/ADR-0005-CONTROL-CENTRE-NO-DIRECT-DATABASE-ACCESS.md).

## Platform operations (monitoring)

The Control Centre must eventually surface:

| Metric / signal | Purpose |
| --- | --- |
| Requests per second | Capacity and traffic monitoring |
| Success and error rates | Reliability |
| P50, P95, P99 latency | Performance |
| Bottleneck detection | Incident response |
| Service dependencies | Blast-radius analysis |
| Database health | Data layer status |
| Cache health | Redis-compatible cache status |
| Queue health | Async pipeline status |
| Choice Bank health | Provider connectivity |
| Payment-rail health | M-PESA, PesaLink, RTGS/EFT, etc. |
| Settlement failures | Financial operations |
| Reconciliation mismatches | Ledger vs provider drift |
| Webhook failures | Partner delivery issues |
| Evidence-processing backlog | Evidence pipeline capacity |

## Partner management

| Capability | Description |
| --- | --- |
| Partner organizations | Register and manage partners |
| Applications | Partner app registrations |
| Environments | Sandbox vs production separation |
| Credentials | Issuance and rotation (via APIs) |
| Scopes | API scope assignment |
| Quotas | Usage quotas |
| Rate limits | Per-partner throttling configuration |
| API usage | Consumption metrics |
| Endpoint consumption | Per-route analytics |
| Latency | Partner-specific performance |
| Errors | Partner error patterns |
| Webhook health | Delivery success/failure |
| API version | Version adoption tracking |
| Security warnings | Anomaly and policy alerts |
| Compliance warnings | Regulatory and policy flags |

## Operational controls

Authorized operators must be able to (via audited APIs):

| Control | Effect |
| --- | --- |
| Warn | Notify partner of issue |
| Throttle | Reduce throughput |
| Adjust approved rate limits | Within policy bounds |
| Restrict scope | Remove API capabilities |
| Read-only mode | Block mutations |
| Block payment creation | Stop new payment intents |
| Suspend production access | Emergency partner suspension |
| Revoke credentials | Invalidate client credentials |
| Rotate webhook secret | Force webhook re-verification |
| Require compliance review | Hold partner actions pending review |
| Terminate integration | End partner relationship |

**Locked doctrine:** None of the above may bypass Payment Ready rules, ledger immutability, or direct fund release outside backend authorization.

## Phase 1 scope

**Confirmed:** No Control Centre UI or administration APIs are implemented in Phase 1. Requirements are documented for Phase 2+.

## Related documents

- [Master Architecture](../architecture/SECUREPAY_MASTER_ARCHITECTURE.md)
- [Security Baseline](../security/SECUREPAY_SECURITY_BASELINE.md)
- [Application–Infrastructure Contract](../handover/APPLICATION_INFRASTRUCTURE_CONTRACT.md)
