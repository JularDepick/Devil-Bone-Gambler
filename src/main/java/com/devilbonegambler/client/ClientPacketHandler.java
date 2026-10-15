/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.client.gui.AdminConfigScreen;
import com.devilbonegambler.client.gui.AltarScreen;
import com.devilbonegambler.network.packet.AdminPanelDataPacket;
import com.devilbonegambler.network.packet.AltarStatusPacket;
import com.devilbonegambler.network.packet.ConfigSyncPacket;
import com.devilbonegambler.network.packet.ConfigWriteResponsePacket;
import com.devilbonegambler.network.packet.DrawResultPacket;
import com.devilbonegambler.network.packet.EconomyDataPacket;
import com.devilbonegambler.network.packet.EconomyResponsePacket;
import com.devilbonegambler.network.packet.MobListPacket;
import com.devilbonegambler.network.packet.OpenValueTablePacket;
import com.devilbonegambler.network.packet.PointDisplayPacket;
import net.minecraft.client.Minecraft;

/**
 * Client side packet dispatch, reached only from the DistExecutor branch of a packet handler.
 */
public final class ClientPacketHandler
{
    private ClientPacketHandler()
    {
    }

    public static void handleDrawResult(DrawResultPacket message)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof AltarScreen altar)
        {
            altar.onDrawResult(message);
        }
    }

    public static void handleAdminPanelData(AdminPanelDataPacket message)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof AdminConfigScreen panel)
        {
            panel.applyServerData(message.prizePoolJson(), message.itemValuesJson(), message.shopJson());
            return;
        }

        ClientActions.openAdminPanel(message.prizePoolJson(), message.itemValuesJson(), message.shopJson());
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

    public static void handleEconomyData(EconomyDataPacket message)
    {
        ClientActions.setEconomyData(message.coin(), message.tear(), message.counterCoin(), message.counterTear(),
                message.vipUntil(), message.pendingCount(), message.tickCount(), message.shopJson(),
                message.economyJson());
    }

    public static void handleEconomyResponse(EconomyResponsePacket message)
    {
        ClientActions.onEconomyResponse(message.sequence(), message.success(), message.reasonKey());
    }
}
