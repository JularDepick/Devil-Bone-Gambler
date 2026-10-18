/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.util.ModConstants;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Hooks the mod into the config button of the mod list, so the button is one more way to reach the same admin panel
 * as the in game key. The factory runs on the client only; it sends the normal panel request and lets the server answer
 * with the panel, so the permission rule stays on the server for both entry points.
 */
public final class ModConfigScreen
{
    private ModConfigScreen()
    {
    }

    public static void register()
    {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> {
                    ClientActions.openAdminPanelFromMenu();
                    return null;
                }));
    }
}
