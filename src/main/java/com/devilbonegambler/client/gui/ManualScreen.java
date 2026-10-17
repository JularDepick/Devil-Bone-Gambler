/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.client.manual.ManualContent;
import com.devilbonegambler.client.manual.ManualLine;
import com.devilbonegambler.client.manual.ManualParser;
import com.devilbonegambler.client.manual.ManualRow;
import com.devilbonegambler.client.manual.ManualText;
import com.devilbonegambler.manual.ManualKind;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;

/**
 * The generic book screen of the mod: one screen serves every manual, because a manual is only a kind plus shipped
 * markdown content.
 * The markdown is parsed into rows once per opening, wrapped to the width of the page, and drawn with a draggable
 * scroll bar; the content itself is never written or edited, so a manual always reads the same text.
 */
public class ManualScreen extends Screen
{
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 226;
    private static final int PADDING = 10;
    private static final int ROW_HEIGHT = 10;
    private static final int TITLE_Y = 8;
    private static final int CONTENT_Y = 24;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 16;

    /*
     * The footer band holds the close button and the author line, and the page area stops above it.
     * Both are derived from the same baseline, so the button can never cover the last rows of text again.
     */
    private static final int FOOTER_Y = PANEL_HEIGHT - PADDING - BUTTON_HEIGHT;
    private static final int FOOTER_GAP = 4;
    private static final int CONTENT_BOTTOM = FOOTER_Y - FOOTER_GAP;
    private static final int VISIBLE_ROWS = (CONTENT_BOTTOM - CONTENT_Y) / ROW_HEIGHT;

    private static final int PANEL_COLOR = 0xF01A1410;
    private static final int BORDER_COLOR = 0xFF7A5C34;
    private static final int RULE_COLOR = 0xFF6B5A3A;
    private static final int TITLE_COLOR = 0xFFE8D8A8;
    private static final int HEADING_COLOR = 0xFFD8B860;
    private static final int SUBHEADING_COLOR = 0xFFC9B070;
    private static final int MINOR_COLOR = 0xFFA89058;
    private static final int BODY_COLOR = 0xFFD8D0C0;
    private static final int MARKER_COLOR = 0xFFB0A080;
    private static final int CODE_COLOR = 0xFF8FC8E8;
    private static final int NOTICE_COLOR = 0xFFC08080;
    private static final int AUTHOR_COLOR = 0xFF707070;

    private static final String LANG = "gui." + ModConstants.MOD_ID + ".manual.";

    private final ManualKind kind;
    private final ScrollBar scrollBar = new ScrollBar();
    private List<ManualRow> rows = new ArrayList<ManualRow>();
    private int leftPos;
    private int topPos;
    private int contentWidth;

    public ManualScreen(ManualKind kind)
    {
        super(Component.translatable(kind.titleKey()));
        this.kind = kind;
    }

