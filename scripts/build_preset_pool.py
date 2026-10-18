#!/usr/bin/env python3
"""Builds DefaultPrizePoolData.java, the six-region preset prize pool.

Run from the repository root:
    python scripts/build_preset_pool.py

Every entry carries its Chinese and English name and notify text as plain strings, because the preset
texts are frozen into the server config file the first time it is generated. Names must stay unique
across all regions: the config validator rejects duplicate entry ids.
"""

import pathlib
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")

ITEMS = {
    "bread": ("面包", "Bread"),
    "baked_potato": ("烤马铃薯", "Baked Potato"),
    "apple": ("苹果", "Apple"),
    "carrot": ("胡萝卜", "Carrot"),
    "wheat": ("小麦", "Wheat"),
    "torch": ("火把", "Torch"),
    "cobblestone": ("圆石", "Cobblestone"),
    "oak_planks": ("橡木木板", "Oak Planks"),
    "stick": ("木棍", "Stick"),
    "coal": ("煤炭", "Coal"),
    "charcoal": ("木炭", "Charcoal"),
    "leather_helmet": ("皮革帽子", "Leather Cap"),
    "leather_boots": ("皮革靴子", "Leather Boots"),
    "stone_sword": ("石剑", "Stone Sword"),
    "stone_pickaxe": ("石镐", "Stone Pickaxe"),
    "stone_axe": ("石斧", "Stone Axe"),
    "arrow": ("箭", "Arrow"),
    "string": ("线", "String"),
    "flint": ("燧石", "Flint"),
    "oak_log": ("橡木原木", "Oak Log"),
    "gravel": ("沙砾", "Gravel"),
    "sand": ("沙子", "Sand"),
    "dirt": ("泥土", "Dirt"),
    "clay_ball": ("黏土球", "Clay Ball"),
    "bone": ("骨头", "Bone"),
    "egg": ("鸡蛋", "Egg"),
    "sugar_cane": ("甘蔗", "Sugar Cane"),
    "potato": ("马铃薯", "Potato"),
    "beetroot": ("甜菜根", "Beetroot"),
    "cooked_beef": ("牛排", "Steak"),
    "iron_ingot": ("铁锭", "Iron Ingot"),
    "iron_sword": ("铁剑", "Iron Sword"),
    "iron_pickaxe": ("铁镐", "Iron Pickaxe"),
    "iron_axe": ("铁斧", "Iron Axe"),
    "iron_chestplate": ("铁胸甲", "Iron Chestplate"),
    "iron_helmet": ("铁头盔", "Iron Helmet"),
    "shield": ("盾牌", "Shield"),
    "iron_block": ("铁块", "Block of Iron"),
    "lapis_lazuli": ("青金石", "Lapis Lazuli"),
    "redstone": ("红石", "Redstone Dust"),
    "gold_nugget": ("金粒", "Gold Nugget"),
    "copper_ingot": ("铜锭", "Copper Ingot"),
    "coal_block": ("煤炭块", "Block of Coal"),
    "bucket": ("桶", "Bucket"),
    "water_bucket": ("水桶", "Water Bucket"),
    "milk_bucket": ("奶桶", "Milk Bucket"),
    "flint_and_steel": ("打火石", "Flint and Steel"),
    "shears": ("剪刀", "Shears"),
    "fishing_rod": ("钓鱼竿", "Fishing Rod"),
    "bow": ("弓", "Bow"),
    "crossbow": ("弩", "Crossbow"),
    "tnt": ("TNT", "TNT"),
    "compass": ("指南针", "Compass"),
    "clock": ("时钟", "Clock"),
    "name_tag": ("命名牌", "Name Tag"),
    "saddle": ("鞍", "Saddle"),
    "lead": ("拴绳", "Lead"),
    "hay_block": ("干草捆", "Hay Bale"),
    "cooked_porkchop": ("熟猪排", "Cooked Porkchop"),
    "pumpkin_pie": ("南瓜派", "Pumpkin Pie"),
    "cookie": ("曲奇", "Cookie"),
    "iron_boots": ("铁靴子", "Iron Boots"),
    "iron_leggings": ("铁护腿", "Iron Leggings"),
    "gold_ingot": ("金锭", "Gold Ingot"),
    "gold_block": ("金块", "Block of Gold"),
    "emerald": ("绿宝石", "Emerald"),
    "emerald_block": ("绿宝石块", "Block of Emerald"),
    "diamond": ("钻石", "Diamond"),
    "lapis_block": ("青金石块", "Block of Lapis Lazuli"),
    "amethyst_shard": ("紫水晶碎片", "Amethyst Shard"),
    "experience_bottle": ("附魔之瓶", "Bottle o' Enchanting"),
    "book": ("书", "Book"),
    "ender_pearl": ("末影珍珠", "Ender Pearl"),
    "blaze_powder": ("烈焰粉", "Blaze Powder"),
    "ghast_tear": ("恶魂之泪", "Ghast Tear"),
    "glowstone_dust": ("荧石粉", "Glowstone Dust"),
    "redstone_block": ("红石块", "Block of Redstone"),
    "quartz": ("下界石英", "Nether Quartz"),
    "obsidian": ("黑曜石", "Obsidian"),
    "crying_obsidian": ("哭泣的黑曜石", "Crying Obsidian"),
    "soul_sand": ("灵魂沙", "Soul Sand"),
    "nether_wart": ("下界疣", "Nether Wart"),
    "brewing_stand": ("酿造台", "Brewing Stand"),
    "cauldron": ("炼药锅", "Cauldron"),
    "enchanting_table": ("附魔台", "Enchanting Table"),
    "bookshelf": ("书架", "Bookshelf"),
    "anvil": ("铁砧", "Anvil"),
    "grindstone": ("砂轮", "Grindstone"),
    "smithing_table": ("锻造台", "Smithing Table"),
    "glass_bottle": ("玻璃瓶", "Glass Bottle"),
    "glowstone": ("荧石", "Glowstone"),
    "golden_carrot": ("金胡萝卜", "Golden Carrot"),
    "golden_apple": ("金苹果", "Golden Apple"),
    "spectral_arrow": ("光灵箭", "Spectral Arrow"),
    "tipped_arrow": ("药箭", "Tipped Arrow"),
    "diamond_block": ("钻石块", "Block of Diamond"),
    "diamond_sword": ("钻石剑", "Diamond Sword"),
    "diamond_pickaxe": ("钻石镐", "Diamond Pickaxe"),
    "diamond_chestplate": ("钻石胸甲", "Diamond Chestplate"),
    "netherite_scrap": ("下界合金碎片", "Netherite Scrap"),
    "ancient_debris": ("远古残骸", "Ancient Debris"),
    "blaze_rod": ("烈焰棒", "Blaze Rod"),
    "shulker_shell": ("潜影壳", "Shulker Shell"),
    "ender_eye": ("末影之眼", "Eye of Ender"),
    "prismarine_shard": ("海晶碎片", "Prismarine Shard"),
    "sea_lantern": ("海晶灯", "Sea Lantern"),
    "sponge": ("海绵", "Sponge"),
    "heart_of_the_sea": ("海洋之心", "Heart of the Sea"),
    "nautilus_shell": ("鹦鹉螺壳", "Nautilus Shell"),
    "turtle_helmet": ("海龟壳", "Turtle Shell"),
    "phantom_membrane": ("幻翼膜", "Phantom Membrane"),
    "magma_cream": ("岩浆膏", "Magma Cream"),
    "fire_charge": ("火焰弹", "Fire Charge"),
    "wither_skeleton_skull": ("凋灵骷髅头颅", "Wither Skeleton Skull"),
    "dragon_breath": ("龙息", "Dragon's Breath"),
    "chorus_fruit": ("紫颂果", "Chorus Fruit"),
    "popped_chorus_fruit": ("爆裂紫颂果", "Popped Chorus Fruit"),
    "end_stone": ("末地石", "End Stone"),
    "purpur_block": ("紫珀块", "Purpur Block"),
    "end_crystal": ("末影水晶", "End Crystal"),
    "elytra": ("鞘翅", "Elytra"),
    "netherite_ingot": ("下界合金锭", "Netherite Ingot"),
    "netherite_sword": ("下界合金剑", "Netherite Sword"),
    "netherite_chestplate": ("下界合金胸甲", "Netherite Chestplate"),
    "enchanted_golden_apple": ("附魔金苹果", "Enchanted Golden Apple"),
    "totem_of_undying": ("不死图腾", "Totem of Undying"),
    "beacon": ("信标", "Beacon"),
    "nether_star": ("下界之星", "Nether Star"),
    "conduit": ("潮涌核心", "Conduit"),
    "trident": ("三叉戟", "Trident"),
    "dragon_egg": ("龙蛋", "Dragon Egg"),
    "shulker_box": ("潜影盒", "Shulker Box"),
    "ender_chest": ("末影箱", "Ender Chest"),
    "netherite_block": ("下界合金块", "Block of Netherite"),
    "respawn_anchor": ("重生锚", "Respawn Anchor"),
    "lodestone": ("磁石", "Lodestone"),
}
EFFECTS = {
    "hunger": ("饥饿", "Hunger"),
    "weakness": ("虚弱", "Weakness"),
    "blindness": ("失明", "Blindness"),
    "slowness": ("缓慢", "Slowness"),
    "poison": ("中毒", "Poison"),
    "wither": ("凋零", "Wither"),
    "nausea": ("反胃", "Nausea"),
    "darkness": ("黑暗", "Darkness"),
    "levitation": ("飘浮", "Levitation"),
    "mining_fatigue": ("挖掘疲劳", "Mining Fatigue"),
    "instant_damage": ("瞬间伤害", "Instant Damage"),
    "bad_omen": ("不祥之兆", "Bad Omen"),
    "glowing": ("发光", "Glowing"),
    "regeneration": ("生命恢复", "Regeneration"),
    "speed": ("速度", "Speed"),
    "strength": ("力量", "Strength"),
    "resistance": ("抗性提升", "Resistance"),
    "fire_resistance": ("抗火", "Fire Resistance"),
    "water_breathing": ("水下呼吸", "Water Breathing"),
    "night_vision": ("夜视", "Night Vision"),
    "jump_boost": ("跳跃提升", "Jump Boost"),
    "absorption": ("伤害吸收", "Absorption"),
    "haste": ("急迫", "Haste"),
    "luck": ("幸运", "Luck"),
    "health_boost": ("生命提升", "Health Boost"),
    "saturation": ("饱和", "Saturation"),
    "slow_falling": ("缓降", "Slow Falling"),
    "dolphins_grace": ("海豚的恩惠", "Dolphin's Grace"),
    "conduit_power": ("潮涌能量", "Conduit Power"),
}
MOBS = {
    "zombie": ("僵尸", "Zombie"),
    "skeleton": ("骷髅", "Skeleton"),
    "creeper": ("苦力怕", "Creeper"),
    "spider": ("蜘蛛", "Spider"),
    "witch": ("女巫", "Witch"),
    "husk": ("尸壳", "Husk"),
    "stray": ("流浪者", "Stray"),
    "pillager": ("掠夺者", "Pillager"),
    "vindicator": ("卫道士", "Vindicator"),
    "evoker": ("唤魔者", "Evoker"),
    "blaze": ("烈焰人", "Blaze"),
    "ghast": ("恶魂", "Ghast"),
    "magma_cube": ("岩浆怪", "Magma Cube"),
    "wither_skeleton": ("凋灵骷髅", "Wither Skeleton"),
    "enderman": ("末影人", "Enderman"),
    "silverfish": ("蠹虫", "Silverfish"),
    "slime": ("史莱姆", "Slime"),
    "phantom": ("幻翼", "Phantom"),
    "drowned": ("溺尸", "Drowned"),
    "ravager": ("劫掠兽", "Ravager"),
    "wither": ("凋灵", "Wither"),
    "iron_golem": ("铁傀儡", "Iron Golem"),
    "bee": ("蜜蜂", "Bee"),
    "axolotl": ("美西螈", "Axolotl"),
    "goat": ("山羊", "Goat"),
    "allay": ("悦灵", "Allay"),
    "vex": ("恼鬼", "Vex"),
    "guardian": ("守卫者", "Guardian"),
    "elder_guardian": ("远古守卫者", "Elder Guardian"),
    "ender_dragon": ("末影龙", "Ender Dragon"),
}
LOOT = {
    "simple_dungeon": "地牢宝箱",
    "abandoned_mineshaft": "废弃矿井宝箱",
    "stronghold_corridor": "要塞走廊宝箱",
    "desert_pyramid": "沙漠神殿宝箱",
    "jungle_temple": "丛林神庙宝箱",
    "woodland_mansion": "林地府邸宝箱",
    "nether_bridge": "下界要塞宝箱",
    "bastion_treasure": "堡垒遗迹宝藏",
    "end_city_treasure": "末地城宝藏",
    "ancient_city": "远古城市宝箱",
    "pillager_outpost": "掠夺者前哨站宝箱",
    "buried_treasure": "埋藏的宝藏",
    "ruined_portal": "废弃传送门宝箱",
    "ocean_ruin_cold": "海底废墟宝箱",
    "village_weaponsmith": "村庄武器匠宝箱",
    "village_armorer": "村庄盔甲匠宝箱",
}


ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "config" / "DefaultPrizePoolData.java"
PLAYER = "$" + "{player}"
RANDOM_ITEM = "$" + "{random:item}"

