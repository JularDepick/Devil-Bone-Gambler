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
    "bat": ("蝙蝠", "Bat"),
    "rabbit": ("兔子", "Rabbit"),
    "cat": ("猫", "Cat"),
    "fox": ("狐狸", "Fox"),
    "panda": ("熊猫", "Panda"),
    "parrot": ("鹦鹉", "Parrot"),
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

POOLS = [
    ("survival", "纯生存", "Survival",
     "面向纯生存玩法: 基础物资, 战斗补给, 食物, 家畜与增益, 不含雷电与传送等趣味指令.",
     "Pure survival: supplies, combat gear, food, livestock and buffs, without lightning or teleport tricks."),
    ("creative", "创意生存", "Creative",
     "面向创意玩法: 允许各种趣味与高级指令, 可以受伤但不会轻易致死, 只有极低概率直接死亡.",
     "Creative: playful and advanced commands, damage is possible but death stays extremely rare."),
]

ROWS = []
NAMES = set()
PLAYER = "$" + "{player}"
RANDOM_ITEM = "$" + "{random:item}"


def add(pool, tier, nzh, nen, tzh, ten, weight, color, major, command):
    key = (pool, nzh)

    if key in NAMES:
        raise SystemExit("duplicate preset name in " + pool + ": " + nzh)

    NAMES.add(key)
    ROWS.append((pool, tier, nzh, nen, tzh, ten, round(weight, 1), color, major, command))


def g(pool, tier, item, count, weight, nzh, nen, color="red", major=False):
    add(pool, tier, nzh, nen, nzh, nen, weight, color, major,
        "give " + PLAYER + " minecraft:" + item + " " + str(count))


def e(pool, tier, eff, seconds, amp, weight, nzh, nen, color="red", major=False):
    add(pool, tier, nzh, nen, nzh, nen, weight, color, major,
        "effect give " + PLAYER + " minecraft:" + eff + " " + str(seconds) + " " + str(amp))


def mob(pool, tier, mob_id, count, weight, nzh, nen, color="black", major=False):
    line = "execute at " + PLAYER + " run summon minecraft:" + mob_id + " ~ ~ ~"
    add(pool, tier, nzh, nen, nzh, nen, weight, color, major, "\n".join([line] * count))


def loot(pool, tier, table, weight, nzh, nen):
    add(pool, tier, nzh, nen, nzh, nen, weight, "red", False,
        "loot give " + PLAYER + " loot minecraft:chests/" + table)


def damage(pool, tier, nzh, nen, amount, weight, color="black", major=False):
    add(pool, tier, nzh, nen, nzh, nen, weight, color, major,
        "damage " + PLAYER + " " + str(amount))


def xp(pool, tier, nzh, nen, amount, weight, unit="points"):
    add(pool, tier, nzh, nen, nzh, nen, weight, "red", False,
        "experience add " + PLAYER + " " + str(amount) + " " + unit)


def raw(pool, tier, nzh, nen, weight, command, color="red", major=False):
    add(pool, tier, nzh, nen, nzh, nen, weight, color, major, command)


# ---------------------------------------------------------------------------
# Survival pool
# ---------------------------------------------------------------------------
g("survival", 1, "bread", "%(4,16)", 120, "一袋面包", "A bag of bread")
g("survival", 1, "baked_potato", "%(4,12)", 110, "热腾腾的马铃薯", "Warm baked potatoes")
g("survival", 1, "apple", "%(3,9)", 100, "几只苹果", "A few apples")
g("survival", 1, "carrot", "%(4,12)", 95, "一把胡萝卜", "A bunch of carrots")
g("survival", 1, "torch", "%(8,32)", 90, "一捆火把", "A bundle of torches")
g("survival", 1, "cobblestone", "%(16,48)", 85, "一堆圆石", "A heap of cobblestone")
g("survival", 1, "oak_planks", "%(16,40)", 80, "几摞木板", "A few stacks of planks")
g("survival", 1, "stick", "%(8,24)", 75, "一把木棍", "A handful of sticks")
g("survival", 1, "coal", "%(4,16)", 70, "一捧煤炭", "A handful of coal")
g("survival", 1, "charcoal", "%(4,12)", 65, "一捧木炭", "A handful of charcoal")
g("survival", 1, "gravel", "%(16,32)", 60, "一堆沙砾", "A heap of gravel")
g("survival", 1, "clay_ball", "%(8,16)", 55, "一袋黏土", "A sack of clay")
g("survival", 1, "bone", "%(3,9)", 50, "几根骨头", "A few bones")
g("survival", 1, "egg", "%(4,12)", 50, "一篮鸡蛋", "A basket of eggs")
g("survival", 1, "sugar_cane", "%(4,12)", 45, "一把甘蔗", "A bundle of sugar cane")
g("survival", 1, "flint", "%(2,6)", 45, "几块燧石", "A few pieces of flint")
g("survival", 1, "leather_helmet", 1, 25, "一顶皮帽", "A leather cap")
g("survival", 1, "stone_sword", 1, 30, "一把石剑", "A stone sword")
g("survival", 1, "arrow", "%(12,32)", 35, "一小袋箭", "A quiver of arrows")
g("survival", 1, "cooked_beef", "%(2,6)", 30, "几块牛排", "A few steaks")
e("survival", 1, "hunger", "%(20,40)", 0, 22, "饥肠辘辘", "Hunger gnaws", "black")
e("survival", 1, "weakness", "%(15,30)", 0, 18, "手脚发软", "Arms feel weak", "black")
e("survival", 1, "slowness", "%(10,25)", 0, 16, "步履沉重", "Heavy steps", "black")
damage("survival", 1, "一阵刺痛", "A sudden sting", "%(1,3)", 14)

