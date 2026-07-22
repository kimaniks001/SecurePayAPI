# SecurePay API

**Money should follow the agreement.**

SecurePay is an API-first, domain-first agreement and financial platform built by Keyman Oak. It enables versioned agreements (SecureLinks), deterministic Payment Ready evaluation, authoritative ledger accounting, and regulated settlement — accessed by SecurePay clients, Keyman business solutions, institutional partners, and approved external developers.

## Why API-first?

The public API is the stable contract. Web apps, mobile apps, partner platforms, and the Control Centre are **replaceable clients**. They inform usability but do not own:

- domain models or state machines
- financial architecture or ledger truth
- Payment Ready or release authority
- Choice Bank integration

This keeps money movement auditable, testable, and consistent regardless of which client initiates an action.

## Planned deployable components

| Service | Role |
| --- | --- |
| `securepay-core` | Identity, agreements, governance, Payment Ready, admin APIs |
| `financial-ledger` | Double-entry ledger (authoritative money record) |
| `choice-bank-connector` | Sole Choice Bank BaaS integration boundary |
| `evidence-service` | Evidence storage and processing |
| `notification-service` | OTP and notifications |
| `webhook-service` | Partner webhook delivery |
| `securepay-control-centre` | Operational administration UI (API client only) |

## Repository structure

```
securepayAPI/
├── docs/           # Doctrine, architecture, ADRs, banking findings
├── contracts/      # OpenAPI, event/error envelopes, schemas
├── services/       # Deployable service scaffolds
├── applications/   # Control Centre application scaffold
├── database/       # Migrations, seeds, test data
├── testing/        # Unit, integration, contract, doctrine tests
├── infrastructure/ # Environment configs and runbooks
└── scripts/        # Validation and tooling
```

See [AGENTS.md](AGENTS.md) for engineering agent rules and [Master Architecture](docs/architecture/SECUREPAY_MASTER_ARCHITECTURE.md) for architectural boundaries.

## Documentation-first workflow

1. Record doctrine and ADRs before implementing domain logic.
2. Update OpenAPI and event contracts when APIs change.
3. Add doctrine tests when enforcing financial rules.
4. Update the unresolved-items register when ambiguity exists — never guess.

## Local development dependencies

Phase 1 provides PostgreSQL and Redis-compatible cache for local engineering only.

```bash
# Copy environment placeholders (never commit real .env)
cp .env.example .env

# Start PostgreSQL and Redis
docker compose --env-file .env.example up -d

# Check health
docker compose --env-file .env.example ps

# Stop and remove containers (data volumes persist)
docker compose --env-file .env.example down
```

**Warning:** `.env.example` values are for local development only. Do not use in staging or production. Choice credentials must never appear in committed files.

## Run validations locally

```bash
python3 -m pip install -r scripts/requirements-validation.txt
bash scripts/run_all_validations.sh
```

Individual checks:

```bash
python3 scripts/validate_required_files.py
python3 scripts/validate_markdown_links.py
python3 scripts/validate_doctrine.py
python3 scripts/scan_secrets.py
docker compose --env-file .env.example config --quiet
```

## GitHub Actions

Workflow [`.github/workflows/phase-1-validation.yml`](.github/workflows/phase-1-validation.yml) runs on pull requests and pushes to `main`:

- Required file and directory validation
- Markdown internal link validation
- Doctrine consistency checks
- Accidental secret scanning
- OpenAPI 3.1 validation
- JSON Schema syntax validation
- Docker Compose configuration validation

## Branch and PR expectations

- Feature work uses descriptive branches (e.g. `phase-01-foundation`).
- Every phase ends with a completion report and updated unresolved register.
- Pull requests require passing CI — **no automatic merge**.
- Doctrine changes require explicit review.

## Phased build strategy

| Phase | Scope |
| --- | --- |
| **Phase 1** (current) | Repository foundation, doctrine, contracts, validation — **no production business services** |
| Phase 2+ | Service implementation per ADRs and domain doctrine |

**Phase 1 exclusions:** No KS Number issuance, authentication, OTP delivery, SecureLink business logic, payment processing, ledger postings, Choice Bank API calls, production infrastructure, or Control Centre UI.

Choice Bank implementation is deferred to a later phase behind `choice-bank-connector`.

## Secrets policy

- Never commit production secrets, Choice private keys, or real `.env` files.
- Production secrets belong in an approved secrets manager.
- Report suspected secret exposure immediately.

## License

Proprietary — Keyman Oak Limited.