ROWS = []
NAMES = set()


def add(tier, nzh, nen, tzh, ten, weight, color, major, command):
    for name in (nzh, nen):
        if name in NAMES:
            raise SystemExit("duplicate preset name: " + name)
        NAMES.add(name)
    ROWS.append((tier, nzh, nen, tzh, ten, round(weight, 1), color, major, command))


def give(tier, item, count, weight, color="red", major=False):
    zh, en = ITEMS[item]
    nzh = zh + "×" + str(count)
    nen = en + " x" + str(count)
    add(tier, nzh, nen, nzh, nen, weight, color, major,
        "give " + PLAYER + " minecraft:" + item + " " + str(count))


def stack(tier, item, counts, weight, color="red"):
    for index, count in enumerate(counts):
        factor = (1.0, 0.55, 0.3)[min(index, 2)]
        give(tier, item, count, weight * factor, color)


def effect(tier, eff, seconds, amp, weight, color="red", major=False):
    zh, en = EFFECTS[eff]
    roman = "I" * (amp + 1)
    nzh = zh + roman + " " + str(seconds) + "秒"
    nen = en + " " + roman + " " + str(seconds) + "s"
    add(tier, nzh, nen, nzh, nen, weight, color, major,
        "effect give " + PLAYER + " minecraft:" + eff + " " + str(seconds) + " " + str(amp))


