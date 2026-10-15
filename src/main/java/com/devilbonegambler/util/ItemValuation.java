/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Stack level valuation: the table value of the item plus the enchantment bonus, scaled by the remaining
 * durability. The same arithmetic runs on both sides, so the client estimate matches the server total.
 * Stacks without durability keep their full value.
 */
public final class ItemValuation
{
    private ItemValuation()
    {
    }

    /**
     * Value of one unit of the given stack.
     */
    public static int stackValue(ItemStack stack, int baseValue, int enchantValueBase)
    {
        if (stack == null || stack.isEmpty())
        {
            return 0;
        }

        return value(baseValue, stack.getDamageValue(), stack.getMaxDamage(), enchantLevelSum(stack),
                enchantValueBase);
    }

    /**
     * Value from raw numbers, which is what the server holds for one offering entry.
     */
    public static int value(int baseValue, int damage, int maxDamage, int enchantLevels, int enchantValueBase)
    {
        long total = Math.max(0, baseValue) + (long) Math.max(0, enchantLevels) * Math.max(0, enchantValueBase);

        if (maxDamage <= 0)
        {
            return (int) Math.min(Integer.MAX_VALUE, total);
        }

        int remaining = Math.max(0, maxDamage - Math.max(0, damage));
        long scaled = Math.round((double) total * (double) remaining / (double) maxDamage);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, scaled));
    }

    /**
     * Sum of the levels of every enchantment on the stack.
     */
    public static int enchantLevelSum(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return 0;
        }

        int total = 0;

        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stack).entrySet())
        {
            Integer level = entry.getValue();

            if (level != null)
            {
                total += Math.max(0, level.intValue());
            }
        }

        return total;
    }
}
