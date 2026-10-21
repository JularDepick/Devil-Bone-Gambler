/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.registry;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.item.DevilsDiceItem;
import com.devilbonegambler.item.GamblersDiceItem;
import com.devilbonegambler.item.LuckyAppleItem;
import com.devilbonegambler.item.ManualItem;
import com.devilbonegambler.manual.ManualKind;
import com.devilbonegambler.item.OfferingValueTableItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Item registration.
 * The dice keep the vanilla default stack size of 64; the value table follows the vanilla book stack size of 1.
 * The dealer spawn egg uses the Forge spawn egg, which carries the entity type for the vanilla placement path.
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

    /*
     * The lucky apple is edible, but it fills the hunger bar from its own class instead of through the food value, so a
     * meal always restores it to full even when the player is nearly full.
     */
    public static final RegistryObject<Item> LUCKY_APPLE = ITEMS.register("lucky_apple",
            () -> new LuckyAppleItem(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(0).saturationMod(0.0F).alwaysEat().build())));

    public static final RegistryObject<Item> GAMBLERS_DEALER_SPAWN_EGG = ITEMS.register("gamblers_dealer_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GAMBLERS_DEALER, 0x4A3B2A, 0xC8A165, new Item.Properties()));

    /*
     * The three manuals are book shaped and never craftable. The gameplay manual is an ordinary book that may be
     * stored away and stacks as usual, while the creator and the admin manual follow a role and never stack.
     */
    public static final RegistryObject<Item> GAMEPLAY_MANUAL = ITEMS.register(ManualKind.GAMEPLAY.itemId(),
            () -> new ManualItem(ManualKind.GAMEPLAY, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ADMIN_MANUAL = registerBoundManual(ManualKind.ADMIN);
    public static final RegistryObject<Item> CREATOR_MANUAL = registerBoundManual(ManualKind.CREATOR);

    private static RegistryObject<Item> registerBoundManual(ManualKind kind)
    {
        return ITEMS.register(kind.itemId(), () -> new ManualItem(kind, new Item.Properties().stacksTo(1)));
    }

    /**
     * Registered item of a manual kind, used by the grant service.
     */
    public static Item manualOf(ManualKind kind)
    {
        if (kind == ManualKind.CREATOR)
        {
            return CREATOR_MANUAL.get();
        }

        if (kind == ManualKind.ADMIN)
        {
            return ADMIN_MANUAL.get();
        }

        return GAMEPLAY_MANUAL.get();
    }

    private ModItems()
    {
    }
}
