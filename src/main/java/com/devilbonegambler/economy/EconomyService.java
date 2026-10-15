/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.economy;

import com.devilbonegambler.data.AccountData;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Every money movement of the mod goes through here, so the asset counter rule stays in one place:
 * money that is charged to a player and never reaches another player is added to the server counter
 * at the retention rate, and the rest is the world running cost.
 */
public final class EconomyService
{
    private EconomyService()
    {
    }

    public static AccountData data(MinecraftServer server)
    {
        return AccountData.get(server);
    }

    /**
     * Charges a player and keeps the money on the server side. Used by the shop and by the exchange fees.
     */
    public static boolean chargeSystem(MinecraftServer server, UUID playerId, CurrencyType type, long amount)
    {
        AccountData data = data(server);

        if (!data.takeBalance(playerId, type, amount))
        {
            return false;
        }

        data.addToCounter(type, amount);
        return true;
    }

    /**
     * Moves money between two players. No counter change, because the money still belongs to a player.
     */
    public static boolean transfer(MinecraftServer server, UUID from, UUID to, CurrencyType type, long amount)
    {
        AccountData data = data(server);

        if (!data.takeBalance(from, type, amount))
        {
            return false;
        }

        data.addBalance(to, type, amount);
        return true;
    }

    /**
     * Converts currency items into balance. No counter change, the money never left the player side.
     */
    public static boolean depositItems(ServerPlayer player, CurrencyType type, int amount)
    {
        if (amount <= 0 || countItems(player, type) < amount)
        {
            return false;
        }

        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return false;
        }

