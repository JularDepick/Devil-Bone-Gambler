/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.draw.Offering;
import com.devilbonegambler.draw.OfferingBundle;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.DevilsDiceRequestPacket;
import com.devilbonegambler.network.packet.DrawResultPacket;
import com.devilbonegambler.network.packet.MobListPacket;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.ItemValuation;
import com.devilbonegambler.util.ValueTableBook;
import com.devilbonegambler.client.ClientPacketHandler;
import com.devilbonegambler.util.ValueTableSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The offering screen of the devil's dice, laid out in three columns.
 * The left column lists the creatures inside the altar range, the middle column keeps the read-only player
 * inventory and the 3x3 altar slots, and the right column carries the numeric offerings.
 * The player inventory is drawn read-only: the screen never registers a slot, so there is no path that could
 * change the real inventory. The altar itself is a pure client side staging list validated by the server on submit.
 */
public class AltarScreen extends Screen
{
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 220;
    private static final int SLOT = 18;
    private static final int PANEL_COLOR = 0xC0101010;
    private static final int SLOT_COLOR = 0xFF373737;
    private static final int SLOT_SHADE = 0xFF2A2A2A;
    private static final int HIGHLIGHT_COLOR = 0x80FFFFFF;
    private static final int LIST_COLOR = 0x20000000;
    private static final int ROW_HOVER_COLOR = 0x30FFFFFF;
    private static final int CHECK_COLOR = 0xFF7FC97F;
    private static final int SEPARATOR_COLOR = 0x40FFFFFF;
    private static final int SCROLL_TRACK_COLOR = 0x40000000;
    private static final int SCROLL_THUMB_COLOR = 0x80FFFFFF;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int STATUS_COLOR = 0xFF8080;
    private static final int AUTHOR_COLOR = 0x707070;
    private static final int TEXT_COLOR = 0xE0E0E0;
    private static final long TIMEOUT_MS = 10000L;

    private static final int LABEL_OFFSET = 14;
    private static final int CHECK_SIZE = 9;

    private static final int MOB_X = 8;
    private static final int MOB_WIDTH = 110;
    private static final int MOB_LABEL_Y = 20;
    private static final int MOB_LIST_Y = 32;
    private static final int MOB_ROW_HEIGHT = 11;
    private static final int MOB_VISIBLE_ROWS = 12;
    private static final int MOB_TEXT_WIDTH = 92;
    private static final int HINT_Y = 166;
    private static final int HINT_LINE_HEIGHT = 10;

    private static final int INVENTORY_X = 132;
    private static final int INVENTORY_LABEL_Y = 18;
    private static final int INVENTORY_Y = 28;
    private static final int HOTBAR_Y = 82;
    private static final int ALTAR_X = 132;
    private static final int ALTAR_LABEL_Y = 104;
    private static final int ALTAR_Y = 114;
    private static final int SUMMARY_X = 190;
    private static final int SUMMARY_Y = 104;
    private static final int SUMMARY_WIDTH = 108;
    private static final int SUMMARY_LINE_HEIGHT = 11;
    private static final int ESTIMATE_COLOR = 0xD8B860;

    private static final int VALUE_X = 308;
    private static final int VALUE_WIDTH = 84;
    private static final int VALUE_LABEL_Y = 20;
    private static final int FIELD_X = 356;
    private static final int FIELD_WIDTH = 36;
    private static final int FIELD_HEIGHT = 14;
    private static final int FIELD_MAX_LENGTH = 5;
    private static final int HEALTH_Y = 34;
    private static final int HEALTH_FIELD_Y = 33;
    private static final int HEALTH_CURRENT_Y = 48;
    private static final int FOOD_Y = 60;
    private static final int FOOD_FIELD_Y = 59;
    private static final int FOOD_CURRENT_Y = 74;
    private static final int EXPERIENCE_Y = 86;
    private static final int EXPERIENCE_NOTE_Y = 96;
    private static final int NOTE_LINE_HEIGHT = 9;
    private static final int QUICK_BUTTON_Y = 116;
    private static final int QUICK_BUTTON_STEP = 16;
    private static final int QUICK_BUTTON_WIDTH = 84;
    private static final int QUICK_BUTTON_HEIGHT = 14;
    private static final int DEATH_DROP_Y = 150;
    private static final int DEATH_DROP_WIDTH = 70;
    private static final int DEATH_DROP_LINE_HEIGHT = 10;

