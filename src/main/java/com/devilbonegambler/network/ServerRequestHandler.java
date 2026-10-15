/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.draw.DrawEngine;
import com.devilbonegambler.draw.DrawResult;
import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.draw.OfferingBundle;
import com.devilbonegambler.item.OfferingValueTableItem;
import com.devilbonegambler.network.packet.AdminPanelDataPacket;
import com.devilbonegambler.network.packet.AdminPanelRequestPacket;
import com.devilbonegambler.network.packet.ConfigSyncPacket;
import com.devilbonegambler.network.packet.ConfigWriteRequestPacket;
import com.devilbonegambler.network.packet.ConfigWriteResponsePacket;
import com.devilbonegambler.network.packet.DevilsDiceRequestPacket;
import com.devilbonegambler.network.packet.DrawResultPacket;
import com.devilbonegambler.network.packet.GamblersDiceRequestPacket;
import com.devilbonegambler.network.packet.ValueTableRefreshRequestPacket;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.ValueTableBook;
import com.devilbonegambler.wager.WagerService;
import java.util.Collections;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Handles every client to server request on the server thread.
 * The client is never trusted: identity, item ownership and permissions are re-checked here.
 */
public final class ServerRequestHandler
{
    private ServerRequestHandler()
    {
    }

    public static void handleGamblersDice(ServerPlayer player, GamblersDiceRequestPacket message)
    {
        handleDraw(player, message.sequence(), message.playerName(), message.itemId(), OfferingBundle.empty(), true);
    }

    public static void handleDevilsDice(ServerPlayer player, DevilsDiceRequestPacket message)
    {
        handleDraw(player, message.sequence(), message.playerName(), message.itemId(), message.bundle(), false);
    }

