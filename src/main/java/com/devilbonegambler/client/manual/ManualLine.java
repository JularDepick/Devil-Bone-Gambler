/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.manual;

import java.util.List;

/**
 * One logical line of a manual, before it is wrapped to the width of the book page.
 * The block decides how the line is labelled and coloured: a heading, a bullet, a numbered item, a plain paragraph,
 * a separator rule or a bare spacer.
 */
public record ManualLine(ManualLine.Block block, String marker, List<ManualText> texts)
{
    public enum Block
    {
        PARAGRAPH,
        HEADING,
        SUBHEADING,
        MINOR,
        BULLET,
        NUMBER,
        SEPARATOR,
        SPACER
    }

    public static ManualLine of(Block block, String marker, List<ManualText> texts)
    {
        return new ManualLine(block, marker == null ? "" : marker, texts);
    }

    public static ManualLine separator()
    {
        return new ManualLine(Block.SEPARATOR, "", List.of());
    }

    public static ManualLine spacer()
    {
        return new ManualLine(Block.SPACER, "", List.of());
    }
}