g("survival", 2, "iron_ingot", "%(3,12)", 60, "一捧铁锭", "A handful of iron")
g("survival", 2, "iron_sword", 1, 30, "一把铁剑", "An iron sword")
g("survival", 2, "iron_pickaxe", 1, 30, "一把铁镐", "An iron pickaxe")
g("survival", 2, "iron_chestplate", 1, 24, "一件铁胸甲", "An iron chestplate")
g("survival", 2, "shield", 1, 28, "一面盾牌", "A shield")
g("survival", 2, "iron_block", "%(1,4)", 30, "几块铁块", "A few iron blocks")
g("survival", 2, "lapis_lazuli", "%(4,16)", 45, "一袋青金石", "A pouch of lapis")
g("survival", 2, "redstone", "%(8,32)", 50, "一捧红石", "A handful of redstone")
g("survival", 2, "coal_block", "%(1,4)", 30, "几块煤炭块", "A few coal blocks")
g("survival", 2, "bucket", 1, 20, "一只铁桶", "An iron bucket")
g("survival", 2, "milk_bucket", 1, 18, "一桶牛奶", "A bucket of milk")
g("survival", 2, "shears", 1, 18, "一把剪刀", "A pair of shears")
g("survival", 2, "fishing_rod", 1, 18, "一根钓鱼竿", "A fishing rod")
g("survival", 2, "bow", 1, 20, "一把弓", "A bow")
g("survival", 2, "arrow", "%(16,48)", 30, "一大袋箭", "A large quiver")
g("survival", 2, "hay_block", "%(4,16)", 25, "几捆干草", "A few hay bales")
g("survival", 2, "cooked_porkchop", "%(4,12)", 25, "几块熟猪排", "Cooked porkchops")
g("survival", 2, "pumpkin_pie", "%(2,6)", 20, "几块南瓜派", "Pumpkin pies")
g("survival", 2, "cookie", "%(8,24)", 22, "一包曲奇", "A bag of cookies")
g("survival", 2, "saddle", 1, 10, "一副鞍", "A saddle")
g("survival", 2, "lead", 1, 12, "一根拴绳", "A lead")
mob("survival", 2, "cow", 1, 12, "一头牛", "A cow", "red")
mob("survival", 2, "pig", 1, 12, "一头猪", "A pig", "red")
mob("survival", 2, "chicken", 2, 10, "两只鸡", "Two chickens", "red")
mob("survival", 2, "sheep", 1, 10, "一只羊", "A sheep", "red")
e("survival", 2, "regeneration", "%(5,15)", 0, 18, "伤口愈合", "Wounds close")
e("survival", 2, "absorption", "%(20,60)", 0, 16, "一层护盾", "A golden shield")
e("survival", 2, "slowness", "%(10,20)", 1, 16, "双腿灌铅", "Legs turn to lead", "black")
e("survival", 2, "mining_fatigue", "%(20,40)", 0, 12, "手臂酸麻", "Arms go numb", "black")

g("survival", 3, "gold_ingot", "%(4,16)", 50, "一捧金锭", "A handful of gold")
g("survival", 3, "emerald", "%(2,8)", 22, "几颗绿宝石", "A few emeralds")
g("survival", 3, "diamond", "%(1,3)", 18, "几颗钻石", "A few diamonds")
g("survival", 3, "experience_bottle", "%(4,16)", 30, "一袋附魔之瓶", "Bottles of enchanting")
g("survival", 3, "book", "%(4,12)", 28, "几本空书", "A few books")
g("survival", 3, "ender_pearl", "%(1,4)", 20, "几颗末影珍珠", "A few ender pearls")
g("survival", 3, "glowstone", "%(2,8)", 16, "几块荧石", "A few glowstone blocks")
g("survival", 3, "quartz", "%(8,24)", 20, "一捧下界石英", "A handful of quartz")
g("survival", 3, "obsidian", "%(2,8)", 16, "几块黑曜石", "A few obsidian blocks")
g("survival", 3, "enchanting_table", 1, 6, "一张附魔台", "An enchanting table")
g("survival", 3, "bookshelf", "%(2,8)", 10, "几个书架", "A few bookshelves")
g("survival", 3, "anvil", 1, 6, "一座铁砧", "An anvil")
g("survival", 3, "golden_apple", "%(1,3)", 12, "几颗金苹果", "A few golden apples")
g("survival", 3, "golden_carrot", "%(2,6)", 16, "几根金胡萝卜", "Golden carrots")
raw("survival", 3, "一瓶治疗药水", "A healing potion", 8,
    "give " + PLAYER + " minecraft:potion{Potion:\"minecraft:strong_healing\"} 1")
