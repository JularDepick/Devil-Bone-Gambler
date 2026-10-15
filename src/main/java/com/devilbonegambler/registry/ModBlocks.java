/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.registry;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.block.DevilsAltarBlock;
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

    private ModBlocks()
    {
    }
}
