#!/usr/bin/env python3
"""Validate that Phase 1 required files and directories exist."""

from __future__ import annotations

import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]

REQUIRED_DIRS = [
    "docs/doctrine",
    "docs/architecture",
    "docs/domains",
    "docs/banking/contract-findings",
    "docs/banking/source",
    "docs/security",
    "docs/operations",
    "docs/handover",
    "docs/decisions",
    "contracts/openapi",
    "contracts/events",
    "contracts/webhooks",
    "contracts/errors",
    "contracts/schemas",
    "services/securepay-core",
    "services/financial-ledger",
    "services/choice-bank-connector",
    "services/evidence-service",
    "services/notification-service",
    "services/webhook-service",
    "applications/control-centre",
    "database/migrations",
    "database/seeds",
    "database/test-data",
    "testing/unit",
    "testing/integration",
    "testing/contract",
    "testing/doctrine",
    "testing/security",
    "testing/performance",
    "testing/end-to-end",
    "infrastructure/local",
    "infrastructure/staging",
    "infrastructure/production",
    "infrastructure/monitoring",
    "infrastructure/runbooks",
    "scripts",
    ".github/workflows",
]

REQUIRED_FILES = [
    "README.md",
    "AGENTS.md",
    ".gitignore",
    ".editorconfig",
    ".env.example",
    "docker-compose.yml",
    ".github/workflows/phase-1-validation.yml",
    "docs/architecture/SECUREPAY_MASTER_ARCHITECTURE.md",
    "docs/domains/KS_NUMBER_DOCTRINE.md",
    "docs/domains/AUTHENTICATION_DOCTRINE.md",
    "docs/domains/SECURELINK_STATE_MACHINE.md",
    "docs/domains/PAYMENT_READY_DOCTRINE.md",
    "docs/domains/FINANCIAL_LEDGER_DOCTRINE.md",
    "docs/banking/CHOICE_BANK_INTEGRATION_BOUNDARY.md",
    "docs/banking/CHOICE_BANK_SOURCE_REGISTER.md",
    "docs/banking/contract-findings/CHOICE_CONTRACT_CONFIRMED_REQUIREMENTS.md",
    "docs/banking/contract-findings/CHOICE_CONTRACT_TECHNICAL_OBLIGATIONS.md",
    "docs/banking/contract-findings/CHOICE_CONTRACT_COMMERCIAL_RULES_REGISTER.md",
    "docs/banking/contract-findings/CHOICE_CONTRACT_OPEN_QUESTIONS.md",
    "docs/security/SECUREPAY_SECURITY_BASELINE.md",
    "docs/operations/CONTROL_CENTRE_REQUIREMENTS.md",
    "docs/operations/PHASE_01_COMPLETION_REPORT.md",
    "docs/handover/APPLICATION_INFRASTRUCTURE_CONTRACT.md",
    "docs/decisions/ADR-0001-API-FIRST-DOMAIN-FIRST.md",
    "docs/decisions/ADR-0002-MODULAR-PLATFORM-BOUNDARIES.md",
    "docs/decisions/ADR-0003-POSTGRESQL-SYSTEM-OF-RECORD.md",
    "docs/decisions/ADR-0004-CHOICE-BANK-ADAPTER-BOUNDARY.md",
    "docs/decisions/ADR-0005-CONTROL-CENTRE-NO-DIRECT-DATABASE-ACCESS.md",
    "contracts/openapi/securepay-api-v1.yaml",
    "contracts/events/event-envelope-v1.schema.json",
    "contracts/errors/error-envelope-v1.schema.json",
    "scripts/validate_required_files.py",
    "scripts/validate_markdown_links.py",
    "scripts/validate_doctrine.py",
    "scripts/scan_secrets.py",
    "scripts/run_all_validations.sh",
]


def main() -> int:
    errors: list[str] = []

    for rel in REQUIRED_DIRS:
        path = REPO_ROOT / rel
        if not path.is_dir():
            errors.append(f"Missing directory: {rel}")

    for rel in REQUIRED_FILES:
        path = REPO_ROOT / rel
        if not path.is_file():
            errors.append(f"Missing file: {rel}")

    if errors:
        print("Required file validation FAILED:")
        for err in errors:
            print(f"  - {err}")
        return 1

    print(f"Required file validation PASSED ({len(REQUIRED_DIRS)} dirs, {len(REQUIRED_FILES)} files).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
