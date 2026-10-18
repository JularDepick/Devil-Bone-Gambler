/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.manual;

import com.devilbonegambler.util.ModConstants;

/**
 * The three manuals of the mod.
 * A manual is a book shaped item with fixed, never craftable content: the gameplay manual explains the mod to any
 * player, the admin manual explains the configuration panel, and the creator manual repeats the admin manual behind a
 * creator only introduction. The gameplay manual is an ordinary item, the other two follow a role.
 * The content is shipped as markdown next to the language files, one file per language.
 */
public enum ManualKind
{
    GAMEPLAY("gameplay_manual", 0x00FF00),
    ADMIN("admin_manual", 0x00FFAA),
    CREATOR("creator_manual", 0xFFAA00);

    private final String itemId;
    private final int nameColor;

    ManualKind(String itemId, int nameColor)
    {
        this.itemId = itemId;
        this.nameColor = nameColor;
    }

    /**
     * Registry path of the item, which is also the stem of its content files.
     */
    public String itemId()
    {
        return this.itemId;
    }

    /**
     * Colour of the item name, one per manual so the three are told apart in the inventory and in the chat.
     */
    public int nameColor()
    {
        return this.nameColor;
    }

    /**
     * True for the manuals that follow a role: they may never be dropped and are granted and taken back with the role.
     * The gameplay manual is an ordinary item and may be kept, stored or thrown away.
     */
    public boolean bound()
    {
        return this != GAMEPLAY;
    }

    /**
     * Translation key of the item name.
     */
    public String titleKey()
    {
        return "item." + ModConstants.MOD_ID + "." + this.itemId;
    }

    /**
     * Resource path of the content in one language, relative to the mod assets root.
     */
    public String contentPath(String language)
    {
        return ModConstants.MANUAL_RESOURCE_DIR + this.itemId + "_" + language + ModConstants.MANUAL_RESOURCE_EXTENSION;
    }

    /**
     * Language code a client language resolves to: every Chinese variant reads the Simplified Chinese content and
     * everything else reads the English content.
     */
    public static String languageFor(String gameLanguage)
    {
        return gameLanguage != null && gameLanguage.toLowerCase(java.util.Locale.ROOT).startsWith("zh")
                ? "zh_cn" : "en_us";
    }

    /**
     * Manual of an item id, or null when the id is not a manual.
     */
    public static ManualKind byItemId(String itemId)
    {
        for (ManualKind kind : values())
        {
            if (kind.itemId.equals(itemId))
            {
                return kind;
            }
        }

        return null;
    }
}
