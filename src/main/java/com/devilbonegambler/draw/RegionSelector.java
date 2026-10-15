/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.draw;

import com.devilbonegambler.config.PrizeEntry;
import com.devilbonegambler.config.PrizeRegion;
import com.devilbonegambler.util.ModConstants;
import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * Pure selection rules of one draw: the region is picked by the dice point, the overflow boosts same colour entries,
 * and the entry itself is picked by cumulative weight. Kept free of server state so the rules stay easy to reason about.
 */
public final class RegionSelector
{
    private RegionSelector()
    {
    }

    /**
     * Region whose tier equals the point, or the highest tier when the point overshoots.
     */
    public static int baseTier(List<PrizeRegion> sortedRegions, int maxTier, int point)
    {
        if (sortedRegions.isEmpty())
        {
            return ModConstants.MIN_REGION_TIER;
        }

        for (PrizeRegion region : sortedRegions)
        {
            if (region.tierOrZero() == point)
            {
                return point;
            }
        }

        return maxTier;
    }

    /**
     * Points beyond the hit tier become the overflow that boosts same colour entries.
     */
    public static int overflow(int point, int hitTier)
    {
        return Math.max(0, point - hitTier);
    }

    public static int clampTier(int tier, int minTier, int maxTier)
    {
        return Math.max(minTier, Math.min(maxTier, tier));
    }

    /**
     * Nearest usable region: the exact tier first, then the closest higher tier, then the closest lower tier.
     */
    public static PrizeRegion select(List<PrizeRegion> regions, int targetTier)
    {
        PrizeRegion exact = null;
        PrizeRegion above = null;
        PrizeRegion below = null;

        for (PrizeRegion region : regions)
        {
            int tier = region.tierOrZero();

            if (!isUsable(region))
            {
                continue;
            }

            if (tier == targetTier)
            {
                exact = region;
            }
            else if (tier > targetTier && (above == null || tier < above.tierOrZero()))
            {
                above = region;
            }
            else if (tier < targetTier && (below == null || tier > below.tierOrZero()))
            {
                below = region;
            }
        }

        if (exact != null)
        {
            return exact;
        }

        return above != null ? above : below;
    }

    public static boolean isUsable(PrizeRegion region)
    {
        return region != null && totalWeight(region, true, 0) > 0.0D;
    }

    /**
     * Weight of one entry after the overflow boost. Only same colour entries are boosted, opposite ones stay untouched.
     */
    public static double effectiveWeight(PrizeEntry entry, boolean redPoint, int overflow)
    {
        double weight = entry.weightOrZero();

        if (weight <= 0.0D || overflow <= 0)
        {
            return weight;
        }

        if (redPoint != entry.isRed())
        {
            return weight;
        }

        return weight * (1.0D + overflow * ModConstants.OVERFLOW_WEIGHT_BONUS);
    }

    public static double totalWeight(PrizeRegion region, boolean redPoint, int overflow)
    {
        double result = 0.0D;

        for (PrizeEntry entry : region.entriesOrEmpty())
        {
            if (entry.isEnabled())
            {
                result += effectiveWeight(entry, redPoint, overflow);
            }
        }

        return result;
    }

    /**
     * Cumulative weight pick: one uniform roll over the open interval of the weight sum.
     */
    public static PrizeEntry pick(RandomSource random, PrizeRegion region, boolean redPoint, int overflow)
    {
        double weightSum = totalWeight(region, redPoint, overflow);

        if (weightSum <= 0.0D)
        {
            return null;
        }

        double roll = random.nextDouble() * weightSum;
        double accumulated = 0.0D;

        for (PrizeEntry entry : region.entriesOrEmpty())
        {
            if (!entry.isEnabled())
            {
                continue;
            }

            double weight = effectiveWeight(entry, redPoint, overflow);

            if (weight <= 0.0D)
            {
                continue;
            }

            accumulated += weight;

            if (accumulated > roll)
            {
                return entry;
            }
        }

        return null;
    }
}
