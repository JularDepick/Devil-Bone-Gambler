/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.altar.AltarMobScan;
import com.devilbonegambler.altar.AltarService;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.menu.AltarMenu;
import com.devilbonegambler.dealer.DealerRewardService;
import com.devilbonegambler.dealer.DealerWagerService;
import com.devilbonegambler.draw.DrawEngine;
import com.devilbonegambler.draw.DrawResult;
import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.draw.OfferingBundle;
import com.devilbonegambler.item.OfferingValueTableItem;
import com.devilbonegambler.network.packet.AdminEditPacket;
import com.devilbonegambler.network.packet.AdminPanelDataPacket;
import com.devilbonegambler.network.packet.AdminPanelRequestPacket;
import com.devilbonegambler.network.packet.AltarOpenRequestPacket;
import com.devilbonegambler.network.packet.ConfigSyncPacket;
import com.devilbonegambler.network.packet.ConfigWriteRequestPacket;
import com.devilbonegambler.network.packet.ConfigWriteResponsePacket;
import com.devilbonegambler.network.packet.DealerBetOpenRequestPacket;
import com.devilbonegambler.network.packet.DealerInteractPacket;
import com.devilbonegambler.network.packet.DevilsDiceRequestPacket;
import com.devilbonegambler.network.packet.DrawResultPacket;
import com.devilbonegambler.network.packet.GamblersDiceRequestPacket;
import com.devilbonegambler.network.packet.MobListRequestPacket;
import com.devilbonegambler.network.packet.ValueTableRefreshRequestPacket;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.ValueTableBook;
import com.devilbonegambler.wager.WagerService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

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
        OfferingBundle bundle = message.bundle();

        /*
         * The altar container is the authority of the item part: only what really sits in it is offered, and a refused
         * sacrifice is handed straight back instead of being swallowed.
         */
        if (player != null && player.containerMenu instanceof AltarMenu altarMenu)
        {
            bundle = altarMenu.takeOfferings(bundle);
        }

        DrawResult result = handleDraw(player, message.sequence(), message.playerName(), message.itemId(), bundle,
                false);

        if (!result.isAccepted())
        {
            DrawEngine.returnTaken(player, bundle);
        }
    }

    /**
     * Opens the altar container. The offering grid lives in a server side container and the altar range is re-checked
     * here, so the screen is only ever handed out by the server.
     */
    public static void handleAltarOpenRequest(ServerPlayer player, AltarOpenRequestPacket message)
    {
        if (player == null)
        {
            return;
        }

        if (RequestGuard.checkSequence(player, message.sequence()) != null)
        {
            return;
        }

        if (AltarService.governingValue(player) < 0L)
        {
            NotifySender.sendFailure(player, "no_altar");
            return;
        }

        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (windowId, inventory, owner) -> new AltarMenu(windowId, inventory,
                        new SimpleContainer(ModConstants.ALTAR_SLOTS)),
                Component.translatable("gui." + ModConstants.MOD_ID + ".altar.title")));
        AltarMobScan.send(player);
    }

    /**
     * Dealer panel intents: open or join, create, start and leave. Every intent is validated against the server
     * owned wager, the client never decides who may join or start.
     */
    public static void handleDealerInteract(ServerPlayer player, DealerInteractPacket message)
    {
        if (player == null)
        {
            return;
        }

        if (message.action() == DealerInteractPacket.ACTION_OPEN)
        {
            DealerWagerService.open(player, message.dealerEntityId());
        }
        else if (message.action() == DealerInteractPacket.ACTION_CREATE)
        {
            DealerWagerService.create(player, message.dealerEntityId());
        }
        else if (message.action() == DealerInteractPacket.ACTION_START)
        {
            DealerWagerService.start(player, message.dealerEntityId());
        }
        else if (message.action() == DealerInteractPacket.ACTION_LEAVE)
        {
            DealerWagerService.leave(player, message.dealerEntityId());
        }
    }

    /**
     * The bet box of one member: the server checks the session membership and then opens the container menu itself, so
     * a client can never ask for a box it does not own.
     */
    public static void handleDealerBetOpenRequest(ServerPlayer player, DealerBetOpenRequestPacket message)
    {
        if (player == null)
        {
            return;
        }

        if (RequestGuard.checkSequence(player, message.sequence()) != null)
        {
            return;
        }

        DealerWagerService.openBetMenu(player, message.dealerEntityId(), message.targetId());
    }

    /**
     * The creature list of the altar screen: the server scans its own range and answers the asking player only.
     */
    public static void handleMobListRequest(ServerPlayer player, MobListRequestPacket message)
    {
        if (player == null)
        {
            return;
        }

        if (RequestGuard.checkSequence(player, message.sequence()) != null)
        {
            return;
        }

        AltarMobScan.send(player);
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

        if (!isManager(player))
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

        sendPanel(player);
    }

    /**
     * One change of the admin list. Only the creator may ask for it, and the creator can never be removed.
     */
    public static void handleAdminEdit(ServerPlayer player, AdminEditPacket message)
    {
        if (player == null || !ServerConfigManager.canManageAdmins(player.getGameProfile().getName()))
        {
            NotifySender.sendFailure(player, "no_permission");
            return;
        }

        if (RequestGuard.checkSequence(player, message.sequence()) != null)
        {
            return;
        }

        String name = message.name() == null ? "" : message.name().trim();
        boolean changed;

        if (message.action() == AdminEditPacket.ACTION_ADD)
        {
            changed = ServerConfigManager.addAdmin(name);
        }
        else if (message.action() == AdminEditPacket.ACTION_REMOVE)
        {
            changed = ServerConfigManager.removeAdmin(name);
        }
        else
        {
            changed = false;
        }

        if (!changed)
        {
            NotifySender.sendFailure(player, "admin_edit_rejected");
        }

        /* The panel is refreshed either way, so the list always shows the stored state. */
        sendPanel(player);
    }

    private static boolean isManager(ServerPlayer player)
    {
        return player != null && ServerConfigManager.isManager(player.getGameProfile().getName());
    }

    /**
     * Pushes the panel payload with the management access of the requester, shared by the shortcut and the command.
     */
    public static void sendPanel(ServerPlayer player)
    {
        ServerPlayer target = player;

        if (target == null)
        {
            return;
        }

        ModNetwork.sendToPlayer(target, new AdminPanelDataPacket(
                ServerConfigManager.prizePoolJson(), ServerConfigManager.itemValuesJson(),
                ServerConfigManager.access() == null ? "" : ServerConfigManager.access().creatorOrEmpty(),
                ServerConfigManager.access() == null ? new ArrayList<String>()
                        : new ArrayList<String>(ServerConfigManager.access().adminsOrEmpty()),
                ServerConfigManager.canManageAdmins(target.getGameProfile().getName())));
    }

    public static void handleConfigWrite(ServerPlayer player, ConfigWriteRequestPacket message)
    {
        if (player == null)
        {
            return;
        }

        if (!isManager(player))
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

    private static DrawResult handleDraw(ServerPlayer player, int sequence, String playerName, String itemId,
            OfferingBundle bundle, boolean wagerAllowed)
    {
        if (player == null)
        {
            return DrawResult.failure("identity_mismatch");
        }

        if (!player.getGameProfile().getName().equals(playerName))
        {
            DrawResult failure = DrawResult.failure("identity_mismatch");
            respondDraw(player, sequence, itemId, failure);
            return failure;
        }

        String guard = RequestGuard.checkDraw(player, sequence);

        if (guard != null)
        {
            DrawResult failure = DrawResult.failure(guard);
            respondDraw(player, sequence, itemId, failure);
            return failure;
        }

        /*
         * The gambler's dice is locked for the joined members of a running wager, the initiator included.
         * The check sits before the sneak branch so that both the single draw and the wager start are refused.
         */
        if (wagerAllowed && WagerService.isGamblersDiceLocked(player))
        {
            DrawResult failure = DrawResult.failure("wager_locked");
            respondDraw(player, sequence, itemId, failure);
            return failure;
        }

        /*
         * One settlement at a time: while the previous draw still waits for its result every dice use is refused
         * before anything is taken, so a refused use never costs a dice.
         */
        if (RequestGuard.isSettling(player))
        {
            DrawResult failure = DrawResult.failure("settling");
            respondDraw(player, sequence, itemId, failure);
            return failure;
        }

        if (wagerAllowed && player.isShiftKeyDown())
        {
            return handleWager(player, sequence, itemId);
        }

        DrawResult result = DrawEngine.perform(player, itemId, bundle, sequence);
        respondDraw(player, sequence, itemId, result);
        return result;
    }

    /**
     * A sneaking right click starts a wager instead of a single draw.
     * The dice is taken before the wager is created and handed back when the creation fails, so a wager never starts
     * unpaid and a failed start never costs the player a dice.
     */
    private static DrawResult handleWager(ServerPlayer player, int sequence, String itemId)
    {
        if (!WagerService.hasOtherPlayersNearby(player))
        {
            DrawResult failure = DrawResult.failure("no_player_nearby");
            respondDraw(player, sequence, itemId, failure);
            return failure;
        }

        if (!DrawEngine.hasDice(player, itemId) || !DrawEngine.consumeDice(player, itemId))
        {
            DrawResult failure = DrawResult.failure("item_mismatch");
            respondDraw(player, sequence, itemId, failure);
            return failure;
        }

        if (!WagerService.startWager(player))
        {
            DrawEngine.refundOneDice(player, itemId);
            DrawResult failure = DrawResult.failure("wager_failed");
            respondDraw(player, sequence, itemId, failure);
            return failure;
        }

        DealerRewardService.recordDiceUse(player);
        return DrawResult.accepted();
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
            if (isManager(target))
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
