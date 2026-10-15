/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.util.ModConstants;

/**
 * A single prize entry: the smallest unit that can be drawn inside a region.
 * Optional fields are boxed so that a missing JSON field stays distinguishable from an explicit false or zero.
 */
public class PrizeEntry
{
    public String id;
    public Boolean enabled;
    public Double weight;
    public String color;
    public Boolean majorEvent;
    public String command;
    public NotifyConfig notify;

    public PrizeEntry()
    {
    }

    public PrizeEntry(String id, boolean enabled, double weight, String color, boolean majorEvent, String command,
            String notifyMode, String notifyText)
    {
        this.id = id;
        this.enabled = Boolean.valueOf(enabled);
        this.weight = Double.valueOf(weight);
        this.color = color;
        this.majorEvent = Boolean.valueOf(majorEvent);
        this.command = command;
        this.notify = new NotifyConfig(notifyMode, notifyText);
    }

    public boolean isEnabled()
    {
        return this.enabled == null || this.enabled.booleanValue();
    }

    public double weightOrZero()
    {
        return this.weight == null ? 0.0D : this.weight.doubleValue();
    }

    public boolean isMajorEvent()
    {
        return this.majorEvent != null && this.majorEvent.booleanValue();
    }

    public boolean isRed()
    {
        return !ModConstants.ENTRY_COLOR_BLACK.equals(colorOrDefault());
    }

    public String colorOrDefault()
    {
        if (this.color == null || this.color.isEmpty())
        {
            return ModConstants.ENTRY_COLOR_RED;
        }

        for (String candidate : ModConstants.ENTRY_COLORS)
        {
            if (candidate.equals(this.color))
            {
                return candidate;
            }
        }

        return ModConstants.ENTRY_COLOR_RED;
    }

    public String commandOrEmpty()
    {
        return this.command == null ? "" : this.command;
    }

    public NotifyConfig notifyOrDefault()
    {
        return this.notify == null ? new NotifyConfig() : this.notify;
    }

    public PrizeEntry copy()
    {
        PrizeEntry result = new PrizeEntry();
        result.id = this.id;
        result.enabled = this.enabled;
        result.weight = this.weight;
        result.color = this.color;
        result.majorEvent = this.majorEvent;
        result.command = this.command;
        result.notify = this.notify == null ? null : this.notify.copy();
        return result;
    }
}
