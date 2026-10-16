/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.manual;

import java.util.ArrayList;
import java.util.List;

/**
 * One drawn row of a manual page: a wrapped part of a logical line.
 * The first row of a bullet or of a numbered item carries the marker, every following row is indented by the width of
 * that marker instead. A separator rule and a blank spacer are rows without text.
 */
public record ManualRow(ManualLine.Block block, String marker, int indent, List<ManualText> texts)
{
    public static ManualRow of(ManualLine.Block block, String marker, int indent, List<ManualText> texts)
    {
        return new ManualRow(block, marker == null ? "" : marker, Math.max(0, indent), texts);
    }

    public static ManualRow separator()
    {
        return new ManualRow(ManualLine.Block.SEPARATOR, "", 0, List.of());
    }

    public static ManualRow spacer()
    {
        return new ManualRow(ManualLine.Block.SPACER, "", 0, List.of());
    }

    public ManualRow withText(ManualText text)
    {
        List<ManualText> grown = new ArrayList<ManualText>(this.texts);
        grown.add(text);
        return new ManualRow(this.block, this.marker, this.indent, grown);
    }

    public boolean isSeparator()
    {
        return this.block == ManualLine.Block.SEPARATOR;
    }

    public boolean isSpacer()
    {
        return this.block == ManualLine.Block.SPACER;
    }

    public boolean isEmpty()
    {
        return this.texts.isEmpty() && this.marker.isEmpty();
    }
}
