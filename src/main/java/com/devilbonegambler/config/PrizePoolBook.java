/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Every prize pool of the server plus the pointer to the one that is active.
 *
 * The config file itself is a plain JSON array of pools; the revision and the active id live in a small state file,
 * so an administrator can keep several scenarios side by side and switch which one a dice draw uses.
 */
public class PrizePoolBook
{
    public static final int CURRENT_SCHEMA = 2;

    public Integer schemaVersion;
    public Integer revision;
    public String activeId;
    public List<PrizePoolConfig> pools;

    public PrizePoolBook()
    {
    }

    public static PrizePoolBook empty()
    {
        PrizePoolBook result = new PrizePoolBook();
        result.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);
        result.revision = Integer.valueOf(0);
        result.activeId = "";
        result.pools = new ArrayList<PrizePoolConfig>();
        return result;
    }

    public int revisionOrZero()
    {
        return this.revision == null ? 0 : this.revision.intValue();
    }

    public List<PrizePoolConfig> poolsOrEmpty()
    {
        return this.pools == null ? new ArrayList<PrizePoolConfig>() : this.pools;
    }

    public PrizePoolConfig findById(String id)
    {
        for (PrizePoolConfig pool : poolsOrEmpty())
        {
            if (pool.idOrEmpty().equals(id))
            {
                return pool;
            }
        }

        return null;
    }

    /**
     * The active pool, falling back to the first pool so a dice draw never runs without a pool.
     */
    public PrizePoolConfig active()
    {
        PrizePoolConfig found = findById(this.activeId == null ? "" : this.activeId);

        if (found != null)
        {
            return found;
        }

        List<PrizePoolConfig> list = poolsOrEmpty();
        return list.isEmpty() ? null : list.get(0);
    }

    public int totalEntryCount()
    {
        int total = 0;

        for (PrizePoolConfig pool : poolsOrEmpty())
        {
            total += pool.totalEntryCount();
        }

        return total;
    }

    public PrizePoolBook copy()
    {
        PrizePoolBook result = new PrizePoolBook();
        result.schemaVersion = this.schemaVersion;
        result.revision = this.revision;
        result.activeId = this.activeId;
        result.pools = new ArrayList<PrizePoolConfig>();

        for (PrizePoolConfig pool : poolsOrEmpty())
        {
            result.pools.add(pool.copy());
        }

        return result;
    }

    public List<String> validate()
    {
        List<String> problems = new ArrayList<String>();
        List<PrizePoolConfig> list = poolsOrEmpty();

        if (list.isEmpty())
        {
            problems.add("no prize pool");
        }
        else if (list.size() > ModConstants.MAX_POOLS)
        {
            problems.add("too many prize pools: " + list.size());
        }

        Set<String> ids = new HashSet<String>();

        for (PrizePoolConfig pool : list)
        {
            String id = pool.idOrEmpty();

            if (id.isEmpty() || id.length() > ModConstants.MAX_POOL_ID_LENGTH)
            {
                problems.add("pool id out of range: " + id);
            }
            else if (!ids.add(id))
            {
                problems.add("duplicate pool id: " + id);
            }

            String name = pool.nameOrEmpty();

            if (name.isEmpty() || name.length() > ModConstants.MAX_POOL_NAME_LENGTH)
            {
                problems.add("pool name out of range: " + name);
            }

            if (pool.descriptionOrEmpty().length() > ModConstants.MAX_POOL_DESCRIPTION_LENGTH)
            {
                problems.add("pool description too long: " + name);
            }

            problems.addAll(pool.validate());
        }

        if (this.activeId == null || !ids.contains(this.activeId))
        {
            problems.add("active pool is missing: " + this.activeId);
        }

        return problems;
    }

    /**
     * Fills missing fields and points the active id at a pool that exists.
     */
    public void normalize()
    {
        if (this.schemaVersion == null || this.schemaVersion.intValue() < CURRENT_SCHEMA)
        {
            this.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);
        }

        if (this.revision == null)
        {
            this.revision = Integer.valueOf(0);
        }

        if (this.pools == null)
        {
            this.pools = new ArrayList<PrizePoolConfig>();
        }

        for (PrizePoolConfig pool : this.pools)
        {
            pool.normalize();
        }

        if (findById(this.activeId == null ? "" : this.activeId) == null)
        {
            this.activeId = this.pools.isEmpty() ? "" : this.pools.get(0).idOrEmpty();
        }
    }
}
