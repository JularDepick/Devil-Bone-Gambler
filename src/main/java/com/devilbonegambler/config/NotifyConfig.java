/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.util.ModConstants;

/**
 * Result display settings of a single prize entry.
 */
public class NotifyConfig
{
    public String mode;
    public String text;

    public NotifyConfig()
    {
    }

    public NotifyConfig(String mode, String text)
    {
        this.mode = mode;
        this.text = text;
    }

    public String modeOrDefault()
    {
        if (this.mode == null || this.mode.isEmpty())
        {
            return ModConstants.NOTIFY_MODE_ACTIONBAR;
        }

        for (String candidate : ModConstants.NOTIFY_MODES)
        {
            if (candidate.equals(this.mode))
            {
                return candidate;
            }
        }

        return ModConstants.NOTIFY_MODE_ACTIONBAR;
    }

    public String textOrEmpty()
    {
        return this.text == null ? "" : this.text;
    }

    public NotifyConfig copy()
    {
        return new NotifyConfig(this.mode, this.text);
    }
}
