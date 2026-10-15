/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.config.ItemValueConfig;
import com.devilbonegambler.config.NotifyConfig;
import com.devilbonegambler.config.PrizeEntry;
import com.devilbonegambler.config.PrizePoolConfig;
import com.devilbonegambler.config.PrizeRegion;
import com.devilbonegambler.config.ShopConfig;
import com.devilbonegambler.config.ShopOffer;
import com.devilbonegambler.economy.EconomyService;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.ConfigSyncPacket;
import com.devilbonegambler.network.packet.ConfigWriteRequestPacket;
import com.devilbonegambler.network.packet.ConfigWriteResponsePacket;
import com.devilbonegambler.util.ModConstants;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The admin config panel.
 * The panel only edits a client side copy of the config domains and submits whole domains;
 * the server validates the payload, applies the optimistic lock and writes the files.
 */
public class AdminConfigScreen extends Screen
{
    private static final Gson GSON = new Gson();

    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 274;
    private static final int PANEL_COLOR = 0xC0101010;
    private static final int ROW_COLOR = 0x20FFFFFF;
    private static final int ROW_SELECTED_COLOR = 0x60FFFFFF;
    private static final int LIST_X = 8;
    private static final int LIST_WIDTH = 210;
    private static final int LIST_ROW_HEIGHT = 11;
    private static final int LIST_VISIBLE_ROWS = 14;
    private static final int EDIT_X = 226;
    private static final int EDIT_WIDTH = 146;
    private static final int FIELD_HEIGHT = 14;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int TEXT_COLOR = 0xE0E0E0;
    private static final int STATUS_COLOR = 0x80C0FF;

    private final List<Row> prizeRows = new ArrayList<Row>();
    private final List<String> valueKeys = new ArrayList<String>();

    private PrizePoolConfig prizePool = PrizePoolConfig.empty();
    private ItemValueConfig itemValues = ItemValueConfig.empty();
    private ShopConfig shop = ShopConfig.empty();

    private int leftPos;
    private int topPos;
    private boolean valueTab;
    private boolean shopTab;
    private int selectedShopIndex = -1;
    private int selectedPrizeIndex = -1;
    private int selectedValueIndex = -1;
    private int scroll;
    private boolean waiting;
    private int pendingSequence = -1;
    private String status = "";
    private boolean widgetsReady;

    private EditBox fieldTier;
    private Button colorButton;
    private Button majorEventButton;
    private Button chapterButton;
    private boolean entityChapter;
    private EditBox fieldId;
    private EditBox fieldWeight;
    private EditBox fieldCommand;
    private EditBox fieldNotifyText;
    private EditBox fieldValue;
    private EditBox fieldFilter;
    private EditBox fieldOfferId;
    private EditBox fieldOfferItem;
    private EditBox fieldOfferCount;
    private EditBox fieldOfferCoin;
    private EditBox fieldOfferTear;
    private Button offerEnabledButton;
    private Button notifyModeButton;
    private Button enabledButton;
    private Button applyBatchButton;
    private Button addButton;

    public AdminConfigScreen(String prizePoolJson, String itemValuesJson, String shopJson)
    {
        super(Component.translatable(key("admin.title")));
        applyServerData(prizePoolJson, itemValuesJson, shopJson);
        this.status = Component.translatable(key("admin.status_idle")).getString();
    }

    /**
     * Replaces the local copy with the payload pushed by the server.
     */
    public void applyServerData(String prizePoolJson, String itemValuesJson, String shopJson)
    {
        this.prizePool = parsePrizePool(prizePoolJson);
        this.itemValues = parseItemValues(itemValuesJson);
        this.shop = parseShop(shopJson);
        this.selectedPrizeIndex = -1;
        this.selectedValueIndex = -1;
        this.selectedShopIndex = -1;
        this.scroll = 0;
        rebuildPrizeRows();
        rebuildValueKeys();

        if (this.widgetsReady)
        {
            refreshFields();
        }
    }

    public void onWriteResponse(ConfigWriteResponsePacket message)
    {
        this.waiting = false;

        if (message.success())
        {
            this.status = Component.translatable(key("admin.status_saved")).getString();
            return;
        }

        if ("revision_conflict".equals(message.reasonKey()))
        {
            this.status = Component.translatable(key("admin.status_conflict")).getString();
            return;
        }

        this.status = Component.translatable(key("admin.status_failed"),
                Component.translatable(key("reason." + message.reasonKey()))).getString();
    }

