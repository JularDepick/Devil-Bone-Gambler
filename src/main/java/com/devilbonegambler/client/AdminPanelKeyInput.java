/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.util.ModConstants;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Turns the admin panel shortcut into a panel request.
 * The client only asks, the server decides whether the sender is the creator or a granted admin, so the access list
 * never has to reach the client before the request.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AdminPanelKeyInput
{
    private AdminPanelKeyInput()
    {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event)
    {
        if (event.getAction() != GLFW.GLFW_PRESS)
        {
            return;
        }

        InputConstants.Key key = InputConstants.getKey(event.getKey(), event.getScanCode());

        /* The mapping carries the Shift modifier and the in game conflict context, so both are checked here. */
        if (!AdminPanelKey.ADMIN_PANEL.isActiveAndMatches(key))
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.screen != null)
        {
            return;
        }

        ClientActions.sendAdminPanelRequest();
    }
}
