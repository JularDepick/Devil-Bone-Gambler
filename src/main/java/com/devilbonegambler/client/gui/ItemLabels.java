/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Names for the mod screens that list items and entity types.
 * A list shows the translated name, because a player reads Diamond and not minecraft:diamond, and the raw registry id
 * goes into a hover tooltip so an administrator can still tell which id a row stands for.
 */
public final class ItemLabels
{
    private ItemLabels()
    {
    }

    /*
     * Both lookups test the key first: the item and creature registries are defaulted, so a plain getValue on an
     * unknown id answers with the default entry (air and the pig) instead of failing.
     */
    public static Item itemOf(String id)
    {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null || !ForgeRegistries.ITEMS.containsKey(key) ? null : ForgeRegistries.ITEMS.getValue(key);
    }

    public static EntityType<?> entityTypeOf(String id)
    {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null || !ForgeRegistries.ENTITY_TYPES.containsKey(key)
                ? null : ForgeRegistries.ENTITY_TYPES.getValue(key);
    }

    public static ItemStack itemStackOf(String id)
    {
        Item item = itemOf(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The raw registry id of a stack, empty when the item is unregistered. */
    public static String idOf(ItemStack stack)
    {
        ResourceLocation key = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    /** The translated item name, falling back to the id when the item is not registered. */
    public static String itemName(String id)
    {
        ItemStack stack = itemStackOf(id);
        return stack.isEmpty() ? id : stack.getHoverName().getString();
    }

    /** The translated creature name, falling back to the id when the type is not registered. */
    public static String entityName(String id)
    {
        EntityType<?> type = entityTypeOf(id);
        return type == null ? id : type.getDescription().getString();
    }

    /** The hover line of a translated row: the raw id, so the row can be traced back to its configuration entry. */
    public static Component idTooltip(String id)
    {
        return Component.literal(id);
    }
}
