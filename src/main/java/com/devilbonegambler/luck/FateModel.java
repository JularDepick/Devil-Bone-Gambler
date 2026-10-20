/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.luck;

/**
 * The hidden fate model: a deterministic mapping from the birth instant and the current instant to a value in
 * [-50, 50].
 *
 * The model is a three channel 卦气 superposition. Each channel reads the elapsed time through one traditional period,
 * chooses one of the twelve 辟卦 by its 十二消息 phase, and reads the current 爻 inside that 辟卦. The three scales are
 * the solar year (365.25 days), the synodic month (29.530588 days) and the day, so the channels never lock together and
 * the value keeps moving for decades.
 *
 * Only the elapsed time y - x enters the channels: the birth instant shifts the phase but never drifts, and no calendar,
 * time zone, leap day or game tick is involved. Wall clock milliseconds in UTC are the only input.
 */
public final class FateModel
{
    private static final long YEAR_MS = 31557600000L;
    private static final long SYNODIC_MONTH_MS = 2551442803L;
    private static final long DAY_MS = 86400000L;

    /* The three channels: the year carries the 卦气 skeleton, the month and the day add the shorter tides. */
    private static final double YEAR_WEIGHT = 0.55D;
    private static final double MONTH_WEIGHT = 0.30D;
    private static final double DAY_WEIGHT = 0.15D;

    private FateModel()
    {
    }

    /**
     * The fate value of a player, always inside the closed interval [-50, 50].
     */
    public static double value(long birthMillis, long nowMillis)
    {
        long elapsed = Math.max(0L, nowMillis - birthMillis);
        double channel = YEAR_WEIGHT * channel(elapsed, YEAR_MS)
                + MONTH_WEIGHT * channel(elapsed, SYNODIC_MONTH_MS)
                + DAY_WEIGHT * channel(elapsed, DAY_MS);
        return Math.max(-50.0D, Math.min(50.0D, 50.0D * channel));
    }

    /**
     * One channel: twelve 辟卦 over the period, each split into six 爻. The 辟卦 grows one yang line from 复 to 乾 and
     * then one yin line from 姤 to 坤, so the yang count is a triangular wave and the current line follows the same
     * growth.
     */
    private static double channel(long elapsed, long period)
    {
        long remainder = elapsed % period;
        int hexagram = (int) (12L * remainder / period);
        int line = (int) ((72L * remainder / period) % 6L);
        hexagram = Math.max(0, Math.min(11, hexagram));
        line = Math.max(0, Math.min(5, line));
        int yang = hexagram < 6 ? hexagram + 1 : 11 - hexagram;
        boolean lineYang = hexagram < 6 ? line <= hexagram : line > hexagram - 6;
        double lineValue = lineYang ? 1.0D : -1.0D;
        return 0.75D * (yang - 3) / 3.0D + 0.25D * lineValue;
    }
}
