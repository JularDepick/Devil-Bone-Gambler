/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.draw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Everything a devil's dice sacrifice can contain: item offerings, mob offerings and numeric offerings.
 * Any of the three parts may be empty; the server re-validates every part before deducting.
 */
public class OfferingBundle
{
    private final List<Offering> items;
    private final List<UUID> mobs;
    private final int health;
    private final int food;
    private final boolean allHealth;
    private final boolean deathDrop;

    public OfferingBundle(List<Offering> items, List<UUID> mobs, int health, int food, boolean allHealth,
            boolean deathDrop)
    {
        this.items = items == null ? new ArrayList<Offering>() : items;
        this.mobs = mobs == null ? new ArrayList<UUID>() : mobs;
        this.health = Math.max(0, health);
        this.food = Math.max(0, food);
        this.allHealth = allHealth;
        this.deathDrop = deathDrop;
    }

    public static OfferingBundle empty()
    {
        return new OfferingBundle(Collections.<Offering>emptyList(), Collections.<UUID>emptyList(), 0, 0, false, false);
    }

    public static OfferingBundle itemsOnly(List<Offering> items)
    {
        return new OfferingBundle(items, Collections.<UUID>emptyList(), 0, 0, false, false);
    }

    public List<Offering> items()
    {
        return this.items;
    }

    public List<UUID> mobs()
    {
        return this.mobs;
    }

    public int health()
    {
        return this.health;
    }

    public int food()
    {
        return this.food;
    }

    public boolean allHealth()
    {
        return this.allHealth;
    }

    public boolean deathDrop()
    {
        return this.deathDrop;
    }

    /**
     * A bundle counts as empty when it carries no offering at all, including no numeric offering.
     */
    public boolean isEmpty()
    {
        return this.items.isEmpty() && this.mobs.isEmpty() && this.health <= 0 && this.food <= 0 && !this.allHealth;
    }

    public int itemEntryCount()
    {
        return this.items.size();
    }
}
