#!/usr/bin/env python3
"""Translation boundary guard.

The boundary of the v0.6.0 translation work:

  1. every player visible text lives in both language files with the same placeholders,
  2. the English side never carries Chinese text,
  3. source code carries no Chinese literal except the preset display text table of the default prize pool,
  4. the preset display texts are config data and never language keys,
  5. the shipped English manuals stay English.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
LANG = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "lang"
MANUALS = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "manuals"
PREFIX = "gui.devil_bone_gambler."
PRESET_SOURCE = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "config" / "DefaultPrizePool.java"
CJK = re.compile(r"[\u4e00-\u9fff]")
PLACEHOLDER = re.compile(r"%[sd]")

problems = []


def strip_comments(text: str) -> str:
    out = []
    index = 0
    size = len(text)
    in_string = False

    while index < size:
        char = text[index]

        if in_string:
            out.append(char)

            if char == "\\":
                out.append(text[index + 1] if index + 1 < size else "")
                index += 2
                continue

            if char == '"':
                in_string = False

            index += 1
            continue

        if char == '"':
            in_string = True
            out.append(char)
            index += 1
            continue

        if char == "/" and index + 1 < size and text[index + 1] == "/":
            while index < size and text[index] != "\n":
                index += 1
            continue

        if char == "/" and index + 1 < size and text[index + 1] == "*":
            index += 2

            while index + 1 < size and not (text[index] == "*" and text[index + 1] == "/"):
                index += 1

            index += 2
            continue

        out.append(char)
        index += 1

    return "".join(out)


def main() -> int:
    zh = json.loads((LANG / "zh_cn.json").read_text(encoding="utf-8"))
    en = json.loads((LANG / "en_us.json").read_text(encoding="utf-8"))

    for key in sorted(set(zh) - set(en)):
        problems.append("missing in en_us: " + key)

    for key in sorted(set(en) - set(zh)):
        problems.append("missing in zh_cn: " + key)

    for key in sorted(set(zh) & set(en)):
        if not str(zh[key]).strip() or not str(en[key]).strip():
            problems.append("empty value: " + key)
        elif sorted(PLACEHOLDER.findall(str(zh[key]))) != sorted(PLACEHOLDER.findall(str(en[key]))):
            problems.append("placeholder mismatch: " + key)

    for key, value in sorted(en.items()):
        if CJK.search(str(value)):
            problems.append("Chinese text left in en_us: " + key)

    for path in sorted((ROOT / "src" / "main" / "java").rglob("*.java")):
        code = strip_comments(path.read_text(encoding="utf-8"))

        for match in re.finditer(r'"((?:[^"\\]|\\.)*)"', code):
            if CJK.search(match.group(1)) and path.resolve() != PRESET_SOURCE.resolve():
                problems.append("Chinese literal outside the preset table: " + str(path.relative_to(ROOT))
                                + " | " + match.group(1))

    preset = PRESET_SOURCE.read_text(encoding="utf-8")
    chinese_rows = len(CJK.findall(preset))
    rows = len(re.findall(r'\{"[a-z0-9_]+", "[^"]+", "[^"]+"\}', preset))

    if rows != 24 or chinese_rows < 24:
        problems.append("the preset text table no longer holds 24 Chinese and English rows")

    slugs = sorted(set(re.findall(r'add\(\s*[A-Za-z]+,\s*"([A-Za-z0-9_]+)"', preset)))

    for slug in slugs:
        display = PREFIX + ".notify." + slug

        for table, name in ((zh, "zh_cn"), (en, "en_us")):
            if display in table:
                problems.append("preset display text is a language key again: " + display + " in " + name)

    for key in (PREFIX + "language", PREFIX + "draw.reward"):
        for table, name in ((zh, "zh_cn"), (en, "en_us")):
            if key not in table:
                problems.append("missing boundary key " + key + " in " + name)

    for path in sorted(MANUALS.glob("*_en_us.md")):
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if CJK.search(line):
                problems.append("Chinese text left in the English manual: " + path.name + ":" + str(number))

    for suffix in ("gameplay_manual", "admin_manual", "creator_manual"):
        for language in ("zh_cn", "en_us"):
            if not (MANUALS / (suffix + "_" + language + ".md")).is_file():
                problems.append("missing manual: " + suffix + "_" + language + ".md")

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("translation boundary check passed: " + str(len(zh)) + " keys, " + str(len(slugs)) + " presets")
    return 0


if __name__ == "__main__":
    sys.exit(main())
