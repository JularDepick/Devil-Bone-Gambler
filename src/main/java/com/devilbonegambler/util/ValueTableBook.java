/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

import com.devilbonegambler.config.ItemValueConfig;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.OpenValueTablePacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;

/**
 * Reads and writes the value table snapshot carried by the offering value table book.
 * The snapshot lives next to the vanilla written book tags so that the vanilla book screen keeps working.
 * Both chapters are written into the readable pages, while the client hover only needs the item chapter.
 */
public final class ValueTableBook
{
    private static final String TAG_REVISION = "revision";
    private static final String TAG_DEFAULT_ITEM = "defaultItemValue";
    private static final String TAG_DEFAULT_ENTITY = "defaultEntityValue";
    private static final String TAG_ENCHANT_VALUE = "enchantValueBase";
    private static final String TAG_ITEMS = "items";
    private static final String TAG_ENTITIES = "entities";

    private static final String TITLE_KEY = "gui." + ModConstants.MOD_ID + ".value.book_title";
    private static final String PAGE_HEADER_KEY = "gui." + ModConstants.MOD_ID + ".value.page_header";
    private static final String PAGE_ITEM_HEADER_KEY = "gui." + ModConstants.MOD_ID + ".value.page_item_header";
    private static final String PAGE_ENTITY_HEADER_KEY = "gui." + ModConstants.MOD_ID + ".value.page_entity_header";
    private static final String PAGE_LINE_KEY = "gui." + ModConstants.MOD_ID + ".value.page_line";
    private static final String PAGE_MORE_KEY = "gui." + ModConstants.MOD_ID + ".value.page_more";

    private static final int MAX_PAGES = 100;

    private ValueTableBook()
    {
    }

    /**
     * Server side: rewrites the snapshot from the live config and asks the client to open the book screen.
     */
    public static void refreshSnapshot(ItemStack stack, ServerPlayer player)
    {
        ItemValueConfig config = ServerConfigManager.itemValues();

        if (config == null || stack.isEmpty())
        {
            return;
        }

        String titleJson = Component.Serializer.toJson(Component.translatable(TITLE_KEY));
        List<String> pages = buildPages(config);

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(WrittenBookItem.TAG_TITLE, titleJson);
        tag.putString(WrittenBookItem.TAG_AUTHOR, AuthorInfo.AUTHOR_NAME);

        ListTag pageTags = new ListTag();

        for (String page : pages)
        {
            pageTags.add(StringTag.valueOf(page));
        }

        tag.put(WrittenBookItem.TAG_PAGES, pageTags);
        tag.putInt(WrittenBookItem.TAG_GENERATION, 0);
        tag.putBoolean(WrittenBookItem.TAG_RESOLVED, true);
        tag.put(ModConstants.VALUE_TABLE_TAG, buildSnapshotTag(config));

        ModNetwork.sendToPlayer(player, new OpenValueTablePacket(titleJson, AuthorInfo.AUTHOR_NAME, pages));
    }

