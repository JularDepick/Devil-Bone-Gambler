/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.economy.CurrencyType;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The system shop: a bounded list of offers with unlimited stock.
 */
public class ShopConfig
{
    public static final int CURRENT_SCHEMA = 1;

    public Integer schemaVersion;
    public Integer revision;
    public List<ShopOffer> offers;

    public ShopConfig()
    {
    }

    public static ShopConfig empty()
    {
        ShopConfig result = new ShopConfig();
        result.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);
        result.revision = Integer.valueOf(0);
        result.offers = new ArrayList<ShopOffer>();
        return result;
    }

    /**
     * The preset shop only contains the official exchange between the two currencies.
     */
    public static ShopConfig create()
    {
        ShopConfig result = empty();
        result.offers.add(new ShopOffer("coin_to_tear", true, CurrencyType.TEAR.itemId(), 1, 64L, 0L));
        result.offers.add(new ShopOffer("tear_to_coin", true, CurrencyType.COIN.itemId(), 8, 0L, 1L));
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

    public List<ShopOffer> offersOrEmpty()
    {
        return this.offers == null ? new ArrayList<ShopOffer>() : this.offers;
    }

    public ShopOffer find(String id)
    {
        for (ShopOffer offer : offersOrEmpty())
        {
            if (offer.id != null && offer.id.equals(id))
            {
                return offer;
            }
        }

        return null;
    }

    public ShopConfig copy()
    {
        ShopConfig result = new ShopConfig();
        result.schemaVersion = this.schemaVersion;
        result.revision = this.revision;
        result.offers = new ArrayList<ShopOffer>();

        for (ShopOffer offer : offersOrEmpty())
        {
            result.offers.add(offer.copy());
        }

        return result;
    }

    public List<String> validate()
    {
        List<String> problems = new ArrayList<String>();
        List<ShopOffer> list = offersOrEmpty();

        if (list.size() > ModConstants.MAX_SHOP_OFFERS)
        {
            problems.add("too many shop offers: " + list.size());
        }

        Set<String> ids = new HashSet<String>();

        for (ShopOffer offer : list)
        {
            String id = offer.id;

            if (id == null || id.isEmpty())
            {
                problems.add("empty shop offer id");
            }
            else if (!ids.add(id))
            {
                problems.add("duplicate shop offer id: " + id);
            }

            String item = offer.itemOrEmpty();

            if (item.isEmpty() || !item.contains(":"))
            {
                problems.add("invalid shop offer item: " + item);
            }

            if (offer.countOrZero() <= 0)
            {
                problems.add("invalid shop offer count on " + id);
            }

            if (offer.priceCoinOrZero() < 0L || offer.priceTearOrZero() < 0L)
            {
                problems.add("negative price on " + id);
            }

            if (offer.isFree())
            {
                problems.add("free shop offer: " + id);
            }
        }

        return problems;
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

        if (this.offers == null)
        {
            this.offers = new ArrayList<ShopOffer>();
        }

        for (ShopOffer offer : this.offers)
        {
            if (offer.enabled == null)
            {
                offer.enabled = Boolean.TRUE;
            }

            if (offer.item == null)
            {
                offer.item = "";
            }

            if (offer.count == null)
            {
                offer.count = Integer.valueOf(1);
            }

            if (offer.priceCoin == null)
            {
                offer.priceCoin = Long.valueOf(0L);
            }

            if (offer.priceTear == null)
            {
                offer.priceTear = Long.valueOf(0L);
            }
        }
    }
}
