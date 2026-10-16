/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.commands;

import com.devilbonegambler.config.RandomPoolConfig;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.util.CommandPlaceholders;
import com.devilbonegambler.util.ModConstants;
import java.util.List;
import java.util.regex.Matcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

/**
 * Sample replacement of the random prize tokens, the single place where a random id is picked.
 * The sample space lives in the server config, which is refreshed from the registries on every world load.
 */
public final class RandomPoolSampler
{
    private RandomPoolSampler()
    {
    }

    /**
     * Replaces every random token of the line with one sampled id of that pool.
     * Returns null when the token names an unknown pool or the pool is empty, so the caller can skip the line
     * instead of running a command with a broken argument.
     */
    public static String sample(String line, MinecraftServer server, RandomSource random)
    {
        if (line == null || line.indexOf(ModConstants.RANDOM_TOKEN_OPEN) < 0)
        {
            return line;
        }

        RandomPoolConfig pools = server == null ? null : ServerConfigManager.randomPools();

        if (pools == null)
        {
            return null;
        }

        Matcher matcher = CommandPlaceholders.RANDOM.matcher(line);
        StringBuilder builder = new StringBuilder();
        int cursor = 0;

        while (matcher.find())
        {
            List<String> entries = pools.poolOf(matcher.group(1));

            if (entries.isEmpty())
            {
                return null;
            }

            builder.append(line, cursor, matcher.start());
            builder.append(entries.get(random.nextInt(entries.size())));
            cursor = matcher.end();
        }

        builder.append(line, cursor, line.length());
        return builder.toString();
    }
}
