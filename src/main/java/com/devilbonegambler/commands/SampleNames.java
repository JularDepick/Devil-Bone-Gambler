/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.commands;

import com.devilbonegambler.util.ModConstants;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Turns one sampled registry id into the name a player reads, so a draw that used a sample placeholder can append what
 * it actually rolled to the result line. Types with no display name of their own fall back to the id.
 */
public final class SampleNames
{
    private static final Pattern BOOK_ENCHANTMENT = Pattern.compile("id:\"([^\"]+)\"");
    private static final Pattern BOOK_LEVEL = Pattern.compile("lvl:(\\d+)s");
    private static final String KEY_BOOK = "gui." + ModConstants.MOD_ID + ".sample.enchanted_book";

    private SampleNames()
    {
    }

    public static String displayFor(String type, String sampled)
    {
        if (sampled == null || sampled.isEmpty())
        {
            return "";
        }

        if (ModConstants.SAMPLE_DERIVED_ENCHANTED_BOOK.equals(type))
        {
            return enchantedBookName(sampled);
        }

        ResourceLocation key = ResourceLocation.tryParse(sampled);

        if (key == null)
        {
            return sampled;
        }

        if (ModConstants.SAMPLE_TYPE_ITEM.equals(type))
        {
            return itemName(key);
        }

        if (ModConstants.SAMPLE_TYPE_BLOCK.equals(type))
        {
            return blockName(key);
        }

        if (ModConstants.SAMPLE_TYPE_ENTITY.equals(type) || ModConstants.SAMPLE_TYPE_MOB.equals(type))
        {
            EntityType<?> entity = ForgeRegistries.ENTITY_TYPES.getValue(key);
            return entity == null ? sampled : entity.getDescription().getString();
        }

        if (ModConstants.SAMPLE_TYPE_EFFECT.equals(type) || ModConstants.SAMPLE_DERIVED_BUFF.equals(type)
                || ModConstants.SAMPLE_DERIVED_DEBUFF.equals(type) || ModConstants.SAMPLE_DERIVED_NEUBUFF.equals(type))
        {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(key);
            return effect == null ? sampled : effect.getDisplayName().getString();
        }

        if (ModConstants.SAMPLE_TYPE_ENCHANTMENT.equals(type)
                || ModConstants.SAMPLE_DERIVED_BUFF_ENCHANTMENT.equals(type)
                || ModConstants.SAMPLE_DERIVED_CURSE.equals(type))
        {
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(key);
            return enchantment == null ? sampled : enchantment.getFullname(1).getString();
        }

        if (ModConstants.SAMPLE_TYPE_POTION.equals(type))
        {
            Potion potion = ForgeRegistries.POTIONS.getValue(key);

            if (potion == null)
            {
                return sampled;
            }

            return Component.translatable(potion.getName("item.minecraft.potion.effect.")).getString();
        }

        if (ModConstants.SAMPLE_TYPE_ATTRIBUTE.equals(type))
        {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(key);

            if (attribute == null)
            {
                return sampled;
            }

            return Component.translatable(attribute.getDescriptionId()).getString();
        }

        return sampled;
    }

    private static String itemName(ResourceLocation key)
    {
        Item item = ForgeRegistries.ITEMS.getValue(key);

        if (item == null || item == Items.AIR)
        {
            return key.toString();
        }

        return new ItemStack(item).getHoverName().getString();
    }

    private static String blockName(ResourceLocation key)
    {
        Block block = ForgeRegistries.BLOCKS.getValue(key);

        if (block == null)
        {
            return key.toString();
        }

        Item item = block.asItem();

        if (item != null && item != Items.AIR)
        {
            return new ItemStack(item).getHoverName().getString();
        }

        return block.getName().getString();
    }

    private static String enchantedBookName(String sampled)
    {
        Matcher enchantment = BOOK_ENCHANTMENT.matcher(sampled);

        if (!enchantment.find())
        {
            return Component.translatable(KEY_BOOK).getString();
        }

        ResourceLocation key = ResourceLocation.tryParse(enchantment.group(1));
        Enchantment value = key == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(key);
        int level = 1;
        Matcher levelMatcher = BOOK_LEVEL.matcher(sampled);

        if (levelMatcher.find())
        {
            try
            {
                level = Integer.parseInt(levelMatcher.group(1));
            }
            catch (NumberFormatException ignored)
            {
                level = 1;
            }
        }

        String book = Component.translatable(KEY_BOOK).getString();
        return value == null ? book : book + " " + value.getFullname(level).getString();
    }
}
