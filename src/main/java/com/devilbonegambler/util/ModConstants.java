/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

import com.devilbonegambler.DevilBoneGambler;

/**
 * Design detail constants that a developer may tune without touching core logic.
 * Every value here is a documented design detail of the current version.
 */
public final class ModConstants
{
    /*
     * Identity and protocol
     */
    public static final String MOD_ID = DevilBoneGambler.MODID;
    public static final String NETWORK_CHANNEL = "main";
    public static final String PROTOCOL_VERSION = "1";

    /*
     * Config files and domains
     */
    public static final String CONFIG_DIR_NAME = "devil_bone_gambler";
    public static final String PRIZE_POOL_FILE = "prize_pool.json";
    public static final String ITEM_VALUES_FILE = "item_values.json";
    public static final String ACCESS_FILE = "access.json";
    public static final String CONFIG_DOMAIN_PRIZE_POOL = "prize_pool";
    public static final String CONFIG_DOMAIN_ITEM_VALUES = "item_values";

    /*
     * Save data names
     */
    public static final String LUCK_DATA_NAME = "devil_bone_gambler_luck";

    /*
     * Manuals: shipped markdown content, one file per manual and language, read only and never generated at runtime.
     */
    public static final String MANUAL_RESOURCE_DIR = "manuals/";
    public static final String MANUAL_RESOURCE_EXTENSION = ".md";
    /*
     * Interval of the calibration pass over the online players, in ticks: a bound manual is granted again, deduplicated
     * or taken away, and a copy lying in a loaded chunk is cleared, at most this long after the fact. Ten seconds leave
     * a player enough time to put a manual back into the inventory by hand before a pass can react.
     */
    public static final int MANUAL_CALIBRATION_INTERVAL_TICKS = 200;
    /* A held dice reveals what it belongs to: the reach, the rescan interval and the white highlight both share. */
    public static final double DICE_SIGHT_ALTAR_RADIUS = 24.0D;
    public static final double DICE_SIGHT_DEALER_RADIUS = 24.0D;
    public static final int DICE_SIGHT_SCAN_INTERVAL_TICKS = 10;
    /*
     * The sight is a halo around the real model, not a repaint of it: a slightly larger copy of the geometry is pushed
     * a little away from the camera and drawn additively with the depth test on, so the model keeps its own texture and
     * only the rim around it glows.
     */
    public static final int DICE_SIGHT_HIGHLIGHT_COLOR = 0xFFF2C0;
    public static final float DICE_SIGHT_HIGHLIGHT_ALPHA = 0.8F;
    public static final float DICE_SIGHT_HALO_SCALE = 1.08F;
    public static final float DICE_SIGHT_HALO_PUSH = 0.08F;

    /*
     * Luck
     */
    public static final int BASE_LUCK = 50;
    public static final int MIN_LUCK = 0;
    public static final int MAX_LUCK = 200;

    /*
     * Value table
     */
    public static final int DEFAULT_ITEM_VALUE = 20;
    public static final int DEFAULT_ENTITY_VALUE = 10;
    public static final int DEFAULT_ENCHANT_VALUE = 100;
    public static final long MAX_TOTAL_VALUE = 100000000L;

    /*
     * Prize pool
     */
    public static final int MIN_REGION_TIER = 1;
    public static final int MAX_REGIONS = 6;
    public static final int DEFAULT_REGION_COUNT = 3;
    public static final int MAX_ENTRIES_PER_REGION = 128;
    public static final int MAX_TOTAL_ENTRIES = 512;

    public static final int MAX_COMMAND_LENGTH = 512;
    public static final int MAX_NOTIFY_TEXT_LENGTH = 256;
    /*
     * A preset entry name is stored as a text component, which takes more room than a plain label.
     */
    public static final int MAX_ENTRY_ID_LENGTH = 96;

    /*
     * Dice roll
     */
    public static final int MIN_DICE_POINT = 1;
    public static final int MAX_DICE_POINT = 6;
    public static final double OVERFLOW_WEIGHT_BONUS = 0.25D;
    public static final double LUCK_BIAS_WEIGHT = 0.7D;
    public static final double ALTAR_BIAS_WEIGHT = 0.3D;
    public static final double MAX_POINT_BIAS_CHANCE = 0.5D;
    public static final int POINT_DISPLAY_TICKS = 20;

