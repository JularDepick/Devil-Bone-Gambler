/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

/**
 * One shop offer. Both prices may be set, which makes the offer a mixed currency purchase.
 * The two currencies never substitute for each other, so each price is paid in its own currency.
 */
public class ShopOffer
{
    public String id;
    public Boolean enabled;
    public String item;
    public Integer count;
    public Long priceCoin;
    public Long priceTear;

    public ShopOffer()
    {
    }

    public ShopOffer(String id, boolean enabled, String item, int count, long priceCoin, long priceTear)
    {
        this.id = id;
        this.enabled = Boolean.valueOf(enabled);
        this.item = item;
        this.count = Integer.valueOf(count);
        this.priceCoin = Long.valueOf(priceCoin);
        this.priceTear = Long.valueOf(priceTear);
    }

    public boolean isEnabled()
    {
        return this.enabled == null || this.enabled.booleanValue();
    }

    public String itemOrEmpty()
    {
        return this.item == null ? "" : this.item;
    }

    public int countOrZero()
    {
        return this.count == null ? 0 : this.count.intValue();
    }

    public long priceCoinOrZero()
    {
        return this.priceCoin == null ? 0L : this.priceCoin.longValue();
    }

    public long priceTearOrZero()
    {
        return this.priceTear == null ? 0L : this.priceTear.longValue();
    }

    public boolean isFree()
    {
        return priceCoinOrZero() <= 0L && priceTearOrZero() <= 0L;
    }

    public ShopOffer copy()
    {
        ShopOffer result = new ShopOffer();
        result.id = this.id;
        result.enabled = this.enabled;
        result.item = this.item;
        result.count = this.count;
        result.priceCoin = this.priceCoin;
        result.priceTear = this.priceTear;
        return result;
    }
}
