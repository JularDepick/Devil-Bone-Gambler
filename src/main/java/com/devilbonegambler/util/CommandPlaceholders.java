/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Expands the command placeholders of one prize entry command line.
 * Supported tokens: the triggering player name, a named player position and the altar anchor of a named player.
 * A position token accepts an optional trailing random modifier that spreads the position over a bounded range.
 * The legacy brace tokens stay supported so that older configuration files keep working.
 */
public final class CommandPlaceholders
{
    /**
     * Resolves the altar anchor of a referenced player. Only the server can answer it, so a client side caller
     * passes a source that returns null.
     */
    public interface AnchorSource
    {
        BlockPos altarOf(ServerPlayer player);
    }

    private static final Pattern POSITION = Pattern.compile(
            "([@#])([\\[(])([A-Za-z0-9_]*)([\\])])(?:\\[random=(\\d{1,3})&(\\d{1,3})\\])?");
    private static final Pattern DOLLAR = Pattern.compile("\\$[A-Za-z_][A-Za-z0-9_]*");
    private static final String SAMPLE_NAME = "DevilBoneGambler";

    private CommandPlaceholders()
    {
    }

    /**
     * Expands every placeholder of the line. Returns null when a referenced player or altar cannot be resolved.
     */
    public static String expand(String line, ServerPlayer trigger, AnchorSource anchors, RandomSource random)
    {
        if (line == null || line.isEmpty())
        {
            return line == null ? "" : line;
        }

        String result = line.replace(ModConstants.PLACEHOLDER_PLAYER_CAPITAL, trigger.getGameProfile().getName());
        Matcher matcher = POSITION.matcher(result);
        StringBuilder builder = new StringBuilder();
        int cursor = 0;

        while (matcher.find())
        {
            String resolved = resolvePosition(matcher, trigger, anchors, random);

            if (resolved == null)
            {
                return null;
            }

            builder.append(result, cursor, matcher.start());
            builder.append(resolved);
            cursor = matcher.end();
        }

        builder.append(result, cursor, result.length());
        return legacy(builder.toString(), trigger);
    }

    /**
     * Returns a description of the first malformed placeholder of the line, or null when the line is well formed.
     */
    public static String findProblem(String line)
    {
        if (line == null || line.isEmpty())
        {
            return null;
        }

        String withoutPositions = POSITION.matcher(line).replaceAll("");

        if (withoutPositions.contains("@(") || withoutPositions.contains("@[")
                || withoutPositions.contains("#(") || withoutPositions.contains("#["))
        {
            return "malformed position token";
        }

        Matcher matcher = POSITION.matcher(line);

        while (matcher.find())
        {
            String function = matcher.group(1);
            String open = matcher.group(2);
            String close = matcher.group(4);
            String delimiterProblem = delimiterProblem(function, open, close);

            if (delimiterProblem != null)
            {
                return delimiterProblem;
            }

            if (matcher.group(5) != null)
            {
                int mode = Integer.parseInt(matcher.group(5));
                int range = Integer.parseInt(matcher.group(6));

                if (mode > ModConstants.COMMAND_RANDOM_MAX_MODE)
                {
                    return "random mode out of range";
                }

                if (range > ModConstants.COMMAND_RANDOM_MAX_RANGE)
                {
                    return "random range out of range";
                }
            }
        }

        Matcher dollar = DOLLAR.matcher(line.replace(ModConstants.PLACEHOLDER_PLAYER_CAPITAL, ""));

        if (dollar.find())
        {
            return "unknown dollar token: " + dollar.group();
        }

        return null;
    }

    /**
     * Replaces every placeholder with a sample value so that the result can be parsed for syntax.
     */
    public static String sampleForm(String line)
    {
        if (line == null || line.isEmpty())
        {
            return line == null ? "" : line;
        }

        String result = line.replace(ModConstants.PLACEHOLDER_PLAYER_CAPITAL, SAMPLE_NAME);
        result = POSITION.matcher(result).replaceAll("0 64 0");
        result = result.replace(ModConstants.PLACEHOLDER_PLAYER, SAMPLE_NAME);
        result = result.replace(ModConstants.PLACEHOLDER_X, "0");
        result = result.replace(ModConstants.PLACEHOLDER_Y, "64");
        result = result.replace(ModConstants.PLACEHOLDER_Z, "0");
        return result;
    }

