# SecurePay Operating Doctrine

**Status:** Locked doctrine  
**Phase:** 1 — Foundation

## Central principle

> **Money should follow the agreement.**

SecurePay exists so that funds move only when recorded agreement terms, governance, evidence, reviews, and authoritative financial position permit.

## Platform posture

| Classification | Statement |
| --- | --- |
| **Locked doctrine** | SecurePay is API-first and domain-first. |
| **Locked doctrine** | Frontends and partner applications are replaceable clients. |
| **Locked doctrine** | Public API contracts remain stable while clients evolve. |
| **Current architectural decision** | Modular deployable services with strong isolation for ledger, banking, evidence, notifications, and webhooks. |
| **Future implementation requirement** | Horizontally scalable services capable of thousands of requests per second when demand requires. |

## Source authority hierarchy

When requirements conflict, resolve in this order:

1. Applicable Kenyan law and regulation
2. Executed agreements between Choice Microfinance Bank Limited and Keyman Oak Limited
3. Official Choice Bank BaaS technical documentation
4. SecurePay Terms and Conditions
5. SecurePay Operating Doctrine (this document)
6. Locked SecurePay commercial and product rules
7. Approved architectural decision records
8. Technical specifications
9. Source code

**Engineering rule:** Do not resolve conflicts by guessing. Document ambiguity in the [Unresolved Items Register](../operations/UNRESOLVED_ITEMS_REGISTER.md).

## Related doctrine

- [Master Architecture](../architecture/SECUREPAY_MASTER_ARCHITECTURE.md)
- [KS Number Doctrine](../domains/KS_NUMBER_DOCTRINE.md)
- [Authentication Doctrine](../domains/AUTHENTICATION_DOCTRINE.md)
- [SecureLink State Machine](../domains/SECURELINK_STATE_MACHINE.md)
- [Payment Ready Doctrine](../domains/PAYMENT_READY_DOCTRINE.md)
- [Financial Ledger Doctrine](../domains/FINANCIAL_LEDGER_DOCTRINE.md)
