/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.commands;

import com.devilbonegambler.config.SampleSpaceConfig;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.SampleContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The ^(TYPE)[namespace] placeholders of a prize command or a chat command.
 * The type names a sample space that is discovered from the live registries and stored in the sample space config;
 * the optional namespace narrows the space to one namespace. The sample is picked from the ids of the space with the
 * point, the overflow, the luck value and the colour of the draw, and the placeholder is replaced with that id.
 * Only ids are sampled, never live entities or blocks of the world.
 */
public final class SamplePlaceholders
{
    public static final Pattern TOKEN = Pattern.compile("\\^\\(([A-Za-z0-9_]+)\\)(?:\\[([a-z0-9_.-]+)\\])?");

    private static final int TIERS = 5;

    private SamplePlaceholders()
    {
    }

    /**
     * Replaces every sample space placeholder with one sampled id. Returns null when a type is unknown, the space is
     * empty or the namespace leaves no sample, so the caller can skip the line instead of running a broken command.
     */
    public static String expand(String line, SampleContext context)
    {
        if (line == null || line.indexOf("^(") < 0)
        {
            return line;
        }

        SampleSpaceConfig config = ServerConfigManager.sampleSpaces();

        if (config == null)
        {
            return null;
        }

        SampleContext effective = context == null ? SampleContext.DEFAULT : context;
        Matcher matcher = TOKEN.matcher(line);
        StringBuilder builder = new StringBuilder();
        int cursor = 0;
        int index = 0;

        while (matcher.find())
        {
            String type = matcher.group(1).toUpperCase(Locale.ROOT);
            String namespace = matcher.group(2);
            List<String> candidates = candidates(config, type, namespace, effective.red());

            if (candidates.isEmpty())
            {
                return null;
            }

            String sampled = sample(config, type, candidates, effective, index++);

            if (sampled == null)
            {
                return null;
            }

            builder.append(line, cursor, matcher.start());
            builder.append(sampled);
            cursor = matcher.end();
        }

        builder.append(line, cursor, line.length());
        return builder.toString();
    }

    /**
     * Returns a description of the first unsupported or empty sample placeholder, or null when every one is usable.
     * On the client, where the sample space file does not exist, only the syntax is checked.
     */
    public static String findProblem(String line)
    {
        if (line == null || line.indexOf("^(") < 0)
        {
            return null;
        }

        String rest = TOKEN.matcher(line).replaceAll("");

        if (rest.contains("^(") || rest.contains("^["))
        {
            return "malformed sample placeholder";
        }

        SampleSpaceConfig config = ServerConfigManager.sampleSpaces();
        Matcher matcher = TOKEN.matcher(line);

        while (matcher.find())
        {
            String type = matcher.group(1).toUpperCase(Locale.ROOT);
            String namespace = matcher.group(2);

            if (config == null)
            {
                continue;
            }

            if (!config.knows(type))
            {
                return "unknown sample space: " + type;
            }

            if (candidates(config, type, namespace, false).isEmpty())
            {
                return namespace == null ? "empty sample space: " + type
                        : "empty sample space: " + type + "[" + namespace + "]";
            }
        }

        return null;
    }

    /**
     * Replaces every token with a representative id so that the vanilla parser can read the line during validation.
     */
    public static String sampleForm(String line)
    {
        if (line == null || line.indexOf("^(") < 0)
        {
            return line;
        }

        SampleSpaceConfig config = ServerConfigManager.sampleSpaces();
        Matcher matcher = TOKEN.matcher(line);
        StringBuilder builder = new StringBuilder();
        int cursor = 0;

        while (matcher.find())
        {
            String type = matcher.group(1).toUpperCase(Locale.ROOT);
            String sample = representative(config, type);

            builder.append(line, cursor, matcher.start());
            builder.append(sample);
            cursor = matcher.end();
        }

        builder.append(line, cursor, line.length());
        return builder.toString();
    }

