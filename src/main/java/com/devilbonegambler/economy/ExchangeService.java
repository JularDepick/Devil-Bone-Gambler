/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.economy;

import com.devilbonegambler.config.EconomyConfig;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.data.AccountData;
import com.devilbonegambler.data.ExchangeData;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Player to player trading.
 * A sell order escrows the items, a buy order escrows the price plus the fee. Settlement is immediate,
 * the buyer collects the goods from any exchange unless VIP delivers them straight into the inventory.
 * Currency settlement only ever uses account balances.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ExchangeService
{
    private static int retryCounter;

    private ExchangeService()
    {
    }

    /**
     * Publishes a sell order. The items leave the seller inventory immediately and are escrowed.
     */
    public static String publishSell(ServerPlayer player, ItemStack stack, long priceCoin, long priceTear)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return "not_ready";
        }

        if (stack == null || stack.isEmpty())
        {
            return "invalid_config";
        }

        if (priceCoin < 0L || priceTear < 0L || (priceCoin == 0L && priceTear == 0L))
        {
            return "invalid_config";
        }

        ExchangeData data = ExchangeData.get(server);

        if (data.orderCountOf(player.getUUID()) >= ModConstants.MAX_ORDERS_PER_PLAYER)
        {
            return "order_limit";
        }

        EconomyConfig config = ServerConfigManager.economy();

        if (config == null)
        {
            return "not_ready";
        }

        long feeCoin = ExchangeOrder.feeOf(priceCoin);
        long feeTear = ExchangeOrder.feeOf(priceTear);
        long expiry = (long) server.getTickCount() + config.exchangeOrderTimeoutTicksOr();

        ExchangeOrder order = new ExchangeOrder(UUID.randomUUID().toString(), player.getUUID(), true,
                stack.copy(), priceCoin, priceTear, feeCoin, feeTear, expiry);
        data.putOrder(order);
        return null;
    }

    /**
     * Publishes a buy order. The price and the fee are escrowed from the buyer balance at once.
     */
    public static String publishBuy(ServerPlayer player, String itemId, int count, long priceCoin, long priceTear)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return "not_ready";
        }

        if (itemId == null || itemId.isEmpty() || count <= 0 || count > 64)
        {
            return "invalid_config";
        }

        if (priceCoin < 0L || priceTear < 0L || (priceCoin == 0L && priceTear == 0L))
        {
            return "invalid_config";
        }

        ExchangeData data = ExchangeData.get(server);

        if (data.orderCountOf(player.getUUID()) >= ModConstants.MAX_ORDERS_PER_PLAYER)
        {
            return "order_limit";
        }

        EconomyConfig config = ServerConfigManager.economy();

        if (config == null)
        {
            return "not_ready";
        }

        long feeCoin = ExchangeOrder.feeOf(priceCoin);
        long feeTear = ExchangeOrder.feeOf(priceTear);
        AccountData accounts = AccountData.get(server);

        if (!accounts.takeBalance(player.getUUID(), CurrencyType.COIN, priceCoin + feeCoin))
        {
            return "not_enough_currency";
        }

        if (!accounts.takeBalance(player.getUUID(), CurrencyType.TEAR, priceTear + feeTear))
        {
            accounts.addBalance(player.getUUID(), CurrencyType.COIN, priceCoin + feeCoin);
            return "not_enough_currency";
        }

        long expiry = (long) server.getTickCount() + config.exchangeOrderTimeoutTicksOr();
        ExchangeOrder order = new ExchangeOrder(UUID.randomUUID().toString(), player.getUUID(), false,
                new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                        net.minecraft.resources.ResourceLocation.tryParse(itemId)), count),
                priceCoin, priceTear, feeCoin, feeTear, expiry);
        data.putOrder(order);
        return null;
    }

    /**
     * Accepts an order. Settlement is immediate: money moves, the fee is kept by the system.
     */
    public static String accept(ServerPlayer player, String orderId)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return "not_ready";
        }

        ExchangeData data = ExchangeData.get(server);
        ExchangeOrder order = data.orders().get(orderId);

        if (order == null)
        {
            return "order_missing";
        }

        if (order.owner().equals(player.getUUID()))
        {
            return "self_trade";
        }

        AccountData accounts = AccountData.get(server);

        if (order.isSell())
        {
            if (!accounts.takeBalance(player.getUUID(), CurrencyType.COIN, order.priceCoin()))
            {
                return "not_enough_currency";
            }

            if (!accounts.takeBalance(player.getUUID(), CurrencyType.TEAR, order.priceTear()))
            {
                accounts.addBalance(player.getUUID(), CurrencyType.COIN, order.priceCoin());
                return "not_enough_currency";
            }

            accounts.addBalance(order.owner(), CurrencyType.COIN, order.priceCoin() - order.feeCoin());
            accounts.addBalance(order.owner(), CurrencyType.TEAR, order.priceTear() - order.feeTear());
            accounts.addToCounter(CurrencyType.COIN, order.feeCoin());
            accounts.addToCounter(CurrencyType.TEAR, order.feeTear());
            data.addPending(player.getUUID(), order.stack());
        }
        else
        {
            if (EconomyService.countItems(player, CurrencyType.COIN) < order.stack().getCount()
                    && !order.stack().isEmpty())
            {
                return "not_enough_items";
            }

            accounts.addBalance(order.owner(), CurrencyType.COIN, order.priceCoin());
            accounts.addBalance(order.owner(), CurrencyType.TEAR, order.priceTear());
            accounts.addToCounter(CurrencyType.COIN, order.feeCoin());
            accounts.addToCounter(CurrencyType.TEAR, order.feeTear());
            data.addPending(order.owner(), order.stack());
        }

        data.removeOrder(orderId);
        deliver(player);
        return null;
    }

    /**
     * Cancels an own order. Escrowed money goes back to the balance, escrowed items to the collection.
     */
    public static String cancel(ServerPlayer player, String orderId)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return "not_ready";
        }

        ExchangeData data = ExchangeData.get(server);
        ExchangeOrder order = data.orders().get(orderId);

        if (order == null)
        {
            return "order_missing";
        }

        if (!order.owner().equals(player.getUUID()))
        {
            return "order_missing";
        }

        refund(server, order);
        data.removeOrder(orderId);
        return null;
    }

    /**
     * Moves every collectable item into the inventory when there is room, otherwise leaves it queued.
     */
    public static int deliver(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return 0;
        }

        ExchangeData data = ExchangeData.get(server);
        List<ItemStack> pending = data.pendingOf(player.getUUID());

        if (pending.isEmpty())
        {
            return 0;
        }

        int delivered = 0;
        Iterator<ItemStack> iterator = pending.iterator();

        while (iterator.hasNext())
        {
            ItemStack stack = iterator.next();

            if (!EconomyService.canFullyFit(player.getInventory(), stack))
            {
                continue;
            }

            player.getInventory().add(stack.copy());
            iterator.remove();
            delivered++;
        }

        if (delivered > 0)
        {
            if (pending.isEmpty())
            {
                data.clearPending(player.getUUID());
            }
            else
            {
                data.setDirty();
            }
        }

        return delivered;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        MinecraftServer server = event.getServer();
        ExchangeData data = ExchangeData.get(server);
        long now = server.getTickCount();
        List<String> expired = new ArrayList<String>();

        for (ExchangeOrder order : data.orders().values())
        {
            if (order.expiryTick() <= now)
            {
                expired.add(order.id());
            }
        }

        for (String id : expired)
        {
            ExchangeOrder order = data.orders().get(id);

            if (order == null)
            {
                continue;
            }

            refund(server, order);
            data.removeOrder(id);
        }

        retryCounter++;

        if (retryCounter < ModConstants.VIP_RETRY_TICKS)
        {
            return;
        }

        retryCounter = 0;
        retryVipDeliveries(server, data);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        retryCounter = 0;
    }

    /**
     * Money always returns to the balance, items always wait in the collection.
     */
    private static void refund(MinecraftServer server, ExchangeOrder order)
    {
        AccountData accounts = AccountData.get(server);
        ExchangeData data = ExchangeData.get(server);

        if (order.isSell())
        {
            data.addPending(order.owner(), order.stack());
            return;
        }

        accounts.addBalance(order.owner(), CurrencyType.COIN, order.priceCoin() + order.feeCoin());
        accounts.addBalance(order.owner(), CurrencyType.TEAR, order.priceTear() + order.feeTear());
    }

    /**
     * VIP holders get their goods pushed into the inventory, with one attempt per minute and a chat line each time.
     */
    private static void retryVipDeliveries(MinecraftServer server, ExchangeData data)
    {
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (data.pendingOf(player.getUUID()).isEmpty())
            {
                continue;
            }

            if (!EconomyService.isVip(player))
            {
                continue;
            }

            int delivered = deliver(player);

            if (delivered > 0)
            {
                player.sendSystemMessage(Component.translatable("gui." + ModConstants.MOD_ID + ".exchange.vip_delivered",
                        Integer.valueOf(delivered)));
            }
            else
            {
                player.sendSystemMessage(Component.translatable("gui." + ModConstants.MOD_ID + ".exchange.vip_no_space"));
            }
        }
    }
}
