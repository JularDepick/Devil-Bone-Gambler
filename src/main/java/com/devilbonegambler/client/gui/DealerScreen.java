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
import java.util.ArrayList;
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
    private static final ResourceLocation DEALER_TEXTURE =
            ResourceLocation.tryParse("devil_bone_gambler:textures/entity/gamblers_dealer.png");

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 210;
    private static final int CELL = 34;
    private static final int GRID = CELL * 3;
    private static final int GRID_TOP = 32;
    private static final int CHEST_SIZE = 14;
    /* The head of the dealer texture is an 8x8x8 box at (0,0), so its front face sits at (8,8). */
    private static final int TEXTURE_SIZE = 64;
    private static final int HEAD_U = 8;
    private static final int HEAD_V = 8;
    private static final int HEAD_SOURCE = 8;
    private static final int PORTRAIT_SIZE = 24;
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

    /**
     * Hover tooltip of the nine grid cells, held back for the shared hover delay like every other cell of the mod.
     */
    private final DelayedTooltip tooltip = new DelayedTooltip();
    private int leftPos;
    private int topPos;

    public DealerScreen(DealerPanelPacket panel)
    {
        super(Component.translatable(LANG + "title"));
        this.panel = panel;
    }

    /**
     * A panel push arrives once per second while a wager recruits, so the hover timer is only released when the panel
     * belongs to another dealer; the hover key already follows the cell under the pointer and the lines are rebuilt
     * from the live panel every frame, so a push must not restart the delay.
     */
    public void applyPanel(DealerPanelPacket updated)
    {
        if (this.panel == null || this.panel.dealerEntityId() != updated.dealerEntityId())
        {
            this.tooltip.reset();
        }

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
        int member = this.memberCellAt(mouseX, mouseY);

        if (member >= 0 && this.panel != null)
        {
            /*
             * Every cell of the nine grid opens the bet box of the member sitting in it. The box is a container menu
             * owned by the server, so the panel only asks for it and the server opens it after it checked that the
             * requester is a member of the wager and marked whether that box may be changed.
             */
            ClientActions.sendDealerBetOpenRequest(this.panel.dealerEntityId(),
                    this.panel.members().get(member).playerId());
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
        /*
         * Every action leads out of this panel, leaving the wager included: the refreshed panel only goes to the
         * players still in the wager, so a member who just left would otherwise keep staring at a panel they are no
         * longer part of.
         */
        this.onClose();
    }

    private void drawDealerCell(GuiGraphics graphics, int x, int y)
    {
        this.drawCellFrame(graphics, x, y);
        /*
         * The portrait is cut straight out of the dealer texture, so the panel shows the same face as the entity.
         * The blit overload takes the source region and the texture size, so the 8x8 head face is scaled to the cell.
         */
        graphics.blit(DEALER_TEXTURE, x + 4, y + 1, PORTRAIT_SIZE, PORTRAIT_SIZE, HEAD_U, HEAD_V, HEAD_SOURCE,
                HEAD_SOURCE, TEXTURE_SIZE, TEXTURE_SIZE);
        float health = this.panel.dealerMaxHealth() <= 0.0F ? 0.0F
                : this.panel.dealerHealth() / this.panel.dealerMaxHealth();
        float satiety = this.panel.dealerMaxSatiety() <= 0 ? 0.0F
                : this.panel.dealerSatiety() / (float) this.panel.dealerMaxSatiety();
        /* Both bars sit under the portrait, so the two states stay readable next to the face. */
        int barX = x + 1;
        int barWidth = CELL - 2;
        this.drawBar(graphics, barX, y + 1 + PORTRAIT_SIZE, barWidth, health, HEALTH_COLOR);
        this.drawBar(graphics, barX, y + 1 + PORTRAIT_SIZE + BAR_HEIGHT + 1, barWidth, satiety, SATIETY_COLOR);
    }

    private void drawMemberCell(GuiGraphics graphics, int x, int y, DealerPanelPacket.MemberEntry entry)
    {
        this.drawCellFrame(graphics, x, y);
        PlayerFaceRenderer.draw(graphics, this.skinOf(entry.playerId()), x + 1, y + 1, 32);

        /*
         * Every member has a bet box, so every cell carries the box icon and the whole cell is the click target;
         * the tooltip says whether that box may be changed.
         */
        graphics.pose().pushPose();
        graphics.pose().translate(x + 1 + 32 - CHEST_SIZE, y + 1 + 32 - CHEST_SIZE, 0.0D);
        graphics.pose().scale(0.875F, 0.875F, 1.0F);
        graphics.renderItem(new ItemStack(Items.CHEST), 0, 0);
        graphics.pose().popPose();
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
                List<Component> lines = new ArrayList<Component>();
                /*
                 * A member who went offline is shown under the name the session knows; a session that never saw the
                 * name shows the offline wording instead of any identifier.
                 */
                lines.add(entry.name().isEmpty()
                        ? Component.translatable(LANG + "offline_member")
                        : Component.literal(entry.name()));
                lines.add(Component.translatable(LANG + "bet_value", Long.valueOf(entry.betValue())));
                lines.add(Component.translatable(LANG + "bet_count", Integer.valueOf(entry.betCount())));
                lines.add(Component.translatable(entry.playerId().equals(this.selfId())
                        ? LANG + "open_hint" : LANG + "open_hint_others"));
                /* The key carries the member, so a cell whose occupant changed waits again instead of reusing the delay. */
                this.tooltip.render(graphics, this.font, "member:" + entry.playerId(), lines, mouseX, mouseY);
                return;
            }
        }

        int dealerX = gridX + CELL;
        int dealerY = gridY + CELL;

        if (mouseX >= dealerX && mouseX < dealerX + CELL - 2 && mouseY >= dealerY && mouseY < dealerY + CELL - 2)
        {
            List<Component> lines = new ArrayList<Component>();
            /*
             * The pocket comes first, then the two bars and the experience. The vanilla tooltip keeps one line height
             * between them, so the block stays evenly spaced however long the numbers get.
             */
            lines.add(Component.translatable(LANG + "pocket_space",
                    Integer.valueOf(this.panel.dealerPocketFree())));
            lines.add(Component.translatable(LANG + "health",
                    Integer.valueOf((int) this.panel.dealerHealth()),
                    Integer.valueOf((int) this.panel.dealerMaxHealth())));
            lines.add(Component.translatable(LANG + "satiety", Integer.valueOf(this.panel.dealerSatiety()),
                    Integer.valueOf(this.panel.dealerMaxSatiety())));
            lines.add(Component.translatable(LANG + "experience",
                    Component.literal(String.format(java.util.Locale.ROOT, "%.1f", this.panel.dealerExperience()))));
            this.tooltip.render(graphics, this.font, "dealer", lines, mouseX, mouseY);
            return;
        }

        /* A pointer that left the grid releases the timer, so the next cell waits again. */
        this.tooltip.render(graphics, this.font, null, null, mouseX, mouseY);
    }

    /**
     * Returns the member index whose cell of the nine grid is under the cursor, or minus one.
     * Any member may be asked for, because the box of another member is opened as a read only view.
     */
    private int memberCellAt(double mouseX, double mouseY)
    {
        if (this.panel == null)
        {
            return -1;
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
