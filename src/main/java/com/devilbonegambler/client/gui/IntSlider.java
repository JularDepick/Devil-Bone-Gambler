/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import java.util.function.IntConsumer;
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
    private final int step;
    private int minimum;
    private int maximum;
    private IntConsumer valueListener;
    /* True between a press on the track and the release, so a drag can be routed to this slider alone. */
    private boolean draggingValue;

    public IntSlider(int x, int y, int width, int height, Component label, int minimum, int maximum, int value)
    {
        this(x, y, width, height, label, minimum, maximum, value, 1);
    }

    /**
     * A slider over whole numbers on a grid of the given step. A wide range such as an offering value is only meant for
     * coarse dragging, so the grid keeps the number readable while the text field stays the precise way in.
     */
    public IntSlider(int x, int y, int width, int height, Component label, int minimum, int maximum, int value, int step)
    {
        super(x, y, width, height, label, fraction(minimum, maximum, snap(minimum, Math.max(minimum, maximum), value,
                Math.max(1, step))));
        this.label = label;
        this.minimum = minimum;
        this.maximum = Math.max(minimum, maximum);
        this.step = Math.max(1, step);
        refreshMessage();
    }

    /**
     * Notified with the stepped number after the player drags or clicks the control, never after a programmatic set.
     */
    public void setValueListener(IntConsumer listener)
    {
        this.valueListener = listener;
    }

    /**
     * The whole number the slider currently stands for.
     */
    public int value()
    {
        return snapDown(this.minimum + (int) Math.round(this.value * (double) (this.maximum - this.minimum)));
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
        this.value = fraction(this.minimum, this.maximum, this.snapNearest(value));
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
     * A container screen never forwards a drag to its widgets, so the screen asks this flag before it hands a drag over.
     */
    public boolean isDraggingValue()
    {
        return this.draggingValue;
    }

    /**
     * The release arrives from the screen rather than through the normal widget chain, which is why the flag is cleared
     * from the outside as well.
     */
    public void stopDraggingValue()
    {
        this.draggingValue = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        if (handled)
        {
            this.draggingValue = true;
        }

        return handled;
    }

    /**
     * Dragging writes a fraction, so the fraction is snapped back to the whole number it stands for.
     */
    @Override
    protected void applyValue()
    {
        this.value = fraction(this.minimum, this.maximum, value());

        if (this.valueListener != null)
        {
            this.valueListener.accept(value());
        }
    }

    /**
     * A release settles on the nearest step instead of the step below the pointer, so letting go of a coarse drag still
     * lands on a round number.
     */
    @Override
    public void onRelease(double mouseX, double mouseY)
    {
        this.value = fraction(this.minimum, this.maximum, this.snapNearest(value()));
        refreshMessage();
        super.onRelease(mouseX, mouseY);
    }

    private int snapDown(int raw)
    {
        int clamped = Math.max(this.minimum, Math.min(raw, this.maximum));
        int offset = clamped - this.minimum;
        return this.minimum + offset - offset % this.step;
    }

    private int snapNearest(int raw)
    {
        int clamped = Math.max(this.minimum, Math.min(raw, this.maximum));
        int offset = clamped - this.minimum;
        return Math.min(this.maximum, this.minimum + ((offset + this.step / 2) / this.step) * this.step);
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
    private static int snap(int minimum, int maximum, int value, int step)
    {
        int clamped = Math.max(minimum, Math.min(value, maximum));
        return minimum + (clamped - minimum) - (clamped - minimum) % step;
    }

    private static double fraction(int minimum, int maximum, int value)
    {
        int span = Math.max(0, maximum - minimum);
        int clamped = Math.max(minimum, Math.min(value, maximum));
        return span <= 0 ? 0.0D : (double) (clamped - minimum) / (double) span;
    }
}
