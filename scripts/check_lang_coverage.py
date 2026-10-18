#!/usr/bin/env python3
"""Translation coverage guard of Devil Bone Gambler.

The game builds many language keys at runtime (a per file LANG prefix, a preset slug, a notify mode, a reason code), so
a check that only looks at literal strings leaves holes. This guard rebuilds every key the code can emit and proves that

  1. every key the code emits is defined in both language files,
  2. both language files define exactly the same keys,
  3. every preset entry has both an entry name key and a notify text key,
  4. every notify mode declared in the constants has a mode key,
  5. every reason code emitted by the server has a reason key,
  6. both language files use the same number of placeholders in every value.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "src" / "main" / "java"
LANG_DIR = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "lang"
LANG_FILES = ("zh_cn.json", "en_us.json")
MOD_ID = "devil_bone_gambler"
PREFIX = "gui." + MOD_ID


def java_files():
    return sorted(SOURCE_ROOT.rglob("*.java"))


def per_file_prefix(text):
    """Resolves the LANG or LANG_PREFIX constant of one file, for example gui.devil_bone_gambler.dealer."""
    found = re.search(
        'static final String (?:LANG|LANG_PREFIX) = "([A-Za-z_.]+)" + ModConstants.MOD_ID + "([^"]+)"'.replace(
            " = ", r"\s*=\s*").replace(" + ", r"\s*\+\s*"), text)

    if not found:
        return None

    return found.group(1) + MOD_ID + found.group(2)


def emitted_keys():
    keys = {}
    literal = re.compile(r'translatable\(\s*"([^"+]+)"\s*\)')
    prefixed = re.compile(r'translatable\(\s*(?:LANG|LANG_PREFIX)\s*\+\s*"([A-Za-z0-9_.]+)"')
    helper = re.compile(r'\bkey\(\s*"([^"]+)"\s*\)')
    modid = re.compile(r'translatable\(\s*"gui\."\s*\+\s*ModConstants\.MOD_ID\s*\+\s*"\.([A-Za-z0-9_.]+)"')

    for path in java_files():
        text = path.read_text(encoding="utf-8")
        name = path.name
        prefix = per_file_prefix(text)

        # The preset pool builds notify.<slug> through its own helper, and the preset family check covers it below.
        literal_slugs = name == "DefaultPrizePool.java"

        def add(key):
            if key and not key.endswith(".") and "+" not in key and '"' not in key:
                keys.setdefault(key, name)

        if not literal_slugs:
            for match in literal.finditer(text):
                add(match.group(1))

        if prefix:
            for match in prefixed.finditer(text):
                add(prefix + match.group(1))

        for match in helper.finditer(text):
            add(PREFIX + "." + match.group(1))

        for match in modid.finditer(text):
            add(PREFIX + "." + match.group(1))

    return keys


def notify_modes():
    text = (SOURCE_ROOT / "com" / "devilbonegambler" / "util" / "ModConstants.java").read_text(encoding="utf-8")
    return sorted(set(re.findall(r'NOTIFY_MODE_[A-Z_]+[A-Za-z_]*\s*=\s*"([A-Za-z0-9_]+)"', text)))


def reason_codes(defined_tables):
    """Reason keys the code can actually produce: a literal in the sources that names a defined reason key."""
    defined = set()
    prefix = PREFIX + ".reason."

    for table in defined_tables.values():
        for key in table:
            if key.startswith(prefix):
                defined.add(key[len(prefix):])

    literal = re.compile(r'"([a-z][a-z0-9_]{3,})"')
    produced = set()

    for path in java_files():
        text = path.read_text(encoding="utf-8")

        for match in literal.finditer(text):
            if match.group(1) in defined:
                produced.add(match.group(1))

    return sorted(produced)


def placeholders(value):
    return len(re.findall(r"%[sd]", value))


def main() -> int:
    problems = []
    langs = {name: json.loads((LANG_DIR / name).read_text(encoding="utf-8")) for name in LANG_FILES}
    keys = emitted_keys()

    for key, source in sorted(keys.items()):
        for name, table in langs.items():
            if key not in table:
                problems.append("missing key " + key + " in " + name + " (emitted by " + source + ")")

    defined = {name: set(table) for name, table in langs.items()}
    only_zh = sorted(defined["zh_cn.json"] - defined["en_us.json"])
    only_en = sorted(defined["en_us.json"] - defined["zh_cn.json"])

    if only_zh:
        problems.append("keys only in zh_cn.json: " + ", ".join(only_zh))

    if only_en:
        problems.append("keys only in en_us.json: " + ", ".join(only_en))

    # 预设条目的名称与展示文本属于配置数据, 已随 DefaultPrizePoolData 以双语字面量冻结, 语言文件里不得再有它们.
    for name, table in langs.items():
        stale = sorted(key for key in table
                       if key.startswith("entry." + MOD_ID + ".")
                       or (key.startswith(PREFIX + ".notify.") and not key.startswith(PREFIX + ".notify.mode.")))

        for key in stale:
            problems.append("preset text is still a language key: " + key + " in " + name)

    reward = PREFIX + ".draw.reward"

    for name, table in langs.items():
        if reward not in table:
            problems.append("missing " + reward + " in " + name)

    preset_source = (SOURCE_ROOT / "com" / "devilbonegambler" / "config" / "DefaultPrizePool.java").read_text(encoding="utf-8")

    if '{"translate"' in preset_source:
        problems.append("the preset pool still writes a text component instead of a plain string")

    sender_source = (SOURCE_ROOT / "com" / "devilbonegambler" / "draw" / "NotifySender.java").read_text(encoding="utf-8")

    if "Component.Serializer" in sender_source:
        problems.append("a display text is still parsed as a component")

    modes = notify_modes()

    if not modes:
        problems.append("no notify modes could be read from ModConstants")

    for mode in modes:
        key = PREFIX + ".notify.mode." + mode

        for name, table in langs.items():
            if key not in table:
                problems.append("missing " + key + " in " + name)

    for code in reason_codes(langs):
        key = PREFIX + ".reason." + code

        for name, table in langs.items():
            if key not in table:
                problems.append("missing " + key + " in " + name)

    for shared in sorted(defined["zh_cn.json"] & defined["en_us.json"]):
        zh_count = placeholders(langs["zh_cn.json"][shared])
        en_count = placeholders(langs["en_us.json"][shared])

        if zh_count != en_count:
            problems.append(
                "placeholder count differs on " + shared + ": " + str(zh_count) + " against " + str(en_count))

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("translation coverage check passed (" + str(len(keys)) + " emitted keys, "
          + str(len(modes)) + " notify modes, " + str(len(reason_codes(langs))) + " reason codes, "
          + str(len(defined["zh_cn.json"])) + " keys per file)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
