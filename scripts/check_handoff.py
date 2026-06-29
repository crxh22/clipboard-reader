#!/usr/bin/env python3
"""Check a MOB-NN handoff doc has every required section, filled.

A handoff (docs/handoff/MOB-<NN>.md) is the state the successor session inherits.
This is the mechanical floor: it fails (exit 1) if any required `##` section is
MISSING or EMPTY, and names the offenders. It enforces the SLOTS, not the QUALITY --
a present section can still be shallow, so re-read the whole session before writing.
It is the executable form of docs/runbooks/handoff-template.md.

Matching: a required section is satisfied by any header whose text contains its
key-phrase (case-insensitive); its body runs to the next header of the same or higher
level. HTML comments (<!-- ... -->, the template guidance) are stripped first, so a
section left as the bare placeholder counts as EMPTY.

Usage:
  python3 scripts/check_handoff.py docs/handoff/MOB-12.md
  # exit 0 = complete | 1 = incomplete (sections on stderr) | 2 = usage/IO error
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

#: (key-phrase matched case-insensitively in a header, human display name).
#: Order = template order. Keep in sync with docs/runbooks/handoff-template.md.
REQUIRED: list[tuple[str, str]] = [
    ("state now", "State now"),
    ("done this session", "Done this session"),
    ("in progress", "In progress"),
    ("open decisions", "Open decisions"),
    ("gotchas", "Gotchas / constraints"),
    ("next action", "Next action"),
    ("verification status", "Verification status"),
]

_HEADER = re.compile(r"^(#{1,6})\s+(.*?)\s*#*\s*$")
_COMMENT = re.compile(r"<!--.*?-->", re.DOTALL)


def _sections(text: str) -> list[tuple[str, list[str]]]:
    """(title, body_lines) per ATX header; body runs to the next header of the
    same-or-higher level (so a `##` section owns its `###` subsections)."""
    lines = text.splitlines()
    headers = [
        (i, len(m.group(1)), m.group(2).strip())
        for i, line in enumerate(lines)
        if (m := _HEADER.match(line))
    ]
    out: list[tuple[str, list[str]]] = []
    for n, (i, level, title) in enumerate(headers):
        end = len(lines)
        for j in range(n + 1, len(headers)):
            if headers[j][1] <= level:
                end = headers[j][0]
                break
        out.append((title, lines[i + 1 : end]))
    return out


def find_problems(text: str) -> list[str]:
    """One message per required section that is missing or empty; [] = complete.
    A section is non-empty iff its body has a line that is neither blank nor a header
    (comments are already stripped, so a bare placeholder reads as empty)."""
    sections = _sections(_COMMENT.sub("", text))
    problems: list[str] = []
    for key, name in REQUIRED:
        body = next((b for title, b in sections if key in title.lower()), None)
        if body is None:
            problems.append(f"{name}: MISSING (no header contains '{key}')")
        elif not any(line.strip() and not _HEADER.match(line) for line in body):
            problems.append(f"{name}: EMPTY (header present but no content)")
    return problems


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: python3 scripts/check_handoff.py <handoff.md>", file=sys.stderr)
        return 2
    path = Path(argv[0])
    try:
        text = path.read_text(encoding="utf-8")
    except OSError as exc:
        print(f"cannot read {path}: {exc}", file=sys.stderr)
        return 2
    problems = find_problems(text)
    if problems:
        print(
            f"INCOMPLETE handoff: {path} -- {len(problems)} section(s) missing/empty:",
            file=sys.stderr,
        )
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
        return 1
    print(f"OK: {path} -- all {len(REQUIRED)} required sections present and non-empty.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
