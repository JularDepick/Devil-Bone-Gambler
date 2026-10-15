/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import java.util.ArrayList;
import java.util.List;

/**
 * A prize pool region.
 * The tier number is the seniority of the region and also the dice point that lands on it: 1 is the poorest, 6 the best.
 */
public class PrizeRegion
{
    public Integer tier;
    public List<PrizeEntry> entries;

    public PrizeRegion()
    {
    }

    public PrizeRegion(int tier)
    {
        this.tier = Integer.valueOf(tier);
        this.entries = new ArrayList<PrizeEntry>();
    }

    public int tierOrZero()
    {
        return this.tier == null ? 0 : this.tier.intValue();
    }

    public List<PrizeEntry> entriesOrEmpty()
    {
        return this.entries == null ? new ArrayList<PrizeEntry>() : this.entries;
    }

    public PrizeRegion copy()
    {
        PrizeRegion result = new PrizeRegion();
        result.tier = this.tier;
        result.entries = new ArrayList<PrizeEntry>();

        for (PrizeEntry entry : entriesOrEmpty())
        {
            result.entries.add(entry.copy());
        }

        return result;
    }
}
