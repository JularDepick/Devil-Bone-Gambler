"""Content tables and entry builders for the v0.8.0 preset pools.

The tables hold the bilingual names of the vanilla things an entry can hand out, summon or apply, and the builders turn
one table row into several entries by walking a few parameter axes (amount, duration, strength). Every entry keeps the
row shape the builder in build_preset_pool.py expects:

    (pool, tier, name_zh, name_en, notify_zh, notify_en, weight, colour, major, command)

Names stay readable and unique per pool because the axis word is part of the name, for example a small bag of bread
becomes 一袋面包 / A bag of bread, while two whole stacks of the same thing get their own wording.
"""

PLAYER = "$" + "{player}"

# id, Chinese, English
ITEMS = [
    ("bread", "面包", "Bread"),
    ("baked_potato", "烤马铃薯", "Baked Potato"),
    ("apple", "苹果", "Apple"),
    ("carrot", "胡萝卜", "Carrot"),
    ("golden_carrot", "金胡萝卜", "Golden Carrot"),
    ("cooked_beef", "牛排", "Steak"),
    ("cooked_porkchop", "熟猪排", "Cooked Porkchop"),
    ("cooked_mutton", "熟羊肉", "Cooked Mutton"),
    ("cooked_chicken", "熟鸡肉", "Cooked Chicken"),
    ("cooked_cod", "熟鳕鱼", "Cooked Cod"),
    ("cookie", "曲奇", "Cookie"),
    ("melon_slice", "西瓜片", "Melon Slice"),
    ("sweet_berries", "甜浆果", "Sweet Berries"),
    ("honey_bottle", "蜂蜜瓶", "Honey Bottle"),
    ("milk_bucket", "牛奶桶", "Milk Bucket"),
    ("torch", "火把", "Torch"),
    ("lantern", "灯笼", "Lantern"),
    ("cobblestone", "圆石", "Cobblestone"),
    ("stone", "石头", "Stone"),
    ("oak_planks", "橡木木板", "Oak Planks"),
    ("spruce_planks", "云杉木板", "Spruce Planks"),
    ("oak_log", "橡木原木", "Oak Log"),
    ("stick", "木棍", "Stick"),
    ("coal", "煤炭", "Coal"),
    ("charcoal", "木炭", "Charcoal"),
    ("iron_ingot", "铁锭", "Iron Ingot"),
    ("gold_ingot", "金锭", "Gold Ingot"),
    ("copper_ingot", "铜锭", "Copper Ingot"),
    ("lapis_lazuli", "青金石", "Lapis Lazuli"),
    ("redstone", "红石", "Redstone Dust"),
    ("quartz", "下界石英", "Nether Quartz"),
    ("amethyst_shard", "紫水晶碎片", "Amethyst Shard"),
    ("emerald", "绿宝石", "Emerald"),
    ("diamond", "钻石", "Diamond"),
    ("netherite_scrap", "下界合金碎片", "Netherite Scrap"),
    ("ancient_debris", "远古残骸", "Ancient Debris"),
    ("blaze_rod", "烈焰棒", "Blaze Rod"),
    ("ender_pearl", "末影珍珠", "Ender Pearl"),
    ("ender_eye", "末影之眼", "Eye of Ender"),
    ("shulker_shell", "潜影壳", "Shulker Shell"),
    ("phantom_membrane", "幻翼膜", "Phantom Membrane"),
    ("slime_ball", "黏液球", "Slimeball"),
    ("glowstone_dust", "荧石粉", "Glowstone Dust"),
    ("gunpowder", "火药", "Gunpowder"),
    ("string", "线", "String"),
    ("leather", "皮革", "Leather"),
    ("flint", "燧石", "Flint"),
    ("feather", "羽毛", "Feather"),
    ("bone", "骨头", "Bone"),
    ("sugar_cane", "甘蔗", "Sugar Cane"),
    ("wheat", "小麦", "Wheat"),
    ("egg", "鸡蛋", "Egg"),
    ("clay_ball", "黏土球", "Clay Ball"),
    ("gravel", "沙砾", "Gravel"),
    ("sand", "沙子", "Sand"),
    ("dirt", "泥土", "Dirt"),
    ("glass", "玻璃", "Glass"),
    ("hay_block", "干草捆", "Hay Bale"),
    ("iron_block", "铁块", "Block of Iron"),
    ("iron_sword", "铁剑", "Iron Sword"),
    ("iron_pickaxe", "铁镐", "Iron Pickaxe"),
    ("iron_axe", "铁斧", "Iron Axe"),
    ("iron_shovel", "铁锹", "Iron Shovel"),
    ("iron_helmet", "铁头盔", "Iron Helmet"),
    ("iron_chestplate", "铁胸甲", "Iron Chestplate"),
    ("iron_leggings", "铁护腿", "Iron Leggings"),
    ("iron_boots", "铁靴子", "Iron Boots"),
    ("diamond_sword", "钻石剑", "Diamond Sword"),
    ("diamond_pickaxe", "钻石镐", "Diamond Pickaxe"),
    ("diamond_chestplate", "钻石胸甲", "Diamond Chestplate"),
    ("bow", "弓", "Bow"),
    ("crossbow", "弩", "Crossbow"),
    ("arrow", "箭", "Arrow"),
    ("shield", "盾牌", "Shield"),
    ("fishing_rod", "钓鱼竿", "Fishing Rod"),
    ("bucket", "桶", "Bucket"),
    ("water_bucket", "水桶", "Water Bucket"),
    ("shears", "剪刀", "Shears"),
    ("compass", "指南针", "Compass"),
    ("clock", "钟", "Clock"),
    ("map", "空地图", "Empty Map"),
    ("totem_of_undying", "不死图腾", "Totem of Undying"),
    ("enchanted_golden_apple", "附魔金苹果", "Enchanted Golden Apple"),
    ("golden_apple", "金苹果", "Golden Apple"),
    ("beacon", "信标", "Beacon"),
    ("nether_star", "下界之星", "Nether Star"),
    ("elytra", "鞘翅", "Elytra"),
    ("trident", "三叉戟", "Trident"),
    ("conduit", "潮涌核心", "Conduit"),
    ("sponge", "海绵", "Sponge"),
    ("sea_lantern", "海晶灯", "Sea Lantern"),
    ("prismarine_shard", "海晶碎片", "Prismarine Shard"),
    ("heart_of_the_sea", "海洋之心", "Heart of the Sea"),
    ("turtle_helmet", "海龟壳", "Turtle Shell"),
    ("nautilus_shell", "鹦鹉螺壳", "Nautilus Shell"),
    ("rabbit_foot", "兔子脚", "Rabbit's Foot"),
    ("dragon_breath", "龙息", "Dragon's Breath"),
    ("wither_skeleton_skull", "凋灵骷髅头颅", "Wither Skeleton Skull"),
    ("shulker_box", "潜影盒", "Shulker Box"),
    ("ender_chest", "末影箱", "Ender Chest"),
    ("respawn_anchor", "重生锚", "Respawn Anchor"),
    ("name_tag", "命名牌", "Name Tag"),
    ("saddle", "鞍", "Saddle"),
    ("lead", "拴绳", "Lead"),
    ("experience_bottle", "附魔之瓶", "Bottle o' Enchanting"),
]

