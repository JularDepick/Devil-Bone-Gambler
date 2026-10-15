/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.client.gui.AdminConfigScreen;
import com.devilbonegambler.client.gui.AltarScreen;
import com.devilbonegambler.client.gui.DealerBetScreen;
import com.devilbonegambler.client.gui.DealerScreen;
import com.devilbonegambler.network.packet.AdminPanelDataPacket;
import com.devilbonegambler.network.packet.DealerPanelPacket;
import com.devilbonegambler.network.packet.AltarStatusPacket;
import com.devilbonegambler.network.packet.ConfigSyncPacket;
import com.devilbonegambler.network.packet.ConfigWriteResponsePacket;
import com.devilbonegambler.network.packet.DrawResultPacket;
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
     * one only refreshes it. The bet box screen keeps its own copy of the same data.
     */
    public static void handleDealerPanel(DealerPanelPacket message)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof DealerBetScreen betScreen)
        {
            betScreen.applyPanel(message);
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
            panel.applyServerData(message.prizePoolJson(), message.itemValuesJson());
            return;
        }

        ClientActions.openAdminPanel(message.prizePoolJson(), message.itemValuesJson());
    }

    public static void handleConfigWriteResponse(ConfigWriteResponsePacket message)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof AdminConfigScreen panel)
        {
            panel.onWriteResponse(message);
        }
    }

    public static void handleConfigSync(ConfigSyncPacket message)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof AdminConfigScreen panel)
        {
            panel.onConfigSync(message);
        }
    }

    public static void handleOpenValueTable(OpenValueTablePacket message)
    {
        ClientActions.openValueTableScreen(message.title(), message.author(), message.pages());
    }

    public static void handlePointDisplay(PointDisplayPacket message)
    {
        ClientActions.showPointDisplay(message.point(), message.red(), message.overflow());
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
