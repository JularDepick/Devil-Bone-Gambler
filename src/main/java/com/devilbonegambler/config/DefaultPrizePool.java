/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.util.ModConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The prize pool written when the config file does not exist yet.
 * Three regions keyed by dice point: 1 for free draws, 2 and 3 for higher points.
 * A preset entry keeps its name as a text component, so the name an administrator reads follows the language of the
 * game instead of the language the file was created in.
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
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("bread_ration"));
        add(first, "torch_bundle", 110.0D, true, false, "give ${player} minecraft:torch 16",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("torch_bundle"));
        add(first, "stone_haul", 100.0D, true, false, "give ${player} minecraft:cobblestone 32",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("stone_haul"));
        add(first, "xp_offering", 70.0D, true, false, "experience add ${player} 30 points",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("xp_offering"));
        add(first, "iron_cache", 60.0D, true, false, "give ${player} minecraft:iron_ingot 4",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("iron_cache"));
        add(first, "hunger_curse", 60.0D, false, false, "effect give ${player} minecraft:hunger 60 1",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("hunger_curse"));
        add(first, "lightning_curse", 25.0D, false, false, "summon minecraft:lightning_bolt",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("lightning_curse"));
        add(first, "random_item_small", 150.0D, true, false, "give ${player} ${random:item} 1",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("random_item_small"));
        add(first, "random_effect_short", 90.0D, true, false,
                "effect give ${player} ${random:effect} 30 0",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("random_effect_short"));
        config.regions.add(first);

        PrizeRegion second = new PrizeRegion(2);
        add(second, "gold_cache", 60.0D, true, false, "give ${player} minecraft:gold_ingot 6",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("gold_cache"));
        add(second, "diamond_cache", 30.0D, true, false, "give ${player} minecraft:diamond 3",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("diamond_cache"));
        add(second, "emerald_cache", 25.0D, true, false, "give ${player} minecraft:emerald 8",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("emerald_cache"));
        add(second, "golden_apple_gift", 18.0D, true, false, "give ${player} minecraft:golden_apple 2",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("golden_apple_gift"));
        add(second, "blindness_curse", 12.0D, false, false, "effect give ${player} minecraft:blindness 15 0",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("blindness_curse"));
        add(second, "random_item_stack", 55.0D, true, false, "give ${player} ${random:item} 8",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("random_item_stack"));
        add(second, "random_potion", 30.0D, true, false,
                "give ${player} minecraft:potion{Potion:\"${random:potion}\"} 1",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("random_potion"));
        add(second, "random_mob_drop", 20.0D, false, false,
                "execute at ${player} run summon ${random:entity} ~ ~ ~",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("random_mob_drop"));
        config.regions.add(second);

        PrizeRegion third = new PrizeRegion(3);
        add(third, "netherite_cache", 40.0D, true, false, "give ${player} minecraft:netherite_ingot 1",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("netherite_cache"));
        add(third, "enchanted_apple_gift", 20.0D, true, false,
                "give ${player} minecraft:enchanted_golden_apple 1",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("enchanted_apple_gift"));
        add(third, "shulker_gift", 15.0D, true, false, "give ${player} minecraft:shulker_shell 4",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("shulker_gift"));
        add(third, "nausea_curse", 8.0D, false, false, "effect give ${player} minecraft:nausea 20 0",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("nausea_curse"));
        add(third, "elytra_gift", 4.0D, true, true, "give ${player} minecraft:elytra 1",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("elytra_gift"));
        add(third, "random_item_mass", 22.0D, true, false, "give ${player} ${random:item} 32",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("random_item_mass"));
        add(third, "random_enchant_book", 14.0D, true, true,
                "give ${player} minecraft:enchanted_book{StoredEnchantments:[{id:\"${random:enchantment}\",lvl:1s}]} 1",
                ModConstants.NOTIFY_MODE_TELLRAW, translatable("random_enchant_book"));
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

    /**
     * Language key of the name of a preset entry. A configuration written before names became translatable stores the
     * plain slug, which is resolved through the same key.
     */
    public static String nameKey(String slug)
    {
        return "entry." + ModConstants.MOD_ID + "." + slug;
    }

    /**
     * Stored form of a preset entry name: a text component, so the name follows the reader language.
     */
    public static String nameJson(String slug)
    {
        return "{\"translate\":\"" + nameKey(slug) + "\"}";
    }

    /**
     * Name of an entry as it is shown: a stored text component is resolved, a stored slug is resolved through the
     * preset key when that key exists in the language of the reader, and anything else stays the literal the
     * administrator typed.
     */
    public static String displayName(String stored)
    {
        if (stored == null || stored.isEmpty())
        {
            return "";
        }

        if (stored.startsWith("{"))
        {
            try
            {
                MutableComponent parsed = Component.Serializer.fromJson(stored);
                return parsed == null ? stored : resolve(parsed.getString(), stored);
            }
            catch (RuntimeException exception)
            {
                return stored;
            }
        }

        String key = nameKey(stored);
        String translated = Component.translatable(key).getString();
        return key.equals(translated) ? stored : translated;
    }

    /**
     * A stored text component resolves through the language of the reader; when that language has no entry for the key
     * yet, the slug behind the key is still readable and is shown instead of the raw key.
     */
    private static String resolve(String translated, String stored)
    {
        String prefix = "entry." + ModConstants.MOD_ID + ".";

        if (translated.startsWith(prefix))
        {
            return translated.substring(prefix.length());
        }

        return translated.isEmpty() ? stored : translated;
    }

    private static void add(PrizeRegion region, String slug, double weight, boolean red, boolean majorEvent,
            String command, String mode, String text)
    {
        String color = red ? ModConstants.ENTRY_COLOR_RED : ModConstants.ENTRY_COLOR_BLACK;
        region.entriesOrEmpty().add(new PrizeEntry(nameJson(slug), true, weight, color, majorEvent, command, mode,
                text));
    }
}