# id, Chinese, English
MOBS = [
    ("cow", "牛", "Cow"),
    ("sheep", "羊", "Sheep"),
    ("pig", "猪", "Pig"),
    ("chicken", "鸡", "Chicken"),
    ("rabbit", "兔子", "Rabbit"),
    ("horse", "马", "Horse"),
    ("donkey", "驴", "Donkey"),
    ("llama", "羊驼", "Llama"),
    ("wolf", "狼", "Wolf"),
    ("cat", "猫", "Cat"),
    ("bee", "蜜蜂", "Bee"),
    ("goat", "山羊", "Goat"),
    ("zombie", "僵尸", "Zombie"),
    ("skeleton", "骷髅", "Skeleton"),
    ("spider", "蜘蛛", "Spider"),
    ("creeper", "苦力怕", "Creeper"),
    ("husk", "尸壳", "Husk"),
    ("stray", "流浪者", "Stray"),
    ("drowned", "溺尸", "Drowned"),
    ("witch", "女巫", "Witch"),
    ("pillager", "掠夺者", "Pillager"),
    ("vindicator", "卫道士", "Vindicator"),
    ("ravager", "劫掠兽", "Ravager"),
    ("phantom", "幻翼", "Phantom"),
    ("blaze", "烈焰人", "Blaze"),
    ("magma_cube", "岩浆怪", "Magma Cube"),
    ("piglin", "猪灵", "Piglin"),
    ("hoglin", "疣猪兽", "Hoglin"),
    ("enderman", "末影人", "Enderman"),
    ("shulker", "潜影贝", "Shulker"),
    ("elder_guardian", "远古守卫者", "Elder Guardian"),
    ("wither", "凋灵", "Wither"),
    ("ender_dragon", "末影龙", "Ender Dragon"),
]

