/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.client.gui.AdminConfigScreen;
import com.devilbonegambler.client.gui.AltarScreen;
import com.devilbonegambler.draw.OfferingBundle;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.AdminPanelRequestPacket;
import com.devilbonegambler.network.packet.DevilsDiceRequestPacket;
import com.devilbonegambler.network.packet.GamblersDiceRequestPacket;
import com.devilbonegambler.network.packet.ValueTableRefreshRequestPacket;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
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
    private static long economyCoin = 0L;
    private static long economyTear = 0L;
    private static long economyCounterCoin = 0L;
    private static long economyCounterTear = 0L;
    private static long economyVipUntil = 0L;
    private static int economyPending = 0;
    private static int economyTick = 0;
    private static String economyShopJson = "{}";
    private static String economyJson = "{}";

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

    public static void openAltarScreen()
    {
        Minecraft.getInstance().setScreen(new AltarScreen());
    }

    /**
     * Opens one of the four economy screens and asks the server for a fresh snapshot.
     */
    public static void openEconomyScreen(com.devilbonegambler.block.EconomyBlock.Kind kind)
    {
        sendEconomyRequest(com.devilbonegambler.economy.EconomyOperation.REQUEST_DATA,
                com.devilbonegambler.economy.CurrencyType.COIN, 0L, 0L, 0, "", "");
        Minecraft.getInstance().setScreen(new com.devilbonegambler.client.gui.EconomyScreen(kind));
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
            openAltarScreen();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null)
        {
            return;
        }

        ModNetwork.sendToServer(new DevilsDiceRequestPacket(nextSequence(),
                minecraft.player.getGameProfile().getName(), devilsDiceId(), OfferingBundle.empty()));
        minecraft.gui.setOverlayMessage(
                Component.translatable("gui." + ModConstants.MOD_ID + ".altar.place_hint"), false);
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
     * Cached economy snapshot pushed by the server. The screens only read it, never write it.
     */
    public static void setEconomyData(long coin, long tear, long counterCoin, long counterTear, long vipUntil,
            int pendingCount, int tickCount, String shopJson, String economyJson)
    {
        economyCoin = coin;
        economyTear = tear;
        economyCounterCoin = counterCoin;
        economyCounterTear = counterTear;
        economyVipUntil = vipUntil;
        economyPending = pendingCount;
        economyTick = tickCount;
        economyShopJson = shopJson == null ? "{}" : shopJson;
        economyJson = economyJson == null ? "{}" : economyJson;
    }

    public static long economyCoin() { return economyCoin; }

    public static long economyTear() { return economyTear; }

    public static long economyCounterCoin() { return economyCounterCoin; }

    public static long economyCounterTear() { return economyCounterTear; }

    public static long economyVipUntil() { return economyVipUntil; }

    public static int economyPending() { return economyPending; }

    public static int economyTick() { return economyTick; }

    public static String economyShopJson() { return economyShopJson; }

    public static String economyJson() { return economyJson; }

    public static boolean economyVipActive()
    {
        return economyVipUntil > economyTick;
    }

    /**
     * Sends one economy operation. Every field is an intent only; the server re-validates all of them.
     */
    public static void sendEconomyRequest(com.devilbonegambler.economy.EconomyOperation operation,
            com.devilbonegambler.economy.CurrencyType currency, long amount, long amountB, int count, String textA,
            String textB)
    {
        ModNetwork.sendToServer(new com.devilbonegambler.network.packet.EconomyRequestPacket(nextSequence(), operation,
                currency, amount, amountB, count, textA, textB));
    }

    /**
     * Shows the outcome of the last economy operation on the action bar.
     */
    public static void onEconomyResponse(int sequence, boolean success, String reasonKey)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.gui == null)
        {
            return;
        }

        Component text = success
                ? Component.translatable("gui." + ModConstants.MOD_ID + ".economy.done")
                : Component.translatable("gui." + ModConstants.MOD_ID + ".reason."
                        + (reasonKey == null || reasonKey.isEmpty() ? "unknown" : reasonKey));
        minecraft.gui.setOverlayMessage(text, false);
    }

    public static void openValueTableScreen(String titleJson, String author, List<String> pages)
    {
        ItemStack display = new ItemStack(ModItems.OFFERING_VALUE_TABLE.get());
        CompoundTag tag = display.getOrCreateTag();
        tag.putString(WrittenBookItem.TAG_TITLE, titleJson);
        tag.putString(WrittenBookItem.TAG_AUTHOR, author);
        ListTag pageTags = new ListTag();

        for (String page : pages)
        {
            pageTags.add(StringTag.valueOf(page));
        }

        tag.put(WrittenBookItem.TAG_PAGES, pageTags);
        tag.putInt(WrittenBookItem.TAG_GENERATION, 0);
        tag.putBoolean(WrittenBookItem.TAG_RESOLVED, true);

        Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.inventory.BookViewScreen(
                new net.minecraft.client.gui.screens.inventory.BookViewScreen.WrittenBookAccess(display)));
    }

    public static void openAdminPanel(String prizePoolJson, String itemValuesJson, String shopJson)
    {
        Minecraft.getInstance().setScreen(new AdminConfigScreen(prizePoolJson, itemValuesJson, shopJson));
    }

    /**
     * Shows the rolled point and colour on the action bar, before the result text arrives.
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

        minecraft.gui.setOverlayMessage(text, false);
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
