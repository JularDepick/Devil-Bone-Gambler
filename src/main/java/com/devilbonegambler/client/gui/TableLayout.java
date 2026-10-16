/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Fixed column widths of a list table.
 * A column first asks for the width of its longest content, never more than the equal share of the table, and the
 * leftover width is then handed out again in proportion to those widths; a column that already reached the equal
 * share never grows again, so a wide column cannot starve the others.
 * A column may also be declared fixed, for example the icon column of the value table, and then it takes part in
 * neither step.
 * Every cell is centred inside its own column and clipped to it; the caller shows the full text as a hover tooltip.
 */
public final class TableLayout
{
    private final Font font;
    private final int totalWidth;
    private final List<Component> titles = new ArrayList<Component>();
    private final List<Integer> declaredWidths = new ArrayList<Integer>();
    private final List<Integer> naturalWidths = new ArrayList<Integer>();
    private final List<Integer> widths = new ArrayList<Integer>();

    public TableLayout(Font font, int totalWidth)
    {
        this.font = font;
        this.totalWidth = Math.max(1, totalWidth);
    }

    /**
     * Adds one column. A positive fixed width pins the column, otherwise the width is measured from the longest of
     * the given cell texts.
     */
    public void addColumn(Component title, int fixedWidth, List<String> cells)
    {
        int natural = fixedWidth;

        if (fixedWidth <= 0)
        {
            int widest = 0;

            for (String cell : cells)
            {
                widest = Math.max(widest, this.font.width(cell == null ? "" : cell));
            }

            natural = widest + ModConstants.TABLE_CELL_PADDING * 2;
        }

        this.titles.add(title);
        this.declaredWidths.add(Integer.valueOf(fixedWidth));
        this.naturalWidths.add(Integer.valueOf(natural));
    }

    /**
     * Column count of the table.
     */
    public int count()
    {
        return this.titles.size();
    }

    public int totalWidth()
    {
        return this.totalWidth;
    }

    /**
     * Locks the widths of every column; call once after the last column was added and after the rows are known.
     */
    public void compute()
    {
        int count = this.titles.size();
        int[] result = new int[count];
        int cap = Math.max(1, this.totalWidth / Math.max(1, count));
        int used = 0;
        List<Integer> flexible = new ArrayList<Integer>();

        for (int index = 0; index < count; index++)
        {
            int declared = this.declaredWidths.get(index).intValue();

            if (declared > 0)
            {
                result[index] = declared;
            }
            else
            {
                result[index] = Math.min(this.naturalWidths.get(index).intValue(), cap);
                flexible.add(Integer.valueOf(index));
            }

            used += result[index];
        }

        int remaining = this.totalWidth - used;

        while (remaining > 0 && !flexible.isEmpty())
        {
            int sumFree = 0;
            int freeCount = 0;

            for (Integer holder : flexible)
            {
                int index = holder.intValue();

                if (result[index] < cap)
                {
                    sumFree += result[index];
                    freeCount++;
                }
            }

            if (freeCount == 0)
            {
                break;
            }

            int added = 0;

            for (Integer holder : flexible)
            {
                int index = holder.intValue();

                if (result[index] >= cap)
                {
                    continue;
                }

                int share = (int) ((long) remaining * result[index] / Math.max(1, sumFree));
                int grow = Math.min(share, cap - result[index]);

                if (grow > 0)
                {
                    result[index] += grow;
                    added += grow;
                }
            }

            if (added == 0)
            {
                /* Rounding left a remainder that no proportional share could take, so it goes out one pixel a column. */
                for (Integer holder : flexible)
                {
                    if (remaining <= 0)
                    {
                        break;
                    }

                    int index = holder.intValue();

                    if (result[index] < cap)
                    {
                        result[index]++;
                        added++;
                        remaining--;
                    }
                }

                if (added == 0)
                {
                    break;
                }
            }

            remaining -= added;
        }

        this.widths.clear();

        for (int index = 0; index < count; index++)
        {
            this.widths.add(Integer.valueOf(result[index]));
        }
    }

    public int width(int index)
    {
        return this.widths.get(index).intValue();
    }

    /**
     * Left edge of a column, relative to the left edge of the table.
     */
    public int x(int index)
    {
        int offset = 0;

        for (int position = 0; position < index; position++)
        {
            offset += this.widths.get(position).intValue();
        }

        return offset;
    }

    public Component title(int index)
    {
        return this.titles.get(index);
    }

    /**
     * Width a cell text may use, after the padding of both sides.
     */
    public int textWidth(int index)
    {
        return Math.max(1, this.widths.get(index).intValue() - ModConstants.TABLE_CELL_PADDING * 2);
    }

    /**
     * Text shortened to its column, with an ellipsis when something was cut.
     */
    public String clip(int index, String text)
    {
        String value = text == null ? "" : text;
        int limit = this.textWidth(index);

        if (this.font.width(value) <= limit)
        {
            return value;
        }

        return this.font.plainSubstrByWidth(value, Math.max(1, limit - this.font.width("..."))) + "...";
    }

    public boolean isClipped(int index, String text)
    {
        return this.font.width(text == null ? "" : text) > this.textWidth(index);
    }

    /**
     * Left edge of a centred cell text, relative to the left edge of the table.
     */
    public int centeredX(int index, String text)
    {
        String value = text == null ? "" : text;
        int available = this.textWidth(index);
        int width = Math.min(this.font.width(value), available);
        return this.x(index) + ModConstants.TABLE_CELL_PADDING + (available - width) / 2;
    }

    public void drawCell(GuiGraphics graphics, int index, String text, int tableX, int y, int color)
    {
        String clipped = this.clip(index, text);
        graphics.drawString(this.font, clipped, tableX + this.x(index) + ModConstants.TABLE_CELL_PADDING
                + (this.textWidth(index) - this.font.width(clipped)) / 2, y, color);
    }

    public void drawHeader(GuiGraphics graphics, int tableX, int y, int color)
    {
        for (int index = 0; index < this.titles.size(); index++)
        {
            String text = this.clip(index, this.titles.get(index).getString());
            graphics.drawString(this.font, text, tableX + this.x(index) + ModConstants.TABLE_CELL_PADDING
                    + (this.textWidth(index) - this.font.width(text)) / 2, y, color);
        }
    }
}
