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
 * Both chapters are kept, because the searchable view lists items and creatures alike, while the client hover
 * estimate only ever asks the item chapter.
 */
public final class ValueTableSnapshot
{
    private final int revision;
    private final int defaultValue;
    private final int defaultEntityValue;
    private final Map<String, Integer> values;
    private final Map<String, Integer> entityValues;

    public ValueTableSnapshot(int revision, int defaultValue, int defaultEntityValue, Map<String, Integer> values,
            Map<String, Integer> entityValues)
    {
        this.revision = revision;
        this.defaultValue = Math.max(0, defaultValue);
        this.defaultEntityValue = Math.max(0, defaultEntityValue);
        this.values = values == null ? Collections.<String, Integer>emptyMap() : values;
        this.entityValues = entityValues == null ? Collections.<String, Integer>emptyMap() : entityValues;
    }

    /**
     * An empty snapshot, used when the client opens the screen before any server push arrived.
     */
    public static ValueTableSnapshot empty()
    {
        return new ValueTableSnapshot(0, 0, 0, Collections.<String, Integer>emptyMap(),
                Collections.<String, Integer>emptyMap());
    }

    /**
     * The item chapter, so a screen can iterate it without copying.
     */
    public Map<String, Integer> values()
    {
        return this.values;
    }

    /**
     * The creature chapter, so a screen can iterate it without copying.
     */
    public Map<String, Integer> entities()
    {
        return this.entityValues;
    }

    public int revision()
    {
        return this.revision;
    }

    public int defaultValue()
    {
        return this.defaultValue;
    }

    public int defaultEntityValue()
    {
        return this.defaultEntityValue;
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

    public boolean isRegisteredEntity(String entityId)
    {
        return entityId != null && this.entityValues.containsKey(entityId);
    }

    public int entityValueOf(String entityId)
    {
        Integer value = entityId == null ? null : this.entityValues.get(entityId);
        return value == null ? this.defaultEntityValue : value.intValue();
    }
}
