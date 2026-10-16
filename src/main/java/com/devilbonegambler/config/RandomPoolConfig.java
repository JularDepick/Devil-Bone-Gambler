/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Sample space of the random prize tokens, rebuilt from the registries on every world load so that the pool always
 * mirrors the content that is actually installed, mod items included.
 * The scan overwrites the pools instead of merging them, so removed content never lingers.
 */
public class RandomPoolConfig
{
    public static final int CURRENT_SCHEMA = 1;

    public Integer schemaVersion;
    public Integer revision;
    public Map<String, List<String>> pools;

    public RandomPoolConfig()
    {
    }

    public static RandomPoolConfig empty()
    {
        RandomPoolConfig result = new RandomPoolConfig();
        result.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);
        result.revision = Integer.valueOf(0);
        result.pools = new LinkedHashMap<String, List<String>>();

        for (String name : ModConstants.RANDOM_POOLS)
        {
            result.pools.put(name, new ArrayList<String>());
        }

        return result;
    }

    public int revisionOrZero()
    {
        return this.revision == null ? 0 : this.revision.intValue();
    }

    public Map<String, List<String>> poolsOrEmpty()
    {
        return this.pools == null ? new LinkedHashMap<String, List<String>>() : this.pools;
    }

    /**
     * Entries of one pool, or an empty list when the pool is unknown, so a caller never sees null.
     */
    public List<String> poolOf(String name)
    {
        List<String> entries = this.poolsOrEmpty().get(name);
        return entries == null ? new ArrayList<String>() : entries;
    }

    public int totalEntries()
    {
        int total = 0;

        for (List<String> entries : this.poolsOrEmpty().values())
        {
            total += entries.size();
        }

        return total;
    }

    /**
     * Replaces one pool with the scanned ids, which is how the world load refreshes the sample space.
     */
    public boolean replacePool(String name, List<String> entries)
    {
        if (this.pools == null)
        {
            this.pools = new LinkedHashMap<String, List<String>>();
        }

        TreeSet<String> sorted = new TreeSet<String>(entries);
        List<String> previous = this.pools.get(name);

        if (previous != null && previous.equals(new ArrayList<String>(sorted)))
        {
            return false;
        }

        this.pools.put(name, new ArrayList<String>(sorted));
        return true;
    }

    public void normalize()
    {
        this.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);

        if (this.revision == null || this.revision.intValue() < 0)
        {
            this.revision = Integer.valueOf(0);
        }

        if (this.pools == null)
        {
            this.pools = new LinkedHashMap<String, List<String>>();
        }

        for (String name : ModConstants.RANDOM_POOLS)
        {
            List<String> entries = this.pools.get(name);

            if (entries == null)
            {
                this.pools.put(name, new ArrayList<String>());
            }
            else
            {
                TreeSet<String> sorted = new TreeSet<String>(entries);
                this.pools.put(name, new ArrayList<String>(sorted));
            }
        }
    }
}
