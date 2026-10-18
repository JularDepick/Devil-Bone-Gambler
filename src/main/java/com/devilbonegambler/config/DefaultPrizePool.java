/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.util.ModConstants;
import net.minecraft.network.chat.Component;

/**
 * The prize pool written when the config file does not exist yet.
 * Three regions keyed by dice point: 1 for free draws, 2 and 3 for higher points.
 *
 * Every preset name and display text is resolved once, in the language the server runs with at that moment, and then
 * stored as a plain string. The file is the only place these texts live afterwards, so a later language change never
 * rewrites the wording a player already saw.
 */
public final class DefaultPrizePool
{
    /*
     * Preset display texts in the two shipped languages. They are not language keys on purpose: the text is written
     * into the server file when the defaults are created and read from there afterwards.
     */
    private static final String[][] PRESET_TEXTS =
    {
        {"bread_ration", "四块面包", "Four loaves of bread"},
        {"torch_bundle", "一叠火把", "A bundle of torches"},
        {"stone_haul", "三十二块圆石", "Thirty-two cobblestone"},
        {"xp_offering", "三十点经验", "Thirty experience points"},
        {"hunger_curse", "饥饿缠身", "Hunger gnaws at you"},
        {"iron_cache", "四块铁锭", "Four iron ingots"},
        {"lightning_curse", "天雷落下", "Lightning falls"},
        {"random_item_small", "随机物品一件", "One random item"},
        {"random_effect_short", "随机效果加持", "A random effect"},
        {"gold_cache", "六块金锭", "Six gold ingots"},
        {"diamond_cache", "三颗钻石", "Three diamonds"},
        {"emerald_cache", "八颗绿宝石", "Eight emeralds"},
        {"golden_apple_gift", "两颗金苹果", "Two golden apples"},
        {"blindness_curse", "双目失明", "Blinded"},
        {"random_item_stack", "随机物品八件", "Eight random items"},
        {"random_potion", "随机药水一瓶", "One random potion"},
        {"random_mob_drop", "随机生物降临", "A random mob appears"},
        {"netherite_cache", "一块下界合金锭", "One netherite ingot"},
        {"enchanted_apple_gift", "一颗附魔金苹果", "One enchanted golden apple"},
        {"shulker_gift", "四片潜影壳", "Four shulker shells"},
        {"nausea_curse", "天旋地转", "The world spins"},
        {"elytra_gift", "一副鞘翅", "One elytra"},
        {"random_item_mass", "随机物品一大堆", "A pile of random items"},
        {"random_enchant_book", "随机附魔书一本", "One random enchanted book"},
    };

    private DefaultPrizePool()
    {
    }

    public static PrizePoolConfig create()
    {
        PrizePoolConfig config = PrizePoolConfig.empty();

        PrizeRegion first = new PrizeRegion(1);
        add(first, "bread_ration", 120.0D, true, false, "give ${player} minecraft:bread 4");
        add(first, "torch_bundle", 110.0D, true, false, "give ${player} minecraft:torch 16");
        add(first, "stone_haul", 100.0D, true, false, "give ${player} minecraft:cobblestone 32");
        add(first, "xp_offering", 70.0D, true, false, "experience add ${player} 30 points");
        add(first, "iron_cache", 60.0D, true, false, "give ${player} minecraft:iron_ingot 4");
        add(first, "hunger_curse", 60.0D, false, false, "effect give ${player} minecraft:hunger 60 1");
        add(first, "lightning_curse", 25.0D, false, false, "summon minecraft:lightning_bolt");
        add(first, "random_item_small", 150.0D, true, false, "give ${player} ${random:item} 1");
        add(first, "random_effect_short", 90.0D, true, false, "effect give ${player} ${random:effect} 30 0");
        config.regions.add(first);

        PrizeRegion second = new PrizeRegion(2);
        add(second, "gold_cache", 60.0D, true, false, "give ${player} minecraft:gold_ingot 6");
        add(second, "diamond_cache", 30.0D, true, false, "give ${player} minecraft:diamond 3");
        add(second, "emerald_cache", 25.0D, true, false, "give ${player} minecraft:emerald 8");
        add(second, "golden_apple_gift", 18.0D, true, false, "give ${player} minecraft:golden_apple 2");
        add(second, "blindness_curse", 12.0D, false, false, "effect give ${player} minecraft:blindness 15 0");
        add(second, "random_item_stack", 55.0D, true, false, "give ${player} ${random:item} 8");
        add(second, "random_potion", 30.0D, true, false, "give ${player} minecraft:potion{Potion:\"${random:potion}\"} 1");
        add(second, "random_mob_drop", 20.0D, false, false, "execute at ${player} run summon ${random:entity} ~ ~ ~");
        config.regions.add(second);

        PrizeRegion third = new PrizeRegion(3);
        add(third, "netherite_cache", 40.0D, true, false, "give ${player} minecraft:netherite_ingot 1");
        add(third, "enchanted_apple_gift", 20.0D, true, false, "give ${player} minecraft:enchanted_golden_apple 1");
        add(third, "shulker_gift", 15.0D, true, false, "give ${player} minecraft:shulker_shell 4");
        add(third, "nausea_curse", 8.0D, false, false, "effect give ${player} minecraft:nausea 20 0");
        add(third, "elytra_gift", 4.0D, true, true, "give ${player} minecraft:elytra 1");
        add(third, "random_item_mass", 22.0D, true, false, "give ${player} ${random:item} 32");
        add(third, "random_enchant_book", 14.0D, true, true, "give ${player} minecraft:enchanted_book{StoredEnchantments:[{id:\"${random:enchantment}\",lvl:1s}]} 1");
        config.regions.add(third);

        return config;
    }

    /**
     * Display text of a preset entry in the language this server runs with, or the slug when the table has no row.
     */
    private static String presetText(String slug)
    {
        for (String[] row : PRESET_TEXTS)
        {
            if (row[0].equals(slug))
            {
                return isChinese() ? row[1] : row[2];
            }
        }

        return slug;
    }

    /**
     * The client language reaches the integrated server through the shared language instance, so a probe key decides
     * which column of the preset table applies. A dedicated server keeps the default language and therefore writes the
     * English column.
     */
    private static boolean isChinese()
    {
        return Component.translatable("gui." + ModConstants.MOD_ID + ".language").getString().startsWith("zh");
    }

    /**
     * Name of a preset entry in the current language, stored as a plain string like the display text.
     */
    private static String presetName(String slug)
    {
        String key = nameKey(slug);
        String translated = Component.translatable(key).getString();
        return key.equals(translated) ? slug : translated;
    }

    /**
     * Language key of the name of a preset entry, used only while the defaults are generated.
     */
    private static String nameKey(String slug)
    {
        return "entry." + ModConstants.MOD_ID + "." + slug;
    }

    private static void add(PrizeRegion region, String slug, double weight, boolean red, boolean majorEvent,
            String command)
    {
        String color = red ? ModConstants.ENTRY_COLOR_RED : ModConstants.ENTRY_COLOR_BLACK;
        region.entriesOrEmpty().add(new PrizeEntry(presetName(slug), true, weight, color, majorEvent, command,
                ModConstants.NOTIFY_MODE_TELLRAW, presetText(slug)));
    }
}
