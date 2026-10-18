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
 * A column starts at the width of its longest content, header included, so a narrow column such as the tier number
 * stays narrow instead of taking a share of the table. Only the columns that declare a growth weight take the
 * leftover width, in proportion to that weight, so the widest text column absorbs the free space while the short
 * columns stay at their measured width. When the content does not fit, the flexible columns are shrunk towards their
 * share and the widest one gives way first.
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
    private final List<Integer> growWeights = new ArrayList<Integer>();
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
        addColumn(title, fixedWidth, cells, 0);
    }

    /**
     * Adds one column with an explicit growth weight. A positive weight lets the column take part of the leftover
     * width; a weight of zero keeps the column at its measured width, which is what a narrow column like the tier
     * number wants.
     */
    public void addColumn(Component title, int fixedWidth, List<String> cells, int growWeight)
    {
        int natural = fixedWidth;

        if (fixedWidth <= 0)
        {
            int widest = this.font.width(title == null ? "" : title.getString());

            for (String cell : cells)
            {
                widest = Math.max(widest, this.font.width(cell == null ? "" : cell));
            }

            natural = widest + ModConstants.TABLE_CELL_PADDING * 2;
        }

        this.titles.add(title);
        this.declaredWidths.add(Integer.valueOf(fixedWidth));
        this.naturalWidths.add(Integer.valueOf(natural));
        this.growWeights.add(Integer.valueOf(Math.max(0, growWeight)));
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
        int fixedSum = 0;
        int flexibleSum = 0;
        List<Integer> flexible = new ArrayList<Integer>();
        int growSum = 0;

        for (int index = 0; index < count; index++)
        {
            int declared = this.declaredWidths.get(index).intValue();

            if (declared > 0)
            {
                result[index] = declared;
                fixedSum += declared;
            }
            else
            {
                result[index] = Math.max(1, this.naturalWidths.get(index).intValue());
                flexibleSum += result[index];
                flexible.add(Integer.valueOf(index));
                growSum += this.growWeights.get(index).intValue();
            }
        }

        int available = Math.max(1, this.totalWidth - fixedSum);

        if (flexibleSum > available)
        {
            shrinkToFit(result, flexible, available);
            flexibleSum = 0;

            for (Integer holder : flexible)
            {
                flexibleSum += result[holder.intValue()];
            }
        }

        int remaining = available - flexibleSum;

        if (remaining > 0 && !flexible.isEmpty())
        {
            List<Integer> growers = new ArrayList<Integer>();

            for (Integer holder : flexible)
            {
                if (this.growWeights.get(holder.intValue()).intValue() > 0)
                {
                    growers.add(holder);
                }
            }

            /* When no column asks for growth, the free width goes out evenly so the table still fills its row. */
            if (growSum <= 0)
            {
                growers.clear();
                growers.addAll(flexible);
            }

            int handed = 0;

            for (Integer holder : growers)
            {
                int index = holder.intValue();
                int weight = growSum <= 0 ? 1 : this.growWeights.get(index).intValue();
                int share = (int) ((long) remaining * weight / Math.max(1, growSum <= 0 ? growers.size() : growSum));
                result[index] += share;
                handed += share;
            }

            if (handed < remaining)
            {
                result[growers.get(growers.size() - 1).intValue()] += remaining - handed;
            }
        }

        this.widths.clear();

        for (int index = 0; index < count; index++)
        {
            this.widths.add(Integer.valueOf(result[index]));
        }
    }

    /**
     * Shrinks the flexible columns until their sum fits the available width. The widest column gives way first, so a
     * long entry name is clipped while the short columns keep their readable width.
     */
    private void shrinkToFit(int[] result, List<Integer> flexible, int available)
    {
        int sum = 0;

        for (Integer holder : flexible)
        {
            sum += result[holder.intValue()];
        }

        while (sum > available)
        {
            int widest = -1;
            int widestIndex = -1;

            for (Integer holder : flexible)
            {
                int index = holder.intValue();

                if (result[index] > 1 && (widest < 0 || result[index] > widest) && result[index] > 0)
                {
                    widest = result[index];
                    widestIndex = index;
                }
            }

            if (widestIndex < 0)
            {
                break;
            }

            result[widestIndex]--;
            sum--;
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
