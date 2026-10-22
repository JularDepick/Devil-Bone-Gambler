/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network;

import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits one config payload into the parts that travel in separate packets. A pool of thousands of entries is far
 * larger than a single packet may carry, so the payload is cut into fixed slices and the receiver glues them back
 * together in order.
 */
public final class ConfigChunks
{
    private ConfigChunks()
    {
    }

    public static List<String> split(String payload)
    {
        List<String> parts = new ArrayList<String>();

        if (payload == null || payload.isEmpty())
        {
            parts.add("");
            return parts;
        }

        int size = ModConstants.CONFIG_CHUNK_CHARS;

        for (int start = 0; start < payload.length(); start += size)
        {
            parts.add(payload.substring(start, Math.min(payload.length(), start + size)));
        }

        return parts;
    }
}
