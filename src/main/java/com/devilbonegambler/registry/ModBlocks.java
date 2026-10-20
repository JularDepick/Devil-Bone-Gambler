/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.registry;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.block.DealerGlowBlock;
import com.devilbonegambler.block.DevilsAltarBlock;
import com.devilbonegambler.item.DevilsAltarItem;
import com.devilbonegambler.util.ModConstants;
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
 * Its model is a custom multi part shape that does not fill the cube, so the block must not occlude the
 * neighbouring faces, otherwise the faces that touch the altar are culled and the gaps become see through.
 * The four candles of the model are lit, and the model format of this Minecraft version cannot emit light,
 * so the light level is declared here instead.
 */
public final class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DevilBoneGambler.MODID);

    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DevilBoneGambler.MODID);

    public static final RegistryObject<Block> DEVILS_ALTAR = BLOCKS.register("devils_altar",
            () -> new DevilsAltarBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, ModConstants.ALTAR_EXPLOSION_RESISTANCE)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> ModConstants.ALTAR_LIGHT_LEVEL)));

    /*
     * The dealer glow is registered as a block and never as an item, so no player can hold it and no creative tab
     * offers it; it only ever exists in the world under a dealer.
     */
    public static final RegistryObject<Block> DEALER_GLOW = BLOCKS.register("dealer_glow", DealerGlowBlock::new);

    public static final RegistryObject<Item> DEVILS_ALTAR_ITEM = BLOCK_ITEMS.register("devils_altar",
            () -> new DevilsAltarItem(DEVILS_ALTAR.get(), new Item.Properties().stacksTo(1)));

    private ModBlocks()
    {
    }
}
