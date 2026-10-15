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
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Own bet box of one member: nine bet slots on top and the read only player inventory below.
 * A left click moves one item, a right click moves the whole stack, both directions are server validated.
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
        this.drawTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int betIndex = this.betIndexAt(mouseX, mouseY);

        if (betIndex >= 0 && this.panel != null)
        {
            ClientActions.sendDealerBetMove(this.panel.dealerEntityId(), DealerBetMovePacket.FROM_BET_BOX, betIndex,
                    button == 1);
            return true;
        }

        int inventoryIndex = this.inventoryIndexAt(mouseX, mouseY);

        if (inventoryIndex >= 0 && this.panel != null)
        {
            ClientActions.sendDealerBetMove(this.panel.dealerEntityId(), DealerBetMovePacket.FROM_INVENTORY,
                    inventoryIndex, button == 1);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose()
    {
        Minecraft minecraft = Minecraft.getInstance();

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
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null)
        {
            return;
        }

        Inventory inventory = minecraft.player.getInventory();
        int hovered = this.inventoryIndexAt(mouseX, mouseY);

        for (int row = 0; row < 3; row++)
        {
            for (int column = 0; column < 9; column++)
            {
                int index = 9 + row * 9 + column;
                this.drawSlot(graphics, this.leftPos + INVENTORY_X + column * SLOT,
                        this.topPos + INVENTORY_Y + row * SLOT, inventory.getItem(index), hovered == index);
            }
        }

        for (int column = 0; column < 9; column++)
        {
            this.drawSlot(graphics, this.leftPos + INVENTORY_X + column * SLOT, this.topPos + HOTBAR_Y,
                    inventory.getItem(column), hovered == column);
        }
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
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player != null)
            {
                ItemStack stack = minecraft.player.getInventory().getItem(inventoryIndex);

                if (!stack.isEmpty())
                {
                    graphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
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
