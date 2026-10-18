/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.commands.CommandRunner;
import com.devilbonegambler.config.ItemValueConfig;
import com.devilbonegambler.config.NotifyConfig;
import com.devilbonegambler.config.PrizeEntry;
import com.devilbonegambler.config.PrizePoolConfig;
import com.devilbonegambler.config.PrizeRegion;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.ConfigSyncPacket;
import com.devilbonegambler.network.packet.ConfigWriteRequestPacket;
import com.devilbonegambler.network.packet.ConfigWriteResponsePacket;
import com.devilbonegambler.util.ModConstants;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The admin config panel.
 * The panel only edits a client side copy of the config domains and submits whole domains;
 * the server validates the payload, applies the optimistic lock and writes the files.
 * Both lists are tables under the shared list rules: a frozen header row, fixed column widths, centred and clipped
 * cells, a draggable scroll bar and a hover tooltip that waits before it appears.
 * Rows can be picked one at a time or, with the control key held, several at once; an edit of a field that several
 * selected rows share is applied to every one of them, while the entry name stays the field of a single row.
 * The revision behind the optimistic lock stays an internal detail and is never shown.
 */
public class AdminConfigScreen extends Screen
{
    private static final Gson GSON = new Gson();

    private static final int PANEL_WIDTH = 380;
    /*
     * The frame keeps a margin from the window edge and the content keeps a padding inside the frame. The vertical
     * padding is half the horizontal one, and the half taken away shows up as the margin outside the frame, so the
     * left column gains the height the old padding spent below the list.
     */
    private static final int PANEL_PADDING = 6;
    private static final int PANEL_PADDING_Y = 3;
    private static final int PANEL_MARGIN = 3;
    private static final int PANEL_COLOR = 0xC0101010;
    private static final int PANEL_BORDER_COLOR = 0xF03C3C3C;
    private static final int ROW_COLOR = 0x20FFFFFF;
    private static final int ROW_SELECTED_COLOR = 0x60FFFFFF;
    private static final int HEADER_COLOR = 0x30FFFFFF;

    private static final int TITLE_Y = 8;
    private static final int ADMIN_BUTTON_Y = 18;
    private static final int TAB_Y = 32;
    private static final int LIST_X = 8;
    private static final int LIST_WIDTH = 210;
    private static final int PRIZE_LIST_Y = 50;
    private static final int VALUE_LIST_Y = 72;
    private static final int FILTER_Y = 54;
    private static final int SCROLL_GAP = 2;

    private static final int EDIT_X = 226;
    private static final int EDIT_WIDTH = 146;
    private static final int FIELD_HEIGHT = 14;
    private static final int CONTENT_ROW_HEIGHT = 24;
    private static final int LABEL_GAP = 10;
    private static final int TOGGLE_WIDTH = 70;
    /*
     * Rows of the right column. A text field is fourteen pixels tall and carries its label in the ten pixels above it,
     * which is the rhythm of CONTENT_ROW_HEIGHT; a row of buttons is two pixels taller, so the row after it starts two
     * pixels lower and its label still clears the buttons.
     */
    private static final int ROW_REGION_Y = 50;
    private static final int ROW_COLOR_Y = 74;
    private static final int ROW_NAME_Y = ROW_COLOR_Y + 26;
    private static final int ROW_WEIGHT_Y = ROW_NAME_Y + CONTENT_ROW_HEIGHT;
    private static final int ROW_COMMAND_Y = ROW_WEIGHT_Y + CONTENT_ROW_HEIGHT;
    private static final int ROW_NOTIFY_Y = ROW_COMMAND_Y + CONTENT_ROW_HEIGHT;
    private static final int ROW_TOGGLE_Y = ROW_NOTIFY_Y + CONTENT_ROW_HEIGHT;
    private static final int COLUMN_BOTTOM = ROW_TOGGLE_Y + 16;

    /*
     * Footer and button row hang from the bottom of the content area, so the list is the only part that grows or
     * shrinks with the window.
     */
    private static final int BUTTON_ROW_HEIGHT = 18;
    private static final int BUTTON_ROW_MARGIN = 6;
    private static final int BUTTON_ROW_FROM_BOTTOM = BUTTON_ROW_HEIGHT + BUTTON_ROW_MARGIN;
    private static final int FOOTER_FROM_BOTTOM = BUTTON_ROW_FROM_BOTTOM + 14;
    private static final int BASE_CONTENT_HEIGHT = COLUMN_BOTTOM + 4 + FOOTER_FROM_BOTTOM;
    private static final int LIST_MIN_ROWS = 5;
    private static final int LIST_MAX_ROWS = 10;
    private static final int LIST_DEFAULT_ROWS = 9;
    private static final int LIST_BOTTOM_GAP = 4;
    private static final int COMMAND_FIELD_WIDTH = 126;
    private static final int COMMAND_WINDOW_X = 80;
    private static final int COMMAND_WINDOW_Y = 89;
    private static final int COMMAND_WINDOW_WIDTH = 220;
    private static final int COMMAND_WINDOW_HEIGHT = 96;
    private static final int COMMAND_EDITOR_HEIGHT = 52;
    /*
     * A sub page is opaque and dims the page behind it, so both layers stay readable instead of mixing.
     */
    private static final int WINDOW_COLOR = 0xFF141414;
    /* Fully opaque, so nothing of the page behind a sub page can show through its text. */
    private static final int SUBPAGE_SCRIM_COLOR = 0xFF0E0E0E;
    private static final int WINDOW_BORDER_COLOR = 0xFF707070;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int TEXT_COLOR = 0xE0E0E0;
    private static final int MUTED_COLOR = 0x808080;
    private static final int HEADER_TEXT_COLOR = 0xC0C0C0;
    private static final int STATUS_COLOR = 0x80C0FF;
    private static final int CREATOR_COLOR = 0xD8B860;

    private static final int ADMIN_WINDOW_X = 60;
    private static final int ADMIN_WINDOW_Y = 56;
    private static final int ADMIN_WINDOW_WIDTH = 260;
    private static final int ADMIN_WINDOW_HEIGHT = 152;
    private static final int ADMIN_LIST_X = 5;
    private static final int ADMIN_LIST_WIDTH = 236;
    private static final int ADMIN_LIST_Y = 22;
    private static final int ADMIN_VISIBLE_ROWS = 6;
    private static final int ADMIN_FIELD_Y = 112;
    private static final int ADMIN_FIELD_WIDTH = 142;
    private static final int ADMIN_CLOSE_Y = 132;

    private final List<Row> prizeRows = new ArrayList<Row>();
    private final List<String> valueKeys = new ArrayList<String>();
    private final List<Integer> selectedPrizeIndices = new ArrayList<Integer>();
    private final List<Integer> selectedValueIndices = new ArrayList<Integer>();
    private final ScrollBar scrollBar = new ScrollBar();
    private final DelayedTooltip tooltip = new DelayedTooltip();

    private PrizePoolConfig prizePool = PrizePoolConfig.empty();
    private ItemValueConfig itemValues = ItemValueConfig.empty();

    private int leftPos;
    private int topPos;
    private boolean valueTab;
    private boolean waiting;
    private int pendingSequence = -1;

