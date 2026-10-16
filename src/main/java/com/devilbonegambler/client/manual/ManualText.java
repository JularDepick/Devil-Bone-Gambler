/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.manual;

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
}