def mob(tier, mob_id, count, weight, color="black", major=False):
    zh, en = MOBS[mob_id]
    nzh = zh + ("×" + str(count) if count > 1 else "")
    nen = en + (" x" + str(count) if count > 1 else "")
    line = "execute at " + PLAYER + " run summon minecraft:" + mob_id + " ~ ~ ~"
    add(tier, nzh, nen, nzh, nen, weight, color, major, "\n".join([line] * count))


def loot(tier, table, weight, color="red", major=False):
    zh = LOOT[table]
    en = table.replace("_", " ").title()
    add(tier, zh, en, zh, en, weight, color, major,
        "loot give " + PLAYER + " loot minecraft:chests/" + table)


def special(tier, nzh, nen, command, weight, color="red", major=False):
    add(tier, nzh, nen, nzh, nen, weight, color, major, command)


def book(tier, nzh, nen, count, weight, color="red", major=False, enchant="^(ENCHANTED_BOOK)", level=1):
    command = ("give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\""
               + enchant + "\",lvl:" + str(level) + "s}]} " + str(count))
    add(tier, nzh, nen, nzh, nen, weight, color, major, command)


def xp(tier, nzh, nen, amount, unit, weight):
    add(tier, nzh, nen, nzh, nen, weight, "red", False,
        "experience add " + PLAYER + " " + str(amount) + " " + unit)


def sound(tier, nzh, nen, weight):
    add(tier, nzh, nen, nzh, nen, weight, "red", False,
        "playsound ^(SOUND) master " + PLAYER + " ~ ~ ~ 1 1")


def particles(tier, nzh, nen, weight):
    add(tier, nzh, nen, nzh, nen, weight, "red", False,
        "execute at " + PLAYER + " run particle ^(PARTICLE) ~ ~1 ~ 1 1 1 0 24")


# ---------------------------------------------------------------------------
# Region 1: travel supplies
# ---------------------------------------------------------------------------
stack(1, "bread", [4, 16, 64], 120)
stack(1, "baked_potato", [4, 16, 48], 110)
stack(1, "apple", [3, 12, 32], 100)
stack(1, "carrot", [4, 16, 48], 95)
stack(1, "wheat", [4, 16, 32], 90)
stack(1, "torch", [8, 32, 64], 85)
stack(1, "cobblestone", [16, 32, 64], 80)
stack(1, "oak_planks", [16, 32, 64], 75)
stack(1, "stick", [8, 32, 64], 70)
stack(1, "coal", [4, 16, 32], 65)
stack(1, "charcoal", [4, 16, 32], 60)
stack(1, "gravel", [16, 32, 64], 55)
stack(1, "sand", [16, 32, 64], 50)
stack(1, "dirt", [16, 32, 64], 45)
stack(1, "clay_ball", [8, 32, 64], 40)
stack(1, "bone", [3, 12, 24], 35)
stack(1, "egg", [4, 16, 32], 30)
stack(1, "sugar_cane", [4, 16, 32], 25)
stack(1, "string", [4, 16, 32], 28)
stack(1, "flint", [2, 6, 12], 30)
give(1, "leather_helmet", 1, 20)
give(1, "leather_boots", 1, 20)
give(1, "stone_sword", 1, 25)
give(1, "stone_pickaxe", 1, 25)
give(1, "stone_axe", 1, 22)
give(1, "arrow", 12, 30)
give(1, "oak_log", 8, 18)
give(1, "cooked_beef", 2, 22)
effect(1, "speed", 30, 0, 18)
effect(1, "regeneration", 10, 0, 18)
effect(1, "saturation", 5, 0, 14)
effect(1, "absorption", 30, 0, 16)
effect(1, "fire_resistance", 60, 0, 14)
effect(1, "water_breathing", 60, 0, 12)
effect(1, "jump_boost", 30, 0, 12)
effect(1, "haste", 30, 0, 12)
effect(1, "hunger", 30, 0, 22, "black")
effect(1, "weakness", 20, 0, 20, "black")
effect(1, "slowness", 15, 0, 18, "black")
effect(1, "poison", 10, 0, 16, "black")
effect(1, "blindness", 8, 0, 14, "black")
effect(1, "nausea", 12, 0, 12, "black")
special(1, "一件随机物品", "One random item", "give " + PLAYER + " ^(ITEM) 1", 25)
special(1, "四件随机物品", "Four random items", "give " + PLAYER + " ^(ITEM) 4", 18)
special(1, "一叠随机方块", "A stack of random blocks", "give " + PLAYER + " ^(BLOCK) 8", 20)
special(1, "一次随机增益", "A random boon", "effect give " + PLAYER + " ^(BUFF) 30 0", 16)
special(1, "一次随机负面效果", "A random bane", "effect give " + PLAYER + " ^(DEBUFF) 15 0", 14, "black")
sound(1, "一声随机响动", "A random sound", 8)
particles(1, "一阵随机粒子", "A swirl of particles", 8)
special(1, "一只随机生物", "A random creature", "execute at " + PLAYER + " run summon ^(MOB) ~ ~ ~", 12, "black")
xp(1, "三十点经验", "Thirty experience points", 30, "points", 35)
xp(1, "一百点经验", "One hundred experience points", 100, "points", 10)
special(1, "一场细雨", "A light rain", "weather rain", 8)
special(1, "一场雷暴", "A thunderstorm", "weather thunder", 5, "black")
special(1, "三只鸡", "Three chickens",
        "\n".join(["execute at " + PLAYER + " run summon minecraft:chicken ~ ~ ~"] * 3), 10)
