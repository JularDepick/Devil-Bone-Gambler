/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Whether an item or a mob may be offered at an altar.
 * The items and mobs of this mod are excluded by their registry namespace, never by a hard coded list, so every item
 * and every mob of the mod is excluded the moment it is registered and nothing has to be updated when one is added.
 */
public final class OfferEligibility
{
    private OfferEligibility()
    {
    }

    public static boolean excluded(ItemStack stack)
    {
        return !stack.isEmpty() && excludedItem(stack.getItem());
    }

    public static boolean excludedItem(Item item)
    {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key != null && ModConstants.MOD_ID.equals(key.getNamespace());
    }

    /**
     * Registry id form, used for the ids that arrive from a client.
     */
    public static boolean excludedRegistryId(String registryId)
    {
        ResourceLocation key = ResourceLocation.tryParse(registryId == null ? "" : registryId);
        return key != null && ModConstants.MOD_ID.equals(key.getNamespace());
    }

    public static boolean excluded(Entity entity)
    {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && ModConstants.MOD_ID.equals(key.getNamespace());
    }
}
