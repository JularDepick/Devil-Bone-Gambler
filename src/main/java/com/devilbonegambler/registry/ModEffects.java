/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.registry;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.effect.LuckChildEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Effect registration.
 *
 * The lucky apple grants the only effect of the mod. It is registered as beneficial, so the sample space classifies it
 * as a buff and the random effect placeholder can pick it like any other buff.
 */
public final class ModEffects
{
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, DevilBoneGambler.MODID);

    public static final RegistryObject<MobEffect> LUCK_CHILD = EFFECTS.register("luck_child",
            () -> new LuckChildEffect(0xFFD24A));

    private ModEffects()
    {
    }
}
