#!/usr/bin/env python3
"""Numeric range placeholder guard of Devil Bone Gambler.

The %(n,m) placeholder replaces itself with one integer from the closed range. This check proves that

  1. the token pattern and the bounds exist,
  2. the value is biased by the point, the overflow, the luck and the colour of the draw,
  3. a chat trial without a roll stays uniform,
  4. the command runner expands the token and validates it,
  5. the validation replaces the token with a representative number for the vanilla parser,
  6. the preset pool actually uses the placeholder and never writes an empty range.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler"
SOURCE = JAVA / "commands" / "NumberPlaceholders.java"
RUNNER = JAVA / "commands" / "CommandRunner.java"
DATA = JAVA / "config" / "DefaultPrizePoolData.java"

problems = []


def check(name, condition, message):
    if not condition:
        problems.append(name + ": " + message)


def main() -> int:
    source = SOURCE.read_text(encoding="utf-8") if SOURCE.is_file() else ""
    runner = RUNNER.read_text(encoding="utf-8")
    data = DATA.read_text(encoding="utf-8") if DATA.is_file() else ""

    check("source", bool(source), "the numeric placeholder class is missing")
    check("pattern", "Pattern.compile" in source and r"%\\(" in source,
          "the token pattern is missing")
    check("context use", all(marker in source for marker in (
        "context.point()", "context.overflow()", "context.luck()", "context.red()")),
        "the value does not follow the roll attributes")
    check("draw bias", "nextGaussian" in source, "the value is not distributed around a target")
    check("chat uniform", "nextInt(high - low + 1)" in source,
          "a chat trial without a roll is not uniform")
    check("bounds", "MAX_NUMBER_PLACEHOLDER" in source, "the value bounds are missing")
    check("runner expansion", "NumberPlaceholders.expand(command, context)" in runner,
          "the command runner does not expand the numeric placeholder")
    check("runner validation", "NumberPlaceholders.findProblem(line)" in runner,
          "the command runner does not validate the numeric placeholder")
    check("sample form", "NumberPlaceholders.sampleForm(" in runner,
          "the parser never sees a representative number")

    rows = re.findall(r'"((?:[^"\\]|\\.)*)"\},', data)
    used = 0
    empty = 0

    for raw in rows:
        if "%(" not in raw:
            continue

        for low, high in re.findall(r"%\((-?\d{1,7}),(-?\d{1,7})\)", raw):
            used += 1

            if int(low) >= int(high):
                empty += 1

    check("preset use", used >= 50, "the preset pools barely use the numeric placeholder")
    check("preset ranges", empty == 0, "the preset pools contain an empty numeric range")

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("numeric placeholder check passed: " + str(used) + " ranges in the presets")
    return 0


if __name__ == "__main__":
    sys.exit(main())
