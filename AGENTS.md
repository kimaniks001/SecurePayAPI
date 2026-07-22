# SecurePay — Engineering Agent Instructions

This file applies to **every engineering agent** working in the SecurePay API repository.

## Central doctrine

> **Money should follow the agreement.**

SecurePay is an **API-first, domain-first** agreement and financial platform. Frontends (web, mobile, Control Centre) and partner applications are **replaceable clients**. They must not dictate:

- the domain model
- financial architecture or ledger structure
- security controls or service boundaries
- public API contracts or state transitions
- Payment Ready evaluation or release authority
- Choice Bank integration

## Non-negotiable financial and identity rules

| Rule | Requirement |
| --- | --- |
| Payment Ready | Must be **calculated by backend domain logic**. May **never** be directly assigned by a user, frontend, partner application, or administrator. |
| Financial ledger | **Authoritative financial source of truth.** Every posted journal must balance. Posted entries are **immutable** — corrections use reversals or compensating entries. |
| Idempotency | Every financial command must be idempotent. |
| Balances | No frontend or partner may directly edit balances. |
| Choice Bank | No frontend or partner may directly access Choice Bank. Integration is **only** through `choice-bank-connector`. |
| Fund release | No frontend or partner may directly release funds outside approved backend rules. |
| KS Numbers | Only the central KS Number domain may allocate numbers. Applications may not calculate, reserve, or issue KS Numbers. |
| Audit | Important identity, agreement, governance, review, security, and financial actions must be audited. |
| Files | Permanent files must not be stored on local application disks. Use object storage. |
| Doctrine env vars | Core financial doctrine must **not** be switchable through ordinary environment variables. |
| Migrations | Database schema changes must use migrations in `database/migrations`. |
| Doctrine tests | Doctrine-enforcing implementation must include doctrine tests in `testing/doctrine`. |
| Data access | Services must use least-privilege data access. |
| Scaling | APIs must support horizontal scaling. |
| Provider outages | Must not corrupt SecurePay state. |
| Choice placeholders | Unconfirmed Choice Bank capabilities remain adapter placeholders — do not implement as production behavior. |
| Doctrine changes | No agent may silently change locked doctrine. Escalate conflicts. |
| Failing doctrine tests | **No code may bypass a failing doctrine test.** |
| Control Centre | No administrator interface may directly edit production financial records. |

## Source authority hierarchy

When requirements conflict:

1. Applicable Kenyan law and regulation
2. Executed Choice–Keyman agreements
3. Official Choice Bank BaaS documentation (https://choice-bank.gitbook.io/choice-bank)
4. SecurePay Terms and Conditions
5. SecurePay Operating Doctrine (`docs/doctrine/`)
6. Locked commercial and product rules
7. Approved ADRs (`docs/decisions/`)
8. Technical specifications
9. Source code

**Do not guess.** Document ambiguity in `docs/operations/UNRESOLVED_ITEMS_REGISTER.md`.

## Classification labels

Every document and significant code comment must distinguish:

- **Locked doctrine**
- **Current architectural decision**
- **Confirmed contractual fact**
- **Confirmed technical-documentation fact**
- **Engineering assumption**
- **Pending external confirmation**
- **Future implementation requirement**

Do not present assumptions as confirmed facts.

## Repository map

| Path | Purpose |
| --- | --- |
| `docs/doctrine/` | Operating doctrine |
| `docs/architecture/` | Master architecture |
| `docs/domains/` | Domain doctrine (KS Number, auth, SecureLink, Payment Ready, ledger) |
| `docs/banking/` | Choice integration boundary and contract findings |
| `docs/security/` | Security baseline |
| `docs/operations/` | Control Centre requirements, completion reports, unresolved register |
| `docs/decisions/` | ADRs |
| `contracts/` | OpenAPI, events, errors, schemas |
| `services/` | Deployable services (scaffold in Phase 1) |
| `database/migrations/` | Schema migrations |
| `testing/` | Unit, integration, contract, doctrine, security tests |
| `scripts/` | Validation and tooling |

## Definition of Done — Every Phase

A phase is complete only when **all** apply:

1. **Implementation or documentation** appropriate to the phase scope
2. **Automated tests** where code exists
3. **Updated API and event contracts** when interfaces change
4. **Migrations** where data structures change
5. **Validation commands** pass (see `scripts/run_all_validations.sh`)
6. **Security review** against `docs/security/SECUREPAY_SECURITY_BASELINE.md`
7. **Completion report** in `docs/operations/`
8. **Unresolved-items register** updated
9. **Pull request** opened — **no automatic merge**

## Secrets and security

- **Production secrets must never be committed.**
- Choice credentials must never appear in committed files.
- Use `.env.example` for local placeholders only.
- Run `scripts/scan_secrets.py` before every commit.

## Local development

```bash
cp .env.example .env
docker compose --env-file .env.example up -d
docker compose --env-file .env.example down
```

## Validation

```bash
pip install -r scripts/requirements-validation.txt
bash scripts/run_all_validations.sh
```

GitHub Actions workflow `.github/workflows/phase-1-validation.yml` runs the same checks on pull requests and pushes to `main`.

## Phased build strategy

- **Phase 1 (current):** Repository, doctrine, architecture, contracts, validation — **no production business services**
- **Phase 2+:** Implement services per ADRs and domain doctrine, with doctrine tests

Do not implement out-of-phase features unless explicitly instructed.

## Key doctrine documents

- [Operating Doctrine](docs/doctrine/SECUREPAY_OPERATING_DOCTRINE.md)
- [Master Architecture](docs/architecture/SECUREPAY_MASTER_ARCHITECTURE.md)
- [KS Number Doctrine](docs/domains/KS_NUMBER_DOCTRINE.md)
- [Authentication Doctrine](docs/domains/AUTHENTICATION_DOCTRINE.md)
- [SecureLink State Machine](docs/domains/SECURELINK_STATE_MACHINE.md)
- [Payment Ready Doctrine](docs/domains/PAYMENT_READY_DOCTRINE.md)
- [Financial Ledger Doctrine](docs/domains/FINANCIAL_LEDGER_DOCTRINE.md)
- [Choice Bank Integration Boundary](docs/banking/CHOICE_BANK_INTEGRATION_BOUNDARY.md)
- [Security Baseline](docs/security/SECUREPAY_SECURITY_BASELINE.md)
- [Unresolved Items Register](docs/operations/UNRESOLVED_ITEMS_REGISTER.md)