e("survival", 3, "strength", "%(30,60)", 1, 14, "力量涌动", "Strength surges")
e("survival", 3, "resistance", "%(30,60)", 1, 12, "皮肤硬化", "Skin hardens")
e("survival", 3, "fire_resistance", "%(60,120)", 1, 10, "烈焰不侵", "Flames cannot bite")
e("survival", 3, "night_vision", "%(60,180)", 0, 8, "暗中视物", "Eyes pierce the dark")
e("survival", 3, "water_breathing", "%(60,120)", 0, 8, "水中呼吸", "Breathe underwater")
e("survival", 3, "poison", "%(10,20)", 1, 10, "毒素入体", "Poison in the blood", "black")
e("survival", 3, "wither", "%(5,10)", 0, 8, "凋零侵蚀", "Withering touch", "black")
loot("survival", 3, "village_weaponsmith", 10, "村庄武器匠的箱子", "A village weaponsmith chest")
loot("survival", 3, "simple_dungeon", 10, "地牢的箱子", "A dungeon chest")
loot("survival", 3, "abandoned_mineshaft", 8, "废弃矿井的箱子", "A mineshaft chest")

g("survival", 4, "diamond", "%(2,5)", 18, "一小把钻石", "A small handful of diamonds")
g("survival", 4, "diamond_block", "%(1,2)", 8, "一块钻石块", "A diamond block")
g("survival", 4, "netherite_scrap", "%(1,4)", 12, "几块合金碎片", "Netherite scraps")
g("survival", 4, "ancient_debris", "%(1,3)", 8, "几块远古残骸", "Ancient debris")
g("survival", 4, "blaze_rod", "%(2,8)", 20, "几根烈焰棒", "A few blaze rods")
g("survival", 4, "shulker_shell", "%(2,6)", 16, "几片潜影壳", "A few shulker shells")
g("survival", 4, "ender_eye", "%(1,4)", 20, "几颗末影之眼", "A few eyes of ender")
g("survival", 4, "prismarine_shard", "%(8,24)", 18, "一捧海晶碎片", "A handful of prismarine")
g("survival", 4, "sea_lantern", "%(2,8)", 16, "几盏海晶灯", "A few sea lanterns")
g("survival", 4, "sponge", "%(1,4)", 10, "几块海绵", "A few sponges")
g("survival", 4, "heart_of_the_sea", "%(1,2)", 6, "一颗海洋之心", "A heart of the sea")
g("survival", 4, "turtle_helmet", 1, 6, "一顶海龟壳", "A turtle shell")
g("survival", 4, "phantom_membrane", "%(2,8)", 14, "几片幻翼膜", "Phantom membranes")
g("survival", 4, "magma_cream", "%(2,8)", 14, "一捧岩浆膏", "A handful of magma cream")
g("survival", 4, "fire_charge", "%(4,12)", 16, "几颗火焰弹", "A few fire charges")
g("survival", 4, "wither_skeleton_skull", "%(1,2)", 6, "一颗凋灵骷髅头颅", "A wither skull")
g("survival", 4, "dragon_breath", "%(1,4)", 8, "几瓶龙息", "Bottles of dragon breath")
g("survival", 4, "diamond_sword", 1, 8, "一把钻石剑", "A diamond sword")
g("survival", 4, "diamond_pickaxe", 1, 8, "一把钻石镐", "A diamond pickaxe")
g("survival", 4, "diamond_chestplate", 1, 6, "一件钻石胸甲", "A diamond chestplate")
g("survival", 4, "totem_of_undying", "%(1,2)", 4, "一枚不死图腾", "A totem of undying")
e("survival", 4, "strength", "%(60,120)", 2, 10, "蛮力加身", "Brute strength")
e("survival", 4, "regeneration", "%(20,45)", 2, 10, "血肉重生", "Flesh knits")
e("survival", 4, "resistance", "%(60,120)", 2, 8, "钢铁之躯", "A body of steel")
e("survival", 4, "wither", "%(10,20)", 2, 8, "凋零啃噬", "Wither gnaws", "black")
e("survival", 4, "darkness", "%(10,20)", 1, 6, "深渊凝视", "The abyss stares", "black")

