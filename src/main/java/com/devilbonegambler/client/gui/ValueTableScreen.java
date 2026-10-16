/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.ValueTableBook;
import com.devilbonegambler.util.ValueTableSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Searchable view of the offering value table.
 * A row shows the localised name instead of the raw id, because the id is meaningless to a player, and the search
 * box matches both the localised name and the id, so a player who knows an id still finds its row.
 * The rows come from the snapshot the server pushed, so the screen never reads a config file itself.
 * The table follows the shared list rules: a frozen header row, fixed column widths, centred and clipped cells, a
 * draggable scroll bar and a hover tooltip that waits before it appears.
 * The revision of the snapshot stays an internal detail of the optimistic lock and is never shown.
 */
public class ValueTableScreen extends Screen
{
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 216;
    private static final int SLOT = 16;

    private static final int PANEL_COLOR = 0xC0101010;
    private static final int SLOT_SHADE = 0xFF2A2A2A;
    private static final int LIST_COLOR = 0x20000000;
    private static final int HEADER_COLOR = 0x30FFFFFF;
    private static final int ROW_HOVER_COLOR = 0x30FFFFFF;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int AUTHOR_COLOR = 0x707070;
    private static final int TEXT_COLOR = 0xE0E0E0;
    private static final int HEADER_TEXT_COLOR = 0xC0C0C0;
    private static final int ENTITY_COLOR = 0xB0C4DE;

    private static final int SEARCH_X = 8;
    private static final int SEARCH_Y = 20;
    private static final int SEARCH_WIDTH = 148;
    private static final int SEARCH_HEIGHT = 16;
    private static final int BUTTON_HEIGHT = 18;
    private static final int CHAPTER_X = 162;
    private static final int CHAPTER_WIDTH = 78;
    private static final int CLOSE_WIDTH = 50;

    private static final int LIST_X = 8;
    private static final int LIST_Y = 44;
    private static final int LIST_WIDTH = 300;
    private static final int ICON_WIDTH = SLOT + 2;
    private static final int MATCHES_Y = 190;
    private static final int HINT_Y = 202;
    private static final int AUTHOR_Y = 202;

    private static final String KEY_PREFIX = "gui." + ModConstants.MOD_ID + ".value.";

    private final ValueTableSnapshot snapshot;
    private final List<Row> rows = new ArrayList<Row>();
    private final ScrollBar scrollBar = new ScrollBar();
    private final DelayedTooltip tooltip = new DelayedTooltip();
    private List<Row> filtered = new ArrayList<Row>();
    private TableLayout layout;
    private int chapter;
    private int leftPos;
    private int topPos;
    private EditBox search;
    private Button chapterButton;

    public ValueTableScreen(ValueTableSnapshot snapshot)
    {
        super(Component.translatable(ValueTableBook.BOOK_TITLE_KEY));
        this.snapshot = snapshot == null ? ValueTableSnapshot.empty() : snapshot;
    }

    @Override
    protected void init()
    {
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;
        this.buildRows();

        this.search = new EditBox(this.font, this.leftPos + SEARCH_X, this.topPos + SEARCH_Y, SEARCH_WIDTH,
                SEARCH_HEIGHT, Component.translatable(KEY_PREFIX + "search"));
        this.search.setMaxLength(ModConstants.VALUE_TABLE_SEARCH_MAX);
        this.search.setHint(Component.translatable(KEY_PREFIX + "search"));
        this.search.setResponder(this::applyFilter);
        this.addRenderableWidget(this.search);

        this.chapterButton = this.addRenderableWidget(Button.builder(this.chapterLabel(),
                button -> this.cycleChapter()).bounds(this.leftPos + CHAPTER_X, this.topPos + SEARCH_Y,
                        CHAPTER_WIDTH, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.translatable(KEY_PREFIX + "close"), button -> this.onClose())
                .bounds(this.leftPos + PANEL_WIDTH - 8 - CLOSE_WIDTH, this.topPos + SEARCH_Y - 1, CLOSE_WIDTH,
                        BUTTON_HEIGHT).build());

        this.scrollBar.setBounds(this.leftPos + LIST_X + LIST_WIDTH + 2, this.topPos + LIST_Y
                + ModConstants.TABLE_HEADER_HEIGHT, ModConstants.VALUE_TABLE_SCREEN_ROWS
                        * ModConstants.TABLE_ROW_HEIGHT);
        this.applyFilter("");
        this.setInitialFocus(this.search);
    }

