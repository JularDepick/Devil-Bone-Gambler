#!/usr/bin/env python3
"""Command guard of the preset prize pools.

The presets are plain command strings, and a string that names an id that does not exist fails quietly in game: the
draw says the player won something and the command errors out. This check parses every preset command, proves its verb
and its argument shape, and matches every id it names against the tables of the running Minecraft version.

The ids come from the game itself: the class literals of the 1.20.1 registry classes for entities, effects, particles,
blocks, enchantments, potions and sounds, and the resources of the client jar for structures and loot tables. Those jars
live in the Gradle cache, so the id half of this check reports SKIP on a machine that has never built the mod; the
syntax half always runs.
"""

from __future__ import annotations

import glob
import json
import os
import pathlib
import re
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "presets" / "presets.example.json"
VANILLA_ITEMS = ROOT / "scripts" / "data" / "vanilla_items.txt"
GRADLE = pathlib.Path(os.environ.get("GRADLE_USER_HOME") or (pathlib.Path.home() / ".gradle"))

CLASS_TABLES = {
    "entity": "net/minecraft/world/entity/EntityType.class",
    "effect": "net/minecraft/world/effect/MobEffects.class",
    "particle": "net/minecraft/core/particles/ParticleTypes.class",
    "block": "net/minecraft/world/level/block/Blocks.class",
    "enchantment": "net/minecraft/world/item/enchantment/Enchantments.class",
    "potion": "net/minecraft/world/item/alchemy/Potions.class",
    "sound": "net/minecraft/sounds/SoundEvents.class",
}

ROOTS = {"give", "summon", "effect", "particle", "playsound", "place", "setblock", "fill", "loot", "tp",
         "spreadplayers", "weather", "time", "experience"}
# The presets position and retarget a command; anything that branches or rewrites the source is not something a
# prize entry may do, so a new subcommand has to be admitted here on purpose.
FORBIDDEN = {"if", "unless", "store", "rotate", "facing", "align", "anchored", "on", "summon", "data"}
HEIGHTMAPS = {"world_surface", "motion_blocking", "motion_blocking_no_leaves", "ocean_floor"}
COORDINATE = re.compile(r"^(?:[~^]-?\d*(?:\.\d+)?|[~^]?%\(-?\d+,-?\d+\)|-?\d+(?:\.\d+)?|@[saper]|~)$")
IDENT = r"[a-z0-9_.\-]+"

# One row per verb the presets may carry. Every coordinate is one token, so a shape is the verb plus the exact
# number of arguments the vanilla parser wants, with the table id named where the verb takes one.
SHAPES = [
    ("give", re.compile(r"^give \$\{player\} minecraft:(?P<item>" + IDENT + r")(?:\{.*\})?(?:\s+%\(\d+,\d+\))?$")),
    ("summon", re.compile(r"^summon minecraft:(?P<entity>" + IDENT + r")(?:\s+\S+){3}(?:\s+\{.*\})?(?:\s+--R\(\d+\))?$")),
    ("effect", re.compile(r"^effect give \$\{player\} minecraft:(?P<effect>" + IDENT + r")\s+\S+\s+\d+$")),
    ("particle", re.compile(r"^particle minecraft:(?P<particle>" + IDENT + r")(?:\s+\S+){8}$")),
    ("playsound", re.compile(r"^playsound minecraft:(?P<sound>" + IDENT + r")\s+master\s+\$\{player\}(?:\s+\S+){5}$")),
    ("place", re.compile(r"^place structure minecraft:(?P<structure>" + IDENT + r")$")),
    ("setblock", re.compile(r"^setblock(?:\s+\S+){3}\s+(?:minecraft:)?(?P<block>" + IDENT + r")(?:\[[^\]]*\])?$")),
    ("fill", re.compile(r"^fill(?:\s+\S+){6}\s+(?:minecraft:)?(?P<block>" + IDENT + r")(?:\[[^\]]*\])?"
                        r"(?:\s+(?:destroy|keep|replace|hollow|outline)(?:\s+\S+)?)?$")),
    ("loot-give", re.compile(r"^loot give \$\{player\} loot minecraft:(?P<loot>" + IDENT + r"(?:/" + IDENT + r")*)$")),
    ("loot-spawn", re.compile(r"^loot spawn(?:\s+\S+){3}\s+loot\s+minecraft:(?P<loot>" + IDENT + r"(?:/" + IDENT + r")*)$")),
    ("tp", re.compile(r"^tp @s(?:\s+\S+){3}$")),
    ("spreadplayers", re.compile(r"^spreadplayers(?:\s+\S+){5}\s+@s$")),
    ("weather", re.compile(r"^weather (?:clear|rain|thunder)\s+\S+$")),
    ("time", re.compile(r"^time (?:set|add)\s+\S+$")),
    ("experience", re.compile(r"^experience add \$\{player\}\s+\S+\s+points$")),
]


def fail(problems, message):
    problems.append(message)


def load_commands():
    data = json.loads(SOURCE.read_text(encoding="utf-8"))
    seen = []

    for pool in data.get("pools") or []:
        for region in pool.get("regions") or []:
            for entry in region.get("entries") or []:
                seen.append((pool.get("id"), region.get("tier"), entry.get("nameZh"), entry.get("command") or ""))

    return seen


def strip_chain(command):
    """Drops the execute chain in front of the verb and returns the verb part plus the chain tokens."""
    tokens = command.split(" ")

    if not tokens or tokens[0] != "execute" or "run" not in tokens:
        return command, []

    cut = tokens.index("run")
    return " ".join(tokens[cut + 1:]), tokens[1:cut]


