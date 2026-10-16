/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.data;

import com.devilbonegambler.util.ModConstants;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Players that already received their manuals, keyed by UUID.
 * A player is handed the manuals once, on the first join the mod sees, so a rejoin never duplicates them.
 */
public class ManualGrantData extends SavedData
{
    private static final String TAG_ENTRIES = "granted";

    private final Set<UUID> entries = new HashSet<UUID>();

    public static ManualGrantData get(MinecraftServer server)
    {
        return server.overworld().getDataStorage()
                .computeIfAbsent(ManualGrantData::load, ManualGrantData::new, ModConstants.MANUAL_GRANT_DATA_NAME);
    }

    public static ManualGrantData load(CompoundTag tag)
    {
        ManualGrantData data = new ManualGrantData();
        ListTag stored = tag.getList(TAG_ENTRIES, Tag.TAG_STRING);

        for (int index = 0; index < stored.size(); index++)
        {
            try
            {
                data.entries.add(UUID.fromString(stored.getString(index)));
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
        ListTag stored = new ListTag();

        for (UUID playerId : this.entries)
        {
            stored.add(StringTag.valueOf(playerId.toString()));
        }

        tag.put(TAG_ENTRIES, stored);
        return tag;
    }

    public boolean isGranted(UUID playerId)
    {
        return this.entries.contains(playerId);
    }

    public void markGranted(UUID playerId)
    {
        if (this.entries.add(playerId))
        {
            setDirty();
        }
    }

    public int trackedPlayers()
    {
        return this.entries.size();
    }
}
