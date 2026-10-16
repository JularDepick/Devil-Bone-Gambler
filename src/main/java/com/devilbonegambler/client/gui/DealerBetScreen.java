/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.network.packet.DealerBetMovePacket;
import com.devilbonegambler.network.packet.DealerPanelPacket;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Own bet box of one member: nine bet slots on top and the player inventory below.
 * The bet box is filled the way the vanilla inventory is filled: a click picks a stack onto the cursor, another click
 * puts it down, a right click moves half or a single item, and shift moves a whole stack. The drawn inventory is the
 * real one minus what sits on the cursor, and every box move is still re-checked and performed by the server.
 */
public class DealerBetScreen extends Screen
{
    private static final String LANG = "gui." + ModConstants.MOD_ID + ".dealer.";
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;
    private static final int SLOT = 18;
    private static final int BET_X = 61;
    private static final int BET_Y = 18;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int PANEL_COLOR = 0xC0101010;
    private static final int SLOT_COLOR = 0xFF3B3B3B;
    private static final int SLOT_SHADE = 0xFF101010;
    private static final int HIGHLIGHT_COLOR = 0x40FFFFFF;
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFFA0A0A0;

    private DealerPanelPacket panel;
    private List<ItemStack> bet = new ArrayList<ItemStack>();
    private ItemStack carried = ItemStack.EMPTY;
    private int leftPos;
    private int topPos;

    public DealerBetScreen(DealerPanelPacket panel)
    {
        super(Component.translatable(LANG + "bet_title"));
        this.applyPanel(panel);
    }

    public void applyPanel(DealerPanelPacket updated)
    {
        this.panel = updated;
        this.bet = new ArrayList<ItemStack>(updated.selfBet());
        trimCarried();
    }

