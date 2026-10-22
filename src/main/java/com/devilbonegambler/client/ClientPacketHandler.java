/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.client.gui.AdminConfigScreen;
import com.devilbonegambler.client.gui.AltarScreen;
import com.devilbonegambler.client.gui.DealerScreen;
import com.devilbonegambler.network.packet.AdminPanelDataPacket;
import com.devilbonegambler.network.packet.DealerPanelPacket;
import com.devilbonegambler.network.packet.AltarStatusPacket;
import com.devilbonegambler.network.packet.ConfigPayloadPacket;
import com.devilbonegambler.network.packet.ConfigWriteResponsePacket;
import com.devilbonegambler.network.packet.DrawResultPacket;
import com.devilbonegambler.network.packet.BehaviorStatePacket;
import com.devilbonegambler.network.packet.LuckSyncPacket;
import com.devilbonegambler.network.packet.PlacedDiceUsePacket;
import com.devilbonegambler.network.packet.MobListPacket;
import com.devilbonegambler.network.packet.OpenValueTablePacket;
import com.devilbonegambler.network.packet.PointDisplayPacket;
import com.devilbonegambler.network.packet.ValueTableSyncPacket;
import com.devilbonegambler.util.ValueTableBook;
import com.devilbonegambler.util.ValueTableSnapshot;
import net.minecraft.client.Minecraft;

/**
 * Client side packet dispatch, reached only from the DistExecutor branch of a packet handler.
 */
public final class ClientPacketHandler
{
    private static ValueTableSnapshot valueTable;

    private ClientPacketHandler()
    {
    }

    /**
     * The newest value table snapshot pushed by the server, or null before the first sync.
     */
    public static ValueTableSnapshot valueTable()
    {
        return valueTable;
    }

    public static void handleBehaviorState(BehaviorStatePacket message)
    {
        ClientBehaviorState.set(message.triggered(), message.value());
    }

    public static void handlePlacedDiceUse(PlacedDiceUsePacket message)
    {
        if (message.mode() == PlacedDiceUsePacket.GAMBLERS_DRAW)
        {
            ClientActions.sendGamblersDiceRequest();
        }
        else if (message.mode() == PlacedDiceUsePacket.DEVILS_ALTAR)
        {
            ClientActions.requestAltarScreen();
        }
        else if (message.mode() == PlacedDiceUsePacket.DEVILS_DRAW)
        {
            ClientActions.sendDevilsDiceDraw();
        }
    }

    public static void handleLuckSync(LuckSyncPacket message)
    {
        ClientLuck.set(message.value());
    }

    public static void handleValueTableSync(ValueTableSyncPacket message)
    {
        ValueTableSnapshot snapshot = ValueTableBook.fromTag(message.snapshot());

        if (snapshot != null)
        {
            valueTable = snapshot;
        }
    }

    public static void handleDrawResult(DrawResultPacket message)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof AltarScreen altar)
        {
            altar.onDrawResult(message);
        }
    }

    /**
     * The dealer panel is opened by the server, so the first panel of a new wager opens the screen and every later
     * one only refreshes it. The bet box is a container menu and takes its contents from the slot synchronisation.
     */
    public static void handleDealerPanel(DealerPanelPacket message)
    {
        Minecraft minecraft = Minecraft.getInstance();

        /*
         * A session that ended closes the panel it opened: the client never keeps a screen whose wager is gone, so a
         * recruit that times out cannot leave a stale panel behind.
         */
        if (message.closeScreen())
        {
            if (minecraft.screen instanceof DealerScreen)
            {
                minecraft.setScreen(null);
            }

            return;
        }

        if (minecraft.screen instanceof DealerScreen dealerScreen)
        {
            dealerScreen.applyPanel(message);
            return;
        }

        if (message.openScreen())
        {
            ClientActions.openDealerScreen(message);
        }
    }

    public static void handleAdminPanelData(AdminPanelDataPacket message)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof AdminConfigScreen panel)
        {
            panel.applyAccess(message.creatorName(), message.admins(), message.canManageAdmins());
            return;
        }

        ClientConfigCache.onPanelData(message.creatorName(), message.admins(), message.canManageAdmins());
        ClientConfigCache.tryOpenPanel();
    }

    public static void handleConfigWriteResponse(ConfigWriteResponsePacket message)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof AdminConfigScreen panel)
        {
            panel.onWriteResponse(message);
        }
    }

    /**
     * One part of a config domain. The panel is fed once the last part arrives, and a panel that is still waiting to
     * open does so on the part that completes its second payload.
     */
    public static void handleConfigPayload(ConfigPayloadPacket message)
    {
        if (message.partCount() < 1 || message.partIndex() < 0 || message.partIndex() >= message.partCount())
        {
            return;
        }

        String payload = ClientConfigCache.accept(message.domain(), message.partIndex(), message.partCount(),
                message.chunk());

        if (payload == null)
        {
            return;
        }

        ClientConfigCache.store(message.domain(), payload);
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof AdminConfigScreen panel)
        {
            panel.onConfigSync(message.domain(), payload);
            return;
        }

        ClientConfigCache.tryOpenPanel();
    }

    public static void handleOpenValueTable(OpenValueTablePacket message)
    {
        ClientActions.openValueTableScreen();
    }

    public static void handlePointDisplay(PointDisplayPacket message)
    {
        ClientActions.showPointDisplay(message.point(), message.red(), message.devils());
    }

    public static void handleMobList(MobListPacket message)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof AltarScreen altar)
        {
            altar.onMobList(message.entries());
        }
    }

    public static void handleAltarStatus(AltarStatusPacket message)
    {
        ClientActions.setAltarStatus(message.hasAltar(), message.pos(), message.value(), message.radius(),
                message.inRange());
    }
}
