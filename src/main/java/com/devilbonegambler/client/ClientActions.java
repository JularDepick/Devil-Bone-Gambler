/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.client.gui.AdminConfigScreen;
import com.devilbonegambler.client.gui.DealerScreen;
import com.devilbonegambler.client.gui.ManualScreen;
import com.devilbonegambler.client.gui.ValueTableScreen;
import com.devilbonegambler.draw.OfferingBundle;
import com.devilbonegambler.manual.ManualKind;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.AdminEditPacket;
import com.devilbonegambler.network.packet.AdminPanelRequestPacket;
import com.devilbonegambler.network.packet.AltarOpenRequestPacket;
import com.devilbonegambler.network.packet.DealerBetOpenRequestPacket;
import com.devilbonegambler.network.packet.DealerInteractPacket;
import com.devilbonegambler.network.packet.DealerPanelPacket;
import com.devilbonegambler.network.packet.DevilsDiceRequestPacket;
import com.devilbonegambler.network.packet.GamblersDiceRequestPacket;
import com.devilbonegambler.network.packet.MobListRequestPacket;
import com.devilbonegambler.network.packet.ValueTableRefreshRequestPacket;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import java.util.UUID;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Client only entry points, always reached through DistExecutor from common code.
 */
public final class ClientActions
{
    private static int sequence = 0;
    private static boolean altarInRange = false;
    private static long altarValue = 0L;
    private static int altarRadius = ModConstants.ALTAR_BASE_RADIUS;
    private static net.minecraft.core.BlockPos altarPos = null;

    private ClientActions()
    {
    }

    public static synchronized int nextSequence()
    {
        sequence++;
        return sequence;
    }

    public static void sendGamblersDiceRequest()
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null)
        {
            return;
        }

        ModNetwork.sendToServer(new GamblersDiceRequestPacket(nextSequence(),
                minecraft.player.getGameProfile().getName(), itemId(ModItems.GAMBLERS_DICE.get())));
    }

    /**
     * Asks the server to open the altar container. The offering grid lives in a server side container and the altar
     * range is re-checked there, so the client only ever asks.
     */
    public static void requestAltarScreen()
    {
        ModNetwork.sendToServer(new AltarOpenRequestPacket(nextSequence()));
    }

    /**
     * Dealer panel intents. The panel data itself is always pushed by the server, the client only asks for an action.
     */
    public static void sendDealerInteract(int dealerEntityId, int action)
    {
        ModNetwork.sendToServer(new DealerInteractPacket(dealerEntityId, action));
    }

    /**
     * Asks the server for the bet box of one member of one dealer. The box is a container menu owned by the server, so
     * the client only requests it and the server opens it after it checked the membership and marked whether the
     * requester may change that box.
     */
    public static void sendDealerBetOpenRequest(int dealerEntityId, UUID targetId)
    {
        ModNetwork.sendToServer(new DealerBetOpenRequestPacket(nextSequence(), dealerEntityId, targetId));
    }

    public static void openDealerScreen(DealerPanelPacket panel)
    {
        Minecraft.getInstance().setScreen(new DealerScreen(panel));
    }

    /**
     * Cached altar status pushed by the server. Used only to choose the branch; the server re-checks the range.
     */
    public static void setAltarStatus(boolean hasAltar, net.minecraft.core.BlockPos pos, long value, int radius,
            boolean inRange)
    {
        altarInRange = hasAltar && inRange;
        altarValue = value;
        altarRadius = radius;
        altarPos = hasAltar ? pos : null;
    }

    public static boolean hasAltar()
    {
        return altarPos != null;
    }

    public static net.minecraft.core.BlockPos altarPosition()
    {
        return altarPos;
    }

    public static boolean isInAltarRange()
    {
        return altarInRange;
    }

    public static long altarValue()
    {
        return altarValue;
    }

    public static int altarRadius()
    {
        return altarRadius;
    }

    /**
     * Devil's dice right click: open the altar screen in range, otherwise draw with an empty offering.
     */
    public static void onDevilsDiceUse()
    {
        if (altarInRange)
        {
            requestAltarScreen();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null)
        {
            return;
        }

        ModNetwork.sendToServer(new DevilsDiceRequestPacket(nextSequence(),
                minecraft.player.getGameProfile().getName(), devilsDiceId(), OfferingBundle.empty()));
        /* The hint goes to the chat box, so nothing of the mod ever appears above the hotbar. */
        minecraft.gui.getChat().addMessage(
                Component.translatable("gui." + ModConstants.MOD_ID + ".altar.place_hint"));
    }

    /**
     * Refreshes the creature list of the open altar screen, so the list always describes the current range.
     */
    public static void sendMobListRequest()
    {
        ModNetwork.sendToServer(new MobListRequestPacket(nextSequence()));
    }

    public static void sendValueTableRefreshRequest()
    {
        ModNetwork.sendToServer(new ValueTableRefreshRequestPacket(nextSequence()));
    }

    public static void sendAdminPanelRequest()
    {
        ModNetwork.sendToServer(new AdminPanelRequestPacket(nextSequence()));
    }

    /**
     * One change of the admin list. Only the creator may ask for it and the server re-checks that.
     */
    public static void sendAdminEdit(boolean add, String name)
    {
        ModNetwork.sendToServer(new AdminEditPacket(nextSequence(),
                add ? AdminEditPacket.ACTION_ADD : AdminEditPacket.ACTION_REMOVE, name));
    }

    /**
     * Opens the searchable value table view. The data comes from the server pushed snapshot, so the rows are always
     * the live table and every row shows the localised item name.
     */
    /**
     * Opens the generic book screen on one manual. The whole content is client side, so no server round trip happens.
     */
    public static void openManualScreen(ManualKind kind)
    {
        Minecraft.getInstance().setScreen(new ManualScreen(kind));
    }

    public static void openValueTableScreen()
    {
        Minecraft.getInstance().setScreen(new ValueTableScreen(ClientPacketHandler.valueTable()));
    }

    public static void openAdminPanel(String prizePoolJson, String itemValuesJson, String creatorName,
            List<String> admins, boolean canManageAdmins)
    {
        Minecraft.getInstance().setScreen(new AdminConfigScreen(prizePoolJson, itemValuesJson, creatorName, admins,
                canManageAdmins));
    }

    /**
     * Shows the rolled point and colour in the chat box, before the result text arrives.
     */
    public static void showPointDisplay(int point, boolean red, int overflow)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.gui == null)
        {
            return;
        }

        Component text = Component.translatable("gui." + ModConstants.MOD_ID + ".dice.point", Integer.valueOf(point))
                .append(Component.literal(" "))
                .append(Component.translatable("gui." + ModConstants.MOD_ID + ".dice.color."
                        + (red ? "red" : "black")));

        if (overflow > 0)
        {
            text = text.copy().append(Component.literal(" "))
                    .append(Component.translatable("gui." + ModConstants.MOD_ID + ".dice.overflow",
                            Integer.valueOf(overflow)));
        }

        minecraft.gui.getChat().addMessage(text);
    }

    public static String devilsDiceId()
    {
        return itemId(ModItems.DEVILS_DICE.get());
    }

    public static String itemId(net.minecraft.world.item.Item item)
    {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? ModConstants.MOD_ID + ":unknown" : key.toString();
    }
}