g("survival", 5, "netherite_ingot", "%(1,3)", 10, "几块下界合金锭", "Netherite ingots")
g("survival", 5, "netherite_sword", 1, 4, "一把合金剑", "A netherite sword")
g("survival", 5, "netherite_chestplate", 1, 4, "一件合金胸甲", "A netherite chestplate")
g("survival", 5, "elytra", 1, 3, "一副鞘翅", "A pair of elytra")
g("survival", 5, "enchanted_golden_apple", "%(1,3)", 6, "几颗附魔金苹果", "Enchanted golden apples")
g("survival", 5, "totem_of_undying", "%(2,5)", 5, "一小把不死图腾", "A handful of totems")
g("survival", 5, "beacon", 1, 2, "一座信标", "A beacon")
g("survival", 5, "nether_star", 1, 2, "一颗下界之星", "A nether star")
g("survival", 5, "conduit", 1, 3, "一颗潮涌核心", "A conduit")
g("survival", 5, "trident", 1, 3, "一把三叉戟", "A trident")
g("survival", 5, "dragon_egg", 1, 1, "一颗龙蛋", "A dragon egg")
g("survival", 5, "shulker_box", 1, 4, "一个潜影盒", "A shulker box")
g("survival", 5, "ender_chest", 1, 4, "一个末影箱", "An ender chest")
g("survival", 5, "respawn_anchor", "%(1,2)", 3, "一座重生锚", "A respawn anchor")
g("survival", 5, "dragon_breath", "%(8,16)", 8, "一打龙息", "A dozen dragon breaths")
g("survival", 5, "diamond_block", "%(2,4)", 6, "几块钻石块", "A few diamond blocks")
g("survival", 5, "netherite_block", 1, 1, "一块下界合金块", "A netherite block")
loot("survival", 5, "end_city_treasure", 8, "末地城的宝藏", "End city treasure")
loot("survival", 5, "bastion_treasure", 6, "堡垒遗迹的宝藏", "Bastion treasure")
loot("survival", 5, "ancient_city", 5, "远古城市的箱子", "An ancient city chest")
e("survival", 5, "strength", "%(120,300)", 3, 6, "巨力临身", "Titan strength")
e("survival", 5, "regeneration", "%(60,120)", 3, 6, "不死之身", "An undying body")
e("survival", 5, "resistance", "%(120,300)", 3, 4, "铜墙铁壁", "Walls of iron")
damage("survival", 5, "一记重击", "A heavy blow", "%(4,8)", 8)
e("survival", 5, "levitation", "%(10,20)", 2, 4, "失重上升", "Weightless ascent", "black")

g("survival", 6, "netherite_block", "%(1,3)", 3, "几块合金块", "Netherite blocks")
g("survival", 6, "netherite_ingot", "%(4,8)", 4, "一把合金锭", "A stack of netherite")
g("survival", 6, "elytra", 2, 1, "两副鞘翅", "Two pairs of elytra")
g("survival", 6, "totem_of_undying", "%(8,16)", 2, "一堆不死图腾", "A pile of totems")
g("survival", 6, "enchanted_golden_apple", "%(8,16)", 2, "一堆附魔金苹果", "A pile of enchanted apples")
g("survival", 6, "beacon", 2, 2, "两座信标", "Two beacons")
g("survival", 6, "nether_star", 2, 1, "两颗下界之星", "Two nether stars")
g("survival", 6, "conduit", 2, 2, "两颗潮涌核心", "Two conduits")
g("survival", 6, "trident", 2, 2, "两把三叉戟", "Two tridents")
g("survival", 6, "dragon_egg", 2, 1, "两颗龙蛋", "Two dragon eggs")
g("survival", 6, "respawn_anchor", "%(4,8)", 2, "几座重生锚", "Respawn anchors")
g("survival", 6, "ender_chest", "%(4,8)", 2, "几个末影箱", "A few ender chests")
xp("survival", 6, "一大笔经验", "A fortune in experience", "%(2000,5000)", 4)
mob("survival", 6, "ender_dragon", 1, 1, "末影龙降临", "The ender dragon descends", "red", True)
mob("survival", 6, "wither", 1, 2, "凋灵现身", "A wither appears", "black", True)
mob("survival", 6, "elder_guardian", 1, 3, "远古守卫者", "An elder guardian")
mob("survival", 6, "ravager", 1, 3, "劫掠兽冲阵", "A ravager charges")
mob("survival", 6, "vindicator", 3, 3, "卫道士小队", "A vindicator squad")
mob("survival", 6, "phantom", 5, 4, "幻翼盘旋", "Phantoms circle")
damage("survival", 6, "开膛一击", "A deep cut", "%(6,12)", 4)
e("survival", 6, "wither", "%(30,60)", 3, 6, "凋零缠身", "Wither clings", "black")
e("survival", 6, "darkness", "%(30,60)", 3, 6, "永夜降临", "The endless night", "black")

# ---------------------------------------------------------------------------
# Creative pool
# ---------------------------------------------------------------------------
raw("creative", 1, "一件随机小玩意", "A random trinket", 25,
    "give " + PLAYER + " ^(ITEM) %(1,3)")
