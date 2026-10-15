/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;

/**
 * The economy rates: recycle pricing, exchange fee and VIP, and the asset counter retention rate.
 * Every rate is administrator editable, and every one of them is clamped to a sane range on load.
 */
public class EconomyConfig
{
    public static final int CURRENT_SCHEMA = 1;

    public Integer schemaVersion;
    public Integer revision;
    public Double recycleMultiplier;
    public Double recycleCoinShare;
    public Double recycleTearShare;
    public Double exchangeFeeRate;
    public Integer exchangeOrderTimeoutTicks;
    public Long vipPriceCoin;
    public Long vipPriceTear;
    public Integer vipDurationTicks;
    public Double assetRetentionRate;
    public List<String> recycleExcludedItems;

    public EconomyConfig()
    {
    }

    public static EconomyConfig empty()
    {
        EconomyConfig result = new EconomyConfig();
        result.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);
        result.revision = Integer.valueOf(0);
        result.recycleMultiplier = Double.valueOf(ModConstants.DEFAULT_RECYCLE_MULTIPLIER);
        result.recycleCoinShare = Double.valueOf(ModConstants.DEFAULT_RECYCLE_COIN_SHARE);
        result.recycleTearShare = Double.valueOf(ModConstants.DEFAULT_RECYCLE_TEAR_SHARE);
        result.exchangeFeeRate = Double.valueOf(ModConstants.EXCHANGE_FEE_RATE);
        result.exchangeOrderTimeoutTicks = Integer.valueOf(ModConstants.EXCHANGE_ORDER_TIMEOUT_TICKS);
        result.vipPriceCoin = Long.valueOf(ModConstants.DEFAULT_VIP_PRICE_COIN);
        result.vipPriceTear = Long.valueOf(ModConstants.DEFAULT_VIP_PRICE_TEAR);
        result.vipDurationTicks = Integer.valueOf(ModConstants.DEFAULT_VIP_DURATION_TICKS);
        result.assetRetentionRate = Double.valueOf(ModConstants.ASSET_RETENTION_RATE);
        result.recycleExcludedItems = new ArrayList<String>();
        return result;
    }

    public int schemaVersionOrZero()
    {
        return this.schemaVersion == null ? 0 : this.schemaVersion.intValue();
    }

    public int revisionOrZero()
    {
        return this.revision == null ? 0 : this.revision.intValue();
    }

    public double recycleMultiplierOr() { return or(this.recycleMultiplier, ModConstants.DEFAULT_RECYCLE_MULTIPLIER); }

    public double recycleCoinShareOr() { return or(this.recycleCoinShare, ModConstants.DEFAULT_RECYCLE_COIN_SHARE); }

    public double recycleTearShareOr() { return or(this.recycleTearShare, ModConstants.DEFAULT_RECYCLE_TEAR_SHARE); }

    public double exchangeFeeRateOr() { return or(this.exchangeFeeRate, ModConstants.EXCHANGE_FEE_RATE); }

    public int exchangeOrderTimeoutTicksOr()
    {
        return this.exchangeOrderTimeoutTicks == null
                ? ModConstants.EXCHANGE_ORDER_TIMEOUT_TICKS
                : Math.max(1, this.exchangeOrderTimeoutTicks.intValue());
    }

    public long vipPriceCoinOr()
    {
        return this.vipPriceCoin == null ? ModConstants.DEFAULT_VIP_PRICE_COIN : Math.max(0L, this.vipPriceCoin.longValue());
    }

    public long vipPriceTearOr()
    {
        return this.vipPriceTear == null ? ModConstants.DEFAULT_VIP_PRICE_TEAR : Math.max(0L, this.vipPriceTear.longValue());
    }

    public int vipDurationTicksOr()
    {
        return this.vipDurationTicks == null
                ? ModConstants.DEFAULT_VIP_DURATION_TICKS
                : Math.max(1, this.vipDurationTicks.intValue());
    }

    public double assetRetentionRateOr() { return or(this.assetRetentionRate, ModConstants.ASSET_RETENTION_RATE); }

    public List<String> recycleExcludedItemsOrEmpty()
    {
        return this.recycleExcludedItems == null ? new ArrayList<String>() : this.recycleExcludedItems;
    }

    /**
     * Recycle payout split into the two currencies.
     * The total is rounded down once, then the coin leg is rounded down and the tear leg takes the
     * remainder, so the two legs always add up to the total and no value is lost to rounding.
     */
    public long[] recyclePayout(long baseValue)
    {
        long total = (long) Math.floor(Math.max(0L, baseValue) * recycleMultiplierOr());
        long coin = (long) Math.floor(total * recycleCoinShareOr());
        long tear = total - coin;
        return new long[] {Math.max(0L, coin), Math.max(0L, tear)};
    }

    public EconomyConfig copy()
    {
        EconomyConfig result = new EconomyConfig();
        result.schemaVersion = this.schemaVersion;
        result.revision = this.revision;
        result.recycleMultiplier = this.recycleMultiplier;
        result.recycleCoinShare = this.recycleCoinShare;
        result.recycleTearShare = this.recycleTearShare;
        result.exchangeFeeRate = this.exchangeFeeRate;
        result.exchangeOrderTimeoutTicks = this.exchangeOrderTimeoutTicks;
        result.vipPriceCoin = this.vipPriceCoin;
        result.vipPriceTear = this.vipPriceTear;
        result.vipDurationTicks = this.vipDurationTicks;
        result.assetRetentionRate = this.assetRetentionRate;
        result.recycleExcludedItems = new ArrayList<String>(recycleExcludedItemsOrEmpty());
        return result;
    }

    public List<String> validate()
    {
        List<String> problems = new ArrayList<String>();
        checkRate(problems, "recycleMultiplier", this.recycleMultiplier, true);
        checkRate(problems, "recycleCoinShare", this.recycleCoinShare, false);
        checkRate(problems, "recycleTearShare", this.recycleTearShare, false);
        checkRate(problems, "exchangeFeeRate", this.exchangeFeeRate, false);
        checkRate(problems, "assetRetentionRate", this.assetRetentionRate, false);

        if (this.recycleCoinShare != null && this.recycleTearShare != null)
        {
            double sum = this.recycleCoinShare.doubleValue() + this.recycleTearShare.doubleValue();

            if (Math.abs(sum - 1.0D) > 0.001D)
            {
                problems.add("recycle share does not sum to one: " + sum);
            }
        }

        if (this.exchangeOrderTimeoutTicks != null && this.exchangeOrderTimeoutTicks.intValue() <= 0)
        {
            problems.add("invalid order timeout: " + this.exchangeOrderTimeoutTicks);
        }

        if (this.vipDurationTicks != null && this.vipDurationTicks.intValue() <= 0)
        {
            problems.add("invalid vip duration: " + this.vipDurationTicks);
        }

        for (String item : recycleExcludedItemsOrEmpty())
        {
            if (item == null || !item.contains(":"))
            {
                problems.add("invalid excluded item: " + item);
            }
        }

        return problems;
    }

    private static void checkRate(List<String> problems, String name, Double value, boolean allowAboveOne)
    {
        if (value == null)
        {
            return;
        }

        double raw = value.doubleValue();

        if (raw < 0.0D || (!allowAboveOne && raw > 1.0D))
        {
            problems.add("rate out of range: " + name + "=" + raw);
        }
    }

    public void normalize()
    {
        if (this.schemaVersion == null)
        {
            this.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);
        }

        if (this.revision == null)
        {
            this.revision = Integer.valueOf(0);
        }

        EconomyConfig defaults = empty();

        if (this.recycleMultiplier == null) { this.recycleMultiplier = defaults.recycleMultiplier; }

        if (this.recycleCoinShare == null) { this.recycleCoinShare = defaults.recycleCoinShare; }

        if (this.recycleTearShare == null) { this.recycleTearShare = defaults.recycleTearShare; }

        if (this.exchangeFeeRate == null) { this.exchangeFeeRate = defaults.exchangeFeeRate; }

        if (this.exchangeOrderTimeoutTicks == null) { this.exchangeOrderTimeoutTicks = defaults.exchangeOrderTimeoutTicks; }

        if (this.vipPriceCoin == null) { this.vipPriceCoin = defaults.vipPriceCoin; }

        if (this.vipPriceTear == null) { this.vipPriceTear = defaults.vipPriceTear; }

        if (this.vipDurationTicks == null) { this.vipDurationTicks = defaults.vipDurationTicks; }

        if (this.assetRetentionRate == null) { this.assetRetentionRate = defaults.assetRetentionRate; }

        if (this.recycleExcludedItems == null)
        {
            this.recycleExcludedItems = new ArrayList<String>();
        }
    }

    private static double or(Double value, double fallback)
    {
        return value == null ? fallback : value.doubleValue();
    }
}