    @Override
    protected void init()
    {
        super.init();
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;
        this.contentWidth = PANEL_WIDTH - PADDING * 2 - ModConstants.TABLE_SCROLL_WIDTH - 2;
        this.rows = ManualParser.layout(ManualContent.lines(this.kind), this.font, this.contentWidth);
        this.scrollBar.setBounds(this.leftPos + PANEL_WIDTH - PADDING - ModConstants.TABLE_SCROLL_WIDTH,
                this.topPos + CONTENT_Y, VISIBLE_ROWS * ROW_HEIGHT);
        this.scrollBar.setRange(Math.max(0, this.rows.size() - VISIBLE_ROWS), VISIBLE_ROWS);
        this.scrollBar.setValue(0);
        this.addRenderableWidget(Button.builder(Component.translatable(LANG + "close"), button -> this.onClose())
                .bounds(this.leftPos + (PANEL_WIDTH - BUTTON_WIDTH) / 2, this.topPos + FOOTER_Y, BUTTON_WIDTH,
                        BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(graphics);
        int right = this.leftPos + PANEL_WIDTH;
        int bottom = this.topPos + PANEL_HEIGHT;
        graphics.fill(this.leftPos, this.topPos, right, bottom, PANEL_COLOR);
        graphics.fill(this.leftPos, this.topPos, right, this.topPos + 1, BORDER_COLOR);
        graphics.fill(this.leftPos, bottom - 1, right, bottom, BORDER_COLOR);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, bottom, BORDER_COLOR);
        graphics.fill(right - 1, this.topPos, right, bottom, BORDER_COLOR);
        graphics.drawString(this.font, this.title, this.leftPos + PADDING, this.topPos + TITLE_Y, TITLE_COLOR);
        int ruleY = this.topPos + CONTENT_Y - 3;
        graphics.fill(this.leftPos + PADDING, ruleY, right - PADDING, ruleY + 1, RULE_COLOR);
        this.drawPage(graphics);
        this.scrollBar.render(graphics);
        Component author = Component.translatable("gui." + ModConstants.MOD_ID + ".author");
        graphics.drawString(this.font, author, right - PADDING - this.font.width(author),
                bottom - PADDING - this.font.lineHeight, AUTHOR_COLOR);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * The page: every visible row of the parsed markdown, clipped to the page area so a row never leaves the book.
     */
    private void drawPage(GuiGraphics graphics)
    {
        int pageTop = this.topPos + CONTENT_Y;
        int pageBottom = this.topPos + CONTENT_BOTTOM;
        graphics.enableScissor(this.leftPos + PADDING, pageTop, this.leftPos + PANEL_WIDTH - PADDING, pageBottom);

        if (this.rows.isEmpty())
        {
            Component notice = Component.translatable(LANG + "missing");
            graphics.drawString(this.font, notice, this.leftPos + PADDING
                    + (this.contentWidth - this.font.width(notice)) / 2, pageTop, NOTICE_COLOR);
        }
        else
        {
            for (int line = 0; line < VISIBLE_ROWS; line++)
            {
                int index = this.scrollBar.value() + line;

                if (index >= this.rows.size())
                {
                    break;
                }

                drawRow(graphics, this.rows.get(index), this.topPos + CONTENT_Y + line * ROW_HEIGHT);
            }
        }

        graphics.disableScissor();
    }

    private void drawRow(GuiGraphics graphics, ManualRow row, int y)
    {
        if (row.isSpacer())
        {
            return;
        }

        int pageRight = this.leftPos + PANEL_WIDTH - PADDING - ModConstants.TABLE_SCROLL_WIDTH - 2;

        if (row.isSeparator())
        {
            graphics.fill(this.leftPos + PADDING, y + ROW_HEIGHT / 2, pageRight, y + ROW_HEIGHT / 2 + 1, RULE_COLOR);
            return;
        }

        int x = this.leftPos + PADDING + row.indent();

        if (!row.marker().isEmpty())
        {
            String marker = row.marker() + " ";
            graphics.drawString(this.font, marker, x, y, MARKER_COLOR);
            x += this.font.width(marker);
        }

        int color = colorOf(row.block());

        for (ManualText part : row.texts())
        {
            if (part.style() == ManualText.Style.CODE)
            {
                graphics.drawString(this.font, part.text(), x, y, CODE_COLOR);
            }
            else if (part.style() == ManualText.Style.BOLD)
            {
                graphics.drawString(this.font, Component.literal(part.text()).withStyle(ChatFormatting.BOLD), x, y,
                        color);
            }
            else
            {
                graphics.drawString(this.font, part.text(), x, y, color);
            }

            x += part.width(this.font);
        }
    }

    private static int colorOf(ManualLine.Block block)
    {
        if (block == ManualLine.Block.HEADING)
        {
            return HEADING_COLOR;
        }

        if (block == ManualLine.Block.SUBHEADING)
        {
            return SUBHEADING_COLOR;
        }

        if (block == ManualLine.Block.MINOR)
        {
            return MINOR_COLOR;
        }

        return BODY_COLOR;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (!this.scrollBar.isActive())
        {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        this.scrollBar.scrollBy(delta > 0.0D ? -1 : 1);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (this.scrollBar.mouseClicked(mouseX, mouseY, button))
        {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (this.scrollBar.mouseDragged(mouseX, mouseY, button))
        {
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
}
