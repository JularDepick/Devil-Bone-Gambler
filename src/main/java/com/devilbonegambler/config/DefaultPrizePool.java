/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.util.ModConstants;

/**
 * The prize pool written when the config file does not exist yet.
 * Three regions keyed by dice point: 1 for free draws, 2 and 3 for higher points.
 */
public final class DefaultPrizePool
{
    private DefaultPrizePool()
    {
    }

    public static PrizePoolConfig create()
    {
        PrizePoolConfig config = PrizePoolConfig.empty();

        PrizeRegion first = new PrizeRegion(1);
        add(first, "bread_ration", 120.0D, true, false, "give ${player} minecraft:bread 4",
                ModConstants.NOTIFY_MODE_ACTIONBAR, translatable("bread_ration"));
        add(first, "torch_bundle", 110.0D, true, false, "give ${player} minecraft:torch 16",
                ModConstants.NOTIFY_MODE_ACTIONBAR, translatable("torch_bundle"));
        add(first, "stone_haul", 100.0D, true, false, "give ${player} minecraft:cobblestone 32",
                ModConstants.NOTIFY_MODE_ACTIONBAR, translatable("stone_haul"));
        add(first, "xp_offering", 70.0D, true, false, "experience add ${player} 30 points",
                ModConstants.NOTIFY_MODE_ACTIONBAR, translatable("xp_offering"));
        add(first, "iron_cache", 60.0D, true, false, "give ${player} minecraft:iron_ingot 4",
                ModConstants.NOTIFY_MODE_ACTIONBAR, translatable("iron_cache"));
        add(first, "hunger_curse", 60.0D, false, false, "effect give ${player} minecraft:hunger 60 1",
                ModConstants.NOTIFY_MODE_TITLE, translatable("hunger_curse"));
        add(first, "lightning_curse", 25.0D, false, false, "summon minecraft:lightning_bolt",
                ModConstants.NOTIFY_MODE_TITLE, translatable("lightning_curse"));
        add(first, "random_item_small", 150.0D, true, false, "give ${player} ${random:item} 1",
                ModConstants.NOTIFY_MODE_ACTIONBAR, translatable("random_item_small"));
        add(first, "random_effect_short", 90.0D, true, false,
                "effect give ${player} ${random:effect} 30 0",
                ModConstants.NOTIFY_MODE_ACTIONBAR, translatable("random_effect_short"));
        config.regions.add(first);

        PrizeRegion second = new PrizeRegion(2);
        add(second, "gold_cache", 60.0D, true, false, "give ${player} minecraft:gold_ingot 6",
                ModConstants.NOTIFY_MODE_ACTIONBAR, translatable("gold_cache"));
        add(second, "diamond_cache", 30.0D, true, false, "give ${player} minecraft:diamond 3",
                ModConstants.NOTIFY_MODE_TITLE, translatable("diamond_cache"));
        add(second, "emerald_cache", 25.0D, true, false, "give ${player} minecraft:emerald 8",
                ModConstants.NOTIFY_MODE_ACTIONBAR, translatable("emerald_cache"));
        add(second, "golden_apple_gift", 18.0D, true, false, "give ${player} minecraft:golden_apple 2",
                ModConstants.NOTIFY_MODE_ACTIONBAR, translatable("golden_apple_gift"));
        add(second, "blindness_curse", 12.0D, false, false, "effect give ${player} minecraft:blindness 15 0",
                ModConstants.NOTIFY_MODE_TITLE, translatable("blindness_curse"));
        add(second, "random_item_stack", 55.0D, true, false, "give ${player} ${random:item} 8",
                ModConstants.NOTIFY_MODE_ACTIONBAR, translatable("random_item_stack"));
        add(second, "random_potion", 30.0D, true, false,
                "give ${player} minecraft:potion{Potion:\"${random:potion}\"} 1",
                ModConstants.NOTIFY_MODE_TITLE, translatable("random_potion"));
        add(second, "random_mob_drop", 20.0D, false, false,
                "execute at ${player} run summon ${random:entity} ~ ~ ~",
                ModConstants.NOTIFY_MODE_TITLE, translatable("random_mob_drop"));
        config.regions.add(second);

        PrizeRegion third = new PrizeRegion(3);
        add(third, "netherite_cache", 40.0D, true, false, "give ${player} minecraft:netherite_ingot 1",
                ModConstants.NOTIFY_MODE_TITLE, translatable("netherite_cache"));
        add(third, "enchanted_apple_gift", 20.0D, true, false,
                "give ${player} minecraft:enchanted_golden_apple 1",
                ModConstants.NOTIFY_MODE_TITLE, translatable("enchanted_apple_gift"));
        add(third, "shulker_gift", 15.0D, true, false, "give ${player} minecraft:shulker_shell 4",
                ModConstants.NOTIFY_MODE_ACTIONBAR, translatable("shulker_gift"));
        add(third, "nausea_curse", 8.0D, false, false, "effect give ${player} minecraft:nausea 20 0",
                ModConstants.NOTIFY_MODE_TITLE, translatable("nausea_curse"));
        add(third, "elytra_gift", 4.0D, true, true, "give ${player} minecraft:elytra 1",
                ModConstants.NOTIFY_MODE_TITLE, translatable("elytra_gift"));
        add(third, "random_item_mass", 22.0D, true, false, "give ${player} ${random:item} 32",
                ModConstants.NOTIFY_MODE_TITLE, translatable("random_item_mass"));
        add(third, "random_enchant_book", 14.0D, true, true,
                "give ${player} minecraft:enchanted_book{StoredEnchantments:[{id:\"${random:enchantment}\",lvl:1s}]} 1",
                ModConstants.NOTIFY_MODE_TITLE, translatable("random_enchant_book"));
        config.regions.add(third);

        return config;
    }

    /**
     * Preset display texts are stored as translatable text components so that both language files apply.
     */
    private static String translatable(String key)
    {
        return "{\"translate\":\"gui." + ModConstants.MOD_ID + ".notify." + key + "\"}";
    }

    private static void add(PrizeRegion region, String id, double weight, boolean red, boolean majorEvent,
            String command, String mode, String text)
    {
        String color = red ? ModConstants.ENTRY_COLOR_RED : ModConstants.ENTRY_COLOR_BLACK;
        region.entriesOrEmpty().add(new PrizeEntry(id, true, weight, color, majorEvent, command, mode, text));
    }
}
