/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

import com.devilbonegambler.DevilBoneGambler;

/**
 * Design detail constants that a developer may tune without touching core logic.
 * Every value here is a documented design detail of v0.2.0.
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
    public static final String CONFIG_DOMAIN_PRIZE_POOL = "prize_pool";
    public static final String CONFIG_DOMAIN_ITEM_VALUES = "item_values";

    /*
     * Save data names
     */
    public static final String LUCK_DATA_NAME = "devil_bone_gambler_luck";

    /*
     * Luck
     */
    public static final int BASE_LUCK = 50;
    public static final int MIN_LUCK = 0;
    public static final int MAX_LUCK = 200;

    /*
     * Value table
     */
    public static final int DEFAULT_ITEM_VALUE = 10;
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
    public static final int MAX_ENTRY_ID_LENGTH = 64;

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
    public static final float ALTAR_EXHAUSTION_PER_TICK = 0.01F;
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
    public static final int ADMIN_PERMISSION_LEVEL = 2;
    public static final int DRAW_COMMAND_PERMISSION_LEVEL = 4;

    /*
     * Value table book
     */
    public static final String VALUE_TABLE_TAG = "DevilBoneGamblerValueTable";
    public static final int VALUE_TABLE_PAGE_ENTRIES = 8;

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
     * Result display modes
     */
    public static final String NOTIFY_MODE_TITLE = "title";
    public static final String NOTIFY_MODE_SUBTITLE = "subtitle";
    public static final String NOTIFY_MODE_ACTIONBAR = "actionbar";
    public static final String NOTIFY_MODE_TELLRAW = "tellraw";
    public static final String NOTIFY_MODE_NONE = "none";

    public static final String[] NOTIFY_MODES = {
        NOTIFY_MODE_ACTIONBAR,
        NOTIFY_MODE_TITLE,
        NOTIFY_MODE_SUBTITLE,
        NOTIFY_MODE_TELLRAW,
        NOTIFY_MODE_NONE
    };

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
