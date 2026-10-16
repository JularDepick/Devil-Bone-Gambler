/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * Slider over whole numbers, the step is one.
 * The wrapped slider works on a fraction, this control snaps that fraction back to the whole number it stands for, so a
 * numeric offering is always a whole number. The range is meant to be refreshed while the screen is open, because the
 * health of a player can be raised or lowered by another mod and the upper limit has to follow the real current value.
 */
public class IntSlider extends AbstractSliderButton
{
    private final Component label;
    private int minimum;
    private int maximum;

    public IntSlider(int x, int y, int width, int height, Component label, int minimum, int maximum, int value)
    {
        super(x, y, width, height, label, fraction(minimum, maximum, value));
        this.label = label;
        this.minimum = minimum;
        this.maximum = Math.max(minimum, maximum);
        refreshMessage();
    }

    /**
     * The whole number the slider currently stands for.
     */
    public int value()
    {
        return this.minimum + (int) Math.round(this.value * (double) (this.maximum - this.minimum));
    }

    public int maximum()
    {
        return this.maximum;
    }

    /**
     * Moves the slider to a whole number, clamped into the current range. Used by the batch offering buttons.
     */
    public void setValue(int value)
    {
        this.value = fraction(this.minimum, this.maximum, value);
        refreshMessage();
    }

    /**
     * Follows a limit that changes while the screen is open, keeping the current number and clamping it into the new
     * range.
     */
    public void setRange(int minimum, int maximum)
    {
        int current = value();
        this.minimum = minimum;
        this.maximum = Math.max(minimum, maximum);
        this.value = fraction(this.minimum, this.maximum, current);
        refreshMessage();
    }

    /**
     * Dragging writes a fraction, so the fraction is snapped back to the whole number it stands for.
     */
    @Override
    protected void applyValue()
    {
        this.value = fraction(this.minimum, this.maximum, value());
    }

    @Override
    protected void updateMessage()
    {
        refreshMessage();
    }

    private void refreshMessage()
    {
        setMessage(this.label.copy().append(Component.literal(" " + value())));
    }

    /**
     * Fraction of a whole number inside a range. An empty range sits at its lower bound.
     */
    private static double fraction(int minimum, int maximum, int value)
    {
        int span = Math.max(0, maximum - minimum);
        int clamped = Math.max(minimum, Math.min(value, maximum));
        return span <= 0 ? 0.0D : (double) (clamped - minimum) / (double) span;
    }
}
