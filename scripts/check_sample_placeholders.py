#!/usr/bin/env python3
"""Sample space placeholder guard.

The ^(TYPE)[namespace] placeholders:

  1. discover their spaces from the live registries on every world load, never from a hand written table in source,
  2. persist those spaces to their own config file,
  3. sample ids only, never a live entity or block of the world,
  4. read the point, the overflow, the luck value and the colour of the draw,
  5. are documented in the admin manual and its creator copy, including /dbgr run,
  6. leave the older ${random:...} tokens untouched.

Exit code 0 means the check passed.
"""

from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler"
SPACE = JAVA / "config" / "SampleSpaceConfig.java"
SAMPLER = JAVA / "commands" / "SamplePlaceholders.java"
CONTEXT = JAVA / "util" / "SampleContext.java"
RUNNER = JAVA / "commands" / "CommandRunner.java"
COMMANDS = JAVA / "commands" / "ModCommands.java"
ENGINE = JAVA / "draw" / "DrawEngine.java"
CONFIG = JAVA / "config" / "ServerConfigManager.java"
CONSTANTS = JAVA / "util" / "ModConstants.java"
MANUALS = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "manuals"
LANG = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "lang"

TYPES = ("ITEM", "BLOCK", "ENTITY", "MOB", "EFFECT", "ENCHANTMENT", "POTION", "SOUND", "PARTICLE",
         "PAINTING", "ATTRIBUTE", "FLUID", "BIOME", "STRUCTURE", "DAMAGE_TYPE")
DERIVED = ("BUFF", "DEBUFF", "NEUBUFF", "BUFF_ENCHANTMENT", "CURSE", "ENCHANTED_BOOK")

problems = []


def check(name, condition, message=None):
    if not condition:
        problems.append(message or name)


def text(path):
    return path.read_text(encoding="utf-8")