raw("creative", 1, "一堆随机方块", "A pile of random blocks", 22,
    "give " + PLAYER + " ^(BLOCK) %(4,24)")
raw("creative", 1, "一次随机增益", "A random boon", 18,
    "effect give " + PLAYER + " ^(BUFF) %(10,60) %(0,1)")
raw("creative", 1, "一次随机负面效果", "A random bane", 14,
    "effect give " + PLAYER + " ^(DEBUFF) %(5,20) %(0,1)", "black")
raw("creative", 1, "一声随机响动", "A random sound", 10,
    "playsound ^(SOUND) master " + PLAYER + " ~ ~ ~ 1 1")
raw("creative", 1, "一阵随机粒子", "A swirl of particles", 10,
    "execute at " + PLAYER + " run particle ^(PARTICLE) ~ ~1 ~ 1 1 1 0 24")
g("creative", 1, "name_tag", 1, 12, "一枚命名牌", "A name tag")
g("creative", 1, "saddle", 1, 10, "一副鞍", "A saddle")
mob("creative", 1, "pig", 1, 12, "一头猪", "A pig", "red")
mob("creative", 1, "chicken", 5, 10, "一群鸡", "A flock of chickens", "red")
mob("creative", 1, "bat", 3, 8, "三只蝙蝠", "Three bats", "red")
mob("creative", 1, "slime", 2, 8, "两只史莱姆", "Two slimes", "red")
mob("creative", 1, "rabbit", 3, 8, "三只兔子", "Three rabbits", "red")
raw("creative", 1, "时间到白天", "Daylight returns", 8, "time set day")
raw("creative", 1, "时间到夜晚", "Night falls", 8, "time set night")
raw("creative", 1, "一场细雨", "A light rain", 8, "weather rain")
xp("creative", 1, "一小笔经验", "A little experience", "%(10,100)", 20)
g("creative", 1, "cooked_beef", 1, 1, "一块牛排", "A steak")
g("creative", 1, "golden_apple", 1, 8, "一颗金苹果", "A golden apple")
e("creative", 1, "speed", "%(10,60)", "%(0,1)", 14, "脚下生风", "Fleet of foot")
e("creative", 1, "jump_boost", "%(10,60)", "%(0,1)", 14, "身轻如燕", "Light as a feather")
e("creative", 1, "slow_falling", "%(10,40)", 0, 8, "缓缓落地", "A gentle fall")
e("creative", 1, "glowing", "%(10,40)", 0, 6, "通体发光", "Glowing bright")
e("creative", 1, "blindness", "%(5,15)", 0, 6, "眼前一黑", "Everything goes dark", "black")
e("creative", 1, "nausea", "%(5,15)", 0, 6, "天旋地转", "The world spins", "black")

raw("creative", 2, "一只随机生物", "A random creature", 16,
    "execute at " + PLAYER + " run summon ^(MOB) ~ ~ ~", "black")
raw("creative", 2, "四件随机物品", "Four random items", 14,
    "give " + PLAYER + " ^(ITEM) 4")
raw("creative", 2, "一瓶随机药水", "A random potion", 12,
    "give " + PLAYER + " minecraft:potion{Potion:\"^(POTION)\"} 1")
raw("creative", 2, "一本随机附魔书", "A random enchanted book", 10,
    "give " + PLAYER + " ^(ENCHANTED_BOOK) 1")
raw("creative", 2, "十六块随机方块", "Sixteen random blocks", 10,
    "give " + PLAYER + " ^(BLOCK) 16")
g("creative", 2, "ender_pearl", "%(2,8)", 16, "几颗末影珍珠", "A few ender pearls")
g("creative", 2, "chorus_fruit", "%(4,16)", 12, "几颗紫颂果", "A few chorus fruits")
g("creative", 2, "tnt", "%(1,4)", 10, "几块炸药", "A few blocks of TNT", "black")
g("creative", 2, "water_bucket", 1, 10, "一桶水", "A bucket of water")
raw("creative", 2, "短距闪现", "A short blink", 12,
    "tp " + PLAYER + " ~%(5,15) ~ ~%(5,15)")
e("creative", 2, "levitation", "%(5,15)", 1, 8, "轻飘飘", "Drifting upward")
e("creative", 2, "slow_falling", "%(10,40)", 1, 8, "羽落", "Feather fall")
e("creative", 2, "speed", "%(20,60)", 2, 8, "疾风步", "Wind step")
e("creative", 2, "jump_boost", "%(20,60)", 2, 8, "弹跳本能", "Spring legs")
loot("creative", 2, "village_armorer", 10, "村庄盔甲匠的箱子", "A village armorer chest")
loot("creative", 2, "desert_pyramid", 8, "沙漠神殿的箱子", "A desert pyramid chest")
g("creative", 2, "saddle", 1, 8, "一副旧鞍", "An old saddle")
g("creative", 2, "name_tag", 1, 8, "一枚空白命名牌", "A blank name tag")
mob("creative", 2, "parrot", 2, 8, "两只鹦鹉", "Two parrots", "red")
mob("creative", 2, "cat", 2, 8, "两只猫", "Two cats", "red")
mob("creative", 2, "fox", 2, 8, "两只狐狸", "Two foxes", "red")
mob("creative", 2, "panda", 1, 6, "一只熊猫", "A panda", "red")

