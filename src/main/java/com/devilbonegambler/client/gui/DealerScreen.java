/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.network.packet.DealerInteractPacket;
import com.devilbonegambler.network.packet.DealerPanelPacket;
import com.devilbonegambler.util.ModConstants;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Dealer panel: the nine grid with the dealer in the middle, eight member cells around it and the wager buttons.
 * Every value comes from the server pushed panel, the screen never derives a rule on its own.
 */
public class DealerScreen extends Screen
{
    private static final String LANG = "gui." + ModConstants.MOD_ID + ".dealer.";
    private static final ResourceLocation VILLAGER_TEXTURE =
            ResourceLocation.tryParse("minecraft:textures/entity/villager/villager.png");

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 210;
    private static final int CELL = 34;
    private static final int GRID = CELL * 3;
    private static final int GRID_TOP = 32;
    private static final int CHEST_SIZE = 14;
    private static final int BAR_HEIGHT = 3;
    private static final int AUTHOR_OFFSET_Y = 198;
    private static final int STATUS_OFFSET_Y = 20;
    private static final int BUTTON_WIDTH = 88;
    private static final int BUTTON_HEIGHT = 20;

    private static final int PANEL_COLOR = 0xC0101010;
    private static final int SLOT_COLOR = 0xFF3B3B3B;
    private static final int SLOT_SHADE = 0xFF101010;
    private static final int BAR_BACK_COLOR = 0xFF202020;
    private static final int HEALTH_COLOR = 0xFFD03030;
    private static final int SATIETY_COLOR = 0xFFC08030;
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFFA0A0A0;

    /*
     * Slot order of the eight cells around the dealer: bottom, top, left, right, top right, top left,
     * bottom left, bottom right. The dealer itself always sits in the middle cell.
     */
    private static final int[][] SLOT_CELLS = {
        {1, 2}, {1, 0}, {0, 1}, {2, 1}, {2, 0}, {0, 0}, {0, 2}, {2, 2}
    };

    private DealerPanelPacket panel;
    private int leftPos;
    private int topPos;

    public DealerScreen(DealerPanelPacket panel)
    {
        super(Component.translatable(LANG + "title"));
        this.panel = panel;
    }

    public void applyPanel(DealerPanelPacket updated)
    {
        this.panel = updated;
        this.rebuildButtons();
    }

    @Override
    protected void init()
    {
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;
        this.rebuildButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(graphics);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + PANEL_WIDTH, this.topPos + PANEL_HEIGHT, PANEL_COLOR);
        int gridX = this.leftPos + (PANEL_WIDTH - GRID) / 2;
        int gridY = this.topPos + GRID_TOP;

