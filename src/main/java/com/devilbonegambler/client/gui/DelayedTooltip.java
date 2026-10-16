/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.util.ModConstants;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Hover tooltip of the list tables, held back for a moment before it appears.
 * The timer starts when the pointer reaches a new row and is reset as soon as the pointer leaves, so a tooltip never
 * flashes while the pointer travels across the list.
 */
public final class DelayedTooltip
{
    private String current;
    private long since;

    /**
     * Draws the lines once the pointer has rested on the same row for the configured delay.
     */
    public void render(GuiGraphics graphics, Font font, String hoverKey, List<Component> lines, int mouseX, int mouseY)
    {
        if (hoverKey == null || lines == null || lines.isEmpty())
        {
            this.current = null;
            return;
        }

        long now = System.currentTimeMillis();

        if (!hoverKey.equals(this.current))
        {
            this.current = hoverKey;
            this.since = now;
            return;
        }

        if (now - this.since >= ModConstants.TOOLTIP_DELAY_MS)
        {
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    public void reset()
    {
        this.current = null;
    }
}
