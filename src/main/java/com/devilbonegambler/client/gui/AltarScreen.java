/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.client.ClientNotices;
import com.devilbonegambler.client.ClientPacketHandler;
import com.devilbonegambler.draw.Offering;
import com.devilbonegambler.draw.OfferingBundle;
import com.devilbonegambler.menu.AltarMenu;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.DevilsDiceRequestPacket;
import com.devilbonegambler.network.packet.DrawResultPacket;
import com.devilbonegambler.network.packet.MobListPacket;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.ValueTableBook;
import com.devilbonegambler.util.ValueTableSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The offering screen of the devil's dice, laid out in three columns.
 * The left column lists the creatures inside the altar range, the middle column keeps the real container slots of the
 * offering grid and of the player inventory, and the right column carries the numeric offerings.
 * Both grids are vanilla container slots, so picking up, placing, dragging and shift clicking are carried by the vanilla
 * container rules and the client keeps no copy of the inventory of its own. The offering grid belongs to the menu, so
 * the server reads the stacks that really sit there when the sacrifice is submitted and returns whatever is left when
 * the screen closes.
 */
public class AltarScreen extends AbstractContainerScreen<AltarMenu>
{
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 220;

    private static final int PANEL_COLOR = 0xC0101010;
    private static final int SLOT_COLOR = 0xFF373737;
    private static final int SLOT_SHADE = 0xFF2A2A2A;
    private static final int LIST_COLOR = 0x20000000;
    private static final int HEADER_COLOR = 0x30FFFFFF;
    private static final int ROW_HOVER_COLOR = 0x30FFFFFF;
    private static final int ROW_SELECTED_COLOR = 0x307FC97F;
    private static final int CHECK_COLOR = 0xFF7FC97F;
    private static final int SEPARATOR_COLOR = 0x40FFFFFF;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int HEADER_TEXT_COLOR = 0xC0C0C0;
    private static final int TEXT_COLOR = 0xE0E0E0;
    private static final int STATUS_COLOR = 0xFF8080;
    private static final int AUTHOR_COLOR = 0x707070;
    private static final int ESTIMATE_COLOR = 0xD8B860;
    private static final long TIMEOUT_MS = 10000L;

    private static final int LABEL_OFFSET = 14;
    private static final int CHECK_SIZE = 9;

    /*
     * Left column: the frozen header, the scrolling creature rows and the hint.
     */
    private static final int MOB_X = 8;
    private static final int MOB_WIDTH = 110;
    private static final int MOB_TABLE_WIDTH = 100;
    private static final int MOB_HEALTH_WIDTH = 40;
    private static final int MOB_VISIBLE_ROWS = 8;
    private static final int MOB_LIST_Y = 20;
    private static final int MOB_SCROLL_GAP = 2;

    private static final int TITLE_Y = 6;
    /*
     * The altar summary, the estimate, the accumulated altar value and the hint sit right of the offering grid instead
     * of above the creature list, so the left column belongs to the creature list alone.
     */
    private static final int MID_TEXT_X = 192;
    private static final int MID_TEXT_WIDTH = 106;
    private static final int SUMMARY_Y = 18;
    private static final int ESTIMATE_Y = 28;
    private static final int ALTAR_VALUE_Y = 38;
    private static final int HINT_Y = 54;
    private static final int HINT_LINE_HEIGHT = 10;

    /*
     * Middle column: the offering grid above and the real player inventory below, both positioned by the menu.
     */
    private static final int OFFERING_LABEL_Y = 14;
    private static final int INVENTORY_LABEL_Y = 80;
    private static final int OFFER_BUTTON_X = 126;
    private static final int CANCEL_BUTTON_X = 216;
    private static final int BOTTOM_BUTTON_Y = 174;
    private static final int BOTTOM_BUTTON_WIDTH = 84;
    private static final int BOTTOM_BUTTON_HEIGHT = 20;
    private static final int STATUS_X = 132;
    private static final int STATUS_Y = 196;
    private static final int STATUS_WIDTH = 168;
    private static final int AUTHOR_X = 132;
    private static final int AUTHOR_Y = 207;