        if (this.panel != null)
        {
            this.drawDealerCell(graphics, gridX + CELL, gridY + CELL);
            List<DealerPanelPacket.MemberEntry> members = this.panel.members();

            for (int index = 0; index < members.size() && index < SLOT_CELLS.length; index++)
            {
                this.drawMemberCell(graphics, gridX + SLOT_CELLS[index][0] * CELL,
                        gridY + SLOT_CELLS[index][1] * CELL, members.get(index));
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        this.drawLabels(graphics);
        this.drawTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int chest = this.chestCellAt(mouseX, mouseY);

        if (chest >= 0 && this.panel != null)
        {
            ClientActions.openDealerBetScreen(this.panel);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    private void rebuildButtons()
    {
        this.clearWidgets();

        if (this.panel == null)
        {
            return;
        }

        int leftX = this.leftPos + 8;
        int rightX = this.leftPos + PANEL_WIDTH - 8 - BUTTON_WIDTH;
        int buttonY = this.topPos + PANEL_HEIGHT - 30;
        int topRowY = buttonY - 22;

        if (this.panel.canCreate())
        {
            this.addRenderableWidget(Button.builder(Component.translatable(LANG + "create"),
                    button -> this.send(DealerInteractPacket.ACTION_CREATE))
                    .bounds(leftX, topRowY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        if (this.panel.canStart())
        {
            this.addRenderableWidget(Button.builder(Component.translatable(LANG + "start"),
                    button -> this.send(DealerInteractPacket.ACTION_START))
                    .bounds(leftX, topRowY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        if (this.panel.selfIsMember() && this.panel.phase() == 1)
        {
            this.addRenderableWidget(Button.builder(Component.translatable(LANG + "leave"),
                    button -> this.send(DealerInteractPacket.ACTION_LEAVE))
                    .bounds(rightX, topRowY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        this.addRenderableWidget(Button.builder(Component.translatable(LANG + "close"), button -> this.onClose())
                .bounds(rightX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void send(int action)
    {
        if (this.panel == null)
        {
            return;
        }

        ClientActions.sendDealerInteract(this.panel.dealerEntityId(), action);

        if (action != DealerInteractPacket.ACTION_LEAVE)
        {
            this.onClose();
        }
    }

    private void drawDealerCell(GuiGraphics graphics, int x, int y)
    {
        this.drawCellFrame(graphics, x, y);
        /*
         * The villager head is an 8x10x8 box at texture offset (0,0), so its front face sits at (8,8) with a source
         * size of 8x10. The scalable blit overload is required: the plain (u, v, width, height) overload treats the
         * width and height as the source region too, which sampled a 32x32 quarter of the texture sheet.
         */
        graphics.blit(VILLAGER_TEXTURE, x + 4, y + 1, 26, 32, 8.0F, 8.0F, 8, 10, 64, 64);
        float health = this.panel.dealerMaxHealth() <= 0.0F ? 0.0F
                : this.panel.dealerHealth() / this.panel.dealerMaxHealth();
        float satiety = this.panel.dealerMaxSatiety() <= 0 ? 0.0F
                : this.panel.dealerSatiety() / (float) this.panel.dealerMaxSatiety();
        this.drawBar(graphics, x + 1, y + CELL - 9, 32, health, HEALTH_COLOR);
        this.drawBar(graphics, x + 1, y + CELL - 5, 32, satiety, SATIETY_COLOR);
    }

    private void drawMemberCell(GuiGraphics graphics, int x, int y, DealerPanelPacket.MemberEntry entry)
    {
        this.drawCellFrame(graphics, x, y);
        PlayerFaceRenderer.draw(graphics, this.skinOf(entry.playerId()), x + 1, y + 1, 32);

        if (entry.playerId().equals(this.selfId()))
        {
            graphics.pose().pushPose();
            graphics.pose().translate(x + 1 + 32 - CHEST_SIZE, y + 1 + 32 - CHEST_SIZE, 0.0D);
            graphics.pose().scale(0.875F, 0.875F, 1.0F);
            graphics.renderItem(new ItemStack(Items.CHEST), 0, 0);
            graphics.pose().popPose();
        }
    }

    private void drawCellFrame(GuiGraphics graphics, int x, int y)
    {
        graphics.fill(x - 1, y - 1, x + CELL - 1, y + CELL - 1, SLOT_SHADE);
        graphics.fill(x, y, x + CELL - 2, y + CELL - 2, SLOT_COLOR);
    }

    private void drawBar(GuiGraphics graphics, int x, int y, int width, float ratio, int color)
    {
        graphics.fill(x, y, x + width, y + BAR_HEIGHT, BAR_BACK_COLOR);
        int filled = (int) (width * Math.max(0.0F, Math.min(1.0F, ratio)));
        graphics.fill(x, y, x + filled, y + BAR_HEIGHT, color);
    }

    private void drawLabels(GuiGraphics graphics)
    {
        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 8, LABEL_COLOR);

        if (this.panel == null)
        {
            return;
        }

        if (this.panel.hasWager() && this.panel.phase() == 1)
        {
            Component remaining = Component.translatable(LANG + "remaining", Integer.valueOf(this.panel.remainingSeconds()));
            graphics.drawString(this.font, remaining, this.leftPos + PANEL_WIDTH - 8 - this.font.width(remaining),
                    this.topPos + 8, MUTED_COLOR);
        }

        graphics.drawString(this.font, Component.translatable("gui." + ModConstants.MOD_ID + ".author"),
                this.leftPos + 8, this.topPos + AUTHOR_OFFSET_Y, MUTED_COLOR);
    }

    private void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if (this.panel == null)
        {
            return;
        }

        int gridX = this.leftPos + (PANEL_WIDTH - GRID) / 2;
        int gridY = this.topPos + GRID_TOP;
        List<DealerPanelPacket.MemberEntry> members = this.panel.members();

        for (int index = 0; index < members.size() && index < SLOT_CELLS.length; index++)
        {
            int x = gridX + SLOT_CELLS[index][0] * CELL;
            int y = gridY + SLOT_CELLS[index][1] * CELL;

            if (mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2)
            {
                DealerPanelPacket.MemberEntry entry = members.get(index);
                Component text = Component.literal(entry.name())
                        .append(Component.literal("\n"))
                        .append(Component.translatable(LANG + "bet_value", Long.valueOf(entry.betValue())))
                        .append(Component.literal("\n"))
                        .append(Component.translatable(LANG + "bet_count", Integer.valueOf(entry.betCount())));
                graphics.renderTooltip(this.font, text, mouseX, mouseY);
                return;
            }
        }

        int dealerX = gridX + CELL;
        int dealerY = gridY + CELL;

        if (mouseX >= dealerX && mouseX < dealerX + CELL - 2 && mouseY >= dealerY && mouseY < dealerY + CELL - 2)
        {
            Component text = Component.translatable(LANG + "health",
                    Integer.valueOf((int) this.panel.dealerHealth()),
                    Integer.valueOf((int) this.panel.dealerMaxHealth()))
                    .copy()
                    .append(Component.literal("\n"))
                    .append(Component.translatable(LANG + "satiety", Integer.valueOf(this.panel.dealerSatiety()),
                            Integer.valueOf(this.panel.dealerMaxSatiety())));
            graphics.renderTooltip(this.font, text, mouseX, mouseY);
        }
    }

    /**
     * Returns the cell index of the own chest icon under the cursor, or minus one.
     */
    private int chestCellAt(double mouseX, double mouseY)
    {
        if (this.panel == null)
        {
            return -1;
        }

        int gridX = this.leftPos + (PANEL_WIDTH - GRID) / 2;
        int gridY = this.topPos + GRID_TOP;
        List<DealerPanelPacket.MemberEntry> members = this.panel.members();
        UUID self = this.selfId();

        for (int index = 0; index < members.size() && index < SLOT_CELLS.length; index++)
        {
            if (!members.get(index).playerId().equals(self))
            {
                continue;
            }

            int x = gridX + SLOT_CELLS[index][0] * CELL + 1 + 32 - CHEST_SIZE;
            int y = gridY + SLOT_CELLS[index][1] * CELL + 1 + 32 - CHEST_SIZE;

            if (mouseX >= x && mouseX < x + CHEST_SIZE && mouseY >= y && mouseY < y + CHEST_SIZE)
            {
                return index;
            }
        }

        return -1;
    }

    private UUID selfId()
    {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null : minecraft.player.getUUID();
    }

    private ResourceLocation skinOf(UUID playerId)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.getConnection() != null)
        {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(playerId);

            if (info != null)
            {
                return info.getSkinLocation();
            }
        }

        return DefaultPlayerSkin.getDefaultSkin(playerId);
    }
}