# id, Chinese, English, harmful
EFFECTS = [
    ("strength", "力量", "Strength", False),
    ("speed", "迅捷", "Speed", False),
    ("haste", "急迫", "Haste", False),
    ("regeneration", "生命恢复", "Regeneration", False),
    ("resistance", "抗性提升", "Resistance", False),
    ("fire_resistance", "抗火", "Fire Resistance", False),
    ("water_breathing", "水下呼吸", "Water Breathing", False),
    ("night_vision", "夜视", "Night Vision", False),
    ("absorption", "伤害吸收", "Absorption", False),
    ("jump_boost", "跳跃提升", "Jump Boost", False),
    ("luck", "幸运", "Luck", False),
    ("health_boost", "生命提升", "Health Boost", False),
    ("saturation", "饱和", "Saturation", False),
    ("slow_falling", "缓降", "Slow Falling", False),
    ("conduit_power", "潮涌能量", "Conduit Power", False),
    ("dolphins_grace", "海豚的恩惠", "Dolphin's Grace", False),
    ("poison", "中毒", "Poison", True),
    ("wither", "凋零", "Wither", True),
    ("slowness", "缓慢", "Slowness", True),
    ("weakness", "虚弱", "Weakness", True),
    ("blindness", "失明", "Blindness", True),
    ("nausea", "反胃", "Nausea", True),
    ("hunger", "饥饿", "Hunger", True),
    ("mining_fatigue", "挖掘疲劳", "Mining Fatigue", True),
    ("levitation", "漂浮", "Levitation", True),
    ("darkness", "黑暗", "Darkness", True),
    ("glowing", "发光", "Glowing", True),
]

# table, Chinese, English
LOOT = [
    ("simple_dungeon", "地牢的箱子", "A dungeon chest"),
    ("abandoned_mineshaft", "废弃矿井的箱子", "A mineshaft chest"),
    ("village_weaponsmith", "村庄武器匠的箱子", "A village weaponsmith chest"),
    ("village_armorer", "村庄盔甲匠的箱子", "A village armorer chest"),
    ("desert_pyramid", "沙漠神殿的箱子", "A desert temple chest"),
    ("jungle_temple", "丛林神庙的箱子", "A jungle temple chest"),
    ("shipwreck_treasure", "沉船的宝藏", "Shipwreck treasure"),
    ("buried_treasure", "埋藏的宝藏", "Buried treasure"),
    ("woodland_mansion", "林地府邸的箱子", "A woodland mansion chest"),
    ("nether_bridge", "下界要塞的箱子", "A nether fortress chest"),
    ("bastion_treasure", "堡垒遗迹的宝藏", "Bastion treasure"),
    ("end_city_treasure", "末地城的宝藏", "End city treasure"),
    ("ancient_city", "远古城市的箱子", "An ancient city chest"),
]

