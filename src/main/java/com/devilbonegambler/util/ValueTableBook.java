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
import com.devilbonegambler.network.packet.ValueTableSyncPacket;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Reads and writes the value table snapshot carried by the offering value table book.
 * The snapshot is the data source of the searchable view and of the client hover estimate, and it is pushed to the
 * client on join and on every refresh, so the screens never read a config file themselves.
 */
public final class ValueTableBook
{
    private static final String TAG_REVISION = "revision";
    private static final String TAG_DEFAULT_ITEM = "defaultItemValue";
    private static final String TAG_DEFAULT_ENTITY = "defaultEntityValue";
    private static final String TAG_ITEMS = "items";
    private static final String TAG_ENTITIES = "entities";

    /**
     * Title of the searchable view, resolved on the client from this key so the screen text follows the client
     * language instead of being baked into the item data.
     */
    public static final String BOOK_TITLE_KEY = "gui." + ModConstants.MOD_ID + ".value.book_title";

    private ValueTableBook()
    {
    }

    /**
     * Server side: rewrites the snapshot of the held book from the live config and asks the client to open the
     * searchable view. The searchable view reads the pushed snapshot, so only the stack tag needs a refresh here.
     */
    public static void refreshSnapshot(ItemStack stack, ServerPlayer player)
    {
        ItemValueConfig config = ServerConfigManager.itemValues();

        if (config == null || stack.isEmpty())
        {
            return;
        }

        CompoundTag snapshot = buildSnapshotTag(config);
        stack.getOrCreateTag().put(ModConstants.VALUE_TABLE_TAG, snapshot);
        /*
         * The searchable view reads the pushed snapshot, so the fresh one has to reach the client before the screen
         * opens, otherwise a right click after an admin edit would show the older table.
         */
        ModNetwork.sendToPlayer(player, new ValueTableSyncPacket(snapshot));
        ModNetwork.sendToPlayer(player, new OpenValueTablePacket());
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

        return fromTag(tag.getCompound(ModConstants.VALUE_TABLE_TAG));
    }

    /**
     * Server side: the snapshot tag of the live config, or null when the config is not loaded.
     */
    public static CompoundTag snapshotTag()
    {
        ItemValueConfig config = ServerConfigManager.itemValues();

        return config == null ? null : buildSnapshotTag(config);
    }

    /**
     * Reads a standalone snapshot tag, used by the join time sync that does not go through an item.
     * Both chapters are read, because the searchable view lists items and creatures alike.
     */
    public static ValueTableSnapshot fromTag(CompoundTag snapshot)
    {
        if (snapshot == null || !snapshot.contains(TAG_ITEMS, Tag.TAG_COMPOUND))
        {
            return null;
        }

        return new ValueTableSnapshot(snapshot.getInt(TAG_REVISION), snapshot.getInt(TAG_DEFAULT_ITEM),
                snapshot.getInt(TAG_DEFAULT_ENTITY), readChapter(snapshot, TAG_ITEMS),
                readChapter(snapshot, TAG_ENTITIES));
    }

    private static Map<String, Integer> readChapter(CompoundTag snapshot, String key)
    {
        Map<String, Integer> chapter = new HashMap<String, Integer>();

        if (!snapshot.contains(key, Tag.TAG_COMPOUND))
        {
            return chapter;
        }

        CompoundTag stored = snapshot.getCompound(key);

        for (String entry : stored.getAllKeys())
        {
            chapter.put(entry, Integer.valueOf(stored.getInt(entry)));
        }

        return chapter;
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

}