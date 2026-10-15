/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.economy;

import com.devilbonegambler.util.ModConstants;

/**
 * The two currencies of the mod. Each one exists both as an item and as an account balance.
 */
public enum CurrencyType
{
    COIN("coin", "gamblers_coin"),
    TEAR("tear", "devils_tears");

    private final String id;
    private final String itemPath;

    CurrencyType(String id, String itemPath)
    {
        this.id = id;
        this.itemPath = itemPath;
    }

    public String id()
    {
        return this.id;
    }

    public String itemPath()
    {
        return this.itemPath;
    }

    public String itemId()
    {
        return ModConstants.MOD_ID + ":" + this.itemPath;
    }

    public static CurrencyType byId(String id)
    {
        for (CurrencyType candidate : values())
        {
            if (candidate.id.equals(id))
            {
                return candidate;
            }
        }

        return COIN;
    }

    public static int count()
    {
        return values().length;
    }
}