# amount word, Chinese prefix, English prefix, (low, high) count range
AMOUNTS = [
    ("few", "一小把", "A small handful of", (2, 4)),
    ("some", "一些", "Some", (4, 8)),
    ("bag", "一袋", "A bag of", (8, 16)),
    ("batch", "一批", "A batch of", (16, 32)),
    ("pile", "一堆", "A pile of", (32, 48)),
    ("chest", "一整箱", "A chest of", (48, 64)),
]


def give(pool, tier, item, amount, weight, colour, major=False):
    item_id, name_zh, name_en = item
    key, prefix_zh, prefix_en, low_high = amount
    low = low_high[0]
    high = low_high[1]
    name_zh_full = prefix_zh + name_zh
    name_en_full = prefix_en + " " + name_en
    command = "give " + PLAYER + " minecraft:" + item_id + " %(" + str(low) + "," + str(high) + ")"
    return (pool, tier, name_zh_full, name_en_full, name_zh_full, name_en_full, weight, colour, major, command)


def effect(pool, tier, entry, seconds, amplifier, weight):
    effect_id, name_zh, name_en, harmful = entry
    colour = "black" if harmful else "red"
    text_zh = name_zh + " " + str(amplifier + 1) + " 级, " + str(seconds[0]) + " 到 " + str(seconds[1]) + " 秒"
    text_en = name_en + " " + str(amplifier + 1) + ", " + str(seconds[0]) + " to " + str(seconds[1]) + " seconds"
    command = ("effect give " + PLAYER + " minecraft:" + effect_id + " %(" + str(seconds[0]) + ","
               + str(seconds[1]) + ") " + str(amplifier))
    return (pool, tier, text_zh, text_en, text_zh, text_en, weight, colour, False, command)


def summon(pool, tier, mob, count, weight, colour):
    mob_id, name_zh, name_en = mob
    name_zh_full = "召唤 " + name_zh + " " + str(count) + " 只"
    name_en_full = "Summon " + name_en + " x" + str(count)
    line = "execute at " + PLAYER + " run summon minecraft:" + mob_id + " ~ ~ ~"
    return (pool, tier, name_zh_full, name_en_full, name_zh_full, name_en_full, weight, colour, False,
            "\n".join([line] * count))


def loot(pool, tier, table, weight):
    table_id, name_zh, name_en = table
    return (pool, tier, name_zh, name_en, name_zh, name_en, weight, "red", False,
            "loot give " + PLAYER + " loot minecraft:chests/" + table_id)


def xp(pool, tier, amount, weight):
    text_zh = str(amount) + " 点经验"
    text_en = str(amount) + " experience points"
    return (pool, tier, text_zh, text_en, text_zh, text_en, weight, "red", False,
            "experience add " + PLAYER + " " + str(amount) + " points")


def damage(pool, tier, name_zh, name_en, low, high, weight):
    return (pool, tier, name_zh, name_en, name_zh, name_en, weight, "black", False,
            "damage " + PLAYER + " %(" + str(low) + "," + str(high) + ")")


def survival_tier_one():
    """The camp fire supply region: warm food, light, the first tools and a few small misfortunes."""
    rows = []
    foods = [item for item in ITEMS if item[0] in (
        "bread", "baked_potato", "apple", "carrot", "cooked_beef", "cooked_porkchop", "cooked_mutton",
        "cooked_chicken", "cookie", "melon_slice", "sweet_berries", "cooked_cod")]
    supplies = [item for item in ITEMS if item[0] in (
        "torch", "lantern", "cobblestone", "stone", "oak_planks", "oak_log", "stick", "coal", "charcoal",
        "dirt", "sand", "gravel", "clay_ball")]

    for item in foods:
        for amount in AMOUNTS:
            rows.append(give("survival", 1, item, amount, 120, "red"))

    for item in supplies:
        for amount in AMOUNTS:
            rows.append(give("survival", 1, item, amount, 90, "red"))

    for entry in EFFECTS:
        if entry[3]:
            continue
        rows.append(effect("survival", 1, entry, (30, 90), 0, 40))

    for entry in [row for row in EFFECTS if row[3]][:6]:
        rows.append(effect("survival", 1, entry, (10, 25), 0, 25))

    for mob in MOBS[:8]:
        rows.append(summon("survival", 1, mob, 2, 30, "red"))

    for table in LOOT[:5]:
        rows.append(loot("survival", 1, table, 20))

    for amount in (30, 60, 90, 120):
        rows.append(xp("survival", 1, amount, 20))

    rows.append(damage("survival", 1, "脚下的裂缝", "A crack underfoot", 1, 3, 25))

    return rows

