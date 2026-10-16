#!/usr/bin/env python3
"""Language key guard of Devil Bone Gambler.

Checks that
  1. the simplified Chinese and the English language file carry exactly the same key set,
  2. every language key a Java source file statically refers to exists in both files,
  3. every value of the two files is non empty and every placeholder of one language is present in the other.

A key built by string concatenation or by a dynamic suffix is reported as dynamic and never fails the check.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src" / "main" / "java"
LANG_ROOT = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "lang"
LANGUAGES = ("zh_cn.json", "en_us.json")
MOD_ID = "devil_bone_gambler"

FULL_KEY_PATTERN = re.compile(r'"(?P<key>(?:gui|item|block|entity|key|itemGroup|entry)\.' + MOD_ID + r'(?:\.[A-Za-z0-9_]+)*)"')
CONCAT_PATTERN = re.compile(r'"gui\."\s*\+\s*ModConstants\.MOD_ID\s*\+\s*"\.(?P<suffix>[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)*)"')
HELPER_PATTERN = re.compile(r'"gui\."\s*\+\s*ModConstants\.MOD_ID\s*\+\s*"\."\s*\+\s*suffix')
HELPER_CALL_PATTERN = re.compile(r'\bkey\("(?P<suffix>[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)*)"\)')
PREFIX_CONSTANT_PATTERN = re.compile(r'String\s+(?P<name>[A-Za-z0-9_]+)\s*=\s*"gui\."\s*\+\s*ModConstants\.MOD_ID\s*\+\s*"\.(?P<prefix>[A-Za-z0-9_.]*)"\s*;')
PLACEHOLDER_PATTERN = re.compile(r"%[sd]")


def read_language(name: str) -> dict:
    path = LANG_ROOT / name
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def referenced_keys() -> tuple:
    static = set()
    dynamic = set()

    for path in sorted(JAVA_ROOT.rglob("*.java")):
        text = path.read_text(encoding="utf-8")

        for match in FULL_KEY_PATTERN.finditer(text):
            static.add(match.group("key"))

        for match in CONCAT_PATTERN.finditer(text):
            static.add("gui." + MOD_ID + "." + match.group("suffix"))

        if HELPER_PATTERN.search(text):
            for match in HELPER_CALL_PATTERN.finditer(text):
                static.add("gui." + MOD_ID + "." + match.group("suffix"))

        for match in PREFIX_CONSTANT_PATTERN.finditer(text):
            name = match.group("name")
            prefix = match.group("prefix")
            suffix_pattern = re.compile(r'\b' + re.escape(name) + r'\s*\+\s*"(?P<suffix>[A-Za-z0-9_.]*)"')

            for call in suffix_pattern.finditer(text):
                static.add("gui." + MOD_ID + "." + prefix + call.group("suffix"))

        for line in text.splitlines():
            if "ModConstants.MOD_ID" in line and re.search(r'"\.(?:reason|value|admin|altar|dealer|wager|dice|notify)\.', line):
                dynamic.add(line.strip())

    return static, dynamic


def main() -> int:
    problems = []
    languages = {}

    for name in LANGUAGES:
        path = LANG_ROOT / name

        if not path.is_file():
            problems.append("missing language file: " + name)
            continue

        languages[name] = read_language(name)

    if len(languages) != len(LANGUAGES):
        print("\n".join(problems))
        return 1

    first, second = (languages[name] for name in LANGUAGES)
    only_first = sorted(set(first) - set(second))
    only_second = sorted(set(second) - set(first))

    for key in only_first:
        problems.append("only in " + LANGUAGES[0] + ": " + key)

    for key in only_second:
        problems.append("only in " + LANGUAGES[1] + ": " + key)

    for name, table in languages.items():
        for key, value in sorted(table.items()):
            if value is None or not str(value).strip():
                problems.append("empty value in " + name + ": " + key)

    for key in sorted(set(first) & set(second)):
        left = sorted(set(PLACEHOLDER_PATTERN.findall(str(first[key]))))
        right = sorted(set(PLACEHOLDER_PATTERN.findall(str(second[key]))))

        if left != right:
            problems.append("placeholder mismatch for " + key + ": " + str(left) + " against " + str(right))

    static, dynamic = referenced_keys()

    for key in sorted(static):
        if key not in first:
            problems.append("referenced but not defined: " + key)

    unused = sorted(set(first) - static)
    print("language keys: %d" % len(first))
    print("statically referenced keys: %d" % len(static))
    print("dynamic key sites (informational): %d" % len(dynamic))
    print("keys not referenced statically (informational): %d" % len(unused))

    if "--verbose" in sys.argv:
        for key in unused:
            print("  informational, not referenced statically: " + key)

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("language key check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