    /*
     * Right column: the numeric offerings, their sliders and the action buttons.
     */
    private static final int VALUE_X = 308;
    private static final int VALUE_WIDTH = 84;
    private static final int VALUE_LABEL_Y = 18;
    private static final int SLIDER_HEIGHT = 20;
    private static final int HEALTH_Y = 30;
    private static final int HEALTH_SLIDER_Y = 41;
    private static final int HEALTH_CURRENT_Y = 62;
    private static final int FOOD_Y = 73;
    private static final int FOOD_SLIDER_Y = 84;
    private static final int FOOD_CURRENT_Y = 105;
    private static final int EXPERIENCE_Y = 116;
    private static final int EXPERIENCE_NOTE_Y = 126;
    private static final int NOTE_LINE_HEIGHT = 9;
    private static final int QUICK_BUTTON_Y = 146;
    private static final int QUICK_BUTTON_STEP = 15;
    private static final int QUICK_BUTTON_WIDTH = 84;
    private static final int QUICK_BUTTON_HEIGHT = 14;
    private static final int DEATH_DROP_Y = 177;
    private static final int DEATH_DROP_WIDTH = 70;
    private static final int DEATH_DROP_LINE_HEIGHT = 10;

    private static final int SEPARATOR_LEFT_X = 122;
    private static final int SEPARATOR_RIGHT_X = 304;
    private static final int SEPARATOR_TOP_Y = 14;
    private static final int SEPARATOR_BOTTOM_Y = 200;

    /*
     * Value factors of the client estimate, mirroring the server arithmetic so the summary line matches the settled
     * value instead of only the item part.
     */
    private static final int HEALTH_VALUE_FACTOR = 2;
    private static final double FOOD_LEVEL_MAX = 20.0D;

    private final List<MobListPacket.MobEntry> mobEntries = new ArrayList<MobListPacket.MobEntry>();
    private final Set<UUID> selectedMobs = new HashSet<UUID>();
    private final ScrollBar mobScrollBar = new ScrollBar();
    private final DelayedTooltip mobTooltip = new DelayedTooltip();

    private TableLayout mobLayout;
    private IntSlider healthSlider;
    private IntSlider foodSlider;
    private boolean submitting;
    private int pendingSequence = -1;
    private long submitTime;
    private String status = "";
    private boolean healthChecked;
    private boolean foodChecked;
    private boolean deathDropChecked;
    private int mobRefreshTicks;

