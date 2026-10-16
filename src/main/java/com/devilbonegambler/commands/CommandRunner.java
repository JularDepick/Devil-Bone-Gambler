/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.commands;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.altar.AltarService;
import com.devilbonegambler.block.DevilsAltarBlockEntity;
import com.devilbonegambler.util.CommandPlaceholders;
import com.devilbonegambler.util.ModConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Runs the command lines of one prize entry: one vanilla command per line, executed in order, with an optional
 * leading slash. A line whose placeholders cannot be resolved is skipped with a warning instead of failing the
 * whole entry, and a runtime failure of one line never stops the remaining lines.
 */
public final class CommandRunner
{
    private CommandRunner()
    {
    }

    /**
     * Splits an entry command into its executable lines, dropping blank lines and leading slashes.
     */
    public static List<String> lines(String raw)
    {
        List<String> result = new ArrayList<String>();

        if (raw == null || raw.isEmpty())
        {
            return result;
        }

        for (String piece : raw.split("\n", -1))
        {
            String line = piece.trim();

            while (line.startsWith("/"))
            {
                line = line.substring(1).trim();
            }

            if (!line.isEmpty())
            {
                result.add(line);
            }
        }

        return result;
    }

    /**
     * Executes every line in order and returns how many commands were dispatched.
     */
    public static int run(MinecraftServer server, ServerPlayer trigger, String raw)
    {
        int executed = 0;

        for (String line : lines(raw))
        {
            String sampled = RandomPoolSampler.sample(line, server, trigger.getRandom());

            if (sampled == null)
            {
                DevilBoneGambler.LOGGER.warn(
                        "Skipped a prize command line, a random pool is unknown or empty, line={}", line);
                continue;
            }

            String command = CommandPlaceholders.expand(sampled, trigger, CommandRunner::altarOf, trigger.getRandom());

            if (command == null)
            {
                DevilBoneGambler.LOGGER.warn(
                        "Skipped a prize command line, a referenced player or altar is unavailable, line={}", line);
                continue;
            }

            if (command.isEmpty())
            {
                continue;
            }

            try
            {
                CommandSourceStack source = trigger.createCommandSourceStack()
                        .withPermission(ModConstants.DRAW_COMMAND_PERMISSION_LEVEL);
                server.getCommands().performPrefixedCommand(source, command);
                executed++;
            }
            catch (RuntimeException exception)
            {
                DevilBoneGambler.LOGGER.error("Prize command failed, line={}", line, exception);
            }
        }

        return executed;
    }

    /**
     * Returns a description of the first problem of the entry command, or null when every line is well formed and
     * parses against the given dispatcher. A null dispatcher only checks the placeholders. The source type follows
     * the dispatcher, so the server passes its command source and the client its suggestion provider.
     */
    public static <S extends SharedSuggestionProvider> String problem(CommandDispatcher<S> dispatcher, S source,
            String raw)
    {
        for (String line : lines(raw))
        {
            String placeholderProblem = CommandPlaceholders.findProblem(line);

            if (placeholderProblem != null)
            {
                return placeholderProblem;
            }

            if (dispatcher == null || source == null)
            {
                continue;
            }

            try
            {
                ParseResults<S> results = dispatcher.parse(CommandPlaceholders.sampleForm(line), source);

                if (!results.getExceptions().isEmpty())
                {
                    return results.getExceptions().values().iterator().next().getMessage();
                }

                if (results.getReader().canRead())
                {
                    return "trailing input: " + results.getReader().getRemaining();
                }
            }
            catch (RuntimeException exception)
            {
                /*
                 * Argument types that need live world state can fail outside a syntax error, which is not a reason
                 * to reject the entry.
                 */
            }
        }

        return null;
    }

    /**
     * Altar anchor of a player: the worn altar anchor is the player position, otherwise the governing altar block.
     */
    private static BlockPos altarOf(ServerPlayer player)
    {
        if (AltarService.wornAltarValue(player) >= 0L)
        {
            return player.blockPosition();
        }

        DevilsAltarBlockEntity altar = AltarService.nearestAltar(player);
        return altar == null ? null : altar.getBlockPos();
    }
}