    private static String resolvePosition(Matcher matcher, ServerPlayer trigger, AnchorSource anchors,
            RandomSource random)
    {
        String name = matcher.group(3);
        String function = matcher.group(1);

        if (delimiterProblem(function, matcher.group(2), matcher.group(4)) != null)
        {
            return null;
        }

        ServerPlayer target = trigger;

        if (name != null && !name.isEmpty() && !name.equals(trigger.getGameProfile().getName()))
        {
            MinecraftServer server = trigger.getServer();

            if (server == null)
            {
                return null;
            }

            target = server.getPlayerList().getPlayerByName(name);

            if (target == null)
            {
                return null;
            }
        }

        boolean altar = "#".equals(matcher.group(1));
        double x;
        double y;
        double z;

        if (altar)
        {
            BlockPos anchor = anchors == null ? null : anchors.altarOf(target);

            if (anchor == null)
            {
                return null;
            }

            x = anchor.getX();
            y = anchor.getY();
            z = anchor.getZ();
        }
        else
        {
            x = target.getX();
            y = target.getY();
            z = target.getZ();
        }

        int mode = matcher.group(5) == null
                ? ModConstants.COMMAND_RANDOM_DEFAULT_MODE
                : Integer.parseInt(matcher.group(5));
        int range = matcher.group(6) == null
                ? ModConstants.COMMAND_RANDOM_DEFAULT_RANGE
                : Integer.parseInt(matcher.group(6));
        return spread(x, y, z, mode, Math.min(range, ModConstants.COMMAND_RANDOM_MAX_RANGE), random);
    }

    /**
     * Spreads a base position inside the range that the mode selects: 0 keeps the base position, 1 spreads on the
     * Z axis, 2 spreads over the horizontal plane and 3 spreads over the whole sphere of the given radius.
     */
    private static String spread(double baseX, double baseY, double baseZ, int mode, int range, RandomSource random)
    {
        if (mode <= 0 || range <= 0 || random == null)
        {
            return format(baseX, baseY, baseZ);
        }

        if (mode == 1)
        {
            return format(baseX, baseY, baseZ + signed(random, range));
        }

        if (mode == 2)
        {
            double angle = random.nextDouble() * (Math.PI * 2.0D);
            double radius = Math.sqrt(random.nextDouble()) * range;
            return format(baseX + Math.cos(angle) * radius, baseY, baseZ + Math.sin(angle) * radius);
        }

        double angle = random.nextDouble() * (Math.PI * 2.0D);
        double vertical = random.nextDouble() * 2.0D - 1.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
        double radius = Math.cbrt(random.nextDouble()) * range;
        return format(baseX + Math.cos(angle) * horizontal * radius,
                baseY + vertical * radius,
                baseZ + Math.sin(angle) * horizontal * radius);
    }

    /**
     * Each function owns its argument delimiter: the position function takes round brackets and the altar
     * function takes square brackets, so the two forms never mix.
     */
    private static String delimiterProblem(String function, String open, String close)
    {
        if ("@".equals(function) && !("(".equals(open) && ")".equals(close)))
        {
            return "the position function takes round brackets";
        }

        if ("#".equals(function) && !("[".equals(open) && "]".equals(close)))
        {
            return "the altar function takes square brackets";
        }

        return null;
    }

    private static double signed(RandomSource random, int range)
    {
        return (random.nextDouble() * 2.0D - 1.0D) * range;
    }

    private static String format(double x, double y, double z)
    {
        return Mth.floor(x) + " " + Mth.floor(y) + " " + Mth.floor(z);
    }

    private static String legacy(String line, ServerPlayer trigger)
    {
        if (line.indexOf('$') < 0)
        {
            return line;
        }

        String result = line.replace(ModConstants.PLACEHOLDER_PLAYER, trigger.getGameProfile().getName());
        result = result.replace(ModConstants.PLACEHOLDER_X, Integer.toString(Mth.floor(trigger.getX())));
        result = result.replace(ModConstants.PLACEHOLDER_Y, Integer.toString(Mth.floor(trigger.getY())));
        result = result.replace(ModConstants.PLACEHOLDER_Z, Integer.toString(Mth.floor(trigger.getZ())));
        return result;
    }
}