    public AltarScreen(AltarMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init()
    {
        super.init();

        /*
         * A resize rebuilds every widget, so the numbers the player already dialled in are carried over.
         */
        int health = this.healthSlider == null ? 0 : this.healthSlider.value();
        int food = this.foodSlider == null ? 0 : this.foodSlider.value();

        addRenderableWidget(Button.builder(Component.translatable(key("altar.offer")), button -> submit())
                .pos(this.leftPos + OFFER_BUTTON_X, this.topPos + BOTTOM_BUTTON_Y)
                .size(BOTTOM_BUTTON_WIDTH, BOTTOM_BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable(key("altar.cancel")), button -> onClose())
                .pos(this.leftPos + CANCEL_BUTTON_X, this.topPos + BOTTOM_BUTTON_Y)
                .size(BOTTOM_BUTTON_WIDTH, BOTTOM_BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable(key("altar.offer_all")), button -> offerAll())
                .pos(this.leftPos + VALUE_X, this.topPos + QUICK_BUTTON_Y)
                .size(QUICK_BUTTON_WIDTH, QUICK_BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable(key("altar.offer_half")), button -> offerHalf())
                .pos(this.leftPos + VALUE_X, this.topPos + QUICK_BUTTON_Y + QUICK_BUTTON_STEP)
                .size(QUICK_BUTTON_WIDTH, QUICK_BUTTON_HEIGHT).build());

        this.healthSlider = addRenderableWidget(new IntSlider(this.leftPos + VALUE_X, this.topPos + HEALTH_SLIDER_Y,
                VALUE_WIDTH, SLIDER_HEIGHT, Component.translatable(key("altar.health")), 0, currentHealth(), health));
        this.foodSlider = addRenderableWidget(new IntSlider(this.leftPos + VALUE_X, this.topPos + FOOD_SLIDER_Y,
                VALUE_WIDTH, SLIDER_HEIGHT, Component.translatable(key("altar.food")), 0, currentFood(), food));

        this.mobScrollBar.setBounds(this.leftPos + MOB_X + MOB_TABLE_WIDTH + MOB_SCROLL_GAP,
                this.topPos + MOB_LIST_Y, ModConstants.TABLE_HEADER_HEIGHT
                        + MOB_VISIBLE_ROWS * ModConstants.TABLE_ROW_HEIGHT);
        this.mobScrollBar.setRange(maximumMobScroll(), MOB_VISIBLE_ROWS);
        this.mobRefreshTicks = 0;
        rebuildMobTable();
        /* The list is asked for here as well as on the interval, so an opened screen fills at once. */
        ClientActions.sendMobListRequest();
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();

        /*
         * The limits follow the player, because another mod may raise the maximum health or the current value itself.
         */
        if (this.healthSlider != null)
        {
            this.healthSlider.setRange(0, currentHealth());
        }

        if (this.foodSlider != null)
        {
            this.foodSlider.setRange(0, currentFood());
        }

        normalizeDeathDrop();

        if (++this.mobRefreshTicks >= ModConstants.TICKS_PER_SECOND)
        {
            this.mobRefreshTicks = 0;
            ClientActions.sendMobListRequest();
        }

        if (this.submitting && System.currentTimeMillis() - this.submitTime > TIMEOUT_MS)
        {
            this.submitting = false;
            this.status = Component.translatable(key("altar.timeout")).getString();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawMobTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    /**
     * Panel, separators, the slot frames of every real slot and the creature table.
     * Slot contents and the cursor stack are drawn by the container screen itself, so nothing here touches an item.
     */
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.fill(this.leftPos, this.topPos, this.leftPos + PANEL_WIDTH, this.topPos + PANEL_HEIGHT, PANEL_COLOR);
        drawSeparators(graphics);
        drawSlotFrames(graphics);
        drawMobList(graphics, mouseX, mouseY);
        drawChecks(graphics);
    }

    /**
     * Every label of the panel, drawn above the slots like a vanilla container screen does with its own labels.
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(this.font, this.title, MOB_X, TITLE_Y, TITLE_COLOR, false);
        graphics.drawString(this.font, clip(Component.translatable(key("altar.summary"),
                Integer.valueOf(countEntries()), Integer.valueOf(countItems())).getString(), MID_TEXT_WIDTH),
                MID_TEXT_X, SUMMARY_Y, TEXT_COLOR, false);
        graphics.drawString(this.font, clip(Component.translatable(key("altar.estimate"),
                Long.valueOf(estimatedValue())).getString(), MID_TEXT_WIDTH), MID_TEXT_X, ESTIMATE_Y, ESTIMATE_COLOR,
                false);
        drawAltarValue(graphics);
        graphics.drawString(this.font, Component.translatable(key("altar.altar")), AltarMenu.OFFERING_X,
                OFFERING_LABEL_Y, LABEL_COLOR, false);
        graphics.drawString(this.font, Component.translatable(key("altar.backpack")), AltarMenu.INVENTORY_X,
                INVENTORY_LABEL_Y, LABEL_COLOR, false);
        drawWrapped(graphics, Component.translatable(key("altar.hint")), MID_TEXT_X, HINT_Y, MID_TEXT_WIDTH,
                HINT_LINE_HEIGHT, LABEL_COLOR);
        drawValueLabels(graphics);
        graphics.drawString(this.font, clip(this.status, STATUS_WIDTH), STATUS_X, STATUS_Y, STATUS_COLOR, false);
        graphics.drawString(this.font, Component.translatable("gui." + ModConstants.MOD_ID + ".author"), AUTHOR_X,
                AUTHOR_Y, AUTHOR_COLOR, false);
    }

    /**
     * The accumulated value of the altar the screen belongs to, read from the status the server pushes, so the player
     * sees what this altar is worth while choosing what to offer. Without a governing altar the line stays away
     * instead of showing a zero that looks like a verdict.
     */
    private void drawAltarValue(GuiGraphics graphics)
    {
        if (!ClientActions.hasAltar())
        {
            return;
        }

        graphics.drawString(this.font, clip(Component.translatable(key("altar.altar_value"),
                Long.valueOf(ClientActions.altarValue())).getString(), MID_TEXT_WIDTH), MID_TEXT_X, ALTAR_VALUE_Y,
                TEXT_COLOR, false);
    }

    /**
     * Adds one extra value line under the vanilla tooltip of a hovered slot.
     * A stack inside the offering grid carries its subtotal, anything else carries its unit value.
     */
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack)
    {
        List<Component> lines = super.getTooltipFromContainerItem(stack);
        boolean insideOffering = this.hoveredSlot != null && this.hoveredSlot.index < AltarMenu.OFFERING_SLOTS;
        appendValueLine(lines, stack, insideOffering);
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (!this.submitting)
        {
            if (this.mobScrollBar.mouseClicked(mouseX, mouseY, button))
            {
                this.mobTooltip.reset();
                return true;
            }

            int mobRow = mobRowAt(mouseX, mouseY);

            if (mobRow >= 0)
            {
                toggleMob(mobRow);
                return true;
            }

            if (insideCheck(mouseX, mouseY, this.leftPos + VALUE_X, this.topPos + HEALTH_Y))
            {
                this.healthChecked = !this.healthChecked;
                return true;
            }

            if (insideCheck(mouseX, mouseY, this.leftPos + VALUE_X, this.topPos + FOOD_Y))
            {
                this.foodChecked = !this.foodChecked;
                return true;
            }

            if (deathDropAvailable()
                    && insideCheck(mouseX, mouseY, this.leftPos + VALUE_X, this.topPos + DEATH_DROP_Y))
            {
                this.deathDropChecked = !this.deathDropChecked;
                return true;
            }
        }

        /* The container slots keep their vanilla behaviour, so a pending request never blocks a stack. */
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (this.mobScrollBar.mouseDragged(mouseX, mouseY, button))
        {
            this.mobTooltip.reset();
            return true;
        }

        /*
         * A container screen swallows drags and never hands them to its widgets, because vanilla only puts buttons in
         * menus. Without this the two number sliders jump on a click but cannot be dragged, so the focused slider gets
         * the drag itself.
         */
        if (this.getFocused() instanceof IntSlider slider && slider.isDraggingValue()
                && slider.mouseDragged(mouseX, mouseY, button, dragX, dragY))
        {
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (this.mobScrollBar.mouseReleased(mouseX, mouseY, button))
        {
            return true;
        }

        if (this.getFocused() instanceof IntSlider slider)
        {
            slider.stopDraggingValue();
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (!this.submitting && insideMobList(mouseX, mouseY) && this.mobScrollBar.isActive())
        {
            this.mobScrollBar.scrollBy(delta > 0.0D ? -1 : 1);
            this.mobTooltip.reset();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    /**
     * Called by the client packet handler while the altar screen is open.
     * The selection survives a refresh by identifier, and identifiers that left the list are dropped.
     */
    public void onMobList(List<MobListPacket.MobEntry> entries)
    {
        this.mobEntries.clear();
        Set<UUID> present = new HashSet<UUID>();

        if (entries != null)
        {
            for (MobListPacket.MobEntry entry : entries)
            {
                if (this.mobEntries.size() >= ModConstants.ALTAR_MAX_MOB_ENTRIES)
                {
                    this.status = Component.translatable(key("altar.mob_limit")).getString();
                    break;
                }

                this.mobEntries.add(entry);
                present.add(entry.id());
            }
        }

        this.selectedMobs.retainAll(present);
        rebuildMobTable();
        this.mobScrollBar.setRange(maximumMobScroll(), MOB_VISIBLE_ROWS);
    }

    /**
     * Called by the client packet handler when the server answers the pending request.
     */
    public void onDrawResult(DrawResultPacket message)
    {
        if (!this.submitting || message.sequence() != this.pendingSequence)
        {
            return;
        }

        this.submitting = false;

        if (message.success())
        {
            onClose();
            return;
        }

        if (ModConstants.REASON_TOO_FREQUENT.equals(message.reasonKey()))
        {
            return;
        }

        this.status = Component.translatable(key("reason." + message.reasonKey())).getString();
    }

    private void offerAll()
    {
        if (this.submitting)
        {
            return;
        }

        this.healthChecked = true;
        this.foodChecked = true;

        if (this.healthSlider != null)
        {
            this.healthSlider.setValue(currentHealth());
        }

        if (this.foodSlider != null)
        {
            this.foodSlider.setValue(currentFood());
        }
    }

    private void offerHalf()
    {
        if (this.submitting)
        {
            return;
        }

        this.healthChecked = true;
        this.foodChecked = true;

        if (this.healthSlider != null)
        {
            this.healthSlider.setValue(Math.max(0, currentHealth() / 2));
        }

        if (this.foodSlider != null)
        {
            this.foodSlider.setValue(Math.max(0, currentFood() / 2));
        }
    }

    private void toggleMob(int index)
    {
        if (index < 0 || index >= this.mobEntries.size())
        {
            return;
        }

        UUID id = this.mobEntries.get(index).id();

        if (!this.selectedMobs.remove(id))
        {
            this.selectedMobs.add(id);
        }
    }

    private void submit()
    {
        if (this.submitting)
        {
            return;
        }

        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.player == null)
        {
            return;
        }

        List<UUID> mobs = selectedMobIds();
        int health = offeredHealth();
        int food = offeredFood();
        boolean allHealth = allHealthSelected();
        boolean deathDrop = this.deathDropChecked && deathDropAvailable();

        if (offeringEmpty() && mobs.isEmpty() && health <= 0 && food <= 0 && !allHealth)
        {
            this.status = Component.translatable(key(numericOfferingEmpty()
                    ? "altar.no_offering" : "altar.empty")).getString();
            return;
        }

        this.pendingSequence = ClientActions.nextSequence();
        this.submitting = true;
        this.submitTime = System.currentTimeMillis();

        /*
         * The item part travels empty on purpose: the server takes the stacks straight out of the altar container, so
         * the grid is the only authority on what the player really offers. The request is sent before the screen
         * closes, therefore the server takes the stacks first and the close only follows it.
         */
        ModNetwork.sendToServer(new DevilsDiceRequestPacket(this.pendingSequence,
                minecraft.player.getGameProfile().getName(), ClientActions.devilsDiceId(),
                new OfferingBundle(new ArrayList<Offering>(), mobs, health, food, allHealth, deathDrop)));
        ClientNotices.sendThrottled(key("altar.waiting"));
        onClose();
    }

    private List<UUID> selectedMobIds()
    {
        List<UUID> result = new ArrayList<UUID>();

        for (MobListPacket.MobEntry entry : this.mobEntries)
        {
            if (this.selectedMobs.contains(entry.id()))
            {
                result.add(entry.id());
            }
        }

        return result;
    }

    /**
     * Rebuilds the frozen header and the column widths of the creature table.
     * The name column takes the leftover width, the health column keeps a fixed pixel width.
     */
    private void rebuildMobTable()
    {
        if (this.font == null)
        {
            return;
        }

        List<String> names = new ArrayList<String>();
        List<String> healths = new ArrayList<String>();

        for (MobListPacket.MobEntry entry : this.mobEntries)
        {
            names.add(entry.name().getString());
            healths.add(mobHealth(entry));
        }

        this.mobLayout = new TableLayout(this.font, MOB_TABLE_WIDTH);
        this.mobLayout.addColumn(Component.translatable(key("altar.mobs")), 0, names, 1);
        this.mobLayout.addColumn(Component.translatable(key("altar.column_health")), MOB_HEALTH_WIDTH, healths);
        this.mobLayout.compute();
    }

    private void drawSeparators(GuiGraphics graphics)
    {
        int top = this.topPos + SEPARATOR_TOP_Y;
        int bottom = this.topPos + SEPARATOR_BOTTOM_Y;
        graphics.fill(this.leftPos + SEPARATOR_LEFT_X, top, this.leftPos + SEPARATOR_LEFT_X + 1, bottom, SEPARATOR_COLOR);
        graphics.fill(this.leftPos + SEPARATOR_RIGHT_X, top, this.leftPos + SEPARATOR_RIGHT_X + 1, bottom,
                SEPARATOR_COLOR);
    }

    /**
     * The frame of every real slot, read from the menu so the drawn grid can never drift away from the slots.
     */
    private void drawSlotFrames(GuiGraphics graphics)
    {
        for (Slot slot : this.getMenu().slots)
        {
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_SHADE);
            graphics.fill(x, y, x + 16, y + 16, SLOT_COLOR);
        }
    }

    /**
     * Frozen header, scrolling rows with their selection mark and the draggable scroll bar.
     */
    private void drawMobList(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if (this.mobLayout == null)
        {
            return;
        }

        int listX = this.leftPos + MOB_X;
        int headerY = this.topPos + MOB_LIST_Y;
        int rowsY = headerY + ModConstants.TABLE_HEADER_HEIGHT;
        int hovered = mobRowAt(mouseX, mouseY);
        int visible = Math.min(MOB_VISIBLE_ROWS,
                Math.max(0, this.mobEntries.size() - this.mobScrollBar.value()));

        graphics.fill(listX, headerY, listX + MOB_TABLE_WIDTH, headerY + ModConstants.TABLE_HEADER_HEIGHT,
                HEADER_COLOR);
        this.mobLayout.drawHeader(graphics, listX, headerY + 2, HEADER_TEXT_COLOR);

        for (int line = 0; line < visible; line++)
        {
            int index = this.mobScrollBar.value() + line;
            int y = rowsY + line * ModConstants.TABLE_ROW_HEIGHT;
            MobListPacket.MobEntry entry = this.mobEntries.get(index);
            boolean selected = this.selectedMobs.contains(entry.id());
            int background = index == hovered ? ROW_HOVER_COLOR : (selected ? ROW_SELECTED_COLOR : LIST_COLOR);

            graphics.fill(listX, y, listX + MOB_TABLE_WIDTH, y + ModConstants.TABLE_ROW_HEIGHT - 1, background);
            this.mobLayout.drawCell(graphics, 0, entry.name().getString(), listX, y + 2,
                    selected ? TEXT_COLOR : LABEL_COLOR);
            this.mobLayout.drawCell(graphics, 1, mobHealth(entry), listX, y + 2,
                    selected ? TEXT_COLOR : LABEL_COLOR);
            /* The selection mark sits in the trailing gutter, so it never covers a cell text. */
            drawCheck(graphics, listX + MOB_TABLE_WIDTH - CHECK_SIZE - 1, y + 1, selected);
        }

        this.mobScrollBar.render(graphics);
    }

    /**
     * Holds the hover tooltip of one creature row back for a moment, then shows the full name and the registry id.
     */
    private void drawMobTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int index = mobRowAt(mouseX, mouseY);

        if (index < 0 || index >= this.mobEntries.size() || this.mobLayout == null)
        {
            this.mobTooltip.render(graphics, this.font, null, null, mouseX, mouseY);
            return;
        }

        MobListPacket.MobEntry entry = this.mobEntries.get(index);
        String name = entry.name().getString();
        String health = mobHealth(entry);
        List<Component> lines = new ArrayList<Component>();

        if (this.mobLayout.isClipped(0, name))
        {
            lines.add(Component.literal(name));
        }

        if (this.mobLayout.isClipped(1, health))
        {
            lines.add(Component.literal(health));
        }

        lines.add(ItemLabels.idTooltip(entry.typeId()));
        this.mobTooltip.render(graphics, this.font, entry.id().toString(), lines, mouseX, mouseY);
    }

    private void drawChecks(GuiGraphics graphics)
    {
        drawCheck(graphics, this.leftPos + VALUE_X, this.topPos + HEALTH_Y, this.healthChecked);
        drawCheck(graphics, this.leftPos + VALUE_X, this.topPos + FOOD_Y, this.foodChecked);

        if (deathDropAvailable())
        {
            drawCheck(graphics, this.leftPos + VALUE_X, this.topPos + DEATH_DROP_Y, this.deathDropChecked);
            drawWrapped(graphics, Component.translatable(key("altar.death_drop")),
                    this.leftPos + VALUE_X + LABEL_OFFSET, this.topPos + DEATH_DROP_Y, DEATH_DROP_WIDTH,
                    DEATH_DROP_LINE_HEIGHT, TEXT_COLOR);
        }
    }

    private void drawCheck(GuiGraphics graphics, int x, int y, boolean checked)
    {
        graphics.fill(x - 1, y - 1, x + CHECK_SIZE + 1, y + CHECK_SIZE + 1, SLOT_SHADE);
        graphics.fill(x, y, x + CHECK_SIZE, y + CHECK_SIZE, SLOT_COLOR);

        if (checked)
        {
            graphics.fill(x + 2, y + 2, x + CHECK_SIZE - 2, y + CHECK_SIZE - 2, CHECK_COLOR);
        }
    }

    private void drawValueLabels(GuiGraphics graphics)
    {
        graphics.drawString(this.font, Component.translatable(key("altar.values")), VALUE_X, VALUE_LABEL_Y, LABEL_COLOR,
                false);
        graphics.drawString(this.font, Component.translatable(key("altar.current"),
                Integer.valueOf(currentHealth())), VALUE_X + LABEL_OFFSET, HEALTH_CURRENT_Y, TEXT_COLOR, false);
        graphics.drawString(this.font, Component.translatable(key("altar.current"),
                Integer.valueOf(currentFood())), VALUE_X + LABEL_OFFSET, FOOD_CURRENT_Y, TEXT_COLOR, false);

        Component experience = Component.translatable(key("altar.experience"));
        graphics.drawString(this.font, experience, VALUE_X, EXPERIENCE_Y, LABEL_COLOR, false);
        graphics.drawString(this.font, Integer.toString(currentLevel()), VALUE_X + this.font.width(experience) + 3,
                EXPERIENCE_Y, TEXT_COLOR, false);
        drawWrapped(graphics, Component.translatable(key("altar.experience_note")), VALUE_X, EXPERIENCE_NOTE_Y,
                VALUE_WIDTH, NOTE_LINE_HEIGHT, LABEL_COLOR);
    }

    private void drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int lineHeight, int color)
    {
        List<FormattedCharSequence> lines = this.font.split(text, width);

        for (int index = 0; index < lines.size(); index++)
        {
            graphics.drawString(this.font, lines.get(index), x, y + index * lineHeight, color, false);
        }
    }

    /**
     * Adds one extra value line under the vanilla tooltip of a hovered stack.
     */
    private void appendValueLine(List<Component> lines, ItemStack stack, boolean insideOffering)
    {
        ValueTableSnapshot snapshot = currentSnapshot();

        if (snapshot == null || stack.isEmpty())
        {
            return;
        }

        ResourceLocation registryKey = ForgeRegistries.ITEMS.getKey(stack.getItem());

        if (registryKey == null)
        {
            return;
        }

        int unit = snapshot.valueOf(registryKey.toString());

        if (insideOffering)
        {
            lines.add(Component.translatable(key("value.subtotal"), Integer.valueOf(unit),
                    Integer.valueOf(stack.getCount()), Integer.valueOf(unit * stack.getCount())));
        }
        else
        {
            lines.add(Component.translatable(key("value.line"), Integer.valueOf(unit)));
        }
    }

    /**
     * The value table known to this client, pushed by the server at join and only refreshed by the carried item.
     */
    private ValueTableSnapshot currentSnapshot()
    {
        ValueTableSnapshot pushed = ClientPacketHandler.valueTable();

        if (pushed != null)
        {
            return pushed;
        }

        Minecraft minecraft = this.minecraft;

        return minecraft == null || minecraft.player == null
                ? null : ValueTableBook.findInInventory(minecraft.player);
    }

    /**
     * Client side estimate of the offering value: the grid stacks, the selected creatures and the numeric offering,
     * with the experience bonus once any numeric offering is selected. The item part uses the public value table; the
     * server still settles the real value, but the panel only shows the estimate instead of restating that rule.
     */
    private long estimatedValue()
    {
        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.player == null)
        {
            return 0L;
        }

        ValueTableSnapshot snapshot = currentSnapshot();

        if (snapshot == null)
        {
            return 0L;
        }

        long total = 0L;

        for (ItemStack stack : offeringStacks())
        {
            if (stack.isEmpty())
            {
                continue;
            }

            ResourceLocation registryKey = ForgeRegistries.ITEMS.getKey(stack.getItem());

            if (registryKey != null)
            {
                total += (long) snapshot.valueOf(registryKey.toString()) * (long) stack.getCount();
            }
        }

        for (MobListPacket.MobEntry entry : this.mobEntries)
        {
            if (this.selectedMobs.contains(entry.id()))
            {
                total += snapshot.entityValueOf(entry.typeId());
            }
        }

        total += (long) offeredHealth() * HEALTH_VALUE_FACTOR + (long) offeredFood();

        if (numericOfferingSelected())
        {
            total += experienceValue();
        }

        return Math.min(total, ModConstants.MAX_TOTAL_VALUE);
    }