    /*
     * Field values of the primary row as they were loaded into the panel. A shared field is spread over the whole
     * selection only when it really differs from this baseline, so picking a second row never rewrites its weight,
     * its colour or its value.
     */
    private boolean entryBaselineReady;
    private double baselineWeight;
    private boolean baselineRed;
    private boolean baselineMajor;
    private boolean baselineEnabled;
    private String baselineCommand = "";
    private String baselineNotifyText = "";
    private boolean valueBaselineReady;
    private int baselineValue;
    private String status = "";
    private boolean widgetsReady;
    private TableLayout prizeLayout;
    private TableLayout valueLayout;
    private TableLayout adminLayout;

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
    private Button notifyModeButton;
    private Button enabledButton;
    private Button tabPrizeButton;
    private Button tabValueButton;
    private Button addRegionButton;
    private Button addEntryButton;
    private Button refreshButton;
    private Button submitButton;
    private Button closeButton;
    private ConfirmButton removeButton;
    private Button commandEditButton;
    private MultiLineEditBox commandEditor;
    private Button commandApplyButton;
    private Button commandCancelButton;
    private boolean commandEditorOpen;

    /*
     * Management access as pushed by the server. The creator is shown first and can never be removed, the granted
     * admins follow and each of them can be revoked.
     */
    private String creatorName = "";
    private List<String> admins = new ArrayList<String>();
    private boolean canManageAdmins;
    private boolean adminWindowOpen;
    private int selectedAdminRow = -1;
    private EditBox fieldAdminName;
    private Button adminButton;
    private Button adminAddButton;
    private ConfirmButton adminRemoveButton;
    private Button adminCloseButton;

    public AdminConfigScreen(String prizePoolJson, String itemValuesJson, String creatorName, List<String> admins,
            boolean canManageAdmins)
    {
        super(Component.translatable(key("admin.title")));
        applyServerData(prizePoolJson, itemValuesJson, creatorName, admins, canManageAdmins);
        this.status = Component.translatable(key("admin.status_idle")).getString();
    }

    /**
     * Replaces the local copy with the payload pushed by the server.
     */
    public void applyServerData(String prizePoolJson, String itemValuesJson, String creatorName,
            List<String> admins, boolean canManageAdmins)
    {
        this.prizePool = parsePrizePool(prizePoolJson);
        this.itemValues = parseItemValues(itemValuesJson);
        this.creatorName = creatorName == null ? "" : creatorName;
        this.admins = admins == null ? new ArrayList<String>() : new ArrayList<String>(admins);
        this.canManageAdmins = canManageAdmins;
        this.selectedPrizeIndices.clear();
        this.selectedValueIndices.clear();
        this.selectedAdminRow = -1;
        this.scrollBar.setValue(0);
        rebuildPrizeRows();
        rebuildValueKeys();
        rebuildAdminTable();

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

        rebuildAdminTable();

        if (this.widgetsReady)
        {
            refreshFields();
        }
    }

