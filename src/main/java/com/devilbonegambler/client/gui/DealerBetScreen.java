/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.menu.DealerBetMenu;
import com.devilbonegambler.network.packet.DealerInteractPacket;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Own bet box of one member as a real container screen: the nine bet slots on top, the inventory and the hotbar below.
 * Both grids are slots of the menu, so picking a stack up, putting it down, the half and single item click and the
 * shift move are the vanilla container rules and the server re-checks every move.
 * The header numbers and the button flags arrive with the container data of the menu, and every action button leaves
 * the screen, which is the rule the whole dealer page follows.
 */
public class DealerBetScreen extends AbstractContainerScreen<DealerBetMenu>
{
    private static final String LANG = "gui." + ModConstants.MOD_ID + ".dealer.";
    private static final String AUTHOR = "gui." + ModConstants.MOD_ID + ".author";
    private static final int PANEL_COLOR = 0xC0101010;
    private static final int BET_FRAME_COLOR = 0xFF262626;
    private static final int SLOT_COLOR = 0xFF3B3B3B;
    private static final int SLOT_SHADE = 0xFF101010;
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFFA0A0A0;
    private static final int TITLE_Y = 6;
    private static final int BET_TOTAL_Y = 76;
    private static final int BET_COUNT_Y = 86;
    private static final int BUTTON_WIDTH = 76;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_ROW_Y = 178;
    private static final int BUTTON_CLOSE_Y = 200;
    private static final int AUTHOR_OFFSET_Y = 224;
    private static final int PANEL_MARGIN = 8;
    private static final int FRAME_INSET = 4;
    private static final int FRAME_MARGIN = 3;

    private boolean returnToPanel = true;
    private int builtFlags = Integer.MIN_VALUE;

    public DealerBetScreen(DealerBetMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        this.imageWidth = DealerBetMenu.PANEL_WIDTH;
        this.imageHeight = DealerBetMenu.PANEL_HEIGHT;
    }

    @Override
    protected void init()
    {
        super.init();
        this.rebuildButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        /*
         * The flags decide which buttons exist and they only arrive with the container data, which is pushed after the
         * screen was built, so the button row is rebuilt whenever the flags changed.
         */
        if (this.builtFlags != this.menu.flags())
        {
            this.rebuildButtons();
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        int left = this.leftPos;
        int top = this.topPos;
        graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
        /* The bet box gets its own recessed frame so the two grids are easy to tell apart. */
        graphics.fill(left + DealerBetMenu.BET_X - FRAME_INSET, top + DealerBetMenu.BET_Y - FRAME_INSET,
                left + DealerBetMenu.BET_X + 3 * DealerBetMenu.SLOT_SIZE + FRAME_MARGIN,
                top + DealerBetMenu.BET_Y + 3 * DealerBetMenu.SLOT_SIZE + FRAME_MARGIN, BET_FRAME_COLOR);

        for (int index = 0; index < this.menu.slots.size(); index++)
        {
            Slot slot = this.menu.slots.get(index);
            this.drawSlotFrame(graphics, left + slot.x, top + slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(this.font, this.title, PANEL_MARGIN, TITLE_Y, LABEL_COLOR, false);

        if (this.menu.phase() == DealerBetMenu.PHASE_RECRUIT)
        {
            Component remaining = Component.translatable(LANG + "remaining",
                    Integer.valueOf(this.menu.remainingSeconds()));
            graphics.drawString(this.font, remaining, this.imageWidth - PANEL_MARGIN - this.font.width(remaining),
                    TITLE_Y, MUTED_COLOR, false);
        }

        graphics.drawString(this.font,
                Component.translatable(LANG + "bet_total", Long.valueOf(this.menu.betValue())), PANEL_MARGIN,
                BET_TOTAL_Y, MUTED_COLOR, false);
        graphics.drawString(this.font,
                Component.translatable(LANG + "bet_count", Integer.valueOf(this.menu.betCount())), PANEL_MARGIN,
                BET_COUNT_Y, MUTED_COLOR, false);

        /*
         * A box of another member is a view only: the title names that member and this marker sits on the count line,
         * so the player never wonders why the slots do not react.
         */
        if ((this.menu.flags() & DealerBetMenu.FLAG_VIEW_ONLY) != 0)
        {
            Component viewOnly = Component.translatable(LANG + "bet_view_only");
            graphics.drawString(this.font, viewOnly,
                    this.imageWidth - PANEL_MARGIN - this.font.width(viewOnly), BET_COUNT_Y, MUTED_COLOR, false);
        }
        graphics.drawString(this.font, Component.translatable(AUTHOR), PANEL_MARGIN, AUTHOR_OFFSET_Y, MUTED_COLOR,
                false);
    }

    /**
     * A plain close goes back to the dealer panel the box was entered from. The server closes the screen on its own
     * when the session ends and that path never calls this method, so a forced close never asks the panel back.
     */
    @Override
    public void onClose()
    {
        int dealerEntityId = this.menu.dealerEntityId();
        boolean reopen = this.returnToPanel;
        super.onClose();

        if (reopen && dealerEntityId >= 0)
        {
            ClientActions.sendDealerInteract(dealerEntityId, DealerInteractPacket.ACTION_OPEN);
        }
    }

    private void rebuildButtons()
    {
        this.clearWidgets();
        int flags = this.menu.flags();
        this.builtFlags = flags;
        int leftX = this.leftPos + PANEL_MARGIN;
        int rightX = this.leftPos + this.imageWidth - PANEL_MARGIN - BUTTON_WIDTH;
        int rowY = this.topPos + BUTTON_ROW_Y;

        if ((flags & DealerBetMenu.FLAG_CAN_CREATE) != 0)
        {
            this.addRenderableWidget(Button.builder(Component.translatable(LANG + "create"),
                    button -> this.closeWithAction(DealerInteractPacket.ACTION_CREATE))
                    .bounds(leftX, rowY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }
        else if ((flags & DealerBetMenu.FLAG_CAN_START) != 0)
        {
            this.addRenderableWidget(Button.builder(Component.translatable(LANG + "start"),
                    button -> this.closeWithAction(DealerInteractPacket.ACTION_START))
                    .bounds(leftX, rowY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        if ((flags & DealerBetMenu.FLAG_MEMBER) != 0 && this.menu.phase() == DealerBetMenu.PHASE_RECRUIT)
        {
            this.addRenderableWidget(Button.builder(Component.translatable(LANG + "leave"),
                    button -> this.closeWithAction(DealerInteractPacket.ACTION_LEAVE))
                    .bounds(rightX, rowY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        this.addRenderableWidget(Button.builder(Component.translatable(LANG + "close"), button -> this.onClose())
                .bounds(rightX, this.topPos + BUTTON_CLOSE_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    /**
     * Every action leaves the screen, and the panel is not asked back because the player either left the wager or
     * started it.
     */
    private void closeWithAction(int action)
    {
        this.returnToPanel = false;
        ClientActions.sendDealerInteract(this.menu.dealerEntityId(), action);
        this.onClose();
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y)
    {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_SHADE);
        graphics.fill(x, y, x + 16, y + 16, SLOT_COLOR);
    }
}
