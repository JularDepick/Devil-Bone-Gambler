/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.data;

import com.devilbonegambler.util.ModConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Consumed gambler's dice per player, keyed by UUID.
 * The counter survives death and reconnects because it lives in the overworld data storage.
 */
public class DealerUsageData extends SavedData
{
    private static final String TAG_ENTRIES = "usage";

    private final Map<UUID, Integer> entries = new HashMap<UUID, Integer>();

    public static DealerUsageData get(MinecraftServer server)
    {
        return server.overworld().getDataStorage()
                .computeIfAbsent(DealerUsageData::load, DealerUsageData::new, ModConstants.DEALER_USAGE_DATA_NAME);
    }

    public static DealerUsageData load(CompoundTag tag)
    {
        DealerUsageData data = new DealerUsageData();
        CompoundTag stored = tag.getCompound(TAG_ENTRIES);

        for (String key : stored.getAllKeys())
        {
            try
            {
                data.entries.put(UUID.fromString(key), Integer.valueOf(stored.getInt(key)));
            }
            catch (IllegalArgumentException exception)
            {
                continue;
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        CompoundTag stored = new CompoundTag();

        for (Map.Entry<UUID, Integer> entry : this.entries.entrySet())
        {
            stored.putInt(entry.getKey().toString(), entry.getValue().intValue());
        }

        tag.put(TAG_ENTRIES, stored);
        return tag;
    }

    public int usageOf(UUID playerId)
    {
        Integer value = this.entries.get(playerId);
        return value == null ? 0 : value.intValue();
    }

    /**
     * Adds one consumed dice and returns how many thresholds were crossed, which is the number of eggs to hand out.
     * The remainder stays in the counter, so the progress is never lost.
     */
    public int addUsage(UUID playerId, int perReward)
    {
        int threshold = Math.max(1, perReward);
        int updated = this.usageOf(playerId) + 1;
        int rewards = updated / threshold;
        this.entries.put(playerId, Integer.valueOf(updated % threshold));
        setDirty();
        return rewards;
    }
}