    /**
     * Builds one row list per chapter, then flattens the chapters the current filter selects.
     * An item row carries the item icon, a creature row carries its spawn egg when the creature has one.
     */
    private void buildRows()
    {
        this.rows.clear();

        for (Map.Entry<String, Integer> entry : this.snapshot.values().entrySet())
        {
            ItemStack stack = ItemLabels.itemStackOf(entry.getKey());
            this.rows.add(new Row(stack, ItemLabels.itemName(entry.getKey()), entry.getKey(),
                    entry.getValue().intValue(), false));
        }

        for (Map.Entry<String, Integer> entry : this.snapshot.entities().entrySet())
        {
            EntityType<?> type = ItemLabels.entityTypeOf(entry.getKey());
            this.rows.add(new Row(spawnEggOf(type), ItemLabels.entityName(entry.getKey()), entry.getKey(),
                    entry.getValue().intValue(), true));
        }

        this.rows.sort((first, second) -> first.name().compareToIgnoreCase(second.name()));
    }

    /**
     * Spawn eggs follow a fixed naming convention, so the icon is looked up by id instead of through the
     * deprecated spawn egg lookup.
     */
    private ItemStack spawnEggOf(EntityType<?> type)
    {
        if (type == null)
        {
            return ItemStack.EMPTY;
        }

        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);

        if (key == null)
        {
            return ItemStack.EMPTY;
        }

        ResourceLocation eggKey = ResourceLocation.tryBuild(key.getNamespace(), key.getPath() + "_spawn_egg");

        if (eggKey == null || !ForgeRegistries.ITEMS.containsKey(eggKey))
        {
            return ItemStack.EMPTY;
        }