    public static final String ENTRY_COLOR_RED = "red";
    public static final String ENTRY_COLOR_BLACK = "black";
    public static final String[] ENTRY_COLORS = {ENTRY_COLOR_RED, ENTRY_COLOR_BLACK};

    /*
     * Altar
     */
    public static final int ALTAR_SLOTS = 9;
    public static final int ALTAR_BASE_RADIUS = 6;
    public static final int ALTAR_MAX_RADIUS = 10;
    public static final int ALTAR_RADIUS_VALUE_STEP = 2000;
    public static final int ALTAR_LEVEL_VALUE_STEP = 1000;
    public static final int ALTAR_MAX_LEVEL = 10;
    public static final long ALTAR_MAX_VALUE = 100000L;
    public static final String ALTAR_VALUE_TAG = "DevilBoneGamblerAltarValue";
    public static final int ALTAR_EFFECT_INTERVAL_TICKS = 60;
    public static final int ALTAR_EFFECT_DURATION_TICKS = 600;
    public static final int ALTAR_CARRIED_EFFECT_DIVISOR = 2;
    public static final int ALTAR_BIOME_SCAN_INTERVAL_TICKS = 10;
    public static final int ALTAR_MAX_MOB_ENTRIES = 32;

    /*
     * Height of the altar model in pixels, which is also where the collision box stops.
     */
    public static final int ALTAR_MODEL_HEIGHT = 13;

    /*
     * Light level of the altar, driven by the four lit candles of the model. The model format of this
     * Minecraft version cannot carry an emission, so the level is declared on the block itself.
     */
    public static final int ALTAR_LIGHT_LEVEL = 14;

    /*
     * The glow a dealer carries: half of a torch. A torch sits at fourteen, so the dealer sits at seven, and like the
     * altar it has to be a block, because this Minecraft version gives no light to an entity.
     */
    public static final int DEALER_LIGHT_LEVEL = 7;
    /* How often a dealer re-checks which block it is standing in, in ticks. */
    public static final int DEALER_GLOW_INTERVAL_TICKS = 10;

    /*
     * The offering value written over an altar, shown only while a player stands in its range and looks at it.
     */
    public static final double ALTAR_LABEL_LOOK_DOT = 0.7D;
    public static final double ALTAR_LABEL_RANGE = 6.0D;

    /*
     * Range exclusivity of the altar. An altar worn in the head slot counts as a placed altar, so two altar
     * ranges may never overlap: a placement inside a range is destroyed and dropped, and a wearer inside
     * another range takes damage and gets a chat warning on the same interval.
     */
    public static final float ALTAR_INTRUSION_DAMAGE = 1.0F;
    public static final int ALTAR_INTRUSION_INTERVAL_TICKS = 20;

    /*
     * Wager
     */
    public static final int WAGER_COUNTDOWN_SECONDS = 5;
    public static final int WAGER_RADIUS = 6;
    public static final int WAGER_SNEAK_WINDOW_TICKS = 20;
    public static final int WAGER_MIN_PLAYERS = 1;

    /*
     * Requests and permissions
     */
    public static final long MIN_REQUEST_INTERVAL_MS = 250L;
    /* A settlement that never reports back is treated as finished after this, so a stuck draw cannot lock the dice. */
    public static final long DRAW_SETTLEMENT_TIMEOUT_MS = 10000L;

    public static final int DRAW_COMMAND_PERMISSION_LEVEL = 4;

    /*
     * Value table
     */
    public static final String VALUE_TABLE_TAG = "DevilBoneGamblerValueTable";
    /*
     * Rows held by the searchable value table view at once, and the longest query it accepts.
     */
    public static final int VALUE_TABLE_SCREEN_ROWS = 11;
    public static final int VALUE_TABLE_SEARCH_MAX = 48;
    /*
     * Chapter filters of the searchable view, cycled in this order by one button.
     */
    public static final int VALUE_TABLE_CHAPTER_ALL = 0;
    public static final int VALUE_TABLE_CHAPTER_ITEM = 1;
    public static final int VALUE_TABLE_CHAPTER_ENTITY = 2;
    public static final int VALUE_TABLE_CHAPTER_COUNT = 3;