special(1, "升空二十格", "Twenty blocks up", "tp " + PLAYER + " ~ ~20 ~", 6)
give(1, "golden_apple", 2, 8)
special(1, "时间到白天", "Daylight returns", "time set day", 8)
special(1, "时间到夜晚", "Night falls", "time set night", 8)
xp(1, "五级经验", "Five levels", 5, "levels", 10)

# ---------------------------------------------------------------------------
# Region 2: smith and miner
# ---------------------------------------------------------------------------
stack(2, "iron_ingot", [3, 12, 32], 60)
stack(2, "iron_block", [1, 3, 8], 30)
stack(2, "lapis_lazuli", [4, 16, 32], 45)
stack(2, "redstone", [8, 32, 64], 50)
stack(2, "gold_nugget", [8, 32, 64], 40)
stack(2, "copper_ingot", [8, 32, 64], 40)
stack(2, "coal_block", [1, 4, 8], 30)
stack(2, "arrow", [24, 48, 64], 30)
stack(2, "hay_block", [4, 16, 32], 25)
stack(2, "cooked_porkchop", [4, 16, 32], 25)
stack(2, "pumpkin_pie", [2, 8, 16], 20)
stack(2, "cookie", [8, 32, 64], 22)
stack(2, "tnt", [2, 6, 12], 14, "black")
give(2, "iron_sword", 1, 30)
give(2, "iron_pickaxe", 1, 30)
give(2, "iron_axe", 1, 28)
give(2, "iron_chestplate", 1, 24)
give(2, "iron_helmet", 1, 26)
give(2, "iron_boots", 1, 26)
give(2, "iron_leggings", 1, 26)
give(2, "shield", 1, 28)
give(2, "bucket", 1, 20)
give(2, "water_bucket", 1, 18)
give(2, "milk_bucket", 1, 18)
give(2, "flint_and_steel", 1, 20)
give(2, "shears", 1, 18)
give(2, "fishing_rod", 1, 18)
give(2, "bow", 1, 20)
give(2, "crossbow", 1, 16)
give(2, "compass", 1, 15)
give(2, "clock", 1, 15)
give(2, "name_tag", 1, 12)
give(2, "saddle", 1, 10)
give(2, "lead", 1, 12)
effect(2, "speed", 60, 1, 16)
effect(2, "strength", 30, 0, 16)
effect(2, "resistance", 30, 0, 14)
effect(2, "fire_resistance", 90, 0, 14)
effect(2, "regeneration", 20, 1, 12)
effect(2, "absorption", 60, 0, 12)
effect(2, "haste", 60, 1, 10)
effect(2, "night_vision", 120, 0, 10)
effect(2, "slowness", 20, 1, 16, "black")
effect(2, "weakness", 30, 1, 14, "black")
effect(2, "mining_fatigue", 30, 1, 12, "black")
effect(2, "poison", 15, 1, 10, "black")
effect(2, "blindness", 10, 0, 8, "black")
effect(2, "hunger", 40, 1, 8, "black")
loot(2, "abandoned_mineshaft", 12)
loot(2, "desert_pyramid", 10)
loot(2, "jungle_temple", 10)
mob(2, "zombie", 1, 14)
mob(2, "skeleton", 2, 12)
mob(2, "spider", 3, 10)
mob(2, "creeper", 1, 10)
mob(2, "pillager", 2, 8)
mob(2, "husk", 2, 8)
special(2, "八件随机物品", "Eight random items", "give " + PLAYER + " ^(ITEM) 8", 18)
special(2, "十六块随机方块", "Sixteen random blocks", "give " + PLAYER + " ^(BLOCK) 16", 16)
special(2, "一次强力增益", "A strong boon", "effect give " + PLAYER + " ^(BUFF) 60 1", 12)
special(2, "一次强力负面效果", "A strong bane", "effect give " + PLAYER + " ^(DEBUFF) 20 1", 10, "black")
book(2, "一本随机附魔书", "One random enchanted book", 1, 10)
special(2, "一瓶随机药水", "One random potion",
        "give " + PLAYER + " minecraft:potion{Potion:\"^(POTION)\"} 1", 10)
sound(2, "一段随机乐声", "A random tune", 6)
particles(2, "一阵随机火花", "A burst of sparks", 6)
xp(2, "两百点经验", "Two hundred experience points", 200, "points", 10)
mob(2, "horse", 1, 8) if False else None
special(2, "一匹骏马", "A horse", "execute at " + PLAYER + " run summon minecraft:horse ~ ~ ~", 8)
special(2, "铁傀儡护卫", "An iron golem guard",
        "execute at " + PLAYER + " run summon minecraft:iron_golem ~ ~ ~", 6)
special(2, "牧场来客", "Farm visitors",
        "execute at " + PLAYER + " run summon minecraft:cow ~ ~ ~\n"
        "execute at " + PLAYER + " run summon minecraft:pig ~ ~ ~", 8)
give(2, "cooked_beef", 4, 10)
give(2, "hay_block", 8, 10)
special(2, "雨过天晴", "The rain stops", "weather clear", 5)
special(2, "净化", "Cleansed", "effect clear " + PLAYER, 8)
give(2, "experience_bottle", 8, 8)
give(2, "ender_pearl", 4, 8)

