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

    /**
     * Whether a region holds at least one selectable entry with a positive weight.
     * The colour is ignored on purpose here, because a region that an admin edited down to a single colour can still
     * be drawn from, and usability must not depend on the colour that happens to be announced.
     */
    public static boolean isUsable(PrizeRegion region)
    {
        if (region == null)
        {
            return false;
        }

        for (PrizeEntry entry : region.entriesOrEmpty())
        {
            if (entry.isSelectable() && entry.weightOrZero() > 0.0D)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Whether the announced colour owns a usable pool in this region.
     * The colour owns the pool so that a black point never hands out a red entry and the other way round, which keeps
     * the promise of the announced colour. A region without any entry of that colour falls back to every selectable
     * entry, so a degenerate admin configuration still draws instead of failing.
     */
    private static boolean colorOwnsPool(PrizeRegion region, boolean redPoint)
    {
        for (PrizeEntry entry : region.entriesOrEmpty())
        {
            if (entry.isSelectable() && entry.weightOrZero() > 0.0D && entry.isRed() == redPoint)
            {
                return true;
            }
        }

        return false;
    }

    private static boolean isCandidate(PrizeEntry entry, boolean redPoint, boolean colorOwned)
    {
        return entry.isSelectable() && (!colorOwned || entry.isRed() == redPoint);
    }

    /**
     * Heaviest candidate weight of the pool, used as the reference point of the rarity boost.
     * Zero when the pool holds no candidate.
     */
    private static double poolMaxWeight(PrizeRegion region, boolean redPoint, boolean colorOwned)
    {
        double max = 0.0D;

        for (PrizeEntry entry : region.entriesOrEmpty())
        {
            if (isCandidate(entry, redPoint, colorOwned))
            {
                max = Math.max(max, entry.weightOrZero());
            }
        }

        return max;
    }

    /**
     * Weight of one entry after the overflow boost.
     * The boost grows with how uncommon an entry is inside the pool, so a larger overflow favours the entries that
     * usually stay rare. Scaling the whole pool by one factor would change nothing, because the pool is single
     * colour and a uniform factor cancels out of the odds.
     */
    public static double effectiveWeight(PrizeEntry entry, double poolMaxWeight, int overflow)
    {
        double weight = entry.weightOrZero();

        if (weight <= 0.0D || overflow <= 0 || poolMaxWeight <= 0.0D)
        {
            return weight;
        }

        double rarity = 1.0D - Math.min(1.0D, weight / poolMaxWeight);
        return weight * (1.0D + overflow * ModConstants.OVERFLOW_WEIGHT_BONUS * rarity);
    }

    public static double totalWeight(PrizeRegion region, boolean redPoint, int overflow)
    {
        return totalWeight(region, redPoint, overflow, colorOwnsPool(region, redPoint));
    }

    private static double totalWeight(PrizeRegion region, boolean redPoint, int overflow, boolean colorOwned)
    {
        double max = poolMaxWeight(region, redPoint, colorOwned);
        double result = 0.0D;

        for (PrizeEntry entry : region.entriesOrEmpty())
        {
            if (isCandidate(entry, redPoint, colorOwned))
            {
                result += effectiveWeight(entry, max, overflow);
            }
        }

        return result;
    }

    /**
     * Cumulative weight pick: one uniform roll over the open interval of the weight sum.
     */
    public static PrizeEntry pick(RandomSource random, PrizeRegion region, boolean redPoint, int overflow)
    {
        boolean colorOwned = colorOwnsPool(region, redPoint);
        double max = poolMaxWeight(region, redPoint, colorOwned);
        double weightSum = totalWeight(region, redPoint, overflow, colorOwned);

        if (weightSum <= 0.0D)
        {
            return null;
        }

        double roll = random.nextDouble() * weightSum;
        double accumulated = 0.0D;

        for (PrizeEntry entry : region.entriesOrEmpty())
        {
            if (!isCandidate(entry, redPoint, colorOwned))
            {
                continue;
            }

            double weight = effectiveWeight(entry, max, overflow);

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
