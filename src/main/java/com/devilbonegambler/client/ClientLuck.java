/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.util.ModConstants;

/**
 * The latest luck value pushed by the server, used only by the corner display.
 */
public final class ClientLuck
{
    private static int value = ModConstants.BASE_LUCK;

    private ClientLuck()
    {
    }

    public static int value()
    {
        return value;
    }

    public static void set(int newValue)
    {
        value = newValue;
    }
}