    public static void handleValueTableRefresh(ServerPlayer player, ValueTableRefreshRequestPacket message)
    {
        if (player == null)
        {
            return;
        }

        if (RequestGuard.checkSequence(player, message.sequence()) != null)
        {
            return;
        }

        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof OfferingValueTableItem))
        {
            stack = player.getOffhandItem();
        }

        if (stack.getItem() instanceof OfferingValueTableItem)
        {
            ValueTableBook.refreshSnapshot(stack, player);
        }
    }

    public static void handleAdminPanelRequest(ServerPlayer player, AdminPanelRequestPacket message)
    {
        if (player == null)
        {
            return;
        }

        if (!player.hasPermissions(ModConstants.ADMIN_PERMISSION_LEVEL))
        {
            NotifySender.sendFailure(player, "no_permission");
            return;
        }

        if (RequestGuard.checkSequence(player, message.sequence()) != null)
        {
            return;
        }

        if (!ServerConfigManager.isReady())
        {
            NotifySender.sendFailure(player, "not_ready");
            return;
        }

        ModNetwork.sendToPlayer(player, new AdminPanelDataPacket(
                ServerConfigManager.prizePoolJson(), ServerConfigManager.itemValuesJson()));
    }

    public static void handleConfigWrite(ServerPlayer player, ConfigWriteRequestPacket message)
    {
        if (player == null)
        {
            return;
        }

        if (!player.hasPermissions(ModConstants.ADMIN_PERMISSION_LEVEL))
        {
            ModNetwork.sendToPlayer(player, new ConfigWriteResponsePacket(
                    message.sequence(), message.domain(), 0, false, "no_permission"));
            return;
        }

        if (RequestGuard.checkSequence(player, message.sequence()) != null)
        {
            ModNetwork.sendToPlayer(player, new ConfigWriteResponsePacket(
                    message.sequence(), message.domain(), 0, false, "duplicate_request"));
            return;
        }

        String failure;
        int revision;

        if (ModConstants.CONFIG_DOMAIN_PRIZE_POOL.equals(message.domain()))
        {
            failure = ServerConfigManager.applyPrizePoolWrite(message.revision(), message.payload());
            revision = ServerConfigManager.prizePool() == null ? 0 : ServerConfigManager.prizePool().revisionOrZero();
        }
        else if (ModConstants.CONFIG_DOMAIN_ITEM_VALUES.equals(message.domain()))
        {
            failure = ServerConfigManager.applyItemValuesWrite(message.revision(), message.payload());
            revision = ServerConfigManager.itemValues() == null ? 0 : ServerConfigManager.itemValues().revisionOrZero();
        }
        else
        {
            failure = "invalid_config";
            revision = 0;
        }

        ModNetwork.sendToPlayer(player, new ConfigWriteResponsePacket(message.sequence(), message.domain(), revision,
                failure == null, failure == null ? "" : failure));

        if (failure == null)
        {
            broadcast(player, message.domain());
        }
        else if ("revision_conflict".equals(failure))
        {
            sendLatest(player, message.domain());
        }
    }

    private static void handleDraw(ServerPlayer player, int sequence, String playerName, String itemId,
            OfferingBundle bundle, boolean wagerAllowed)
    {
        if (player == null)
        {
            return;
        }

        if (!player.getGameProfile().getName().equals(playerName))
        {
            respondDraw(player, sequence, itemId, DrawResult.failure("identity_mismatch"));
            return;
        }

        String guard = RequestGuard.checkDraw(player, sequence);

        if (guard != null)
        {
            respondDraw(player, sequence, itemId, DrawResult.failure(guard));
            return;
        }

        /*
         * The gambler's dice is locked for the joined members of a running wager, the initiator included.
         * The check sits before the sneak branch so that both the single draw and the wager start are refused.
         */
        if (wagerAllowed && WagerService.isGamblersDiceLocked(player))
        {
            respondDraw(player, sequence, itemId, DrawResult.failure("wager_locked"));
            return;
        }

        if (wagerAllowed && player.isShiftKeyDown())
        {
            handleWager(player, sequence, itemId);
            return;
        }

        respondDraw(player, sequence, itemId, DrawEngine.perform(player, itemId, bundle, sequence));
    }

    /**
     * A sneaking right click starts a wager instead of a single draw.
     * The dice is consumed once the wager area is created, and the outcome is reported by the wager itself.
     */
    private static void handleWager(ServerPlayer player, int sequence, String itemId)
    {
        if (!WagerService.hasOtherPlayersNearby(player))
        {
            respondDraw(player, sequence, itemId, DrawResult.failure("no_player_nearby"));
            return;
        }

        if (!WagerService.startWager(player))
        {
            respondDraw(player, sequence, itemId, DrawResult.failure("wager_failed"));
            return;
        }

        if (!DrawEngine.consumeDice(player, itemId))
        {
            DevilBoneGambler.LOGGER.error("The wager started but the dice could not be consumed, player={}",
                    player.getGameProfile().getName());
        }
    }

    private static void respondDraw(ServerPlayer player, int sequence, String itemId, DrawResult result)
    {
        if (result.isAccepted())
        {
            return;
        }

        if (!result.isSuccess())
        {
            NotifySender.sendFailure(player, result.reasonKey());
        }

        ModNetwork.sendToPlayer(player, new DrawResultPacket(sequence, result.isSuccess(), result.reasonKey(), itemId, 1));
    }

    /**
     * Every online player that holds the panel permission gets the new revision.
     */
    private static void broadcast(ServerPlayer source, String domain)
    {
        MinecraftServer server = source.getServer();

        if (server == null)
        {
            sendLatest(source, domain);
            return;
        }

        for (ServerPlayer target : server.getPlayerList().getPlayers())
        {
            if (target.hasPermissions(ModConstants.ADMIN_PERMISSION_LEVEL))
            {
                sendLatest(target, domain);
            }
        }
    }

    private static void sendLatest(ServerPlayer player, String domain)
    {
        if (!ServerConfigManager.isReady())
        {
            return;
        }

        if (ModConstants.CONFIG_DOMAIN_PRIZE_POOL.equals(domain))
        {
            ModNetwork.sendToPlayer(player, new ConfigSyncPacket(domain,
                    ServerConfigManager.prizePool().revisionOrZero(), ServerConfigManager.prizePoolJson()));
        }
        else if (ModConstants.CONFIG_DOMAIN_ITEM_VALUES.equals(domain))
        {
            ModNetwork.sendToPlayer(player, new ConfigSyncPacket(domain,
                    ServerConfigManager.itemValues().revisionOrZero(), ServerConfigManager.itemValuesJson()));
        }
    }
}
