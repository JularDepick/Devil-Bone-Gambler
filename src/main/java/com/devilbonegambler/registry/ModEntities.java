/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.registry;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.mob.GamblersDealer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Entity type registration.
 * The dealer uses the villager sized hitbox so the villager model fits without a custom model.
 */
public final class ModEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DevilBoneGambler.MODID);

    public static final RegistryObject<EntityType<GamblersDealer>> GAMBLERS_DEALER = ENTITIES.register("gamblers_dealer",
            () -> EntityType.Builder.of(GamblersDealer::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(DevilBoneGambler.MODID + ":gamblers_dealer"));

    private ModEntities()
    {
    }
}
