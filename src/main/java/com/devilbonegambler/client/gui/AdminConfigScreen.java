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
    private static final int COMMAND_FIELD_WIDTH = 126;
    private static final int COMMAND_WINDOW_X = 80;
    private static final int COMMAND_WINDOW_Y = 89;
    private static final int COMMAND_WINDOW_WIDTH = 220;
    private static final int COMMAND_WINDOW_HEIGHT = 96;
    private static final int COMMAND_EDITOR_HEIGHT = 52;
    private static final int WINDOW_COLOR = 0xF0101010;
    private static final int WINDOW_BORDER_COLOR = 0xFF707070;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int TEXT_COLOR = 0xE0E0E0;
    private static final int STATUS_COLOR = 0x80C0FF;
    private static final int CREATOR_COLOR = 0xD8B860;

    private static final int ADMIN_WINDOW_X = 60;
    private static final int ADMIN_WINDOW_Y = 56;
    private static final int ADMIN_WINDOW_WIDTH = 260;
    private static final int ADMIN_WINDOW_HEIGHT = 152;
    private static final int ADMIN_LIST_Y = 22;
    private static final int ADMIN_ROW_HEIGHT = 11;
    private static final int ADMIN_VISIBLE_ROWS = 7;
    private static final int ADMIN_FIELD_Y = 106;
    private static final int ADMIN_FIELD_WIDTH = 142;
    private static final int ADMIN_CLOSE_Y = 128;

    private final List<Row> prizeRows = new ArrayList<Row>();
    private final List<String> valueKeys = new ArrayList<String>();

    private PrizePoolConfig prizePool = PrizePoolConfig.empty();
    private ItemValueConfig itemValues = ItemValueConfig.empty();

    private int leftPos;
    private int topPos;
    private boolean valueTab;
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
    private Button notifyModeButton;
    private Button enabledButton;
    private Button applyBatchButton;
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
    private Button adminRemoveButton;
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
        this.selectedPrizeIndex = -1;
        this.selectedValueIndex = -1;
        this.selectedAdminRow = -1;
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

        this.fieldTier = addField(62, Component.translatable(key("admin.label_region")), 6);
        this.colorButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleColor())
                .pos(this.leftPos + EDIT_X, this.topPos + 88).size(70, 16).build());
        this.majorEventButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleMajorEvent())
                .pos(this.leftPos + EDIT_X + 76, this.topPos + 88).size(70, 16).build());
        this.fieldId = addField(114, Component.translatable(key("admin.label_id")), ModConstants.MAX_ENTRY_ID_LENGTH);
        this.fieldWeight = addField(140, Component.translatable(key("admin.label_weight")), 12);
        this.fieldCommand = new EditBox(this.font, this.leftPos + EDIT_X, this.topPos + 166, COMMAND_FIELD_WIDTH,
                FIELD_HEIGHT, Component.translatable(key("admin.label_command")));
        this.fieldCommand.setMaxLength(ModConstants.MAX_COMMAND_LENGTH);
        addRenderableWidget(this.fieldCommand);
        this.commandEditButton = addRenderableWidget(Button.builder(Component.literal("..."),
                button -> openCommandEditor()).pos(this.leftPos + EDIT_X + COMMAND_FIELD_WIDTH + 2,
                        this.topPos + 166).size(18, FIELD_HEIGHT).build());

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

        this.fieldFilter = new EditBox(this.font, this.leftPos + LIST_X, this.topPos + 54, LIST_WIDTH, FIELD_HEIGHT,
                Component.translatable(key("admin.filter")));
        this.fieldFilter.setResponder(value -> {
            rebuildValueKeys();
            this.scroll = 0;
        });
        addRenderableWidget(this.fieldFilter);

        int buttonY = this.topPos + PANEL_HEIGHT - 22;
        addRenderableWidget(Button.builder(Component.translatable(key("admin.add_region")), button -> addRegion())
                .pos(this.leftPos + 8, buttonY).size(68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(key("admin.add_entry")), button -> addEntry())
                .pos(this.leftPos + 80, buttonY).size(68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(key("admin.remove")),
                button -> removeSelected()).pos(this.leftPos + 152, buttonY).size(68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(key("admin.refresh")),
                button -> ClientActions.sendAdminPanelRequest())
                .pos(this.leftPos + 224, buttonY).size(24, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(key("admin.submit")), button -> submit())
                .pos(this.leftPos + 252, buttonY).size(64, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(key("admin.close")), button -> onClose())
                .pos(this.leftPos + 320, buttonY).size(52, 18).build());

        this.adminButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.manage_admins")),
                button -> openAdminWindow()).pos(this.leftPos + PANEL_WIDTH - 8 - 72, this.topPos + 18)
                        .size(72, FIELD_HEIGHT + 2).build());
        this.fieldAdminName = new EditBox(this.font, this.leftPos + ADMIN_WINDOW_X + 6,
                this.topPos + ADMIN_WINDOW_Y + ADMIN_FIELD_Y, ADMIN_FIELD_WIDTH, FIELD_HEIGHT,
                Component.translatable(key("admin.label_admin_name")));
        this.fieldAdminName.setMaxLength(ModConstants.MAX_PLAYER_NAME_LENGTH);
        addRenderableWidget(this.fieldAdminName);
        this.adminAddButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.admin_add")),
                button -> addAdmin()).pos(this.leftPos + ADMIN_WINDOW_X + 152,
                        this.topPos + ADMIN_WINDOW_Y + ADMIN_FIELD_Y - 1).size(46, FIELD_HEIGHT + 2).build());
        this.adminRemoveButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.remove")),
                button -> removeSelectedAdmin()).pos(this.leftPos + ADMIN_WINDOW_X + 202,
                        this.topPos + ADMIN_WINDOW_Y + ADMIN_FIELD_Y - 1).size(50, FIELD_HEIGHT + 2).build());
        this.adminCloseButton = addRenderableWidget(Button.builder(Component.translatable(key("admin.close")),
                button -> closeAdminWindow()).pos(this.leftPos + ADMIN_WINDOW_X + 6,
                        this.topPos + ADMIN_WINDOW_Y + ADMIN_CLOSE_Y).size(64, 16).build());

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

        if (this.commandEditorOpen)
        {
            int windowX = this.leftPos + COMMAND_WINDOW_X;
            int windowY = this.topPos + COMMAND_WINDOW_Y;
            graphics.fill(windowX - 1, windowY - 1, windowX + COMMAND_WINDOW_WIDTH + 1,
                    windowY + COMMAND_WINDOW_HEIGHT + 1, WINDOW_BORDER_COLOR);
            graphics.fill(windowX, windowY, windowX + COMMAND_WINDOW_WIDTH, windowY + COMMAND_WINDOW_HEIGHT,
                    WINDOW_COLOR);
            graphics.drawString(this.font, Component.translatable(key("admin.label_command")), windowX + 6,
                    windowY + 6, TITLE_COLOR, false);
        }

        if (this.adminWindowOpen)
        {
            drawAdminWindow(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        drawFieldLabels(graphics);
    }

    /**
     * The creator is listed first and carries the creator tag, the granted admins follow and can be revoked.
     */
    private void drawAdminWindow(GuiGraphics graphics)
    {
        int windowX = this.leftPos + ADMIN_WINDOW_X;
        int windowY = this.topPos + ADMIN_WINDOW_Y;
        graphics.fill(windowX - 1, windowY - 1, windowX + ADMIN_WINDOW_WIDTH + 1,
                windowY + ADMIN_WINDOW_HEIGHT + 1, WINDOW_BORDER_COLOR);
        graphics.fill(windowX, windowY, windowX + ADMIN_WINDOW_WIDTH, windowY + ADMIN_WINDOW_HEIGHT, WINDOW_COLOR);
        graphics.drawString(this.font, Component.translatable(key("admin.manage_admins")), windowX + 6, windowY + 6,
                TITLE_COLOR, false);
        graphics.drawString(this.font, Component.translatable(key("admin.label_admin_name")), windowX + 6,
                windowY + ADMIN_FIELD_Y - 10, LABEL_COLOR, false);

        List<String> rows = adminDisplayRows();

        for (int row = 0; row < Math.min(ADMIN_VISIBLE_ROWS, rows.size()); row++)
        {
            int y = windowY + ADMIN_LIST_Y + row * ADMIN_ROW_HEIGHT;
            graphics.fill(windowX + 5, y - 1, windowX + ADMIN_WINDOW_WIDTH - 5, y + ADMIN_ROW_HEIGHT - 2,
                    row == this.selectedAdminRow ? ROW_SELECTED_COLOR : ROW_COLOR);
            boolean creatorRow = isCreatorRow(row);
            String name = rows.get(row);
            String label = creatorRow
                    ? name + " (" + Component.translatable(key("admin.creator_tag")).getString() + ")"
                    : name;
            graphics.drawString(this.font, label, windowX + 8, y, creatorRow ? CREATOR_COLOR : TEXT_COLOR, false);
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
        int listX = this.leftPos + ADMIN_WINDOW_X + 5;
        int listY = this.topPos + ADMIN_WINDOW_Y + ADMIN_LIST_Y;
        int rows = Math.min(ADMIN_VISIBLE_ROWS, adminDisplayRows().size());

        if (mouseX < listX || mouseX > listX + ADMIN_WINDOW_WIDTH - 10 || mouseY < listY)
        {
            return -1;
        }

        int row = (int) ((mouseY - listY) / ADMIN_ROW_HEIGHT);
        return row >= 0 && row < rows ? row : -1;
    }

    private void openAdminWindow()
    {
        this.adminWindowOpen = true;
        this.selectedAdminRow = -1;
        refreshFields();
    }

    private void closeAdminWindow()
    {
        this.adminWindowOpen = false;
        refreshFields();
    }

    /**
     * Adds the typed name to the admin list. The server re-checks that the requester is the creator.
     */
    private void addAdmin()
    {
        String name = this.fieldAdminName.getValue() == null ? "" : this.fieldAdminName.getValue().trim();

        if (name.isEmpty())
        {
            this.status = Component.translatable(key("admin.admin_name_required")).getString();
            return;
        }

        ClientActions.sendAdminEdit(true, name);
        this.fieldAdminName.setValue("");
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
            this.commandEditor.mouseClicked(mouseX, mouseY, button);
            this.commandApplyButton.mouseClicked(mouseX, mouseY, button);
            this.commandCancelButton.mouseClicked(mouseX, mouseY, button);
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

            this.fieldAdminName.mouseClicked(mouseX, mouseY, button);
            this.adminAddButton.mouseClicked(mouseX, mouseY, button);
            this.adminRemoveButton.mouseClicked(mouseX, mouseY, button);
            this.adminCloseButton.mouseClicked(mouseX, mouseY, button);
            return true;
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
        if (this.commandEditorOpen)
        {
            return this.commandEditor.mouseScrolled(mouseX, mouseY, delta);
        }

        if (this.adminWindowOpen)
        {
            return true;
        }

        int total = this.valueTab ? this.valueKeys.size() : this.prizeRows.size();
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
        this.scroll = 0;
        refreshFields();
    }

    private void drawList(GuiGraphics graphics)
    {
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
        RegionRow regionRow = selectedRegionRow();
        EntryRow entryRow = selectedEntryRow();

        setVisible(this.fieldTier, regionRow != null);
        setVisible(this.colorButton, entryRow != null);
        setVisible(this.majorEventButton, entryRow != null);
        setVisible(this.fieldId, entryRow != null);
        setVisible(this.fieldWeight, entryRow != null);
        setVisible(this.fieldCommand, entryRow != null);
        setVisible(this.commandEditButton, entryRow != null);
        setVisible(this.commandEditor, this.commandEditorOpen);
        setVisible(this.commandApplyButton, this.commandEditorOpen);
        setVisible(this.commandCancelButton, this.commandEditorOpen);
        setVisible(this.fieldNotifyText, entryRow != null);
        setVisible(this.notifyModeButton, entryRow != null);
        setVisible(this.enabledButton, entryRow != null);
        setVisible(this.fieldValue, this.valueTab);
        setVisible(this.applyBatchButton, this.valueTab);
        setVisible(this.chapterButton, this.valueTab);
        setVisible(this.fieldFilter, this.valueTab);
        setVisible(this.adminButton, this.canManageAdmins);
        setVisible(this.fieldAdminName, this.adminWindowOpen);
        setVisible(this.adminAddButton, this.adminWindowOpen);
        setVisible(this.adminRemoveButton, this.adminWindowOpen);
        setVisible(this.adminCloseButton, this.adminWindowOpen);
        this.adminRemoveButton.active = canRemoveSelectedAdmin();

        if (regionRow != null)
        {
            this.fieldTier.setValue(Integer.toString(regionRow.region.tierOrZero()));
        }

        if (entryRow != null)
        {
            this.fieldId.setValue(entryRow.entry.id == null ? "" : entryRow.entry.id);
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
        }

        if (this.valueTab)
        {
            this.chapterButton.setMessage(Component.translatable(key(this.entityChapter
                    ? "admin.chapter_entity" : "admin.chapter_item")));
            this.fieldValue.setValue(Integer.toString(selectedValue()));
        }

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

        this.commandEditor.setValue(entryRow.entry.commandOrEmpty());
        this.commandEditorOpen = true;
        this.commandEditor.setFocused(true);
        refreshFields();
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
            this.commandEditor.setFocused(false);
        }

        refreshFields();
        reportCommandStatus();
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
            entryRow.entry.command = unescapeLines(this.fieldCommand.getValue());
            entryRow.entry.notify = new NotifyConfig(entryRow.entry.notifyOrDefault().modeOrDefault(),
                    this.fieldNotifyText.getValue());
        }

        if (this.valueTab && this.selectedValueIndex >= 0 && this.selectedValueIndex < this.valueKeys.size())
        {
            setValueOfKey(this.valueKeys.get(this.selectedValueIndex),
                    Math.max(0, parseInt(this.fieldValue.getValue(), 0)));
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
                ModConstants.NOTIFY_MODE_TELLRAW, "");
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
