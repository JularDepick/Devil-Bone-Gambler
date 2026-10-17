#!/usr/bin/env python3
"""Stack name guard of Devil Bone Gambler.

Checks that a message which names a handed over item reads the name before the inventory can take the stack, because an
inventory that takes the whole stack empties it and an emptied stack reports itself as air.

  1. the shared hand over helper exists and reads the name before the inventory add,
  2. no other file reads a stack name after handing a stack to an inventory,
  3. the manual grant and the dice refund both go through the helper.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "src" / "main" / "java"
HANDOVER = SOURCE_ROOT / "com" / "devilbonegambler" / "util" / "InventoryHandover.java"
GRANT_SITES = (
    SOURCE_ROOT / "com" / "devilbonegambler" / "manual" / "ManualService.java",
    SOURCE_ROOT / "com" / "devilbonegambler" / "draw" / "DrawEngine.java",
)
NAME = "getHoverName()"
ADD = "getInventory().add("


def main() -> int:
    problems = []

    if not HANDOVER.is_file():
        print("missing the shared hand over helper: " + str(HANDOVER))
        return 1

    text = HANDOVER.read_text(encoding="utf-8")
    name_at = text.find(NAME)
    add_at = text.find(ADD)

    if name_at < 0 or add_at < 0 or name_at > add_at:
        problems.append("the hand over helper does not read the name before the add: " + HANDOVER.name)

    for path in sorted(SOURCE_ROOT.rglob("*.java")):
        if path == HANDOVER:
            continue

        body = path.read_text(encoding="utf-8")
        name_at = body.find(NAME)
        add_at = body.find(ADD)

        if name_at >= 0 and add_at >= 0 and name_at > add_at:
            problems.append("reads a stack name after an inventory add, which reports air: " + path.name)

    for path in GRANT_SITES:
        if not path.is_file():
            problems.append("missing the grant site: " + str(path))
            continue

        if "InventoryHandover.handOver(" not in path.read_text(encoding="utf-8"):
            problems.append("names a granted item without the shared helper: " + path.name)

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("stack name check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