    @Override
    protected void init()
    {
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(graphics);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + PANEL_WIDTH, this.topPos + PANEL_HEIGHT, PANEL_COLOR);
        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 6, LABEL_COLOR);
        this.drawBetSlots(graphics, mouseX, mouseY);
        this.drawInventory(graphics, mouseX, mouseY);
        graphics.drawString(this.font, Component.translatable(LANG + "bet_total", Long.valueOf(this.betValue())),
                this.leftPos + 8, this.topPos + 74, MUTED_COLOR);
        graphics.drawString(this.font, Component.translatable("gui." + ModConstants.MOD_ID + ".author"),
                this.leftPos + 8, this.topPos + PANEL_HEIGHT - 10, MUTED_COLOR);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (!this.carried.isEmpty())
        {
            graphics.renderItem(this.carried, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(this.font, this.carried, mouseX - 8, mouseY - 8);
        }

        this.drawTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (this.panel == null)
        {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int betIndex = this.betIndexAt(mouseX, mouseY);

        if (betIndex >= 0)
        {
            clickBet(betIndex, button);
            return true;
        }

        int inventoryIndex = this.inventoryIndexAt(mouseX, mouseY);

        if (inventoryIndex >= 0)
        {
            clickInventorySlot(inventoryIndex, button);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose()
    {
        Minecraft minecraft = Minecraft.getInstance();

        /* Whatever the cursor still holds belongs to the player, so it goes back before the panel closes. */
        if (!this.carried.isEmpty())
        {
            this.carried = ItemStack.EMPTY;
        }

        if (this.panel != null)
        {
            minecraft.setScreen(new DealerScreen(this.panel));
            return;
        }

        super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    /**
     * The drawn inventory: the real one minus what the cursor carries. The box is not subtracted because the server
     * already moved those items out of the inventory.
     */
    private List<ItemStack> currentView()
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null)
        {
            return new ArrayList<ItemStack>();
        }

        return SlotMoves.visibleInventory(minecraft.player.getInventory(), this.carried,
                Collections.<ItemStack>emptyList());
    }

    /**
     * Vanilla style click on a bet slot: take the slot out with an empty cursor, otherwise place what is carried.
     */
    private void clickBet(int index, int button)
    {
        ItemStack slot = this.stackAt(index);

        if (hasShiftDown())
        {
            if (!slot.isEmpty())
            {
                ClientActions.sendDealerBetMove(this.panel.dealerEntityId(), DealerBetMovePacket.ACTION_TO_INVENTORY,
                        index, itemIdOf(slot), slot.getCount());
            }

            return;
        }

        if (this.carried.isEmpty())
        {
            if (slot.isEmpty())
            {
                return;
            }

            int amount = button == 1 ? (slot.getCount() + 1) / 2 : slot.getCount();
            ClientActions.sendDealerBetMove(this.panel.dealerEntityId(), DealerBetMovePacket.ACTION_TO_INVENTORY, index,
                    itemIdOf(slot), amount);
            this.carried = slot.copy();
            this.carried.setCount(amount);
            return;
        }

        if (!slot.isEmpty() && !SlotMoves.sameStack(slot, this.carried))
        {
            return;
        }

        int amount = button == 1 ? 1 : this.carried.getCount();
        placeIntoBox(index, amount);
    }

    /**
     * Vanilla style click on an inventory slot: pick a stack up, put it down, or shift it straight into the box.
     */
    private void clickInventorySlot(int index, int button)
    {
        List<ItemStack> view = currentView();
        ItemStack slot = index >= 0 && index < view.size() ? view.get(index) : ItemStack.EMPTY;

        if (hasShiftDown())
        {
            if (!slot.isEmpty())
            {
                placeIntoBox(DealerBetMovePacket.ANY_INDEX, slot.getCount());
            }

            return;
        }

        if (this.carried.isEmpty())
        {
            if (slot.isEmpty())
            {
                return;
            }

            int amount = button == 1 ? (slot.getCount() + 1) / 2 : slot.getCount();
            this.carried = slot.copy();
            this.carried.setCount(amount);
            return;
        }

        if (!slot.isEmpty() && !SlotMoves.sameStack(slot, this.carried))
        {
            /* A different item swaps, which returns the previous stack to the inventory view. */
            this.carried = slot.copy();
            return;
        }

        int amount = button == 1 ? 1 : this.carried.getCount();
        this.carried.shrink(amount);

        if (this.carried.getCount() <= 0)
        {
            this.carried = ItemStack.EMPTY;
        }
    }

    private void placeIntoBox(int index, int amount)
    {
        if (this.carried.isEmpty() || amount <= 0)
        {
            return;
        }

        int moved = Math.min(amount, this.carried.getCount());
        ClientActions.sendDealerBetMove(this.panel.dealerEntityId(), DealerBetMovePacket.ACTION_TO_BOX, index,
                itemIdOf(this.carried), moved);
        this.carried.shrink(moved);

        if (this.carried.getCount() <= 0)
        {
            this.carried = ItemStack.EMPTY;
        }
    }

    /**
     * Drops cursor items the inventory no longer backs, so a refused move can not leave a phantom stack behind.
     */
    private void trimCarried()
    {
        if (this.carried.isEmpty())
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null)
        {
            this.carried = ItemStack.EMPTY;
            return;
        }

        Inventory inventory = minecraft.player.getInventory();
        int available = 0;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);

            if (!stack.isEmpty() && SlotMoves.sameStack(stack, this.carried))
            {
                available += stack.getCount();
            }
        }

        if (available >= this.carried.getCount())
        {
            return;
        }

        if (available <= 0)
        {
            this.carried = ItemStack.EMPTY;
            return;
        }

        this.carried.setCount(available);
    }

    private static String itemIdOf(ItemStack stack)
    {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    private void drawBetSlots(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int hovered = this.betIndexAt(mouseX, mouseY);

        for (int index = 0; index < ModConstants.DEALER_BET_SLOTS; index++)
        {
            int row = index / 3;
            int column = index % 3;
            this.drawSlot(graphics, this.leftPos + BET_X + column * SLOT, this.topPos + BET_Y + row * SLOT,
                    this.stackAt(index), hovered == index);
        }
    }

    private void drawInventory(GuiGraphics graphics, int mouseX, int mouseY)
    {
        List<ItemStack> view = currentView();
        int hovered = this.inventoryIndexAt(mouseX, mouseY);

        for (int row = 0; row < 3; row++)
        {
            for (int column = 0; column < 9; column++)
            {
                int index = 9 + row * 9 + column;
                this.drawSlot(graphics, this.leftPos + INVENTORY_X + column * SLOT,
                        this.topPos + INVENTORY_Y + row * SLOT, stackIn(view, index), hovered == index);
            }
        }

        for (int column = 0; column < 9; column++)
        {
            this.drawSlot(graphics, this.leftPos + INVENTORY_X + column * SLOT, this.topPos + HOTBAR_Y,
                    stackIn(view, column), hovered == column);
        }
    }

    private static ItemStack stackIn(List<ItemStack> view, int index)
    {
        return index >= 0 && index < view.size() ? view.get(index) : ItemStack.EMPTY;
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, ItemStack stack, boolean hovered)
    {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_SHADE);
        graphics.fill(x, y, x + 16, y + 16, SLOT_COLOR);

        if (hovered)
        {
            graphics.fill(x, y, x + 16, y + 16, HIGHLIGHT_COLOR);
        }

        if (!stack.isEmpty())
        {
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(this.font, stack, x, y);
        }
    }

    private void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if (!this.carried.isEmpty())
        {
            graphics.renderTooltip(this.font, this.carried, mouseX, mouseY);
            return;
        }

        int betIndex = this.betIndexAt(mouseX, mouseY);

        if (betIndex >= 0)
        {
            ItemStack stack = this.stackAt(betIndex);

            if (!stack.isEmpty())
            {
                graphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }

            return;
        }

        int inventoryIndex = this.inventoryIndexAt(mouseX, mouseY);

        if (inventoryIndex >= 0)
        {
            ItemStack stack = stackIn(currentView(), inventoryIndex);

            if (!stack.isEmpty())
            {
                graphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
        }
    }

    private ItemStack stackAt(int index)
    {
        return index < 0 || index >= this.bet.size() ? ItemStack.EMPTY : this.bet.get(index);
    }

    private long betValue()
    {
        if (this.panel == null)
        {
            return 0L;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null)
        {
            return 0L;
        }

        for (DealerPanelPacket.MemberEntry entry : this.panel.members())
        {
            if (entry.playerId().equals(minecraft.player.getUUID()))
            {
                return entry.betValue();
            }
        }

        return 0L;
    }

    private int betIndexAt(double mouseX, double mouseY)
    {
        for (int index = 0; index < ModConstants.DEALER_BET_SLOTS; index++)
        {
            int row = index / 3;
            int column = index % 3;
            int x = this.leftPos + BET_X + column * SLOT;
            int y = this.topPos + BET_Y + row * SLOT;

            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
            {
                return index;
            }
        }

        return -1;
    }

    private int inventoryIndexAt(double mouseX, double mouseY)
    {
        for (int row = 0; row < 3; row++)
        {
            for (int column = 0; column < 9; column++)
            {
                int x = this.leftPos + INVENTORY_X + column * SLOT;
                int y = this.topPos + INVENTORY_Y + row * SLOT;

                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
                {
                    return 9 + row * 9 + column;
                }
            }
        }

        for (int column = 0; column < 9; column++)
        {
            int x = this.leftPos + INVENTORY_X + column * SLOT;
            int y = this.topPos + HOTBAR_Y;

            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
            {
                return column;
            }
        }

        return -1;
    }
}
