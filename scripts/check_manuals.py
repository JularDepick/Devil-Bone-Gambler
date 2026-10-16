#!/usr/bin/env python3
"""Manual content guard of Devil Bone Gambler.

Checks the shipped markdown of the three manuals:
  1. every manual has a file for the Simplified Chinese and the English content,
  2. each file is non empty, starts with a level one heading and keeps the heading count of its language pair in step,
  3. the creator manual ends with the exact admin manual content, because that half is reused verbatim,
  4. no manual describes an internal rule that must never reach a player, currently the silent spawn rule.

Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MANUAL_ROOT = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "manuals"
LANGUAGES = ("zh_cn", "en_us")
MANUALS = ("gameplay_manual", "admin_manual", "creator_manual")
REUSED_MANUAL = "creator_manual"
REUSED_SOURCE = "admin_manual"
SEPARATOR = "---"

# Wording that would leak an internal rule, which the modules must never publish into a shipped manual.
FORBIDDEN = ("日出", "日落", "sunrise", "sunset")


def read(manual: str, language: str) -> list:
    path = MANUAL_ROOT / (manual + "_" + language + ".md")

    if not path.is_file():
        return []

    with path.open(encoding="utf-8") as handle:
        return handle.read().splitlines()


def headings(lines: list) -> int:
    return sum(1 for line in lines if line.startswith("# "))


def suffixed_by(lines: list, source: list) -> bool:
    """True when the source lines are the trailing block of the given lines."""

    if not source:
        return False

    tail = [line for line in lines]

    while tail and not tail[-1].strip():
        tail.pop()

    while source and not source[-1].strip():
        source = source[:-1]

    if len(tail) < len(source):
        return False

    return tail[len(tail) - len(source):] == source


def main() -> int:
    problems = []
    tables = {}

    for manual in MANUALS:
        for language in LANGUAGES:
            lines = read(manual, language)
            tables[(manual, language)] = lines

            if not lines:
                problems.append("missing or empty content: %s_%s.md" % (manual, language))
                continue

            first = next((line for line in lines if line.strip()), "")

            if not first.startswith("# "):
                problems.append("content does not start with a level one heading: %s_%s.md" % (manual, language))

            for line in lines:
                for word in FORBIDDEN:
                    if word in line and not line.lstrip().startswith("<!--"):
                        problems.append("internal rule wording %r in %s_%s.md" % (word, manual, language))

            if not any(line.strip() == SEPARATOR for line in lines) and manual == REUSED_MANUAL:
                problems.append("the creator manual has no separator before the reused content")

        chinese = headings(tables[(manual, "zh_cn")])
        english = headings(tables[(manual, "en_us")])

        if chinese != english:
            problems.append("heading count differs for %s: %d against %d" % (manual, chinese, english))

    for language in LANGUAGES:
        reused = tables[(REUSED_MANUAL, language)]
        source = tables[(REUSED_SOURCE, language)]

        if reused and source and not suffixed_by(reused, source):
            problems.append("the %s content is not the trailing block of the %s content in %s"
                            % (REUSED_SOURCE, REUSED_MANUAL, language))

    print("manuals: %d, languages: %d" % (len(MANUALS), len(LANGUAGES)))

    for manual in MANUALS:
        for language in LANGUAGES:
            print("  %s_%s.md: %d lines, %d headings"
                  % (manual, language, len(tables[(manual, language)]), headings(tables[(manual, language)])))

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("manual content check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
