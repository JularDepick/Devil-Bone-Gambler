/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.data;

import com.devilbonegambler.economy.ExchangeOrder;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Exchange orders and the items waiting to be collected.
 * This file carries item stacks, so it is kept apart from the account data, which is written far more often.
 * Every order count is capped to stay well below the single tag size limit.
 */
public class ExchangeData extends SavedData
{
    private static final String TAG_ORDERS = "orders";
    private static final String TAG_PENDING = "pending";

    private final Map<String, ExchangeOrder> orders = new LinkedHashMap<String, ExchangeOrder>();
    private final Map<UUID, List<ItemStack>> pendingItems = new HashMap<UUID, List<ItemStack>>();

    public static ExchangeData get(MinecraftServer server)
    {
        return server.overworld().getDataStorage()
                .computeIfAbsent(ExchangeData::load, ExchangeData::new, ModConstants.EXCHANGE_DATA_NAME);
    }

    public static ExchangeData load(CompoundTag tag)
    {
        ExchangeData data = new ExchangeData();
        ListTag stored = tag.getList(TAG_ORDERS, Tag.TAG_COMPOUND);

        for (int index = 0; index < stored.size(); index++)
        {
            ExchangeOrder order = ExchangeOrder.load(stored.getCompound(index));

            if (order.id() != null && !order.id().isEmpty())
            {
                data.orders.put(order.id(), order);
            }
        }

        CompoundTag pending = tag.getCompound(TAG_PENDING);

        for (String key : pending.getAllKeys())
        {
            ListTag items = pending.getList(key, Tag.TAG_COMPOUND);
            List<ItemStack> list = new ArrayList<ItemStack>();

            for (int index = 0; index < items.size(); index++)
            {
                ItemStack stack = ItemStack.of(items.getCompound(index));

                if (!stack.isEmpty())
                {
                    list.add(stack);
                }
            }

            try
            {
                data.pendingItems.put(UUID.fromString(key), list);
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

        for (ExchangeOrder order : this.orders.values())
        {
            stored.add(order.save());
        }

        tag.put(TAG_ORDERS, stored);

        CompoundTag pending = new CompoundTag();

        for (Map.Entry<UUID, List<ItemStack>> entry : this.pendingItems.entrySet())
        {
            ListTag items = new ListTag();

            for (ItemStack stack : entry.getValue())
            {
                if (!stack.isEmpty())
                {
                    items.add(stack.copy().save(new CompoundTag()));
                }
            }

            pending.put(entry.getKey().toString(), items);
        }

        tag.put(TAG_PENDING, pending);
        return tag;
    }

    public Map<String, ExchangeOrder> orders()
    {
        return this.orders;
    }

    public List<ExchangeOrder> ordersOf(UUID owner)
    {
        List<ExchangeOrder> result = new ArrayList<ExchangeOrder>();

        for (ExchangeOrder order : this.orders.values())
        {
            if (order.owner().equals(owner))
            {
                result.add(order);
            }
        }

        return result;
    }

    public int orderCountOf(UUID owner)
    {
        return ordersOf(owner).size();
    }

    public void putOrder(ExchangeOrder order)
    {
        this.orders.put(order.id(), order);
        setDirty();
    }

    public ExchangeOrder removeOrder(String id)
    {
        ExchangeOrder removed = this.orders.remove(id);

        if (removed != null)
        {
            setDirty();
        }

        return removed;
    }

    public void addPending(UUID playerId, ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return;
        }

        List<ItemStack> list = this.pendingItems.computeIfAbsent(playerId, key -> new ArrayList<ItemStack>());

        if (list.size() >= ModConstants.MAX_ESCROW_ENTRIES * ModConstants.MAX_ORDERS_PER_PLAYER)
        {
            return;
        }

        list.add(stack.copy());
        setDirty();
    }

    public List<ItemStack> pendingOf(UUID playerId)
    {
        List<ItemStack> list = this.pendingItems.get(playerId);
        return list == null ? new ArrayList<ItemStack>() : list;
    }

    public void clearPending(UUID playerId)
    {
        if (this.pendingItems.remove(playerId) != null)
        {
            setDirty();
        }
    }
}
