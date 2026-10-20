#!/usr/bin/env python3
"""Altar protection guard of Devil Bone Gambler.

The altar is a low grade bedrock: an iron pickaxe or better is the only way to take it down and to get the item.
Checks that the destroy progress, the direct removal hook, the drops, the explosion resistance, the piston reaction and
the entity destruction gate all enforce that rule.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler"
BLOCK = JAVA / "block" / "DevilsAltarBlock.java"
BLOCKS = JAVA / "registry" / "ModBlocks.java"
CONSTANTS = JAVA / "util" / "ModConstants.java"

problems = []


def check(name, condition, message):
    if not condition:
        problems.append(name + ": " + message)


def main() -> int:
    block = BLOCK.read_text(encoding="utf-8")
    blocks = BLOCKS.read_text(encoding="utf-8")
    constants = CONSTANTS.read_text(encoding="utf-8")

    check("resistance constant", "ALTAR_EXPLOSION_RESISTANCE = 3600000.0F" in constants,
          "the altar explosion resistance is missing")

    check("registered resistance", "ALTAR_EXPLOSION_RESISTANCE" in blocks,
          "the altar block properties do not use the explosion resistance")
    check("no weak resistance", "strength(3.0F, 6.0F)" not in blocks,
          "the altar still has the ordinary explosion resistance")

    check("iron gate", "Tiers.IRON" in block and "TieredItem" in block,
          "the iron pickaxe tier is not required")
    check("correct tool", "ForgeHooks.isCorrectToolForDrops" in block,
          "the altar does not require a correct tool")
    check("progress gate", "if (!canMine(state, player))" in block,
          "the destroy progress does not consult the iron gate")
    check("direct removal gate", "onDestroyedByPlayer" in block and "player == null || !canMine" in block,
          "a direct block removal bypasses the iron gate")
    check("drops gate", "tool.isCorrectToolForDrops(state)" in block and "isIronTier(tool)" in block,
          "the altar can drop without an iron pickaxe")
    check("explosion hook", "getExplosionResistance" in block,
          "the altar does not override the explosion resistance")
    check("piston gate", "getPistonPushReaction" in block and "PushReaction.BLOCK" in block,
          "a piston can still move the altar")
    check("entity gate", "canEntityDestroy" in block and "return false;" in block,
          "the dragon or the wither can still destroy the altar")
    check("tier sorting", "TierSortingRegistry" in block,
          "modded tool tiers are not compared through the Forge sorting registry")

    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("altar protection check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
