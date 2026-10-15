/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.economy;

import com.devilbonegambler.util.ModConstants;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * One exchange order.
 * A sell order escrows the items, a buy order escrows the price plus the fee. Both expire automatically.
 */
public class ExchangeOrder
{
    private final String id;
    private final UUID owner;
    private final boolean sell;
    private final ItemStack stack;
    private final long priceCoin;
    private final long priceTear;
    private final long feeCoin;
    private final long feeTear;
    private long expiryTick;

    public ExchangeOrder(String id, UUID owner, boolean sell, ItemStack stack, long priceCoin, long priceTear,
            long feeCoin, long feeTear, long expiryTick)
    {
        this.id = id;
        this.owner = owner;
        this.sell = sell;
        this.stack = stack == null ? ItemStack.EMPTY : stack;
        this.priceCoin = Math.max(0L, priceCoin);
        this.priceTear = Math.max(0L, priceTear);
        this.feeCoin = Math.max(0L, feeCoin);
        this.feeTear = Math.max(0L, feeTear);
        this.expiryTick = expiryTick;
    }

    public String id() { return this.id; }

    public UUID owner() { return this.owner; }

    public boolean isSell() { return this.sell; }

    public ItemStack stack() { return this.stack; }

    public long priceCoin() { return this.priceCoin; }

    public long priceTear() { return this.priceTear; }

    public long feeCoin() { return this.feeCoin; }

    public long feeTear() { return this.feeTear; }

    public long expiryTick() { return this.expiryTick; }

    public void setExpiryTick(long tick) { this.expiryTick = tick; }

    /**
     * Fee of one currency leg, rounded down so the system never overcharges.
     */
    public static long feeOf(long price)
    {
        return (long) Math.floor(Math.max(0L, price) * ModConstants.EXCHANGE_FEE_RATE);
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", this.id);
        tag.putUUID("owner", this.owner);
        tag.putBoolean("sell", this.sell);

        if (!this.stack.isEmpty())
        {
            CompoundTag item = new CompoundTag();
            this.stack.copy().save(item);
            tag.put("stack", item);
        }

        tag.putLong("priceCoin", this.priceCoin);
        tag.putLong("priceTear", this.priceTear);
        tag.putLong("feeCoin", this.feeCoin);
        tag.putLong("feeTear", this.feeTear);
        tag.putLong("expiry", this.expiryTick);
        return tag;
    }

    public static ExchangeOrder load(CompoundTag tag)
    {
        ItemStack stack = ItemStack.EMPTY;

        if (tag.contains("stack"))
        {
            stack = ItemStack.of(tag.getCompound("stack"));
        }

        return new ExchangeOrder(tag.getString("id"), tag.getUUID("owner"), tag.getBoolean("sell"), stack,
                tag.getLong("priceCoin"), tag.getLong("priceTear"), tag.getLong("feeCoin"), tag.getLong("feeTear"),
                tag.getLong("expiry"));
    }
}
