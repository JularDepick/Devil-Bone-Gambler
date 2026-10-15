/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.registry;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.item.DevilsDiceItem;
import com.devilbonegambler.item.GamblersDiceItem;
import com.devilbonegambler.item.OfferingValueTableItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Item registration.
 * The dice keep the vanilla default stack size of 64; the value table follows the vanilla book stack size of 1.
 */
public final class ModItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DevilBoneGambler.MODID);

    public static final RegistryObject<Item> GAMBLERS_DICE = ITEMS.register("gamblers_dice",
            () -> new GamblersDiceItem(new Item.Properties()));

    public static final RegistryObject<Item> DEVILS_DICE = ITEMS.register("devils_dice",
            () -> new DevilsDiceItem(new Item.Properties()));

    public static final RegistryObject<Item> OFFERING_VALUE_TABLE = ITEMS.register("offering_value_table",
            () -> new OfferingValueTableItem(new Item.Properties().stacksTo(1)));

    private ModItems()
    {
    }
}
