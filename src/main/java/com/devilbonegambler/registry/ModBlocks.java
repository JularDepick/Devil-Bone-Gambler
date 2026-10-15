/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.registry;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.block.DevilsAltarBlock;
import com.devilbonegambler.block.EconomyBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Block registration.
 * The altar requires an iron pickaxe and stacks to one item because it carries a block entity value.
 */
public final class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DevilBoneGambler.MODID);

    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DevilBoneGambler.MODID);

    public static final RegistryObject<Block> DEVILS_ALTAR = BLOCKS.register("devils_altar",
            () -> new DevilsAltarBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final RegistryObject<Item> DEVILS_ALTAR_ITEM = BLOCK_ITEMS.register("devils_altar",
            () -> new BlockItem(DEVILS_ALTAR.get(), new Item.Properties().stacksTo(1)));

    /*
     * The four economy blocks are stateless: a stone pickaxe is enough, and they all stack to one item.
     */
    public static final RegistryObject<Block> GAMBLERS_BANK = BLOCKS.register("gamblers_bank",
            () -> new EconomyBlock(EconomyBlock.Kind.BANK, economyProperties()));

    public static final RegistryObject<Item> GAMBLERS_BANK_ITEM = BLOCK_ITEMS.register("gamblers_bank",
            () -> new BlockItem(GAMBLERS_BANK.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Block> GAMBLERS_SHOP = BLOCKS.register("gamblers_shop",
            () -> new EconomyBlock(EconomyBlock.Kind.SHOP, economyProperties()));

    public static final RegistryObject<Item> GAMBLERS_SHOP_ITEM = BLOCK_ITEMS.register("gamblers_shop",
            () -> new BlockItem(GAMBLERS_SHOP.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Block> GAMBLERS_EXCHANGE = BLOCKS.register("gamblers_exchange",
            () -> new EconomyBlock(EconomyBlock.Kind.EXCHANGE, economyProperties()));

    public static final RegistryObject<Item> GAMBLERS_EXCHANGE_ITEM = BLOCK_ITEMS.register("gamblers_exchange",
            () -> new BlockItem(GAMBLERS_EXCHANGE.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Block> GAMBLERS_RECYCLER = BLOCKS.register("gamblers_recycler",
            () -> new EconomyBlock(EconomyBlock.Kind.RECYCLER, economyProperties()));

    public static final RegistryObject<Item> GAMBLERS_RECYCLER_ITEM = BLOCK_ITEMS.register("gamblers_recycler",
            () -> new BlockItem(GAMBLERS_RECYCLER.get(), new Item.Properties().stacksTo(1)));

    private static BlockBehaviour.Properties economyProperties()
    {
        return BlockBehaviour.Properties.of()
                .strength(3.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.WOOD);
    }

    private ModBlocks()
    {
    }
}
