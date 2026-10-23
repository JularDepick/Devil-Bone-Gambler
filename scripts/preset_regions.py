"""
Region builders for the preset pools.

One entry stands for one idea. There is no amount axis and no intensity word: a region never repeats an item or a
particle with only a quantity changed. Regions differ by the slice of every content table they draw from, so the
regions of one pool do not all show the same list.

The tables and the small helpers live in preset_content; this module only decides the composition of one region and
keeps every region at the same number of entries.
"""

import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

import preset_content as content

PLAYER = content.PLAYER
TIER_ENTRIES = 260
SURVIVAL_SCALE = content.SURVIVAL_SCALE
CREATIVE_SCALE = content.CREATIVE_SCALE

# The rarity steps the config validator accepts: a plain item is common, a boss is rare.
WEIGHT_COMMON = 120
WEIGHT_UNCOMMON = 60
WEIGHT_RARE = 25
WEIGHT_VERY_RARE = 10
WEIGHT_LEGEND = 3

# A potion that hurts the drinker or the target: it belongs on the black side of the pool.
HARMFUL_POTIONS = {"poison", "harming", "slowness", "weakness"}

# A first or second region should not open with a boss or a raid monster. The bosses stay out of every preset region
# until the late regions; the raid and nether monsters wait for the third.
BOSS_MOBS = {"wither", "ender_dragon", "elder_guardian"}
LATE_MOBS = {"ravager", "evoker", "shulker", "enderman", "wither_skeleton", "blaze", "magma_cube", "piglin",
             "hoglin", "zoglin", "guardian", "vex", "witch", "pillager", "vindicator", "zombified_piglin",
             "strider"}

# A low region opens common chests; the end city and the ancient city are late rewards.
LATE_LOOT = {"bastion_treasure", "end_city_treasure", "ancient_city"}

ENCHANT_WORDS = {
    "sharpness": ("锋利", "Sharpness"),
    "efficiency": ("效率", "Efficiency"),
    "protection": ("保护", "Protection"),
    "unbreaking": ("耐久", "Unbreaking"),
    "power": ("力量", "Power"),
    "fortune": ("时运", "Fortune"),
}


def _named(pool, tier, body_zh, body_en):
    theme_zh, theme_en = content.THEMES[pool][tier]
    return theme_zh + "-" + body_zh, theme_en + " " + body_en


def _rotate(rows, start, count):
    """Takes count different rows from the table, wrapping around, so a region never repeats one."""
    if count > len(rows):
        raise ValueError("asked for %d rows from a table of %d" % (count, len(rows)))

    return [rows[(start + index) % len(rows)] for index in range(count)]


def _stride(rows, start, count, step):
    """Takes count rows with a stride, so a slice of a table ordered by kind mixes kinds instead of taking a block."""
    if count > len(rows):
        raise ValueError("asked for %d rows from a table of %d" % (count, len(rows)))

    return [rows[(start + index * step) % len(rows)] for index in range(count)]


# Items whose value does not grow with the tier. A draw hands out one to three of these whatever region it came from,
# so a late region never pays twenty nether stars for one roll.
PRECIOUS_ITEMS = {
    "ancient_debris", "beacon", "blaze_rod", "conduit", "diamond", "diamond_block", "dragon_breath",
    "elytra", "emerald", "emerald_block", "enchanted_golden_apple", "experience_bottle", "ghast_tear",
    "golden_apple", "heart_of_the_sea", "nautilus_shell", "nether_star", "netherite_block", "netherite_ingot",
    "netherite_scrap", "rabbit_foot", "respawn_anchor", "shulker_box", "shulker_shell", "sponge",
    "totem_of_undying", "trident", "wither_skeleton_skull",
}

# A tool that is not a prize in bulk: maps, clocks and the like stay at one or two.
UTILITY_ITEMS = {
    "clock", "compass", "lead", "map", "minecart", "name_tag", "saddle",
}


def _value_class(item_id):
    if content.stack_size(item_id) <= 1:
        return "single"

    if item_id in PRECIOUS_ITEMS:
        return "precious"

    if item_id in UTILITY_ITEMS:
        return "utility"

    return "standard"


def _item_weight(item_id):
    """Common supplies are the bread and butter of a draw; a rare piece is a fifth as likely."""
    value = _value_class(item_id)

    if value == "precious":
        return WEIGHT_RARE

    if value == "utility":
        return WEIGHT_UNCOMMON

    return WEIGHT_COMMON


