/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.registry;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.block.DevilsAltarBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Block entity registration.
 */
public final class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DevilBoneGambler.MODID);

    public static final RegistryObject<BlockEntityType<DevilsAltarBlockEntity>> DEVILS_ALTAR =
            BLOCK_ENTITIES.register("devils_altar", () -> BlockEntityType.Builder
                    .of(DevilsAltarBlockEntity::new, ModBlocks.DEVILS_ALTAR.get())
                    .build(null));

    private ModBlockEntities()
    {
    }
}
