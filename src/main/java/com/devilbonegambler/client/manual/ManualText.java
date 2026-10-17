/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.manual;

import net.minecraft.client.gui.Font;

/**
 * One styled run of text inside a manual line.
 * Bold and code are the two inline styles the manual markdown supports.
 */
public record ManualText(String text, ManualText.Style style)
{
    public enum Style
    {
        BODY,
        BOLD,
        CODE
    }

    public static ManualText body(String text)
    {
        return new ManualText(text, Style.BODY);
    }

    /**
     * Advance width of this run as it is actually drawn: the bold style adds one pixel per character, which the plain
     * text width of the font does not include. Measuring bold runs with the body width made long bold lines run past
     * the page edge and get clipped.
     */
    public int width(Font font)
    {
        return font.width(this.text) + (this.style == Style.BOLD ? this.text.length() : 0);
    }
}