# Theme short name of every region: it opens the entry name, so an item may appear in two regions without colliding.
THEMES = {
    "survival": {
        1: ("篝火", "Campfire"),
        2: ("铁匠", "Smithy"),
        3: ("矿脉", "Vein"),
        4: ("深岩", "Deepslate"),
        5: ("远征", "Expedition"),
        6: ("极限", "Endgame"),
    },
    "creative": {
        1: ("小丑", "Jester"),
        2: ("天候", "Weather"),
        3: ("空投", "Airdrop"),
        4: ("剧场", "Theatre"),
        5: ("诅咒", "Curse"),
        6: ("终局", "Finale"),
    },
}

# How many entries of one region each category must hold. The item rows carry the bulk, the rest adds the variety.
QUOTA = {
    "items": 300,
    "effects": 70,
    "mobs": 40,
    "loot": 30,
    "xp": 20,
    "damage": 20,
    "gear": 20,
}
TIER_ENTRIES = 500


def _spread(total, slots):
    """Splits a total over the given slots: the first ones one extra, and zero is allowed."""
    if not slots:
        return []

    base = total // len(slots)
    extra = total % len(slots)
    return [base + (1 if index < extra else 0) for index in range(len(slots))]


def _named(pool, tier, body_zh, body_en):
    theme_zh, theme_en = THEMES[pool][tier]
    return theme_zh + "-" + body_zh, theme_en + " " + body_en


# The bigger the portion, the rarer it is: a handful is common, a cartload is nearly a legend.
BUCKET_WEIGHTS = (120, 120, 60, 60, 25, 25, 10, 3)


def item_rows(pool, tier, item_ids, count, weight):
    theme_zh, theme_en = THEMES[pool][tier]
    items = [item for item in ITEMS if item[0] in item_ids]
    rows = []

    for item, buckets in zip(items, _spread(count, items)):
        for bucket_index, amount in enumerate(AMOUNTS[:buckets]):
            name_zh, name_en = _named(pool, tier, amount[1] + item[1], amount[2] + " " + item[2])
            command = ("give " + PLAYER + " minecraft:" + item[0] + " %(" + str(amount[3][0]) + ","
                       + str(amount[3][1]) + ")")
            rows.append((pool, tier, name_zh, name_en, name_zh, name_en,
                         BUCKET_WEIGHTS[min(bucket_index, len(BUCKET_WEIGHTS) - 1)], "red", False, command))

    while len(rows) < count:
        rows.append(rows[len(rows) % max(len(rows), 1)])

    return rows[:count]


def effect_rows(pool, tier, plan, weight):
    rows = []

    for effect_id, seconds, amplifier in plan:
        entry = next(item for item in EFFECTS if item[0] == effect_id)
        name_zh, name_en = _named(pool, tier, entry[1] + " " + str(amplifier + 1) + " 级",
                                  entry[2] + " " + str(amplifier + 1))
        command = ("effect give " + PLAYER + " minecraft:" + effect_id + " %(" + str(seconds[0]) + ","
                   + str(seconds[1]) + ") " + str(amplifier))
        colour = "black" if entry[3] else "red"
        step = weight if amplifier < 2 else (25 if amplifier == 2 else (10 if amplifier == 3 else 3))
        rows.append((pool, tier, name_zh, name_en, name_zh, name_en, step, colour, False, command))

    return rows