raw("creative", 3, "三本随机附魔书", "Three random enchanted books", 10,
    "give " + PLAYER + " ^(ENCHANTED_BOOK) 3")
raw("creative", 3, "一本随机增益附魔书", "A random blessed book", 8,
    "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"^(BUFF_ENCHANTMENT)\",lvl:%(1,5)s}]} 1")
raw("creative", 3, "一本随机诅咒书", "A random cursed book", 6,
    "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"^(CURSE)\",lvl:%(1,3)s}]} 1", "black")
raw("creative", 3, "三瓶随机药水", "Three random potions", 8,
    "give " + PLAYER + " minecraft:potion{Potion:\"^(POTION)\"} 3")
raw("creative", 3, "一次长效增益", "A lasting boon", 8,
    "effect give " + PLAYER + " ^(BUFF) %(60,180) %(1,2)")
raw("creative", 3, "八件随机物品", "Eight random items", 10,
    "give " + PLAYER + " ^(ITEM) 8")
raw("creative", 3, "三十二块随机方块", "Thirty-two random blocks", 8,
    "give " + PLAYER + " ^(BLOCK) 32")
raw("creative", 3, "三只随机生物", "Three random creatures", 6,
    "\n".join(["execute at " + PLAYER + " run summon ^(MOB) ~ ~ ~"] * 3), "black")
g("creative", 3, "shulker_box", 1, 6, "一个潜影盒", "A shulker box")
g("creative", 3, "ender_chest", 1, 6, "一个末影箱", "An ender chest")
g("creative", 3, "enchanting_table", 1, 4, "一张附魔台", "An enchanting table")
g("creative", 3, "beacon", 1, 3, "一座信标", "A beacon")
g("creative", 3, "trident", 1, 4, "一把三叉戟", "A trident")
g("creative", 3, "elytra", 1, 3, "一副鞘翅", "A pair of elytra")
raw("creative", 3, "一场粒子雨", "A rain of particles", 6,
    "execute at " + PLAYER + " run particle ^(PARTICLE) ~ ~10 ~ 3 3 3 0 60")
raw("creative", 3, "一声怪响", "An odd sound", 6,
    "playsound ^(SOUND) master " + PLAYER + " ~ ~ ~ 1 0.5")
raw("creative", 3, "中距传送", "A medium blink", 6,
    "tp " + PLAYER + " ~%(10,30) ~ ~%(10,30) %(0,1)")
e("creative", 3, "levitation", "%(10,30)", 2, 6, "升空", "Lift off")
e("creative", 3, "blindness", "%(15,30)", 0, 5, "致盲一闪", "A flash of blindness", "black")
e("creative", 3, "nausea", "%(15,30)", 0, 5, "头重脚轻", "Everything sways", "black")
e("creative", 3, "hunger", "%(30,60)", 2, 5, "饿意翻涌", "A wave of hunger", "black")
e("creative", 3, "weakness", "%(30,60)", 1, 5, "浑身乏力", "All strength drains", "black")
damage("creative", 3, "一阵剧痛", "A sharp pain", "%(2,6)", 5)

raw("creative", 4, "天雷落下", "Lightning falls", 8,
    "execute at " + PLAYER + " run summon minecraft:lightning_bolt ~ ~ ~", "black")
raw("creative", 4, "雷暴天气", "A thunderstorm", 6, "weather thunder")
raw("creative", 4, "烟花风暴", "A storm of sparks", 8,
    "execute at " + PLAYER + " run particle minecraft:firework ~ ~3 ~ 2 2 2 0 120")
g("creative", 4, "tnt", "%(2,6)", 6, "一堆炸药", "A pile of TNT", "black")
mob("creative", 4, "ender_dragon", 1, 1, "末影龙盘旋", "The ender dragon circles", "red", True)
mob("creative", 4, "wither", 1, 2, "凋灵降世", "A wither is born", "black", True)
mob("creative", 4, "ravager", 2, 4, "两只劫掠兽", "Two ravagers")
mob("creative", 4, "vindicator", 5, 4, "五个卫道士", "Five vindicators")
mob("creative", 4, "phantom", 6, 4, "八只幻翼", "Eight phantoms")
mob("creative", 4, "magma_cube", 6, 4, "六只岩浆怪", "Six magma cubes")
mob("creative", 4, "ghast", 2, 4, "两只恶魂", "Two ghasts")
mob("creative", 4, "blaze", 4, 4, "四只烈焰人", "Four blazes")
mob("creative", 4, "enderman", 6, 4, "八只末影人", "Eight endermen")
xp("creative", 4, "一笔大经验", "A large pile of experience", "%(1000,3000)", 6)
g("creative", 4, "enchanted_golden_apple", "%(2,6)", 5, "几颗附魔金苹果", "Enchanted apples")
g("creative", 4, "totem_of_undying", "%(2,4)", 5, "几枚不死图腾", "A few totems")
g("creative", 4, "netherite_ingot", "%(2,6)", 5, "几块合金锭", "Netherite ingots")
g("creative", 4, "diamond_block", "%(4,8)", 4, "几块钻石块", "Diamond blocks")
raw("creative", 4, "远距传送", "A long blink", 5,
    "tp " + PLAYER + " ~%(20,60) ~ ~%(20,60)")