    private static final int SEPARATOR_LEFT_X = 122;
    private static final int SEPARATOR_RIGHT_X = 304;
    private static final int SEPARATOR_TOP_Y = 18;
    private static final int SEPARATOR_BOTTOM_Y = 194;

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

    private final List<ItemStack> altar = new ArrayList<ItemStack>();
    private final List<Component> tooltipLines = new ArrayList<Component>();
    private final List<MobListPacket.MobEntry> mobEntries = new ArrayList<MobListPacket.MobEntry>();
    private final Set<UUID> selectedMobs = new HashSet<UUID>();

    private int leftPos;
    private int topPos;
    private boolean submitting;
    private int pendingSequence = -1;
    private long submitTime;
    private String status = "";
    private int mobScroll;
    private boolean healthChecked;
    private boolean foodChecked;
    private boolean deathDropChecked;
    private EditBox healthField;
    private EditBox foodField;

    public AltarScreen()
    {
        super(Component.translatable(key("altar.title")));

        for (int index = 0; index < ModConstants.ALTAR_SLOTS; index++)
        {
            this.altar.add(ItemStack.EMPTY);
        }
    }

    @Override
    protected void init()
    {
        super.init();
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;

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

        this.healthField = addValueField(HEALTH_FIELD_Y);
        this.foodField = addValueField(FOOD_FIELD_Y);
    }

