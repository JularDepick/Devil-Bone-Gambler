/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.util.ModConstants;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Chat hints of the client side, held back the same way as the server ones: the same hint appears at most once per
 * cooldown, so an action that keeps failing cannot flood the chat box. Results and rewards never pass through here.
 */
public final class ClientNotices
{
    private static final Map<String, Long> LAST = new HashMap<String, Long>();

    private ClientNotices()
    {
    }

    /**
     * Shows one translation key in the chat box unless the same key was already shown inside the cooldown.
     */
    public static void sendThrottled(String key, Object... args)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.gui == null)
        {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = LAST.get(key);

        if (last != null && now - last < ModConstants.NOTICE_COOLDOWN_MS)
        {
            return;
        }

        LAST.put(key, now);
        minecraft.gui.getChat().addMessage(Component.translatable(key, args));
    }
}
