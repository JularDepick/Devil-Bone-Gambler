/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The --R(n) suffix of a prize command line: the line is executed n times, n from 1 to 10.
 *
 * The suffix only counts at the very end of the line, trailing whitespace ignored, and is removed before any other
 * placeholder is expanded. A line without the suffix runs exactly once.
 */
public final class RepeatPlaceholders
{
    public static final int MIN_REPEAT = 1;
    public static final int MAX_REPEAT = 10;
    private static final Pattern TOKEN = Pattern.compile("--R\\(([^)]*)\\)");

    private RepeatPlaceholders()
    {
    }

    /**
     * The command line without a trailing repeat suffix.
     */
    public static String body(String line)
    {
        if (line == null)
        {
            return null;
        }

        Matcher matcher = TOKEN.matcher(line);
        int start = -1;

        while (matcher.find())
        {
            if (line.substring(matcher.end()).trim().isEmpty())
            {
                start = matcher.start();
            }
        }

        return start < 0 ? line : line.substring(0, start).trim();
    }

    /**
     * The repeat count of the line, or 1 when no suffix is present.
     */
    public static int count(String line)
    {
        if (line == null)
        {
            return 1;
        }

        Matcher matcher = TOKEN.matcher(line);
        int count = 1;

        while (matcher.find())
        {
            if (line.substring(matcher.end()).trim().isEmpty())
            {
                try
                {
                    count = Integer.parseInt(matcher.group(1));
                }
                catch (NumberFormatException exception)
                {
                    count = 1;
                }
            }
        }

        return count < MIN_REPEAT || count > MAX_REPEAT ? 1 : count;
    }

    /**
     * Returns a description of the first problem with a repeat suffix, or null when the line is usable.
     */
    public static String findProblem(String line)
    {
        if (line == null)
        {
            return null;
        }

        Matcher matcher = TOKEN.matcher(line);
        int matches = 0;
        int lastStart = -1;
        int lastEnd = -1;
        String argument = null;

        while (matcher.find())
        {
            matches++;
            lastStart = matcher.start();
            lastEnd = matcher.end();
            argument = matcher.group(1);
        }

        if (matches == 0)
        {
            return line.contains("--R") ? "malformed repeat placeholder" : null;
        }

        if (matches > 1)
        {
            return "only one repeat placeholder is allowed";
        }

        if (!line.substring(lastEnd).trim().isEmpty())
        {
            return "the repeat placeholder must be at the end of the line";
        }

        if (!argument.matches("\\d+"))
        {
            return "repeat count must be an integer";
        }

        int count;

        try
        {
            count = Integer.parseInt(argument);
        }
        catch (NumberFormatException exception)
        {
            return "repeat count out of range: " + argument;
        }

        if (count < MIN_REPEAT || count > MAX_REPEAT)
        {
            return "repeat count out of range: " + count;
        }

        if (line.substring(0, lastStart).trim().isEmpty())
        {
            return "repeat placeholder has no command";
        }

        return null;
    }
}