    @Override
    public void tick()
    {
        super.tick();
        normalizeDeathDrop();

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
        graphics.fill(this.leftPos, this.topPos, this.leftPos + PANEL_WIDTH, this.topPos + PANEL_HEIGHT, PANEL_COLOR);
        drawSeparators(graphics);
        drawMobList(graphics, mouseX, mouseY);
        drawValueChecks(graphics);
        drawInventorySlots(graphics, mouseX, mouseY);
        drawAltarSlots(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawLabels(graphics);
        drawTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (this.submitting)
        {
            return super.mouseClicked(mouseX, mouseY, button);
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

        if (deathDropAvailable() && insideCheck(mouseX, mouseY, this.leftPos + VALUE_X, this.topPos + DEATH_DROP_Y))
        {
            this.deathDropChecked = !this.deathDropChecked;
            return true;
        }

        int altarIndex = altarIndexAt(mouseX, mouseY);

        if (altarIndex >= 0)
        {
            takeBack(altarIndex, button == 1);
            return true;
        }

        int inventoryIndex = inventoryIndexAt(mouseX, mouseY);

        if (inventoryIndex >= 0)
        {
            offerFrom(inventoryIndex, button == 1);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (!this.submitting && insideMobList(mouseX, mouseY))
        {
            int maximum = maximumMobScroll();

            if (delta > 0.0D)
            {
                this.mobScroll = Math.max(0, this.mobScroll - 1);
            }
            else if (delta < 0.0D)
            {
                this.mobScroll = Math.min(maximum, this.mobScroll + 1);
            }

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
        this.mobScroll = Math.min(this.mobScroll, maximumMobScroll());
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

        this.status = Component.translatable(key("reason." + message.reasonKey())).getString();
    }

    private EditBox addValueField(int offsetY)
    {
        EditBox box = new EditBox(this.font, this.leftPos + FIELD_X, this.topPos + offsetY, FIELD_WIDTH,
                FIELD_HEIGHT, Component.empty());
        box.setMaxLength(FIELD_MAX_LENGTH);
        box.setFilter(value -> value.matches("[0-9]*"));
        box.setValue("0");
        box.setResponder(value -> normalizeDeathDrop());
        addRenderableWidget(box);
        return box;
    }

    private void offerAll()
    {
        if (this.submitting)
        {
            return;
        }

        this.healthChecked = true;
        this.foodChecked = true;
        setFieldValue(this.healthField, currentHealth());
        setFieldValue(this.foodField, currentFood());
    }

    private void offerHalf()
    {
        if (this.submitting)
        {
            return;
        }

        this.healthChecked = true;
        this.foodChecked = true;
        setFieldValue(this.healthField, Math.max(0, currentHealth() / 2));
        setFieldValue(this.foodField, Math.max(0, currentFood() / 2));
    }

    private void toggleMob(int index)
    {
        MobListPacket.MobEntry entry = this.mobEntries.get(index);

        if (entry == null)
        {
            return;
        }

        if (!this.selectedMobs.remove(entry.id()))
        {
            this.selectedMobs.add(entry.id());
        }
    }

    private void submit()
    {
        if (this.submitting)
        {
            return;
        }

        List<Offering> offerings = collectOfferings();
        List<UUID> mobs = selectedMobIds();
        int health = offeredHealth();
        int food = offeredFood();
        boolean allHealth = this.healthChecked && healthInput() >= currentHealth();
        boolean deathDrop = this.deathDropChecked && deathDropAvailable();

        if (offerings.isEmpty() && mobs.isEmpty() && health <= 0 && food <= 0 && !allHealth)
        {
            this.status = Component.translatable(key(numericOfferingEmpty()
                    ? "altar.no_offering" : "altar.empty")).getString();
            return;
        }

        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.player == null)
        {
            return;
        }

        this.pendingSequence = ClientActions.nextSequence();
        this.submitting = true;
        this.submitTime = System.currentTimeMillis();
        this.status = Component.translatable(key("altar.waiting")).getString();

        ModNetwork.sendToServer(new DevilsDiceRequestPacket(this.pendingSequence,
                minecraft.player.getGameProfile().getName(), ClientActions.devilsDiceId(),
                new OfferingBundle(offerings, mobs, health, food, allHealth, deathDrop)));
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

    private List<Offering> collectOfferings()
    {
        List<Offering> result = new ArrayList<Offering>();

        for (ItemStack stack : this.altar)
        {
            if (stack.isEmpty())
            {
                continue;
            }

            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());

            if (key == null)
            {
                continue;
            }

            /*
             * One entry per stack, so the damage and the enchantment levels travel to the server and the
             * value can depend on the instance rather than on the registry name alone.
             */
            result.add(new Offering(key.toString(), stack.getCount(), stack.getDamageValue(),
                    ItemValuation.enchantLevelSum(stack)));
        }

        return result;
    }

    private void offerFrom(int inventoryIndex, boolean wholeStack)
    {
        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.player == null)
        {
            return;
        }

        ItemStack source = minecraft.player.getInventory().getItem(inventoryIndex);

        if (source.isEmpty())
        {
            return;
        }

        int amount = wholeStack ? source.getCount() : 1;
        int remaining = insert(source, amount);

        if (remaining >= amount)
        {
            this.status = Component.translatable(key("altar.full")).getString();
        }
        else
        {
            this.status = "";
        }
    }

    /**
     * Returns how many items could not be placed into the altar.
     */
    private int insert(ItemStack source, int amount)
    {
        int remaining = amount;

        for (ItemStack slot : this.altar)
        {
            if (remaining <= 0)
            {
                break;
            }

            if (slot.isEmpty() || !ItemStack.isSameItemSameTags(slot, source))
            {
                continue;
            }

            int space = slot.getMaxStackSize() - slot.getCount();

            if (space <= 0)
            {
                continue;
            }

            int moved = Math.min(space, remaining);
            slot.grow(moved);
            remaining -= moved;
        }

        for (int index = 0; index < this.altar.size() && remaining > 0; index++)
        {
            if (!this.altar.get(index).isEmpty())
            {
                continue;
            }

            int moved = Math.min(source.getMaxStackSize(), remaining);
            ItemStack copy = source.copy();
            copy.setCount(moved);
            this.altar.set(index, copy);
            remaining -= moved;
        }

        return remaining;
    }

    private void takeBack(int index, boolean wholeStack)
    {
        ItemStack slot = this.altar.get(index);

        if (slot.isEmpty())
        {
            return;
        }

        if (wholeStack)
        {
            this.altar.set(index, ItemStack.EMPTY);
            return;
        }

        slot.shrink(1);

        if (slot.isEmpty())
        {
            this.altar.set(index, ItemStack.EMPTY);
        }
    }

    private void drawSeparators(GuiGraphics graphics)
    {
        int top = this.topPos + SEPARATOR_TOP_Y;
        int bottom = this.topPos + SEPARATOR_BOTTOM_Y;
        graphics.fill(this.leftPos + SEPARATOR_LEFT_X, top, this.leftPos + SEPARATOR_LEFT_X + 1, bottom, SEPARATOR_COLOR);
        graphics.fill(this.leftPos + SEPARATOR_RIGHT_X, top, this.leftPos + SEPARATOR_RIGHT_X + 1, bottom, SEPARATOR_COLOR);
    }

    private void drawMobList(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int listTop = this.topPos + MOB_LIST_Y;
        int listBottom = listTop + MOB_VISIBLE_ROWS * MOB_ROW_HEIGHT;
        graphics.fill(this.leftPos + MOB_X, listTop, this.leftPos + MOB_X + MOB_WIDTH, listBottom, LIST_COLOR);

        int hovered = mobRowAt(mouseX, mouseY);

        for (int line = 0; line < MOB_VISIBLE_ROWS; line++)
        {
            int index = this.mobScroll + line;

            if (index >= this.mobEntries.size())
            {
                break;
            }

            MobListPacket.MobEntry entry = this.mobEntries.get(index);
            int rowY = listTop + line * MOB_ROW_HEIGHT;

            if (index == hovered)
            {
                graphics.fill(this.leftPos + MOB_X, rowY, this.leftPos + MOB_X + MOB_WIDTH, rowY + MOB_ROW_HEIGHT - 1,
                        ROW_HOVER_COLOR);
            }

            boolean selected = this.selectedMobs.contains(entry.id());
            drawCheck(graphics, this.leftPos + MOB_X + 1, rowY + 1, selected);
            graphics.drawString(this.font, clip(mobLabel(entry), MOB_TEXT_WIDTH),
                    this.leftPos + MOB_X + LABEL_OFFSET, rowY + 1, selected ? TEXT_COLOR : LABEL_COLOR, false);
        }

        drawMobScrollBar(graphics, listTop, listBottom);
    }

    private void drawMobScrollBar(GuiGraphics graphics, int listTop, int listBottom)
    {
        int total = this.mobEntries.size();

        if (total <= MOB_VISIBLE_ROWS)
        {
            return;
        }

        int trackHeight = listBottom - listTop;
        int maximum = maximumMobScroll();
        int thumbHeight = Math.max(6, trackHeight * MOB_VISIBLE_ROWS / total);
        int thumbTop = listTop + (trackHeight - thumbHeight) * this.mobScroll / Math.max(1, maximum);
        int barX = this.leftPos + MOB_X + MOB_WIDTH - 2;
        graphics.fill(barX, listTop, barX + 2, listBottom, SCROLL_TRACK_COLOR);
        graphics.fill(barX, thumbTop, barX + 2, thumbTop + thumbHeight, SCROLL_THUMB_COLOR);
    }

    private void drawValueChecks(GuiGraphics graphics)
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

    private void drawInventorySlots(GuiGraphics graphics, int mouseX, int mouseY)
    {
        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.player == null)
        {
            return;
        }

        Inventory inventory = minecraft.player.getInventory();
        int hovered = inventoryIndexAt(mouseX, mouseY);

        for (int row = 0; row < 3; row++)
        {
            for (int column = 0; column < 9; column++)
            {
                int index = 9 + row * 9 + column;
                drawSlot(graphics, this.leftPos + INVENTORY_X + column * SLOT, this.topPos + INVENTORY_Y + row * SLOT,
                        inventory.getItem(index), hovered == index);
            }
        }

        for (int column = 0; column < 9; column++)
        {
            drawSlot(graphics, this.leftPos + INVENTORY_X + column * SLOT, this.topPos + HOTBAR_Y,
                    inventory.getItem(column), hovered == column);
        }
    }

    private void drawAltarSlots(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int hovered = altarIndexAt(mouseX, mouseY);

        for (int index = 0; index < this.altar.size(); index++)
        {
            int row = index / 3;
            int column = index % 3;
            drawSlot(graphics, this.leftPos + ALTAR_X + column * SLOT, this.topPos + ALTAR_Y + row * SLOT,
                    this.altar.get(index), hovered == index);
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

    private void drawCheck(GuiGraphics graphics, int x, int y, boolean checked)
    {
        graphics.fill(x - 1, y - 1, x + CHECK_SIZE + 1, y + CHECK_SIZE + 1, SLOT_SHADE);
        graphics.fill(x, y, x + CHECK_SIZE, y + CHECK_SIZE, SLOT_COLOR);

        if (checked)
        {
            graphics.fill(x + 2, y + 2, x + CHECK_SIZE - 2, y + CHECK_SIZE - 2, CHECK_COLOR);
        }
    }

    private void drawLabels(GuiGraphics graphics)
    {
        graphics.drawString(this.font, this.title, this.leftPos + MOB_X, this.topPos + 6, TITLE_COLOR, false);
        graphics.drawString(this.font, Component.translatable(key("altar.mobs")),
                this.leftPos + MOB_X, this.topPos + MOB_LABEL_Y, LABEL_COLOR, false);
        graphics.drawString(this.font, Component.translatable(key("altar.values")),
                this.leftPos + VALUE_X, this.topPos + VALUE_LABEL_Y, LABEL_COLOR, false);
        graphics.drawString(this.font, Component.translatable(key("altar.inventory")),
                this.leftPos + INVENTORY_X, this.topPos + INVENTORY_LABEL_Y, LABEL_COLOR, false);
        graphics.drawString(this.font, Component.translatable(key("altar.altar")),
                this.leftPos + ALTAR_X, this.topPos + ALTAR_LABEL_Y, LABEL_COLOR, false);
        graphics.drawString(this.font, clip(Component.translatable(key("altar.summary"),
                Integer.valueOf(countEntries()), Integer.valueOf(countItems())).getString(), SUMMARY_WIDTH),
                this.leftPos + SUMMARY_X, this.topPos + SUMMARY_Y, TEXT_COLOR, false);
        graphics.drawString(this.font, clip(Component.translatable(key("altar.estimate"),
                Long.valueOf(estimatedValue())).getString(), SUMMARY_WIDTH),
                this.leftPos + SUMMARY_X, this.topPos + SUMMARY_Y + SUMMARY_LINE_HEIGHT, ESTIMATE_COLOR, false);

        drawWrapped(graphics, Component.translatable(key("altar.hint")), this.leftPos + MOB_X,
                this.topPos + HINT_Y, MOB_WIDTH, HINT_LINE_HEIGHT, LABEL_COLOR);
        drawValueLabels(graphics);
        graphics.drawString(this.font, clip(this.status, STATUS_WIDTH), this.leftPos + STATUS_X,
                this.topPos + STATUS_Y, STATUS_COLOR, false);
        graphics.drawString(this.font, Component.translatable("gui." + ModConstants.MOD_ID + ".author"),
                this.leftPos + AUTHOR_X, this.topPos + AUTHOR_Y, AUTHOR_COLOR, false);
    }

    private void drawValueLabels(GuiGraphics graphics)
    {
        graphics.drawString(this.font, Component.translatable(key("altar.health")),
                this.leftPos + VALUE_X + LABEL_OFFSET, this.topPos + HEALTH_Y + 1, LABEL_COLOR, false);
        graphics.drawString(this.font, Component.translatable(key("altar.current"), Integer.valueOf(currentHealth())),
                this.leftPos + VALUE_X + LABEL_OFFSET, this.topPos + HEALTH_CURRENT_Y, TEXT_COLOR, false);
        graphics.drawString(this.font, Component.translatable(key("altar.food")),
                this.leftPos + VALUE_X + LABEL_OFFSET, this.topPos + FOOD_Y + 1, LABEL_COLOR, false);
        graphics.drawString(this.font, Component.translatable(key("altar.current"), Integer.valueOf(currentFood())),
                this.leftPos + VALUE_X + LABEL_OFFSET, this.topPos + FOOD_CURRENT_Y, TEXT_COLOR, false);

        Component experience = Component.translatable(key("altar.experience"));
        graphics.drawString(this.font, experience, this.leftPos + VALUE_X, this.topPos + EXPERIENCE_Y, LABEL_COLOR,
                false);
        graphics.drawString(this.font, Integer.toString(currentLevel()),
                this.leftPos + VALUE_X + this.font.width(experience) + 3, this.topPos + EXPERIENCE_Y, TEXT_COLOR,
                false);
        drawWrapped(graphics, Component.translatable(key("altar.experience_note")), this.leftPos + VALUE_X,
                this.topPos + EXPERIENCE_NOTE_Y, VALUE_WIDTH, NOTE_LINE_HEIGHT, LABEL_COLOR);
    }

    private void drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int lineHeight, int color)
    {
        List<FormattedCharSequence> lines = this.font.split(text, width);

        for (int index = 0; index < lines.size(); index++)
        {
            graphics.drawString(this.font, lines.get(index), x, y + index * lineHeight, color, false);
        }
    }

    private void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.player == null)
        {
            return;
        }

        this.tooltipLines.clear();
        int altarIndex = altarIndexAt(mouseX, mouseY);
        ItemStack stack = ItemStack.EMPTY;
        boolean insideAltar = false;

        if (altarIndex >= 0)
        {
            stack = this.altar.get(altarIndex);
            insideAltar = true;
        }
        else
        {
            int inventoryIndex = inventoryIndexAt(mouseX, mouseY);

            if (inventoryIndex >= 0)
            {
                stack = minecraft.player.getInventory().getItem(inventoryIndex);
            }
        }

        if (stack.isEmpty())
        {
            return;
        }

        this.tooltipLines.addAll(Screen.getTooltipFromItem(minecraft, stack));
        appendValueLine(stack, insideAltar);
        graphics.renderTooltip(this.font, this.tooltipLines, Optional.<net.minecraft.world.inventory.tooltip.TooltipComponent>empty(),
                stack, mouseX, mouseY);
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
     * Client side estimate of the offering value, computed from the public value table only.
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

        for (ItemStack stack : this.altar)
        {
            if (stack.isEmpty())
            {
                continue;
            }

            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());

            if (key != null)
            {
                total += (long) snapshot.valueOf(key.toString()) * (long) stack.getCount();
            }
        }

