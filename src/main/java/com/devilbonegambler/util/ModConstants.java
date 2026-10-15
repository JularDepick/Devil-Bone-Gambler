/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

import com.devilbonegambler.DevilBoneGambler;

/**
 * Design detail constants that a developer may tune without touching core logic.
 * Every value here is a documented design detail of v0.1.0.
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
    public static final String SHOP_FILE = "shop.json";
    public static final String ECONOMY_FILE = "economy.json";
    public static final String CONFIG_DOMAIN_PRIZE_POOL = "prize_pool";
    public static final String CONFIG_DOMAIN_ITEM_VALUES = "item_values";
    public static final String CONFIG_DOMAIN_SHOP = "shop";
    public static final String CONFIG_DOMAIN_ECONOMY = "economy";

    /*
     * Save data names
     */
    public static final String LUCK_DATA_NAME = "devil_bone_gambler_luck";
    public static final String ACCOUNT_DATA_NAME = "devil_bone_gambler_accounts";
    public static final String EXCHANGE_DATA_NAME = "devil_bone_gambler_exchange";

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
     * Wager
     */
    public static final int WAGER_COUNTDOWN_SECONDS = 5;
    public static final int WAGER_RADIUS = 6;
    public static final int WAGER_SNEAK_WINDOW_TICKS = 20;
    public static final int WAGER_MIN_PLAYERS = 1;

    /*
     * Economy
     */
    public static final long MAX_BALANCE = 1000000000000L;
    public static final double ASSET_RETENTION_RATE = 0.95D;
    public static final double EXCHANGE_FEE_RATE = 0.01D;
    public static final int EXCHANGE_ORDER_TIMEOUT_TICKS = 72000;
    public static final int MAX_ORDERS_PER_PLAYER = 16;
    public static final int MAX_ESCROW_ENTRIES = 64;
    public static final int MAX_SHOP_OFFERS = 64;
    public static final int MAX_RECYCLE_SLOTS = 256;
    public static final int VIP_RETRY_TICKS = 1200;
    public static final double DEFAULT_RECYCLE_MULTIPLIER = 0.25D;
    public static final double DEFAULT_RECYCLE_COIN_SHARE = 0.7D;
    public static final double DEFAULT_RECYCLE_TEAR_SHARE = 0.3D;
    public static final long DEFAULT_VIP_PRICE_COIN = 500L;
    public static final long DEFAULT_VIP_PRICE_TEAR = 0L;
    public static final int DEFAULT_VIP_DURATION_TICKS = 72000;

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