    /**
     * Reads the item chapter of one stack snapshot, or null when the stack carries none.
     */
    public static ValueTableSnapshot readSnapshot(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return null;
        }

        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.contains(ModConstants.VALUE_TABLE_TAG, Tag.TAG_COMPOUND))
        {
            return null;
        }

        CompoundTag snapshot = tag.getCompound(ModConstants.VALUE_TABLE_TAG);
        CompoundTag stored = snapshot.getCompound(TAG_ITEMS);
        Map<String, Integer> values = new HashMap<String, Integer>();

        for (String key : stored.getAllKeys())
        {
            values.put(key, Integer.valueOf(stored.getInt(key)));
        }

        return new ValueTableSnapshot(snapshot.getInt(TAG_REVISION), snapshot.getInt(TAG_DEFAULT_ITEM),
                snapshot.getInt(TAG_ENCHANT_VALUE), values);
    }

    /**
     * Newest snapshot carried by the player inventory, or null when the player holds no value table.
     */
    public static ValueTableSnapshot findInInventory(Player player)
    {
        if (player == null)
        {
            return null;
        }

        Inventory inventory = player.getInventory();
        ValueTableSnapshot best = null;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ValueTableSnapshot snapshot = readSnapshot(inventory.getItem(slot));

            if (snapshot != null && (best == null || snapshot.revision() > best.revision()))
            {
                best = snapshot;
            }
        }

        return best;
    }

    private static CompoundTag buildSnapshotTag(ItemValueConfig config)
    {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putInt(TAG_REVISION, config.revisionOrZero());
        snapshot.putInt(TAG_DEFAULT_ITEM, config.defaultItemValueOrDefault());
        snapshot.putInt(TAG_DEFAULT_ENTITY, config.defaultEntityValueOrDefault());
        snapshot.putInt(TAG_ENCHANT_VALUE, config.enchantValueBaseOrDefault());
        snapshot.put(TAG_ITEMS, writeChapter(config.itemsOrEmpty()));
        snapshot.put(TAG_ENTITIES, writeChapter(config.entitiesOrEmpty()));
        return snapshot;
    }

    private static CompoundTag writeChapter(Map<String, Number> source)
    {
        CompoundTag result = new CompoundTag();

        for (Map.Entry<String, Number> entry : source.entrySet())
        {
            result.putInt(entry.getKey(), entry.getValue().intValue());
        }

        return result;
    }

    /**
     * Pages are laid out per chapter because the vanilla book wraps long lines and holds a limited page height.
     */
    private static List<String> buildPages(ItemValueConfig config)
    {
        List<String> pages = new ArrayList<String>();
        List<Component> lines = new ArrayList<Component>();
        int entriesOnPage = 0;
        boolean truncated = false;

        lines.add(Component.translatable(PAGE_HEADER_KEY, Integer.valueOf(config.revisionOrZero())));

        for (Map.Entry<String, Number> entry : config.itemsOrEmpty().entrySet())
        {
            if (entriesOnPage >= ModConstants.VALUE_TABLE_PAGE_ENTRIES)
            {
                if (pages.size() >= MAX_PAGES - 1)
                {
                    truncated = true;
                    break;
                }

                pages.add(buildPage(lines));
                lines = new ArrayList<Component>();
                entriesOnPage = 0;
            }

            lines.add(Component.translatable(PAGE_LINE_KEY, entry.getKey(),
                    Integer.valueOf(config.itemValueOf(entry.getKey()))));
            entriesOnPage++;
        }

        if (!truncated)
        {
            if (entriesOnPage >= ModConstants.VALUE_TABLE_PAGE_ENTRIES)
            {
                pages.add(buildPage(lines));
                lines = new ArrayList<Component>();
            }

            lines.add(Component.translatable(PAGE_ENTITY_HEADER_KEY));

            for (Map.Entry<String, Number> entry : config.entitiesOrEmpty().entrySet())
            {
                if (entriesOnPage >= ModConstants.VALUE_TABLE_PAGE_ENTRIES)
                {
                    if (pages.size() >= MAX_PAGES - 1)
                    {
                        truncated = true;
                        break;
                    }

                    pages.add(buildPage(lines));
                    lines = new ArrayList<Component>();
                    entriesOnPage = 0;
                }

                lines.add(Component.translatable(PAGE_LINE_KEY, entry.getKey(),
                        Integer.valueOf(config.entityValueOf(entry.getKey()))));
                entriesOnPage++;
            }
        }

        if (truncated)
        {
            lines.add(Component.translatable(PAGE_MORE_KEY));
        }

        if (!lines.isEmpty())
        {
            pages.add(buildPage(lines));
        }

        return pages;
    }

    private static String buildPage(List<Component> lines)
    {
        MutableComponent page = Component.empty();

        for (int index = 0; index < lines.size(); index++)
        {
            if (index > 0)
            {
                page.append(Component.literal("\n"));
            }

            page.append(lines.get(index));
        }

        return Component.Serializer.toJson(page);
    }
}
