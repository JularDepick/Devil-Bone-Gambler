/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.draw;

import com.devilbonegambler.util.ModConstants;
import net.minecraft.util.RandomSource;

/**
 * One dice roll: a point from 1 to 6 plus an independent red or black colour.
 * The point is drawn uniformly first, then biased by luck and by the altar value.
 */
public final class DiceRoll
{
    private final int point;
    private final boolean red;

    private DiceRoll(int point, boolean red)
    {
        this.point = point;
        this.red = red;
    }

    public static DiceRoll of(int point, boolean red)
    {
        return new DiceRoll(clampPoint(point), red);
    }

    /**
     * Rolls the point and the colour. The colour is an independent event and never affects the point.
     */
    public static DiceRoll roll(RandomSource random, int luck, long altarValue)
    {
        int point = random.nextInt(ModConstants.MAX_DICE_POINT) + ModConstants.MIN_DICE_POINT;
        double bias = biasChance(luck, altarValue);

        if (bias > 0.0D && random.nextDouble() < bias)
        {
            point = clampPoint(point + 1);
        }
        else if (bias < 0.0D && random.nextDouble() < -bias)
        {
            point = clampPoint(point - 1);
        }

        return new DiceRoll(point, random.nextDouble() < redChance(luck));
    }

    /**
     * Probability of a red roll: the luck value over one hundred, clamped to the unit interval.
     */
    public static double redChance(int luck)
    {
        return clamp(luck / 100.0D, 0.0D, 1.0D);
    }

    /**
     * Signed chance of shifting the point by one step. Luck dominates, the altar value contributes a share.
     */
    public static double biasChance(int luck, long altarValue)
    {
        double luckTerm;

        if (luck >= ModConstants.BASE_LUCK)
        {
            luckTerm = (luck - ModConstants.BASE_LUCK)
                    / (double) Math.max(1, ModConstants.MAX_LUCK - ModConstants.BASE_LUCK);
        }
        else
        {
            luckTerm = (luck - ModConstants.BASE_LUCK)
                    / (double) Math.max(1, ModConstants.BASE_LUCK - ModConstants.MIN_LUCK);
        }

        double altarTerm = Math.max(0L, altarValue) / (double) Math.max(1L, ModConstants.ALTAR_MAX_VALUE);
        double combined = luckTerm * ModConstants.LUCK_BIAS_WEIGHT + altarTerm * ModConstants.ALTAR_BIAS_WEIGHT;
        return clamp(combined, -ModConstants.MAX_POINT_BIAS_CHANCE, ModConstants.MAX_POINT_BIAS_CHANCE);
    }

    public int point()
    {
        return this.point;
    }

    public boolean isRed()
    {
        return this.red;
    }

    public String colorId()
    {
        return this.red ? ModConstants.ENTRY_COLOR_RED : ModConstants.ENTRY_COLOR_BLACK;
    }

    private static int clampPoint(int point)
    {
        return Math.max(ModConstants.MIN_DICE_POINT, Math.min(ModConstants.MAX_DICE_POINT, point));
    }

    private static double clamp(double value, double min, double max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
