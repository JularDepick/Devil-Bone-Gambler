/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network;

import com.devilbonegambler.config.EconomyConfig;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.config.ShopOffer;
import com.devilbonegambler.data.AccountData;
import com.devilbonegambler.data.ExchangeData;
import com.devilbonegambler.draw.Offering;
import com.devilbonegambler.economy.CurrencyType;
import com.devilbonegambler.economy.EconomyService;
import com.devilbonegambler.economy.ExchangeService;
import com.devilbonegambler.network.packet.EconomyDataPacket;
import com.devilbonegambler.network.packet.EconomyRequestPacket;
import com.devilbonegambler.network.packet.EconomyResponsePacket;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Server side handling of every economy operation.
 * The client is never trusted: permissions, balances, inventory contents and config legality are all re-checked.
 */
public final class EconomyRequestHandler
{
    private static final String REASON_NO_PERMISSION = "no_permission";
    private static final String REASON_INVALID = "invalid_config";
    private static final String REASON_NOT_READY = "not_ready";

    private EconomyRequestHandler()
    {
    }

    public static void handle(ServerPlayer player, EconomyRequestPacket message)
    {
        if (player == null)
        {
            return;
        }

        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return;
        }

        if (RequestGuard.checkSequence(player, message.sequence()) != null)
        {
            respond(player, message.sequence(), false, "duplicate_request");
            return;
        }

        String failure;

        switch (message.operation())
        {
            case REQUEST_DATA:
                sendData(player);
                return;
            case BANK_DEPOSIT:
                failure = EconomyService.depositItems(player, message.currency(), (int) Math.min(Integer.MAX_VALUE,
                        message.amount())) ? null : "not_enough_currency";
                break;
            case BANK_WITHDRAW:
                failure = EconomyService.withdrawItems(player, message.currency(), (int) Math.min(Integer.MAX_VALUE,
                        message.amount())) ? null : "not_enough_currency";
                break;
            case BANK_TRANSFER:
                failure = transfer(server, player, message);
                break;
            case COUNTER_WITHDRAW:
                failure = counterWithdraw(server, player, message);
                break;
            case SHOP_BUY:
                failure = shopBuy(server, player, message);
                break;
            case EXCHANGE_PUBLISH_SELL:
                failure = ExchangeService.publishSell(player, player.getMainHandItem(), message.amount(),
                        message.amountB());
                break;
            case EXCHANGE_PUBLISH_BUY:
                failure = ExchangeService.publishBuy(player, message.textA(), message.count(), message.amount(),
                        message.amountB());
                break;
            case EXCHANGE_ACCEPT:
                failure = ExchangeService.accept(player, message.textA());
                break;
            case EXCHANGE_CANCEL:
                failure = ExchangeService.cancel(player, message.textA());
                break;
            case EXCHANGE_COLLECT:
                ExchangeService.deliver(player);
                failure = null;
                break;
            case EXCHANGE_VIP:
                failure = openVip(server, player);
                break;
            case RECYCLE:
                failure = recycle(server, player, message);
                break;
            default:
                failure = REASON_INVALID;
                break;
        }

        respond(player, message.sequence(), failure == null, failure);

