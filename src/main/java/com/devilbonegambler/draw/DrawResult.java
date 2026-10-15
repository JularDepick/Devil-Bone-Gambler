/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.draw;

/**
 * Outcome of one draw attempt.
 * Accepted means every validation passed and the result is delivered later, after the point display delay.
 */
public final class DrawResult
{
    private final boolean success;
    private final boolean accepted;
    private final String reasonKey;

    private DrawResult(boolean success, boolean accepted, String reasonKey)
    {
        this.success = success;
        this.accepted = accepted;
        this.reasonKey = reasonKey;
    }

    public static DrawResult success()
    {
        return new DrawResult(true, false, "");
    }

    public static DrawResult accepted()
    {
        return new DrawResult(true, true, "");
    }

    public static DrawResult failure(String reasonKey)
    {
        return new DrawResult(false, false, reasonKey);
    }

    public boolean isSuccess()
    {
        return this.success;
    }

    public boolean isAccepted()
    {
        return this.accepted;
    }

    public String reasonKey()
    {
        return this.reasonKey;
    }
}