        return total;
    }

    /**
     * Adds one extra value line under the vanilla tooltip, but only when the player carries a filled value table.
     */
    private void appendValueLine(ItemStack stack, boolean insideAltar)
    {
        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.player == null)
        {
            return;
        }

        ValueTableSnapshot snapshot = currentSnapshot();

        if (snapshot == null)
        {
            return;
        }

        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());

        if (key == null)
        {
            return;
        }

        String itemId = key.toString();
        int unit = snapshot.valueOf(itemId);
        boolean registered = snapshot.isRegistered(itemId);

        if (insideAltar)
        {
            this.tooltipLines.add(Component.translatable(key("value.subtotal"), Integer.valueOf(unit),
                    Integer.valueOf(stack.getCount()), Integer.valueOf(unit * stack.getCount())));
        }
        else if (registered)
        {
            this.tooltipLines.add(Component.translatable(key("value.line"), Integer.valueOf(unit)));
        }
        else
        {
            this.tooltipLines.add(Component.translatable(key("value.line_default"), Integer.valueOf(unit)));
        }
    }

    private int countEntries()
    {
        int result = 0;

        for (ItemStack stack : this.altar)
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

        for (ItemStack stack : this.altar)
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

    private int healthInput()
    {
        return fieldValue(this.healthField);
    }

    private int foodInput()
    {
        return fieldValue(this.foodField);
    }

    private int offeredHealth()
    {
        return this.healthChecked ? healthInput() : 0;
    }

    private int offeredFood()
    {
        return this.foodChecked ? foodInput() : 0;
    }

    /**
     * Whether a numeric checkbox is ticked while its field stays at zero, so nothing would be offered.
     */
    private boolean numericOfferingEmpty()
    {
        return (this.healthChecked && healthInput() <= 0) || (this.foodChecked && foodInput() <= 0);
    }

    /**
     * The death drop option needs a full health sacrifice and a world that keeps no inventory.
     */
    private boolean deathDropAvailable()
    {
        return !keepInventory() && healthInput() >= currentHealth() && healthChecked;
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

    private static void setFieldValue(EditBox box, int value)
    {
        if (box != null)
        {
            box.setValue(Integer.toString(Math.max(0, value)));
        }
    }

    private static int fieldValue(EditBox box)
    {
        if (box == null)
        {
            return 0;
        }

        String raw = box.getValue().trim();

        if (raw.isEmpty())
        {
            return 0;
        }

        try
        {
            return Math.max(0, Integer.parseInt(raw));
        }
        catch (NumberFormatException exception)
        {
            return 0;
        }
    }

    private static String mobLabel(MobListPacket.MobEntry entry)
    {
        return Component.translatable(key("altar.mob_row"), entry.name(),
                formatNumber(entry.health()), formatNumber(entry.maxHealth())).getString();
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
                && mouseY <= this.topPos + MOB_LIST_Y + MOB_VISIBLE_ROWS * MOB_ROW_HEIGHT;
    }

    private boolean insideCheck(double mouseX, double mouseY, int x, int y)
    {
        return mouseX >= x && mouseX < x + CHECK_SIZE && mouseY >= y && mouseY < y + CHECK_SIZE;
    }

    private int mobRowAt(double mouseX, double mouseY)
    {
        if (!insideMobList(mouseX, mouseY))
        {
            return -1;
        }

        int line = (int) Math.floor((mouseY - (this.topPos + MOB_LIST_Y)) / MOB_ROW_HEIGHT);
        int index = this.mobScroll + line;
        return index >= 0 && index < this.mobEntries.size() ? index : -1;
    }

    private int inventoryIndexAt(double mouseX, double mouseY)
    {
        int column = (int) Math.floor((mouseX - (this.leftPos + INVENTORY_X)) / SLOT);
        int row = (int) Math.floor((mouseY - (this.topPos + INVENTORY_Y)) / SLOT);

        if (column >= 0 && column < 9 && row >= 0 && row < 3)
        {
            return 9 + row * 9 + column;
        }

        int hotbarRow = (int) Math.floor((mouseY - (this.topPos + HOTBAR_Y)) / SLOT);

        if (column >= 0 && column < 9 && hotbarRow == 0)
        {
            return column;
        }

        return -1;
    }

    private int altarIndexAt(double mouseX, double mouseY)
    {
        int column = (int) Math.floor((mouseX - (this.leftPos + ALTAR_X)) / SLOT);
        int row = (int) Math.floor((mouseY - (this.topPos + ALTAR_Y)) / SLOT);

        if (column < 0 || column >= 3 || row < 0 || row >= 3)
        {
            return -1;
        }

        int index = row * 3 + column;
        return index < this.altar.size() ? index : -1;
    }

    private String clip(String text, int maxWidth)
    {
        if (this.font.width(text) <= maxWidth)
        {
            return text;
        }

        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < text.length(); index++)
        {
            String candidate = builder.toString() + text.charAt(index);

            if (this.font.width(candidate) > maxWidth - 6)
            {
                break;
            }

            builder.append(text.charAt(index));
        }

        return builder + "...";
    }

    private static String key(String suffix)
    {
        return "gui." + ModConstants.MOD_ID + "." + suffix;
    }
}