        if (failure == null)
        {
            sendData(player);
        }
    }

    /**
     * Pushes the balances, the counters and the economy config to the client.
     */
    public static void sendData(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return;
        }

        AccountData accounts = AccountData.get(server);
        ExchangeData exchange = ExchangeData.get(server);
        boolean admin = player.hasPermissions(ModConstants.ADMIN_PERMISSION_LEVEL);

        ModNetwork.sendToPlayer(player, new EconomyDataPacket(
                accounts.balanceOf(player.getUUID(), CurrencyType.COIN),
                accounts.balanceOf(player.getUUID(), CurrencyType.TEAR),
                admin ? accounts.counterOf(CurrencyType.COIN) : 0L,
                admin ? accounts.counterOf(CurrencyType.TEAR) : 0L,
                accounts.vipUntil(player.getUUID()),
                exchange.pendingOf(player.getUUID()).size(),
                server.getTickCount(),
                ServerConfigManager.shopJson(),
                ServerConfigManager.economyJson()));
    }

    private static void respond(ServerPlayer player, int sequence, boolean success, String reasonKey)
    {
        ModNetwork.sendToPlayer(player, new EconomyResponsePacket(sequence, success,
                success ? "" : (reasonKey == null ? "unknown" : reasonKey)));
    }

    private static String transfer(MinecraftServer server, ServerPlayer player, EconomyRequestPacket message)
    {
        String name = message.textA();

        if (name == null || name.isEmpty() || name.equalsIgnoreCase(player.getGameProfile().getName()))
        {
            return "target_missing";
        }

        var target = server.getProfileCache() == null ? null : server.getProfileCache().get(name);

        if (target.isEmpty())
        {
            return "target_missing";
        }

        return EconomyService.transfer(server, player.getUUID(), target.get().getId(), message.currency(),
                message.amount()) ? null : "not_enough_currency";
    }

    private static String counterWithdraw(MinecraftServer server, ServerPlayer player, EconomyRequestPacket message)
    {
        if (!player.hasPermissions(ModConstants.ADMIN_PERMISSION_LEVEL))
        {
            return REASON_NO_PERMISSION;
        }

        AccountData accounts = AccountData.get(server);

        if (!accounts.takeFromCounter(message.currency(), message.amount()))
        {
            return "not_enough_currency";
        }

        accounts.addBalance(player.getUUID(), message.currency(), message.amount());
        return null;
    }

    private static String shopBuy(MinecraftServer server, ServerPlayer player, EconomyRequestPacket message)
    {
        var shop = ServerConfigManager.shop();

        if (shop == null)
        {
            return REASON_NOT_READY;
        }

        ShopOffer offer = shop.find(message.textA());

        if (offer == null || !offer.isEnabled())
        {
            return "offer_missing";
        }

        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(offer.itemOrEmpty()));

        if (item == null)
        {
            return REASON_INVALID;
        }

        ItemStack stack = new ItemStack(item, offer.countOrZero());

        if (!EconomyService.canFullyFit(player.getInventory(), stack))
        {
            return "no_space";
        }

        if (offer.priceCoinOrZero() > 0L
                && !EconomyService.payMixed(player, CurrencyType.COIN, offer.priceCoinOrZero(), message.amount()))
        {
            return "not_enough_currency";
        }

        if (offer.priceTearOrZero() > 0L
                && !EconomyService.payMixed(player, CurrencyType.TEAR, offer.priceTearOrZero(), message.amountB()))
        {
            return "not_enough_currency";
        }

        player.getInventory().add(stack);
        return null;
    }

    private static String openVip(MinecraftServer server, ServerPlayer player)
    {
        EconomyConfig config = ServerConfigManager.economy();

        if (config == null)
        {
            return REASON_NOT_READY;
        }

        return EconomyService.activateVip(player, config.vipPriceCoinOr(), config.vipPriceTearOr(),
                config.vipDurationTicksOr()) ? null : "not_enough_currency";
    }

    /**
     * Recycles the listed items. The payout is the item value times the recycle rate, split into the two currencies.
     * A system payout never touches the asset counter.
     */
    private static String recycle(MinecraftServer server, ServerPlayer player, EconomyRequestPacket message)
    {
        EconomyConfig config = ServerConfigManager.economy();
        var values = ServerConfigManager.itemValues();

        if (config == null || values == null)
        {
            return REASON_NOT_READY;
        }

        List<Offering> parsed = parseRecycleList(message.textA());

        if (parsed.isEmpty())
        {
            return REASON_INVALID;
        }

        long baseValue = 0L;

        for (Offering entry : parsed)
        {
            if (entry.count() <= 0 || entry.count() > ModConstants.MAX_RECYCLE_SLOTS)
            {
                return REASON_INVALID;
            }

            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(entry.itemId()));

            if (item == null || config.recycleExcludedItemsOrEmpty().contains(entry.itemId()))
            {
                return REASON_INVALID;
            }

            if (EconomyService.countItemId(player, entry.itemId()) < entry.count())
            {
                return "not_enough_items";
            }

            baseValue += (long) values.itemValueOf(entry.itemId()) * (long) entry.count();
        }

        long[] payout = config.recyclePayout(baseValue);

        if (payout[0] <= 0L && payout[1] <= 0L)
        {
            return "value_too_low";
        }

        AccountData accounts = AccountData.get(server);

        for (Offering entry : parsed)
        {
            EconomyService.removeItemId(player, entry.itemId(), entry.count());
        }

        accounts.addBalance(player.getUUID(), CurrencyType.COIN, payout[0]);
        accounts.addBalance(player.getUUID(), CurrencyType.TEAR, payout[1]);
        return null;
    }

    /**
     * Parses the compact recycle list sent by the client: registry names and amounts separated by semicolons.
     */
    private static List<Offering> parseRecycleList(String raw)
    {
        List<Offering> result = new ArrayList<Offering>();

        if (raw == null || raw.isEmpty())
        {
            return result;
        }

        for (String part : raw.split(";"))
        {
            int split = part.lastIndexOf(':');

            if (split <= 0 || split >= part.length() - 1)
            {
                continue;
            }

            String itemId = part.substring(0, split);
            String amount = part.substring(split + 1);

            try
            {
                result.add(new Offering(itemId, Integer.parseInt(amount)));
            }
            catch (NumberFormatException exception)
            {
                continue;
            }

            if (result.size() >= ModConstants.MAX_RECYCLE_SLOTS)
            {
                break;
            }
        }

        return result;
    }
}