        removeItems(player, type, amount);
        data(server).addBalance(player.getUUID(), type, amount);
        return true;
    }

    /**
     * Converts balance into currency items. The inventory must be able to hold the whole stack.
     */
    public static boolean withdrawItems(ServerPlayer player, CurrencyType type, int amount)
    {
        if (amount <= 0)
        {
            return false;
        }

        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return false;
        }

        ItemStack stack = new ItemStack(itemOf(type), amount);

        if (!canFullyFit(player.getInventory(), stack))
        {
            return false;
        }

        if (!data(server).takeBalance(player.getUUID(), type, amount))
        {
            return false;
        }

        player.getInventory().add(stack);
        return true;
    }

    /**
     * Pays a mixed amount: the item share comes out of the inventory, the remainder out of the balance.
     * The whole charged amount is a system sink, so the counter is fed with the total.
     */
    public static boolean payMixed(ServerPlayer player, CurrencyType type, long total, long itemShare)
    {
        if (total < 0L)
        {
            return false;
        }

        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return false;
        }

        long fromItems = Math.max(0L, Math.min(itemShare, total));
        long fromBalance = total - fromItems;

        if (fromItems > 0L)
        {
            if (fromItems > Integer.MAX_VALUE || countItems(player, type) < (int) fromItems)
            {
                return false;
            }

            removeItems(player, type, (int) fromItems);
        }

        if (fromBalance > 0L && !chargeSystem(server, player.getUUID(), type, fromBalance))
        {
            /*
             * The balance could not cover the remainder, so put the item share back.
             */
            if (fromItems > 0L)
            {
                player.getInventory().add(new ItemStack(itemOf(type), (int) fromItems));
            }

            return false;
        }

        if (fromItems > 0L)
        {
            data(server).addToCounter(type, fromItems);
        }

        return true;
    }

    public static boolean isVip(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return false;
        }

        return data(server).vipUntil(player.getUUID()) > server.getTickCount();
    }

    /**
     * Opens or renews VIP, charging both currencies as configured. Both prices are system sinks.
     */
    public static boolean activateVip(ServerPlayer player, long priceCoin, long priceTear, int durationTicks)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return false;
        }

        AccountData data = data(server);

        if (priceCoin > 0L && !data.takeBalance(player.getUUID(), CurrencyType.COIN, priceCoin))
        {
            return false;
        }

        if (priceTear > 0L && !data.takeBalance(player.getUUID(), CurrencyType.TEAR, priceTear))
        {
            if (priceCoin > 0L)
            {
                data.addBalance(player.getUUID(), CurrencyType.COIN, priceCoin);
            }

            return false;
        }

        data.addToCounter(CurrencyType.COIN, priceCoin);
        data.addToCounter(CurrencyType.TEAR, priceTear);

        long now = server.getTickCount();
        long base = Math.max(now, data.vipUntil(player.getUUID()));
        data.setVipUntil(player.getUUID(), base + Math.max(1, durationTicks));
        return true;
    }

    public static Item itemOf(CurrencyType type)
    {
        return type == CurrencyType.COIN ? ModItems.GAMBLERS_COIN.get() : ModItems.DEVILS_TEARS.get();
    }

    public static int countItems(ServerPlayer player, CurrencyType type)
    {
        Item item = itemOf(type);
        Inventory inventory = player.getInventory();
        int total = 0;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);

            if (!stack.isEmpty() && stack.getItem() == item)
            {
                total += stack.getCount();
            }
        }

        return total;
    }

    /**
     * Counts one arbitrary item by registry name, used by the recycler and the exchange.
     */
    public static int countItemId(ServerPlayer player, String itemId)
    {
        ResourceLocation key = ResourceLocation.tryParse(itemId);

        if (key == null)
        {
            return 0;
        }

        Item item = ForgeRegistries.ITEMS.getValue(key);

        if (item == null)
        {
            return 0;
        }

        Inventory inventory = player.getInventory();
        int total = 0;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);

            if (!stack.isEmpty() && stack.getItem() == item)
            {
                total += stack.getCount();
            }
        }

        return total;
    }

    /**
     * Removes one arbitrary item by registry name.
     */
    public static void removeItemId(ServerPlayer player, String itemId, int amount)
    {
        ResourceLocation key = ResourceLocation.tryParse(itemId);

        if (key == null)
        {
            return;
        }

        Item item = ForgeRegistries.ITEMS.getValue(key);

        if (item == null)
        {
            return;
        }

        Inventory inventory = player.getInventory();
        int remaining = amount;

        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++)
        {
            ItemStack stack = inventory.getItem(slot);

            if (stack.isEmpty() || stack.getItem() != item)
            {
                continue;
            }

            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
        }
    }

    public static void removeItems(ServerPlayer player, CurrencyType type, int amount)
    {
        Item item = itemOf(type);
        Inventory inventory = player.getInventory();
        int remaining = amount;

        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++)
        {
            ItemStack stack = inventory.getItem(slot);

            if (stack.isEmpty() || stack.getItem() != item)
            {
                continue;
            }

            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
        }
    }

    /**
     * Manual capacity check, because the vanilla add call only reports whether anything fitted at all.
     * Damaged and enchanted items take a whole slot each, so they are counted per slot.
     */
    public static boolean canFullyFit(Inventory inventory, ItemStack prototype)
    {
        int remaining = prototype.getCount();
        int maxStack = prototype.getMaxStackSize();
        boolean singlePerSlot = prototype.isDamaged() || prototype.isEnchanted();

        for (int slot = 0; slot < inventory.items.size() && remaining > 0; slot++)
        {
            ItemStack stack = inventory.items.get(slot);

            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, prototype))
            {
                remaining -= Math.max(0, Math.min(maxStack, stack.getMaxStackSize()) - stack.getCount());
            }
        }

        if (remaining <= 0)
        {
            return true;
        }

        int freeSlots = 0;

        for (int slot = 0; slot < inventory.items.size(); slot++)
        {
            if (inventory.items.get(slot).isEmpty())
            {
                freeSlots++;
            }
        }

        if (inventory.offhand.get(0).isEmpty())
        {
            freeSlots++;
        }

        int perSlot = singlePerSlot ? 1 : maxStack;
        return remaining <= freeSlots * perSlot;
    }

    /**
     * Formats an amount for display, because the vanilla text component adds no thousands separator.
     */
    public static String format(long amount)
    {
        return String.format(java.util.Locale.ROOT, "%,d", Long.valueOf(Math.max(0L, amount)));
    }

    /**
     * Small helper for the empty stack constant, kept here so callers do not need the item registry.
     */
    public static ItemStack emptyStack()
    {
        return new ItemStack(Items.AIR, 0);
    }
}
