/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.commands;

import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.SampleContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The %(n,m) placeholder of a prize command: one integer drawn from the closed range n to m.
 *
 * The draw is not uniform when the command comes from a dice roll. A target inside the range follows the point, the
 * overflow and the luck of the roll, red pushes it towards the upper end and black towards the lower end, and a normal
 * distribution around that target picks the value. A chat trial without a roll stays uniform.
 */
public final class NumberPlaceholders
{
    private static final Pattern TOKEN = Pattern.compile("%\\((-?\\d{1,7}),(-?\\d{1,7})\\)");
    private static final long SEED_STEP = 0x9E3779B97F4A7C15L;

    private NumberPlaceholders()
    {
    }

    public static String expand(String line, SampleContext context)
    {
        if (line == null || line.indexOf("%(") < 0)
        {
            return line;
        }

        Matcher matcher = TOKEN.matcher(line);
        StringBuilder builder = new StringBuilder();
        int cursor = 0;
        int index = 0;

        while (matcher.find())
        {
            int low = Integer.parseInt(matcher.group(1));
            int high = Integer.parseInt(matcher.group(2));

            builder.append(line, cursor, matcher.start());

            if (low >= high)
            {
                builder.append(matcher.group());
            }
            else
            {
                builder.append(pick(low, high, context, index++));
            }

            cursor = matcher.end();
        }

        builder.append(line, cursor, line.length());
        return builder.toString();
    }

    /**
     * Returns a description of the first malformed range, or null when every range is usable.
     */
    public static String findProblem(String line)
    {
        if (line == null || line.indexOf("%(") < 0)
        {
            return null;
        }

        String rest = TOKEN.matcher(line).replaceAll("");

        if (rest.contains("%("))
        {
            return "malformed number placeholder";
        }

        Matcher matcher = TOKEN.matcher(line);

        while (matcher.find())
        {
            int low = Integer.parseInt(matcher.group(1));
            int high = Integer.parseInt(matcher.group(2));

            if (low >= high)
            {
                return "number placeholder range is empty: " + matcher.group();
            }

            if (Math.abs((long) low) > ModConstants.MAX_NUMBER_PLACEHOLDER
                    || Math.abs((long) high) > ModConstants.MAX_NUMBER_PLACEHOLDER)
            {
                return "number placeholder out of bounds: " + matcher.group();
            }
        }

        return null;
    }

    /**
     * Replaces every range with its midpoint so that the vanilla parser can read the line during validation.
     */
    public static String sampleForm(String line)
    {
        if (line == null || line.indexOf("%(") < 0)
        {
            return line;
        }

        Matcher matcher = TOKEN.matcher(line);
        StringBuilder builder = new StringBuilder();
        int cursor = 0;

        while (matcher.find())
        {
            int low = Integer.parseInt(matcher.group(1));
            int high = Integer.parseInt(matcher.group(2));
            builder.append(line, cursor, matcher.start());
            builder.append(low >= high ? low : (low + high) / 2);
            cursor = matcher.end();
        }

        builder.append(line, cursor, line.length());
        return builder.toString();
    }

    private static int pick(int low, int high, SampleContext context, int index)
    {
        RandomSource random = context == null || context.seed() == 0L
                ? RandomSource.create()
                : RandomSource.create(context.seed() + index * SEED_STEP);

        if (context == null || !context.fromDraw())
        {
            return low + random.nextInt(high - low + 1);
        }

        double target = low + (high - low) * targetFraction(context);
        double spread = Math.max(1.0D, (high - low) / 5.0D);
        int value = (int) Math.round(target + random.nextGaussian() * spread);
        return Mth.clamp(value, low, high);
    }

    private static double targetFraction(SampleContext context)
    {
        double span = Math.max(1, ModConstants.MAX_DICE_POINT - ModConstants.MIN_DICE_POINT);
        double point = (context.point() - ModConstants.MIN_DICE_POINT) / span;
        double luck = (context.luck() - ModConstants.BASE_LUCK)
                / (double) Math.max(1, ModConstants.MAX_LUCK - ModConstants.BASE_LUCK);
        double overflow = Math.min(1.0D, context.overflow() / span);
        double aim = 0.5D * point + 0.3D * luck + 0.2D * overflow;

        aim += context.red() ? 0.1D : -0.1D;
        return Mth.clamp(aim, 0.0D, 1.0D);
    }
}
