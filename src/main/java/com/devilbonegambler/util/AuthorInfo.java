/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

/**
 * Single source of the author credit shown on every client screen.
 * Replace the values here to re-brand the client side without touching any screen code.
 */
public final class AuthorInfo
{
    public static final String AUTHOR_NAME = "JularDepick";
    public static final String AUTHOR_URL = "https://github.com/JularDepick";
    public static final String COPYRIGHT_YEAR = "2026";

    public static final String SCREEN_CREDIT_KEY = "gui.devil_bone_gambler.author";
    public static final String SOURCE_HEADER = "Devil Bone Gambler - Copyright (c) " + COPYRIGHT_YEAR + " " + AUTHOR_NAME;

    private AuthorInfo()
    {
    }
}
