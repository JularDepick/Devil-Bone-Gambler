#!/usr/bin/env python3
"""Preset prize pool guard of Devil Bone Gambler.

The preset pools are data, and data drifts. This check proves that

  1. the readable source exists and parses,
  2. both pools keep six regions of two hundred and sixty entries,
  3. names are unique inside a pool, and every region carries both colours,
  4. weights stay on the five rarity steps,
  5. commands carry no leftover build marker, no retired token, a bounded length, balanced NBT braces and a real range,
  6. the build packs the readable source into the jar as one minified line,
  7. every preset item exists in the bundled vanilla item table and every give portion fits the item's stack size.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "presets" / "presets.example.json"
GRADLE = ROOT / "build.gradle"
GENERATOR = ROOT / "scripts" / "build_preset_pool.py"
VANILLA = ROOT / "scripts" / "data" / "vanilla_items.txt"
WEIGHTS = {120.0, 60.0, 25.0, 10.0, 3.0}
REGION_ENTRIES = 260
REGIONS = 6
POOLS = ("survival", "creative")
GIVE = re.compile(r"^give \$\{player\} minecraft:([a-z0-9_]+)(?:\{[^\n]*\})?(?: %\((\d+),(\d+)\))?$")
# A stackable prize stays inside two slots even after the region factor is applied.
GIVE_SLOT_LIMIT = 128

sys.path.insert(0, str(ROOT / "scripts"))

import preset_content

problems = []


def check(name, condition, detail=""):
    if not condition:
        problems.append(name + (": " + detail if detail else ""))


def load_vanilla():
    """The vanilla item table: id to maximum stack size."""
    table = {}

    if not VANILLA.is_file():
        return table

    for line in VANILLA.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue

        name, _space, size = line.partition(" ")
        table[name] = int(size)

    return table


def check_item_table(vanilla):
    """Every table item must exist, and the declared stack size must match the vanilla one."""
    ids = [item[0] for item in preset_content.ITEMS]
    check("unique preset items", len(ids) == len(set(ids)), str(len(ids) - len(set(ids))) + " duplicates")

    for item in preset_content.ITEMS:
        item_id = item[0]
        size = vanilla.get(item_id)
        check("preset item " + item_id, size is not None, "not a vanilla item")
        declared = preset_content.stack_size(item_id)
        expected = size if size is not None else 64
        check("stack size " + item_id, declared == expected, "declared %d, vanilla %d" % (declared, expected))


def check_give(name, command, vanilla):
    """A portion must fit the slots the item really has, even after the region factor."""
    for line in command.split("\n"):
        match = GIVE.match(line.strip())

        if match is None:
            continue

        item_id = match.group(1)
        size = vanilla.get(item_id)
        check("give item " + name, size is not None, "unknown item " + item_id)

        if size is None:
            continue

        if size <= 1:
            check("single portion " + name, match.group(2) is None, "a non stacking item carries a count: " + line)
            continue

        if match.group(2) is not None:
            limit = size if size < 64 else GIVE_SLOT_LIMIT
            high = int(match.group(3))
            check("portion size " + name, high <= limit,
                  "%s allows %d above the slot limit %d" % (item_id, high, limit))


def main() -> int:
    check("source file", SOURCE.is_file(), str(SOURCE))

    if not SOURCE.is_file():
        return report()

    try:
        data = json.loads(SOURCE.read_text(encoding="utf-8"))
    except ValueError as error:
        problems.append("source parses: " + str(error))
        return report()

    pools = data.get("pools") or []
    check("two pools", [pool.get("id") for pool in pools] == list(POOLS), str([p.get("id") for p in pools]))

    vanilla = load_vanilla()
    check("vanilla item table", len(vanilla) > 1000, str(len(vanilla)))

    if vanilla:
        check_item_table(vanilla)

    for pool in pools:
        regions = pool.get("regions") or []
        check("six regions " + str(pool.get("id")), len(regions) == REGIONS, str(len(regions)))
        names = set()

        for region in regions:
            entries = region.get("entries") or []
            tier = region.get("tier")
            check("region size %s/%s" % (pool.get("id"), tier), len(entries) == REGION_ENTRIES, str(len(entries)))
            colours = set()

            for entry in entries:
                name = entry.get("nameZh") or ""
                command = entry.get("command") or ""
                names.add(name)
                colours.add(entry.get("color"))
                check("weight %s" % name, float(entry.get("weight") or 0.0) in WEIGHTS, str(entry.get("weight")))
                check("length %s" % name, 0 < len(command) <= 512, str(len(command)))
                check("braces %s" % name, command.count("{") == command.count("}"), name)
                check("marker %s" % name, "#(" not in command and "%p" not in command, name)

                for low, high in re.findall(r"%\((-?\d+),(-?\d+)\)", command):
                    check("range %s" % name, int(low) < int(high), command)

                if vanilla:
                    check_give(name, command, vanilla)

        check("unique names " + str(pool.get("id")), len(names) == REGIONS * REGION_ENTRIES, str(len(names)))

        for region in regions:
            colours = {entry.get("color") for entry in (region.get("entries") or [])}
            check("both colours %s/%s" % (pool.get("id"), region.get("tier")),
                  colours == {"red", "black"}, str(sorted(colours)))

    gradle = GRADLE.read_text(encoding="utf-8") if GRADLE.is_file() else ""
    check("build packs the source", "presets/presets.example.json" in gradle and "presetTarget" in gradle
          and "collect { it.trim() }.join('')" in gradle, "the minify step is missing")

    generator = GENERATOR.read_text(encoding="utf-8") if GENERATOR.is_file() else ""
    check("generator writes the source", '"presets" / "presets.example.json"' in generator, "the output path moved")

    return report()


def report() -> int:
    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("preset pool check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