def check_chain(problems, name, chain):
    for token in chain:
        if token in FORBIDDEN:
            fail(problems, "%s: execute subcommand %s is not allowed" % (name, token))

    if "over" in chain:
        index = chain.index("over")
        heightmap = chain[index + 1] if index + 1 < len(chain) else ""

        if heightmap not in HEIGHTMAPS:
            fail(problems, "%s: unknown heightmap %s" % (name, heightmap or "(none)"))


def check_coordinates(problems, name, body):
    for token in body.split(" "):
        if token.startswith(("~", "^")) and token not in ("~", "^"):
            if not COORDINATE.match(token):
                fail(problems, "%s: malformed coordinate %s" % (name, token))


def check_shape(problems, name, body):
    matched = None

    for label, pattern in SHAPES:
        found = pattern.match(body)

        if found is not None:
            matched = (label, found)
            break

    if matched is None:
        fail(problems, "%s: no known shape for: %s" % (name, body))
        return None

    return matched


def load_vanilla_items():
    table = set()

    for line in VANILLA_ITEMS.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#"):
            table.add(line.partition(" ")[0])

    return table


def game_tables():
    """Reads the registries of the game the mod is built against, or returns None when the jars are not here."""
    rename = sorted(glob.glob(str(GRADLE / "caches" / "forge_gradle" / "mcp_repo" / "de" / "oceanlabs" / "mcp"
                                    / "mcp_config" / "1.20.1-*" / "joined" / "rename" / "output.jar")))
    client = GRADLE / "caches" / "forge_gradle" / "minecraft_repo" / "versions" / "1.20.1" / "client.jar"

    if not rename or not client.is_file():
        return None

    tables = {}

    with zipfile.ZipFile(rename[0]) as jar:
        for name, path in CLASS_TABLES.items():
            try:
                data = jar.read(path)
            except KeyError:
                return None

            tables[name] = {match.decode() for match in re.findall(rb"[a-z0-9_.]{2,60}", data)}

    with zipfile.ZipFile(client) as jar:
        names = jar.namelist()
        tables["structure"] = {item.rsplit("/", 1)[-1][:-5] for item in names
                               if item.startswith("data/minecraft/worldgen/structure/") and item.endswith(".json")}
        tables["loot"] = {item[len("data/minecraft/loot_tables/"):-5] for item in names
                          if item.startswith("data/minecraft/loot_tables/") and item.endswith(".json")}

    return tables


def main():
    problems = []
    commands = load_commands()
    items = load_vanilla_items()
    tables = game_tables()

    if not commands:
        print("FAIL no preset commands found")
        return 1

    checked = 0
    verbs = {}

    for pool, tier, name, command in commands:
        label = "%s/%s/%s" % (pool, tier, name)

        for line in command.split("\n"):
            body, chain = strip_chain(line.strip())
            checked += 1
            verbs[body.split(" ")[0]] = verbs.get(body.split(" ")[0], 0) + 1
            check_chain(problems, label, chain)
            check_coordinates(problems, label, body)
            matched = check_shape(problems, label, body)

            if matched is None:
                continue

            kind, found = matched

            if kind == "give" and found.group("item") not in items:
                fail(problems, "%s: unknown item %s" % (label, found.group("item")))

            if kind == "summon" and tables is not None:
                if found.group("entity") not in tables["entity"]:
                    fail(problems, "%s: unknown entity %s" % (label, found.group("entity")))

            if kind == "effect" and tables is not None:
                if found.group("effect") not in tables["effect"]:
                    fail(problems, "%s: unknown effect %s" % (label, found.group("effect")))

            if kind == "particle" and tables is not None:
                if found.group("particle") not in tables["particle"]:
                    fail(problems, "%s: unknown particle %s" % (label, found.group("particle")))

            if kind == "playsound" and tables is not None:
                if found.group("sound") not in tables["sound"]:
                    fail(problems, "%s: unknown sound %s" % (label, found.group("sound")))

            if kind == "place" and tables is not None:
                if found.group("structure") not in tables["structure"]:
                    fail(problems, "%s: unknown structure %s" % (label, found.group("structure")))

            if kind == "setblock" and tables is not None:
                if found.group("block") not in tables["block"]:
                    fail(problems, "%s: unknown block %s" % (label, found.group("block")))

            if kind == "fill" and tables is not None:
                if found.group("block") not in tables["block"]:
                    fail(problems, "%s: unknown block %s" % (label, found.group("block")))

            if kind in ("loot-give", "loot-spawn") and tables is not None:
                if found.group("loot") not in tables["loot"]:
                    fail(problems, "%s: unknown loot table %s" % (label, found.group("loot")))

    for verb in sorted(verbs):
        if verb not in ROOTS:
            fail(problems, "unknown verb %s" % verb)

    if problems:
        for problem in problems[:40]:
            print("FAIL " + problem)

        if len(problems) > 40:
            print("FAIL ... and %d more" % (len(problems) - 40))

        return 1

    print("PASS verbs: %s" % ", ".join(sorted(verbs)))
    print("PASS commands: %d lines over %d entries" % (checked, len(commands)))

    if tables is None:
        print("SKIP ids: the 1.20.1 jars are not in the Gradle cache")
    else:
        print("PASS ids: entity, effect, particle, block, sound, structure, loot, item")

    return 0


if __name__ == "__main__":
    sys.exit(main())
