"""Content tables for the v0.8.0 preset pools.

The tables hold the bilingual names of the vanilla things an entry can hand out, summon or apply. The region builders
in preset_regions.py decide which rows one region takes, so the tables stay pure data and no table row is multiplied by
an amount or intensity axis. Every entry keeps the row shape that build_preset_pool.py expects:

    (pool, tier, name_zh, name_en, notify_zh, notify_en, weight, colour, major, command)

Names stay readable and unique per pool because the region theme opens every name, for example 篝火-面包 / Campfire
Bread. A name is never reused inside one region, so the same item does not appear twice with only a quantity changed.
"""

import re

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


# A second block of vanilla things, so every survival region starts from its own forty items instead of borrowing from
# the neighbours; a region whose items do not stack may add a few more stackable ones when it builds its rows.
# Same shape as above: id, Chinese, English.
ITEMS = ITEMS + [
    ("white_wool", "白色羊毛", "White Wool"),
    ("red_wool", "红色羊毛", "Red Wool"),
    ("blue_wool", "蓝色羊毛", "Blue Wool"),
    ("green_wool", "绿色羊毛", "Green Wool"),
    ("black_wool", "黑色羊毛", "Black Wool"),
    ("yellow_wool", "黄色羊毛", "Yellow Wool"),
    ("orange_wool", "橙色羊毛", "Orange Wool"),
    ("purple_wool", "紫色羊毛", "Purple Wool"),
    ("white_concrete", "白色混凝土", "White Concrete"),
    ("gray_concrete", "灰色混凝土", "Gray Concrete"),
    ("black_concrete", "黑色混凝土", "Black Concrete"),
    ("red_concrete", "红色混凝土", "Red Concrete"),
    ("blue_concrete", "蓝色混凝土", "Blue Concrete"),
    ("terracotta", "陶瓦", "Terracotta"),
    ("white_terracotta", "白色陶瓦", "White Terracotta"),
    ("bricks", "红砖块", "Bricks"),
    ("stone_bricks", "石砖", "Stone Bricks"),
    ("mossy_stone_bricks", "苔石砖", "Mossy Stone Bricks"),
    ("sandstone", "砂岩", "Sandstone"),
    ("red_sandstone", "红砂岩", "Red Sandstone"),
    ("granite", "花岗岩", "Granite"),
    ("diorite", "闪长岩", "Diorite"),
    ("andesite", "安山岩", "Andesite"),
    ("deepslate", "深板岩", "Deepslate"),
    ("tuff", "凝灰岩", "Tuff"),
    ("calcite", "方解石", "Calcite"),
    ("basalt", "玄武岩", "Basalt"),
    ("blackstone", "黑石", "Blackstone"),
    ("netherrack", "下界岩", "Netherrack"),
    ("soul_sand", "灵魂沙", "Soul Sand"),
    ("soul_soil", "灵魂土", "Soul Soil"),
    ("obsidian", "黑曜石", "Obsidian"),
    ("crying_obsidian", "哭泣的黑曜石", "Crying Obsidian"),
    ("magma_block", "岩浆块", "Magma Block"),
    ("glowstone", "荧石", "Glowstone"),
    ("shroomlight", "菌光体", "Shroomlight"),
    ("sea_pickle", "海泡菜", "Sea Pickle"),
    ("scaffolding", "脚手架", "Scaffolding"),
    ("ladder", "梯子", "Ladder"),
    ("chest", "箱子", "Chest"),
    ("barrel", "木桶", "Barrel"),
    ("crafting_table", "工作台", "Crafting Table"),
    ("furnace", "熔炉", "Furnace"),
    ("blast_furnace", "高炉", "Blast Furnace"),
    ("smoker", "烟熏炉", "Smoker"),
    ("anvil", "铁砧", "Anvil"),
    ("grindstone", "砂轮", "Grindstone"),
    ("smithing_table", "锻造台", "Smithing Table"),
    ("enchanting_table", "附魔台", "Enchanting Table"),
    ("bookshelf", "书架", "Bookshelf"),
    ("lectern", "讲台", "Lectern"),
    ("glass_pane", "玻璃板", "Glass Pane"),
    ("tinted_glass", "遮光玻璃", "Tinted Glass"),
    ("oak_door", "橡木门", "Oak Door"),
    ("iron_door", "铁门", "Iron Door"),
    ("oak_fence", "橡木栅栏", "Oak Fence"),
    ("cobweb", "蜘蛛网", "Cobweb"),
    ("potato", "马铃薯", "Potato"),
    ("beetroot", "甜菜根", "Beetroot"),
    ("pumpkin_pie", "南瓜派", "Pumpkin Pie"),
    ("cooked_rabbit", "熟兔肉", "Cooked Rabbit"),
    ("rabbit_stew", "兔肉煲", "Rabbit Stew"),
    ("mushroom_stew", "蘑菇煲", "Mushroom Stew"),
    ("beef", "生牛肉", "Raw Beef"),
    ("porkchop", "生猪排", "Raw Porkchop"),
    ("mutton", "生羊肉", "Raw Mutton"),
    ("chicken", "生鸡肉", "Raw Chicken"),
    ("cod", "生鳕鱼", "Raw Cod"),
    ("salmon", "生鲑鱼", "Raw Salmon"),
    ("glow_berries", "发光浆果", "Glow Berries"),
    ("dried_kelp", "干海带", "Dried Kelp"),
    ("kelp", "海带", "Kelp"),
    ("sugar", "糖", "Sugar"),
    ("bone_meal", "骨粉", "Bone Meal"),
    ("paper", "纸", "Paper"),
    ("book", "书", "Book"),
    ("slime_block", "黏液块", "Slime Block"),
    ("honey_block", "蜂蜜块", "Honey Block"),
    ("honeycomb", "蜜脾", "Honeycomb"),
    ("snowball", "雪球", "Snowball"),
    ("fire_charge", "火焰弹", "Fire Charge"),
    ("flint_and_steel", "打火石", "Flint and Steel"),
    ("ink_sac", "墨囊", "Ink Sac"),
    ("glow_ink_sac", "荧光墨囊", "Glow Ink Sac"),
    ("prismarine_crystals", "海晶砂粒", "Prismarine Crystals"),
    ("redstone_torch", "红石火把", "Redstone Torch"),
    ("repeater", "红石中继器", "Redstone Repeater"),
    ("comparator", "红石比较器", "Redstone Comparator"),
    ("piston", "活塞", "Piston"),
    ("observer", "侦测器", "Observer"),
    ("hopper", "漏斗", "Hopper"),
    ("dropper", "投掷器", "Dropper"),
    ("dispenser", "发射器", "Dispenser"),
    ("minecart", "矿车", "Minecart"),
    ("rail", "铁轨", "Rail"),
    ("powered_rail", "充能铁轨", "Powered Rail"),
    ("redstone_lamp", "红石灯", "Redstone Lamp"),
    ("note_block", "音符盒", "Note Block"),
    ("golden_sword", "金剑", "Golden Sword"),
    ("golden_pickaxe", "金镐", "Golden Pickaxe"),
    ("golden_axe", "金斧", "Golden Axe"),
    ("golden_helmet", "金头盔", "Golden Helmet"),
    ("golden_chestplate", "金胸甲", "Golden Chestplate"),
    ("golden_leggings", "金护腿", "Golden Leggings"),
    ("golden_boots", "金靴子", "Golden Boots"),
    ("diamond_helmet", "钻石头盔", "Diamond Helmet"),
    ("diamond_leggings", "钻石护腿", "Diamond Leggings"),
    ("diamond_boots", "钻石靴子", "Diamond Boots"),
    ("netherite_sword", "下界合金剑", "Netherite Sword"),
    ("netherite_chestplate", "下界合金胸甲", "Netherite Chestplate"),
    ("white_dye", "白色染料", "White Dye"),
    ("black_dye", "黑色染料", "Black Dye"),
    ("red_dye", "红色染料", "Red Dye"),
    ("blue_dye", "蓝色染料", "Blue Dye"),
    ("green_dye", "绿色染料", "Green Dye"),
    ("yellow_dye", "黄色染料", "Yellow Dye"),
    ("glow_lichen", "发光地衣", "Glow Lichen"),
    ("moss_block", "苔藓块", "Moss Block"),
    ("moss_carpet", "苔藓地毯", "Moss Carpet"),
    ("azalea", "杜鹃花丛", "Azalea"),
    ("flowering_azalea", "盛开的杜鹃花丛", "Flowering Azalea"),
    ("spore_blossom", "孢子花", "Spore Blossom"),
    ("amethyst_block", "紫水晶块", "Block of Amethyst"),
    ("copper_block", "铜块", "Block of Copper"),
    ("exposed_copper", "斑驳的铜块", "Exposed Copper"),
    ("weathered_copper", "风化的铜块", "Weathered Copper"),
    ("oxidized_copper", "氧化的铜块", "Oxidized Copper"),
    ("raw_iron", "粗铁", "Raw Iron"),
    ("raw_gold", "粗金", "Raw Gold"),
    ("raw_copper", "粗铜", "Raw Copper"),
    ("turtle_egg", "海龟蛋", "Turtle Egg"),
    ("frogspawn", "青蛙卵", "Frogspawn"),
    ("powder_snow_bucket", "细雪桶", "Powder Snow Bucket"),
    ("lava_bucket", "岩浆桶", "Lava Bucket"),
    ("axolotl_bucket", "美西螈桶", "Bucket of Axolotl"),
    ("carrot_on_a_stick", "胡萝卜钓竿", "Carrot on a Stick"),
    ("warped_fungus_on_a_stick", "诡异菌钓竿", "Warped Fungus on a Stick"),
    ("soul_lantern", "灵魂灯笼", "Soul Lantern"),
    ("campfire", "营火", "Campfire"),
    ("soul_campfire", "灵魂营火", "Soul Campfire"),
    ("red_bed", "床", "Bed"),
    ("cyan_bed", "青色床", "Cyan Bed"),
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
    ("silverfish", "蠹虫", "Silverfish"),
    ("endermite", "末影螨", "Endermite"),
    ("guardian", "守卫者", "Guardian"),
    ("evoker", "唤魔者", "Evoker"),
    ("vex", "恼鬼", "Vex"),
    ("zoglin", "僵尸疣猪兽", "Zoglin"),
    ("zombified_piglin", "僵尸猪灵", "Zombified Piglin"),
    ("wither_skeleton", "凋灵骷髅", "Wither Skeleton"),
    ("strider", "炽足兽", "Strider"),
    ("fox", "狐狸", "Fox"),
    ("panda", "熊猫", "Panda"),
    ("polar_bear", "北极熊", "Polar Bear"),
    ("turtle", "海龟", "Turtle"),
    ("dolphin", "海豚", "Dolphin"),
    ("bat", "蝙蝠", "Bat"),
    ("slime", "史莱姆", "Slime"),
    ("cave_spider", "洞穴蜘蛛", "Cave Spider"),
    ("zombie_villager", "僵尸村民", "Zombie Villager"),
    ("wandering_trader", "流浪商人", "Wandering Trader"),
    ("trader_llama", "行商羊驼", "Trader Llama"),
    ("squid", "鱿鱼", "Squid"),
    ("glow_squid", "发光鱿鱼", "Glow Squid"),
    ("cod", "鳕鱼", "Cod"),
    ("salmon", "鲑鱼", "Salmon"),
    ("tropical_fish", "热带鱼", "Tropical Fish"),
    ("axolotl", "美西螈", "Axolotl"),
    ("allay", "悦灵", "Allay"),
]