        return new ItemStack(ForgeRegistries.ITEMS.getValue(eggKey));
    }

    /**
     * Locks the column widths of the whole table, so the header and every row line up on the same grid.
     */
    private void rebuildLayout()
    {
        List<String> names = new ArrayList<String>();
        List<String> values = new ArrayList<String>();

        for (Row row : this.filtered)
        {
            names.add(row.name());
            values.add(Integer.toString(row.value()));
        }

        this.layout = new TableLayout(this.font, LIST_WIDTH);
        this.layout.addColumn(Component.empty(), ICON_WIDTH, new ArrayList<String>());
        this.layout.addColumn(Component.translatable(KEY_PREFIX + "column_name"), 0, names);
        this.layout.addColumn(Component.translatable(KEY_PREFIX + "column_value"), 0, values);
        this.layout.compute();
    }

    /**
     * Filters on the localised name and on the raw id, so both a readable query and a known id work.
     */
    private void applyFilter(String query)
    {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Row> result = new ArrayList<Row>();

        for (Row row : this.rows)
        {
            if (!this.chapterAllows(row))
            {
                continue;
            }

            if (needle.isEmpty() || row.name().toLowerCase(Locale.ROOT).contains(needle)
                    || row.id().toLowerCase(Locale.ROOT).contains(needle))
            {
                result.add(row);
            }
        }

        this.filtered = result;
        this.rebuildLayout();
        this.scrollBar.setRange(Math.max(0, this.filtered.size() - ModConstants.VALUE_TABLE_SCREEN_ROWS),
                ModConstants.VALUE_TABLE_SCREEN_ROWS);
        this.scrollBar.setValue(0);
        this.tooltip.reset();
    }

    private boolean chapterAllows(Row row)
    {
        if (this.chapter == ModConstants.VALUE_TABLE_CHAPTER_ALL)
        {
            return true;
        }

        return this.chapter == ModConstants.VALUE_TABLE_CHAPTER_ITEM ? !row.entity() : row.entity();
    }

    private void cycleChapter()
    {
        this.chapter = (this.chapter + 1) % ModConstants.VALUE_TABLE_CHAPTER_COUNT;
        this.chapterButton.setMessage(this.chapterLabel());
        this.applyFilter(this.search == null ? "" : this.search.getValue());
    }

    private Component chapterLabel()
    {
        String suffix = this.chapter == ModConstants.VALUE_TABLE_CHAPTER_ITEM ? "chapter_item"
                : this.chapter == ModConstants.VALUE_TABLE_CHAPTER_ENTITY ? "chapter_entity" : "chapter_all";
        return Component.translatable(KEY_PREFIX + suffix);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(graphics);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + PANEL_WIDTH, this.topPos + PANEL_HEIGHT, PANEL_COLOR);
        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 8, TITLE_COLOR);
        this.drawTable(graphics, mouseX, mouseY);
        this.drawFooter(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.drawTooltip(graphics, mouseX, mouseY);
    }

    /**
     * Header row, scrolling rows and the scroll bar; the header never moves with the scroll position.
     */
    private void drawTable(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int listX = this.leftPos + LIST_X;
        int headerY = this.topPos + LIST_Y;
        int rowsY = headerY + ModConstants.TABLE_HEADER_HEIGHT;
        int visible = Math.min(ModConstants.VALUE_TABLE_SCREEN_ROWS, this.filtered.size());

        graphics.fill(listX, headerY, listX + LIST_WIDTH, headerY + ModConstants.TABLE_HEADER_HEIGHT, HEADER_COLOR);
        this.layout.drawHeader(graphics, listX, headerY + 2, HEADER_TEXT_COLOR);
        graphics.fill(listX, rowsY, listX + LIST_WIDTH, rowsY + ModConstants.VALUE_TABLE_SCREEN_ROWS
                * ModConstants.TABLE_ROW_HEIGHT, LIST_COLOR);

        for (int index = 0; index < visible; index++)
        {
            Row row = this.filtered.get(this.scrollBar.value() + index);
            int y = rowsY + index * ModConstants.TABLE_ROW_HEIGHT;

            if (this.hoveredRow(listX, rowsY, mouseX, mouseY) == this.scrollBar.value() + index)
            {
                graphics.fill(listX, y, listX + LIST_WIDTH, y + ModConstants.TABLE_ROW_HEIGHT, ROW_HOVER_COLOR);
            }

            if (!row.stack().isEmpty())
            {
                int iconX = listX + this.layout.x(0) + 1;
                graphics.fill(iconX, y, iconX + SLOT, y + SLOT - 1, SLOT_SHADE);
                graphics.renderItem(row.stack(), iconX, y - 1);
            }

            this.layout.drawCell(graphics, 1, row.name(), listX, y + 2, row.entity() ? ENTITY_COLOR : TEXT_COLOR);
            this.layout.drawCell(graphics, 2, Integer.toString(row.value()), listX, y + 2, TEXT_COLOR);
        }

        this.scrollBar.render(graphics);
    }

    /**
     * Index of the row under the pointer, or -1 when the pointer is outside the row area.
     */
    private int hoveredRow(int listX, int rowsY, int mouseX, int mouseY)
    {
        if (mouseX < listX || mouseX >= listX + LIST_WIDTH || mouseY < rowsY)
        {
            return -1;
        }

        int index = (mouseY - rowsY) / ModConstants.TABLE_ROW_HEIGHT;

        if (index < 0 || index >= ModConstants.VALUE_TABLE_SCREEN_ROWS)
        {
            return -1;
        }

        int rowIndex = this.scrollBar.value() + index;
        return rowIndex < this.filtered.size() ? rowIndex : -1;
    }

    /**
     * The raw id goes to the hover tooltip, so a row can still be traced back to its configuration entry, and a name
     * that had to be clipped is repeated in full.
     */
    private void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int listX = this.leftPos + LIST_X;
        int rowsY = this.topPos + LIST_Y + ModConstants.TABLE_HEADER_HEIGHT;
        int index = this.hoveredRow(listX, rowsY, mouseX, mouseY);

        if (index < 0)
        {
            this.tooltip.render(graphics, this.font, null, null, mouseX, mouseY);
            return;
        }

        Row row = this.filtered.get(index);
        List<Component> lines = new ArrayList<Component>();

        if (this.layout.isClipped(1, row.name()))
        {
            lines.add(Component.literal(row.name()));
        }

        lines.add(ItemLabels.idTooltip(row.id()));
        this.tooltip.render(graphics, this.font, row.id(), lines, mouseX, mouseY);
    }

    private void drawFooter(GuiGraphics graphics)
    {
        Component count = Component.translatable(KEY_PREFIX + "matches", Integer.valueOf(this.filtered.size()));
        graphics.drawString(this.font, count, this.leftPos + LIST_X, this.topPos + MATCHES_Y, LABEL_COLOR);
        graphics.drawString(this.font, Component.translatable(KEY_PREFIX + "search_hint"),
                this.leftPos + LIST_X, this.topPos + HINT_Y, LABEL_COLOR);

        Component author = Component.translatable("gui." + ModConstants.MOD_ID + ".author");
        graphics.drawString(this.font, author, this.leftPos + PANEL_WIDTH - 8 - this.font.width(author),
                this.topPos + AUTHOR_Y, AUTHOR_COLOR);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (!this.scrollBar.isActive())
        {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        this.scrollBar.scrollBy(delta > 0 ? -1 : 1);
        this.tooltip.reset();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (this.scrollBar.mouseClicked(mouseX, mouseY, button))
        {
            this.tooltip.reset();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (this.scrollBar.mouseDragged(mouseX, mouseY, button))
        {
            this.tooltip.reset();
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (this.scrollBar.mouseReleased(mouseX, mouseY, button))
        {
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    private record Row(ItemStack stack, String name, String id, int value, boolean entity)
    {
    }
}