    /*
     * List tables: every list view shares one fixed header row, one row height, one scroll bar and one hover delay.
     * The hover delay holds a tooltip back until the pointer rested on the same row, so a tooltip never flashes.
     */
    public static final int TABLE_HEADER_HEIGHT = 12;
    public static final int TABLE_ROW_HEIGHT = 12;
    public static final int TABLE_CELL_PADDING = 4;
    public static final int TABLE_SCROLL_WIDTH = 6;
    public static final int TABLE_SCROLL_MIN_THUMB = 8;
    public static final long TOOLTIP_DELAY_MS = 500L;

    /*
     * Placeholders
     */
    public static final String PLACEHOLDER_PLAYER = "${player}";
    public static final String PLACEHOLDER_X = "${x}";
    public static final String PLACEHOLDER_Y = "${y}";
    public static final String PLACEHOLDER_Z = "${z}";
    public static final String PLACEHOLDER_PLAYER_CAPITAL = "$Player";

    /*
     * Prize command position placeholders
     */
    public static final int COMMAND_RANDOM_DEFAULT_MODE = 0;
    public static final int COMMAND_RANDOM_DEFAULT_RANGE = 6;
    public static final int COMMAND_RANDOM_MAX_MODE = 3;
    public static final int COMMAND_RANDOM_MAX_RANGE = 64;

    /*
     * Result display modes. Screen occupying titles are deliberately absent: a result is told in the chat box or on
     * the action bar, never as a title over the whole screen, and the chat box is the default channel.
     */
    public static final String NOTIFY_MODE_TELLRAW = "tellraw";
    public static final String NOTIFY_MODE_NONE = "none";
    /*
     * A repeated identical notice is held back for this long, so a held key or the same mistake made twice in a row
     * cannot fill the chat box with one line. A notice of another kind is never held back, and the cooldown only moves
     * when a notice is really sent, so a player who keeps trying still gets one reminder per second.
     */
    public static final long NOTICE_COOLDOWN_MS = 1000L;

    public static final String[] NOTIFY_MODES = {
        NOTIFY_MODE_TELLRAW,
        NOTIFY_MODE_NONE
    };

    /*
     * Dealer: entity, feeding, pathing and the sunrise and sunset silent spawn.
     */
    /*
     * The dealer pocket holds gambler dice only: 32 slots at the start plus one more per whole experience point, and a
     * fresh dealer already carries 16. Bars start at twice the player base and gain a tenth of that base per point.
     */
    public static final int DEALER_POCKET_BASE = 32;
    public static final int DEALER_POCKET_INITIAL_DICE = 16;
    public static final double DEALER_STAT_BASE_MULTIPLIER = 2.0D;
    public static final double DEALER_STAT_PER_EXPERIENCE = 0.1D;
    public static final int PLAYER_BASE_MAX_HEALTH = 20;
    public static final int PLAYER_BASE_MAX_SATIETY = 20;
    /* One eaten dice restores half of each bar, the wasted part turns into experience. */
    public static final double DEALER_DICE_RESTORE_SHARE = 0.5D;
    public static final int DEALER_MAX_HEALTH = 40;
    public static final int DEALER_MAX_SATIETY = 40;
    /*
     * Every bar cost is a share of the current ceiling, so both bars keep their meaning while experience lifts them.
     * A wager costs 12 percent plus one more per member, a swing half a percent, and a missing two percent of satiety
     * is paid with one percent of health.
     */
    public static final double DEALER_WAGER_COST_BASE_PERCENT = 12.0D;
    public static final double DEALER_WAGER_COST_PER_MEMBER_PERCENT = 1.0D;
    public static final double DEALER_SWING_COST_PERCENT = 0.5D;
    public static final double DEALER_SATIETY_PERCENT_PER_HEALTH_PERCENT = 2.0D;
    /*
     * Both bars drain with time: one whole day empties the satiety bar, checked once per second. A recruit or a wager
     * stops the drain and refills both bars at the same rate, because the players are feeding the dealer then.
     */
    public static final int DEALER_DAY_TICKS = 24000;
    public static final int DEALER_DECAY_INTERVAL_TICKS = 20;
    public static final double DEALER_DECAY_PERCENT_PER_INTERVAL =
            100.0D * DEALER_DECAY_INTERVAL_TICKS / DEALER_DAY_TICKS;
    public static final int DEALER_STAY_SECONDS = 30;
    public static final int DEALER_FEED_RADIUS = 2;
    /*
     * Eating: a dealer starts eating from its pocket when one of its bars is at or under the share of the maximum, and
     * it waits between two meals. Only the gambler's dice is food, the devil's dice is left on the ground.
     */
    public static final double DEALER_FEED_THRESHOLD = 0.6D;
    public static final int DEALER_FEED_INTERVAL_TICKS = 40;
    /* Two dice per pickup, and the next pair only after this many ticks, so a pile is emptied at a walking pace. */
    public static final int DEALER_PICKUP_INTERVAL_TICKS = 40;
    public static final int DEALER_PICKUP_PER_ACTION = 2;
    /*
     * Fight: every swing costs half a percent of the satiety ceiling, and a dealer with an empty bar cannot swing at
     * all, so a long fight slows down and finally stops instead of running forever.
     */
    public static final int DEALER_PATH_RADIUS = 8;
    public static final int DEALER_PATH_SAMPLES = 16;
    public static final int DEALER_PATH_TIMEOUT_TICKS = 400;
    public static final double DEALER_LIGHT_WEIGHT = 4.0D;
    public static final double DEALER_LIQUID_WEIGHT = 3.0D;
    public static final double DEALER_DISTANCE_WEIGHT = 0.5D;
    public static final int DEALER_LIQUID_SCAN_RADIUS = 1;
    /*
     * A dealer in liquid climbs out at once: the shore is looked for in this radius, the target is refreshed on this
     * interval, and the escape runs at this speed instead of the walking one.
     */
    public static final int DEALER_LIQUID_ESCAPE_RADIUS = 16;
    public static final int DEALER_LIQUID_ESCAPE_REPATH_TICKS = 5;
    public static final double DEALER_LIQUID_ESCAPE_SPEED = 1.2D;