# id, Chinese, English, harmful
EFFECTS = [
    ("instant_health", "瞬间治疗", "Instant Health", False),
    ("instant_damage", "瞬间伤害", "Instant Damage", True),
    ("unluck", "霉运", "Bad Luck", True),
    ("bad_omen", "不祥之兆", "Bad Omen", True),
    ("hero_of_the_village", "村庄英雄", "Hero of the Village", False),
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
# id, Chinese, English. A potion is a single item with an effect tag, so a region can hand out the effect itself.
POTIONS = [
    ("healing", "治疗药水", "Potion of Healing"),
    ("regeneration", "再生药水", "Potion of Regeneration"),
    ("strength", "力量药水", "Potion of Strength"),
    ("swiftness", "迅捷药水", "Potion of Swiftness"),
    ("fire_resistance", "抗火药水", "Potion of Fire Resistance"),
    ("water_breathing", "水肺药水", "Potion of Water Breathing"),
    ("night_vision", "夜视药水", "Potion of Night Vision"),
    ("invisibility", "隐身药水", "Potion of Invisibility"),
    ("leaping", "跳跃药水", "Potion of Leaping"),
    ("slow_falling", "缓降药水", "Potion of Slow Falling"),
    ("poison", "剧毒药水", "Potion of Poison"),
    ("harming", "伤害药水", "Potion of Harming"),
    ("slowness", "迟缓药水", "Potion of Slowness"),
    ("weakness", "虚弱药水", "Potion of Weakness"),
    ("turtle_master", "神龟药水", "Potion of the Turtle Master"),
    ("luck", "幸运药水", "Potion of Luck"),
]

# id, Chinese, English. One arrow entry per tipped effect, so a region does not repeat the same arrow.
ARROWS = [
    ("poison", "剧毒之箭", "Arrow of Poison"),
    ("harming", "伤害之箭", "Arrow of Harming"),
    ("healing", "治疗之箭", "Arrow of Healing"),
    ("regeneration", "再生之箭", "Arrow of Regeneration"),
    ("strength", "力量之箭", "Arrow of Strength"),
    ("swiftness", "迅捷之箭", "Arrow of Swiftness"),
    ("fire_resistance", "抗火之箭", "Arrow of Fire Resistance"),
    ("water_breathing", "水肺之箭", "Arrow of Water Breathing"),
    ("night_vision", "夜视之箭", "Arrow of Night Vision"),
    ("invisibility", "隐身之箭", "Arrow of Invisibility"),
    ("leaping", "跳跃之箭", "Arrow of Leaping"),
    ("slow_falling", "缓降之箭", "Arrow of Slow Falling"),
    ("slowness", "迟缓之箭", "Arrow of Slowness"),
    ("weakness", "虚弱之箭", "Arrow of Weakness"),
    ("turtle_master", "神龟之箭", "Arrow of the Turtle Master"),
]

LOOT = [
    ("simple_dungeon", "地牢的箱子", "A dungeon chest"),
    ("abandoned_mineshaft", "废弃矿井的箱子", "A mineshaft chest"),
    ("village/village_weaponsmith", "村庄武器匠的箱子", "A village weaponsmith chest"),
    ("village/village_armorer", "村庄盔甲匠的箱子", "A village armorer chest"),
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

# Vanilla stack sizes of the preset items that do not carry a full stack, taken from the 1.20.2 item registry.
# A portion of such an item has to fit one slot, so the bulk axes stop at sixteen or do not apply at all.
STACK_SIZES = {
    # sixteen per slot
    "bucket": 16,
    "egg": 16,
    "ender_pearl": 16,
    "honey_bottle": 16,
    "snowball": 16,
    # one per slot
    "axolotl_bucket": 1,
    "bow": 1,
    "carrot_on_a_stick": 1,
    "crossbow": 1,
    "cyan_bed": 1,
    "diamond_boots": 1,
    "diamond_chestplate": 1,
    "diamond_helmet": 1,
    "diamond_leggings": 1,
    "diamond_pickaxe": 1,
    "diamond_sword": 1,
    "elytra": 1,
    "fishing_rod": 1,
    "flint_and_steel": 1,
    "golden_axe": 1,
    "golden_boots": 1,
    "golden_chestplate": 1,
    "golden_helmet": 1,
    "golden_leggings": 1,
    "golden_pickaxe": 1,
    "golden_sword": 1,
    "iron_axe": 1,
    "iron_boots": 1,
    "iron_chestplate": 1,
    "iron_helmet": 1,
    "iron_leggings": 1,
    "iron_pickaxe": 1,
    "iron_shovel": 1,
    "iron_sword": 1,
    "lava_bucket": 1,
    "milk_bucket": 1,
    "minecart": 1,
    "mushroom_stew": 1,
    "netherite_chestplate": 1,
    "netherite_sword": 1,
    "powder_snow_bucket": 1,
    "rabbit_stew": 1,
    "red_bed": 1,
    "saddle": 1,
    "shears": 1,
    "shield": 1,
    "shulker_box": 1,
    "totem_of_undying": 1,
    "trident": 1,
    "turtle_helmet": 1,
    "warped_fungus_on_a_stick": 1,
    "water_bucket": 1,
}


def stack_size(item_id):
    """The vanilla stack size of one preset item: sixty four unless the table lists a smaller slot."""
    return STACK_SIZES.get(item_id, 64)


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


# A portion of an item that cannot stack is exactly one piece, so it carries no quantity word and no count.
SINGLE_AMOUNT = ("single", "", "", (1, 1))


def amount_axes(item_id):
    """The portion steps an item may use: one piece, up to one slot, or every bulk step."""
    size = stack_size(item_id)

    if size <= 1:
        return [SINGLE_AMOUNT]

    if size <= 16:
        return AMOUNTS[:3]

    return AMOUNTS


def _item_capacity(item_ids):
    return sum(len(amount_axes(item_id)) for item_id in item_ids)


def _item_ids_with_capacity(item_ids, count):
    """Adds stackable table items until the region can hold its item quota without repeating an item."""
    result = list(item_ids)
    used = set(result)
    cursor = 0

    while _item_capacity(result) < count:
        candidate = None

        while cursor < len(ITEMS):
            item = ITEMS[cursor]
            cursor += 1

            if item[0] not in used and stack_size(item[0]) >= 64:
                candidate = item
                break

        if candidate is None:
            break

        used.add(candidate[0])
        result.append(candidate[0])

    return result


def item_rows(pool, tier, item_ids, count, weight):
    """Builds the item entries of one region one portion step at a time, so every item offers its small portion first
    and a non stacking item never turns into a pile."""
    items = [item for item in ITEMS if item[0] in item_ids]
    rows = []
    step = 0

    while len(rows) < count:
        progressed = False

        for item in items:
            axes = amount_axes(item[0])

            if step >= len(axes):
                continue

            amount = axes[step]
            name_zh, name_en = _named(pool, tier, amount[1] + item[1],
                                      (amount[2] + " " + item[2]).strip())

            if amount is SINGLE_AMOUNT:
                command = "give " + PLAYER + " minecraft:" + item[0]
            else:
                command = ("give " + PLAYER + " minecraft:" + item[0] + " %(" + str(amount[3][0]) + ","
                           + str(amount[3][1]) + ")")

            rows.append((pool, tier, name_zh, name_en, name_zh, name_en,
                         BUCKET_WEIGHTS[min(step, len(BUCKET_WEIGHTS) - 1)], "red", False, command))
            progressed = True

            if len(rows) >= count:
                break

        if not progressed:
            break

        step += 1

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
        command = ("give " + PLAYER + " minecraft:" + item_id + "{Enchantments:[{id:\"minecraft:" + enchantment
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
    2: {"item_start": 40, "items": 40, "effects": ("strength", "speed", "haste", "regeneration", "resistance",
                                                  "fire_resistance", "water_breathing", "absorption",
                                                  "health_boost", "luck", "saturation", "jump_boost",
                                                  "weakness", "slowness"),
        "mobs": ("cow", "sheep", "pig", "chicken", "horse", "donkey", "llama", "wolf", "cat", "bee",
                 "zombie", "skeleton", "spider", "creeper", "husk", "stray", "drowned", "witch", "pillager",
                 "vindicator"),
        "xp": (60, 80, 100, 120, 150, 180, 210, 240, 280, 320, 360, 400, 450, 500, 560, 620, 700)},
    3: {"item_start": 80, "items": 40, "effects": ("strength", "speed", "haste", "regeneration", "resistance",
                                                   "fire_resistance", "water_breathing", "night_vision",
                                                   "absorption", "health_boost", "luck", "conduit_power",
                                                   "poison", "mining_fatigue"),
        "mobs": ("horse", "donkey", "llama", "wolf", "bee", "goat", "zombie", "skeleton", "creeper", "drowned",
                 "witch", "pillager", "vindicator", "phantom", "blaze", "magma_cube", "piglin", "hoglin",
                 "enderman", "shulker"),
        "xp": (120, 160, 200, 240, 300, 360, 420, 480, 540, 600, 700, 800, 900, 1000, 1100, 1200, 1300)},
    4: {"item_start": 120, "items": 40, "effects": ("strength", "speed", "haste", "regeneration", "resistance",
                                                   "fire_resistance", "water_breathing", "night_vision",
                                                   "absorption", "health_boost", "luck", "slow_falling",
                                                   "wither", "darkness"),
        "mobs": ("zombie", "skeleton", "creeper", "drowned", "witch", "pillager", "vindicator", "ravager",
                 "phantom", "blaze", "magma_cube", "piglin", "hoglin", "enderman", "shulker", "elder_guardian",
                 "wither", "ender_dragon", "husk", "stray"),
        "xp": (300, 360, 420, 500, 600, 700, 800, 900, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400, 2600)},
    5: {"item_start": 160, "items": 40, "effects": ("strength", "speed", "haste", "regeneration", "resistance",
                                                   "fire_resistance", "water_breathing", "absorption",
                                                   "health_boost", "luck", "slow_falling", "conduit_power",
                                                   "wither", "levitation"),
        "mobs": ("blaze", "magma_cube", "piglin", "hoglin", "enderman", "shulker", "elder_guardian", "wither",
                 "ender_dragon", "ravager", "vindicator", "pillager", "phantom", "witch", "creeper", "skeleton",
                 "zombie", "drowned", "stray", "husk"),
        "xp": (600, 700, 800, 900, 1000, 1200, 1400, 1600, 1800, 2000, 2400, 2800, 3200, 3600, 4000, 4400, 4800)},
    6: {"item_start": 200, "items": 40, "effects": ("strength", "regeneration", "resistance", "health_boost",
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
    """Builds one survival region to its exact quota, with the black share the region is meant to carry."""
    plan = SURVIVAL_PLAN[tier]
    rows = []

    effects = plan["effects"][:14]
    harmful_effects = [effect_id for effect_id in effects
                       if next(entry for entry in EFFECTS if entry[0] == effect_id)[3]]
    hostile_ids = [mob_id for mob_id in plan["mobs"] if mob_id in HOSTILE_IDS][:SURVIVAL_HOSTILE_MOBS[tier]]
    passive_ids = [mob_id for mob_id in plan["mobs"] if mob_id not in HOSTILE_IDS][:max(0, 20 - len(hostile_ids))]

    damage_count = 20
    loot_count = len(LOOT_IDS)
    xp_count = len(plan["xp"])
    gear_count = 20
    effect_count = len(effects) * 5
    mob_count = (len(hostile_ids) + len(passive_ids)) * 2
    target_black = round(TIER_ENTRIES * SURVIVAL_BLACK_SHARE[tier])
    black_from_effects = len(harmful_effects) * 5
    black_from_mobs = len(hostile_ids) * 2
    mishap_count = max(0, target_black - damage_count - black_from_effects - black_from_mobs)
    item_count = TIER_ENTRIES - (effect_count + mob_count + loot_count + xp_count + gear_count + damage_count
                                 + mishap_count)

    item_ids = _item_ids_with_capacity(_pick(ITEM_IDS, plan["item_start"], plan["items"]), item_count)
    rows += item_rows("survival", tier, item_ids, item_count, STEP_WEIGHTS[0])

    effect_plan = []

    for effect_id in effects:
        for amplifier in range(5):
            effect_plan.append((effect_id, (30 * (amplifier + 1), 60 * (amplifier + 1)), amplifier))

    rows += effect_rows("survival", tier, effect_plan, STEP_WEIGHTS[1])

    mob_plan = []

    for mob_id in hostile_ids + passive_ids:
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
    rows += mishap_rows("survival", tier, mishap_count)

    return rows


SURVIVAL_SCALE = {1: 1.0, 2: 1.2, 3: 1.4, 4: 1.7, 5: 2.0, 6: 2.4}


def survival_pool():
    rows = []

    for tier in range(1, 7):
        for row in survival_tier(tier):
            rows.append(row[:9] + (scale_command(row[9], SURVIVAL_SCALE[tier]),))

    return rows

# ---------------------------------------------------------------------------
# Creative pool
#
# The creative pool works with verbs the survival pool never uses: weather, time, particles, sound, teleporting,
# structure generation, traps, item rain, mounts and fireworks. Every base idea is multiplied by five intensity
# variants, so one line of the table yields five entries with their own name and their own parameter range.
# ---------------------------------------------------------------------------

SCALE_WORDS = [
    ("微", "Faint", 1),
    ("小", "Small", 2),
    ("中", "Moderate", 3),
    ("大", "Large", 4),
    ("极", "Extreme", 5),
]

# name, command template with {s} for the scale step and {a}/{b} for the derived range
WEATHER_IDEAS = [
    ("小雨", "Light rain", "weather rain %(60,120)"),
    ("阵雨", "A shower", "weather rain %(120,240)"),
    ("长雨", "Long rain", "weather rain %(240,600)"),
    ("雷暴", "A thunderstorm", "weather thunder %(60,120)"),
    ("久雷", "A long storm", "weather thunder %(120,300)"),
    ("晴天", "Clear skies", "weather clear %(120,300)"),
    ("放晴", "The sky clears", "weather clear %(300,600)"),
    ("骤雨", "Sudden rain", "weather rain %(30,60)"),
    ("闷雷", "Distant thunder", "weather thunder %(30,60)"),
    ("长晴", "A long clear spell", "weather clear %(600,1200)"),
]

TIME_IDEAS = [
    ("推进一刻", "A quarter hour on", "time add #(200)"),
    ("推进半日", "Half a day on", "time add #(1000)"),
    ("快进一段", "A quick skip", "time add #(3000)"),
    ("跳过白昼", "Over the daylight", "time add #(12000)"),
    ("直接入夜", "Straight into night", "time add #(13000)"),
    ("直接天亮", "Straight into morning", "time add #(23000)"),
    ("拨回清晨", "Back to dawn", "time set #(1000)"),
    ("拨到正午", "Back to noon", "time set #(6000)"),
]

PARTICLE_IDEAS = [
    ("火焰", "Flame", "execute at " + PLAYER + " run particle minecraft:flame ~ ~1 ~ 1 1 1 0.02 %(10,60)"),
    ("烟雾", "Smoke", "execute at " + PLAYER + " run particle minecraft:large_smoke ~ ~1 ~ 1 1 1 0.02 %(10,60)"),
    ("火花", "Sparks", "execute at " + PLAYER + " run particle minecraft:electric_spark ~ ~1 ~ 1 1 1 0.05 %(10,60)"),
    ("爱心", "Hearts", "execute at " + PLAYER + " run particle minecraft:heart ~ ~1 ~ 1 1 1 0.02 %(8,40)"),
    ("音符", "Notes", "execute at " + PLAYER + " run particle minecraft:note ~ ~1 ~ 1 1 1 0.05 %(8,40)"),
    ("云朵", "Clouds", "execute at " + PLAYER + " run particle minecraft:cloud ~ ~1 ~ 1 1 1 0.02 %(10,50)"),
    ("传送门", "Portal", "execute at " + PLAYER + " run particle minecraft:portal ~ ~1 ~ 1 1 1 0.05 %(10,60)"),
    ("附魔", "Enchant", "execute at " + PLAYER + " run particle minecraft:enchant ~ ~1 ~ 1 1 1 0.05 %(10,60)"),
    ("爆炸", "Explosion", "execute at " + PLAYER + " run particle minecraft:explosion ~ ~1 ~ 0 0 0 0.1 %(3,12)"),
    ("龙息", "Dragon breath", "execute at " + PLAYER + " run particle minecraft:dragon_breath ~ ~1 ~ 1 1 1 0.02 %(10,50)"),
    ("末地烛光", "End rod", "execute at " + PLAYER + " run particle minecraft:end_rod ~ ~1 ~ 1 1 1 0.02 %(10,50)"),
    ("哭泣的黑曜石", "Crying obsidian", "execute at " + PLAYER + " run particle minecraft:falling_obsidian_tear ~ ~2 ~ 1 1 1 0.02 %(8,30)"),
    ("雪片", "Snowflakes", "execute at " + PLAYER + " run particle minecraft:snowflake ~ ~2 ~ 2 2 2 0.01 %(20,80)"),
    ("樱花", "Cherry petals", "execute at " + PLAYER + " run particle minecraft:cherry_leaves ~ ~2 ~ 2 2 2 0.01 %(20,80)"),
    ("荧光", "Glow", "execute at " + PLAYER + " run particle minecraft:glow ~ ~1 ~ 1 1 1 0.02 %(10,50)"),
    ("灵魂火", "Soul fire", "execute at " + PLAYER + " run particle minecraft:soul_fire_flame ~ ~1 ~ 1 1 1 0.02 %(10,50)"),
    ("气泡", "Bubbles", "execute at " + PLAYER + " run particle minecraft:bubble ~ ~1 ~ 1 1 1 0.02 %(10,50)"),
    ("水花", "Splash", "execute at " + PLAYER + " run particle minecraft:splash ~ ~1 ~ 1 1 1 0.02 %(10,50)"),
    ("图腾之光", "Totem light", "execute at " + PLAYER + " run particle minecraft:totem_of_undying ~ ~1 ~ 1 1 1 0.02 %(6,30)"),
    ("烟花尾", "Firework trail", "execute at " + PLAYER + " run particle minecraft:firework ~ ~1 ~ 1 1 1 0.02 %(10,60)"),
    ("暴击", "Crit", "execute at " + PLAYER + " run particle minecraft:crit ~ ~1 ~ 1 1 1 0.02 %(10,40)"),
    ("附魔命中", "Enchanted hit", "execute at " + PLAYER + " run particle minecraft:enchanted_hit ~ ~1 ~ 1 1 1 0.02 %(10,40)"),
    ("横扫", "Sweep", "execute at " + PLAYER + " run particle minecraft:sweep_attack ~ ~1 ~ 1 1 1 0.02 %(6,24)"),
    ("村民喜悦", "Happy villagers", "execute at " + PLAYER + " run particle minecraft:happy_villager ~ ~1 ~ 1 1 1 0.02 %(8,30)"),
    ("村民愤怒", "Angry villagers", "execute at " + PLAYER + " run particle minecraft:angry_villager ~ ~1 ~ 1 1 1 0.02 %(8,30)"),
    ("烟团", "Poof", "execute at " + PLAYER + " run particle minecraft:poof ~ ~1 ~ 1 1 1 0.02 %(8,30)"),
    ("细烟", "Thin smoke", "execute at " + PLAYER + " run particle minecraft:smoke ~ ~1 ~ 1 1 1 0.02 %(10,40)"),
    ("岩浆滴", "Lava drip", "execute at " + PLAYER + " run particle minecraft:dripping_lava ~ ~2 ~ 1 1 1 0.02 %(6,20)"),
    ("灰烬", "Ash", "execute at " + PLAYER + " run particle minecraft:ash ~ ~2 ~ 2 2 2 0.01 %(20,60)"),
    ("白色灰烬", "White ash", "execute at " + PLAYER + " run particle minecraft:white_ash ~ ~2 ~ 2 2 2 0.01 %(20,60)"),
    ("打蜡", "Wax on", "execute at " + PLAYER + " run particle minecraft:wax_on ~ ~1 ~ 1 1 1 0.02 %(6,20)"),
    ("刮削", "Scrape", "execute at " + PLAYER + " run particle minecraft:scrape ~ ~1 ~ 1 1 1 0.02 %(6,20)"),
    ("蛋壳", "Egg crack", "execute at " + PLAYER + " run particle minecraft:egg_crack ~ ~1 ~ 1 1 1 0.02 %(4,16)"),
    ("音爆", "Sonic boom", "execute at " + PLAYER + " run particle minecraft:sonic_boom ~ ~1 ~ 0 0 0 0.1 %(1,4)"),
]

SOUND_IDEAS = [
    ("经验音", "A level up chime", "playsound minecraft:entity.player.levelup master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("钟声", "A bell", "playsound minecraft:block.bell.use master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("雷声", "Thunder", "playsound minecraft:entity.lightning_bolt.thunder master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("号角", "A raid horn", "playsound minecraft:event.raid.horn master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("下界传送门", "A portal hum", "playsound minecraft:block.portal.travel master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("宝箱开启", "A chest opening", "playsound minecraft:block.chest.open master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("凋灵生成", "A wither spawning", "playsound minecraft:entity.wither.spawn master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("村民欢呼", "Villagers cheering", "playsound minecraft:entity.villager.celebrate master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("烟花升空", "A firework launch", "playsound minecraft:entity.firework_rocket.launch master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("附魔台", "An enchanting table", "playsound minecraft:block.enchantment_table.use master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("洞穴回响", "A cave echo", "playsound minecraft:ambient.cave master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("末影人", "An enderman", "playsound minecraft:entity.enderman.scream master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("铁砧落地", "An anvil lands", "playsound minecraft:block.anvil.land master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("末影传送", "An enderman warps", "playsound minecraft:entity.enderman.teleport master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("恶魂尖啸", "A ghast screams", "playsound minecraft:entity.ghast.scream master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("铃音", "A chime", "playsound minecraft:block.note_block.chime master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("信标激活", "A beacon activates", "playsound minecraft:block.beacon.activate master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("潮涌核心", "A conduit activates", "playsound minecraft:block.conduit.activate master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("饱嗝", "A burp", "playsound minecraft:entity.player.burp master " + PLAYER + " ~ ~ ~ #(1) 1"),
    ("监守者尖啸", "A warden boom", "playsound minecraft:entity.warden.sonic_boom master " + PLAYER + " ~ ~ ~ #(1) 1"),
]

TELEPORT_IDEAS = [
    ("近处挪移", "A short hop", "execute at " + PLAYER + " run spreadplayers ~ ~ 4 %(4,8) false @s"),
    ("短途散布", "A short scatter", "execute at " + PLAYER + " run spreadplayers ~ ~ 8 %(8,16) false @s"),
    ("中距传送", "A middle distance jump", "execute at " + PLAYER + " run spreadplayers ~ ~ 16 %(16,32) false @s"),
    ("远距传送", "A long jump", "execute at " + PLAYER + " run spreadplayers ~ ~ 32 %(32,64) false @s"),
    ("高处落点", "A high landing", "execute at " + PLAYER + " run tp @s ~ ~%(20,40) ~"),
    ("原地升空", "A straight rise", "execute at " + PLAYER + " run tp @s ~ ~%(10,25) ~"),
    ("水平抛飞", "A sideways throw", "execute at " + PLAYER + " run tp @s ~%(10,30) ~ ~%(10,30)"),
    ("回到底部", "Back to the ground", "execute at " + PLAYER + " run tp @s ~ %(60,80) ~"),
    ("反向拉扯", "A hard pull back", "execute at " + PLAYER + " run tp @s ~%(1,5) ~ ~%(1,5)"),
    ("深潜", "A dive", "execute at " + PLAYER + " run tp @s ~ %(5,25) ~"),
    ("峡谷对岸", "Across the ravine", "execute at " + PLAYER + " run spreadplayers ~ ~ 24 %(24,48) false @s"),
    ("极远落点", "A very far landing", "execute at " + PLAYER + " run spreadplayers ~ ~ 48 %(48,96) false @s"),
    ("洞穴落点", "A cave landing", "execute at " + PLAYER + " run tp @s ~ ~%(10,30) ~"),
    ("水下落点", "A landing underwater", "execute at " + PLAYER + " run spreadplayers ~ ~ 12 %(6,12) false @s"),
    ("高空坠落", "A long drop", "execute at " + PLAYER + " run tp @s ~ ~%(40,80) ~"),
    ("斜向抛飞", "A diagonal throw", "execute at " + PLAYER + " run tp @s ~%(30,60) ~ ~%(30,60)"),
]

# A structure is anchored to a heightmap rather than to the player's feet: a player standing in a cave or under
# water would otherwise grow the structure inside stone or inside the sea, and a structure that grows downward from
# the surface stays inside the world instead of being clipped against bedrock. The ocean floor keeps a wreck or a
# monument on the sea bed instead of floating it on the surface of the sea.
#
# Every row is a structure the current dimension can hold. A preset entry is one plain command with no way to read
# the dimension of the player, so a nether fortress or an end city can not be held back for its own dimension: those
# rows are left out instead of being grown in the wrong place.
STRUCTURE_ANCHORS = {
    "surface": "world_surface",
    "ocean": "ocean_floor",
}


def structure_command(structure_id, anchor, spread):
    """One structure verb, anchored to a heightmap and set a little to the side so it does not grow on the player."""
    heightmap = STRUCTURE_ANCHORS[anchor]
    return ("execute at " + PLAYER + " positioned over " + heightmap + " ~#(" + str(spread) + ") ~ ~#("
            + str(spread) + ") run place structure minecraft:" + structure_id)


STRUCTURE_IDEAS = [
    ("村庄", "A village", structure_command("village_plains", "surface", 4)),
    ("针叶林村庄", "A taiga village", structure_command("village_taiga", "surface", 4)),
    ("雪原村庄", "A snowy village", structure_command("village_snowy", "surface", 4)),
    ("热带草原村庄", "A savanna village", structure_command("village_savanna", "surface", 4)),
    ("掠夺者前哨", "A pillager outpost", structure_command("pillager_outpost", "surface", 4)),
    ("废弃传送门", "A ruined portal", structure_command("ruined_portal", "surface", 4)),
    ("沉船", "A shipwreck", structure_command("shipwreck", "ocean", 4)),
    ("搁浅的沉船", "A beached shipwreck", structure_command("shipwreck_beached", "surface", 4)),
    ("沙漠神殿", "A desert temple", structure_command("desert_pyramid", "surface", 4)),
    ("雪屋", "An igloo", structure_command("igloo", "surface", 4)),
    ("女巫小屋", "A swamp hut", structure_command("swamp_hut", "surface", 4)),
    ("远古城市", "An ancient city", structure_command("ancient_city", "surface", 4)),
    ("海底神殿", "An ocean monument", structure_command("monument", "ocean", 6)),
    ("林地府邸", "A woodland mansion", structure_command("mansion", "surface", 6)),
    ("丛林神庙", "A jungle temple", structure_command("jungle_pyramid", "surface", 4)),
    ("要塞", "A stronghold", structure_command("stronghold", "surface", 6)),
    ("废弃矿井", "An abandoned mineshaft", structure_command("mineshaft", "surface", 4)),
    ("暖海废墟", "A warm ocean ruin", structure_command("ocean_ruin_warm", "ocean", 4)),
    ("寒海废墟", "A cold ocean ruin", structure_command("ocean_ruin_cold", "ocean", 4)),
    ("古迹废墟", "Trail ruins", structure_command("trail_ruins", "surface", 4)),
]

TRAP_IDEAS = [
    ("脚下起火", "Fire underfoot", "execute at " + PLAYER + " run fill ~ ~ ~ ~#(1) ~ ~#(1) fire"),
    ("脚边岩浆", "Lava at the feet", "execute at " + PLAYER + " run setblock ~#(1) ~ ~ lava"),
    ("落沙压顶", "Sand above", "execute at " + PLAYER + " run setblock ~ ~#(3) ~ sand"),
    ("苦力怕空投", "A creeper drop", "execute at " + PLAYER + " run summon minecraft:creeper ~ ~#(6) ~"),
    ("TNT 引线", "A lit fuse", "execute at " + PLAYER + " run summon minecraft:tnt ~ ~4 ~ {Fuse:%(20,60)}"),
    ("细雪陷阱", "Powder snow", "execute at " + PLAYER + " run fill ~ ~ ~ ~#(1) ~ ~#(1) powder_snow"),
    ("蛛网", "A cobweb", "execute at " + PLAYER + " run fill ~ ~ ~ ~#(1) ~ ~#(1) cobweb"),
    ("甜浆果丛", "Sweet berry bushes", "execute at " + PLAYER + " run fill ~ ~ ~ ~#(1) ~ ~#(1) sweet_berry_bush"),
    ("落雷", "A lightning strike", "execute at " + PLAYER + " positioned ~#(1) ~ ~ run summon minecraft:lightning_bolt ~ ~ ~"),
    ("岩浆雨", "A short lava rain", "execute at " + PLAYER + " run summon minecraft:tnt ~ ~#(8) ~ {Fuse:#(40)}"),
    ("深坑", "A pit", "execute at " + PLAYER + " run fill ~-2 ~-1 ~-2 ~2 ~#(-4) ~2 minecraft:air"),
    ("火焰环绕", "A ring of fire", "execute at " + PLAYER + " run fill ~#(-2) ~ ~#(-2) ~#(2) ~ ~#(2) minecraft:fire replace minecraft:air"),
    ("凋灵玫瑰", "A wither rose", "execute at " + PLAYER + " run setblock ~ ~ ~ wither_rose"),
    ("脚下岩浆块", "Magma underfoot", "execute at " + PLAYER + " run setblock ~ ~-1 ~ magma_block"),
    ("仙人掌阵", "A cactus patch", "execute at " + PLAYER + " run fill ~ ~ ~ ~#(1) ~ ~#(1) cactus"),
    ("滴水石", "A dripstone spike", "execute at " + PLAYER + " run setblock ~ ~#(2) ~ pointed_dripstone[vertical_direction=down]"),
    ("蜂蜜粘脚", "Sticky honey", "execute at " + PLAYER + " run setblock ~ ~ ~ honey_block"),
    ("蓝冰", "A sheet of blue ice", "execute at " + PLAYER + " run setblock ~ ~-1 ~ blue_ice"),
    ("头顶铁砧", "An anvil overhead", "execute at " + PLAYER + " run setblock ~ ~#(3) ~ anvil"),
    ("幻翼群", "A flock of phantoms", "execute at " + PLAYER + " run summon minecraft:phantom ~ ~ ~ --R(3)"),
    ("蠹虫群", "A swarm of silverfish", "execute at " + PLAYER + " run summon minecraft:silverfish ~ ~ ~ --R(3)"),
    ("岩浆幕墙", "A wall of lava", "execute at " + PLAYER + " run fill ~#(2) ~ ~ ~#(2) ~#(3) ~ minecraft:lava"),
    ("浮沙柱", "A column of sand", "execute at " + PLAYER + " run fill ~ ~ ~ ~ ~#(4) ~ minecraft:sand"),
    ("凋灵骷髅", "A wither skeleton", "execute at " + PLAYER + " run summon minecraft:wither_skeleton ~ ~ ~ --R(2)"),
]

ITEMRAIN_IDEAS = [
    ("面包雨", "A bread rain", "execute at " + PLAYER + " run summon minecraft:item ~ ~4 ~ {Item:{id:\"minecraft:bread\",Count:%(1,4)b}}"),
    ("苹果雨", "An apple rain", "execute at " + PLAYER + " run summon minecraft:item ~ ~4 ~ {Item:{id:\"minecraft:apple\",Count:%(1,4)b}}"),
    ("铁锭雨", "An iron rain", "execute at " + PLAYER + " run summon minecraft:item ~ ~4 ~ {Item:{id:\"minecraft:iron_ingot\",Count:%(1,3)b}}"),
    ("金锭雨", "A gold rain", "execute at " + PLAYER + " run summon minecraft:item ~ ~4 ~ {Item:{id:\"minecraft:gold_ingot\",Count:%(1,3)b}}"),
    ("钻石雨", "A diamond rain", "execute at " + PLAYER + " run summon minecraft:item ~ ~4 ~ {Item:{id:\"minecraft:diamond\",Count:%(1,2)b}}"),
    ("火把雨", "A torch rain", "execute at " + PLAYER + " run summon minecraft:item ~ ~4 ~ {Item:{id:\"minecraft:torch\",Count:%(2,8)b}}"),
    ("箭雨", "An arrow rain", "execute at " + PLAYER + " run summon minecraft:item ~ ~4 ~ {Item:{id:\"minecraft:arrow\",Count:%(4,16)b}}"),
    ("经验瓶雨", "A bottle rain", "execute at " + PLAYER + " run summon minecraft:item ~ ~4 ~ {Item:{id:\"minecraft:experience_bottle\",Count:%(1,4)b}}"),
    ("烟花雨", "A firework rain", "execute at " + PLAYER + " run summon minecraft:item ~ ~4 ~ {Item:{id:\"minecraft:firework_rocket\",Count:%(2,6)b}}"),
    ("绿宝石雨", "An emerald rain", "execute at " + PLAYER + " run summon minecraft:item ~ ~4 ~ {Item:{id:\"minecraft:emerald\",Count:%(1,3)b}}"),
]

MOUNT_IDEAS = [
    ("备鞍的马", "A saddled horse", "execute at " + PLAYER + " run summon minecraft:horse ~ ~ ~ {Tame:1b,SaddleItem:{id:\"minecraft:saddle\",Count:1b},Attributes:[{Name:\"generic.max_health\",Base:#(20)}]}"),
    ("疾行马", "A swift horse", "execute at " + PLAYER + " run summon minecraft:horse ~ ~ ~ {Tame:1b,SaddleItem:{id:\"minecraft:saddle\",Count:1b},Attributes:[{Name:\"generic.movement_speed\",Base:0.3},{Name:\"generic.max_health\",Base:#(20)}]}"),
    ("驴", "A donkey", "execute at " + PLAYER + " run summon minecraft:donkey ~ ~ ~ {Tame:1b,ChestedHorse:1b,Attributes:[{Name:\"generic.max_health\",Base:#(20)}]}"),
    ("羊驼", "A llama", "execute at " + PLAYER + " run summon minecraft:llama ~ ~ ~ {Tame:1b,ChestedHorse:1b,Attributes:[{Name:\"generic.max_health\",Base:#(20)}]}"),
    ("船", "A boat", "execute at " + PLAYER + " run summon minecraft:boat ~ ~ ~"),
    ("矿车", "A minecart", "execute at " + PLAYER + " run summon minecraft:minecart ~ ~ ~"),
    ("炽足兽", "A strider", "execute at " + PLAYER + " run summon minecraft:strider ~ ~ ~ {Saddle:1b,Attributes:[{Name:\"generic.max_health\",Base:#(20)}]}"),
    ("骆驼", "A camel", "execute at " + PLAYER + " run summon minecraft:camel ~ ~ ~ {SaddleItem:{id:\"minecraft:saddle\",Count:1b},Attributes:[{Name:\"generic.max_health\",Base:#(20)}]}"),
    ("骡", "A mule", "execute at " + PLAYER + " run summon minecraft:mule ~ ~ ~ {Tame:1b,ChestedHorse:1b,Attributes:[{Name:\"generic.max_health\",Base:#(20)}]}"),
    ("骷髅马", "A skeleton horse", "execute at " + PLAYER + " run summon minecraft:skeleton_horse ~ ~ ~ {Tame:1b,Attributes:[{Name:\"generic.max_health\",Base:#(20)}]}"),
]

FIREWORK_IDEAS = [
    ("小型烟花", "A small firework", "execute at " + PLAYER + " run summon minecraft:firework_rocket ~ ~2 ~ {LifeTime:#(10),FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:1b}}}}"),
    ("中型烟花", "A middle firework", "execute at " + PLAYER + " run summon minecraft:firework_rocket ~ ~2 ~ {LifeTime:#(20),FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:2b}}}}"),
    ("大型烟花", "A large firework", "execute at " + PLAYER + " run summon minecraft:firework_rocket ~ ~2 ~ {LifeTime:#(30),FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:3b}}}}"),
    ("礼炮齐放", "A salvo", "execute at " + PLAYER + " run summon minecraft:firework_rocket ~ ~2 ~ {LifeTime:#(25),FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:2b}}}}"),
    ("高空烟花", "A high firework", "execute at " + PLAYER + " run summon minecraft:firework_rocket ~ ~8 ~ {LifeTime:#(40),FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:3b}}}}"),
    ("近距烟花", "A close firework", "execute at " + PLAYER + " run summon minecraft:firework_rocket ~ ~1 ~ {LifeTime:#(8),FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:1b}}}}"),
    ("苦力怕脸", "A creeper face", "execute at " + PLAYER + " run summon minecraft:firework_rocket ~ ~2 ~ {LifeTime:#(20),FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:2b,Explosions:[{Type:3,Colors:[I;11743532,3887386],Trail:1b,Flicker:1b}]}}}}"),
    ("星形烟花", "A star burst", "execute at " + PLAYER + " run summon minecraft:firework_rocket ~ ~2 ~ {LifeTime:#(20),FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:2b,Explosions:[{Type:2,Colors:[I;2651799,4312372],Trail:1b}]}}}}"),
    ("球状烟花", "A round burst", "execute at " + PLAYER + " run summon minecraft:firework_rocket ~ ~2 ~ {LifeTime:#(20),FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:2b,Explosions:[{Type:1,Colors:[I;15790320,11743532]}]}}}}"),
    ("拖尾闪烁", "A trailing flicker", "execute at " + PLAYER + " run summon minecraft:firework_rocket ~ ~2 ~ {LifeTime:#(20),FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:2b,Explosions:[{Type:0,Colors:[I;15435844,8073150],Trail:1b,Flicker:1b}]}}}}"),
]



# Enchanted books hand the player a single enchantment at a level the region decides, which is the one enchantment
# verb the survival pool never uses for the creative pool's purposes.
ENCHANT_IDEAS = [
    ("锋利附魔书", "A sharpness book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:sharpness\",lvl:%(1,3)}]}"),
    ("保护附魔书", "A protection book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:protection\",lvl:%(1,3)}]}"),
    ("耐久附魔书", "An unbreaking book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:unbreaking\",lvl:%(1,3)}]}"),
    ("时运附魔书", "A fortune book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:fortune\",lvl:%(1,2)}]}"),
    ("经验修补附魔书", "A mending book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:mending\",lvl:1}]}"),
    ("力量附魔书", "A power book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:power\",lvl:%(1,3)}]}"),
    ("冰霜行者附魔书", "A frost walker book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:frost_walker\",lvl:%(1,2)}]}"),
    ("灵魂疾行附魔书", "A soul speed book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:soul_speed\",lvl:%(1,3)}]}"),
    ("水下呼吸附魔书", "A respiration book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:respiration\",lvl:%(1,3)}]}"),
    ("穿刺附魔书", "An impaling book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:impaling\",lvl:%(1,3)}]}"),
    ("效率附魔书", "An efficiency book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:efficiency\",lvl:%(1,4)}]}"),
    ("抢夺附魔书", "A looting book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:looting\",lvl:%(1,3)}]}"),
    ("摔落保护附魔书", "A feather falling book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:feather_falling\",lvl:%(1,4)}]}"),
    ("荆棘附魔书", "A thorns book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:thorns\",lvl:%(1,3)}]}"),
    ("火焰附加附魔书", "A fire aspect book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:fire_aspect\",lvl:%(1,2)}]}"),
    ("击退附魔书", "A knockback book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:knockback\",lvl:%(1,2)}]}"),
    ("深海探索者附魔书", "A depth strider book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:depth_strider\",lvl:%(1,3)}]}"),
    ("多重射击附魔书", "A multishot book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:multishot\",lvl:1}]}"),
    ("快速装填附魔书", "A quick charge book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:quick_charge\",lvl:%(1,3)}]}"),
    ("忠诚附魔书", "A loyalty book", "give " + PLAYER + " minecraft:enchanted_book{StoredEnchantments:[{id:\"minecraft:loyalty\",lvl:%(1,3)}]}"),
]

# id, Chinese, English, highest level. One row per vanilla enchantment, so a region never repeats the same book.
BOOK_ENCHANTS = [
    ("sharpness", "锋利", "Sharpness", 5),
    ("smite", "亡灵杀手", "Smite", 5),
    ("bane_of_arthropods", "节肢杀手", "Bane of Arthropods", 5),
    ("knockback", "击退", "Knockback", 2),
    ("fire_aspect", "火焰附加", "Fire Aspect", 2),
    ("looting", "抢夺", "Looting", 3),
    ("sweeping", "横扫之刃", "Sweeping Edge", 3),
    ("efficiency", "效率", "Efficiency", 5),
    ("silk_touch", "精准采集", "Silk Touch", 1),
    ("unbreaking", "耐久", "Unbreaking", 3),
    ("fortune", "时运", "Fortune", 3),
    ("power", "力量", "Power", 5),
    ("punch", "冲击", "Punch", 2),
    ("flame", "火矢", "Flame", 1),
    ("infinity", "无限", "Infinity", 1),
    ("luck_of_the_sea", "海之眷顾", "Luck of the Sea", 3),
    ("lure", "饵钓", "Lure", 3),
    ("loyalty", "忠诚", "Loyalty", 3),
    ("impaling", "穿刺", "Impaling", 5),
    ("riptide", "激流", "Riptide", 3),
    ("channeling", "引雷", "Channeling", 1),
    ("multishot", "多重射击", "Multishot", 1),
    ("quick_charge", "快速装填", "Quick Charge", 3),
    ("piercing", "穿透", "Piercing", 4),
    ("protection", "保护", "Protection", 4),
    ("fire_protection", "火焰保护", "Fire Protection", 4),
    ("feather_falling", "摔落保护", "Feather Falling", 4),
    ("blast_protection", "爆炸保护", "Blast Protection", 4),
    ("projectile_protection", "弹射物保护", "Projectile Protection", 4),
    ("respiration", "水下呼吸", "Respiration", 3),
    ("aqua_affinity", "水下速掘", "Aqua Affinity", 1),
    ("thorns", "荆棘", "Thorns", 3),
    ("depth_strider", "深海探索者", "Depth Strider", 3),
    ("frost_walker", "冰霜行者", "Frost Walker", 2),
    ("soul_speed", "灵魂疾行", "Soul Speed", 3),
    ("swift_sneak", "迅捷潜行", "Swift Sneak", 3),
    ("mending", "经验修补", "Mending", 1),
    ("vanishing_curse", "消失诅咒", "Curse of Vanishing", 1),
    ("binding_curse", "绑定诅咒", "Curse of Binding", 1),
]

CREATIVE_CATEGORIES = [
    ("天候", "Weather", WEATHER_IDEAS, 50, "red"),
    ("时刻", "Hour", TIME_IDEAS, 40, "red"),
    ("粒子", "Particles", PARTICLE_IDEAS, 56, "red"),
    ("声响", "Sound", SOUND_IDEAS, 48, "red"),
    ("位移", "Shift", TELEPORT_IDEAS, 48, "red"),
    ("结构", "Structure", STRUCTURE_IDEAS, 40, "red"),
    ("陷阱", "Trap", TRAP_IDEAS, 48, "black"),
    ("天降", "Rainfall", ITEMRAIN_IDEAS, 50, "red"),
    ("坐骑", "Mount", MOUNT_IDEAS, 40, "red"),
    ("烟花", "Firework", FIREWORK_IDEAS, 30, "red"),
    ("附魔", "Enchantment", ENCHANT_IDEAS, 50, "red"),
]




# ---------------------------------------------------------------------------
# Region scaling
#
# A creative region must feel wilder than the one before it, so every command carries two optional markers:
#   #(value)   a fixed number, scaled by the region factor
#   %(low,high) an integer range, both ends scaled by the region factor
# The markers are replaced while the pool is built, so the tables above stay readable.
# ---------------------------------------------------------------------------

CREATIVE_SCALE = {1: 1.0, 2: 1.4, 3: 1.9, 4: 2.5, 5: 3.2, 6: 4.0}


GIVE_COMMAND = re.compile(r"^(give \$\{player\} minecraft:([a-z0-9_]+)(?:\{[^\n]*\})?)(?: %\((\d+),(\d+)\))?$")
# A stackable prize never exceeds two slots, so a scaled region factor can not turn one portion into a wagon.
GIVE_SLOT_LIMIT = 128


def clamp_give(command):
    """Keeps the count of a scaled give command inside what the item can really carry."""
    match = GIVE_COMMAND.match(command)

    if match is None:
        return command

    size = stack_size(match.group(2))
    base = match.group(1)

    if match.group(3) is None:
        return command

    if size <= 1:
        return base

    limit = size if size < 64 else GIVE_SLOT_LIMIT
    low = max(1, min(limit, int(match.group(3))))
    high = max(1, min(limit, int(match.group(4))))

    if low >= high:
        return base + " " + str(high)

    return base + " %(" + str(low) + "," + str(high) + ")"


def scale_command(command, factor):
    def number(match):
        value = int(round(int(match.group(1)) * factor))

        if value == 0:
            return "0"

        return str(value if abs(value) >= 1 else (1 if value > 0 else -1))

    def pair(match):
        low = int(round(int(match.group(1)) * factor))
        high = int(round(int(match.group(2)) * factor))

        if low > high:
            low, high = high, low

        if low == high:
            high = low + 1

        return "%(" + str(low) + "," + str(high) + ")"

    command = re.sub(r"#\((-?\d+)\)", number, command)
    command = re.sub(r"%\((-?\d+),(-?\d+)\)", pair, command)
    return clamp_give(command)

def category_rows(pool, tier, label_zh, label_en, ideas, count, colour):
    """Multiplies every idea by the intensity words until the category holds exactly its quota."""
    rows = []
    per_idea = max(1, count // max(len(ideas), 1))

    if count % max(len(ideas), 1) != 0:
        raise ValueError("quota " + str(count) + " does not split over " + str(len(ideas)) + " ideas")

    weights = (120, 60, 25, 10, 3)

    for idea_index, (name_zh, name_en, command) in enumerate(ideas):
        for step in range(per_idea):
            scale = SCALE_WORDS[min(step, len(SCALE_WORDS) - 1)]
            # A greater intensity word is not only rarer but also stronger, so the five variants of one idea differ.
            factor = CREATIVE_SCALE[tier] * (0.6 + 0.2 * min(step, 4))
            full_zh = THEMES[pool][tier][0] + "-" + scale[0] + label_zh + name_zh
            full_en = THEMES[pool][tier][1] + " " + scale[1] + " " + label_en + " " + name_en
            rows.append((pool, tier, full_zh, full_en, full_zh, full_en,
                         weights[min(step, len(weights) - 1)], colour, False,
                         scale_command(command, factor)))

    while len(rows) < count:
        rows.append(rows[len(rows) % max(len(rows), 1)])

    return rows[:count]


def creative_tier(tier):
    rows = []

    for label_zh, label_en, ideas, count, colour in CREATIVE_CATEGORIES:
        rows += category_rows("creative", tier, label_zh, label_en, ideas, count, colour)

    return rows


def creative_pool():
    rows = []

    for tier in range(1, 7):
        rows += creative_tier(tier)

    return rows

# ---------------------------------------------------------------------------
# Survival mishaps: the black half of the survival pool
# ---------------------------------------------------------------------------

SURVIVAL_MISHAPS = [
    ("脚下起火", "Fire underfoot", "execute at " + PLAYER + " run fill ~ ~ ~ ~#(1) ~ ~#(1) fire"),
    ("脚边岩浆", "Lava at the feet", "execute at " + PLAYER + " run setblock ~#(1) ~ ~ lava"),
    ("落沙", "Sand from above", "execute at " + PLAYER + " run setblock ~ ~#(3) ~ sand"),
    ("苦力怕", "A creeper", "execute at " + PLAYER + " run summon minecraft:creeper ~ ~#(6) ~"),
    ("TNT", "Lit TNT", "execute at " + PLAYER + " run summon minecraft:tnt ~ ~4 ~ {Fuse:%(20,60)}"),
    ("蛛网", "A cobweb", "execute at " + PLAYER + " run fill ~ ~ ~ ~#(1) ~ ~#(1) cobweb"),
    ("甜浆果丛", "Berry bushes", "execute at " + PLAYER + " run fill ~ ~ ~ ~#(1) ~ ~#(1) sweet_berry_bush"),
    ("落雷", "A lightning strike", "execute at " + PLAYER + " positioned ~#(1) ~ ~ run summon minecraft:lightning_bolt ~ ~ ~"),
    ("坑洞", "A sudden pit", "execute at " + PLAYER + " run fill ~-1 ~-1 ~-1 ~1 ~-3 ~1 minecraft:air"),
    ("饥饿", "Hunger", "effect give " + PLAYER + " minecraft:hunger %(20,60) 0"),
    ("虚弱", "Weakness", "effect give " + PLAYER + " minecraft:weakness %(20,60) 0"),
    ("缓慢", "Slowness", "effect give " + PLAYER + " minecraft:slowness %(20,60) 0"),
    ("失明", "Blindness", "effect give " + PLAYER + " minecraft:blindness %(10,30) 0"),
    ("反胃", "Nausea", "effect give " + PLAYER + " minecraft:nausea %(10,30) 0"),
    ("挖掘疲劳", "Mining fatigue", "effect give " + PLAYER + " minecraft:mining_fatigue %(20,60) 0"),
    ("中毒", "Poison", "effect give " + PLAYER + " minecraft:poison %(10,25) 0"),
    ("蠹虫群", "Silverfish", "execute at " + PLAYER + " run summon minecraft:silverfish ~ ~ ~"),
    ("僵尸", "A zombie", "execute at " + PLAYER + " run summon minecraft:zombie ~ ~ ~"),
    ("骷髅", "A skeleton", "execute at " + PLAYER + " run summon minecraft:skeleton ~ ~ ~"),
    ("蜘蛛", "A spider", "execute at " + PLAYER + " run summon minecraft:spider ~ ~ ~"),
    ("岩浆怪", "A magma cube", "execute at " + PLAYER + " run summon minecraft:magma_cube ~ ~ ~"),
    ("细雪", "Powder snow", "execute at " + PLAYER + " run fill ~ ~ ~ ~#(1) ~ ~#(1) powder_snow"),
    ("仙人掌阵", "A cactus patch", "execute at " + PLAYER + " run fill ~ ~ ~ ~#(1) ~ ~#(1) cactus"),
    ("脚下岩浆块", "Magma underfoot", "execute at " + PLAYER + " run setblock ~ ~-1 ~ magma_block"),
    ("凋灵玫瑰", "A wither rose", "execute at " + PLAYER + " run setblock ~ ~ ~ wither_rose"),
    ("滴水石", "A dripstone spike", "execute at " + PLAYER + " run setblock ~ ~#(2) ~ pointed_dripstone[vertical_direction=down]"),
    ("蜂蜜粘脚", "Sticky honey", "execute at " + PLAYER + " run setblock ~ ~ ~ honey_block"),
    ("蓝冰", "A sheet of blue ice", "execute at " + PLAYER + " run setblock ~ ~-1 ~ blue_ice"),
    ("头顶铁砧", "An anvil overhead", "execute at " + PLAYER + " run setblock ~ ~#(3) ~ anvil"),
    ("幻翼群", "A flock of phantoms", "execute at " + PLAYER + " run summon minecraft:phantom ~ ~ ~ --R(3)"),
]

MISHAP_SCALES = [
    ("微", "Faint"), ("轻", "Light"), ("小", "Small"), ("中", "Moderate"),
    ("大", "Large"), ("重", "Heavy"), ("极", "Extreme"), ("灾", "Disaster"),
]

# Target share of black entries per survival region, from a friendly first region to a hostile last one.
SURVIVAL_BLACK_SHARE = {1: 0.22, 2: 0.28, 3: 0.34, 4: 0.40, 5: 0.44, 6: 0.48}
SURVIVAL_HOSTILE_MOBS = {1: 8, 2: 10, 3: 12, 4: 14, 5: 16, 6: 18}
HOSTILE_IDS = {"zombie", "skeleton", "spider", "creeper", "husk", "stray", "drowned", "witch", "pillager",
               "vindicator", "ravager", "phantom", "blaze", "magma_cube", "hoglin", "wither", "ender_dragon",
               "silverfish", "endermite", "guardian", "evoker", "vex", "zoglin", "zombified_piglin",
               "wither_skeleton", "slime", "cave_spider", "zombie_villager"}


def mishap_rows(pool, tier, count):
    rows = []
    scale_count = len(MISHAP_SCALES)

    for idea_index, (name_zh, name_en, command) in enumerate(SURVIVAL_MISHAPS):
        for scale_index in range(scale_count):
            if len(rows) >= count:
                break

            scale = MISHAP_SCALES[scale_index]
            full_zh = THEMES[pool][tier][0] + "-" + scale[0] + name_zh
            full_en = THEMES[pool][tier][1] + " " + scale[1] + " " + name_en
            rows.append((pool, tier, full_zh, full_en, full_zh, full_en, 25 if scale_index < 4 else 10,
                         "black", False, command))

        if len(rows) >= count:
            break

    return rows[:count]