# ---------------------------------------------------------------------------
# Region 3: vault and sorcery
# ---------------------------------------------------------------------------
stack(3, "gold_ingot", [4, 16, 32], 50)
stack(3, "gold_block", [1, 4, 8], 25)
stack(3, "emerald", [2, 8, 16], 22)
stack(3, "emerald_block", [1, 2, 4], 10)
stack(3, "diamond", [1, 4, 8], 18)
stack(3, "lapis_block", [1, 2, 4], 12)
stack(3, "amethyst_shard", [4, 16, 32], 25)
stack(3, "experience_bottle", [4, 16, 32], 30)
stack(3, "book", [4, 16, 32], 28)
stack(3, "ender_pearl", [2, 6, 12], 20)
stack(3, "blaze_powder", [4, 12, 24], 18)
stack(3, "ghast_tear", [1, 4, 8], 10)
stack(3, "glowstone_dust", [4, 16, 32], 22)
stack(3, "redstone_block", [1, 4, 8], 14)
stack(3, "quartz", [8, 32, 64], 20)
stack(3, "obsidian", [4, 16, 32], 16)
stack(3, "golden_carrot", [2, 8, 16], 16)
stack(3, "golden_apple", [1, 4, 8], 12)
give(3, "enchanting_table", 1, 6)
give(3, "bookshelf", 1, 10)
give(3, "anvil", 1, 6)
give(3, "brewing_stand", 1, 8)
give(3, "cauldron", 1, 8)
give(3, "grindstone", 1, 6)
give(3, "smithing_table", 1, 6)
give(3, "glowstone", 1, 8)
give(3, "nether_wart", 8, 10)
give(3, "soul_sand", 8, 8)
give(3, "crying_obsidian", 4, 8)
give(3, "glass_bottle", 16, 8)
effect(3, "strength", 60, 1, 14)
effect(3, "regeneration", 30, 1, 12)
effect(3, "resistance", 60, 1, 12)
effect(3, "fire_resistance", 120, 1, 10)
effect(3, "water_breathing", 120, 0, 10)
effect(3, "night_vision", 180, 0, 8)
effect(3, "absorption", 60, 1, 8)
effect(3, "jump_boost", 60, 1, 8)
effect(3, "poison", 20, 2, 12, "black")
effect(3, "wither", 10, 1, 10, "black")
effect(3, "nausea", 20, 1, 10, "black")
effect(3, "darkness", 15, 1, 8, "black")
effect(3, "levitation", 10, 0, 8, "black")
effect(3, "instant_damage", 1, 1, 6, "black")
loot(3, "stronghold_corridor", 12)
loot(3, "woodland_mansion", 10)
loot(3, "nether_bridge", 10)
book(3, "一本附魔书", "One enchanted book", 1, 10)
book(3, "两本附魔书", "Two enchanted books", 2, 6)
book(3, "三本附魔书", "Three enchanted books", 3, 4)
book(3, "一本诅咒之书", "One cursed book", 1, 6, "black", enchant="^(CURSE)")
book(3, "一本增益之书", "One blessed book", 1, 8, enchant="^(BUFF_ENCHANTMENT)")
book(3, "五本附魔书", "Five enchanted books", 5, 3)
special(3, "九件随机物品", "Nine random items", "give " + PLAYER + " ^(ITEM) 9", 12)
special(3, "一次长效增益", "A lasting boon", "effect give " + PLAYER + " ^(BUFF) 120 1", 10)
special(3, "一次长效负面", "A lasting bane", "effect give " + PLAYER + " ^(DEBUFF) 30 1", 8, "black")
sound(3, "一首随机乐声", "A random melody", 6)
particles(3, "一场随机粒子雨", "A rain of particles", 6)
special(3, "一件随机物品(旧池)", "One item from the old pool", "give " + PLAYER + " " + RANDOM_ITEM + " 1", 12)
xp(3, "五百点经验", "Five hundred experience points", 500, "points", 6)
special(3, "一瓶强效治疗药水", "A potion of strong healing",
        "give " + PLAYER + " minecraft:potion{Potion:\"minecraft:strong_healing\"} 1", 8)
give(3, "ender_chest", 2, 4)
give(3, "anvil", 2, 6)
give(3, "brewing_stand", 2, 6)
special(3, "雷霆一击", "A bolt from above",
        "execute at " + PLAYER + " run summon minecraft:lightning_bolt ~ ~ ~", 4, "black")

# ---------------------------------------------------------------------------
# Region 4: abyssal treasure
# ---------------------------------------------------------------------------
stack(4, "diamond_block", [1, 2, 4], 18)
stack(4, "netherite_scrap", [1, 2, 4], 12)
stack(4, "ancient_debris", [1, 2, 4], 8)
stack(4, "blaze_rod", [2, 8, 16], 20)
stack(4, "shulker_shell", [2, 4, 8], 16)
stack(4, "ender_eye", [1, 4, 8], 20)
stack(4, "prismarine_shard", [8, 32, 64], 18)
stack(4, "sea_lantern", [2, 8, 16], 16)
stack(4, "sponge", [1, 4, 8], 10)
stack(4, "nautilus_shell", [1, 4, 8], 10)
stack(4, "phantom_membrane", [2, 8, 16], 14)
stack(4, "magma_cream", [2, 8, 16], 14)
stack(4, "fire_charge", [4, 16, 32], 16)
stack(4, "dragon_breath", [1, 4, 8], 8)
stack(4, "chorus_fruit", [4, 16, 32], 14)
stack(4, "popped_chorus_fruit", [4, 16, 32], 12)
stack(4, "end_stone", [16, 32, 64], 12)
stack(4, "purpur_block", [8, 32, 64], 12)
give(4, "diamond_sword", 1, 8)
give(4, "diamond_pickaxe", 1, 8)
give(4, "diamond_chestplate", 1, 6)
give(4, "turtle_helmet", 1, 6)
give(4, "heart_of_the_sea", 1, 4)
give(4, "wither_skeleton_skull", 1, 4)
give(4, "end_crystal", 1, 4)
effect(4, "strength", 90, 2, 10)
effect(4, "regeneration", 45, 2, 10)
effect(4, "resistance", 90, 2, 8)
effect(4, "absorption", 120, 2, 8)
effect(4, "fire_resistance", 180, 1, 8)
effect(4, "water_breathing", 180, 1, 6)
effect(4, "haste", 120, 2, 6)
effect(4, "health_boost", 120, 2, 6)
effect(4, "wither", 15, 2, 10, "black")
effect(4, "poison", 30, 2, 8, "black")
effect(4, "darkness", 20, 2, 8, "black")
effect(4, "levitation", 15, 1, 6, "black")
effect(4, "instant_damage", 1, 2, 5, "black")
effect(4, "bad_omen", 60, 0, 4, "black")
loot(4, "bastion_treasure", 10)
loot(4, "end_city_treasure", 8)
loot(4, "ancient_city", 6)
mob(4, "enderman", 2, 8)
mob(4, "blaze", 2, 8)
mob(4, "magma_cube", 3, 8)
mob(4, "ghast", 1, 6)
mob(4, "wither_skeleton", 2, 6)
mob(4, "phantom", 4, 6)
mob(4, "drowned", 3, 6)
mob(4, "guardian", 2, 6)
special(4, "一组随机物品", "Sixteen random items", "give " + PLAYER + " ^(ITEM) 16", 12)
special(4, "两件稀有随机物品", "Two rare random items", "give " + PLAYER + " ^(ITEM) 2", 12)
special(4, "一次深渊增益", "An abyssal boon", "effect give " + PLAYER + " ^(BUFF) 90 2", 8)
special(4, "一次深渊诅咒", "An abyssal curse", "effect give " + PLAYER + " ^(DEBUFF) 30 2", 8, "black")
book(4, "八本随机附魔书", "Eight random enchanted books", 8, 4)
special(4, "一只随机深海生物", "A random deep creature",
        "execute at " + PLAYER + " run summon ^(MOB) ~ ~ ~", 8, "black")
