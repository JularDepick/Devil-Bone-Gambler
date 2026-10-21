#!/usr/bin/env python3
"""Crafting recipe coverage guard of Devil Bone Gambler.

The vanilla recipe book only lists a mod item when a recipe actually exists for it, so this guard keeps the recipe
files and the registered items honest:

  1. every recipe file is valid JSON and points at an item of this mod that really exists,
  2. every recipe carries the vanilla group field, so the recipe book can sort and search it,
  3. the items the mod never intends to be craftable are named here explicitly, so adding a recipe for one of them
     has to be a deliberate change of that list rather than an accident.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NAMESPACE = "devil_bone_gambler"
RECIPES = os.path.join(ROOT, "src", "main", "resources", "data", NAMESPACE, "recipes")
ITEM_MODELS = os.path.join(ROOT, "src", "main", "resources", "assets", NAMESPACE, "models", "item")
GROUP = NAMESPACE
CRAFTING_TYPES = ("minecraft:crafting_shaped", "minecraft:crafting_shapeless")

# Items that are deliberately not craftable: the three manuals are granted by role and the dealer spawn egg is a
# creative tool.
NEVER_CRAFTABLE = (
    "gameplay_manual",
    "admin_manual",
    "creator_manual",
    "gamblers_dealer_spawn_egg",
)

CRAFTABLE = (
    "devils_altar",
    "devils_dice",
    "gamblers_dice",
    "lucky_apple",
    "offering_value_table",
)

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)


def read_json(path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def main() -> int:
    check(os.path.isdir(RECIPES), "the recipe directory is missing: %s" % RECIPES)

    if not os.path.isdir(RECIPES):
        print("RESULT: %s" % ("OK" if not failures else "%d failure(s)" % len(failures)))
        return 1 if failures else 0

    recipes = sorted(name for name in os.listdir(RECIPES) if name.endswith(".json"))
    results = []

    for name in recipes:
        path = os.path.join(RECIPES, name)

        try:
            recipe = read_json(path)
        except ValueError as error:
            check(False, "%s is not valid JSON: %s" % (name, error))
            continue

        recipe_type = recipe.get("type")
        check(recipe_type in CRAFTING_TYPES, "%s uses an unexpected recipe type: %s" % (name, recipe_type))

        result = recipe.get("result") or {}
        item = result.get("item", "")
        check(item.startswith(NAMESPACE + ":"), "%s does not produce an item of this mod" % name)

        item_id = item.split(":", 1)[1] if ":" in item else item
        results.append(item_id)

        model_path = os.path.join(ITEM_MODELS, item_id + ".json")
        check(os.path.exists(model_path), "%s produces %s which has no item model" % (name, item_id))

        check(recipe.get("group") == GROUP,
              "%s has no %s group, so the recipe book cannot sort or search it" % (name, GROUP))

    for item_id in CRAFTABLE:
        check(item_id in results, "%s should be craftable but has no recipe file" % item_id)

    for item_id in NEVER_CRAFTABLE:
        check(item_id not in results,
              "%s must stay uncraftable but a recipe now produces it" % item_id)

    print("RESULT: %s" % ("OK" if not failures else "%d failure(s)" % len(failures)))

    for failure in failures:
        print("  - %s" % failure)

    print("recipes: %s" % ", ".join(results) if results else "recipes: none")
    print("never craftable: %s" % ", ".join(NEVER_CRAFTABLE))
    sys.exit(0 if not failures else 1)


if __name__ == "__main__":
    main()
