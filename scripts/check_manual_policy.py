#!/usr/bin/env python3
"""Manual policy guard of Devil Bone Gambler.

The manuals of the two roles are kept in place by a calibration pass and by nothing else, so this check proves that

  1. the pass runs on the documented interval and both the constant and the service agree,
  2. the service still counts the cursor and the slots of an open screen as owned, so picking a manual up with the
     mouse is not mistaken for having dropped it,
  3. the copies beyond the first are the ones that are cleared,
  4. the fragile handlers that had to guess where a manual went (toss, join, pickup, container close) are gone,
  5. the item itself still refuses the hotbar drop, so the common drop key never produces a manual in the world.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler"
SERVICE = SOURCE_ROOT / "manual" / "ManualService.java"
CONSTANTS = SOURCE_ROOT / "util" / "ModConstants.java"
ITEM = SOURCE_ROOT / "item" / "ManualItem.java"
INTERVAL = 10
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

    match = re.search(r"MANUAL_CALIBRATION_INTERVAL_TICKS\s*=\s*(\d+)", constants)

    if not match:
        problems.append("the calibration interval is not defined in ModConstants")
    elif int(match.group(1)) != INTERVAL:
        problems.append("the calibration interval is " + match.group(1) + " instead of " + str(INTERVAL))

    if "MANUAL_CALIBRATION_INTERVAL_TICKS" not in service:
        problems.append("the manual service does not run on the calibration interval")

    if "getCarried()" not in service:
        problems.append("the manual service does not count the stack on the cursor as owned")

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

    if "onDroppedByPlayer" not in item:
        problems.append("the manual item no longer refuses the hotbar drop")

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("manual policy check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
