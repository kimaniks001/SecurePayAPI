#!/usr/bin/env python3
"""Validate internal relative Markdown links resolve to existing files."""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
LINK_PATTERN = re.compile(r"\[[^\]]+\]\(([^)]+)\)")


def is_external(link: str) -> bool:
    return link.startswith(("http://", "https://", "mailto:", "#"))


def resolve_link(source: Path, link: str) -> Path | None:
    link = link.split("#", 1)[0].strip()
    if not link or is_external(link):
        return None
    target = (source.parent / link).resolve()
    return target


def main() -> int:
    errors: list[str] = []
    md_files = sorted(REPO_ROOT.rglob("*.md"))

    for md_file in md_files:
        if ".git" in md_file.parts:
            continue
        text = md_file.read_text(encoding="utf-8")
        for match in LINK_PATTERN.finditer(text):
            link = match.group(1).strip()
            if link.startswith("<") and link.endswith(">"):
                link = link[1:-1]
            target = resolve_link(md_file, link)
            if target is None:
                continue
            if not target.exists():
                rel_source = md_file.relative_to(REPO_ROOT)
                errors.append(f"{rel_source}: broken link '{link}' -> {target}")

    if errors:
        print("Markdown link validation FAILED:")
        for err in errors:
            print(f"  - {err}")
        return 1

    print(f"Markdown link validation PASSED ({len(md_files)} files checked).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
