/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import com.devilbonegambler.client.ClientLuck;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * The luck line in the top left corner. The colour bands follow the value: low values are warm warnings and a high
 * value turns green. Vanilla keeps the top left corner free, so the line never covers the potion icons, which sit
 * opposite it.
 */
public final class LuckOverlay implements IGuiOverlay
{
    private static final String KEY = "gui." + ModConstants.MOD_ID + ".hud.luck";
    private static final int RED = 0xFF5555;
    private static final int ORANGE = 0xFFAA00;
    private static final int YELLOW = 0xFFFF55;
    private static final int LIGHT_BLUE = 0x55CCFF;
    private static final int LIGHT_GREEN = 0x55FF99;
    private static final int GREEN = 0x55FF55;
    private static final int LEFT_MARGIN = 4;
    private static final int TOP_MARGIN = 4;

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics, float partialTick,
            int screenWidth, int screenHeight)
    {
        Minecraft minecraft = gui.getMinecraft();

        if (minecraft.player == null || minecraft.options.hideGui)
        {
            return;
        }

        /* Containers, chat and the pause menu all replace the game view, so the line stays out of every screen. */
        if (minecraft.screen != null)
        {
            return;
        }

        int value = ClientLuck.value();
        Component text = Component.translatable(KEY, Integer.valueOf(value));
        graphics.drawString(minecraft.font, text, LEFT_MARGIN, TOP_MARGIN, colorOf(value), true);
    }

    private static int colorOf(int value)
    {
        if (value <= 20)
        {
            return RED;
        }

        if (value <= 40)
        {
            return ORANGE;
        }

        if (value <= 60)
        {
            return YELLOW;
        }

        if (value <= 80)
        {
            return LIGHT_BLUE;
        }

        if (value < 90)
        {
            return LIGHT_GREEN;
        }

        return GREEN;
    }
}