def mob_rows(pool, tier, plan, weight):
    rows = []

    for mob_id, count in plan:
        entry = next(item for item in MOBS if item[0] == mob_id)
        name_zh, name_en = _named(pool, tier, entry[1] + " " + str(count) + " 只", entry[2] + " x" + str(count))
        line = "execute at " + PLAYER + " run summon minecraft:" + mob_id + " ~ ~ ~"
        colour = "black" if mob_id in ("zombie", "skeleton", "spider", "creeper", "husk", "stray", "drowned",
                                       "witch", "pillager", "vindicator", "ravager", "phantom", "blaze",
                                       "magma_cube", "hoglin", "wither", "ender_dragon") else "red"
        rows.append((pool, tier, name_zh, name_en, name_zh, name_en, weight, colour, False, "\n".join([line] * count)))

    return rows


def loot_rows(pool, tier, table_ids, weight):
    rows = []

    for table_id in table_ids:
        entry = next(item for item in LOOT if item[0] == table_id)
        name_zh, name_en = _named(pool, tier, entry[1], entry[2])
        rows.append((pool, tier, name_zh, name_en, name_zh, name_en, weight, "red", False,
                     "loot give " + PLAYER + " loot minecraft:chests/" + table_id))

    return rows


def xp_rows(pool, tier, amounts, weight):
    rows = []

    for amount in amounts:
        name_zh, name_en = _named(pool, tier, str(amount) + " 点经验", str(amount) + " experience points")
        rows.append((pool, tier, name_zh, name_en, name_zh, name_en, weight, "red", False,
                     "experience add " + PLAYER + " " + str(amount) + " points"))

    return rows


def gear_rows(pool, tier, plan, weight):
    rows = []

    for item_id, enchantment, levels in plan:
        entry = next(item for item in ITEMS if item[0] == item_id)
        enchant_zh = {"sharpness": "锋利", "protection": "保护", "efficiency": "效率",
                      "unbreaking": "耐久", "fortune": "时运", "power": "力量"}[enchantment]
        enchant_en = {"sharpness": "Sharpness", "protection": "Protection", "efficiency": "Efficiency",
                      "unbreaking": "Unbreaking", "fortune": "Fortune", "power": "Power"}[enchantment]
        name_zh, name_en = _named(pool, tier, enchant_zh + " " + entry[1] + " " + str(levels[0]) + " 到 "
                                  + str(levels[1]) + " 级",
                                  enchant_en + " " + entry[2] + " " + str(levels[0]) + " to " + str(levels[1]))
        command = ("give " + PLAYER + " minecraft:" + item_id + "{Enchantments:[{id:\"" + enchantment
                   + "\",lvl:%(" + str(levels[0]) + "," + str(levels[1]) + ")}]}")
        rows.append((pool, tier, name_zh, name_en, name_zh, name_en, weight, "red", False, command))

    return rows


def damage_rows(pool, tier, plan, weight):
    rows = []

    for name_zh, name_en, low, high in plan:
        full_zh, full_en = _named(pool, tier, name_zh, name_en)
        rows.append((pool, tier, full_zh, full_en, full_zh, full_en, weight, "black", False,
                     "damage " + PLAYER + " %(" + str(low) + "," + str(high) + ")"))

    return rows

AMOUNTS = AMOUNTS + [
    ("cartload", "两箱", "Two crates of", (64, 96)),
    ("wagon", "一整车", "A cartload of", (96, 128)),
]


def _pick(ids, start, count):
    """Takes count ids from the table starting at start and wraps, so every region gets its own slice."""
    if not ids:
        return []

    result = []

    for index in range(count):
        result.append(ids[(start + index) % len(ids)])

    return result


ITEM_IDS = [item[0] for item in ITEMS]
EFFECT_IDS = [effect[0] for effect in EFFECTS]
MOB_IDS = [mob[0] for mob in MOBS]
LOOT_IDS = [loot[0] for loot in LOOT]