e("creative", 4, "slow_falling", "%(30,80)", 2, 4, "云端漫步", "Walking on clouds")
e("creative", 4, "resistance", "%(60,120)", 3, 4, "金钟罩", "A golden bell")
damage("creative", 4, "震荡冲击", "A shockwave", "%(3,8)", 4)

g("creative", 5, "tnt", "%(4,10)", 5, "一堆烈性炸药", "A pile of high explosives", "black")
raw("creative", 5, "脚下岩浆", "Lava underfoot", 5,
    "execute at " + PLAYER + " run setblock ~ ~-1 ~ minecraft:lava", "black")
mob("creative", 5, "wither", 2, 2, "双生凋灵", "Twin withers", "black", True)
mob("creative", 5, "ender_dragon", 1, 2, "末影龙怒吼", "The ender dragon roars", "red", True)
mob("creative", 5, "ravager", 4, 3, "劫掠兽群", "A ravager herd")
mob("creative", 5, "elder_guardian", 2, 3, "两位远古守卫者", "Two elder guardians")
mob("creative", 5, "phantom", 6, 3, "幻翼成群", "A host of phantoms")
mob("creative", 5, "creeper", 5, 3, "苦力怕围场", "A creeper pen")
mob("creative", 5, "magma_cube", 6, 3, "岩浆怪潮", "A magma tide")
mob("creative", 5, "blaze", 5, 3, "烈焰人阵", "A blaze formation")
raw("creative", 5, "雷暴之夜", "A night of storms", 4, "weather thunder")
raw("creative", 5, "连环落雷", "A chain of lightning", 3,
    "execute at @a run summon minecraft:lightning_bolt ~ ~ ~", "black")
e("creative", 5, "darkness", "%(30,60)", 2, 5, "无光之夜", "A night without light", "black")
e("creative", 5, "wither", "%(20,40)", 3, 5, "凋零风暴", "A withering storm", "black")
damage("creative", 5, "巨力一击", "A massive blow", "%(6,12)", 4)
e("creative", 5, "levitation", "%(20,40)", 3, 4, "天际漂流", "Adrift in the sky", "black")
e("creative", 5, "weakness", "%(60,120)", 3, 4, "力量抽离", "Strength pulled away", "black")
e("creative", 5, "slowness", "%(60,120)", 3, 4, "泥沼缠足", "Mired in mud", "black")
e("creative", 5, "nausea", "%(30,60)", 3, 4, "天地倒转", "The world turns over", "black")
e("creative", 5, "blindness", "%(20,40)", 2, 4, "长夜无边", "Endless dark", "black")
raw("creative", 5, "命运宣判", "Fate passes sentence", 0.01,
    "kill " + PLAYER, "black", True)

raw("creative", 6, "恶魔的礼包", "The devil's bundle", 8,
    "give " + PLAYER + " ^(ITEM) %(16,64)", "red", True)
raw("creative", 6, "十二本附魔书", "Twelve enchanted books", 5,
    "give " + PLAYER + " ^(ENCHANTED_BOOK) 12")
raw("creative", 6, "五只随机生物", "Five random creatures", 5,
    "\n".join(["execute at " + PLAYER + " run summon ^(MOB) ~ ~ ~"] * 5), "black")
raw("creative", 6, "六十四块随机方块", "Sixty-four random blocks", 5,
    "give " + PLAYER + " ^(BLOCK) 64")
raw("creative", 6, "五瓶随机药水", "Five random potions", 4,
    "give " + PLAYER + " minecraft:potion{Potion:\"^(POTION)\"} 5")
g("creative", 6, "netherite_block", "%(2,6)", 3, "几块合金块", "Netherite blocks")
g("creative", 6, "nether_star", "%(2,5)", 2, "几颗下界之星", "A few nether stars")
g("creative", 6, "dragon_egg", "%(2,5)", 2, "几颗龙蛋", "A few dragon eggs")
g("creative", 6, "elytra", "%(2,4)", 2, "几副鞘翅", "A few elytra")
g("creative", 6, "enchanted_golden_apple", "%(8,16)", 2, "一堆附魔金苹果", "A pile of enchanted apples")
g("creative", 6, "totem_of_undying", "%(8,16)", 2, "一堆不死图腾", "A pile of totems")
xp("creative", 6, "巨额经验", "A mountain of experience", "%(5000,20000)", 3)
raw("creative", 6, "全服经验雨", "Experience for everyone", 2,
    "experience add @a %(20,50) levels", "red", True)
