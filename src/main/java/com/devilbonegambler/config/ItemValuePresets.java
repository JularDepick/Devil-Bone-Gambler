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

        /*
        * Ores, raw materials and mineral resources
        */
        putItem(result, "coal_ore", 4);
        putItem(result, "deepslate_coal_ore", 5);
        putItem(result, "copper_ore", 10);
        putItem(result, "deepslate_copper_ore", 12);
        putItem(result, "iron_ore", 22);
        putItem(result, "deepslate_iron_ore", 24);
        putItem(result, "gold_ore", 33);
        putItem(result, "deepslate_gold_ore", 36);
        putItem(result, "nether_gold_ore", 33);
        putItem(result, "redstone_ore", 6);
        putItem(result, "deepslate_redstone_ore", 7);
        putItem(result, "lapis_ore", 8);
        putItem(result, "deepslate_lapis_ore", 9);
        putItem(result, "diamond_ore", 110);
        putItem(result, "deepslate_diamond_ore", 120);
        putItem(result, "emerald_ore", 65);
        putItem(result, "deepslate_emerald_ore", 70);
        putItem(result, "nether_quartz_ore", 7);
        putItem(result, "raw_copper", 7);
        putItem(result, "raw_iron", 18);
        putItem(result, "raw_gold", 27);
        putItem(result, "raw_copper_block", 63);
        putItem(result, "raw_iron_block", 162);
        putItem(result, "raw_gold_block", 243);
        putItem(result, "iron_nugget", 3);
        putItem(result, "gold_nugget", 4);
        putItem(result, "coal_block", 27);
        putItem(result, "iron_block", 243);
        putItem(result, "gold_block", 324);
        putItem(result, "copper_block", 117);
        putItem(result, "diamond_block", 900);
        putItem(result, "emerald_block", 540);
        putItem(result, "lapis_block", 54);
        putItem(result, "redstone_block", 36);
        putItem(result, "netherite_block", 4896);
        putItem(result, "amethyst_block", 20);
        putItem(result, "amethyst_shard", 5);
        putItem(result, "amethyst_cluster", 12);
        putItem(result, "budding_amethyst", 40);
        putItem(result, "small_amethyst_bud", 3);
        putItem(result, "medium_amethyst_bud", 5);
        putItem(result, "large_amethyst_bud", 8);
        putItem(result, "glowstone_dust", 3);
        putItem(result, "prismarine_shard", 3);
        putItem(result, "prismarine_crystals", 5);
        putItem(result, "sea_lantern", 37);
        putItem(result, "magma_cream", 14);
        putItem(result, "blaze_powder", 13);
        putItem(result, "ghast_tear", 40);
        putItem(result, "phantom_membrane", 20);
        putItem(result, "rabbit_hide", 4);
        putItem(result, "rabbit_foot", 25);
        putItem(result, "bone_meal", 2);
        putItem(result, "bone_block", 18);
        putItem(result, "cobweb", 4);
        putItem(result, "flint", 3);
        putItem(result, "clay_ball", 1);
        putItem(result, "brick", 3);
        putItem(result, "nether_brick", 3);
        putItem(result, "sugar", 4);
        putItem(result, "honeycomb", 8);
        putItem(result, "honeycomb_block", 32);
        putItem(result, "honey_block", 40);
        putItem(result, "slime_block", 36);
        putItem(result, "hay_block", 18);
        putItem(result, "ender_eye", 30);
        putItem(result, "nautilus_shell", 25);
        putItem(result, "scute", 20);
        putItem(result, "heart_of_the_sea", 150);
        putItem(result, "ink_sac", 3);
        putItem(result, "glow_ink_sac", 6);
        putItem(result, "sponge", 15);
        putItem(result, "wet_sponge", 12);
        putItem(result, "echo_shard", 60);
        putItem(result, "disc_fragment_5", 40);
        putItem(result, "nether_wart", 3);
        putItem(result, "nether_wart_block", 27);
        putItem(result, "warped_wart_block", 6);
        putItem(result, "shroomlight", 8);
        putItem(result, "gilded_blackstone", 30);
        putItem(result, "crying_obsidian", 12);
        putItem(result, "respawn_anchor", 108);
        /*
        * Mob drops and loot
        */
        putItem(result, "arrow", 2);
        putItem(result, "spectral_arrow", 7);
        putItem(result, "tipped_arrow", 8);
        putItem(result, "spider_eye", 5);
        putItem(result, "fermented_spider_eye", 12);
        putItem(result, "glistering_melon_slice", 26);
        putItem(result, "golden_carrot", 27);
        putItem(result, "lead", 8);
        putItem(result, "goat_horn", 60);
        putItem(result, "skeleton_skull", 150);
        putItem(result, "wither_skeleton_skull", 400);
        putItem(result, "zombie_head", 150);
        putItem(result, "creeper_head", 150);
        putItem(result, "dragon_head", 400);
        putItem(result, "piglin_head", 150);
        putItem(result, "player_head", 200);
        putItem(result, "egg", 3);
        putItem(result, "turtle_egg", 10);
        putItem(result, "sniffer_egg", 200);
        putItem(result, "frogspawn", 5);
        putItem(result, "fire_charge", 8);
        putItem(result, "armor_stand", 25);
        putItem(result, "item_frame", 48);
        putItem(result, "glow_item_frame", 9);
        putItem(result, "painting", 36);
        putItem(result, "flower_pot", 9);
        putItem(result, "decorated_pot", 15);
        putItem(result, "creeper_banner_pattern", 152);
        putItem(result, "flower_banner_pattern", 20);
        putItem(result, "globe_banner_pattern", 40);
        putItem(result, "mojang_banner_pattern", 802);
        putItem(result, "piglin_banner_pattern", 20);
        putItem(result, "skull_banner_pattern", 402);
        putItem(result, "angler_pottery_sherd", 8);
        putItem(result, "archer_pottery_sherd", 8);
        putItem(result, "arms_up_pottery_sherd", 8);
        putItem(result, "blade_pottery_sherd", 8);
        putItem(result, "brewer_pottery_sherd", 8);
        putItem(result, "burn_pottery_sherd", 8);
        putItem(result, "danger_pottery_sherd", 8);
        putItem(result, "explorer_pottery_sherd", 8);
        putItem(result, "friend_pottery_sherd", 8);
        putItem(result, "heart_pottery_sherd", 8);
        putItem(result, "heartbreak_pottery_sherd", 8);
        putItem(result, "howl_pottery_sherd", 8);
        putItem(result, "miner_pottery_sherd", 8);
        putItem(result, "mourner_pottery_sherd", 8);
        putItem(result, "plenty_pottery_sherd", 8);
        putItem(result, "prize_pottery_sherd", 8);
        putItem(result, "sheaf_pottery_sherd", 8);
        putItem(result, "shelter_pottery_sherd", 8);
        putItem(result, "skull_pottery_sherd", 8);
        putItem(result, "snort_pottery_sherd", 8);
        putItem(result, "music_disc_13", 100);
        putItem(result, "music_disc_cat", 100);
        putItem(result, "music_disc_blocks", 100);
        putItem(result, "music_disc_chirp", 100);
        putItem(result, "music_disc_far", 100);
        putItem(result, "music_disc_mall", 100);
        putItem(result, "music_disc_mellohi", 100);
        putItem(result, "music_disc_stal", 100);
        putItem(result, "music_disc_strad", 100);
        putItem(result, "music_disc_ward", 100);
        putItem(result, "music_disc_11", 100);
        putItem(result, "music_disc_wait", 100);
        putItem(result, "music_disc_otherside", 100);
        putItem(result, "music_disc_pigstep", 100);
        putItem(result, "music_disc_5", 360);
        putItem(result, "music_disc_relic", 100);
        putItem(result, "suspicious_sand", 5);
        putItem(result, "suspicious_gravel", 5);
        /*
        * Food, farming and plants
        */
        putItem(result, "beef", 6);
        putItem(result, "cooked_beef", 10);
        putItem(result, "porkchop", 6);
        putItem(result, "cooked_porkchop", 10);
        putItem(result, "chicken", 4);
        putItem(result, "cooked_chicken", 8);
        putItem(result, "mutton", 5);
        putItem(result, "cooked_mutton", 9);
        putItem(result, "rabbit", 5);
        putItem(result, "cooked_rabbit", 9);
        putItem(result, "cod", 5);
        putItem(result, "cooked_cod", 8);
        putItem(result, "salmon", 6);
        putItem(result, "cooked_salmon", 10);
        putItem(result, "tropical_fish", 5);
        putItem(result, "pufferfish", 8);
        putItem(result, "cookie", 3);
        putItem(result, "cake", 58);
        putItem(result, "pumpkin_pie", 12);
        putItem(result, "baked_potato", 6);
        putItem(result, "potato", 2);
        putItem(result, "poisonous_potato", 2);
        putItem(result, "carrot", 3);
        putItem(result, "beetroot", 2);
        putItem(result, "melon_slice", 2);
        putItem(result, "melon", 18);
        putItem(result, "pumpkin", 4);
        putItem(result, "carved_pumpkin", 5);
        putItem(result, "jack_o_lantern", 7);
        putItem(result, "sweet_berries", 4);
        putItem(result, "glow_berries", 4);
        putItem(result, "dried_kelp", 3);
        putItem(result, "dried_kelp_block", 27);
        putItem(result, "kelp", 2);
        putItem(result, "mushroom_stew", 12);
        putItem(result, "beetroot_soup", 13);
        putItem(result, "rabbit_stew", 22);
        putItem(result, "suspicious_stew", 15);
        putItem(result, "honey_bottle", 10);
        putItem(result, "milk_bucket", 15);
        putItem(result, "wheat_seeds", 1);
        putItem(result, "beetroot_seeds", 1);
        putItem(result, "melon_seeds", 2);
        putItem(result, "pumpkin_seeds", 1);
        putItem(result, "torchflower_seeds", 10);
        putItem(result, "pitcher_pod", 10);
        putItem(result, "sugar_cane", 2);
        putItem(result, "bamboo", 2);
        putItem(result, "cactus", 3);
        putItem(result, "cocoa_beans", 3);
        putItem(result, "brown_mushroom", 3);
        putItem(result, "red_mushroom", 3);
        putItem(result, "azalea", 3);
        putItem(result, "flowering_azalea", 4);
        putItem(result, "big_dripleaf", 3);
        putItem(result, "small_dripleaf", 2);
        putItem(result, "spore_blossom", 5);
        putItem(result, "sea_pickle", 4);
        putItem(result, "torchflower", 8);
        putItem(result, "pitcher_plant", 8);
        putItem(result, "chorus_fruit", 3);
        putItem(result, "popped_chorus_fruit", 5);
        putItem(result, "chorus_flower", 8);
        putItem(result, "chorus_plant", 5);
        putItem(result, "dandelion", 2);
        putItem(result, "poppy", 2);
        putItem(result, "blue_orchid", 3);
        putItem(result, "allium", 3);
        putItem(result, "azure_bluet", 2);
        putItem(result, "red_tulip", 2);
        putItem(result, "orange_tulip", 2);
        putItem(result, "white_tulip", 2);
        putItem(result, "pink_tulip", 2);
        putItem(result, "oxeye_daisy", 2);
        putItem(result, "cornflower", 3);
        putItem(result, "lily_of_the_valley", 3);
        putItem(result, "wither_rose", 5);
        putItem(result, "sunflower", 4);
        putItem(result, "lilac", 4);
        putItem(result, "rose_bush", 4);
        putItem(result, "peony", 4);
        /*
        * Wood and wood products
        */
        putItem(result, "stripped_oak_log", 2);
        putItem(result, "oak_wood", 3);
        putItem(result, "oak_sapling", 1);
        putItem(result, "oak_door", 2);
        putItem(result, "oak_trapdoor", 3);
        putItem(result, "oak_fence", 4);
        putItem(result, "oak_fence_gate", 18);
        putItem(result, "oak_sign", 4);
        putItem(result, "oak_slab", 1);
        putItem(result, "oak_stairs", 2);
        putItem(result, "oak_boat", 6);
        putItem(result, "oak_chest_boat", 14);
        putItem(result, "spruce_log", 2);
        putItem(result, "stripped_spruce_log", 2);
        putItem(result, "spruce_wood", 3);
        putItem(result, "spruce_planks", 1);
        putItem(result, "spruce_sapling", 1);
        putItem(result, "spruce_door", 2);
        putItem(result, "spruce_trapdoor", 3);
        putItem(result, "spruce_fence", 4);
        putItem(result, "spruce_fence_gate", 18);
        putItem(result, "spruce_sign", 4);
        putItem(result, "spruce_slab", 1);
        putItem(result, "spruce_stairs", 2);
        putItem(result, "spruce_boat", 6);
        putItem(result, "spruce_chest_boat", 14);
        putItem(result, "birch_log", 2);
        putItem(result, "stripped_birch_log", 2);
        putItem(result, "birch_wood", 3);
        putItem(result, "birch_planks", 1);
        putItem(result, "birch_sapling", 1);
        putItem(result, "birch_door", 2);
        putItem(result, "birch_trapdoor", 3);
        putItem(result, "birch_fence", 4);
        putItem(result, "birch_fence_gate", 18);
        putItem(result, "birch_sign", 4);
        putItem(result, "birch_slab", 1);
        putItem(result, "birch_stairs", 2);
        putItem(result, "birch_boat", 6);
        putItem(result, "birch_chest_boat", 14);
        putItem(result, "jungle_log", 2);
        putItem(result, "stripped_jungle_log", 2);
        putItem(result, "jungle_wood", 3);
        putItem(result, "jungle_planks", 1);
        putItem(result, "jungle_sapling", 1);
        putItem(result, "jungle_door", 2);
        putItem(result, "jungle_trapdoor", 3);
        putItem(result, "jungle_fence", 4);
        putItem(result, "jungle_fence_gate", 18);
        putItem(result, "jungle_sign", 4);
        putItem(result, "jungle_slab", 1);
        putItem(result, "jungle_stairs", 2);
        putItem(result, "jungle_boat", 6);
        putItem(result, "jungle_chest_boat", 14);
        putItem(result, "acacia_log", 2);
        putItem(result, "stripped_acacia_log", 2);
        putItem(result, "acacia_wood", 3);
        putItem(result, "acacia_planks", 1);
        putItem(result, "acacia_sapling", 1);
        putItem(result, "acacia_door", 2);
        putItem(result, "acacia_trapdoor", 3);
        putItem(result, "acacia_fence", 4);
        putItem(result, "acacia_fence_gate", 18);
        putItem(result, "acacia_sign", 4);
        putItem(result, "acacia_slab", 1);
        putItem(result, "acacia_stairs", 2);
        putItem(result, "acacia_boat", 6);
        putItem(result, "acacia_chest_boat", 14);
        putItem(result, "dark_oak_log", 2);
        putItem(result, "stripped_dark_oak_log", 2);
        putItem(result, "dark_oak_wood", 3);
        putItem(result, "dark_oak_planks", 1);
        putItem(result, "dark_oak_sapling", 1);
        putItem(result, "dark_oak_door", 2);
        putItem(result, "dark_oak_trapdoor", 3);
        putItem(result, "dark_oak_fence", 4);
        putItem(result, "dark_oak_fence_gate", 18);
        putItem(result, "dark_oak_sign", 4);
        putItem(result, "dark_oak_slab", 1);
        putItem(result, "dark_oak_stairs", 2);
        putItem(result, "dark_oak_boat", 6);
        putItem(result, "dark_oak_chest_boat", 14);
        putItem(result, "mangrove_log", 2);
        putItem(result, "stripped_mangrove_log", 2);
        putItem(result, "mangrove_wood", 3);
        putItem(result, "mangrove_planks", 1);
        putItem(result, "mangrove_propagule", 2);
        putItem(result, "mangrove_door", 2);
        putItem(result, "mangrove_trapdoor", 3);
        putItem(result, "mangrove_fence", 4);
        putItem(result, "mangrove_fence_gate", 18);
        putItem(result, "mangrove_sign", 4);
        putItem(result, "mangrove_slab", 1);
        putItem(result, "mangrove_stairs", 2);
        putItem(result, "mangrove_boat", 6);
        putItem(result, "mangrove_chest_boat", 14);
        putItem(result, "cherry_log", 2);
        putItem(result, "stripped_cherry_log", 2);
        putItem(result, "cherry_wood", 3);
        putItem(result, "cherry_planks", 1);
        putItem(result, "cherry_sapling", 1);
        putItem(result, "cherry_door", 2);
        putItem(result, "cherry_trapdoor", 3);
        putItem(result, "cherry_fence", 4);
        putItem(result, "cherry_fence_gate", 18);
        putItem(result, "cherry_sign", 4);
        putItem(result, "cherry_slab", 1);
        putItem(result, "cherry_stairs", 2);
        putItem(result, "cherry_boat", 6);
        putItem(result, "cherry_chest_boat", 14);
        putItem(result, "bamboo_block", 18);
        putItem(result, "stripped_bamboo_block", 2);
        putItem(result, "bamboo_planks", 1);
        putItem(result, "bamboo_mosaic", 2);
        putItem(result, "bamboo_door", 2);
        putItem(result, "bamboo_trapdoor", 3);
        putItem(result, "bamboo_fence", 4);
        putItem(result, "bamboo_fence_gate", 18);
        putItem(result, "bamboo_sign", 4);
        putItem(result, "bamboo_slab", 1);
        putItem(result, "bamboo_stairs", 2);
        putItem(result, "bamboo_raft", 6);
        putItem(result, "bamboo_chest_raft", 14);
        putItem(result, "crimson_stem", 2);
        putItem(result, "stripped_crimson_stem", 2);
        putItem(result, "crimson_hyphae", 3);
        putItem(result, "stripped_crimson_hyphae", 3);
        putItem(result, "crimson_planks", 1);
        putItem(result, "crimson_fungus", 2);
        putItem(result, "crimson_door", 2);
        putItem(result, "crimson_trapdoor", 3);
        putItem(result, "crimson_fence", 4);
        putItem(result, "crimson_fence_gate", 18);
        putItem(result, "crimson_sign", 4);
        putItem(result, "crimson_slab", 1);
        putItem(result, "crimson_stairs", 2);
        putItem(result, "warped_stem", 2);
        putItem(result, "stripped_warped_stem", 2);
        putItem(result, "warped_hyphae", 3);
        putItem(result, "stripped_warped_hyphae", 3);
        putItem(result, "warped_planks", 1);
        putItem(result, "warped_fungus", 2);
        putItem(result, "warped_door", 2);
        putItem(result, "warped_trapdoor", 3);
        putItem(result, "warped_fence", 4);
        putItem(result, "warped_fence_gate", 18);
        putItem(result, "warped_sign", 4);
        putItem(result, "warped_slab", 1);
        putItem(result, "warped_stairs", 2);
        /*
        * Stone and building blocks
        */
        putItem(result, "stone_slab", 1);
        putItem(result, "stone_stairs", 2);
        putItem(result, "stone_button", 1);
        putItem(result, "stone_pressure_plate", 2);
        putItem(result, "smooth_stone", 2);
        putItem(result, "smooth_stone_slab", 1);
        putItem(result, "stone_bricks", 2);
        putItem(result, "stone_brick_slab", 1);
        putItem(result, "stone_brick_stairs", 3);
        putItem(result, "stone_brick_wall", 2);
        putItem(result, "chiseled_stone_bricks", 2);
        putItem(result, "cracked_stone_bricks", 2);
        putItem(result, "mossy_stone_bricks", 12);
        putItem(result, "mossy_cobblestone", 11);
        putItem(result, "granite", 7);
        putItem(result, "polished_granite", 2);
        putItem(result, "granite_slab", 1);
        putItem(result, "granite_stairs", 3);
        putItem(result, "diorite", 6);
        putItem(result, "polished_diorite", 2);
        putItem(result, "diorite_slab", 1);
        putItem(result, "diorite_stairs", 3);
        putItem(result, "andesite", 2);
        putItem(result, "polished_andesite", 2);
        putItem(result, "andesite_slab", 1);
        putItem(result, "andesite_stairs", 3);
        putItem(result, "deepslate", 1);
        putItem(result, "cobbled_deepslate", 1);
        putItem(result, "cobbled_deepslate_stairs", 2);
        putItem(result, "polished_deepslate", 2);
        putItem(result, "polished_deepslate_stairs", 3);
        putItem(result, "deepslate_bricks", 2);
        putItem(result, "deepslate_brick_stairs", 3);
        putItem(result, "deepslate_tiles", 2);
        putItem(result, "deepslate_tile_stairs", 3);
        putItem(result, "chiseled_deepslate", 20);
        putItem(result, "cracked_deepslate_bricks", 2);
        putItem(result, "reinforced_deepslate", 50);
        putItem(result, "blackstone", 1);
        putItem(result, "polished_blackstone", 2);
        putItem(result, "polished_blackstone_bricks", 2);
        putItem(result, "polished_blackstone_button", 2);
        putItem(result, "polished_blackstone_pressure_plate", 4);
        putItem(result, "basalt", 1);
        putItem(result, "polished_basalt", 2);
        putItem(result, "smooth_basalt", 2);
        putItem(result, "tuff", 1);
        putItem(result, "calcite", 2);
        putItem(result, "dripstone_block", 8);
        putItem(result, "pointed_dripstone", 2);
        putItem(result, "sandstone", 4);
        putItem(result, "sandstone_slab", 1);
        putItem(result, "sandstone_stairs", 3);
        putItem(result, "sandstone_wall", 2);
        putItem(result, "chiseled_sandstone", 2);
        putItem(result, "cut_sandstone", 2);
        putItem(result, "smooth_sandstone", 2);
        putItem(result, "red_sand", 1);
        putItem(result, "red_sandstone", 4);
        putItem(result, "red_sandstone_slab", 1);
        putItem(result, "red_sandstone_stairs", 3);
        putItem(result, "quartz_block", 22);
        putItem(result, "quartz_bricks", 24);
        putItem(result, "quartz_pillar", 22);
        putItem(result, "quartz_slab", 11);
        putItem(result, "quartz_stairs", 33);
        putItem(result, "smooth_quartz", 22);
        putItem(result, "chiseled_quartz_block", 22);
        putItem(result, "bricks", 12);
        putItem(result, "brick_slab", 6);
        putItem(result, "brick_stairs", 18);
        putItem(result, "brick_wall", 12);
        putItem(result, "mud_bricks", 10);
        putItem(result, "mud_brick_slab", 3);
        putItem(result, "mud_brick_stairs", 9);
        putItem(result, "mud_brick_wall", 6);
        putItem(result, "nether_bricks", 12);
        putItem(result, "nether_brick_fence", 9);
        putItem(result, "nether_brick_slab", 6);
        putItem(result, "nether_brick_stairs", 18);
        putItem(result, "nether_brick_wall", 12);
        putItem(result, "cracked_nether_bricks", 12);
        putItem(result, "chiseled_nether_bricks", 12);
        putItem(result, "red_nether_bricks", 12);
        putItem(result, "red_nether_brick_stairs", 18);
        putItem(result, "prismarine", 12);
        putItem(result, "prismarine_slab", 6);
        putItem(result, "prismarine_stairs", 18);
        putItem(result, "prismarine_wall", 12);
        putItem(result, "prismarine_bricks", 27);
        putItem(result, "dark_prismarine", 34);
        putItem(result, "end_stone", 3);
        putItem(result, "end_stone_bricks", 6);
        putItem(result, "end_stone_brick_stairs", 9);
        putItem(result, "purpur_block", 20);
        putItem(result, "purpur_pillar", 20);
        putItem(result, "purpur_stairs", 30);
        putItem(result, "cut_copper", 72);
        putItem(result, "cut_copper_slab", 36);
        putItem(result, "cut_copper_stairs", 108);
        putItem(result, "exposed_copper", 72);
        putItem(result, "weathered_copper", 72);
        putItem(result, "oxidized_copper", 72);
        putItem(result, "waxed_copper_block", 80);
        putItem(result, "glass", 2);
        putItem(result, "glass_pane", 1);
        putItem(result, "tinted_glass", 12);
        putItem(result, "white_wool", 14);
        putItem(result, "orange_wool", 14);
        putItem(result, "magenta_wool", 14);
        putItem(result, "light_blue_wool", 14);
        putItem(result, "yellow_wool", 14);
        putItem(result, "lime_wool", 14);
        putItem(result, "pink_wool", 14);
        putItem(result, "gray_wool", 14);
        putItem(result, "light_gray_wool", 14);
        putItem(result, "cyan_wool", 14);
        putItem(result, "purple_wool", 14);
        putItem(result, "blue_wool", 14);
        putItem(result, "brown_wool", 14);
        putItem(result, "green_wool", 14);
        putItem(result, "red_wool", 14);
        putItem(result, "black_wool", 14);
        putItem(result, "bookshelf", 72);
        putItem(result, "chiseled_bookshelf", 24);
        putItem(result, "ladder", 10);
        putItem(result, "iron_bars", 12);
        putItem(result, "iron_door", 54);
        putItem(result, "iron_trapdoor", 108);
        putItem(result, "chain", 31);
        putItem(result, "lantern", 18);
        /*
        * Redstone and functional blocks
        */
        putItem(result, "redstone_torch", 8);
        putItem(result, "repeater", 15);
        putItem(result, "comparator", 20);
        putItem(result, "piston", 38);
        putItem(result, "sticky_piston", 16);
        putItem(result, "observer", 19);
        putItem(result, "dispenser", 20);
        putItem(result, "dropper", 12);
        putItem(result, "hopper", 143);
        putItem(result, "target", 34);
        putItem(result, "daylight_detector", 24);
        putItem(result, "tripwire_hook", 16);
        putItem(result, "lever", 5);
        putItem(result, "redstone_lamp", 28);
        putItem(result, "rail", 11);
        putItem(result, "powered_rail", 38);
        putItem(result, "detector_rail", 28);
        putItem(result, "activator_rail", 29);
        putItem(result, "note_block", 12);
        putItem(result, "jukebox", 108);
        putItem(result, "crafting_table", 4);
        putItem(result, "furnace", 8);
        putItem(result, "blast_furnace", 146);
        putItem(result, "smoker", 45);
        putItem(result, "cartography_table", 12);
        putItem(result, "loom", 12);
        putItem(result, "stonecutter", 30);
        putItem(result, "smithing_table", 58);
        putItem(result, "grindstone", 12);
        putItem(result, "lectern", 25);
        putItem(result, "fletching_table", 12);
        putItem(result, "brewing_stand", 30);
        putItem(result, "cauldron", 189);
        putItem(result, "enchanting_table", 254);
        putItem(result, "anvil", 648);
        putItem(result, "chipped_anvil", 100);
        putItem(result, "damaged_anvil", 80);
        putItem(result, "beacon", 1534);
        putItem(result, "conduit", 350);
        putItem(result, "lightning_rod", 39);
        putItem(result, "spyglass", 31);
        putItem(result, "clock", 148);
        putItem(result, "compass", 112);
        putItem(result, "recovery_compass", 520);
        putItem(result, "lodestone", 560);
        putItem(result, "bell", 25);
        putItem(result, "composter", 8);
        putItem(result, "barrel", 8);
        putItem(result, "chest", 8);
        putItem(result, "trapped_chest", 13);
        putItem(result, "ender_chest", 94);
        putItem(result, "shulker_box", 188);
        putItem(result, "bucket", 81);
        putItem(result, "sculk", 3);
        putItem(result, "sculk_vein", 2);
        putItem(result, "sculk_sensor", 25);
        putItem(result, "calibrated_sculk_sensor", 40);
        putItem(result, "sculk_catalyst", 30);
        putItem(result, "sculk_shrieker", 50);
        putItem(result, "tnt", 24);
        putItem(result, "bee_nest", 20);
        putItem(result, "beehive", 30);
        putItem(result, "spawner", 100);
        putItem(result, "end_crystal", 200);
        putItem(result, "end_portal_frame", 100);
        /*
        * Tools and equipment
        */
        putItem(result, "wooden_pickaxe", 11);
        putItem(result, "wooden_axe", 11);
        putItem(result, "wooden_shovel", 9);
        putItem(result, "wooden_hoe", 10);
        putItem(result, "wooden_sword", 6);
        putItem(result, "stone_pickaxe", 11);
        putItem(result, "stone_axe", 11);
        putItem(result, "stone_shovel", 9);
        putItem(result, "stone_hoe", 10);
        putItem(result, "stone_sword", 6);
        putItem(result, "iron_pickaxe", 93);
        putItem(result, "iron_axe", 93);
        putItem(result, "iron_shovel", 35);
        putItem(result, "iron_hoe", 63);
        putItem(result, "iron_sword", 62);
        putItem(result, "iron_helmet", 150);
        putItem(result, "iron_chestplate", 240);
        putItem(result, "iron_leggings", 210);
        putItem(result, "iron_boots", 120);
        putItem(result, "golden_pickaxe", 138);
        putItem(result, "golden_axe", 138);
        putItem(result, "golden_shovel", 48);
        putItem(result, "golden_hoe", 93);
        putItem(result, "golden_sword", 92);
        putItem(result, "golden_helmet", 225);
        putItem(result, "golden_chestplate", 360);
        putItem(result, "golden_leggings", 315);
        putItem(result, "golden_boots", 180);
        putItem(result, "diamond_pickaxe", 453);
        putItem(result, "diamond_axe", 453);
        putItem(result, "diamond_shovel", 153);
        putItem(result, "diamond_hoe", 303);
        putItem(result, "diamond_sword", 302);
        putItem(result, "diamond_helmet", 750);
        putItem(result, "diamond_chestplate", 1200);
        putItem(result, "diamond_leggings", 1050);
        putItem(result, "diamond_boots", 600);
        putItem(result, "netherite_pickaxe", 1803);
        putItem(result, "netherite_axe", 1803);
        putItem(result, "netherite_shovel", 603);
        putItem(result, "netherite_hoe", 1203);
        putItem(result, "netherite_sword", 1202);
        putItem(result, "netherite_helmet", 3000);
        putItem(result, "netherite_chestplate", 4800);
        putItem(result, "netherite_leggings", 4200);
        putItem(result, "netherite_boots", 2400);
        putItem(result, "leather_helmet", 80);
        putItem(result, "leather_chestplate", 128);
        putItem(result, "leather_leggings", 112);
        putItem(result, "leather_boots", 64);
        putItem(result, "chainmail_helmet", 120);
        putItem(result, "chainmail_chestplate", 192);
        putItem(result, "chainmail_leggings", 168);
        putItem(result, "chainmail_boots", 96);
        putItem(result, "bow", 15);
        putItem(result, "crossbow", 46);
        putItem(result, "fishing_rod", 14);
        putItem(result, "shield", 39);
        putItem(result, "shears", 60);
        putItem(result, "flint_and_steel", 35);
        putItem(result, "carrot_on_a_stick", 17);
        putItem(result, "warped_fungus_on_a_stick", 17);
        putItem(result, "turtle_helmet", 113);
        putItem(result, "trident", 400);
        putItem(result, "totem_of_undying", 500);
        putItem(result, "saddle", 50);
        putItem(result, "name_tag", 20);
        putItem(result, "leather_horse_armor", 112);
        putItem(result, "iron_horse_armor", 200);
        putItem(result, "golden_horse_armor", 300);
        putItem(result, "diamond_horse_armor", 700);
        putItem(result, "netherite_upgrade_smithing_template", 800);
        putItem(result, "coast_armor_trim_smithing_template", 501);
        putItem(result, "dune_armor_trim_smithing_template", 501);
        putItem(result, "eye_armor_trim_smithing_template", 502);
        putItem(result, "host_armor_trim_smithing_template", 505);
        putItem(result, "raiser_armor_trim_smithing_template", 505);
        putItem(result, "rib_armor_trim_smithing_template", 505);
        putItem(result, "sentry_armor_trim_smithing_template", 501);
        putItem(result, "shaper_armor_trim_smithing_template", 505);
        putItem(result, "silence_armor_trim_smithing_template", 501);
        putItem(result, "snout_armor_trim_smithing_template", 501);
        putItem(result, "spire_armor_trim_smithing_template", 510);
        putItem(result, "tide_armor_trim_smithing_template", 506);
        putItem(result, "vex_armor_trim_smithing_template", 501);
        putItem(result, "ward_armor_trim_smithing_template", 501);
        putItem(result, "wayfinder_armor_trim_smithing_template", 505);
        putItem(result, "wild_armor_trim_smithing_template", 501);
        /*
        * Miscellaneous
        */
        putItem(result, "water_bucket", 10);
        putItem(result, "lava_bucket", 25);
        putItem(result, "powder_snow_bucket", 12);
        putItem(result, "cod_bucket", 15);
        putItem(result, "salmon_bucket", 16);
        putItem(result, "pufferfish_bucket", 18);
        putItem(result, "tropical_fish_bucket", 20);
        putItem(result, "axolotl_bucket", 25);
        putItem(result, "tadpole_bucket", 12);
        putItem(result, "firework_rocket", 5);
        putItem(result, "firework_star", 8);
        putItem(result, "map", 56);
        putItem(result, "filled_map", 15);
        putItem(result, "writable_book", 26);
        putItem(result, "written_book", 12);
        putItem(result, "knowledge_book", 30);
        putItem(result, "enchanted_book", 100);
        putItem(result, "bundle", 20);
        putItem(result, "potion", 20);
        putItem(result, "splash_potion", 25);
        putItem(result, "lingering_potion", 30);
        putItem(result, "dragon_breath", 60);
        putItem(result, "glass_bottle", 2);
        putItem(result, "bowl", 1);
        putItem(result, "snowball", 1);
        putItem(result, "snow_block", 4);
        putItem(result, "ice", 3);
        putItem(result, "packed_ice", 27);
        putItem(result, "blue_ice", 81);
        putItem(result, "scaffolding", 3);
        putItem(result, "soul_torch", 5);
        putItem(result, "soul_lantern", 18);

        /*
         * Existing anchors kept at their original value
         */
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

        /*
        * Passive and livestock
        */
        putEntity(result, "chicken", 5);
        putEntity(result, "rabbit", 5);
        putEntity(result, "killer_bunny", 5);
        putEntity(result, "bat", 6);
        putEntity(result, "parrot", 8);
        putEntity(result, "cow", 10);
        putEntity(result, "frog", 10);
        putEntity(result, "pig", 10);
        putEntity(result, "sheep", 10);
        putEntity(result, "cat", 12);
        putEntity(result, "fox", 12);
        putEntity(result, "ocelot", 12);
        putEntity(result, "bee", 15);
        putEntity(result, "donkey", 15);
        putEntity(result, "mooshroom", 15);
        putEntity(result, "snow_golem", 15);
        putEntity(result, "mule", 18);
        putEntity(result, "allay", 20);
        putEntity(result, "camel", 20);
        putEntity(result, "villager", 20);
        putEntity(result, "zombie_horse", 20);
        putEntity(result, "horse", 25);
        putEntity(result, "skeleton_horse", 25);
        putEntity(result, "sniffer", 25);
        putEntity(result, "wandering_trader", 25);
        /*
        * Aquatic
        */
        putEntity(result, "cod", 5);
        putEntity(result, "tadpole", 5);
        putEntity(result, "salmon", 6);
        putEntity(result, "tropical_fish", 8);
        putEntity(result, "squid", 10);
        putEntity(result, "pufferfish", 12);
        putEntity(result, "glow_squid", 15);
        putEntity(result, "turtle", 15);
        putEntity(result, "axolotl", 20);
        putEntity(result, "dolphin", 25);
        putEntity(result, "guardian", 120);
        putEntity(result, "elder_guardian", 600);
        /*
        * Neutral
        */
        putEntity(result, "wolf", 12);
        putEntity(result, "llama", 20);
        putEntity(result, "panda", 20);
        putEntity(result, "polar_bear", 20);
        putEntity(result, "goat", 25);
        putEntity(result, "trader_llama", 25);
        putEntity(result, "iron_golem", 300);
        /*
        * Hostile
        */
        putEntity(result, "slime", 20);
        putEntity(result, "endermite", 25);
        putEntity(result, "silverfish", 25);
        putEntity(result, "cave_spider", 30);
        putEntity(result, "skeleton", 30);
        putEntity(result, "spider", 30);
        putEntity(result, "zombie", 30);
        putEntity(result, "husk", 35);
        putEntity(result, "stray", 35);
        putEntity(result, "creeper", 40);
        putEntity(result, "drowned", 40);
        putEntity(result, "phantom", 45);
        putEntity(result, "zombie_villager", 45);
        putEntity(result, "pillager", 60);
        putEntity(result, "vex", 60);
        putEntity(result, "witch", 60);
        putEntity(result, "vindicator", 80);
        putEntity(result, "evoker", 150);
        putEntity(result, "illusioner", 150);
        putEntity(result, "giant", 200);
        putEntity(result, "ravager", 200);
        /*
        * Nether
        */
        putEntity(result, "strider", 30);
        putEntity(result, "magma_cube", 35);
        putEntity(result, "zombified_piglin", 60);
        putEntity(result, "piglin", 80);
        putEntity(result, "hoglin", 100);
        putEntity(result, "zoglin", 100);
        putEntity(result, "blaze", 120);
        putEntity(result, "ghast", 150);
        putEntity(result, "piglin_brute", 150);
        putEntity(result, "wither_skeleton", 200);
        /*
        * End and special
        */
        putEntity(result, "enderman", 100);
        putEntity(result, "shulker", 150);
        putEntity(result, "warden", 800);
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