particles(4, "一阵末地粒子", "A drift of end particles", 6)
sound(4, "一声远古回响", "An ancient echo", 6)
xp(4, "一千点经验", "One thousand experience points", 1000, "points", 4)
give(4, "netherite_scrap", 8, 4)
special(4, "远古守卫者", "An elder guardian",
        "execute at " + PLAYER + " run summon minecraft:elder_guardian ~ ~ ~", 4, "black")
special(4, "凋灵降临", "A wither descends",
        "execute at " + PLAYER + " run summon minecraft:wither ~ ~10 ~", 2, "black", major=True)
give(4, "end_crystal", 2, 4)
give(4, "chorus_fruit", 64, 4)

# ---------------------------------------------------------------------------
# Region 5: relics of old
# ---------------------------------------------------------------------------
stack(5, "netherite_ingot", [1, 5], 10)
stack(5, "enchanted_golden_apple", [1, 3], 6)
stack(5, "totem_of_undying", [1, 3], 5)
stack(5, "diamond_block", [3, 6], 8)
stack(5, "netherite_scrap", [6, 12], 8)
stack(5, "shulker_shell", [12, 24], 8)
stack(5, "diamond", [12, 24], 10)
stack(5, "emerald", [12, 24], 10)
stack(5, "gold_block", [12, 24], 8)
stack(5, "experience_bottle", [48, 64], 6)
stack(5, "ancient_debris", [6, 12], 6)
stack(5, "heart_of_the_sea", [2, 6], 4)
stack(5, "wither_skeleton_skull", [3, 6], 4)
stack(5, "dragon_breath", [12, 24], 4)
give(5, "elytra", 1, 3)
give(5, "netherite_sword", 1, 4)
give(5, "netherite_chestplate", 1, 4)
give(5, "beacon", 1, 2)
give(5, "nether_star", 1, 2)
give(5, "conduit", 1, 3)
give(5, "trident", 1, 3)
give(5, "dragon_egg", 1, 1)
give(5, "shulker_box", 1, 4)
give(5, "ender_chest", 1, 4)
give(5, "netherite_block", 1, 1)
give(5, "respawn_anchor", 1, 3)
give(5, "lodestone", 1, 3)
effect(5, "strength", 300, 3, 6)
effect(5, "regeneration", 120, 3, 6)
effect(5, "resistance", 300, 3, 4)
effect(5, "absorption", 300, 3, 4)
effect(5, "health_boost", 300, 3, 4)
effect(5, "haste", 300, 3, 4)
effect(5, "fire_resistance", 600, 2, 4)
effect(5, "water_breathing", 600, 2, 4)
effect(5, "wither", 30, 3, 8, "black")
effect(5, "poison", 60, 3, 6, "black")
effect(5, "darkness", 40, 3, 6, "black")
effect(5, "levitation", 20, 2, 4, "black")
effect(5, "instant_damage", 1, 3, 4, "black")
effect(5, "bad_omen", 120, 1, 4, "black")
effect(5, "mining_fatigue", 60, 2, 4, "black")
effect(5, "nausea", 60, 2, 4, "black")
loot(5, "buried_treasure", 6)
loot(5, "ocean_ruin_cold", 6)
loot(5, "village_armorer", 6)
mob(5, "enderman", 4, 6)
mob(5, "wither_skeleton", 4, 5)
mob(5, "blaze", 4, 5)
mob(5, "ravager", 2, 4)
mob(5, "evoker", 2, 4)
mob(5, "vex", 4, 4)
special(5, "远古馈赠", "An ancient gift", "give " + PLAYER + " ^(ITEM) 32", 8)
special(5, "六件远古物品", "Six ancient items", "give " + PLAYER + " ^(ITEM) 6", 6)
special(5, "远古方块堆", "A pile of ancient blocks", "give " + PLAYER + " ^(BLOCK) 32", 6)
special(5, "远古祝福", "An ancient blessing", "effect give " + PLAYER + " ^(BUFF) 300 3", 4)
special(5, "远古诅咒", "An ancient curse", "effect give " + PLAYER + " ^(DEBUFF) 60 3", 4, "black")
book(5, "一本远古附魔书", "One ancient enchanted book", 1, 4, enchant="^(BUFF_ENCHANTMENT)", level=5)
book(5, "一本诅咒古籍", "One cursed tome", 1, 3, "black", enchant="^(CURSE)", level=3)
book(5, "一本远古禁书", "One forbidden book", 1, 4)
special(5, "一只远古生物", "An ancient creature",
        "execute at " + PLAYER + " run summon ^(MOB) ~ ~ ~", 4, "black")