    /*
     * Dealer territory: a dealer treats every other dealer inside its own radius as an intruder and fights it until
     * one of them dies, so two dealers rarely share an area. The damage of one hit is rolled from zero to the maximum
     * with a seed built out of the timestamp and the own state and position.
     */
    public static final double DEALER_TERRITORY_RADIUS = 6.0D;
    public static final double DEALER_TERRITORY_ATTACK_REACH = 2.5D;
    public static final int DEALER_TERRITORY_CHECK_INTERVAL_TICKS = 10;
    /* One hit per second while both dealers stay inside reach. */
    public static final int DEALER_TERRITORY_ATTACK_INTERVAL_TICKS = 20;
    /* Two dealers push each other apart, so the aim vector flips near zero distance and is not updated there. */
    public static final double DEALER_TERRITORY_LOOK_MIN_SQR = 0.5D;
    public static final double DEALER_TERRITORY_CHASE_SPEED = 0.9D;
    public static final float DEALER_TERRITORY_MAX_DAMAGE = 5.0F;
    /*
     * A struck dealer fumbles dice out of its pockets: the roll is made once per hit and never adds to the loot of a
     * death, which keeps the death drop the only guaranteed handful. One hit frees dice on five percent of the hits in
     * total: three dice on one percent, two on one and a half percent and one on two and a half percent.
     */
    public static final double DEALER_HURT_DROP_CHANCE_THREE = 0.01D;
    public static final double DEALER_HURT_DROP_CHANCE_TWO = 0.015D;
    public static final double DEALER_HURT_DROP_CHANCE_ONE = 0.025D;
    public static final int DEALER_HURT_DROP_THREE = 3;
    public static final int DEALER_HURT_DROP_TWO = 2;
    public static final int DEALER_HURT_DROP_ONE = 1;
    /*
     * The fumble is rationed per dealer: a window of one minute opens with the first hit that frees dice, and at most
     * six dice leave the pockets inside it. Once the window is spent the pockets stay empty until it runs out, so a
     * dealer cannot be farmed faster than this.
     */
    /* One minute, written out because the tick helper is declared further down and cannot be read from here. */
    public static final int DEALER_HURT_DROP_WINDOW_TICKS = 1200;
    public static final int DEALER_HURT_DROP_WINDOW_MAX = 6;
    /*
     * The idle name of a dealer is only readable up close and only for the player who looks at it, while the hosting
     * and recruiting names are business and stay over its head for everyone.
     */
    public static final double DEALER_NAME_REVEAL_RANGE = 6.0D;
    /* Half a right angle to each side of the direction the player looks at. */
    public static final double DEALER_NAME_LOOK_DOT = 0.7D;
    public static final int DEALER_SPAWN_RADIUS = 6;
    public static final int DEALER_SPAWN_VERTICAL = 3;
    public static final int DEALER_SUNSET_TICK = 12000;
    public static final int DEALER_SUNRISE_TICK = 23000;

