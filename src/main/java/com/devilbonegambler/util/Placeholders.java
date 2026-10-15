/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * Expands the documented placeholders of a command or a display text.
 * Unknown placeholders make the owning entry invalid during config validation.
 */
public final class Placeholders
{
    private static final Pattern PATTERN = Pattern.compile("\\$\\{[A-Za-z0-9_]+\\}");

    private Placeholders()
    {
    }

    public static String expand(String text, ServerPlayer player)
    {
        if (text == null || text.isEmpty())
        {
            return text == null ? "" : text;
        }

        String result = text;
        result = result.replace(ModConstants.PLACEHOLDER_PLAYER, player.getGameProfile().getName());
        result = result.replace(ModConstants.PLACEHOLDER_X, Integer.toString(Mth.floor(player.getX())));
        result = result.replace(ModConstants.PLACEHOLDER_Y, Integer.toString(Mth.floor(player.getY())));
        result = result.replace(ModConstants.PLACEHOLDER_Z, Integer.toString(Mth.floor(player.getZ())));
        return result;
    }

    /**
     * Returns the first unknown placeholder of the text, or null when every placeholder is known.
     */
    public static String findUnknown(String text)
    {
        if (text == null || text.isEmpty())
        {
            return null;
        }

        Matcher matcher = PATTERN.matcher(text);

        while (matcher.find())
        {
            String token = matcher.group();

            if (!ModConstants.PLACEHOLDER_PLAYER.equals(token)
                    && !ModConstants.PLACEHOLDER_X.equals(token)
                    && !ModConstants.PLACEHOLDER_Y.equals(token)
                    && !ModConstants.PLACEHOLDER_Z.equals(token))
            {
                return token;
            }
        }

        return null;
    }
}
