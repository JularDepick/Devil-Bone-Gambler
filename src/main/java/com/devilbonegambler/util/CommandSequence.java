/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The server wide count of executed prize command lines.
 *
 * The count is mixed into the seed of every expansion, so two lines of one entry, two repetitions of one line and two
 * entries settled in the same millisecond never draw from the same stream. It is monotonic for the lifetime of the
 * server process and needs no storage.
 */
public final class CommandSequence
{
    private static final AtomicLong COUNTER = new AtomicLong();

    private CommandSequence()
    {
    }

    /**
     * Returns the next count, so every executed line gets its own value.
     */
    public static long next()
    {
        return COUNTER.incrementAndGet();
    }

    /**
     * The count reached so far, without advancing it.
     */
    public static long current()
    {
        return COUNTER.get();
    }
}
