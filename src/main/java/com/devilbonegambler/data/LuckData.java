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
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server wide luck value per player, keyed by UUID.
 *
 * Every entry also remembers the wall clock instant of the first join, which the hidden fate model uses as its first
 * argument. A player without an entry reads as the base value and receives the birth instant on the first join.
 */
public class LuckData extends SavedData
{
    private static final String TAG_ENTRIES = "luck";
    private static final String TAG_VALUE = "value";
    private static final String TAG_BIRTH = "birth";

    private final Map<UUID, Entry> entries = new HashMap<UUID, Entry>();

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
                UUID playerId = UUID.fromString(key);
                Tag raw = stored.get(key);

                if (raw instanceof CompoundTag entryTag)
                {
                    data.entries.put(playerId, new Entry(entryTag.getInt(TAG_VALUE), entryTag.getLong(TAG_BIRTH)));
                }
                else if (raw instanceof NumericTag number)
                {
                    /* A value written before the birth instant existed keeps its number and gains the instant later. */
                    data.entries.put(playerId, new Entry(number.getAsInt(), 0L));
                }
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

        for (Map.Entry<UUID, Entry> entry : this.entries.entrySet())
        {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt(TAG_VALUE, entry.getValue().value);
            entryTag.putLong(TAG_BIRTH, entry.getValue().birth);
            stored.put(entry.getKey().toString(), entryTag);
        }

        tag.put(TAG_ENTRIES, stored);
        return tag;
    }

    public int luckOf(UUID playerId)
    {
        Entry entry = this.entries.get(playerId);
        return entry == null ? ModConstants.BASE_LUCK : entry.value;
    }

    public long birthOf(UUID playerId)
    {
        Entry entry = this.entries.get(playerId);
        return entry == null ? 0L : entry.birth;
    }

    /**
     * Returns the entry of a player, creating it with the base value and this instant as the birth, or completing a
     * birth that an older value did not carry yet.
     */
    public Entry ensure(UUID playerId, long now)
    {
        Entry entry = this.entries.get(playerId);

        if (entry == null)
        {
            entry = new Entry(ModConstants.BASE_LUCK, now);
            this.entries.put(playerId, entry);
            setDirty();
        }
        else if (entry.birth <= 0L)
        {
            entry.birth = now;
            setDirty();
        }

        return entry;
    }

    public void setLuck(UUID playerId, int value)
    {
        Entry entry = this.entries.get(playerId);

        if (entry == null)
        {
            entry = new Entry(value, 0L);
            this.entries.put(playerId, entry);
        }
        else
        {
            entry.value = value;
        }

        setDirty();
    }

    public int trackedPlayers()
    {
        return this.entries.size();
    }

    /**
     * The stored pair of a player: the current luck and the instant of the first join.
     */
    public static final class Entry
    {
        private int value;
        private long birth;

        private Entry(int value, long birth)
        {
            this.value = value;
            this.birth = birth;
        }

        public int value()
        {
            return this.value;
        }

        public long birth()
        {
            return this.birth;
        }
    }
}
