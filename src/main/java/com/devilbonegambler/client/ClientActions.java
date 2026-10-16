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
import com.devilbonegambler.draw.OfferingBundle;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.AdminPanelRequestPacket;
import com.devilbonegambler.network.packet.DealerBetMovePacket;
import com.devilbonegambler.network.packet.DealerInteractPacket;
import com.devilbonegambler.network.packet.DealerPanelPacket;
import com.devilbonegambler.network.packet.DevilsDiceRequestPacket;
import com.devilbonegambler.network.packet.GamblersDiceRequestPacket;
import com.devilbonegambler.network.packet.ValueTableRefreshRequestPacket;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.ValueTableBook;
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
    /*
     * Vanilla rejects a book tag whose title is longer than this, and the same string is used as the item name.
     */
    private static final int BOOK_TITLE_MAX_LENGTH = 32;

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

    public static void openAltarScreen()
    {
        Minecraft.getInstance().setScreen(new AltarScreen());
    }

    /**
     * Dealer panel intents. The panel data itself is always pushed by the server, the client only asks for an action.
     */
    public static void sendDealerInteract(int dealerEntityId, int action)
    {
        ModNetwork.sendToServer(new DealerInteractPacket(dealerEntityId, action));
    }

    public static void sendDealerBetMove(int dealerEntityId, int action, int index, boolean wholeStack)
    {
        ModNetwork.sendToServer(new DealerBetMovePacket(dealerEntityId, action, index, wholeStack));
    }

    public static void openDealerScreen(DealerPanelPacket panel)
    {
        Minecraft.getInstance().setScreen(new DealerScreen(panel));
    }

    public static void openDealerBetScreen(DealerPanelPacket panel)
    {
        Minecraft.getInstance().setScreen(new DealerBetScreen(panel));
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
     * The vanilla book tag takes a plain title of at most 32 characters and that title also becomes the item name, so
     * it is resolved here in the language of the client instead of being stored on the server side stack.
     */
    public static void openValueTableScreen(String author, List<String> pages)
    {
        ItemStack display = new ItemStack(ModItems.OFFERING_VALUE_TABLE.get());
        CompoundTag tag = display.getOrCreateTag();
        tag.putString(WrittenBookItem.TAG_TITLE, bookTitle());
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

    public static void openAdminPanel(String prizePoolJson, String itemValuesJson)
    {
        Minecraft.getInstance().setScreen(new AdminConfigScreen(prizePoolJson, itemValuesJson));
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

    private static String bookTitle()
    {
        String title = Component.translatable(ValueTableBook.BOOK_TITLE_KEY).getString();
        return title.length() <= BOOK_TITLE_MAX_LENGTH ? title : title.substring(0, BOOK_TITLE_MAX_LENGTH);
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
