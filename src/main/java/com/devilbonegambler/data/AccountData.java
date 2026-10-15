/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.data;

import com.devilbonegambler.economy.CurrencyType;
import com.devilbonegambler.util.ModConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Account balances, the server asset counters and the VIP expiry of every player.
 * All values are plain long integers, so this file stays cheap to rewrite and is kept apart
 * from the exchange data, which carries item stacks and grows much larger.
 */
public class AccountData extends SavedData
{
    private static final String TAG_PLAYERS = "players";
    private static final String TAG_BALANCES = "balances";
    private static final String TAG_COUNTERS = "assetCounters";
    private static final String TAG_VIP = "vip";

    private final Map<UUID, long[]> balances = new HashMap<UUID, long[]>();
    private final long[] assetCounters = new long[CurrencyType.count()];
    private final Map<UUID, Long> vipUntil = new HashMap<UUID, Long>();

    public static AccountData get(MinecraftServer server)
    {
        return server.overworld().getDataStorage()
                .computeIfAbsent(AccountData::load, AccountData::new, ModConstants.ACCOUNT_DATA_NAME);
    }

    public static AccountData load(CompoundTag tag)
    {
        AccountData data = new AccountData();
        CompoundTag stored = tag.getCompound(TAG_PLAYERS);

        for (String key : stored.getAllKeys())
        {
            CompoundTag entry = stored.getCompound(key);
            long[] values = new long[CurrencyType.count()];

            for (CurrencyType type : CurrencyType.values())
            {
                values[type.ordinal()] = entry.getLong(type.id());
            }

            try
            {
                data.balances.put(UUID.fromString(key), values);
            }
            catch (IllegalArgumentException exception)
            {
                continue;
            }
        }

        CompoundTag counters = tag.getCompound(TAG_COUNTERS);

        for (CurrencyType type : CurrencyType.values())
        {
            data.assetCounters[type.ordinal()] = counters.getLong(type.id());
        }

        CompoundTag vip = tag.getCompound(TAG_VIP);

        for (String key : vip.getAllKeys())
        {
            try
            {
                data.vipUntil.put(UUID.fromString(key), Long.valueOf(vip.getLong(key)));
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

        for (Map.Entry<UUID, long[]> entry : this.balances.entrySet())
        {
            CompoundTag row = new CompoundTag();
            long[] values = entry.getValue();

            for (CurrencyType type : CurrencyType.values())
            {
                row.putLong(type.id(), values[type.ordinal()]);
            }

            stored.put(entry.getKey().toString(), row);
        }

        tag.put(TAG_PLAYERS, stored);

        CompoundTag counters = new CompoundTag();

        for (CurrencyType type : CurrencyType.values())
        {
            counters.putLong(type.id(), this.assetCounters[type.ordinal()]);
        }

        tag.put(TAG_COUNTERS, counters);

        CompoundTag vip = new CompoundTag();

        for (Map.Entry<UUID, Long> entry : this.vipUntil.entrySet())
        {
            vip.putLong(entry.getKey().toString(), entry.getValue().longValue());
        }

        tag.put(TAG_VIP, vip);
        return tag;
    }

    public long balanceOf(UUID playerId, CurrencyType type)
    {
        long[] values = this.balances.get(playerId);
        return values == null ? 0L : values[type.ordinal()];
    }

    public void setBalance(UUID playerId, CurrencyType type, long amount)
    {
        long[] values = this.balances.computeIfAbsent(playerId, key -> new long[CurrencyType.count()]);
        values[type.ordinal()] = clamp(amount);
        setDirty();
    }

    /**
     * Adds to a balance and returns the amount that could not be added because of the cap.
     */
    public long addBalance(UUID playerId, CurrencyType type, long amount)
    {
        long[] values = this.balances.computeIfAbsent(playerId, key -> new long[CurrencyType.count()]);
        long current = values[type.ordinal()];
        long updated = clamp(current + Math.max(0L, amount));
        values[type.ordinal()] = updated;
        setDirty();
        return Math.max(0L, amount) - (updated - current);
    }

    /**
     * Removes from a balance, returning false when the balance is too small.
     */
    public boolean takeBalance(UUID playerId, CurrencyType type, long amount)
    {
        if (amount <= 0L)
        {
            return true;
        }

        long[] values = this.balances.get(playerId);

        if (values == null || values[type.ordinal()] < amount)
        {
            return false;
        }

        values[type.ordinal()] -= amount;
        setDirty();
        return true;
    }

    public long counterOf(CurrencyType type)
    {
        return this.assetCounters[type.ordinal()];
    }

    /**
     * Adds the retained share of a system charge to the counter. The rest is the world running cost.
     */
    public void addToCounter(CurrencyType type, long amount)
    {
        if (amount <= 0L)
        {
            return;
        }

        long retained = (long) Math.floor(amount * ModConstants.ASSET_RETENTION_RATE);
        this.assetCounters[type.ordinal()] = clamp(this.assetCounters[type.ordinal()] + retained);
        setDirty();
    }

    public boolean takeFromCounter(CurrencyType type, long amount)
    {
        if (amount <= 0L)
        {
            return true;
        }

        if (this.assetCounters[type.ordinal()] < amount)
        {
            return false;
        }

        this.assetCounters[type.ordinal()] -= amount;
        setDirty();
        return true;
    }

    public long vipUntil(UUID playerId)
    {
        Long value = this.vipUntil.get(playerId);
        return value == null ? 0L : value.longValue();
    }

    public void setVipUntil(UUID playerId, long tick)
    {
        this.vipUntil.put(playerId, Long.valueOf(Math.max(0L, tick)));
        setDirty();
    }

    private static long clamp(long value)
    {
        return Math.max(0L, Math.min(ModConstants.MAX_BALANCE, value));
    }
}