particles(5, "远古粒子", "Ancient particles", 3)
sound(5, "远古回响", "Echoes of old", 3)
special(5, "远古阵营", "An ancient host",
        "execute at " + PLAYER + " run summon minecraft:enderman ~ ~ ~\n"
        "execute at " + PLAYER + " run summon minecraft:enderman ~ ~ ~\n"
        "execute at " + PLAYER + " run summon minecraft:enderman ~ ~ ~", 3, "black")
xp(5, "五千点经验", "Five thousand experience points", 5000, "points", 2)
give(5, "netherite_ingot", 10, 2)
give(5, "diamond_block", 8, 3)
give(5, "shulker_box", 8, 3)
special(5, "远古守卫者×2", "Two elder guardians",
        "execute at " + PLAYER + " run summon minecraft:elder_guardian ~ ~ ~\n"
        "execute at " + PLAYER + " run summon minecraft:elder_guardian ~ ~ ~", 3, "black")
special(5, "凋灵×2", "Two withers",
        "execute at " + PLAYER + " run summon minecraft:wither ~ ~10 ~\n"
        "execute at " + PLAYER + " run summon minecraft:wither ~ ~10 ~", 1, "black", major=True)
give(5, "end_crystal", 4, 4)
give(5, "totem_of_undying", 4, 2)
give(5, "enchanted_golden_apple", 4, 2)
give(5, "trident", 3, 2)
stack(5, "blaze_rod", [20, 40], 6)
stack(5, "ender_pearl", [40, 56], 6)
give(5, "golden_apple", 16, 4)
give(5, "glowstone", 8, 4)
special(5, "全服经验雨", "An experience rain for everyone",
        "experience add @a 50 levels", 2, "red", major=True)
special(5, "天降雷霆", "Lightning over everyone",
        "execute at @a run summon minecraft:lightning_bolt ~ ~ ~", 2, "black", major=True)
special(5, "魔鬼的馈赠", "The devil's gift", "give " + PLAYER + " minecraft:netherite_ingot 32", 1, "red", major=True)
special(5, "恶魔的诅咒", "The devil's curse",
        "effect give @a minecraft:wither 20 2", 1, "black", major=True)
special(5, "赌徒的狂欢", "The gambler's revel",
        "give " + PLAYER + " minecraft:emerald 32", 1, "red", major=True)
special(5, "晨光普照", "Morning light", "time set day", 2, "red", major=True)
special(5, "长夜降临", "The long night", "time set midnight", 2, "black", major=True)
special(5, "诸神之战", "War of the gods",
        "execute at @a run summon minecraft:wither ~ ~20 ~", 1, "black", major=True)

# ---------------------------------------------------------------------------
# Region 6: the devil's table
# ---------------------------------------------------------------------------
stack(6, "netherite_ingot", [8, 16], 4)
stack(6, "diamond_block", [16, 32], 4)
stack(6, "netherite_block", [3, 6], 2)
stack(6, "enchanted_golden_apple", [8, 16], 3)
stack(6, "totem_of_undying", [8, 16], 3)
stack(6, "dragon_breath", [32, 64], 3)
stack(6, "golden_apple", [32, 64], 4)
stack(6, "experience_bottle", [96, 128], 3)
stack(6, "diamond", [32, 64], 4)
stack(6, "emerald", [32, 64], 4)
stack(6, "netherite_scrap", [32, 64], 3)
stack(6, "ancient_debris", [16, 32], 3)
stack(6, "shulker_shell", [48, 64], 3)
stack(6, "blaze_rod", [48, 64], 3)
stack(6, "ender_pearl", [32, 64], 3)
stack(6, "ghast_tear", [16, 32], 2)
give(6, "nether_star", 2, 2)
give(6, "beacon", 2, 2)
give(6, "conduit", 2, 2)
give(6, "trident", 2, 2)
give(6, "dragon_egg", 2, 1)
give(6, "elytra", 2, 1)
give(6, "netherite_sword", 2, 2)
give(6, "netherite_chestplate", 2, 2)
give(6, "respawn_anchor", 2, 2)
give(6, "ender_chest", 3, 2)
give(6, "shulker_box", 2, 3)
give(6, "lodestone", 2, 2)
give(6, "heart_of_the_sea", 8, 2)
give(6, "wither_skeleton_skull", 8, 2)
give(6, "wither_skeleton_skull", 16, 2)
effect(6, "strength", 600, 4, 4)
effect(6, "regeneration", 300, 4, 4)
effect(6, "resistance", 600, 4, 3)
effect(6, "absorption", 600, 4, 3)
effect(6, "health_boost", 600, 4, 3)
effect(6, "haste", 600, 4, 3)
effect(6, "fire_resistance", 1200, 3, 3)
effect(6, "water_breathing", 1200, 3, 3)
effect(6, "speed", 600, 4, 3)
effect(6, "night_vision", 1200, 2, 2)
effect(6, "wither", 60, 4, 6, "black")
effect(6, "poison", 120, 4, 5, "black")
effect(6, "darkness", 90, 4, 5, "black")
effect(6, "levitation", 30, 4, 4, "black")
effect(6, "instant_damage", 1, 4, 5, "black")
effect(6, "bad_omen", 300, 3, 4, "black")
effect(6, "mining_fatigue", 120, 4, 4, "black")
effect(6, "nausea", 120, 4, 4, "black")
effect(6, "blindness", 120, 3, 4, "black")
effect(6, "hunger", 300, 4, 4, "black")
mob(6, "zombie", 8, 4)
mob(6, "skeleton", 8, 4)
mob(6, "creeper", 5, 3)
mob(6, "wither_skeleton", 6, 3)
mob(6, "blaze", 6, 3)
mob(6, "ender_dragon", 2, 1, "black", major=True)
special(6, "至尊随机物品", "A supreme random item", "give " + PLAYER + " ^(ITEM) 64", 6)
special(6, "十二件随机物品", "Twelve random items", "give " + PLAYER + " ^(ITEM) 12", 6)
special(6, "至尊方块堆", "A supreme pile of blocks", "give " + PLAYER + " ^(BLOCK) 64", 6)
special(6, "至尊祝福", "A supreme blessing", "effect give " + PLAYER + " ^(BUFF) 600 4", 4)
special(6, "至尊诅咒", "A supreme curse", "effect give " + PLAYER + " ^(DEBUFF) 120 4", 4, "black")
book(6, "八本至尊附魔书", "Eight supreme enchanted books", 8, 4)
book(6, "一本神话附魔书", "One mythic enchanted book", 1, 3, enchant="^(BUFF_ENCHANTMENT)", level=10)
book(6, "一本神话诅咒书", "One mythic cursed book", 1, 3, "black", enchant="^(CURSE)", level=5)
sound(6, "恶魔的低语", "A devil's whisper", 3)
particles(6, "恶魔的余烬", "Devil's embers", 3)
special(6, "一只神话生物", "A mythic creature",
        "execute at " + PLAYER + " run summon ^(MOB) ~ ~ ~", 3, "black")