    /**
     * The candidate ids of one type after the optional namespace and the colour narrowing; the colour falls back to
     * the whole space when it has no sample of the wanted kind, exactly like the colour rule of the prize regions.
     */
    private static List<String> candidates(SampleSpaceConfig config, String type, String namespace, boolean red)
    {
        List<String> base = baseCandidates(config, type);

        if (base.isEmpty())
        {
            return base;
        }

        boolean benefit = ModConstants.SAMPLE_TYPE_EFFECT.equals(type)
                || ModConstants.SAMPLE_TYPE_ENCHANTMENT.equals(type);
        List<String> coloured = base;

        if (benefit)
        {
            List<String> wanted = new ArrayList<String>();

            for (String id : base)
            {
                String kind = ModConstants.SAMPLE_TYPE_EFFECT.equals(type)
                        ? config.effectKindsOrEmpty().get(id)
                        : config.enchantmentKindsOrEmpty().get(id);

                if (red ? SampleSpaceConfig.KIND_BUFF.equals(kind)
                        : SampleSpaceConfig.KIND_DEBUFF.equals(kind) || SampleSpaceConfig.KIND_CURSE.equals(kind))
                {
                    wanted.add(id);
                }
            }

            if (!wanted.isEmpty())
            {
                coloured = wanted;
            }
        }

        if (namespace == null || namespace.isEmpty())
        {
            return new ArrayList<String>(coloured);
        }

        List<String> narrowed = new ArrayList<String>();
        String prefix = namespace + ":";

        for (String id : coloured)
        {
            if (id.startsWith(prefix))
            {
                narrowed.add(id);
            }
        }

        return narrowed;
    }

    private static List<String> baseCandidates(SampleSpaceConfig config, String type)
    {
        if (ModConstants.SAMPLE_DERIVED_BUFF.equals(type))
        {
            return byKind(config.effectKindsOrEmpty(), SampleSpaceConfig.KIND_BUFF);
        }

        if (ModConstants.SAMPLE_DERIVED_DEBUFF.equals(type))
        {
            return byKind(config.effectKindsOrEmpty(), SampleSpaceConfig.KIND_DEBUFF);
        }

        if (ModConstants.SAMPLE_DERIVED_NEUBUFF.equals(type))
        {
            return byKind(config.effectKindsOrEmpty(), SampleSpaceConfig.KIND_NEUBUFF);
        }

        if (ModConstants.SAMPLE_DERIVED_BUFF_ENCHANTMENT.equals(type))
        {
            return byKind(config.enchantmentKindsOrEmpty(), SampleSpaceConfig.KIND_BUFF);
        }

        if (ModConstants.SAMPLE_DERIVED_CURSE.equals(type))
        {
            return byKind(config.enchantmentKindsOrEmpty(), SampleSpaceConfig.KIND_CURSE);
        }

        if (ModConstants.SAMPLE_DERIVED_ENCHANTED_BOOK.equals(type))
        {
            List<String> buffs = byKind(config.enchantmentKindsOrEmpty(), SampleSpaceConfig.KIND_BUFF);
            return buffs.isEmpty() ? new ArrayList<String>(config.enchantmentKindsOrEmpty().keySet()) : buffs;
        }

        return new ArrayList<String>(config.spaceOf(type));
    }

