/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.draw;

/**
 * One entry of an offering list: a registry name plus an amount.
 */
public class Offering
{
    private final String itemId;
    private final int count;

    public Offering(String itemId, int count)
    {
        this.itemId = itemId == null ? "" : itemId;
        this.count = Math.max(0, count);
    }

    public String itemId()
    {
        return this.itemId;
    }

    public int count()
    {
        return this.count;
    }
}
