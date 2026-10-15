/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.draw;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * Builds the deterministic seed of one draw.
 * The seed mixes the timestamp, the player identity, the player position, the request sequence and the offering digest,
 * so that an audited draw can be reproduced from the log line alone.
 */
public final class DrawSeed
{
    private static final long BASE = 0x243F6A8885A308D3L;
    private static final long ODD_MULTIPLIER = 0x9E3779B97F4A7C15L;
    private static final int SHIFT = 30;

    private DrawSeed()
    {
    }

    public static long of(ServerPlayer player, long timestamp, int sequence, List<Offering> offerings)
    {
        long seed = BASE;
        seed = mix(seed, timestamp);
        seed = mix(seed, player.getUUID().getMostSignificantBits());
        seed = mix(seed, player.getUUID().getLeastSignificantBits());
        seed = mix(seed, coordinateHash(player));
        seed = mix(seed, player.level().dimension().location().hashCode());
        seed = mix(seed, sequence);
        seed = mix(seed, digest(offerings));
        return seed;
    }

    /**
     * Vanilla style coordinate folding, so that nearby positions still spread out before the seed is mixed.
     */
    public static long coordinateHash(ServerPlayer player)
    {
        long x = Mth.floor(player.getX());
        long y = Mth.floor(player.getY());
        long z = Mth.floor(player.getZ());
        long folded = (x * 3129871L) ^ (z * 116129781L) ^ y;
        return folded * folded * 42317861L + folded * 11L;
    }

    /**
     * Bit mixer: the legacy random source keeps only the low 48 bits of a seed, so nearby inputs must be spread out first.
     */
    public static long mix(long seed, long value)
    {
        long mixed = seed ^ value;
        mixed *= ODD_MULTIPLIER;
        mixed ^= mixed >>> SHIFT;
        return mixed;
    }

    private static long digest(List<Offering> offerings)
    {
        long result = 0L;

        if (offerings == null)
        {
            return result;
        }

        for (Offering offering : offerings)
        {
            result = mix(result, offering.itemId().hashCode());
            result = mix(result, offering.count());
        }

        return result;
    }
}
