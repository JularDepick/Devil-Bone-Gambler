/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network;

import com.devilbonegambler.util.ModConstants;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Per player request guard: monotonic request sequence, a minimum interval between draws, and a busy window
 * used while a draw is waiting for its delayed resolution.
 * A rejected request never advances the stored sequence, so a retry with a higher sequence still works.
 */
public final class RequestGuard
{
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<UUID, State>();

    private RequestGuard()
    {
    }

    public static String checkDraw(ServerPlayer player, int sequence)
    {
        return check(player, sequence, true);
    }

    public static String checkSequence(ServerPlayer player, int sequence)
    {
        return check(player, sequence, false);
    }

    /**
     * Marks the player as busy until the given delay elapses, blocking further draws in the meantime.
     */
    public static void markBusy(ServerPlayer player, int delayTicks)
    {
        State state = STATES.computeIfAbsent(player.getUUID(), key -> new State());
        state.busyUntil = System.currentTimeMillis() + delayTicks * 50L;
    }

    public static void clear()
    {
        STATES.clear();
    }

    private static String check(ServerPlayer player, int sequence, boolean throttled)
    {
        long now = System.currentTimeMillis();
        State state = STATES.computeIfAbsent(player.getUUID(), key -> new State());

        if (sequence <= state.sequence)
        {
            return "duplicate_request";
        }

        if (throttled && now < state.busyUntil)
        {
            return "too_frequent";
        }

        if (throttled && now - state.timestamp < ModConstants.MIN_REQUEST_INTERVAL_MS)
        {
            return "too_frequent";
        }

        state.sequence = sequence;

        if (throttled)
        {
            state.timestamp = now;
        }

        return null;
    }

    private static final class State
    {
        private int sequence = -1;
        private long timestamp = 0L;
        private long busyUntil = 0L;
    }
}
