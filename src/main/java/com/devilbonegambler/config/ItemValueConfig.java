/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The value table, split into an item chapter and an entity chapter.
 * Values are held as Number so that a hand written JSON number keeps its original text form.
 * The legacy v1 fields are only read for migration and are cleared so that they are never written back.
 */
public class ItemValueConfig
{
    public static final int CURRENT_SCHEMA = 2;

    public Integer schemaVersion;
    public Integer revision;
    public Integer defaultItemValue;
    public Integer defaultEntityValue;
    public Integer enchantValueBase;
    public Map<String, Number> items;
    public Map<String, Number> entities;

    public Integer defaultValue;
    public Map<String, Number> values;

    public ItemValueConfig()
    {
    }

    public static ItemValueConfig empty()
    {
        ItemValueConfig result = new ItemValueConfig();
        result.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);
        result.revision = Integer.valueOf(0);
        result.defaultItemValue = Integer.valueOf(ModConstants.DEFAULT_ITEM_VALUE);
        result.defaultEntityValue = Integer.valueOf(ModConstants.DEFAULT_ENTITY_VALUE);
        result.enchantValueBase = Integer.valueOf(ModConstants.DEFAULT_ENCHANT_VALUE);
        result.items = new TreeMap<String, Number>();
        result.entities = new TreeMap<String, Number>();
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

    public int defaultItemValueOrDefault()
    {
        return this.defaultItemValue == null
                ? ModConstants.DEFAULT_ITEM_VALUE
                : Math.max(0, this.defaultItemValue.intValue());
    }

    public int defaultEntityValueOrDefault()
    {
        return this.defaultEntityValue == null
                ? ModConstants.DEFAULT_ENTITY_VALUE
                : Math.max(0, this.defaultEntityValue.intValue());
    }

    /**
     * Value added per enchantment level, adjustable by an administrator.
     */
    public int enchantValueBaseOrDefault()
    {
        return this.enchantValueBase == null
                ? ModConstants.DEFAULT_ENCHANT_VALUE
                : Math.max(0, this.enchantValueBase.intValue());
    }

    public Map<String, Number> itemsOrEmpty()
    {
        return this.items == null ? new TreeMap<String, Number>() : this.items;
    }

    public Map<String, Number> entitiesOrEmpty()
    {
        return this.entities == null ? new TreeMap<String, Number>() : this.entities;
    }

    public boolean isRegisteredItem(String itemId)
    {
        return itemId != null && itemsOrEmpty().containsKey(itemId);
    }

    public boolean isRegisteredEntity(String entityId)
    {
        return entityId != null && entitiesOrEmpty().containsKey(entityId);
    }

    /**
     * Value of one unit of the given item, falling back to the configured default value.
     */
    public int itemValueOf(String itemId)
    {
        return resolve(itemsOrEmpty(), itemId, defaultItemValueOrDefault());
    }

    /**
     * Value of the given entity type, falling back to the configured default value.
     */
    public int entityValueOf(String entityId)
    {
        return resolve(entitiesOrEmpty(), entityId, defaultEntityValueOrDefault());
    }

    private static int resolve(Map<String, Number> source, String key, int fallback)
    {
        if (key == null)
        {
            return fallback;
        }

        Number configured = source.get(key);

        if (configured == null)
        {
            return fallback;
        }

        double raw = configured.doubleValue();

        if (raw < 0.0D)
        {
            return fallback;
        }

        return (int) Math.min(Integer.MAX_VALUE, Math.round(raw));
    }

    public ItemValueConfig copy()
    {
        ItemValueConfig result = new ItemValueConfig();
        result.schemaVersion = this.schemaVersion;
        result.revision = this.revision;
        result.defaultItemValue = this.defaultItemValue;
        result.defaultEntityValue = this.defaultEntityValue;
        result.enchantValueBase = this.enchantValueBase;
        result.items = new TreeMap<String, Number>(itemsOrEmpty());
        result.entities = new TreeMap<String, Number>(entitiesOrEmpty());
        return result;
    }

    public List<String> validate()
    {
        List<String> problems = new ArrayList<String>();
        collectProblems(problems, itemsOrEmpty(), "item");
        collectProblems(problems, entitiesOrEmpty(), "entity");
        return problems;
    }

    private static void collectProblems(List<String> problems, Map<String, Number> source, String kind)
    {
        for (Map.Entry<String, Number> entry : source.entrySet())
        {
            String key = entry.getKey();

            if (key == null || key.isEmpty() || !key.contains(":"))
            {
                problems.add("invalid " + kind + " key: " + key);
                continue;
            }

            Number value = entry.getValue();

            if (value == null)
            {
                problems.add("missing value for " + key);
                continue;
            }

            if (value.doubleValue() < 0.0D)
            {
                problems.add("negative value for " + key);
            }
        }
    }

    /**
     * Applies the v1 to v2 migration and clamps the boxed optional fields.
     */
    public void normalize()
    {
        if (this.schemaVersion == null || this.schemaVersion.intValue() < CURRENT_SCHEMA)
        {
            migrateFromLegacy();
        }

        if (this.defaultItemValue == null)
        {
            this.defaultItemValue = Integer.valueOf(ModConstants.DEFAULT_ITEM_VALUE);
        }

        if (this.defaultEntityValue == null)
        {
            this.defaultEntityValue = Integer.valueOf(ModConstants.DEFAULT_ENTITY_VALUE);
        }

        if (this.enchantValueBase == null)
        {
            this.enchantValueBase = Integer.valueOf(ModConstants.DEFAULT_ENCHANT_VALUE);
        }

        if (this.revision == null)
        {
            this.revision = Integer.valueOf(0);
        }

        this.items = sortedCopy(this.items);
        this.entities = sortedCopy(this.entities);

        this.defaultValue = null;
        this.values = null;
    }

    private void migrateFromLegacy()
    {
        if (this.items == null)
        {
            this.items = new TreeMap<String, Number>();

            if (this.values != null)
            {
                this.items.putAll(this.values);
            }
        }

        if (this.entities == null)
        {
            this.entities = new TreeMap<String, Number>();
        }

        if (this.defaultItemValue == null && this.defaultValue != null)
        {
            this.defaultItemValue = this.defaultValue;
        }

        this.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);
    }

    private static Map<String, Number> sortedCopy(Map<String, Number> source)
    {
        Map<String, Number> sorted = new TreeMap<String, Number>();

        if (source == null)
        {
            return sorted;
        }

        for (Map.Entry<String, Number> entry : source.entrySet())
        {
            if (entry.getKey() != null && entry.getValue() != null)
            {
                sorted.put(entry.getKey(), entry.getValue());
            }
        }

        return sorted;
    }
}