    private static List<String> byKind(java.util.Map<String, String> kinds, String kind)
    {
        List<String> result = new ArrayList<String>();

        for (java.util.Map.Entry<String, String> entry : kinds.entrySet())
        {
            if (kind.equals(entry.getValue()))
            {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    /**
     * Picks one candidate. The point and the overflow raise the wanted rarity tier, the luck value pulls it further
     * up, and the weight of a candidate falls off with the distance to that tier. The enchantment book type builds the
     * item with the sampled enchantment, every other type yields the plain id.
     */
    private static String sample(SampleSpaceConfig config, String type, List<String> candidates, SampleContext context,
            int index)
    {
        double aim = targetTier(context);
        String picked = null;
        double total = 0.0D;
        double[] weights = new double[candidates.size()];

        for (int slot = 0; slot < candidates.size(); slot++)
        {
            String id = candidates.get(slot);
            double weight = 1.0D / (1.0D + Math.abs(rarityOf(config, type, id) - aim));
            weights[slot] = weight;
            total += weight;
        }

        RandomSource random = context.seed() == 0L
                ? RandomSource.create()
                : RandomSource.create(context.seed() + index * 0x9E3779B97F4A7C15L);

        double pick = random.nextDouble() * total;

        for (int slot = 0; slot < candidates.size(); slot++)
        {
            pick -= weights[slot];

            if (pick <= 0.0D)
            {
                picked = candidates.get(slot);
                break;
            }
        }

        if (picked == null)
        {
            picked = candidates.get(candidates.size() - 1);
        }

        if (!ModConstants.SAMPLE_DERIVED_ENCHANTED_BOOK.equals(type))
        {
            return picked;
        }

        ResourceLocation key = ResourceLocation.tryParse(picked);
        Enchantment enchantment = key == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(key);

        if (enchantment == null)
        {
            return ModConstants.SAMPLE_FALLBACK_ENCHANTMENT;
        }

        int max = Math.max(1, enchantment.getMaxLevel());
        int level = Mth.clamp(1 + (int) Math.round(aim / (TIERS - 1.0D) * (max - 1)), 1, max);
        return "minecraft:enchanted_book{StoredEnchantments:[{id:\"" + picked + "\",lvl:" + level + "}]}";
    }

    private static double targetTier(SampleContext context)
    {
        if (!context.fromDraw())
        {
            return (TIERS - 1) / 2.0D;
        }

        double span = Math.max(1, ModConstants.MAX_DICE_POINT - ModConstants.MIN_DICE_POINT);
        double aim = (context.point() - ModConstants.MIN_DICE_POINT) / span;
        aim += context.overflow() / span;
        aim += (context.luck() - ModConstants.BASE_LUCK)
                / (double) Math.max(1, ModConstants.MAX_LUCK - ModConstants.BASE_LUCK) * 0.5D;
        return Mth.clamp(aim, 0.0D, 1.0D) * (TIERS - 1);
    }

    private static double rarityOf(SampleSpaceConfig config, String type, String id)
    {
        if (ModConstants.SAMPLE_TYPE_ENCHANTMENT.equals(type)
                || ModConstants.SAMPLE_TYPE_EFFECT.equals(type)
                || ModConstants.SAMPLE_DERIVED_ENCHANTED_BOOK.equals(type)
                || ModConstants.SAMPLE_DERIVED_BUFF_ENCHANTMENT.equals(type)
                || ModConstants.SAMPLE_DERIVED_CURSE.equals(type)
                || ModConstants.SAMPLE_DERIVED_BUFF.equals(type)
                || ModConstants.SAMPLE_DERIVED_DEBUFF.equals(type)
                || ModConstants.SAMPLE_DERIVED_NEUBUFF.equals(type))
        {
            ResourceLocation key = ResourceLocation.tryParse(id);
            Enchantment enchantment = key == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(key);

            if (enchantment != null)
            {
                return Math.min(TIERS - 1, enchantment.getRarity().ordinal() + (enchantment.getMaxLevel() > 3 ? 1 : 0));
            }
        }

        if (ModConstants.SAMPLE_TYPE_ITEM.equals(type) || ModConstants.SAMPLE_TYPE_BLOCK.equals(type))
        {
            if (ServerConfigManager.itemValues() != null)
            {
                int value = ServerConfigManager.itemValues().itemValueOf(id);
                return Math.min(TIERS - 1, valueTier(value));
            }
        }

        return Math.floorMod(id.hashCode(), TIERS);
    }

    private static int valueTier(int value)
    {
        if (value <= 0)
        {
            return 0;
        }

        if (value < 10)
        {
            return 0;
        }

        if (value < 50)
        {
            return 1;
        }

        if (value < 200)
        {
            return 2;
        }

        return value < 1000 ? 3 : 4;
    }

    private static String representative(SampleSpaceConfig config, String type)
    {
        if (config != null)
        {
            List<String> candidates = baseCandidates(config, type);

            if (!candidates.isEmpty())
            {
                return ModConstants.SAMPLE_DERIVED_ENCHANTED_BOOK.equals(type)
                        ? ModConstants.SAMPLE_BOOK_SAMPLE
                        : candidates.get(0);
            }
        }

        if (ModConstants.SAMPLE_TYPE_EFFECT.equals(type) || ModConstants.SAMPLE_DERIVED_BUFF.equals(type)
                || ModConstants.SAMPLE_DERIVED_DEBUFF.equals(type)
                || ModConstants.SAMPLE_DERIVED_NEUBUFF.equals(type))
        {
            return ModConstants.SAMPLE_FALLBACK_EFFECT;
        }

        if (ModConstants.SAMPLE_TYPE_ENCHANTMENT.equals(type)
                || ModConstants.SAMPLE_DERIVED_BUFF_ENCHANTMENT.equals(type)
                || ModConstants.SAMPLE_DERIVED_CURSE.equals(type))
        {
            return ModConstants.SAMPLE_FALLBACK_ENCHANTMENT;
        }

        if (ModConstants.SAMPLE_DERIVED_ENCHANTED_BOOK.equals(type))
        {
            return ModConstants.SAMPLE_BOOK_SAMPLE;
        }

        return ModConstants.SAMPLE_FALLBACK_ID;
    }
}
