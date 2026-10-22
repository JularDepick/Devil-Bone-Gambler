#!/usr/bin/env python3
"""Lucky apple guard of Devil Bone Gambler.

The lucky apple is one item, one effect, one recipe and one pair of textures. This check proves that

  1. the item texture exists, is 16x16 and really is colourful instead of a plain recolour,
  2. the effect icon exists, is 18x18 and is the same apple, so the icon reuses the item texture,
  3. the item model points at that texture,
  4. the recipe is eight gamblers dice around one apple,
  5. the item and the effect are registered and the luck lock is wired into the periodic update.
Exit code 0 means the check passed.
"""

from __future__ import annotations

import colorsys
import json
import pathlib
import sys

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler"
DATA = ROOT / "src" / "main" / "resources" / "data" / "devil_bone_gambler"
JAVA = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler"
ITEM_TEXTURE = ASSETS / "textures" / "item" / "lucky_apple.png"
EFFECT_TEXTURE = ASSETS / "textures" / "mob_effect" / "luck_child.png"
ITEM_MODEL = ASSETS / "models" / "item" / "lucky_apple.json"
RECIPE = DATA / "recipes" / "lucky_apple.json"
MIN_HUE_BUCKETS = 6

problems = []


def check(name, condition, message):
    if not condition:
        problems.append(name + ": " + message)


def read(path):
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def hue_buckets(image):
    pixels = image.convert("RGBA").load()
    width, height = image.size
    buckets = set()

    for y in range(height):
        for x in range(width):
            red, green, blue, alpha = pixels[x, y]

            if alpha == 0:
                continue

            hue = colorsys.rgb_to_hsv(red / 255.0, green / 255.0, blue / 255.0)[0]
            buckets.add(int(hue * 12))

    return buckets


def main() -> int:
    if not ITEM_TEXTURE.is_file():
        return report(["missing item texture " + str(ITEM_TEXTURE)])

    item = Image.open(ITEM_TEXTURE)
    check("item size", item.size == (16, 16), "the item texture is not 16x16")
    check("item colourful", len(hue_buckets(item)) >= MIN_HUE_BUCKETS,
          "the apple is not colourful enough to read as a rainbow apple")

    check("effect icon", EFFECT_TEXTURE.is_file(), "the effect icon is missing")

    if EFFECT_TEXTURE.is_file():
        icon = Image.open(EFFECT_TEXTURE)
        check("icon size", icon.size == (18, 18), "the effect icon is not 18x18")
        check("icon reuses the apple", icon.convert("RGBA").tobytes()
              == item.convert("RGBA").resize((18, 18), Image.NEAREST).tobytes(),
              "the effect icon is not the same apple as the item texture")

    model = json.loads(read(ITEM_MODEL) or "{}")
    check("model layer", (model.get("textures") or {}).get("layer0")
          == "devil_bone_gambler:item/lucky_apple", "the item model does not point at the apple texture")

    recipe = json.loads(read(RECIPE) or "{}")
    check("recipe type", recipe.get("type") == "minecraft:crafting_shaped", "the recipe is not a shaped recipe")
    check("recipe group", recipe.get("group") == "devil_bone_gambler", "the recipe has no recipe book group")
    check("recipe pattern", recipe.get("pattern") == ["DDD", "DAD", "DDD"],
          "the recipe is not eight dice around one apple")
    keys = recipe.get("key") or {}
    check("recipe dice", (keys.get("D") or {}).get("item") == "devil_bone_gambler:gamblers_dice",
          "the ring is not made of gamblers dice")
    check("recipe apple", (keys.get("A") or {}).get("item") == "minecraft:apple", "the centre is not an apple")
    check("recipe result", (recipe.get("result") or {}).get("item") == "devil_bone_gambler:lucky_apple",
          "the recipe does not produce the lucky apple")

    items = read(JAVA / "registry" / "ModItems.java")
    effects = read(JAVA / "registry" / "ModEffects.java")
    service = read(JAVA / "luck" / "LuckService.java")
    lock = read(JAVA / "luck" / "LuckLockService.java")
    apple = read(JAVA / "item" / "LuckyAppleItem.java")

    check("item registered", 'register("lucky_apple"' in items, "the lucky apple is not registered")
    check("effect registered", 'register("luck_child"' in effects and "MobEffectCategory" in read(
        JAVA / "effect" / "LuckChildEffect.java"), "the lucky child effect is not registered as a beneficial effect")
    check("lock wired", "LuckLockService.isLocked(player)" in service and "LUCK_LOCKED_VALUE" in lock,
          "the periodic update does not honour the luck lock")
    check("meal behaviour", "recordEating(player)" in apple, "eating the apple does not count as a behaviour")
    constants = read(JAVA / "util" / "ModConstants.java")
    check("child duration", "LUCK_CHILD_TICKS = 300" in constants, "the child of fortune no longer lasts fifteen seconds")
    check("sneak gain", "LUCK_APPLE_SNEAK_GAIN = 5" in constants and "LUCK_APPLE_SNEAK_LIMIT = 80" in constants,
          "the sneaking bite lost its gain or its limit")
    check("sneak branch", "isShiftKeyDown()" in apple and "LUCK_APPLE_SNEAK_GAIN" in apple
          and "LUCK_APPLE_SNEAK_LIMIT" in apple,
          "the apple has no sneaking branch, or it does not honour the gain and the limit")
    check("sneak pays then falls through", "return false" in apple and "LuckSyncPacket(raised)" in apple
          and "if (!sneakingBite(player))" in apple,
          "a sneaking bite above the limit does not fall through to the effect")
    check("duration accumulates", "running.getDuration()" in apple and "ticks += " in apple,
          "a second bite replaces the effect duration instead of extending it")
    check("lock keeps the first value", "putIfAbsent" in lock,
          "an extension would overwrite the value remembered by the first bite")
    check("refill", "setFoodLevel" in apple and "setHealth(player.getMaxHealth())" in apple,
          "the apple does not refill hunger and health")
    check("harmful effects cleared", "MobEffectCategory.HARMFUL" in apple and "removeEffect" in apple
          and "clearHarmfulEffects(player)" in apple,
          "the apple no longer wipes the harmful effects")

    return report(problems)


def report(problems) -> int:
    if problems:
        for problem in problems:
            print("ERROR " + problem)

        return 1

    print("lucky apple check passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
