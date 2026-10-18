#!/usr/bin/env python3
"""Manual policy guard of Devil Bone Gambler.

The manuals of the two roles are kept in place by a calibration pass and by nothing else, so this check proves that

  1. the pass runs every ten seconds (200 ticks) and both the constant and the service agree,
  2. the service still counts the cursor, the off hand, the armour slots and the slots of an open screen as owned, so
     picking a manual up with the mouse is not mistaken for having dropped it, and a grant is deferred while the cursor
     carries anything at all,
  3. the copies beyond the first are the ones that are cleared,
  4. the fragile handlers that had to guess where a manual went (toss, join, pickup, container close) are gone,
  5. the item no longer reacts to a drop, because a dropped copy is cleared by the next calibration pass instead,
  6. every manual carries its own name colour, the item colours its own name with it, and the book icon of every
     manual is painted with the very same colour: gameplay 00FF00, admin 00FFAA, creator FFAA00.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import re
import sys

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler"
SERVICE = SOURCE_ROOT / "manual" / "ManualService.java"
CONSTANTS = SOURCE_ROOT / "util" / "ModConstants.java"
ITEM = SOURCE_ROOT / "item" / "ManualItem.java"
KIND = SOURCE_ROOT / "manual" / "ManualKind.java"
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "textures" / "item"
NAME_COLORS = {
    "GAMEPLAY": "0x00FF00",
    "ADMIN": "0x00FFAA",
    "CREATOR": "0xFFAA00",
}
INTERVAL = 200
REMOVED_HANDLERS = (
    "ItemTossEvent",
    "EntityJoinLevelEvent",
    "EntityItemPickupEvent",
)


def main() -> int:
    problems = []
    service = SERVICE.read_text(encoding="utf-8")
    constants = CONSTANTS.read_text(encoding="utf-8")
    item = ITEM.read_text(encoding="utf-8")
    kind = KIND.read_text(encoding="utf-8")

    match = re.search(r"MANUAL_CALIBRATION_INTERVAL_TICKS\s*=\s*(\d+)", constants)

    if not match:
        problems.append("the calibration interval is not defined in ModConstants")
    elif int(match.group(1)) != INTERVAL:
        problems.append("the calibration interval is " + match.group(1) + " instead of " + str(INTERVAL))

    if "MANUAL_CALIBRATION_INTERVAL_TICKS" not in service:
        problems.append("the manual service does not run on the calibration interval")

    if "getCarried()" not in service:
        problems.append("the manual service does not count the stack on the cursor as owned")

    if "getCarried().isEmpty()" not in service:
        problems.append("the manual service grants while the cursor carries an item")

    if ".offhand" not in service or ".armor" not in service:
        problems.append("the manual service does not count the off hand and the armour slots as owned")

    if "menu.slots" not in service or "slot.container !=" not in service:
        problems.append("the manual service does not count the slots of an open screen as owned")

    if "subList(1," not in service:
        problems.append("the manual service does not keep the first copy and clear the rest")

    for handler in REMOVED_HANDLERS:
        if handler in service:
            problems.append("the manual service listens to " + handler + " again")

    if "PlayerLoggedInEvent" not in service or "PlayerRespawnEvent" not in service:
        problems.append("the manual service does not react to a join and a respawn")

    if "LivingDeathEvent" not in service:
        problems.append("the manual service does not take the bound manuals back at death")

    if "PlayerContainerEvent" not in service or "onContainerClosed" not in service:
        problems.append("the manual service lets a bound manual stay in a container")

    for name, color in NAME_COLORS.items():
        if not re.search(rf'{name}\("[a-z_]+", {re.escape(color)}\)', kind):
            problems.append("the " + name + " manual name is not coloured " + color)

        texture = TEXTURES / (name.lower() + "_manual.png")

        if not texture.is_file():
            problems.append("the " + name + " manual icon is missing")
            continue

        with Image.open(texture) as raw:
            image = raw.convert("RGBA")

        counts = {}
        for y in range(image.height):
            for x in range(image.width):
                pixel = image.getpixel((x, y))

                if pixel[3] == 255:
                    counts[pixel[:3]] = counts.get(pixel[:3], 0) + 1

        expected = tuple(int(color[index:index + 2], 16) for index in (2, 4, 6))
        dominant = max(counts, key=counts.get) if counts else None

        if dominant != expected:
            problems.append("the " + name + " manual cover is " + str(dominant) + " instead of " + str(expected))

    if "getName(" not in item or "withColor" not in item or "nameColor()" not in item:
        problems.append("the manual item does not colour its own name")

    if "onDroppedByPlayer" in item:
        problems.append("the manual item handles the drop again instead of leaving it to the calibration")

    if "suppressNotice" in item or "manual.kept" in item:
        problems.append("the manual item still prompts about a drop")

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("manual policy check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
