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
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * The luck line in the top right corner. The colour bands follow the value: low values are warm warnings and a high
 * value turns green. The line moves below the potion icons so it never covers them.
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
    /* Vanilla draws beneficial effects in one row and harmful effects in the next, both 26 pixels apart. */
    private static final int EFFECT_ROW_HEIGHT = 26;

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics, float partialTick,
            int screenWidth, int screenHeight)
    {
        Minecraft minecraft = gui.getMinecraft();

        if (minecraft.player == null || minecraft.options.hideGui)
        {
            return;
        }

        int value = ClientLuck.value();
        Component text = Component.translatable(KEY, Integer.valueOf(value));
        int x = screenWidth - 4 - minecraft.font.width(text);
        int y = 4 + effectOffset(minecraft.player);
        graphics.drawString(minecraft.font, text, x, y, colorOf(value), true);
    }

    private static int effectOffset(Player player)
    {
        boolean beneficial = false;
        boolean harmful = false;

        for (net.minecraft.world.effect.MobEffectInstance effect : player.getActiveEffects())
        {
            if (effect.getEffect().isBeneficial())
            {
                beneficial = true;
            }
            else
            {
                harmful = true;
            }
        }

        return ((beneficial ? 1 : 0) + (harmful ? 1 : 0)) * EFFECT_ROW_HEIGHT;
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
