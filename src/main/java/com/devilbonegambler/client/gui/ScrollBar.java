/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.util.ModConstants;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Scroll bar of a list table, drawn as a track with a proportional thumb.
 * The thumb can be dragged, the track jumps by one page, and the wheel stays with the owning screen.
 */
public final class ScrollBar
{
    private static final int TRACK_COLOR = 0x40000000;
    private static final int THUMB_COLOR = 0x80FFFFFF;
    private static final int THUMB_ACTIVE_COLOR = 0xC0FFFFFF;

    private int x;
    private int y;
    private int height;
    private int value;
    private int maximum;
    private int visible;
    private boolean dragging;
    private int grabOffset;

    public void setBounds(int x, int y, int height)
    {
        this.x = x;
        this.y = y;
        this.height = Math.max(1, height);
    }

    /**
     * Value is the first visible row, maximum the highest value it may reach, visible how many rows fit at once.
     */
    public void setRange(int maximum, int visible)
    {
        this.maximum = Math.max(0, maximum);
        this.visible = Math.max(1, visible);
        this.value = Math.max(0, Math.min(this.value, this.maximum));
    }

    public int value()
    {
        return this.value;
    }

    public void setValue(int value)
    {
        this.value = Math.max(0, Math.min(value, this.maximum));
    }

    public void scrollBy(int delta)
    {
        this.setValue(this.value + delta);
    }

    public boolean isActive()
    {
        return this.maximum > 0;
    }

    private int thumbHeight()
    {
        int total = this.maximum + this.visible;
        return Math.max(ModConstants.TABLE_SCROLL_MIN_THUMB, this.height * this.visible / Math.max(1, total));
    }

    private int thumbTop()
    {
        int travel = this.height - this.thumbHeight();
        return this.y + (this.maximum <= 0 ? 0 : travel * this.value / this.maximum);
    }

    private boolean inside(double mouseX, double mouseY)
    {
        return mouseX >= this.x - 1 && mouseX < this.x + ModConstants.TABLE_SCROLL_WIDTH + 1
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button != 0 || !this.isActive() || !this.inside(mouseX, mouseY))
        {
            return false;
        }

        int top = this.thumbTop();

        if (mouseY >= top && mouseY < top + this.thumbHeight())
        {
            this.dragging = true;
            this.grabOffset = (int) mouseY - top;
        }
        else
        {
            /* A click on the track moves by one page towards the click. */
            this.scrollBy(mouseY < top ? -this.visible : this.visible);
        }

        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button)
    {
        if (!this.dragging || !this.isActive())
        {
            return false;
        }

        int travel = this.height - this.thumbHeight();
        int offset = (int) mouseY - this.y - this.grabOffset;
        this.value = travel <= 0 ? 0 : Math.max(0, Math.min(this.maximum, offset * this.maximum / travel));
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (!this.dragging)
        {
            return false;
        }

        this.dragging = false;
        return true;
    }

    public void render(GuiGraphics graphics)
    {
        if (!this.isActive())
        {
            return;
        }

        graphics.fill(this.x, this.y, this.x + ModConstants.TABLE_SCROLL_WIDTH, this.y + this.height, TRACK_COLOR);
        int top = this.thumbTop();
        graphics.fill(this.x, top, this.x + ModConstants.TABLE_SCROLL_WIDTH, top + this.thumbHeight(),
                this.dragging ? THUMB_ACTIVE_COLOR : THUMB_COLOR);
    }
}
