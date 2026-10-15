/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.effect;

import com.devilbonegambler.util.ModConstants;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Bloodlust: the observable behaviour of the altar range effect.
 * It is neutral rather than harmful so it is not removed by the vanilla beneficial or harmful rules,
 * and its only visible effect is the accelerated food exhaustion of the affected player.
 */
public class BloodlustEffect extends MobEffect
{
    private static final int COLOR = 0x6E2430;

    public BloodlustEffect()
    {
        super(MobEffectCategory.NEUTRAL, COLOR);
    }

    /**
     * The effect ticks on every tick of its lifetime.
     */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier)
    {
        return true;
    }

    /**
     * Drains food saturation faster for every level of the effect.
     */
    @Override
    public void applyEffectTick(LivingEntity livingEntity, int amplifier)
    {
        if (livingEntity instanceof Player player)
        {
            player.causeFoodExhaustion(ModConstants.ALTAR_EXHAUSTION_PER_TICK * (amplifier + 1));
        }
    }
}