def _portion(item_id, tier):
    """The count range of one item, by its value class rather than by the tier alone."""
    value = _value_class(item_id)

    if value == "single":
        return 1, 1

    if value == "precious":
        return 1, min(3, 1 + tier // 2)

    if value == "utility":
        return 1, 2

    size = content.stack_size(item_id)
    low = min(size, 2 + tier // 2)
    high = min(size, 5 + tier)
    return low, max(low + 1, high)


def _rain_portion(item_id):
    value = _value_class(item_id)

    if value == "precious":
        return 1, 2

    if value == "utility":
        return 1, 2

    return 1, 3


def _rain_rows(tier, items):
    rows = []

    for item_id, zh, en in items:
        low, high = _rain_portion(item_id)
        name_zh, name_en = _named("creative", tier, zh + "雨", en + " rain")
        command = ("execute at " + PLAYER + " run summon minecraft:item ~ ~4 ~ {Item:{id:\"minecraft:" + item_id
                   + "\",Count:%(" + str(low) + "," + str(high) + ")b}}")
        rows.append(_row("creative", tier, name_zh, name_en, _item_weight(item_id), "red", command))

    return rows


def _row(pool, tier, name_zh, name_en, weight, colour, command):
    return (pool, tier, name_zh, name_en, name_zh, name_en, weight, colour, False, command)


def _item_rows(tier, items):
    rows = []

    for item_id, zh, en in items:
        low, high = _portion(item_id, tier)
        name_zh, name_en = _named("survival", tier, zh, en)
        command = "give " + PLAYER + " minecraft:" + item_id

        if low > 1 or high > 1:
            command += " %(" + str(low) + "," + str(high) + ")"

        rows.append(_row("survival", tier, name_zh, name_en, _item_weight(item_id), "red", command))

    return rows


def _effect_rows(tier, effects):
    rows = []
    amplifier = min(3, (tier - 1) // 2)

    for effect_id, zh, en, harmful in effects:
        name_zh, name_en = _named("survival", tier, zh + " " + str(amplifier + 1) + " 级",
                                  en + " " + str(amplifier + 1))
        command = ("effect give " + PLAYER + " minecraft:" + effect_id + " %(30,60) " + str(amplifier))
        colour = "black" if harmful else "red"
        weight = WEIGHT_RARE if harmful else WEIGHT_UNCOMMON
        rows.append(_row("survival", tier, name_zh, name_en, weight, colour, command))

    return rows


def _mob_rows(tier, mobs, weight=WEIGHT_UNCOMMON):
    rows = []
    count = 1 + tier // 3

    for mob_id, zh, en in mobs:
        name_zh, name_en = _named("survival", tier, str(count) + " 只 " + zh, str(count) + " " + en)
        line = "execute at " + PLAYER + " run summon minecraft:" + mob_id + " ~ ~ ~"
        colour = "black" if mob_id in content.HOSTILE_IDS else "red"
        entry_weight = WEIGHT_RARE if colour == "black" else weight
        rows.append(_row("survival", tier, name_zh, name_en, entry_weight, colour, "\n".join([line] * count)))

    return rows


def _book_rows(tier, pool, books):
    rows = []

    for enchantment, zh, en, top in books:
        name_zh, name_en = _named(pool, tier, zh + "附魔书", en + " book")
        command = ("give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:"
                   + enchantment + "\",lvl:%(1," + str(top) + ")}]}")
        rows.append(_row(pool, tier, name_zh, name_en, WEIGHT_VERY_RARE, "red", command))

    return rows


def _chest_rain_rows(tier, tables):
    rows = []

    for table_id, zh, en in tables:
        name_zh, name_en = _named("creative", tier, zh, en)
        # A chest is not an entity, so the contents are spawned straight out of the table they belong to. That is the
        # same pile of loot a player would find inside the chest, and it falls to the ground instead of hanging in air.
        command = ("execute at " + PLAYER + " run loot spawn ~ ~4 ~ loot minecraft:chests/" + table_id)
        rows.append(_row("creative", tier, name_zh, name_en, WEIGHT_RARE, "red", command))

    return rows


def _potion_rows(tier, potions):
    rows = []

    for potion_id, zh, en in potions:
        name_zh, name_en = _named("survival", tier, zh, en)
        command = "give " + PLAYER + " minecraft:potion{Potion:\"minecraft:" + potion_id + "\"}"
        colour = "black" if potion_id in HARMFUL_POTIONS else "red"
        weight = WEIGHT_RARE if colour == "black" else WEIGHT_UNCOMMON
        rows.append(_row("survival", tier, name_zh, name_en, weight, colour, command))

    return rows


def _arrow_rows(tier, arrows):
    rows = []

    for potion_id, zh, en in arrows:
        name_zh, name_en = _named("survival", tier, zh, en)
        command = ("give " + PLAYER + " minecraft:tipped_arrow{Potion:\"minecraft:" + potion_id
                   + "\"} %(4,10)")
        colour = "black" if potion_id in HARMFUL_POTIONS else "red"
        weight = WEIGHT_RARE if colour == "black" else WEIGHT_UNCOMMON
        rows.append(_row("survival", tier, name_zh, name_en, weight, colour, command))

    return rows


def _potion_rain_rows(tier, potions):
    rows = []

    for potion_id, zh, en in potions:
        name_zh, name_en = _named("creative", tier, zh + "雨", en + " rain")
        command = ("execute at " + PLAYER + " run summon minecraft:item ~ ~4 ~ {Item:{id:\"minecraft:potion\","
                   "Count:1b,tag:{Potion:\"minecraft:" + potion_id + "\"}}}")
        colour = "black" if potion_id in HARMFUL_POTIONS else "red"
        weight = WEIGHT_RARE if colour == "black" else WEIGHT_UNCOMMON
        rows.append(_row("creative", tier, name_zh, name_en, weight, colour, command))

    return rows


def _loot_rows(tier, tables):
    rows = []

    for table_id, zh, en in tables:
        name_zh, name_en = _named("survival", tier, zh, en)
        command = "loot give " + PLAYER + " loot minecraft:chests/" + table_id
        rows.append(_row("survival", tier, name_zh, name_en, WEIGHT_RARE, "red", command))

    return rows


def _gear_rows(tier, plan):
    rows = []
    levels = (1 + tier // 2, 2 + tier // 2)

    for item_id, enchantment in plan:
        entry = next(item for item in content.ITEMS if item[0] == item_id)
        word_zh, word_en = ENCHANT_WORDS[enchantment]
        name_zh, name_en = _named("survival", tier,
                                  word_zh + " " + entry[1] + " " + str(levels[0]) + " 到 " + str(levels[1]) + " 级",
                                  word_en + " " + entry[2] + " " + str(levels[0]) + " to " + str(levels[1]))
        command = ("give " + PLAYER + " minecraft:" + item_id + "{Enchantments:[{id:\"minecraft:" + enchantment
                   + "\",lvl:%(" + str(levels[0]) + "," + str(levels[1]) + ")}]}")
        rows.append(_row("survival", tier, name_zh, name_en, WEIGHT_VERY_RARE, "red", command))

    return rows


def _xp_row(tier):
    name_zh, name_en = _named("survival", tier, "一段经验", "A share of experience")
    command = "experience add " + PLAYER + " %(" + str(20 * tier) + "," + str(40 * tier) + ") points"
    return _row("survival", tier, name_zh, name_en, WEIGHT_RARE, "red", command)


def _mishap_rows(tier, mishaps):
    rows = []

    for zh, en, command in mishaps:
        name_zh, name_en = _named("survival", tier, zh, en)
        rows.append(_row("survival", tier, name_zh, name_en, WEIGHT_VERY_RARE, "black", command))

    return rows


def survival_tier(tier):
    """One survival region: a hundred and two items, thirty effects, thirty creatures, thirteen chests, six pieces of
    gear, sixteen potions, fifteen arrows, one experience share, thirty mishaps and seventeen books, all different."""
    items = content.ITEMS

    if tier <= 2:
        items = [item for item in items
                 if item[0] not in PRECIOUS_ITEMS
                 and not item[0].startswith("diamond_")
                 and not item[0].startswith("netherite_")]
    mobs = content.MOBS

    if tier <= 2:
        mobs = [mob for mob in mobs if mob[0] not in BOSS_MOBS and mob[0] not in LATE_MOBS]
    elif tier <= 4:
        mobs = [mob for mob in mobs if mob[0] not in BOSS_MOBS]

    loot = [table for table in content.LOOT if tier > 2 or table[0] not in LATE_LOOT]
    effects = content.EFFECTS if tier > 3 else [effect for effect in content.EFFECTS if effect[0] != "wither"]
    loot_count = 13 if tier > 2 else 10
    item_count = 115 - loot_count
    rows = []
    rows += _item_rows(tier, _rotate(items, (tier - 1) * 25, item_count))
    rows += _effect_rows(tier, _rotate(effects, (tier - 1) * 2, 30))
    rows += _mob_rows(tier, _rotate(mobs, (tier - 1) * 4, 30))
    rows += _loot_rows(tier, _rotate(loot, (tier - 1) * 2, loot_count))
    rows += _gear_rows(tier, content.GEAR_PLAN[:6])
    rows += _potion_rows(tier, _rotate(content.POTIONS, tier - 1, 16))
    rows += _arrow_rows(tier, _rotate(content.ARROWS, tier - 1, 15))
    rows.append(_xp_row(tier))
    rows += _mishap_rows(tier, _rotate(content.SURVIVAL_MISHAPS, (tier - 1) * 2, 30))
    rows += _book_rows(tier, "survival", _rotate(content.BOOK_ENCHANTS, (tier - 1) * 5, 17))

    if len(rows) != TIER_ENTRIES:
        raise ValueError("survival region %d holds %d entries" % (tier, len(rows)))

    return rows


def survival_pool():
    rows = []

    for tier in range(1, 7):
        for row in survival_tier(tier):
            rows.append(row[:9] + (content.scale_command(row[9], SURVIVAL_SCALE[tier]),))

    return rows





def _idea_rows(tier, ideas, weight, colour):
    rows = []

    for zh, en, command in ideas:
        name_zh, name_en = _named("creative", tier, zh, en)
        rows.append(_row("creative", tier, name_zh, name_en, weight, colour, command))

    return rows


def _creative_mob_rows(tier, mobs):
    rows = []

    for mob_id, zh, en in mobs:
        name_zh, name_en = _named("creative", tier, "一群 " + zh, "A pack of " + en)
        line = "execute at " + PLAYER + " run summon minecraft:" + mob_id + " ~ ~ ~"
        colour = "black" if mob_id in content.HOSTILE_IDS else "red"
        entry_weight = WEIGHT_RARE if colour == "black" else WEIGHT_UNCOMMON
        rows.append(_row("creative", tier, name_zh, name_en, entry_weight, colour, "\n".join([line] * 2)))

    return rows


def _creative_effect_rows(tier, effects):
    rows = []
    amplifier = min(2, tier // 3)

    for effect_id, zh, en, harmful in effects:
        name_zh, name_en = _named("creative", tier, zh + " " + str(amplifier + 1) + " 级",
                                  en + " " + str(amplifier + 1))
        command = "effect give " + PLAYER + " minecraft:" + effect_id + " %(60,120) " + str(amplifier)
        colour = "black" if harmful else "red"
        entry_weight = WEIGHT_RARE if harmful else WEIGHT_UNCOMMON
        rows.append(_row("creative", tier, name_zh, name_en, entry_weight, colour, command))

    return rows


def creative_tier(tier):
    """One creative region: the same verb classes every region, but a different slice of each table, so two regions
    never show the same list."""
    flat = [item for item in content.ITEMS if content.stack_size(item[0]) == 64]
    rows = []
    rows += _rain_rows(tier, _stride(flat, (tier - 1) * 30, 60, 7))
    rows += _potion_rain_rows(tier, _rotate(content.POTIONS, (tier - 1) * 2, 10))
    rows += _chest_rain_rows(tier, _rotate(content.LOOT, (tier - 1) * 2, 12))
    rows += _idea_rows(tier, _rotate(content.PARTICLE_IDEAS, (tier - 1) * 2, 30), WEIGHT_UNCOMMON, "red")
    rows += _idea_rows(tier, _rotate(content.SOUND_IDEAS, (tier - 1) * 2, 20), WEIGHT_UNCOMMON, "red")
    # A teleport, a weather change and a clock jump are intrusive, so they sit on the rarest step and stay a small
    # slice of the red pool instead of arriving every few draws.
    rows += _idea_rows(tier, _rotate(content.TELEPORT_IDEAS, (tier - 1) * 2, 16), WEIGHT_VERY_RARE, "red")
    rows += _idea_rows(tier, _rotate(content.STRUCTURE_IDEAS, (tier - 1) * 2, 20), WEIGHT_RARE, "red")
    rows += _idea_rows(tier, _rotate(content.TRAP_IDEAS, (tier - 1) * 2, 24), WEIGHT_RARE, "black")
    rows += _idea_rows(tier, _rotate(content.MOUNT_IDEAS, (tier - 1) * 2, 10), WEIGHT_RARE, "red")
    rows += _idea_rows(tier, _rotate(content.FIREWORK_IDEAS, (tier - 1) * 2, 10), WEIGHT_RARE, "red")
    rows += _book_rows(tier, "creative", _rotate(content.BOOK_ENCHANTS, (tier - 1) * 2, 20))
    rows += _idea_rows(tier, _rotate(content.WEATHER_IDEAS, tier - 1, 6), WEIGHT_VERY_RARE, "red")
    rows += _idea_rows(tier, _rotate(content.TIME_IDEAS, tier - 1, 6), WEIGHT_VERY_RARE, "red")
    rows += _creative_mob_rows(tier, _rotate(content.MOBS, (tier - 1) * 5, 8))
    rows += _creative_effect_rows(tier, _rotate(content.EFFECTS, (tier - 1) * 4, 8))

    if len(rows) != TIER_ENTRIES:
        raise ValueError("creative region %d holds %d entries" % (tier, len(rows)))

    return rows


def creative_pool():
    rows = []

    for tier in range(1, 7):
        for row in creative_tier(tier):
            rows.append(row[:9] + (content.scale_command(row[9], CREATIVE_SCALE[tier]),))

    return rows