# Per region: how many item ids, which slice of the tables, the effect ids and the mob ids it draws from.
SURVIVAL_PLAN = {
    1: {"item_start": 0, "items": 40, "effects": ("strength", "speed", "haste", "regeneration", "resistance",
                                                  "fire_resistance", "night_vision", "absorption", "jump_boost",
                                                  "saturation", "health_boost", "luck",
                                                  "hunger", "poison"),
        "mobs": ("cow", "sheep", "pig", "chicken", "rabbit", "horse", "donkey", "llama", "bee", "goat",
                 "wolf", "cat", "zombie", "skeleton", "spider", "creeper", "husk", "stray", "drowned", "witch"),
        "xp": (20, 30, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 180, 200, 240, 280, 320)},
    2: {"item_start": 8, "items": 40, "effects": ("strength", "speed", "haste", "regeneration", "resistance",
                                                  "fire_resistance", "water_breathing", "absorption",
                                                  "health_boost", "luck", "saturation", "jump_boost",
                                                  "weakness", "slowness"),
        "mobs": ("cow", "sheep", "pig", "chicken", "horse", "donkey", "llama", "wolf", "cat", "bee",
                 "zombie", "skeleton", "spider", "creeper", "husk", "stray", "drowned", "witch", "pillager",
                 "vindicator"),
        "xp": (60, 80, 100, 120, 150, 180, 210, 240, 280, 320, 360, 400, 450, 500, 560, 620, 700)},
    3: {"item_start": 16, "items": 40, "effects": ("strength", "speed", "haste", "regeneration", "resistance",
                                                   "fire_resistance", "water_breathing", "night_vision",
                                                   "absorption", "health_boost", "luck", "conduit_power",
                                                   "poison", "mining_fatigue"),
        "mobs": ("horse", "donkey", "llama", "wolf", "bee", "goat", "zombie", "skeleton", "creeper", "drowned",
                 "witch", "pillager", "vindicator", "phantom", "blaze", "magma_cube", "piglin", "hoglin",
                 "enderman", "shulker"),
        "xp": (120, 160, 200, 240, 300, 360, 420, 480, 540, 600, 700, 800, 900, 1000, 1100, 1200, 1300)},
    4: {"item_start": 24, "items": 40, "effects": ("strength", "speed", "haste", "regeneration", "resistance",
                                                   "fire_resistance", "water_breathing", "night_vision",
                                                   "absorption", "health_boost", "luck", "slow_falling",
                                                   "wither", "darkness"),
        "mobs": ("zombie", "skeleton", "creeper", "drowned", "witch", "pillager", "vindicator", "ravager",
                 "phantom", "blaze", "magma_cube", "piglin", "hoglin", "enderman", "shulker", "elder_guardian",
                 "wither", "ender_dragon", "husk", "stray"),
        "xp": (300, 360, 420, 500, 600, 700, 800, 900, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400, 2600)},
    5: {"item_start": 32, "items": 40, "effects": ("strength", "speed", "haste", "regeneration", "resistance",
                                                   "fire_resistance", "water_breathing", "absorption",
                                                   "health_boost", "luck", "slow_falling", "conduit_power",
                                                   "wither", "levitation"),
        "mobs": ("blaze", "magma_cube", "piglin", "hoglin", "enderman", "shulker", "elder_guardian", "wither",
                 "ender_dragon", "ravager", "vindicator", "pillager", "phantom", "witch", "creeper", "skeleton",
                 "zombie", "drowned", "stray", "husk"),
        "xp": (600, 700, 800, 900, 1000, 1200, 1400, 1600, 1800, 2000, 2400, 2800, 3200, 3600, 4000, 4400, 4800)},
    6: {"item_start": 40, "items": 40, "effects": ("strength", "regeneration", "resistance", "health_boost",
                                                   "absorption", "luck", "slow_falling", "conduit_power",
                                                   "fire_resistance", "haste", "speed",
                                                   "wither", "levitation", "darkness"),
        "mobs": ("ender_dragon", "wither", "elder_guardian", "ravager", "vindicator", "pillager", "phantom",
                 "blaze", "hoglin", "enderman", "shulker", "magma_cube", "witch", "creeper", "skeleton", "zombie",
                 "drowned", "stray", "husk", "piglin"),
        "xp": (1000, 1200, 1400, 1600, 1800, 2000, 2400, 2800, 3200, 3600, 4000, 4400, 4800, 5200, 5600, 6000,
               6400)},
}

