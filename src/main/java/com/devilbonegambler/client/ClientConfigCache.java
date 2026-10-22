/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client side config receiver.
 *
 * The panel payload arrives in parts, so this class glues them back together per domain and remembers the last full
 * copy of each. Opening the panel needs both copies plus the access data, which arrives first, so the panel opens on
 * the part that completes the second payload instead of on a partially filled screen.
 */
public final class ClientConfigCache
{
    private static final Map<String, List<String>> PARTS = new HashMap<String, List<String>>();
    private static final Map<String, String> PAYLOADS = new HashMap<String, String>();
    private static String creatorName = "";
    private static List<String> admins = new ArrayList<String>();
    private static boolean canManageAdmins;
    private static boolean panelPending;

    private ClientConfigCache()
    {
    }

    /**
     * Adds one part and returns the whole payload once every part is in, or null while something is still missing.
     */
    public static String accept(String domain, int partIndex, int partCount, String chunk)
    {
        List<String> parts = PARTS.get(domain);

        if (parts == null || parts.size() != partCount)
        {
            parts = new ArrayList<String>(Collections.nCopies(partCount, (String) null));
            PARTS.put(domain, parts);
        }

        parts.set(partIndex, chunk);

        for (String part : parts)
        {
            if (part == null)
            {
                return null;
            }
        }

        PARTS.remove(domain);
        StringBuilder builder = new StringBuilder();

        for (String part : parts)
        {
            builder.append(part);
        }

        return builder.toString();
    }

    public static void store(String domain, String payload)
    {
        PAYLOADS.put(domain, payload);
    }

    /**
     * Remembers who may edit what. The payloads may already be here from an earlier push, so an open may follow at once.
     */
    public static void onPanelData(String creator, List<String> names, boolean canManage)
    {
        creatorName = creator == null ? "" : creator;
        admins = names == null ? new ArrayList<String>() : new ArrayList<String>(names);
        canManageAdmins = canManage;
        panelPending = true;
    }

    public static void tryOpenPanel()
    {
        if (!panelPending)
        {
            return;
        }

        String pool = PAYLOADS.get(ModConstants.CONFIG_DOMAIN_PRIZE_POOL);
        String values = PAYLOADS.get(ModConstants.CONFIG_DOMAIN_ITEM_VALUES);

        if (pool == null || values == null)
        {
            return;
        }

        panelPending = false;
        ClientActions.openAdminPanel(pool, values, creatorName, admins, canManageAdmins);
    }

    public static void clear()
    {
        PARTS.clear();
        PAYLOADS.clear();
        panelPending = false;
        creatorName = "";
        admins = new ArrayList<String>();
        canManageAdmins = false;
    }
}