    @Override
    protected void init()
    {
        super.init();
        /* The padding and the margin stay visible even in a window too small for the whole content. */
        this.leftPos = Math.max(PANEL_PADDING, (this.width - PANEL_WIDTH) / 2);
        this.topPos = Math.max(PANEL_PADDING_Y + PANEL_MARGIN, (this.height - contentHeight()) / 2);

        this.tabPrizeButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.tab_prize")),
                button -> switchTab(false)).pos(this.leftPos + 8, this.topPos + TAB_Y).size(88, 16).build());
        this.tabValueButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.tab_value")),
                button -> switchTab(true)).pos(this.leftPos + 100, this.topPos + TAB_Y).size(88, 16).build());

        this.fieldTier = addField(ROW_REGION_Y, Component.translatable(key("admin.label_region")), 6);
        this.colorButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleColor())
                .pos(this.leftPos + EDIT_X, this.topPos + ROW_COLOR_Y).size(70, 16).build());
        this.majorEventButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleMajorEvent())
                .pos(this.leftPos + EDIT_X + 76, this.topPos + ROW_COLOR_Y).size(70, 16).build());
        this.fieldId = addField(ROW_NAME_Y, Component.translatable(key("admin.label_entry_name")),
                ModConstants.MAX_ENTRY_ID_LENGTH);
        this.fieldWeight = addField(ROW_WEIGHT_Y, Component.translatable(key("admin.label_weight")), 12);
        this.fieldCommand = new EditBox(this.font, this.leftPos + EDIT_X, this.topPos + ROW_COMMAND_Y, COMMAND_FIELD_WIDTH,
                FIELD_HEIGHT, Component.translatable(key("admin.label_command")));
        this.fieldCommand.setMaxLength(ModConstants.MAX_COMMAND_LENGTH);
        addRenderableWidget(this.fieldCommand);
        this.commandEditButton = addRenderableWidget(Button.builder(Component.literal("..."),
                button -> openCommandEditor()).pos(this.leftPos + EDIT_X + COMMAND_FIELD_WIDTH + 2,
                        this.topPos + ROW_COMMAND_Y).size(18, FIELD_HEIGHT).build());

        this.commandEditor = new MultiLineEditBox(this.font, this.leftPos + COMMAND_WINDOW_X + 6,
                this.topPos + COMMAND_WINDOW_Y + 18, COMMAND_WINDOW_WIDTH - 12, COMMAND_EDITOR_HEIGHT,
                Component.translatable(key("admin.label_command")),
                Component.translatable(key("admin.label_command")));
        this.commandEditor.setCharacterLimit(ModConstants.MAX_COMMAND_LENGTH);
        addRenderableWidget(this.commandEditor);
        this.commandApplyButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.apply")),
                button -> closeCommandEditor(true)).pos(this.leftPos + COMMAND_WINDOW_X + 6,
                        this.topPos + COMMAND_WINDOW_Y + 74).size(100, 16).build());
        this.commandCancelButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.cancel")),
                button -> closeCommandEditor(false)).pos(this.leftPos + COMMAND_WINDOW_X + 114,
                        this.topPos + COMMAND_WINDOW_Y + 74).size(100, 16).build());
        this.fieldNotifyText = addField(ROW_NOTIFY_Y, Component.translatable(key("admin.label_notify_text")),
                ModConstants.MAX_NOTIFY_TEXT_LENGTH);

        /* The two toggles share one row: the labels are short and the row was the tallest part of the column. */
        this.notifyModeButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleNotifyMode())
                .pos(this.leftPos + EDIT_X, this.topPos + ROW_TOGGLE_Y).size(TOGGLE_WIDTH, 16).build());
        this.enabledButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleEnabled())
                .pos(this.leftPos + EDIT_X + EDIT_WIDTH - TOGGLE_WIDTH, this.topPos + ROW_TOGGLE_Y)
                .size(TOGGLE_WIDTH, 16).build());

        this.fieldValue = addField(ROW_REGION_Y, Component.translatable(key("admin.label_value")), 12);
        this.chapterButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleChapter())
                .pos(this.leftPos + EDIT_X, this.topPos + ROW_COLOR_Y).size(EDIT_WIDTH, 16).build());

        this.fieldFilter = new EditBox(this.font, this.leftPos + LIST_X, this.topPos + FILTER_Y, LIST_WIDTH, FIELD_HEIGHT,
                Component.translatable(key("admin.filter")));
        this.fieldFilter.setResponder(value -> {
            rebuildValueKeys();
            this.scrollBar.setValue(0);
        });
        addRenderableWidget(this.fieldFilter);

        int buttonY = this.topPos + contentHeight() - BUTTON_ROW_FROM_BOTTOM;
        this.addRegionButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.add_region")),
                button -> addRegion()).pos(this.leftPos + 8, buttonY).size(68, 18).build());
        this.addEntryButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.add_entry")),
                button -> addEntry()).pos(this.leftPos + 80, buttonY).size(68, 18).build());
        this.removeButton = addRenderableWidget(new ConfirmButton(this.leftPos + 152, buttonY, 68, 18,
                Component.translatable(key("admin.remove")), Component.translatable(key("admin.remove_confirm")),
                this::removeSelected));
        this.refreshButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.refresh")),
                button -> ClientActions.sendAdminPanelRequest())
                .pos(this.leftPos + 224, buttonY).size(24, 18).build());
        this.submitButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.submit")),
                button -> submit()).pos(this.leftPos + 252, buttonY).size(64, 18).build());
        this.closeButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.close")),
                button -> onClose()).pos(this.leftPos + 320, buttonY).size(52, 18).build());

        this.adminButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.manage_admins")),
                button -> toggleAdminWindow()).pos(this.leftPos + PANEL_WIDTH - 8 - 72, this.topPos + ADMIN_BUTTON_Y)
                        .size(72, FIELD_HEIGHT + 2).build());
        this.fieldAdminName = new EditBox(this.font, this.leftPos + ADMIN_WINDOW_X + 6,
                this.topPos + ADMIN_WINDOW_Y + ADMIN_FIELD_Y, ADMIN_FIELD_WIDTH, FIELD_HEIGHT,
                Component.translatable(key("admin.label_admin_name")));
        this.fieldAdminName.setMaxLength(ModConstants.MAX_PLAYER_NAME_LENGTH);
        addRenderableWidget(this.fieldAdminName);
        this.adminAddButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.admin_add")),
                button -> addAdmin()).pos(this.leftPos + ADMIN_WINDOW_X + 152,
                        this.topPos + ADMIN_WINDOW_Y + ADMIN_FIELD_Y - 1).size(46, FIELD_HEIGHT + 2).build());
        this.adminRemoveButton = addRenderableWidget(new ConfirmButton(this.leftPos + ADMIN_WINDOW_X + 202,
                this.topPos + ADMIN_WINDOW_Y + ADMIN_FIELD_Y - 1, 50, FIELD_HEIGHT + 2,
                Component.translatable(key("admin.remove")), Component.translatable(key("admin.remove_confirm")),
                this::removeSelectedAdmin));
        this.adminCloseButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.close")),
                button -> closeAdminWindow()).pos(this.leftPos + ADMIN_WINDOW_X + 6,
                        this.topPos + ADMIN_WINDOW_Y + ADMIN_CLOSE_Y).size(64, 16).build());

        this.adminLayout = null;
        this.widgetsReady = true;
        rebuildPrizeTable();
        rebuildValueTable();
        rebuildAdminTable();
        refreshFields();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        graphics.fill(panelLeft() - 1, panelTop() - 1, panelRight() + 1, panelBottom() + 1, PANEL_BORDER_COLOR);
        graphics.fill(panelLeft(), panelTop(), panelRight(), panelBottom(), PANEL_COLOR);
        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + TITLE_Y, TITLE_COLOR, false);

        this.removeButton.tickConfirm();
        this.adminRemoveButton.tickConfirm();

        /*
         * The two layers are exclusive: while a sub page is open the page behind it is not drawn at all, so no text of
         * the page can ever end up under or over the sub page. The scrim only keeps the cleared page dark, it does not
         * cover anything that was drawn.
         */
        if (subpageOpen())
        {
            drawSubPageScrim(graphics);
        }
        else
        {
            drawList(graphics, mouseX, mouseY);
        }

        if (this.commandEditorOpen)
        {
            drawCommandWindow(graphics);
        }

        if (this.adminWindowOpen)
        {
            drawAdminWindow(graphics, mouseX, mouseY);
        }

        /*
         * The footer belongs to both layers: the status on the left and the author on the right, truncated so the two
         * never overlap, and painted after the sub page so a rejection of a sub page stays readable.
         */
        int footerY = this.topPos + contentHeight() - FOOTER_FROM_BOTTOM;
        graphics.drawString(this.font, this.font.plainSubstrByWidth(this.status, EDIT_X - 16), this.leftPos + 8,
                footerY, STATUS_COLOR, false);
        graphics.drawString(this.font, Component.translatable("gui." + ModConstants.MOD_ID + ".author"),
                this.leftPos + EDIT_X, footerY, LABEL_COLOR, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        /* Labels and hover tooltips belong to the page behind a sub page, which owns neither while it is open. */
        if (!subpageOpen())
        {
            drawFieldLabels(graphics);
            drawRowTooltip(graphics, mouseX, mouseY);
        }
    }

    /** Dims the page behind a sub page so the two layers never read as one surface. */
    private void drawSubPageScrim(GuiGraphics graphics)
    {
        graphics.fill(panelLeft(), panelTop(), panelRight(), panelBottom(), SUBPAGE_SCRIM_COLOR);
    }

    /* The panel frame, which is the content area grown by the padding on every side. */
    private int panelLeft()
    {
        return this.leftPos - PANEL_PADDING;
    }

    private int panelTop()
    {
        return this.topPos - PANEL_PADDING_Y;
    }

    private int panelRight()
    {
        return this.leftPos + PANEL_WIDTH + PANEL_PADDING;
    }

    private int panelBottom()
    {
        return this.topPos + contentHeight() + PANEL_PADDING_Y;
    }

    /*
     * Rows the list shows. The window height decides them and the content grows with them, so a tall window lists more
     * entries while a short one still keeps the footer and the buttons inside the frame.
     */
    private int listRows()
    {
        int spare = this.height - 2 * (PANEL_PADDING_Y + PANEL_MARGIN) - BASE_CONTENT_HEIGHT;
        int rows = LIST_DEFAULT_ROWS + spare / ModConstants.TABLE_ROW_HEIGHT;
        return Math.max(LIST_MIN_ROWS, Math.min(LIST_MAX_ROWS, rows));
    }

    /*
     * Rows the visible list of the current tab fills: the height between its header and the gap above the footer, so
     * the table reaches the bottom of the left column instead of leaving empty space under its last row.
     */
    private int visibleRows()
    {
        int listTop = this.valueTab ? VALUE_LIST_Y : PRIZE_LIST_Y;
        int listBottom = contentHeight() - FOOTER_FROM_BOTTOM - LIST_BOTTOM_GAP;
        return Math.max(1, (listBottom - listTop - ModConstants.TABLE_HEADER_HEIGHT) / ModConstants.TABLE_ROW_HEIGHT);
    }

    private int contentHeight()
    {
        return Math.max(BASE_CONTENT_HEIGHT,
                BASE_CONTENT_HEIGHT + (listRows() - LIST_DEFAULT_ROWS) * ModConstants.TABLE_ROW_HEIGHT);
    }

    private void drawCommandWindow(GuiGraphics graphics)
    {
        int windowX = this.leftPos + COMMAND_WINDOW_X;
        int windowY = this.topPos + COMMAND_WINDOW_Y;
        graphics.fill(windowX - 1, windowY - 1, windowX + COMMAND_WINDOW_WIDTH + 1,
                windowY + COMMAND_WINDOW_HEIGHT + 1, WINDOW_BORDER_COLOR);
        graphics.fill(windowX, windowY, windowX + COMMAND_WINDOW_WIDTH, windowY + COMMAND_WINDOW_HEIGHT, WINDOW_COLOR);
        graphics.drawString(this.font, Component.translatable(key("admin.label_command")), windowX + 6, windowY + 6,
                TITLE_COLOR, false);
    }

    /**
     * The creator is listed first and carries the creator role, the granted admins follow and can be revoked.
     */
    private void drawAdminWindow(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int windowX = this.leftPos + ADMIN_WINDOW_X;
        int windowY = this.topPos + ADMIN_WINDOW_Y;
        graphics.fill(windowX - 1, windowY - 1, windowX + ADMIN_WINDOW_WIDTH + 1,
                windowY + ADMIN_WINDOW_HEIGHT + 1, WINDOW_BORDER_COLOR);
        graphics.fill(windowX, windowY, windowX + ADMIN_WINDOW_WIDTH, windowY + ADMIN_WINDOW_HEIGHT, WINDOW_COLOR);
        graphics.drawString(this.font, Component.translatable(key("admin.manage_admins")), windowX + 6, windowY + 6,
                TITLE_COLOR, false);
        /*
         * The name field names itself through its own hint text; a separate caption above it would sit on the last row
         * of the list as soon as the window lists six entries.
         */

        int listX = windowX + ADMIN_LIST_X;
        int headerY = windowY + ADMIN_LIST_Y;
        int rowsY = headerY + ModConstants.TABLE_HEADER_HEIGHT;
        int visible = Math.min(ADMIN_VISIBLE_ROWS, adminDisplayRows().size());

        graphics.fill(listX, headerY, listX + ADMIN_LIST_WIDTH, headerY + ModConstants.TABLE_HEADER_HEIGHT,
                HEADER_COLOR);
        this.adminLayout.drawHeader(graphics, listX, headerY + 2, HEADER_TEXT_COLOR);

        for (int row = 0; row < visible; row++)
        {
            int y = rowsY + row * ModConstants.TABLE_ROW_HEIGHT;
            boolean selected = row == this.selectedAdminRow;
            graphics.fill(listX, y, listX + ADMIN_LIST_WIDTH, y + ModConstants.TABLE_ROW_HEIGHT - 1,
                    selected ? ROW_SELECTED_COLOR : ROW_COLOR);
            String name = adminDisplayRows().get(row);
            boolean creatorRow = isCreatorRow(row);
            this.adminLayout.drawCell(graphics, 0, name, listX, y + 2, creatorRow ? CREATOR_COLOR : TEXT_COLOR);
            this.adminLayout.drawCell(graphics, 1, Component.translatable(key(creatorRow
                    ? "admin.role_creator" : "admin.role_admin")).getString(), listX, y + 2, LABEL_COLOR);
        }
    }

    /**
     * Rows of the manager window: the creator first, then the granted admins without a duplicate of the creator.
     */
    private List<String> adminDisplayRows()
    {
        List<String> rows = new ArrayList<String>();

        if (!this.creatorName.isEmpty())
        {
            rows.add(this.creatorName);
        }

        for (String admin : this.admins)
        {
            if (admin == null || admin.trim().isEmpty() || admin.equalsIgnoreCase(this.creatorName))
            {
                continue;
            }

            rows.add(admin);
        }

        return rows;
    }

    private boolean isCreatorRow(int row)
    {
        return row == 0 && !this.creatorName.isEmpty();
    }

    /**
     * The creator row is never removable, which is what keeps the creator from deleting themselves.
     */
    private boolean canRemoveSelectedAdmin()
    {
        return this.canManageAdmins && this.selectedAdminRow >= 0 && !isCreatorRow(this.selectedAdminRow)
                && this.selectedAdminRow < adminDisplayRows().size();
    }

    private int adminRowAt(double mouseX, double mouseY)
    {
        int listX = this.leftPos + ADMIN_WINDOW_X + ADMIN_LIST_X;
        int listY = this.topPos + ADMIN_WINDOW_Y + ADMIN_LIST_Y + ModConstants.TABLE_HEADER_HEIGHT;
        int rows = Math.min(ADMIN_VISIBLE_ROWS, adminDisplayRows().size());

        if (mouseX < listX || mouseX > listX + ADMIN_LIST_WIDTH || mouseY < listY)
        {
            return -1;
        }

        int row = (int) ((mouseY - listY) / ModConstants.TABLE_ROW_HEIGHT);
        return row >= 0 && row < rows ? row : -1;
    }

    /** At most one sub page is open, because both draw above the page and take the pointer. */
    private boolean subpageOpen()
    {
        return this.adminWindowOpen || this.commandEditorOpen;
    }

    /** The admin button opens its sub page and closes it again on a repeated click. */
    private void toggleAdminWindow()
    {
        if (this.adminWindowOpen)
        {
            closeAdminWindow();
            return;
        }

        openAdminWindow();
    }

    private void openAdminWindow()
    {
        closeCommandEditor(false);
        this.adminWindowOpen = true;
        this.selectedAdminRow = -1;
        rebuildAdminTable();
        refreshFields();
        /* The window is opened to type a name, so the field takes the keyboard right away. */
        this.setInitialFocus(this.fieldAdminName);
    }

    private void closeAdminWindow()
    {
        this.adminWindowOpen = false;
        this.setFocused(null);
        refreshFields();
    }

    /**
     * Adds the typed name to the admin list. The server re-checks that the requester is the creator.
     * An empty field, the creator himself and a name that is already granted are refused here, so the panel never
     * sends a request that would only be rejected or stored twice.
     */
    private void addAdmin()
    {
        String name = this.fieldAdminName.getValue() == null ? "" : this.fieldAdminName.getValue().trim();

        if (name.isEmpty())
        {
            this.status = Component.translatable(key("admin.admin_name_required")).getString();
            return;
        }

        if (name.equalsIgnoreCase(this.creatorName) || adminDisplayRows().contains(name)
                || containsIgnoreCase(adminDisplayRows(), name))
        {
            this.status = Component.translatable(key("admin.admin_exists")).getString();
            return;
        }

        ClientActions.sendAdminEdit(true, name);
        this.fieldAdminName.setValue("");
        this.status = Component.translatable(key("admin.status_idle")).getString();
    }

    private static boolean containsIgnoreCase(List<String> names, String name)
    {
        for (String candidate : names)
        {
            if (candidate != null && candidate.equalsIgnoreCase(name))
            {
                return true;
            }
        }

        return false;
    }

    private void removeSelectedAdmin()
    {
        if (!canRemoveSelectedAdmin())
        {
            this.status = Component.translatable(key("admin.creator_protected")).getString();
            return;
        }

        ClientActions.sendAdminEdit(false, adminDisplayRows().get(this.selectedAdminRow));
    }

    /**
     * Characters never travel through keyPressed, so a sub page forwards them to its own field instead of relying on
     * the focus of the screen alone.
     */
    @Override
    public boolean charTyped(char codePoint, int modifiers)
    {
        if (this.commandEditorOpen)
        {
            return this.commandEditor.charTyped(codePoint, modifiers);
        }

        if (this.adminWindowOpen)
        {
            return this.fieldAdminName.charTyped(codePoint, modifiers);
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (this.commandEditorOpen)
        {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE)
            {
                closeCommandEditor(false);
                return true;
            }

            return this.commandEditor.keyPressed(keyCode, scanCode, modifiers);
        }

        if (this.adminWindowOpen)
        {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE)
            {
                closeAdminWindow();
                return true;
            }

            return this.fieldAdminName.keyPressed(keyCode, scanCode, modifiers);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (this.commandEditorOpen)
        {
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (this.adminWindowOpen)
        {
            int adminRow = adminRowAt(mouseX, mouseY);

            if (adminRow >= 0)
            {
                this.selectedAdminRow = adminRow;
                refreshFields();
                return true;
            }

            /* The admin button stays reachable, because a repeated click closes the window again. */
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (this.scrollBar.mouseClicked(mouseX, mouseY, button))
        {
            this.tooltip.reset();
            return true;
        }

        int row = listRowAt(mouseX, mouseY);

        if (row >= 0)
        {
            applyFieldsToSelection();
            selectRow(row, Screen.hasControlDown());
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (!subpageOpen() && this.scrollBar.mouseDragged(mouseX, mouseY, button))
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (this.commandEditorOpen)
        {
            return this.commandEditor.mouseScrolled(mouseX, mouseY, delta);
        }

        if (this.adminWindowOpen)
        {
            return true;
        }

        if (this.scrollBar.isActive())
        {
            this.scrollBar.scrollBy(delta > 0.0D ? -1 : 1);
            this.tooltip.reset();
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
        this.scrollBar.setValue(0);
        this.tooltip.reset();
        refreshFields();
    }

    /**
     * Table of the current tab: a frozen header row, the scrolling rows, the scroll bar and the hover tooltip.
     */
    private void drawList(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int listX = this.leftPos + LIST_X;
        int headerY = this.topPos + (this.valueTab ? VALUE_LIST_Y : PRIZE_LIST_Y);
        int rowsY = headerY + ModConstants.TABLE_HEADER_HEIGHT;
        TableLayout layout = this.valueTab ? this.valueLayout : this.prizeLayout;
        int total = this.valueTab ? this.valueKeys.size() : this.prizeRows.size();
        int visible = Math.min(visibleRows(), Math.max(0, total - this.scrollBar.value()));

        graphics.fill(listX, headerY, listX + LIST_WIDTH, headerY + ModConstants.TABLE_HEADER_HEIGHT, HEADER_COLOR);
        layout.drawHeader(graphics, listX, headerY + 2, HEADER_TEXT_COLOR);

        for (int line = 0; line < visible; line++)
        {
            int index = this.scrollBar.value() + line;
            int y = rowsY + line * ModConstants.TABLE_ROW_HEIGHT;
            boolean selected = this.valueTab ? isSelectedValueRow(index) : isSelectedPrizeRow(index);
            graphics.fill(listX, y, listX + LIST_WIDTH, y + ModConstants.TABLE_ROW_HEIGHT - 1,
                    selected ? ROW_SELECTED_COLOR : ROW_COLOR);
            drawRow(graphics, layout, index, listX, y);
        }

        this.scrollBar.render(graphics);
    }

    private void drawRow(GuiGraphics graphics, TableLayout layout, int index, int listX, int y)
    {
        if (this.valueTab)
        {
            String id = this.valueKeys.get(index);
            int color = isRegisteredValue(id) ? TEXT_COLOR : MUTED_COLOR;
            layout.drawCell(graphics, 0, nameOf(id), listX, y + 2, color);
            layout.drawCell(graphics, 1, Integer.toString(valueOfKey(id)), listX, y + 2, color);
            return;
        }

        Row row = this.prizeRows.get(index);

        if (row instanceof RegionRow)
        {
            RegionRow regionRow = (RegionRow) row;
            layout.drawCell(graphics, 0, Integer.toString(regionRow.region.tierOrZero()), listX, y + 2,
                    CREATOR_COLOR);
            layout.drawCell(graphics, 1, Component.translatable(key("admin.region_row"),
                    Integer.valueOf(regionRow.region.entriesOrEmpty().size())).getString(), listX, y + 2, LABEL_COLOR);
            return;
        }

        EntryRow entryRow = (EntryRow) row;
        PrizeEntry entry = entryRow.entry;
        layout.drawCell(graphics, 0, Integer.toString(entryRow.region.tierOrZero()), listX, y + 2, TEXT_COLOR);
        layout.drawCell(graphics, 1, entry.displayName(), listX, y + 2, TEXT_COLOR);
        layout.drawCell(graphics, 2, formatWeight(entry.weightOrZero()), listX, y + 2, TEXT_COLOR);
        layout.drawCell(graphics, 3, Component.translatable(key(entry.isRed()
                ? "admin.toggle_red" : "admin.toggle_black")).getString(), listX, y + 2,
                entry.isRed() ? TEXT_COLOR : MUTED_COLOR);
    }

    /**
     * The hover tooltip completes a clipped cell and, for a value row, names the registry id the row stands for.
     */
    private void drawRowTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if (subpageOpen())
        {
            this.tooltip.render(graphics, this.font, null, null, mouseX, mouseY);
            return;
        }

        int index = listRowAt(mouseX, mouseY);

        if (index < 0)
        {
            this.tooltip.render(graphics, this.font, null, null, mouseX, mouseY);
            return;
        }

        TableLayout layout = this.valueTab ? this.valueLayout : this.prizeLayout;
        List<Component> lines = new ArrayList<Component>();
        String hoverKey;

        if (this.valueTab)
        {
            String id = this.valueKeys.get(index);
            hoverKey = "value:" + id;

            if (layout.isClipped(0, nameOf(id)))
            {
                lines.add(Component.literal(nameOf(id)));
            }

            lines.add(ItemLabels.idTooltip(id));

            if (!isRegisteredValue(id))
            {
                lines.add(Component.translatable(key("admin.value_unregistered")));
            }
        }
        else
        {
            Row row = this.prizeRows.get(index);
            hoverKey = "prize:" + index;

            if (row instanceof RegionRow)
            {
                this.tooltip.render(graphics, this.font, null, null, mouseX, mouseY);
                return;
            }

            String name = ((EntryRow) row).entry.displayName();

            if (!layout.isClipped(1, name))
            {
                this.tooltip.render(graphics, this.font, null, null, mouseX, mouseY);
                return;
            }

            lines.add(Component.literal(name));
        }

        this.tooltip.render(graphics, this.font, hoverKey, lines, mouseX, mouseY);
    }

    private void drawFieldLabels(GuiGraphics graphics)
    {
        if (this.valueTab)
        {
            drawLabel(graphics, this.fieldValue);
            return;
        }

        if (selectedRegionRow() != null)
        {
            drawLabel(graphics, this.fieldTier);
            return;
        }

        if (selectedEntryRow() != null)
        {
            drawLabel(graphics, this.fieldId);
            drawLabel(graphics, this.fieldWeight);
            drawLabel(graphics, this.fieldCommand);
            drawLabel(graphics, this.fieldNotifyText);
        }
    }

    /**
     * A label sits right above the field it names, so the two always read as one control.
     */
    private void drawLabel(GuiGraphics graphics, EditBox box)
    {
        if (box.visible)
        {
            graphics.drawString(this.font, box.getMessage(), box.getX(), box.getY() - LABEL_GAP, LABEL_COLOR, false);
        }
    }

    /**
     * The translated name of a value row, which is what the list shows instead of the raw id.
     */
    private String nameOf(String id)
    {
        return this.entityChapter ? ItemLabels.entityName(id) : ItemLabels.itemName(id);
    }

    private boolean isRegisteredValue(String id)
    {
        return this.entityChapter ? this.itemValues.isRegisteredEntity(id) : this.itemValues.isRegisteredItem(id);
    }

    private int listRowAt(double mouseX, double mouseY)
    {
        int listY = this.topPos + (this.valueTab ? VALUE_LIST_Y : PRIZE_LIST_Y) + ModConstants.TABLE_HEADER_HEIGHT;

        if (mouseX < this.leftPos + LIST_X || mouseX > this.leftPos + LIST_X + LIST_WIDTH)
        {
            return -1;
        }

        int line = (int) Math.floor((mouseY - listY) / ModConstants.TABLE_ROW_HEIGHT);

        if (line < 0 || line >= visibleRows())
        {
            return -1;
        }

        int index = this.scrollBar.value() + line;
        int total = this.valueTab ? this.valueKeys.size() : this.prizeRows.size();
        return index < total ? index : -1;
    }

    /**
     * Selection of one row. The control key adds a row to the current selection, a plain click replaces it, which is
     * what keeps a single pick and a multiple pick on the same gesture.
     */
    private void selectRow(int index, boolean add)
    {
        List<Integer> selection = this.valueTab ? this.selectedValueIndices : this.selectedPrizeIndices;

        if (!add)
        {
            selection.clear();
        }
        else
        {
            selection.remove(Integer.valueOf(index));
        }

        selection.add(Integer.valueOf(index));
        this.tooltip.reset();
        refreshFields();
    }

    private boolean isSelectedValueRow(int index)
    {
        return this.selectedValueIndices.contains(Integer.valueOf(index));
    }

    private boolean isSelectedPrizeRow(int index)
    {
        return this.selectedPrizeIndices.contains(Integer.valueOf(index));
    }

    /**
     * Primary row of a selection, which is the row whose fields the panel shows and the row an entry name is read
     * from.
     */
    private int primaryValueIndex()
    {
        return this.selectedValueIndices.isEmpty() ? -1
                : this.selectedValueIndices.get(this.selectedValueIndices.size() - 1).intValue();
    }

    private int primaryPrizeIndex()
    {
        return this.selectedPrizeIndices.isEmpty() ? -1
                : this.selectedPrizeIndices.get(this.selectedPrizeIndices.size() - 1).intValue();
    }

    private List<String> selectedValueKeys()
    {
        List<String> keys = new ArrayList<String>();

        for (Integer index : this.selectedValueIndices)
        {
            if (index.intValue() >= 0 && index.intValue() < this.valueKeys.size())
            {
                keys.add(this.valueKeys.get(index.intValue()));
            }
        }

        return keys;
    }

    private List<EntryRow> selectedEntryRows()
    {
        List<EntryRow> rows = new ArrayList<EntryRow>();

        for (Integer index : this.selectedPrizeIndices)
        {
            if (index.intValue() >= 0 && index.intValue() < this.prizeRows.size()
                    && this.prizeRows.get(index.intValue()) instanceof EntryRow)
            {
                rows.add((EntryRow) this.prizeRows.get(index.intValue()));
            }
        }

        return rows;
    }

    private void refreshFields()
    {
        /*
         * An armed delete button belongs to the selection it was armed on, so any change of selection or of the
         * pushed configuration drops it back to the plain action.
         */
        this.removeButton.release();
        this.adminRemoveButton.release();

        RegionRow regionRow = selectedRegionRow();
        EntryRow entryRow = selectedEntryRow();

        /*
         * A sub page is modal: it draws its own opaque frame above the page and owns the pointer, so every widget of
         * the page behind it is hidden instead of showing through it or drawing over it.
         */
        boolean main = !subpageOpen();

        setVisible(this.tabPrizeButton, main);
        setVisible(this.tabValueButton, main);
        setVisible(this.addRegionButton, main);
        setVisible(this.addEntryButton, main);
        setVisible(this.removeButton, main);
        setVisible(this.refreshButton, main);
        setVisible(this.submitButton, main);
        setVisible(this.closeButton, main);
        setVisible(this.fieldTier, main && regionRow != null);
        setVisible(this.colorButton, main && entryRow != null);
        setVisible(this.majorEventButton, main && entryRow != null);
        setVisible(this.fieldId, main && entryRow != null);
        setVisible(this.fieldWeight, main && entryRow != null);
        setVisible(this.fieldCommand, main && entryRow != null);
        setVisible(this.commandEditButton, main && entryRow != null);
        setVisible(this.fieldNotifyText, main && entryRow != null);
        setVisible(this.notifyModeButton, main && entryRow != null);
        setVisible(this.enabledButton, main && entryRow != null);
        setVisible(this.fieldValue, main && this.valueTab);
        setVisible(this.chapterButton, main && this.valueTab);
        setVisible(this.fieldFilter, main && this.valueTab);
        setVisible(this.commandEditor, this.commandEditorOpen);
        setVisible(this.commandApplyButton, this.commandEditorOpen);
        setVisible(this.commandCancelButton, this.commandEditorOpen);
        setVisible(this.fieldAdminName, this.adminWindowOpen);
        setVisible(this.adminAddButton, this.adminWindowOpen);
        setVisible(this.adminRemoveButton, this.adminWindowOpen);
        setVisible(this.adminCloseButton, this.adminWindowOpen);
        /* The admin button stays live while its own sub page is open, because it is also the toggle that closes it. */
        setVisible(this.adminButton, this.canManageAdmins);
        this.adminRemoveButton.active = canRemoveSelectedAdmin();

        if (regionRow != null)
        {
            this.fieldTier.setValue(Integer.toString(regionRow.region.tierOrZero()));
        }

        if (entryRow != null)
        {
            this.fieldId.setValue(entryRow.entry.displayName());
            this.fieldWeight.setValue(formatWeight(entryRow.entry.weightOrZero()));
            this.fieldCommand.setValue(escapeLines(entryRow.entry.commandOrEmpty()));
            this.fieldNotifyText.setValue(entryRow.entry.notifyOrDefault().textOrEmpty());
            this.notifyModeButton.setMessage(
                    Component.translatable(key("notify.mode." + entryRow.entry.notifyOrDefault().modeOrDefault())));
            this.enabledButton.setMessage(Component.translatable(key(entryRow.entry.isEnabled()
                    ? "admin.toggle_on" : "admin.toggle_off")));
            this.colorButton.setMessage(Component.translatable(key(entryRow.entry.isRed()
                    ? "admin.toggle_red" : "admin.toggle_black")));
            this.majorEventButton.setMessage(Component.translatable(key(entryRow.entry.isMajorEvent()
                    ? "admin.major_event_on" : "admin.major_event_off")));
            this.baselineWeight = entryRow.entry.weightOrZero();
            this.baselineRed = entryRow.entry.isRed();
            this.baselineMajor = entryRow.entry.isMajorEvent();
            this.baselineEnabled = entryRow.entry.isEnabled();
            this.baselineCommand = escapeLines(entryRow.entry.commandOrEmpty());
            this.baselineNotifyText = entryRow.entry.notifyOrDefault().textOrEmpty();
            this.entryBaselineReady = true;
        }
        else
        {
            this.entryBaselineReady = false;
        }

        if (this.valueTab)
        {
            this.chapterButton.setMessage(Component.translatable(key(this.entityChapter
                    ? "admin.chapter_entity" : "admin.chapter_item")));
            this.fieldValue.setValue(Integer.toString(selectedValue()));
            this.baselineValue = selectedValue();
            this.valueBaselineReady = true;
        }
        else
        {
            this.valueBaselineReady = false;
        }

        int total = this.valueTab ? this.valueKeys.size() : this.prizeRows.size();
        this.scrollBar.setBounds(this.leftPos + LIST_X + LIST_WIDTH + SCROLL_GAP,
                this.topPos + (this.valueTab ? VALUE_LIST_Y : PRIZE_LIST_Y), ModConstants.TABLE_HEADER_HEIGHT
                        + visibleRows() * ModConstants.TABLE_ROW_HEIGHT);
        this.scrollBar.setRange(Math.max(0, total - visibleRows()), visibleRows());
        reportCommandStatus();
    }

    /**
     * The single line field shows a literal line break escape so that a multi line command stays inside the compact
     * form; the floating editor works on the natural text and both helpers keep the two in sync.
     */
    private static String escapeLines(String raw)
    {
        return raw == null ? "" : raw.replace("\n", "\\n");
    }

    private static String unescapeLines(String field)
    {
        return field == null ? "" : field.replace("\\n", "\n");
    }

    /**
     * Opens the floating command editor on the selected entry, with the command in its natural multi line form.
     */
    private void openCommandEditor()
    {
        EntryRow entryRow = selectedEntryRow();

        if (entryRow == null || this.commandEditor == null)
        {
            return;
        }

        /* Only one sub page at a time, so opening the editor puts the manager window away. */
        closeAdminWindow();
        this.commandEditor.setValue(entryRow.entry.commandOrEmpty());
        this.commandEditorOpen = true;
        refreshFields();
        this.setInitialFocus(this.commandEditor);
    }

    /**
     * Closes the floating command editor, writing the edited text back when the caller asked to apply it.
     */
    private void closeCommandEditor(boolean apply)
    {
        EntryRow entryRow = selectedEntryRow();

        if (apply && entryRow != null && this.commandEditor != null)
        {
            String raw = this.commandEditor.getValue();
            entryRow.entry.command = raw;
            this.fieldCommand.setValue(escapeLines(raw));
        }

        this.commandEditorOpen = false;

        if (this.commandEditor != null)
        {
            this.setFocused(null);
        }

        refreshFields();
        reportCommandStatus();
    }

    /**
     * Fields of the selected row are written back before the selection changes.
     * The entry name belongs to a single row, every other entry field is shared with a multiple selection.
     */
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
            String typed = this.fieldId.getValue() == null ? "" : this.fieldId.getValue();

            /* An untouched preset name keeps its language binding instead of freezing the current language. */
            if (!typed.equals(entryRow.entry.displayName()))
            {
                entryRow.entry.id = typed;
            }

            double weight = Math.max(0.0D, parseDouble(this.fieldWeight.getValue(), entryRow.entry.weightOrZero()));
            String command = unescapeLines(this.fieldCommand.getValue());
            String notifyText = this.fieldNotifyText.getValue();
            String mode = entryRow.entry.notifyOrDefault().modeOrDefault();
            /*
             * Only a field the admin really edited is spread over the rest of the selection: the others keep whatever
             * they had, so a multiple selection never unifies weight, colour or command of untouched rows.
             */
            boolean weightEdited = !this.entryBaselineReady || Math.abs(weight - this.baselineWeight) > 1e-9D;
            boolean commandEdited = !this.entryBaselineReady || !command.equals(this.baselineCommand);
            boolean notifyEdited = !this.entryBaselineReady || !notifyText.equals(this.baselineNotifyText);
            boolean colorEdited = this.entryBaselineReady && entryRow.entry.isRed() != this.baselineRed;
            boolean majorEdited = this.entryBaselineReady && entryRow.entry.isMajorEvent() != this.baselineMajor;
            boolean enabledEdited = this.entryBaselineReady && entryRow.entry.isEnabled() != this.baselineEnabled;

            for (EntryRow selected : selectedEntryRows())
            {
                boolean primary = selected == entryRow;

                if (primary || weightEdited)
                {
                    selected.entry.weight = Double.valueOf(weight);
                }

                if (primary || colorEdited)
                {
                    selected.entry.color = entryRow.entry.colorOrDefault();
                }

                if (primary || majorEdited)
                {
                    selected.entry.majorEvent = Boolean.valueOf(entryRow.entry.isMajorEvent());
                }

                if (primary || enabledEdited)
                {
                    selected.entry.enabled = Boolean.valueOf(entryRow.entry.isEnabled());
                }

                if (primary || commandEdited)
                {
                    selected.entry.command = command;
                }

                if (primary || notifyEdited)
                {
                    selected.entry.notify = new NotifyConfig(mode, notifyText);
                }
            }

            this.baselineWeight = weight;
            this.baselineRed = entryRow.entry.isRed();
            this.baselineMajor = entryRow.entry.isMajorEvent();
            this.baselineEnabled = entryRow.entry.isEnabled();
            this.baselineCommand = command;
            this.baselineNotifyText = notifyText;
            this.entryBaselineReady = true;
        }

        if (this.valueTab)
        {
            int value = Math.max(0, parseInt(this.fieldValue.getValue(), 0));

            /*
             * The value of the primary row is only written over the selection when the admin typed a different number.
             */
            if (!this.valueBaselineReady || value != this.baselineValue)
            {
                for (String key : selectedValueKeys())
                {
                    setValueOfKey(key, value);
                }
            }

            this.baselineValue = value;
            this.valueBaselineReady = true;
        }

        reportCommandStatus();
    }

    /**
     * Validates the command in the field against the command tree the server sent, and reports the first problem on
     * the status line. An entry with a problem stays editable but the server never selects it.
     */
    private void reportCommandStatus()
    {
        Minecraft minecraft = this.minecraft;

        if (minecraft == null || minecraft.player == null || minecraft.player.connection == null)
        {
            return;
        }

        String raw = this.fieldCommand.getValue().replace("\\n", "\n");
        String problem = CommandRunner.problem(minecraft.player.connection.getCommands(),
                minecraft.player.connection.getSuggestionsProvider(), raw);

        if (problem != null)
        {
            this.status = Component.translatable(key("admin.status_command_invalid"), problem).getString();
        }
    }

    private void toggleColor()
    {
        EntryRow entryRow = selectedEntryRow();

        if (entryRow == null)
        {
            return;
        }

        /* Pending field edits are written back first, then the toggled value is spread over the whole selection. */
        applyFieldsToSelection();
        entryRow.entry.color = entryRow.entry.isRed()
                ? ModConstants.ENTRY_COLOR_BLACK : ModConstants.ENTRY_COLOR_RED;
        applyFieldsToSelection();
        refreshFields();
    }

    private void toggleMajorEvent()
    {
        EntryRow entryRow = selectedEntryRow();

        if (entryRow == null)
        {
            return;
        }

        applyFieldsToSelection();
        entryRow.entry.majorEvent = Boolean.valueOf(!entryRow.entry.isMajorEvent());
        applyFieldsToSelection();
        refreshFields();
    }

    private void toggleChapter()
    {
        applyFieldsToSelection();
        this.entityChapter = !this.entityChapter;
        this.selectedValueIndices.clear();
        this.scrollBar.setValue(0);
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
        applyFieldsToSelection();
        entryRow.entry.notify = new NotifyConfig(next, this.fieldNotifyText.getValue());
        applyFieldsToSelection();
        refreshFields();
    }

    private void toggleEnabled()
    {
        EntryRow entryRow = selectedEntryRow();

        if (entryRow == null)
        {
            return;
        }

        applyFieldsToSelection();
        entryRow.entry.enabled = Boolean.valueOf(!entryRow.entry.isEnabled());
        applyFieldsToSelection();
        refreshFields();
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
        this.selectedPrizeIndices.clear();
        this.selectedPrizeIndices.add(Integer.valueOf(this.prizeRows.size() - 1));
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
                ModConstants.NOTIFY_MODE_TELLRAW, "");
        region.entriesOrEmpty().add(entry);
        rebuildPrizeRows();
        this.selectedPrizeIndices.clear();
        this.selectedPrizeIndices.add(Integer.valueOf(indexOfEntry(region, entry)));
        refreshFields();
    }

    /**
     * Removes every selected row: the selected value entries, the selected prize entries, or the selected region.
     */
    private void removeSelected()
    {
        if (this.valueTab)
        {
            List<String> keys = selectedValueKeys();

            if (!keys.isEmpty())
            {
                for (String key : keys)
                {
                    currentChapter().remove(key);
                }

                this.selectedValueIndices.clear();
                rebuildValueKeys();
                refreshFields();
            }

            return;
        }

        List<EntryRow> entryRows = selectedEntryRows();

        if (!entryRows.isEmpty())
        {
            for (EntryRow entryRow : entryRows)
            {
                entryRow.region.entriesOrEmpty().remove(entryRow.entry);
            }

            this.selectedPrizeIndices.clear();
            rebuildPrizeRows();
            refreshFields();
            return;
        }

        RegionRow regionRow = selectedRegionRow();

        if (regionRow != null && this.prizePool.regionsOrEmpty().size() > 1)
        {
            this.prizePool.regionsOrEmpty().remove(regionRow.region);
            this.selectedPrizeIndices.clear();
            rebuildPrizeRows();
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

        this.selectedPrizeIndices.removeIf(index -> index.intValue() >= this.prizeRows.size());
        rebuildPrizeTable();
    }

    /**
     * Locks the column widths of the prize table: the tier, the entry name, the weight and the colour.
     */
    private void rebuildPrizeTable()
    {
        if (this.font == null)
        {
            return;
        }

        List<String> tiers = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        List<String> weights = new ArrayList<String>();
        List<String> colors = new ArrayList<String>();

        for (Row row : this.prizeRows)
        {
            if (row instanceof RegionRow)
            {
                RegionRow regionRow = (RegionRow) row;
                tiers.add(Integer.toString(regionRow.region.tierOrZero()));
                names.add(Component.translatable(key("admin.region_row"),
                        Integer.valueOf(regionRow.region.entriesOrEmpty().size())).getString());
                weights.add("");
                colors.add("");
            }
            else
            {
                EntryRow entryRow = (EntryRow) row;
                tiers.add(Integer.toString(entryRow.region.tierOrZero()));
                names.add(entryRow.entry.displayName());
                weights.add(formatWeight(entryRow.entry.weightOrZero()));
                colors.add(Component.translatable(key(entryRow.entry.isRed()
                        ? "admin.toggle_red" : "admin.toggle_black")).getString());
            }
        }

        this.prizeLayout = new TableLayout(this.font, LIST_WIDTH);
        this.prizeLayout.addColumn(Component.translatable(key("admin.column_tier")), 0, tiers);
        this.prizeLayout.addColumn(Component.translatable(key("admin.column_entry_name")), 0, names);
        this.prizeLayout.addColumn(Component.translatable(key("admin.column_weight")), 0, weights);
        this.prizeLayout.addColumn(Component.translatable(key("admin.column_color")), 0, colors);
        this.prizeLayout.compute();
    }

    private void rebuildValueKeys()
    {
        this.valueKeys.clear();
        String filter = this.fieldFilter == null ? "" : this.fieldFilter.getValue().trim();
        Map<String, Number> sorted = new TreeMap<String, Number>(currentChapter());

        for (String key : sorted.keySet())
        {
            if (filter.isEmpty() || matchesFilter(key, filter))
            {
                this.valueKeys.add(key);
            }
        }

        this.selectedValueIndices.removeIf(index -> index.intValue() >= this.valueKeys.size());
        rebuildValueTable();
    }

    /**
     * Locks the column widths of the value table: the name and the value.
     */
    private void rebuildValueTable()
    {
        if (this.font == null)
        {
            return;
        }

        List<String> names = new ArrayList<String>();
        List<String> values = new ArrayList<String>();

        for (String key : this.valueKeys)
        {
            names.add(nameOf(key));
            values.add(Integer.toString(valueOfKey(key)));
        }

        this.valueLayout = new TableLayout(this.font, LIST_WIDTH);
        this.valueLayout.addColumn(Component.translatable(key("admin.column_value_name")), 0, names);
        this.valueLayout.addColumn(Component.translatable(key("admin.column_value")), 0, values);
        this.valueLayout.compute();
    }

    private void rebuildAdminTable()
    {
        if (this.font == null)
        {
            return;
        }

        List<String> names = new ArrayList<String>();
        List<String> roles = new ArrayList<String>();

        for (int row = 0; row < adminDisplayRows().size(); row++)
        {
            names.add(adminDisplayRows().get(row));
            roles.add(Component.translatable(key(isCreatorRow(row)
                    ? "admin.role_creator" : "admin.role_admin")).getString());
        }

        this.adminLayout = new TableLayout(this.font, ADMIN_LIST_WIDTH);
        this.adminLayout.addColumn(Component.translatable(key("admin.column_admin_name")), 0, names);
        this.adminLayout.addColumn(Component.translatable(key("admin.column_admin_role")), 0, roles);
        this.adminLayout.compute();
    }

    /**
     * Matches the raw id and the translated name, so a filter works with either what is shown or what is stored.
     */
    private boolean matchesFilter(String key, String filter)
    {
        String needle = filter.toLowerCase(Locale.ROOT);
        return key.toLowerCase(Locale.ROOT).contains(needle)
                || nameOf(key).toLowerCase(Locale.ROOT).contains(needle);
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
        int primary = primaryPrizeIndex();

        if (this.valueTab || primary < 0 || primary >= this.prizeRows.size())
        {
            return null;
        }

        Row row = this.prizeRows.get(primary);
        return row instanceof RegionRow ? (RegionRow) row : null;
    }

    private EntryRow selectedEntryRow()
    {
        int primary = primaryPrizeIndex();

        if (this.valueTab || primary < 0 || primary >= this.prizeRows.size())
        {
            return null;
        }

        Row row = this.prizeRows.get(primary);
        return row instanceof EntryRow ? (EntryRow) row : null;
    }

    private int selectedValue()
    {
        int primary = primaryValueIndex();

        if (primary < 0 || primary >= this.valueKeys.size())
        {
            return 0;
        }

        return valueOfKey(this.valueKeys.get(primary));
    }

    private int revision()
    {
        return this.valueTab ? this.itemValues.revisionOrZero() : this.prizePool.revisionOrZero();
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

    private static String key(String suffix)
    {
        return "gui." + ModConstants.MOD_ID + "." + suffix;
    }

    private abstract static class Row
    {
    }

    private static final class RegionRow extends Row
    {
        private final PrizeRegion region;

        private RegionRow(PrizeRegion region)
        {
            this.region = region;
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
    }
}
