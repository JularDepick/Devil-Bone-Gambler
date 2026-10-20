/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

/**
 * Client mirror of the server subjective behaviour state. It is informational only: every rule and every clamp stays
 * on the server, and this mirror merely exposes whether the current round already triggered and what the variable is.
 */
public final class ClientBehaviorState
{
    private static boolean triggered;
    private static int value;

    private ClientBehaviorState()
    {
    }

    public static void set(boolean triggered, int value)
    {
        ClientBehaviorState.triggered = triggered;
        ClientBehaviorState.value = value;
    }

    public static boolean triggered()
    {
        return triggered;
    }

    public static int value()
    {
        return value;
    }
}
