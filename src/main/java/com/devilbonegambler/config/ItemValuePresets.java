/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Built in baseline values for common vanilla items and entity types.
 * The tables are only expanded into the config file when that file is created or when a key is missing.
 */
public final class ItemValuePresets
{
    public static final String VANILLA_NAMESPACE = "minecraft";

    private static final Map<String, Integer> ITEM_PRESETS = buildItemPresets();
    private static final Map<String, Integer> ENTITY_PRESETS = buildEntityPresets();

    private ItemValuePresets()
    {
    }

    public static Map<String, Integer> items()
    {
        return Collections.unmodifiableMap(ITEM_PRESETS);
    }

    public static Map<String, Integer> entities()
    {
        return Collections.unmodifiableMap(ENTITY_PRESETS);
    }

    /**
     * Preset value of a vanilla item, or the fallback when the item is not covered by the table.
     */
    public static int presetItemOf(String itemId, int fallback)
    {
        Integer value = ITEM_PRESETS.get(itemId);
        return value == null ? fallback : value.intValue();
    }

    /**
     * Preset value of a vanilla entity type, or the fallback when it is not covered by the table.
     */
    public static int presetEntityOf(String entityId, int fallback)
    {
        Integer value = ENTITY_PRESETS.get(entityId);
        return value == null ? fallback : value.intValue();
    }

    private static Map<String, Integer> buildItemPresets()
    {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();

        putItem(result, "dirt", 1);
        putItem(result, "grass_block", 1);
        putItem(result, "stone", 1);
        putItem(result, "cobblestone", 1);
        putItem(result, "sand", 1);
        putItem(result, "gravel", 1);
        putItem(result, "oak_planks", 1);
        putItem(result, "stick", 1);
        putItem(result, "torch", 1);
        putItem(result, "string", 1);
        putItem(result, "bone", 1);
        putItem(result, "feather", 1);
        putItem(result, "rotten_flesh", 1);
        putItem(result, "clay", 2);
        putItem(result, "oak_log", 2);
        putItem(result, "wheat", 2);
        putItem(result, "paper", 2);
        putItem(result, "coal", 3);
        putItem(result, "charcoal", 3);
        putItem(result, "leather", 3);
        putItem(result, "apple", 4);
        putItem(result, "gunpowder", 4);
        putItem(result, "slime_ball", 4);
        putItem(result, "redstone", 4);
        putItem(result, "bread", 5);
        putItem(result, "book", 5);
        putItem(result, "quartz", 5);
        putItem(result, "lapis_lazuli", 6);
        putItem(result, "glowstone", 6);
        putItem(result, "obsidian", 8);
        putItem(result, "copper_ingot", 8);
        putItem(result, "experience_bottle", 12);
        putItem(result, "ender_pearl", 15);
        putItem(result, "iron_ingot", 20);
        putItem(result, "blaze_rod", 25);
        putItem(result, "gold_ingot", 30);
        putItem(result, "emerald", 60);
        putItem(result, "golden_apple", 80);
        putItem(result, "shulker_shell", 90);
        putItem(result, "diamond", 100);
        putItem(result, "netherite_scrap", 100);
        putItem(result, "ancient_debris", 120);
        putItem(result, "netherite_ingot", 400);
        putItem(result, "elytra", 600);
        putItem(result, "enchanted_golden_apple", 800);
        putItem(result, "nether_star", 1500);
        putItem(result, "dragon_egg", 2000);

        return result;
    }

    private static Map<String, Integer> buildEntityPresets()
    {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();

        putEntity(result, "chicken", 5);
        putEntity(result, "rabbit", 5);
        putEntity(result, "cod", 5);
        putEntity(result, "salmon", 6);
        putEntity(result, "bat", 6);
        putEntity(result, "parrot", 8);
        putEntity(result, "cow", 10);
        putEntity(result, "pig", 10);
        putEntity(result, "sheep", 10);
        putEntity(result, "cat", 12);
        putEntity(result, "wolf", 12);
        putEntity(result, "fox", 12);
        putEntity(result, "ocelot", 12);
        putEntity(result, "turtle", 15);
        putEntity(result, "bee", 15);
        putEntity(result, "panda", 20);
        putEntity(result, "polar_bear", 20);
        putEntity(result, "llama", 20);
        putEntity(result, "horse", 25);
        putEntity(result, "dolphin", 25);
        putEntity(result, "squid", 10);
        putEntity(result, "zombie", 30);
        putEntity(result, "skeleton", 30);
        putEntity(result, "spider", 30);
        putEntity(result, "creeper", 40);
        putEntity(result, "slime", 20);
        putEntity(result, "magma_cube", 35);
        putEntity(result, "witch", 60);
        putEntity(result, "pillager", 60);
        putEntity(result, "vindicator", 80);
        putEntity(result, "evoker", 150);
        putEntity(result, "ravager", 200);
        putEntity(result, "enderman", 100);
        putEntity(result, "blaze", 120);
        putEntity(result, "ghast", 150);
        putEntity(result, "hoglin", 100);
        putEntity(result, "piglin_brute", 150);
        putEntity(result, "guardian", 120);
        putEntity(result, "elder_guardian", 600);
        putEntity(result, "shulker", 150);
        putEntity(result, "wither_skeleton", 200);
        putEntity(result, "iron_golem", 300);
        putEntity(result, "wither", 2000);
        putEntity(result, "ender_dragon", 5000);

        return result;
    }

    private static void putItem(Map<String, Integer> target, String path, int value)
    {
        target.put(VANILLA_NAMESPACE + ":" + path, Integer.valueOf(value));
    }

    private static void putEntity(Map<String, Integer> target, String path, int value)
    {
        target.put(VANILLA_NAMESPACE + ":" + path, Integer.valueOf(value));
    }
}
