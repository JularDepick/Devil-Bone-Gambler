/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

/**
 * The roll attributes a sample space placeholder uses to pick its sample: the point, the overflow above the target
 * region, the luck value, the colour of the roll and the seed of the draw. The seed is per line: it is derived again
 * for every executed command line, so two lines of one entry never sample the same value.
 * A caller without a draw, like the /dbgr run command, uses the default context, which picks from the whole space with
 * no point or colour bias.
 */
public final class SampleContext
{
    public static final SampleContext DEFAULT = new SampleContext(0, 0, ModConstants.BASE_LUCK, false, 0L);

    private final int point;
    private final int overflow;
    private final int luck;
    private final boolean red;
    private final long seed;

    public SampleContext(int point, int overflow, int luck, boolean red, long seed)
    {
        this.point = point;
        this.overflow = overflow;
        this.luck = luck;
        this.red = red;
        this.seed = seed;
    }

    public int point()
    {
        return this.point;
    }

    public int overflow()
    {
        return this.overflow;
    }

    public int luck()
    {
        return this.luck;
    }

    public boolean red()
    {
        return this.red;
    }

    public long seed()
    {
        return this.seed;
    }

    /**
     * The same roll attributes with a seed derived from the given counter, used once per executed command line so
     * that neither two lines of one entry nor two repetitions of one line share a random stream.
     */
    public SampleContext derive(long counter)
    {
        return new SampleContext(this.point, this.overflow, this.luck, this.red, mix(this.seed, counter));
    }

    /**
     * Bit mixer, the same one the draw seed uses: the legacy random source keeps only the low 48 bits, so nearby
     * counters must be spread out before they reach it.
     */
    private static long mix(long seed, long value)
    {
        long mixed = seed ^ value;
        mixed *= 0x9E3779B97F4A7C15L;
        mixed ^= mixed >>> 30;
        return mixed;
    }

    /**
     * True when the context really came from a draw, which is the only case that carries a colour.
     */
    public boolean fromDraw()
    {
        return this.point > 0;
    }
}