    /**
     * Experience levels are offered in full once any numeric offering is selected, scaled by the health and food ratio.
     */
    private long experienceValue()
    {
        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.player == null)
        {
            return 0L;
        }

        double healthRatio = minecraft.player.getMaxHealth() <= 0.0F
                ? 0.0D : minecraft.player.getHealth() / (double) minecraft.player.getMaxHealth();
        double foodRatio = minecraft.player.getFoodData().getFoodLevel() / FOOD_LEVEL_MAX;
        return (long) (minecraft.player.experienceLevel * (1.0D + healthRatio * foodRatio));
    }

    /**
     * The stacks currently in the offering grid, read from the container the menu synchronises.
     */
    private List<ItemStack> offeringStacks()
    {
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        Container container = this.getMenu().offerings();
        int size = Math.min(AltarMenu.OFFERING_SLOTS, container.getContainerSize());

        for (int index = 0; index < size; index++)
        {
            stacks.add(container.getItem(index));
        }

        return stacks;
    }

    private boolean offeringEmpty()
    {
        for (ItemStack stack : offeringStacks())
        {
            if (!stack.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    private int countEntries()
    {
        int result = 0;

        for (ItemStack stack : offeringStacks())
        {
            if (!stack.isEmpty())
            {
                result++;
            }
        }

        return result;
    }

    private int countItems()
    {
        int result = 0;

        for (ItemStack stack : offeringStacks())
        {
            result += stack.getCount();
        }

        return result;
    }

    /**
     * Current health as whole points, rounded up the same way the server validates the offering.
     */
    private int currentHealth()
    {
        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.player == null)
        {
            return 0;
        }

        return (int) Math.ceil(minecraft.player.getHealth());
    }

    private int currentFood()
    {
        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.player == null)
        {
            return 0;
        }

        return minecraft.player.getFoodData().getFoodLevel();
    }

    private int currentLevel()
    {
        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.player == null)
        {
            return 0;
        }

        return minecraft.player.experienceLevel;
    }

    private int offeredHealth()
    {
        return this.healthChecked && this.healthSlider != null ? this.healthSlider.value() : 0;
    }

    private int offeredFood()
    {
        return this.foodChecked && this.foodSlider != null ? this.foodSlider.value() : 0;
    }

    /**
     * A full health sacrifice, which the death drop option and the server both require to be complete.
     */
    private boolean allHealthSelected()
    {
        return this.healthChecked && this.healthSlider != null && currentHealth() > 0
                && this.healthSlider.value() >= currentHealth();
    }

    private boolean numericOfferingSelected()
    {
        return offeredHealth() > 0 || offeredFood() > 0 || allHealthSelected();
    }

    /**
     * Whether a numeric checkbox is ticked while its slider stays at zero, so nothing would be offered.
     */
    private boolean numericOfferingEmpty()
    {
        return (this.healthChecked && this.healthSlider != null && this.healthSlider.value() <= 0)
                || (this.foodChecked && this.foodSlider != null && this.foodSlider.value() <= 0);
    }

    /**
     * The death drop option needs a full health sacrifice and a world that keeps no inventory.
     */
    private boolean deathDropAvailable()
    {
        return !keepInventory() && allHealthSelected();
    }

    private void normalizeDeathDrop()
    {
        if (!deathDropAvailable())
        {
            this.deathDropChecked = false;
        }
    }

    private boolean keepInventory()
    {
        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.level == null)
        {
            return false;
        }

        return minecraft.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    }

    private static String mobHealth(MobListPacket.MobEntry entry)
    {
        return Component.translatable(key("altar.mob_health"), formatNumber(entry.health()),
                formatNumber(entry.maxHealth())).getString();
    }

    private static String formatNumber(float value)
    {
        return value == Math.floor((double) value)
                ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.1f", Float.valueOf(value));
    }

    private int maximumMobScroll()
    {
        return Math.max(0, this.mobEntries.size() - MOB_VISIBLE_ROWS);
    }

    private boolean insideMobList(double mouseX, double mouseY)
    {
        return mouseX >= this.leftPos + MOB_X && mouseX <= this.leftPos + MOB_X + MOB_WIDTH
                && mouseY >= this.topPos + MOB_LIST_Y
                && mouseY <= this.topPos + MOB_LIST_Y + ModConstants.TABLE_HEADER_HEIGHT
                        + MOB_VISIBLE_ROWS * ModConstants.TABLE_ROW_HEIGHT;
    }

    private boolean insideCheck(double mouseX, double mouseY, int x, int y)
    {
        return mouseX >= x && mouseX < x + CHECK_SIZE && mouseY >= y && mouseY < y + CHECK_SIZE;
    }

    /**
     * Row index under the pointer, or minus one. The frozen header row is never a row of the list.
     */
    private int mobRowAt(double mouseX, double mouseY)
    {
        if (!insideMobList(mouseX, mouseY))
        {
            return -1;
        }

        int rowsY = this.topPos + MOB_LIST_Y + ModConstants.TABLE_HEADER_HEIGHT;

        if (mouseY < rowsY)
        {
            return -1;
        }

        int line = (int) Math.floor((mouseY - rowsY) / ModConstants.TABLE_ROW_HEIGHT);

        if (line < 0 || line >= MOB_VISIBLE_ROWS)
        {
            return -1;
        }

        int index = this.mobScrollBar.value() + line;
        return index >= 0 && index < this.mobEntries.size() ? index : -1;
    }

    private String clip(String text, int maxWidth)
    {
        if (this.font.width(text) <= maxWidth)
        {
            return text;
        }

        return this.font.plainSubstrByWidth(text, Math.max(1, maxWidth - this.font.width("..."))) + "...";
    }

    private static String key(String suffix)
    {
        return "gui." + ModConstants.MOD_ID + "." + suffix;
    }
}