    public void onConfigSync(ConfigSyncPacket message)
    {
        if (ModConstants.CONFIG_DOMAIN_PRIZE_POOL.equals(message.domain()))
        {
            this.prizePool = parsePrizePool(message.payload());
            rebuildPrizeRows();
        }
        else if (ModConstants.CONFIG_DOMAIN_ITEM_VALUES.equals(message.domain()))
        {
            this.itemValues = parseItemValues(message.payload());
            rebuildValueKeys();
        }
        else if (ModConstants.CONFIG_DOMAIN_SHOP.equals(message.domain()))
        {
            this.shop = parseShop(message.payload());
            this.selectedShopIndex = -1;
        }

        if (this.widgetsReady)
        {
            refreshFields();
        }
    }

    @Override
    protected void init()
    {
        super.init();
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;

        addRenderableWidget(Button.builder(Component.translatable(key("admin.tab_prize")), button -> switchTab(false))
                .pos(this.leftPos + 8, this.topPos + 32).size(88, 16).build());
        addRenderableWidget(Button.builder(Component.translatable(key("admin.tab_value")), button -> switchTab(true))
                .pos(this.leftPos + 100, this.topPos + 32).size(88, 16).build());
        addRenderableWidget(Button.builder(Component.translatable(key("admin.tab_shop")), button -> switchShopTab())
                .pos(this.leftPos + 192, this.topPos + 32).size(88, 16).build());

        this.fieldTier = addField(62, Component.translatable(key("admin.label_region")), 6);
        this.colorButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleColor())
                .pos(this.leftPos + EDIT_X, this.topPos + 88).size(70, 16).build());
        this.majorEventButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleMajorEvent())
                .pos(this.leftPos + EDIT_X + 76, this.topPos + 88).size(70, 16).build());
        this.fieldId = addField(114, Component.translatable(key("admin.label_id")), ModConstants.MAX_ENTRY_ID_LENGTH);
        this.fieldWeight = addField(140, Component.translatable(key("admin.label_weight")), 12);
        this.fieldCommand = addField(166, Component.translatable(key("admin.label_command")), ModConstants.MAX_COMMAND_LENGTH);
        this.fieldNotifyText = addField(192, Component.translatable(key("admin.label_notify_text")),
                ModConstants.MAX_NOTIFY_TEXT_LENGTH);

        this.notifyModeButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleNotifyMode())
                .pos(this.leftPos + EDIT_X, this.topPos + 216).size(EDIT_WIDTH, 16).build());
        this.enabledButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleEnabled())
                .pos(this.leftPos + EDIT_X, this.topPos + 234).size(EDIT_WIDTH, 16).build());

        this.fieldValue = addField(62, Component.translatable(key("admin.label_value")), 12);
        this.applyBatchButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.apply_batch")),
                button -> applyBatch()).pos(this.leftPos + EDIT_X, this.topPos + 88).size(EDIT_WIDTH, 16).build());
        this.chapterButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleChapter())
                .pos(this.leftPos + EDIT_X, this.topPos + 112).size(EDIT_WIDTH, 16).build());

        this.fieldOfferId = addField(62, Component.translatable(key("admin.label_id")),
                ModConstants.MAX_ENTRY_ID_LENGTH);
        this.fieldOfferItem = addField(88, Component.translatable(key("admin.label_item")), 128);
        this.fieldOfferCount = addField(114, Component.translatable(key("admin.label_count")), 6);
        this.fieldOfferCoin = addField(140, Component.translatable(key("admin.label_price_coin")), 12);
        this.fieldOfferTear = addField(166, Component.translatable(key("admin.label_price_tear")), 12);
        this.offerEnabledButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleOfferEnabled())
                .pos(this.leftPos + EDIT_X, this.topPos + 192).size(EDIT_WIDTH, 16).build());

        this.fieldFilter = new EditBox(this.font, this.leftPos + LIST_X, this.topPos + 54, LIST_WIDTH, FIELD_HEIGHT,
                Component.translatable(key("admin.filter")));
        this.fieldFilter.setResponder(value -> {
            rebuildValueKeys();
            this.scroll = 0;
        });
        addRenderableWidget(this.fieldFilter);

        int buttonY = this.topPos + PANEL_HEIGHT - 22;
        this.addButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.add_region")),
                button -> primaryAdd()).pos(this.leftPos + 8, buttonY).size(68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(key("admin.add_entry")), button -> addEntry())
                .pos(this.leftPos + 80, buttonY).size(68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(key("admin.remove")),
                button -> removeSelectedOrOffer()).pos(this.leftPos + 152, buttonY).size(68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(key("admin.refresh")),
                button -> ClientActions.sendAdminPanelRequest())
                .pos(this.leftPos + 224, buttonY).size(24, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(key("admin.submit")), button -> submit())
                .pos(this.leftPos + 252, buttonY).size(64, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(key("admin.close")), button -> onClose())
                .pos(this.leftPos + 320, buttonY).size(52, 18).build());

        this.widgetsReady = true;
        refreshFields();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + PANEL_WIDTH, this.topPos + PANEL_HEIGHT, PANEL_COLOR);
        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 8, TITLE_COLOR, false);
        graphics.drawString(this.font, Component.translatable(key("admin.revision"), Integer.valueOf(revision())),
                this.leftPos + 8, this.topPos + 20, LABEL_COLOR, false);
        graphics.drawString(this.font, this.status, this.leftPos + 8, this.topPos + PANEL_HEIGHT - 36, STATUS_COLOR, false);
        graphics.drawString(this.font, Component.translatable("gui." + ModConstants.MOD_ID + ".author"),
                this.leftPos + EDIT_X, this.topPos + PANEL_HEIGHT - 36, LABEL_COLOR, false);

        drawList(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawFieldLabels(graphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (this.shopTab)
        {
            int shopRow = shopRowAt(mouseX, mouseY);

            if (shopRow >= 0)
            {
                applyOfferFields();
                this.selectedShopIndex = shopRow;
                refreshFields();
                return true;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        int row = listRowAt(mouseX, mouseY);

        if (row >= 0)
        {
            applyFieldsToSelection();
            selectRow(row);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        int total = this.shopTab
                ? this.shop.offersOrEmpty().size()
                : (this.valueTab ? this.valueKeys.size() : this.prizeRows.size());
        int maximum = Math.max(0, total - LIST_VISIBLE_ROWS);

        if (delta > 0.0D)
        {
            this.scroll = Math.max(0, this.scroll - 1);
        }
        else if (delta < 0.0D)
        {
            this.scroll = Math.min(maximum, this.scroll + 1);
        }

        return true;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    private EditBox addField(int offsetY, Component message, int maxLength)
    {
        EditBox box = new EditBox(this.font, this.leftPos + EDIT_X, this.topPos + offsetY, EDIT_WIDTH, FIELD_HEIGHT, message);
        box.setMaxLength(maxLength);
        addRenderableWidget(box);
        return box;
    }

    private void switchTab(boolean valueTab)
    {
        applyFieldsToSelection();
        this.valueTab = valueTab;
        this.shopTab = false;
        this.scroll = 0;
        refreshFields();
    }

    private void switchShopTab()
    {
        applyFieldsToSelection();
        this.valueTab = false;
        this.shopTab = true;
        this.scroll = 0;
        refreshFields();
    }

    private void drawList(GuiGraphics graphics)
    {
        if (this.shopTab)
        {
            drawShopList(graphics);
            return;
        }

        int listY = this.topPos + (this.valueTab ? 72 : 54);
        int total = this.valueTab ? this.valueKeys.size() : this.prizeRows.size();
        int selected = this.valueTab ? this.selectedValueIndex : this.selectedPrizeIndex;

        for (int line = 0; line < LIST_VISIBLE_ROWS; line++)
        {
            int index = this.scroll + line;

            if (index >= total)
            {
                break;
            }

            int y = listY + line * LIST_ROW_HEIGHT;
            graphics.fill(this.leftPos + LIST_X, y, this.leftPos + LIST_X + LIST_WIDTH, y + LIST_ROW_HEIGHT - 1,
                    index == selected ? ROW_SELECTED_COLOR : ROW_COLOR);
            graphics.drawString(this.font, clip(labelAt(index), LIST_WIDTH - 4), this.leftPos + LIST_X + 2, y + 2,
                    TEXT_COLOR, false);
        }
    }

    private void drawFieldLabels(GuiGraphics graphics)
    {
        if (this.valueTab)
        {
            drawLabel(graphics, this.fieldValue, 52);
            return;
        }

        if (selectedRegionRow() != null)
        {
            drawLabel(graphics, this.fieldTier, 52);
            return;
        }

        if (selectedEntryRow() != null)
        {
            drawLabel(graphics, this.fieldId, 52);
            drawLabel(graphics, this.fieldWeight, 78);
            drawLabel(graphics, this.fieldCommand, 104);
            drawLabel(graphics, this.fieldNotifyText, 130);
        }
    }

    private void drawLabel(GuiGraphics graphics, EditBox box, int offsetY)
    {
        if (box.visible)
        {
            graphics.drawString(this.font, box.getMessage(), this.leftPos + EDIT_X, this.topPos + offsetY, LABEL_COLOR, false);
        }
    }

    private String labelAt(int index)
    {
        if (this.valueTab)
        {
            String key = this.valueKeys.get(index);
            boolean registered = this.entityChapter
                    ? this.itemValues.isRegisteredEntity(key)
                    : this.itemValues.isRegisteredItem(key);
            return Component.translatable(key(registered ? "admin.value_row" : "admin.value_row_missing"), key,
                    Integer.valueOf(valueOfKey(key))).getString();
        }

        return this.prizeRows.get(index).label();
    }

    private int listRowAt(double mouseX, double mouseY)
    {
        int listY = this.topPos + (this.valueTab ? 72 : 54);

        if (mouseX < this.leftPos + LIST_X || mouseX > this.leftPos + LIST_X + LIST_WIDTH)
        {
            return -1;
        }

        int line = (int) Math.floor((mouseY - listY) / LIST_ROW_HEIGHT);

        if (line < 0 || line >= LIST_VISIBLE_ROWS)
        {
            return -1;
        }

        int index = this.scroll + line;
        int total = this.valueTab ? this.valueKeys.size() : this.prizeRows.size();
        return index < total ? index : -1;
    }

    private void selectRow(int index)
    {
        if (this.valueTab)
        {
            this.selectedValueIndex = index;
        }
        else
        {
            this.selectedPrizeIndex = index;
        }

        refreshFields();
    }

    private void refreshFields()
    {
        if (this.shopTab)
        {
            refreshOfferFields();
            return;
        }

        RegionRow regionRow = selectedRegionRow();
        EntryRow entryRow = selectedEntryRow();

        setVisible(this.fieldTier, regionRow != null);
        setVisible(this.colorButton, entryRow != null);
        setVisible(this.majorEventButton, entryRow != null);
        setVisible(this.fieldId, entryRow != null);
        setVisible(this.fieldWeight, entryRow != null);
        setVisible(this.fieldCommand, entryRow != null);
        setVisible(this.fieldNotifyText, entryRow != null);
        setVisible(this.notifyModeButton, entryRow != null);
        setVisible(this.enabledButton, entryRow != null);
        setVisible(this.fieldValue, this.valueTab);
        setVisible(this.applyBatchButton, this.valueTab);
        setVisible(this.chapterButton, this.valueTab);
        setVisible(this.fieldFilter, this.valueTab);

        if (regionRow != null)
        {
            this.fieldTier.setValue(Integer.toString(regionRow.region.tierOrZero()));
        }

        if (entryRow != null)
        {
            this.fieldId.setValue(entryRow.entry.id == null ? "" : entryRow.entry.id);
            this.fieldWeight.setValue(formatWeight(entryRow.entry.weightOrZero()));
            this.fieldCommand.setValue(entryRow.entry.commandOrEmpty());
            this.fieldNotifyText.setValue(entryRow.entry.notifyOrDefault().textOrEmpty());
            this.notifyModeButton.setMessage(
                    Component.translatable(key("notify.mode." + entryRow.entry.notifyOrDefault().modeOrDefault())));
            this.enabledButton.setMessage(Component.translatable(key(entryRow.entry.isEnabled()
                    ? "admin.toggle_on" : "admin.toggle_off")));
            this.colorButton.setMessage(Component.translatable(key(entryRow.entry.isRed()
                    ? "admin.toggle_red" : "admin.toggle_black")));
            this.majorEventButton.setMessage(Component.translatable(key(entryRow.entry.isMajorEvent()
                    ? "admin.major_event_on" : "admin.major_event_off")));
        }

        if (this.valueTab)
        {
            this.chapterButton.setMessage(Component.translatable(key(this.entityChapter
                    ? "admin.chapter_entity" : "admin.chapter_item")));
            this.fieldValue.setValue(Integer.toString(selectedValue()));
        }
    }

    private void applyFieldsToSelection()
    {
        RegionRow regionRow = selectedRegionRow();

        if (regionRow != null)
        {
            regionRow.region.tier = Integer.valueOf(parseInt(this.fieldTier.getValue(), regionRow.region.tierOrZero()));
        }

        EntryRow entryRow = selectedEntryRow();

        if (entryRow != null)
        {
            entryRow.entry.id = this.fieldId.getValue();
            entryRow.entry.weight = Double.valueOf(Math.max(0.0D,
                    parseDouble(this.fieldWeight.getValue(), entryRow.entry.weightOrZero())));
            entryRow.entry.command = this.fieldCommand.getValue();
            entryRow.entry.notify = new NotifyConfig(entryRow.entry.notifyOrDefault().modeOrDefault(),
                    this.fieldNotifyText.getValue());
        }

        if (this.valueTab && this.selectedValueIndex >= 0 && this.selectedValueIndex < this.valueKeys.size())
        {
            setValueOfKey(this.valueKeys.get(this.selectedValueIndex),
                    Math.max(0, parseInt(this.fieldValue.getValue(), 0)));
        }
    }

    private void toggleColor()
    {
        EntryRow entryRow = selectedEntryRow();

        if (entryRow == null)
        {
            return;
        }

        entryRow.entry.color = entryRow.entry.isRed()
                ? ModConstants.ENTRY_COLOR_BLACK : ModConstants.ENTRY_COLOR_RED;
        refreshFields();
    }

    private void toggleMajorEvent()
    {
        EntryRow entryRow = selectedEntryRow();

        if (entryRow == null)
        {
            return;
        }

        entryRow.entry.majorEvent = Boolean.valueOf(!entryRow.entry.isMajorEvent());
        refreshFields();
    }

    private void toggleChapter()
    {
        applyFieldsToSelection();
        this.entityChapter = !this.entityChapter;
        this.selectedValueIndex = -1;
        this.scroll = 0;
        rebuildValueKeys();
        refreshFields();
    }

    private int valueOfKey(String key)
    {
        return this.entityChapter ? this.itemValues.entityValueOf(key) : this.itemValues.itemValueOf(key);
    }

    private void setValueOfKey(String key, int value)
    {
        if (this.entityChapter)
        {
            this.itemValues.entities.put(key, Integer.valueOf(value));
        }
        else
        {
            this.itemValues.items.put(key, Integer.valueOf(value));
        }
    }

    private Map<String, Number> currentChapter()
    {
        return this.entityChapter ? this.itemValues.entitiesOrEmpty() : this.itemValues.itemsOrEmpty();
    }

    private void cycleNotifyMode()
    {
        EntryRow entryRow = selectedEntryRow();

        if (entryRow == null)
        {
            return;
        }

        String current = entryRow.entry.notifyOrDefault().modeOrDefault();
        int index = 0;

        for (int candidate = 0; candidate < ModConstants.NOTIFY_MODES.length; candidate++)
        {
            if (ModConstants.NOTIFY_MODES[candidate].equals(current))
            {
                index = candidate;
                break;
            }
        }

        String next = ModConstants.NOTIFY_MODES[(index + 1) % ModConstants.NOTIFY_MODES.length];
        entryRow.entry.notify = new NotifyConfig(next, this.fieldNotifyText.getValue());
        refreshFields();
    }

    private void toggleEnabled()
    {
        EntryRow entryRow = selectedEntryRow();

        if (entryRow == null)
        {
            return;
        }

        entryRow.entry.enabled = Boolean.valueOf(!entryRow.entry.isEnabled());
        refreshFields();
    }

    private void applyBatch()
    {
        if (!this.valueTab)
        {
            return;
        }

        int value = Math.max(0, parseInt(this.fieldValue.getValue(), 0));

        for (String key : this.valueKeys)
        {
            setValueOfKey(key, value);
        }

        this.status = Component.translatable(key("admin.status_idle")).getString();
    }

    private void addRegion()
    {
        applyFieldsToSelection();
        int tier = ModConstants.MIN_REGION_TIER;

        while (this.prizePool.findRegion(tier) != null && tier < ModConstants.MAX_REGIONS)
        {
            tier++;
        }

        this.prizePool.regionsOrEmpty().add(new PrizeRegion(tier));
        rebuildPrizeRows();
        this.selectedPrizeIndex = this.prizeRows.size() - 1;
        refreshFields();
    }

    private void addEntry()
    {
        applyFieldsToSelection();
        PrizeRegion region = selectedRegion() != null ? selectedRegion() : firstRegion();

        if (region == null)
        {
            return;
        }

        PrizeEntry entry = new PrizeEntry("entry_" + (region.entriesOrEmpty().size() + 1), true, 10.0D,
                ModConstants.ENTRY_COLOR_RED, false, "give ${player} minecraft:bread 1",
                ModConstants.NOTIFY_MODE_ACTIONBAR, "");
        region.entriesOrEmpty().add(entry);
        rebuildPrizeRows();
        this.selectedPrizeIndex = indexOfEntry(region, entry);
        refreshFields();
    }

    private void removeSelected()
    {
        if (this.valueTab)
        {
            if (this.selectedValueIndex >= 0 && this.selectedValueIndex < this.valueKeys.size())
            {
                currentChapter().remove(this.valueKeys.get(this.selectedValueIndex));
                rebuildValueKeys();
                this.selectedValueIndex = -1;
                refreshFields();
            }

            return;
        }

        EntryRow entryRow = selectedEntryRow();

        if (entryRow != null)
        {
            entryRow.region.entriesOrEmpty().remove(entryRow.entry);
            rebuildPrizeRows();
            this.selectedPrizeIndex = -1;
            refreshFields();
            return;
        }

        RegionRow regionRow = selectedRegionRow();

        if (regionRow != null && this.prizePool.regionsOrEmpty().size() > 1)
        {
            this.prizePool.regionsOrEmpty().remove(regionRow.region);
            rebuildPrizeRows();
            this.selectedPrizeIndex = -1;
            refreshFields();
        }
    }

    private void submit()
    {
        if (this.waiting)
        {
            return;
        }

        applyFieldsToSelection();
        this.prizePool.normalize();
        this.itemValues.normalize();
        this.shop.normalize();

        if (this.shopTab)
        {
            List<String> shopProblems = this.shop.validate();

            if (!shopProblems.isEmpty())
            {
                this.status = Component.translatable(key("admin.status_invalid"), shopProblems.toString()).getString();
                return;
            }

            this.waiting = true;
            this.pendingSequence = ClientActions.nextSequence();
            this.status = Component.translatable(key("admin.status_waiting")).getString();
            ModNetwork.sendToServer(new ConfigWriteRequestPacket(this.pendingSequence,
                    ModConstants.CONFIG_DOMAIN_SHOP, this.shop.revisionOrZero(), GSON.toJson(this.shop)));
            return;
        }

        List<String> problems = this.valueTab ? this.itemValues.validate() : this.prizePool.validate();

        if (!problems.isEmpty())
        {
            this.status = Component.translatable(key("admin.status_local_invalid")).getString();
            return;
        }

        this.waiting = true;
        this.pendingSequence = ClientActions.nextSequence();
        this.status = Component.translatable(key("admin.status_waiting")).getString();

        if (this.valueTab)
        {
            ModNetwork.sendToServer(new ConfigWriteRequestPacket(this.pendingSequence,
                    ModConstants.CONFIG_DOMAIN_ITEM_VALUES, this.itemValues.revisionOrZero(),
                    GSON.toJson(this.itemValues)));
        }
        else
        {
            ModNetwork.sendToServer(new ConfigWriteRequestPacket(this.pendingSequence,
                    ModConstants.CONFIG_DOMAIN_PRIZE_POOL, this.prizePool.revisionOrZero(),
                    GSON.toJson(this.prizePool)));
        }
    }

    private void rebuildPrizeRows()
    {
        this.prizeRows.clear();

        for (PrizeRegion region : this.prizePool.sortedRegions())
        {
            this.prizeRows.add(new RegionRow(region));

            for (PrizeEntry entry : region.entriesOrEmpty())
            {
                this.prizeRows.add(new EntryRow(region, entry));
            }
        }

        if (this.selectedPrizeIndex >= this.prizeRows.size())
        {
            this.selectedPrizeIndex = -1;
        }
    }

    private void rebuildValueKeys()
    {
        this.valueKeys.clear();
        String filter = this.fieldFilter == null ? "" : this.fieldFilter.getValue().trim();
        Map<String, Number> sorted = new TreeMap<String, Number>(currentChapter());

        for (String key : sorted.keySet())
        {
            if (filter.isEmpty() || key.contains(filter))
            {
                this.valueKeys.add(key);
            }
        }

        if (this.selectedValueIndex >= this.valueKeys.size())
        {
            this.selectedValueIndex = -1;
        }
    }

    private int indexOfEntry(PrizeRegion region, PrizeEntry entry)
    {
        for (int index = 0; index < this.prizeRows.size(); index++)
        {
            Row row = this.prizeRows.get(index);

            if (row instanceof EntryRow && ((EntryRow) row).entry == entry)
            {
                return index;
            }
        }

        return -1;
    }

    private PrizeRegion selectedRegion()
    {
        RegionRow row = selectedRegionRow();

        if (row != null)
        {
            return row.region;
        }

        EntryRow entryRow = selectedEntryRow();
        return entryRow == null ? null : entryRow.region;
    }

    private PrizeRegion firstRegion()
    {
        List<PrizeRegion> regions = this.prizePool.sortedRegions();
        return regions.isEmpty() ? null : regions.get(0);
    }

    private RegionRow selectedRegionRow()
    {
        if (this.valueTab || this.selectedPrizeIndex < 0 || this.selectedPrizeIndex >= this.prizeRows.size())
        {
            return null;
        }

        Row row = this.prizeRows.get(this.selectedPrizeIndex);
        return row instanceof RegionRow ? (RegionRow) row : null;
    }

    private EntryRow selectedEntryRow()
    {
        if (this.valueTab || this.selectedPrizeIndex < 0 || this.selectedPrizeIndex >= this.prizeRows.size())
        {
            return null;
        }

        Row row = this.prizeRows.get(this.selectedPrizeIndex);
        return row instanceof EntryRow ? (EntryRow) row : null;
    }

    private int selectedValue()
    {
        if (this.selectedValueIndex < 0 || this.selectedValueIndex >= this.valueKeys.size())
        {
            return 0;
        }

        return valueOfKey(this.valueKeys.get(this.selectedValueIndex));
    }

    private int revision()
    {
        if (this.shopTab)
        {
            return this.shop.revisionOrZero();
        }

        return this.valueTab ? this.itemValues.revisionOrZero() : this.prizePool.revisionOrZero();
    }

    /**
     * Shop tab list. It shares the list geometry of the prize tab but keeps its own selection index.
     */
    private void drawShopList(GuiGraphics graphics)
    {
        List<ShopOffer> offers = this.shop.offersOrEmpty();
        int listY = this.topPos + 54;

        for (int line = 0; line < LIST_VISIBLE_ROWS; line++)
        {
            int index = this.scroll + line;

            if (index >= offers.size())
            {
                break;
            }

            int y = listY + line * LIST_ROW_HEIGHT;
            graphics.fill(this.leftPos + LIST_X, y, this.leftPos + LIST_X + LIST_WIDTH, y + LIST_ROW_HEIGHT - 1,
                    index == this.selectedShopIndex ? ROW_SELECTED_COLOR : ROW_COLOR);
            graphics.drawString(this.font, clip(offerLabel(offers.get(index)), LIST_WIDTH - 4),
                    this.leftPos + LIST_X + 2, y + 2, TEXT_COLOR, false);
        }
    }

    private String offerLabel(ShopOffer offer)
    {
        return Component.translatable(key("admin.offer_row"), offer.id == null ? "" : offer.id, offer.itemOrEmpty(),
                Integer.valueOf(offer.countOrZero()), EconomyService.format(offer.priceCoinOrZero()),
                EconomyService.format(offer.priceTearOrZero()),
                Component.translatable(key(offer.isEnabled() ? "admin.offer_enabled" : "admin.offer_disabled")))
                .getString();
    }

    private int shopRowAt(double mouseX, double mouseY)
    {
        if (mouseX < this.leftPos + LIST_X || mouseX > this.leftPos + LIST_X + LIST_WIDTH)
        {
            return -1;
        }

        int line = (int) Math.floor((mouseY - (this.topPos + 54)) / LIST_ROW_HEIGHT);

        if (line < 0 || line >= LIST_VISIBLE_ROWS)
        {
            return -1;
        }

        int index = this.scroll + line;
        return index < this.shop.offersOrEmpty().size() ? index : -1;
    }

    private ShopOffer selectedOffer()
    {
        List<ShopOffer> offers = this.shop.offersOrEmpty();
        return this.selectedShopIndex >= 0 && this.selectedShopIndex < offers.size()
                ? offers.get(this.selectedShopIndex)
                : null;
    }

    /**
     * Shows only the shop widgets and fills them from the selected offer.
     */
    private void refreshOfferFields()
    {
        setVisible(this.fieldTier, false);
        setVisible(this.colorButton, false);
        setVisible(this.majorEventButton, false);
        setVisible(this.fieldId, false);
        setVisible(this.fieldWeight, false);
        setVisible(this.fieldCommand, false);
        setVisible(this.fieldNotifyText, false);
        setVisible(this.notifyModeButton, false);
        setVisible(this.enabledButton, false);
        setVisible(this.fieldValue, false);
        setVisible(this.applyBatchButton, false);
        setVisible(this.chapterButton, false);
        setVisible(this.fieldFilter, false);

        ShopOffer offer = selectedOffer();
        boolean hasOffer = offer != null;
        setVisible(this.fieldOfferId, hasOffer);
        setVisible(this.fieldOfferItem, hasOffer);
        setVisible(this.fieldOfferCount, hasOffer);
        setVisible(this.fieldOfferCoin, hasOffer);
        setVisible(this.fieldOfferTear, hasOffer);
        setVisible(this.offerEnabledButton, hasOffer);

        if (hasOffer)
        {
            this.fieldOfferId.setValue(offer.id == null ? "" : offer.id);
            this.fieldOfferItem.setValue(offer.itemOrEmpty());
            this.fieldOfferCount.setValue(Integer.toString(offer.countOrZero()));
            this.fieldOfferCoin.setValue(Long.toString(offer.priceCoinOrZero()));
            this.fieldOfferTear.setValue(Long.toString(offer.priceTearOrZero()));
            updateOfferEnabledButton();
        }

        if (this.addButton != null)
        {
            this.addButton.setMessage(Component.translatable(key("admin.add_offer")));
        }
    }

    /**
     * Writes the shop edit fields back into the selected offer.
     */
    private void applyOfferFields()
    {
        ShopOffer offer = selectedOffer();

        if (offer == null)
        {
            return;
        }

        offer.id = this.fieldOfferId.getValue().trim();
        offer.item = this.fieldOfferItem.getValue().trim();
        offer.count = Integer.valueOf(Math.max(1, parseIntOr(this.fieldOfferCount.getValue(), 1)));
        offer.priceCoin = Long.valueOf(Math.max(0L, parseLongOr(this.fieldOfferCoin.getValue(), 0L)));
        offer.priceTear = Long.valueOf(Math.max(0L, parseLongOr(this.fieldOfferTear.getValue(), 0L)));
    }

    /**
     * The first bottom button adds whatever the active tab edits.
     */
    private void primaryAdd()
    {
        if (this.shopTab)
        {
            addOffer();
            return;
        }

        addRegion();
    }

    /**
     * The remove button removes whatever the active tab has selected.
     */
    private void removeSelectedOrOffer()
    {
        if (this.shopTab)
        {
            removeOffer();
            return;
        }

        removeSelected();
    }

    private void addOffer()
    {
        applyOfferFields();
        this.shop.offersOrEmpty().add(new ShopOffer("offer_" + (this.shop.offersOrEmpty().size() + 1), true,
                "minecraft:bread", 1, 10L, 0L));
        this.selectedShopIndex = this.shop.offersOrEmpty().size() - 1;
        refreshFields();
    }

    private void removeOffer()
    {
        List<ShopOffer> offers = this.shop.offersOrEmpty();

        if (this.selectedShopIndex < 0 || this.selectedShopIndex >= offers.size())
        {
            return;
        }

        offers.remove(this.selectedShopIndex);
        this.selectedShopIndex = Math.min(this.selectedShopIndex, offers.size() - 1);
        refreshFields();
    }

    private void toggleOfferEnabled()    {
        ShopOffer offer = selectedOffer();

        if (offer == null)
        {
            return;
        }

        offer.enabled = Boolean.valueOf(!offer.isEnabled());
        updateOfferEnabledButton();
    }

    private void updateOfferEnabledButton()
    {
        ShopOffer offer = selectedOffer();
        boolean enabled = offer == null || offer.isEnabled();
        this.offerEnabledButton.setMessage(
                Component.translatable(key(enabled ? "admin.offer_enabled" : "admin.offer_disabled")));
    }

    private static ShopConfig parseShop(String json)
    {
        try
        {
            ShopConfig parsed = GSON.fromJson(json, ShopConfig.class);
            return parsed == null ? ShopConfig.empty() : parsed;
        }
        catch (RuntimeException exception)
        {
            return ShopConfig.empty();
        }
    }

    private static int parseIntOr(String raw, int fallback)
    {
        try
        {
            return Integer.parseInt(raw.trim());
        }
        catch (NumberFormatException exception)
        {
            return fallback;
        }
    }

    private static long parseLongOr(String raw, long fallback)
    {
        try
        {
            return Long.parseLong(raw.trim());
        }
        catch (NumberFormatException exception)
        {
            return fallback;
        }
    }

    private static void setVisible(net.minecraft.client.gui.components.AbstractWidget widget, boolean visible)
    {
        widget.visible = visible;
        widget.active = visible;
    }

    private static String formatWeight(double weight)
    {
        return weight == Math.floor(weight) ? Long.toString((long) weight) : Double.toString(weight);
    }

    private static int parseInt(String raw, int fallback)
    {
        try
        {
            return Integer.parseInt(raw.trim());
        }
        catch (NumberFormatException exception)
        {
            return fallback;
        }
    }

    private static double parseDouble(String raw, double fallback)
    {
        try
        {
            return Double.parseDouble(raw.trim());
        }
        catch (NumberFormatException exception)
        {
            return fallback;
        }
    }

    private static PrizePoolConfig parsePrizePool(String json)
    {
        try
        {
            PrizePoolConfig parsed = GSON.fromJson(json, PrizePoolConfig.class);
            return parsed == null ? PrizePoolConfig.empty() : parsed;
        }
        catch (RuntimeException exception)
        {
            return PrizePoolConfig.empty();
        }
    }

    private static ItemValueConfig parseItemValues(String json)
    {
        try
        {
            ItemValueConfig parsed = GSON.fromJson(json, ItemValueConfig.class);
            return parsed == null ? ItemValueConfig.empty() : parsed;
        }
        catch (RuntimeException exception)
        {
            return ItemValueConfig.empty();
        }
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

    private abstract static class Row
    {
        abstract String label();
    }

    private static final class RegionRow extends Row
    {
        private final PrizeRegion region;

        private RegionRow(PrizeRegion region)
        {
            this.region = region;
        }

        @Override
        String label()
        {
            return Component.translatable(key("admin.region_row"), Integer.valueOf(this.region.tierOrZero()),
                    Integer.valueOf(this.region.entriesOrEmpty().size())).getString();
        }
    }

    private static final class EntryRow extends Row
    {
        private final PrizeRegion region;
        private final PrizeEntry entry;

        private EntryRow(PrizeRegion region, PrizeEntry entry)
        {
            this.region = region;
            this.entry = entry;
        }

        @Override
        String label()
        {
            return Component.translatable(key("admin.entry_row"),
                    this.entry.id == null ? "" : this.entry.id,
                    Double.valueOf(this.entry.weightOrZero()),
                    Component.translatable(key(this.entry.isRed()
                            ? "admin.toggle_red" : "admin.toggle_black")).getString()).getString();
        }
    }
}
