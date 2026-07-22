#!/usr/bin/env python3
"""Validate doctrine documents contain required locked principles."""

from __future__ import annotations

import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]

DOCTRINE_CHECKS: list[tuple[str, list[str]]] = [
    (
        "docs/domains/KS_NUMBER_DOCTRINE.md",
        [
            "KS001",
            "canonical",
            "alias",
            "UUID",
            "never reused",
        ],
    ),
    (
        "docs/domains/AUTHENTICATION_DOCTRINE.md",
        [
            "KS Number",
            "OTP",
            "password",
            "rate-limited",
            "never stored in plaintext",
        ],
    ),
    (
        "docs/domains/SECURELINK_STATE_MACHINE.md",
        [
            "PAYMENT_READY",
            "DRAFT",
            "version",
            "state transitions",
        ],
    ),
    (
        "docs/domains/PAYMENT_READY_DOCTRINE.md",
        [
            "NOT_READY",
            "READY",
            "No manual Payment Ready override",
            "deterministic",
        ],
    ),
    (
        "docs/domains/FINANCIAL_LEDGER_DOCTRINE.md",
        [
            "double-entry",
            "immutable",
            "idempotency",
            "reversal",
        ],
    ),
    (
        "AGENTS.md",
        [
            "Money should follow the agreement",
            "Payment Ready",
            "financial ledger",
            "Definition of Done",
        ],
    ),
    (
        "docs/banking/CHOICE_BANK_INTEGRATION_BOUNDARY.md",
        [
            "choice-bank-connector",
            "Frontend clients never call Choice directly",
            "idempotent",
        ],
    ),
    (
        "docs/security/SECUREPAY_SECURITY_BASELINE.md",
        [
            "no plaintext passwords",
            "no production secrets",
            "least privilege",
        ],
    ),
    (
        "docs/operations/CONTROL_CENTRE_REQUIREMENTS.md",
        [
            "never directly access production tables",
            "not permit manual Payment Ready assignment",
        ],
    ),
]


def main() -> int:
    errors: list[str] = []

    for rel_path, phrases in DOCTRINE_CHECKS:
        path = REPO_ROOT / rel_path
        if not path.is_file():
            errors.append(f"Missing doctrine file: {rel_path}")
            continue
        content = path.read_text(encoding="utf-8").lower()
        for phrase in phrases:
            if phrase.lower() not in content:
                errors.append(f"{rel_path}: missing required phrase '{phrase}'")

    unresolved_register = REPO_ROOT / "docs" / "operations" / "UNRESOLVED_ITEMS_REGISTER.md"
    if not unresolved_register.is_file():
        errors.append("Missing unresolved-items register: docs/operations/UNRESOLVED_ITEMS_REGISTER.md")
    else:
        text = unresolved_register.read_text(encoding="utf-8")
        if "UNRESOLVED" not in text.upper():
            errors.append("UNRESOLVED_ITEMS_REGISTER.md must document unresolved matters")

    if errors:
        print("Doctrine validation FAILED:")
        for err in errors:
            print(f"  - {err}")
        return 1

    print(f"Doctrine validation PASSED ({len(DOCTRINE_CHECKS)} documents checked).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