xp(6, "一万点经验", "Ten thousand experience points", 10000, "points", 2)
give(6, "netherite_block", 8, 2)
give(6, "beacon", 4, 2)
give(6, "conduit", 4, 2)
give(6, "trident", 4, 2)
give(6, "dragon_egg", 4, 1)
give(6, "totem_of_undying", 32, 1)
give(6, "enchanted_golden_apple", 32, 1)
loot(6, "pillager_outpost", 4)
loot(6, "ruined_portal", 4)
special(6, "恶魔的盛宴", "The devil's feast", "give @a minecraft:enchanted_golden_apple 8", 1, "red", major=True)
special(6, "末日雷霆", "The last thunder", "execute at @a run summon minecraft:lightning_bolt ~ ~ ~", 1, "black", major=True)
special(6, "天降财宝", "Treasure from the sky", "loot give @a loot minecraft:chests/end_city_treasure", 1, "red", major=True)
special(6, "万界诅咒", "A curse upon all worlds", "effect give @a minecraft:wither 30 3", 1, "black", major=True)
special(6, "命运轮盘", "The wheel of fate", "give @a minecraft:diamond 16", 1, "red", major=True)
special(6, "终焉之门", "The gate of the end", "execute at @a run summon minecraft:enderman ~ ~ ~", 1, "black", major=True)


# ---------------------------------------------------------------------------
# Validation and emit
# ---------------------------------------------------------------------------
SAMPLE_TOKEN = re.compile(r"\^\(([A-Z_]+)\)(?:\[[a-z0-9_.-]+\])?")
RANDOM_TOKEN = re.compile(r"\$\{random:([a-z_]+)\}")

COMMAND_ROOTS = {"give", "effect", "experience", "summon", "execute", "loot", "playsound",
                 "particle", "weather", "time", "tp", "clear", "setblock", "fill"}
SAMPLE_TYPES = {"ITEM", "BLOCK", "ENTITY", "MOB", "EFFECT", "ENCHANTMENT", "POTION", "SOUND",
                "PARTICLE", "PAINTING", "ATTRIBUTE", "FLUID", "BIOME", "STRUCTURE", "DAMAGE_TYPE",
                "BUFF", "DEBUFF", "NEUBUFF", "BUFF_ENCHANTMENT", "CURSE", "ENCHANTED_BOOK"}


def fail(message):
    raise SystemExit("preset pool invalid: " + message)


def validate():
    counts = {}

    for tier, nzh, nen, tzh, ten, weight, color, major, command in ROWS:
        if tier < 1 or tier > 6:
            fail("tier out of range: " + str(tier))
        if len(nzh) > 96 or len(nen) > 96:
            fail("name too long: " + nzh)
        if weight < 0:
            fail("negative weight: " + nzh)
        if color not in ("red", "black"):
            fail("bad colour: " + nzh)
        if len(command) > 512:
            fail("command too long: " + nzh)
        if not nzh or not nen or not tzh or not ten:
            fail("empty text: " + nzh)

        for line in command.split("\n"):
            root = line.split(" ")[0]

            if root not in COMMAND_ROOTS:
                fail("unknown command root '" + root + "' in " + nzh)

            for match in SAMPLE_TOKEN.finditer(line):
                if match.group(1) not in SAMPLE_TYPES:
                    fail("unknown sample type " + match.group(1) + " in " + nzh)

            for pool in RANDOM_TOKEN.findall(line):
                if pool not in ("item", "effect", "entity", "enchantment", "potion"):
                    fail("unknown random pool " + pool + " in " + nzh)

            if line.count("{") != line.count("}"):
                fail("unbalanced nbt braces in " + nzh)

        counts[tier] = counts.get(tier, 0) + 1

    if sorted(counts) != [1, 2, 3, 4, 5, 6]:
        fail("regions present: " + str(sorted(counts)))

    for tier in sorted(counts):
        if counts[tier] < 100:
            fail("region " + str(tier) + " has only " + str(counts[tier]) + " entries")

    return counts


def jstr(value):
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""


def emit():
    ROWS.sort(key=lambda row: row[0])
    lines = []
    lines.append("/*")
    lines.append(" * Devil Bone Gambler")
    lines.append(" * Copyright (c) 2026 JularDepick")
    lines.append(" * Author: JularDepick (https://github.com/JularDepick)")
    lines.append(" */")
    lines.append("")
    lines.append("package com.devilbonegambler.config;")
    lines.append("")
    lines.append("/**")
    lines.append(" * Preset prize entries, generated by scripts/build_preset_pool.py.")
    lines.append(" * Do not edit by hand: change the script and run it again.")
    lines.append(" *")
    lines.append(" * Row layout: tier, Chinese name, English name, Chinese notify text, English notify text,")
    lines.append(" * weight, colour, major event, command.")
    lines.append(" */")
    lines.append("final class DefaultPrizePoolData")
    lines.append("{")
    lines.append("    static final String[][] ENTRIES =")
    lines.append("    {")

    for tier, nzh, nen, tzh, ten, weight, color, major, command in ROWS:
        cells = [jstr(str(tier)), jstr(nzh), jstr(nen), jstr(tzh), jstr(ten), jstr(str(weight)), jstr(color),
                 jstr("true" if major else "false"), jstr(command)]
        lines.append("        {" + ", ".join(cells) + "},")

    lines.append("    };")
    lines.append("")
    lines.append("    private DefaultPrizePoolData()")
    lines.append("    {")
    lines.append("    }")
    lines.append("}")
    lines.append("")
    OUT.write_text("\n".join(lines), encoding="utf-8")


counts = validate()

if "--check" not in sys.argv:
    emit()
    print("wrote", len(ROWS), "entries")

for tier in sorted(counts):
    print("region", tier, counts[tier])
