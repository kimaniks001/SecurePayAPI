#!/usr/bin/env python3
"""Scan repository for accidental secret patterns."""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]

SKIP_DIRS = {".git", ".venv", "venv", "node_modules", "__pycache__"}
SKIP_FILES = {"scan_secrets.py", ".env.example"}

PATTERNS: list[tuple[str, re.Pattern[str]]] = [
    ("AWS access key", re.compile(r"AKIA[0-9A-Z]{16}")),
    ("Private key block", re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----")),
    ("Generic API key assignment", re.compile(r"(?i)(api[_-]?key|secret|password|token)\s*=\s*['\"][^'\"]{12,}['\"]")),
    ("Choice private key field", re.compile(r"(?i)senderKey\s*[:=]\s*['\"][^'\"]+['\"]")),
    ("JWT-like token", re.compile(r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}")),
]

ALLOWLIST_SUBSTRINGS = [
    "local_dev_only_change_me",
    "YOUR_PRIVATE_KEY",
    "YOUR_SENDER",
    "yourkey",
    "yourKey",
    "change_me",
    "placeholder",
    "example.com",
]


def should_skip(path: Path) -> bool:
    if path.name in SKIP_FILES:
        return True
    if any(part in SKIP_DIRS for part in path.parts):
        return True
    if path.suffix in {".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".pdf"}:
        return True
    return False


def is_allowlisted(line: str) -> bool:
    lowered = line.lower()
    return any(token.lower() in lowered for token in ALLOWLIST_SUBSTRINGS)


def main() -> int:
    findings: list[str] = []

    for path in REPO_ROOT.rglob("*"):
        if not path.is_file() or should_skip(path):
            continue
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        for lineno, line in enumerate(text.splitlines(), start=1):
            if is_allowlisted(line):
                continue
            for label, pattern in PATTERNS:
                if pattern.search(line):
                    rel = path.relative_to(REPO_ROOT)
                    findings.append(f"{rel}:{lineno}: possible {label}")

    if findings:
        print("Secret scan FAILED:")
        for item in findings:
            print(f"  - {item}")
        return 1

    print("Secret scan PASSED (no suspicious patterns detected).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
