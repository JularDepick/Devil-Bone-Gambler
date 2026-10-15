/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable client side view of the value table carried by one value table book.
 */
public final class ValueTableSnapshot
{
    private final int revision;
    private final int defaultValue;
    private final int enchantValueBase;
    private final Map<String, Integer> values;

    public ValueTableSnapshot(int revision, int defaultValue, int enchantValueBase, Map<String, Integer> values)
    {
        this.revision = revision;
        this.defaultValue = Math.max(0, defaultValue);
        this.enchantValueBase = Math.max(0, enchantValueBase);
        this.values = values == null ? Collections.<String, Integer>emptyMap() : values;
    }

    public int revision()
    {
        return this.revision;
    }

    public int defaultValue()
    {
        return this.defaultValue;
    }

    public int enchantValueBase()
    {
        return this.enchantValueBase;
    }

    public boolean isRegistered(String itemId)
    {
        return itemId != null && this.values.containsKey(itemId);
    }

    public int valueOf(String itemId)
    {
        Integer value = itemId == null ? null : this.values.get(itemId);
        return value == null ? this.defaultValue : value.intValue();
    }
}
