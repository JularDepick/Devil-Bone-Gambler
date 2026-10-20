#!/usr/bin/env python3
"""Repeat placeholder guard of Devil Bone Gambler.

--R(n) is a suffix of one prize command line: it runs that line n times, n from 1 to 10, and is only accepted at the
end of the line with trailing whitespace ignored. This check asserts the parser, the execution loop, the validation
path and the documented examples stay wired.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler"
REPEAT = JAVA / "commands" / "RepeatPlaceholders.java"
RUNNER = JAVA / "commands" / "CommandRunner.java"
MANUALS = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "manuals"

problems = []


def check(name, condition, message):
    if not condition:
        problems.append(name + ": " + message)


def read(path):
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def main() -> int:
    repeat = read(REPEAT)
    runner = read(RUNNER)

    check("repeat class", "class RepeatPlaceholders" in repeat and "Pattern.compile" in repeat,
          "the repeat placeholder parser is missing")
    check("lower bound", "MIN_REPEAT = 1" in repeat, "the lower repeat bound is not 1")
    check("upper bound", "MAX_REPEAT = 10" in repeat, "the upper repeat bound is not 10")
    check("end only", "line.substring(lastEnd).trim().isEmpty()" in repeat,
          "the suffix is not restricted to the end of the line")
    check("body present", "line.substring(0, lastStart).trim().isEmpty()" in repeat,
          "empty command bodies are not rejected")
    check("integer only", "argument.matches(" in repeat and "repeat count must be an integer" in repeat,
          "the repeat count is not restricted to digits")
    check("single suffix", "matches > 1" in repeat and "only one repeat placeholder" in repeat,
          "several suffixes are not rejected")
    check("malformed token", "malformed repeat placeholder" in repeat,
          "a broken --R token is not rejected")
    check("range", "count < MIN_REPEAT || count > MAX_REPEAT" in repeat,
          "the repeat count is not checked against the range")

    check("runner validates", "RepeatPlaceholders.findProblem(line)" in runner,
          "the command runner does not validate the suffix")
    check("runner strips", "RepeatPlaceholders.body(line)" in runner,
          "the suffix is not removed before execution")
    check("runner loops", "RepeatPlaceholders.count(line)" in runner and "for (int repetition" in runner,
          "the command line is not executed once per repetition")
    check("runner expands each", "RandomPoolSampler.sample(body" in runner
          and "SamplePlaceholders.expand(command" in runner and "NumberPlaceholders.expand(command" in runner,
          "a repetition does not re-expand the other placeholders")
    check("validation strips", "CommandPlaceholders.findProblem(body)" in runner
          and "SamplePlaceholders.findProblem(body)" in runner
          and "NumberPlaceholders.findProblem(body)" in runner,
          "the validation still parses the suffix as command text")
    check("sample form strips", "NumberPlaceholders.sampleForm(body)" in runner,
          "the parser sample form does not use the stripped body")

    for name in ("admin_manual_zh_cn.md", "admin_manual_en_us.md",
                 "creator_manual_zh_cn.md", "creator_manual_en_us.md"):
        text = read(MANUALS / name)
        check("manual " + name, "--R(n)" in text and "--R(5)" in text,
              "the repeat placeholder is not documented")

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("repeat placeholder check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