    /*
     * Dealer reward: one spawn egg per this many consumed gambler's dice.
     */
    public static final String DEALER_USAGE_DATA_NAME = "devil_bone_gambler_dealer_usage";
    public static final int DEALER_USAGE_PER_EGG = 64;

    /*
     * Dealer wager and dealer panel.
     */
    public static final int DEALER_WAGER_RECRUIT_SECONDS = 120;
    public static final int DEALER_WAGER_REMIND_SECONDS = 30;
    /* The recruit chime only rings at the start of a wager and again shortly before it begins. */
    public static final int DEALER_WAGER_CHIME_HEAD_SECONDS = 3;
    public static final int DEALER_WAGER_CHIME_TAIL_SECONDS = 10;
    public static final int DEALER_WAGER_PULL_SECONDS = 5;
    public static final int DEALER_WAGER_PULL_TICKS = 40;
    public static final int DEALER_WAGER_MAX_PLAYERS = 8;
    public static final int DEALER_WAGER_MIN_PLAYERS = 2;
    public static final int DEALER_WAGER_RADIUS = 6;
    public static final int DEALER_BET_SLOTS = 9;
    public static final int DEALER_BUSY_TIMEOUT_TICKS = 200;
    public static final int TICKS_PER_SECOND = 20;
    public static final int DEALER_PARTICLE_COUNT = 8;
    public static final float DEALER_SOUND_VOLUME = 0.6F;

    /*
     * Random pools: the sample space of the random prize tokens, rebuilt from the registries on every world load.
     */
    public static final String RANDOM_POOL_FILE = "random_pools.json";
    /*
     * Longest player name the admin panel accepts. A vanilla name is at most 16 characters, the margin covers a
     * hand written file without opening the field to arbitrary text.
     */
    public static final int MAX_PLAYER_NAME_LENGTH = 32;
    public static final String RANDOM_POOL_ITEM = "item";
    public static final String RANDOM_POOL_EFFECT = "effect";
    public static final String RANDOM_POOL_ENTITY = "entity";
    public static final String RANDOM_POOL_ENCHANTMENT = "enchantment";
    public static final String RANDOM_POOL_POTION = "potion";
    public static final String[] RANDOM_POOLS = {
        RANDOM_POOL_ITEM,
        RANDOM_POOL_EFFECT,
        RANDOM_POOL_ENTITY,
        RANDOM_POOL_ENCHANTMENT,
        RANDOM_POOL_POTION
    };
    /*
     * One sample id per pool, aligned with RANDOM_POOLS, so that a random token still parses during syntax checks.
     */
    public static final String[] RANDOM_POOL_SAMPLES = {
        "minecraft:stone",
        "minecraft:speed",
        "minecraft:pig",
        "minecraft:sharpness",
        "minecraft:water"
    };
    public static final String RANDOM_TOKEN_OPEN = "${random:";
    public static final String RANDOM_TOKEN_CLOSE = "}";
    public static final String RANDOM_AIR_ID = "minecraft:air";

    /**
     * Pool name at the given index, or null when the index is outside the table.
     */
    public static String randomPoolAt(int index)
    {
        return index < 0 || index >= RANDOM_POOLS.length ? null : RANDOM_POOLS[index];
    }

    /**
     * Sample id of one pool, or null when the pool name is unknown.
     */
    public static String randomSampleOf(String pool)
    {
        for (int index = 0; index < RANDOM_POOLS.length; index++)
        {
            if (RANDOM_POOLS[index].equals(pool))
            {
                return RANDOM_POOL_SAMPLES[index];
            }
        }

        return null;
    }

    private ModConstants()
    {
    }

    /**
     * Altar radius for a given accumulated offering value.
     */
    public static int altarRadiusFor(long value)
    {
        long steps = Math.max(0L, value) / ALTAR_RADIUS_VALUE_STEP;
        long radius = ALTAR_BASE_RADIUS + steps;
        return (int) Math.min(ALTAR_MAX_RADIUS, radius);
    }

    /**
     * Altar effect level for a given accumulated offering value.
     */
    public static int altarLevelFor(long value)
    {
        long steps = Math.max(0L, value) / ALTAR_LEVEL_VALUE_STEP;
        return (int) Math.min(ALTAR_MAX_LEVEL, steps);
    }
}
