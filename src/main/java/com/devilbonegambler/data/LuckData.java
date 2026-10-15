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
 * Server wide luck value per player, keyed by UUID.
 * The value is fixed at the base value in v0.2.0; only the storage contract is implemented here.
 */
public class LuckData extends SavedData
{
    private static final String TAG_ENTRIES = "luck";

    private final Map<UUID, Integer> entries = new HashMap<UUID, Integer>();

    public static LuckData get(MinecraftServer server)
    {
        return server.overworld().getDataStorage()
                .computeIfAbsent(LuckData::load, LuckData::new, ModConstants.LUCK_DATA_NAME);
    }

    public static LuckData load(CompoundTag tag)
    {
        LuckData data = new LuckData();
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

    public int luckOf(UUID playerId)
    {
        Integer value = this.entries.get(playerId);
        return value == null ? ModConstants.BASE_LUCK : value.intValue();
    }

    public void setLuck(UUID playerId, int value)
    {
        this.entries.put(playerId, Integer.valueOf(value));
        setDirty();
    }

    public int trackedPlayers()
    {
        return this.entries.size();
    }
}
