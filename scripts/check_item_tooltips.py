#!/usr/bin/env python3
"""Item tooltip coverage guard of Devil Bone Gambler.

Every item of the mod carries tooltip lines, and the lines have to stay honest and current:

  1. every registered item has an entry in the tooltip table, and no entry names an item that does not exist,
  2. every listed key exists in both language files,
  3. the English text is really translated instead of a copy of the Chinese text,
  4. no tooltip key is left behind in the language files after an entry was renamed or removed,
  5. the sight lines mention sneaking, because the sight only shows while sneaking and holding the die.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler"
LANG = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "lang"
PREFIX = "tooltip.devil_bone_gambler."
SIGHT = JAVA / "client" / "render" / "DiceSightRenderer.java"

problems = []


def check(name, condition, message):
    if not condition:
        problems.append(name + ": " + message)


def read(path):
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def registered(patterns):
    ids = []

    for path, pattern in patterns:
        for match in re.finditer(pattern, read(path), re.MULTILINE):
            ids.append(match.group(1))

    return ids


def tooltip_table():
    table = {}
    current = None

    for line in read(JAVA / "client" / "ItemTooltips.java").splitlines():
        opened = re.search(r'LINES\.put\("([a-z_0-9]+)"', line)

        if opened:
            current = opened.group(1)
            table[current] = []
            continue

        if current is None:
            continue

        key = re.search(r'"(tooltip\.devil_bone_gambler\.[a-z_0-9]+)"', line)

        if key:
            table[current].append(key.group(1))

        if "});" in line:
            current = None

    return table


def main() -> int:
    items = registered([
        (JAVA / "registry" / "ModItems.java", r'ITEMS\.register\("([a-z_0-9]+)"'),
        (JAVA / "registry" / "ModBlocks.java", r'BLOCK_ITEMS\.register\("([a-z_0-9]+)"'),
        (JAVA / "manual" / "ManualKind.java", r'^\s+[A-Z]+\("([a-z_0-9]+)"')])
    zh = json.loads(read(LANG / "zh_cn.json") or "{}")
    en = json.loads(read(LANG / "en_us.json") or "{}")
    table = tooltip_table()

    for item in items:
        check("entry " + item, item in table, "the item has no tooltip entry")

    for item in table:
        check("unknown " + item, item in items, "the tooltip entry names an item that does not exist")

    listed = set()

    for item, keys in table.items():
        check("empty " + item, len(keys) > 0, "the tooltip entry lists no lines")

        for key in keys:
            listed.add(key)
            check("missing " + key, key in zh and key in en, "the tooltip key is missing from a language file")

            if key in zh and key in en:
                check("copied " + key, zh[key] != en[key], "the English line is a copy of the Chinese line")

    for key in zh:
        if key.startswith(PREFIX):
            check("stale " + key, key in listed, "the tooltip key is no longer used by any item")

    if "isShiftKeyDown() && holds(" in read(SIGHT):
        for key in ("tooltip.devil_bone_gambler.devils_dice_sight", "tooltip.devil_bone_gambler.gamblers_dice_sight"):
            check("sight " + key, "潜行" in zh.get(key, ""), "the sight line does not mention sneaking")
            check("sight en " + key, "Sneak" in en.get(key, ""), "the sight line does not mention sneaking in English")

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("item tooltip check passed: " + str(len(table)) + " items, " + str(len(listed)) + " lines")
    return 0


if __name__ == "__main__":
    sys.exit(main())
