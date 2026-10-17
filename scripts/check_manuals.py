#!/usr/bin/env python3
"""Manual content guard of Devil Bone Gambler.

Checks the shipped markdown of the three manuals:
  1. every manual has a file for the Simplified Chinese and the English content,
  2. each file is non empty, starts with a level one heading and keeps the heading count of its language pair in step,
  3. the creator manual ends with the exact admin manual content, because that half is reused verbatim,
  4. no manual describes an internal rule that must never reach a player, currently the silent spawn rule, except for
     the creator manual above its separator, where that rule is published on purpose,
  5. no manual reveals a manual the reader is not supposed to know about,
  6. every content line uses one of the markdown constructs the book screen renders,
  7. heading levels never skip a level and no manual repeats a heading.

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

# A manual may not reveal the manuals of a higher role: the gameplay manual knows only itself, the admin manual
# knows itself but never the creator manual, and the creator manual is free to name the admin manual it reuses.
HIDDEN_MANUALS = {
    "gameplay_manual": (("管理员手册", "admin manual"), ("创建者手册", "creator manual")),
    "admin_manual": (("创建者手册", "creator manual"),),
    "creator_manual": (),
}

# The markdown constructs the book screen renders: three heading levels, a dash bullet, a numbered item, a rule and a
# plain paragraph. Anything else would fall back to a paragraph, so it is rejected here instead of looking broken.
LEADING_MARKERS = ("* ", "+ ", "> ", "|", "![", "```", "#", ">")
BULLET = "- "
RULE = "---"


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


def unsupported(line: str) -> str:
    """Reason the line is not one of the supported constructs, or an empty string when it is fine."""

    stripped = line.strip()

    if not stripped:
        return ""

    if stripped == RULE:
        return ""

    if stripped.startswith("### ") or stripped.startswith("## ") or stripped.startswith("# "):
        return ""

    if stripped.startswith(BULLET):
        return ""

    if stripped.startswith("* ") or stripped.startswith("+ "):
        return "the bullet uses %r instead of %r" % (stripped[:2], BULLET)

    for marker in LEADING_MARKERS:
        if marker in ("#", ">") and stripped.startswith(marker):
            return "unsupported markdown marker %r" % marker

        if marker not in ("#", ">") and stripped.startswith(marker):
            return "unsupported markdown marker %r" % marker

    if line[:1].isspace():
        return "the line is indented"

    number = stripped.split(".", 1)

    if len(number) == 2 and number[0].isdigit() and number[1].startswith(" "):
        return ""

    return ""


def heading_levels(lines: list) -> list:
    levels = []

    for line in lines:
        stripped = line.strip()

        if stripped.startswith("### "):
            levels.append(3)
        elif stripped.startswith("## "):
            levels.append(2)
        elif stripped.startswith("# "):
            levels.append(1)

    return levels


def separator_index(lines: list) -> int:
    """Index of the separator that introduces the reused content, or the end of the file when there is none."""

    for index, line in enumerate(lines):
        if line.strip() == SEPARATOR:
            return index

    return len(lines)


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

            # The silent spawn rule is published to the creator only: it may appear in the part of the creator
            # manual before the separator, while every manual is still checked against the reused admin tail.
            checked = lines if manual != REUSED_MANUAL else lines[separator_index(lines):]

            for line in checked:
                for word in FORBIDDEN:
                    if word in line and not line.lstrip().startswith("<!--"):
                        problems.append("internal rule wording %r in %s_%s.md" % (word, manual, language))

            if not any(line.strip() == SEPARATOR for line in lines) and manual == REUSED_MANUAL:
                problems.append("the creator manual has no separator before the reused content")

            for words in HIDDEN_MANUALS[manual]:
                for word in words:
                    if any(word in line for line in lines):
                        problems.append("%s mentions %r, which it must not reveal" % (manual, word))

            for line in lines:
                reason = unsupported(line)

                if reason:
                    problems.append("%s_%s.md: %s in %r" % (manual, language, reason, line.strip()[:40]))

            levels = heading_levels(lines)
            seen = set()

            for index, line in enumerate(lines):
                stripped = line.strip()

                if stripped.startswith("#") and " " in stripped:
                    text = stripped.split(" ", 1)[1]

                    if text in seen:
                        problems.append("%s_%s.md repeats the heading %r" % (manual, language, text))

                    seen.add(text)

            for before, after in zip(levels, levels[1:]):
                if after > before + 1:
                    problems.append("%s_%s.md skips a heading level (%d then %d)" % (manual, language, before, after))

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
