/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One named prize pool: a set of regions, each holding a set of weighted entries.
 * The dice point selects the region, so a region no longer carries a value threshold.
 * The config file holds an array of these objects and a separate state file names the active one.
 */
public class PrizePoolConfig
{
    public String id;
    public String name;
    public String description;
    public List<PrizeRegion> regions;

    public PrizePoolConfig()
    {
    }

    public static PrizePoolConfig empty()
    {
        PrizePoolConfig result = new PrizePoolConfig();
        result.id = "";
        result.name = "";
        result.description = "";
        result.regions = new ArrayList<PrizeRegion>();
        return result;
    }

    public String idOrEmpty()
    {
        return this.id == null ? "" : this.id;
    }

    public String nameOrEmpty()
    {
        return this.name == null ? "" : this.name;
    }

    public String descriptionOrEmpty()
    {
        return this.description == null ? "" : this.description;
    }

    public List<PrizeRegion> regionsOrEmpty()
    {
        return this.regions == null ? new ArrayList<PrizeRegion>() : this.regions;
    }

    /**
     * Regions ordered by tier, which is also the dice point that lands on them.
     */
    public List<PrizeRegion> sortedRegions()
    {
        List<PrizeRegion> result = new ArrayList<PrizeRegion>(regionsOrEmpty());
        result.sort(Comparator.comparingInt(PrizeRegion::tierOrZero));
        return result;
    }

    public PrizeRegion findRegion(int tier)
    {
        for (PrizeRegion region : regionsOrEmpty())
        {
            if (region.tierOrZero() == tier)
            {
                return region;
            }
        }

        return null;
    }

    public int minTier()
    {
        int result = Integer.MAX_VALUE;

        for (PrizeRegion region : regionsOrEmpty())
        {
            result = Math.min(result, region.tierOrZero());
        }

        return result == Integer.MAX_VALUE ? ModConstants.MIN_REGION_TIER : result;
    }

    public int maxTier()
    {
        int result = Integer.MIN_VALUE;

        for (PrizeRegion region : regionsOrEmpty())
        {
            result = Math.max(result, region.tierOrZero());
        }

        return result == Integer.MIN_VALUE ? ModConstants.MIN_REGION_TIER : result;
    }

    public int totalEntryCount()
    {
        int result = 0;

        for (PrizeRegion region : regionsOrEmpty())
        {
            result += region.entriesOrEmpty().size();
        }

        return result;
    }

    public PrizePoolConfig copy()
    {
        PrizePoolConfig result = new PrizePoolConfig();
        result.id = this.id;
        result.name = this.name;
        result.description = this.description;
        result.regions = new ArrayList<PrizeRegion>();

        for (PrizeRegion region : regionsOrEmpty())
        {
            result.regions.add(region.copy());
        }

        return result;
    }

    /**
     * Returns the list of structural problems, empty when the config is usable.
     */
    public List<String> validate()
    {
        List<String> problems = new ArrayList<String>();
        List<PrizeRegion> regionList = regionsOrEmpty();

        if (regionList.size() < 1 || regionList.size() > ModConstants.MAX_REGIONS)
        {
            problems.add("region count out of range: " + regionList.size());
        }

        Set<Integer> tiers = new HashSet<Integer>();
        Set<String> ids = new HashSet<String>();
        int totalEntries = 0;

        for (PrizeRegion region : regionList)
        {
            int tier = region.tierOrZero();

            if (tier < ModConstants.MIN_REGION_TIER || tier > ModConstants.MAX_REGIONS)
            {
                problems.add("region tier out of range: " + tier);
            }

            if (!tiers.add(Integer.valueOf(tier)))
            {
                problems.add("duplicate region tier: " + tier);
            }

            List<PrizeEntry> entries = region.entriesOrEmpty();

            if (entries.size() > ModConstants.MAX_ENTRIES_PER_REGION)
            {
                problems.add("too many entries in region " + tier + ": " + entries.size());
            }

            totalEntries += entries.size();

            for (PrizeEntry entry : entries)
            {
                String id = entry.id;

                if (id == null || id.isEmpty())
                {
                    problems.add("empty entry id in region " + tier);
                }
                else if (id.length() > ModConstants.MAX_ENTRY_ID_LENGTH)
                {
                    problems.add("entry id too long: " + id);
                }
                else if (!ids.add(id))
                {
                    problems.add("duplicate entry id: " + id);
                }

                if (entry.weightOrZero() < 0.0D)
                {
                    problems.add("negative weight on entry " + id);
                }

                String command = entry.commandOrEmpty();

                if (command.isEmpty())
                {
                    problems.add("empty command on entry " + id);
                }
                else if (command.length() > ModConstants.MAX_COMMAND_LENGTH)
                {
                    problems.add("command too long on entry " + id);
                }

                String notifyText = entry.notifyOrDefault().textOrEmpty();

                if (notifyText.length() > ModConstants.MAX_NOTIFY_TEXT_LENGTH)
                {
                    problems.add("notify text too long on entry " + id);
                }
            }
        }

        if (totalEntries > ModConstants.MAX_TOTAL_ENTRIES)
        {
            problems.add("too many entries in total: " + totalEntries);
        }

        return problems;
    }

    /**
     * Clamps the boxed optional fields into an explicit, serialisable shape.
     */
    public void normalize()
    {
        if (this.id == null)
        {
            this.id = "";
        }

        if (this.name == null)
        {
            this.name = "";
        }

        if (this.description == null)
        {
            this.description = "";
        }

        if (this.regions == null)
        {
            this.regions = new ArrayList<PrizeRegion>();
        }

        for (PrizeRegion region : this.regions)
        {
            if (region.tier == null)
            {
                region.tier = Integer.valueOf(ModConstants.MIN_REGION_TIER);
            }

            if (region.entries == null)
            {
                region.entries = new ArrayList<PrizeEntry>();
            }

            for (PrizeEntry entry : region.entries)
            {
                if (entry.enabled == null)
                {
                    entry.enabled = Boolean.TRUE;
                }

                if (entry.weight == null)
                {
                    entry.weight = Double.valueOf(0.0D);
                }

                entry.color = entry.colorOrDefault();

                if (entry.majorEvent == null)
                {
                    entry.majorEvent = Boolean.FALSE;
                }

                if (entry.command == null)
                {
                    entry.command = "";
                }

                if (entry.notify == null)
                {
                    entry.notify = new NotifyConfig();
                }

                entry.notify.mode = entry.notify.modeOrDefault();
                entry.notify.text = entry.notify.textOrEmpty();
            }
        }
    }
}