DAMAGE_NAMES = [
    ("脚下的裂缝", "A crack underfoot"),
    ("落石", "Falling rock"),
    ("锋利的碎石", "Sharp rubble"),
    ("一根断枝", "A broken branch"),
    ("荆棘", "A thicket of thorns"),
    ("倒塌的支架", "A collapsing prop"),
    ("滚落的矿石", "Rolling ore"),
    ("一次失足", "A bad step"),
    ("地刺", "A ground spike"),
    ("细碎的伤口", "A dozen small cuts"),
    ("灼热的地面", "Scorching ground"),
    ("一阵寒气", "A wave of cold"),
    ("暗处的利齿", "Teeth in the dark"),
    ("崩塌的石壁", "A crumbling wall"),
    ("失控的落雷", "A stray bolt"),
    ("岩浆的舔舐", "A lick of lava"),
    ("深渊的注视", "A stare from the deep"),
    ("彻底的混乱", "Utter chaos"),
    ("致命一击", "A fatal blow"),
    ("不可名状的代价", "An unnameable price"),
]

GEAR_PLAN = [
    ("iron_sword", "sharpness"), ("iron_pickaxe", "efficiency"), ("iron_chestplate", "protection"),
    ("iron_helmet", "protection"), ("iron_boots", "protection"), ("iron_leggings", "protection"),
    ("diamond_sword", "sharpness"), ("diamond_pickaxe", "efficiency"), ("diamond_chestplate", "protection"),
    ("bow", "power"),
]

# Weights of the five rarity steps, so no single entry is more than about twice as likely as one step above.
STEP_WEIGHTS = (120, 60, 25, 10, 3)


def survival_tier(tier):
    plan = SURVIVAL_PLAN[tier]
    rows = []

    item_ids = _pick(ITEM_IDS, plan["item_start"], plan["items"])
    rows += item_rows("survival", tier, item_ids, 320, STEP_WEIGHTS[0])

    effect_plan = []

    for effect_id in plan["effects"][:14]:
        for amplifier in range(5):
            effect_plan.append((effect_id, (30 * (amplifier + 1), 60 * (amplifier + 1)), amplifier))

    rows += effect_rows("survival", tier, effect_plan, STEP_WEIGHTS[1])

    mob_plan = []

    for mob_id in plan["mobs"][:20]:
        mob_plan.append((mob_id, 1))
        mob_plan.append((mob_id, 3 if tier >= 4 else 2))

    rows += mob_rows("survival", tier, mob_plan, STEP_WEIGHTS[2])

    rows += loot_rows("survival", tier, LOOT_IDS, STEP_WEIGHTS[2])
    rows += xp_rows("survival", tier, plan["xp"], STEP_WEIGHTS[3])

    damage_plan = []

    for index, (name_zh, name_en) in enumerate(DAMAGE_NAMES):
        low = 1 + index // 2
        damage_plan.append((name_zh, name_en, low, low + 2))

    rows += damage_rows("survival", tier, damage_plan, STEP_WEIGHTS[4])

    gear_plan = []

    for item_id, enchantment in GEAR_PLAN:
        gear_plan.append((item_id, enchantment, (1 + tier // 2, 2 + tier // 2)))

    for item_id, enchantment in GEAR_PLAN:
        gear_plan.append((item_id, "unbreaking", (1 + tier // 3, 3 + tier // 3)))

    rows += gear_rows("survival", tier, gear_plan, STEP_WEIGHTS[3])

    return rows


def survival_pool():
    rows = []

    for tier in range(1, 7):
        rows += survival_tier(tier)

    return rows