raw("creative", 6, "全服祝福", "A blessing for everyone", 2,
    "execute as @a run effect give @s minecraft:strength %(60,120) 2")
damage("creative", 6, "全服震荡", "A shock for everyone", "%(2,6)", 2,
       "black", True)
raw("creative", 6, "全服落雷", "Lightning for everyone", 2,
    "execute at @a run summon minecraft:lightning_bolt ~ ~ ~", "black", True)
mob("creative", 6, "wither", 3, 1, "三只凋灵", "Three withers", "black", True)
mob("creative", 6, "ender_dragon", 2, 1, "双龙现世", "Two dragons", "red", True)
raw("creative", 6, "恶魔收账", "The devil collects", 0.01,
    "kill " + PLAYER, "black", True)

# ---------------------------------------------------------------------------
# Validation and emit
# ---------------------------------------------------------------------------
NUMBER_TOKEN = re.compile(r"%\((-?\d{1,7}),(-?\d{1,7})\)")
COMMAND_ROOTS = {"give", "effect", "experience", "summon", "execute", "loot", "playsound",
                 "particle", "weather", "time", "tp", "clear", "setblock", "fill", "damage", "kill"}
SAMPLE_TYPES = {"ITEM", "BLOCK", "ENTITY", "MOB", "EFFECT", "ENCHANTMENT", "POTION", "SOUND",
                "PARTICLE", "PAINTING", "ATTRIBUTE", "FLUID", "BIOME", "STRUCTURE", "DAMAGE_TYPE",
                "BUFF", "DEBUFF", "NEUBUFF", "BUFF_ENCHANTMENT", "CURSE", "ENCHANTED_BOOK"}


def fail(message):
    raise SystemExit("preset pool invalid: " + message)


def validate():
    counts = {}
    pool_ids = {row[0] for row in POOLS}

    for pool, tier, nzh, nen, tzh, ten, weight, color, major, command in ROWS:
        if pool not in pool_ids:
            fail("unknown pool " + pool)
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

            for match in NUMBER_TOKEN.finditer(line):
                low = int(match.group(1))
                high = int(match.group(2))

                if low >= high:
                    fail("empty number range " + match.group() + " in " + nzh)

                if abs(low) > 1000000 or abs(high) > 1000000:
                    fail("number range out of bounds in " + nzh)

            if line.count("{") != line.count("}"):
                fail("unbalanced nbt braces in " + nzh)

        counts[(pool, tier)] = counts.get((pool, tier), 0) + 1

    for pool, _nzh, _nen, _dzh, _den in POOLS:
        for tier in range(1, 7):
            if counts.get((pool, tier), 0) < 8:
                fail("pool " + pool + " region " + str(tier) + " has only "
                     + str(counts.get((pool, tier), 0)) + " entries")

    return counts


def jstr(value):
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""


def emit():
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
    lines.append(" * Preset prize pools, generated by scripts/build_preset_pool.py.")
    lines.append(" * Do not edit by hand: change the script and run it again.")
    lines.append(" *")
    lines.append(" * POOLS rows: id, Chinese name, English name, Chinese description, English description.")
    lines.append(" * ENTRIES rows: pool id, tier, Chinese name, English name, Chinese text, English text,")
    lines.append(" * weight, colour, major event, command.")
    lines.append(" */")
    lines.append("final class DefaultPrizePoolData")
    lines.append("{")
    lines.append("    static final String[][] POOLS =")
    lines.append("    {")
    for pool, nzh, nen, dzh, den in POOLS:
        lines.append("        {" + ", ".join([jstr(pool), jstr(nzh), jstr(nen), jstr(dzh), jstr(den)]) + "},")
    lines.append("    };")
    lines.append("")
    lines.append("    static final String[][] ENTRIES =")
    lines.append("    {")

    for pool, tier, nzh, nen, tzh, ten, weight, color, major, command in ROWS:
        cells = [jstr(pool), jstr(str(tier)), jstr(nzh), jstr(nen), jstr(tzh), jstr(ten),
                 jstr(str(weight)), jstr(color), jstr("true" if major else "false"), jstr(command)]
        lines.append("        {" + ", ".join(cells) + "},")

    lines.append("    };")
    lines.append("")
    lines.append("    private DefaultPrizePoolData()")
    lines.append("    {")
    lines.append("    }")
    lines.append("}")
    lines.append("")
    OUT.write_text("\r\n".join(lines), encoding="utf-8")


counts = validate()

if "--check" not in sys.argv:
    emit()
    print("wrote", len(ROWS), "entries")

for pool, _nzh, _nen, _dzh, _den in POOLS:
    print(pool, [counts.get((pool, tier), 0) for tier in range(1, 7)])
