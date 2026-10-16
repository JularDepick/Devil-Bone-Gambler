/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Who may manage the mod: the creator plus the admin names the creator granted.
 * Only player names are stored, never identifiers, so the file stays readable and hand editable.
 * The creator is the one who joined first when the file was still empty, and stays the creator afterwards.
 */
public class AccessConfig
{
    public static final int CURRENT_SCHEMA = 1;

    public Integer schemaVersion;
    public String creator;
    public List<String> admins;

    public AccessConfig()
    {
    }

    public static AccessConfig empty()
    {
        AccessConfig result = new AccessConfig();
        result.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);
        result.creator = "";
        result.admins = new ArrayList<String>();
        return result;
    }

    public String creatorOrEmpty()
    {
        return this.creator == null ? "" : this.creator.trim();
    }

    public List<String> adminsOrEmpty()
    {
        return this.admins == null ? new ArrayList<String>() : this.admins;
    }

    public boolean hasCreator()
    {
        return !creatorOrEmpty().isEmpty();
    }

    /**
     * Name comparison ignores case, because a player name is unique case insensitively and a hand written entry
     * should not silently stop working over a capital letter.
     */
    private static boolean sameName(String first, String second)
    {
        return first != null && second != null && first.trim().equalsIgnoreCase(second.trim());
    }

    public boolean isCreator(String name)
    {
        return hasCreator() && sameName(creatorOrEmpty(), name);
    }

    public boolean isListedAdmin(String name)
    {
        for (String admin : adminsOrEmpty())
        {
            if (sameName(admin, name))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * The creator counts as a manager without being listed, so the admins array only holds the granted names.
     */
    public boolean isManager(String name)
    {
        return isCreator(name) || isListedAdmin(name);
    }

    /**
     * Only the creator may change the admin list.
     */
    public boolean canManageAdmins(String name)
    {
        return isCreator(name);
    }

    public boolean setCreatorIfAbsent(String name)
    {
        if (hasCreator() || name == null || name.trim().isEmpty())
        {
            return false;
        }

        this.creator = name.trim();
        return true;
    }

    /**
     * Adds one granted admin. The creator is refused, because the creator is a manager without being listed.
     */
    public boolean addAdmin(String name)
    {
        String candidate = name == null ? "" : name.trim();

        if (candidate.isEmpty() || isCreator(candidate) || isListedAdmin(candidate))
        {
            return false;
        }

        if (this.admins == null)
        {
            this.admins = new ArrayList<String>();
        }

        this.admins.add(candidate);
        return true;
    }

    /**
     * Removes one granted admin. The creator can never be removed, which is the only protected entry.
     */
    public boolean removeAdmin(String name)
    {
        if (name == null || isCreator(name))
        {
            return false;
        }

        List<String> current = adminsOrEmpty();
        String key = name.trim().toLowerCase(Locale.ROOT);

        for (int index = 0; index < current.size(); index++)
        {
            String entry = current.get(index);

            if (entry != null && entry.trim().toLowerCase(Locale.ROOT).equals(key))
            {
                current.remove(index);
                return true;
            }
        }

        return false;
    }

    /**
     * Keeps the file tidy: one schema version, a trimmed creator and a deduplicated admin list in file order.
     */
    public void normalize()
    {
        this.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);
        this.creator = creatorOrEmpty();

        List<String> cleaned = new ArrayList<String>();
        List<String> seen = new ArrayList<String>();

        for (String admin : adminsOrEmpty())
        {
            String candidate = admin == null ? "" : admin.trim();
            String key = candidate.toLowerCase(Locale.ROOT);

            if (candidate.isEmpty() || seen.contains(key))
            {
                continue;
            }

            seen.add(key);
            cleaned.add(candidate);
        }

        this.admins = cleaned;
    }
}