def main() -> int:
    for path in (SPACE, SAMPLER, CONTEXT):
        check(str(path.name), path.is_file(), "missing file: " + str(path))

    space = text(SPACE)
    sampler = text(SAMPLER)
    context = text(CONTEXT)
    runner = text(RUNNER)
    commands = text(COMMANDS)
    engine = text(ENGINE)
    config = text(CONFIG)
    constants = text(CONSTANTS)

    check("space file constant", "SAMPLE_SPACE_FILE = \"sample_spaces.json\"" in constants)
    check("registry scan", all(marker in space for marker in (
        "ForgeRegistries.ITEMS", "ForgeRegistries.BLOCKS", "ForgeRegistries.ENTITY_TYPES",
        "ForgeRegistries.MOB_EFFECTS", "ForgeRegistries.ENCHANTMENTS", "ForgeRegistries.POTIONS",
        "ForgeRegistries.SOUND_EVENTS", "ForgeRegistries.PARTICLE_TYPES",
        "ForgeRegistries.PAINTING_VARIANTS", "ForgeRegistries.ATTRIBUTES", "ForgeRegistries.FLUIDS")),
        "the spaces are not discovered from the registries")
    check("world registries", all(marker in space for marker in (
        "Registries.BIOME", "Registries.STRUCTURE", "Registries.DAMAGE_TYPE")),
        "the data pack registries are not scanned")
    check("no hand written id table", "minecraft:stone\", \"minecraft:" not in space,
          "the space scan seems to carry a hand written id table")
    check("effect kinds", "MobEffectCategory.BENEFICIAL" in space and "MobEffectCategory.HARMFUL" in space
          and "KIND_NEUBUFF" in space, "the effect kinds are not classified")
    check("enchantment kinds", "isCurse()" in space, "the enchantments are not split into buffs and curses")

    check("space persisted", "readSampleSpaces" in config and "writeSampleSpaces" in config
          and "refreshSampleSpaces(server)" in config, "the spaces are not persisted or refreshed")
    check("spaces on world load", "sampleSpaces = readSampleSpaces" in config,
          "the spaces are not read on world load")

    check("token pattern", "TOKEN = Pattern.compile" in sampler and "A-Za-z0-9_" in sampler,
          "the placeholder pattern is missing")
    check("context attributes", all(marker in sampler for marker in (
        "context.point()", "context.overflow()", "context.luck()", "red()")),
        "the sampler ignores point, overflow, luck or colour")
    check("colour fallback", "wanted.isEmpty()" in sampler, "the colour has no fallback to the whole space")
    check("namespace narrowing", "id.startsWith(prefix)" in sampler, "the namespace filter is missing")
    check("ids only", "spawn" not in sampler and "addFreshEntity" not in sampler
          and "setBlock" not in sampler, "the sampler touches the world instead of only sampling ids")
    check("enchanted book", "StoredEnchantments" in sampler, "the enchanted book is not constructed")
    check("context fields", all(marker in context for marker in ("point", "overflow", "luck", "red", "seed")),
          "the sample context misses a roll attribute")

    check("runner expansion", "SamplePlaceholders.expand(command, context, sampledNames)" in runner,
          "the prize commands do not expand the sample placeholders")
    check("runner validation", "SamplePlaceholders.findProblem(line)" in runner
          and "SamplePlaceholders.sampleForm(" in runner, "validation does not know the sample placeholders")
    check("draw context", "new SampleContext(pending.roll.point(), pending.overflow, pending.luck,"
          in engine, "the draw does not pass its roll attributes")

    names = (JAVA / "commands" / "SampleNames.java").read_text(encoding="utf-8")
    notify = (JAVA / "draw" / "NotifySender.java").read_text(encoding="utf-8")
    check("sampled display names", "SampleNames.displayFor(type, sampled)" in sampler,
          "the sampler does not report the display name of a sample")
    check("sampled name sources", "ForgeRegistries.ITEMS" in names and "ForgeRegistries.ENTITY_TYPES" in names
          and "ForgeRegistries.MOB_EFFECTS" in names and "ForgeRegistries.ENCHANTMENTS" in names,
          "the display names are not read from the registries")
    check("sampled names passed", "sampledNames" in runner and "sampledNames" in engine,
          "the sampled names are not passed from the draw to the result notice")
    check("sampled names appended", 'String.join(" ", names)' in notify,
          "the result notice does not append the sampled names")

    check("chat entry", "RUN_LITERAL = \"run\"" in commands and "greedyString()" in commands
          and "SampleContext.DEFAULT" in commands, "the /dbgr run entry is missing")

    legacy = (JAVA / "util" / "CommandPlaceholders.java").read_text(encoding="utf-8")
    check("legacy token unchanged", "RANDOM = Pattern.compile" in legacy,
          "the old random token was removed")

    for name, title, run, registry in (
            ("admin_manual_zh_cn.md", "模组指令占位符体系", "/dbgr run", "只抽注册 ID"),
            ("admin_manual_en_us.md", "Mod command placeholder system", "/dbgr run", "registry id"),
            ("creator_manual_zh_cn.md", "模组指令占位符体系", "/dbgr run", "只抽注册 ID"),
            ("creator_manual_en_us.md", "Mod command placeholder system", "/dbgr run", "registry id")):
        body = text(MANUALS / name)
        check("manual section " + name, title in body, "missing placeholder chapter in " + name)
        check("manual run " + name, run in body, "the chapter does not mention /dbgr run")
        check("manual ids " + name, registry in body, "the chapter does not promise id only sampling")

        for type_name in TYPES + DERIVED:
            check("manual type " + type_name + " in " + name, type_name in body,
                  "the chapter misses the type " + type_name)

    for name in ("zh_cn.json", "en_us.json"):
        body = text(LANG / name)
        check("lang command.ran " + name, "gui.devil_bone_gambler.command.ran" in body,
              "the run feedback key is missing in " + name)

    if problems:
        for problem in problems:
            print("ERROR " + problem)
        return 1

    print("sample space placeholder check passed: " + str(len(TYPES)) + " types, "
          + str(len(DERIVED)) + " focused types")
    return 0


if __name__ == "__main__":
    sys.exit(main())
