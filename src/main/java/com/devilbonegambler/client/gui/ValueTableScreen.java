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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Searchable view of the offering value table.
 * A row shows the localised name instead of the raw id, because the id is meaningless to a player, and the search
 * box matches both the localised name and the id, so a player who knows an id still finds its row.
 * The rows come from the snapshot the server pushed, so the screen never reads a config file itself.
 */
public class ValueTableScreen extends Screen
{
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 216;
    private static final int ROW_HEIGHT = 12;
    private static final int SLOT = 16;

    private static final int PANEL_COLOR = 0xC0101010;
    private static final int SLOT_SHADE = 0xFF2A2A2A;
    private static final int LIST_COLOR = 0x20000000;
    private static final int ROW_HOVER_COLOR = 0x30FFFFFF;
    private static final int SCROLL_TRACK_COLOR = 0x40000000;
    private static final int SCROLL_THUMB_COLOR = 0x80FFFFFF;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int AUTHOR_COLOR = 0x707070;
    private static final int TEXT_COLOR = 0xE0E0E0;
    private static final int DEFAULT_COLOR = 0x808080;
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
    private static final int LIST_WIDTH = 316;
    private static final int SCROLL_WIDTH = 6;
    private static final int MATCHES_Y = 190;
    private static final int HINT_Y = 202;
    private static final int AUTHOR_Y = 202;

    private static final String KEY_PREFIX = "gui." + ModConstants.MOD_ID + ".value.";

    private final ValueTableSnapshot snapshot;
    private final List<Row> rows = new ArrayList<Row>();
    private List<Row> filtered = new ArrayList<Row>();
    private int scroll;
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
                    entry.getValue().intValue(), false,
                    entry.getValue().intValue() == this.snapshot.defaultValue()));
        }

        for (Map.Entry<String, Integer> entry : this.snapshot.entities().entrySet())
        {
            EntityType<?> type = ItemLabels.entityTypeOf(entry.getKey());
            this.rows.add(new Row(spawnEggOf(type), ItemLabels.entityName(entry.getKey()), entry.getKey(),
                    entry.getValue().intValue(), true,
                    entry.getValue().intValue() == this.snapshot.defaultEntityValue()));
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
        this.scroll = 0;
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
        this.drawHeader(graphics);
        this.drawRows(graphics, mouseX, mouseY);
        this.drawScrollBar(graphics);
        this.drawFooter(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphics graphics)
    {
        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 8, TITLE_COLOR);
        Component revision = Component.translatable(KEY_PREFIX + "revision",
                Integer.valueOf(this.snapshot.revision()));
        graphics.drawString(this.font, revision, this.leftPos + PANEL_WIDTH - 8 - this.font.width(revision),
                this.topPos + 8, LABEL_COLOR);
    }

    private void drawRows(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int listX = this.leftPos + LIST_X;
        int listY = this.topPos + LIST_Y;
        int visible = Math.min(ModConstants.VALUE_TABLE_SCREEN_ROWS, this.filtered.size());
        graphics.fill(listX, listY, listX + LIST_WIDTH, listY + visible * ROW_HEIGHT, LIST_COLOR);

        for (int index = 0; index < visible; index++)
        {
            int rowIndex = this.scroll + index;
            Row row = this.filtered.get(rowIndex);
            int y = listY + index * ROW_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX < listX + LIST_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT;

            if (hovered)
            {
                graphics.fill(listX, y, listX + LIST_WIDTH, y + ROW_HEIGHT, ROW_HOVER_COLOR);
                graphics.renderTooltip(this.font, Component.literal(row.id()), mouseX, mouseY);
            }

            if (!row.stack().isEmpty())
            {
                graphics.fill(listX + 1, y, listX + SLOT, y + SLOT - 1, SLOT_SHADE);
                graphics.renderItem(row.stack(), listX + 1, y - 1);
            }

            int nameColor = row.entity() ? ENTITY_COLOR : TEXT_COLOR;
            graphics.drawString(this.font, this.clip(row.name(), 170), listX + SLOT + 4, y + 2, nameColor);
            graphics.drawString(this.font, this.valueText(row), listX + LIST_WIDTH - 28, y + 2,
                    row.defaulted() ? DEFAULT_COLOR : TEXT_COLOR);
        }
    }

    private Component valueText(Row row)
    {
        return row.defaulted() ? Component.translatable(KEY_PREFIX + "default", Integer.valueOf(row.value()))
                : Component.literal(Integer.toString(row.value()));
    }

    private void drawScrollBar(GuiGraphics graphics)
    {
        if (this.filtered.size() <= ModConstants.VALUE_TABLE_SCREEN_ROWS)
        {
            return;
        }

        int trackX = this.leftPos + LIST_X + LIST_WIDTH + 2;
        int trackY = this.topPos + LIST_Y;
        int trackHeight = ModConstants.VALUE_TABLE_SCREEN_ROWS * ROW_HEIGHT;
        graphics.fill(trackX, trackY, trackX + SCROLL_WIDTH, trackY + trackHeight, SCROLL_TRACK_COLOR);

        int thumbHeight = Math.max(8, trackHeight * ModConstants.VALUE_TABLE_SCREEN_ROWS / this.filtered.size());
        int travel = trackHeight - thumbHeight;
        int maximum = this.filtered.size() - ModConstants.VALUE_TABLE_SCREEN_ROWS;
        int offset = maximum <= 0 ? 0 : travel * this.scroll / maximum;
        graphics.fill(trackX, trackY + offset, trackX + SCROLL_WIDTH, trackY + offset + thumbHeight,
                SCROLL_THUMB_COLOR);
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
        int maximum = Math.max(0, this.filtered.size() - ModConstants.VALUE_TABLE_SCREEN_ROWS);

        if (maximum == 0)
        {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        this.scroll = Math.max(0, Math.min(maximum, this.scroll + (delta > 0 ? -1 : 1)));
        return true;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    private String clip(String text, int width)
    {
        return this.font.width(text) <= width ? text : this.font.plainSubstrByWidth(text, width - 3) + "...";
    }

    private record Row(ItemStack stack, String name, String id, int value, boolean entity, boolean defaulted)
    {
    }
}
