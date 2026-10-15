/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.draw;

/**
 * One entry of an offering list: a registry name plus an amount, the damage of the offered stack and the
 * sum of its enchantment levels. The two stack fields are what makes the value depend on the instance.
 */
public class Offering
{
    private final String itemId;
    private final int count;
    private final int damage;
    private final int enchantLevels;

    public Offering(String itemId, int count)
    {
        this(itemId, count, 0, 0);
    }

    public Offering(String itemId, int count, int damage, int enchantLevels)
    {
        this.itemId = itemId == null ? "" : itemId;
        this.count = Math.max(0, count);
        this.damage = Math.max(0, damage);
        this.enchantLevels = Math.max(0, enchantLevels);
    }

    public String itemId()
    {
        return this.itemId;
    }

    public int count()
    {
        return this.count;
    }

    public int damage()
    {
        return this.damage;
    }

    public int enchantLevels()
    {
        return this.enchantLevels;
    }
}
