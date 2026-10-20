/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A button that asks for a second click before it runs its action.
 * The first click only relabels the button, and the armed state is released once the confirm window has passed or as
 * soon as the button leaves the screen, so a click made much later can never delete something the player has already
 * stopped thinking about.
 */
public class ConfirmButton extends Button
{
    public static final long CONFIRM_WINDOW_MS = 2000L;

    private final Component normalLabel;
    private final Component confirmLabel;
    private final Runnable action;
    private boolean armed;
    private long armedAt;

    public ConfirmButton(int x, int y, int width, int height, Component normalLabel, Component confirmLabel,
            Runnable action)
    {
        super(x, y, width, height, normalLabel, button -> { }, DEFAULT_NARRATION);
        this.normalLabel = normalLabel;
        this.confirmLabel = confirmLabel;
        this.action = action;
    }

    /** Returns the button to its plain action, which is also what happens when the selection it applies to changes. */
    public void release()
    {
        if (!this.armed)
        {
            return;
        }

        this.armed = false;
        setMessage(this.normalLabel);
    }

    /**
     * The confirm label is drawn in red, which the plain button renderer cannot express because it always paints its
     * message in white. While armed the button keeps an empty message, so the red line is the only text shown.
     */
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        if (this.armed)
        {
            graphics.drawCenteredString(Minecraft.getInstance().font, this.confirmLabel, getX() + this.width / 2,
                    getY() + (this.height - 8) / 2, 0xFF5555);
        }
    }

    @Override
    public void onPress()
    {
        if (!this.armed)
        {
            this.armed = true;
            this.armedAt = Util.getMillis();
            setMessage(Component.empty());
            return;
        }

        release();
        this.action.run();
    }

    /** Drops the armed state after the confirm window, and while the button is not on screen. */
    public void tickConfirm()
    {
        if (this.armed && (!this.visible || Util.getMillis() - this.armedAt > CONFIRM_WINDOW_MS))
        {
            release();
        }
    }
}
