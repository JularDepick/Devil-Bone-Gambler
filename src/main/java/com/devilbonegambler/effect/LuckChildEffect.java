/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The 幸运之子 effect: while it lasts the luck value is held at its ceiling and the periodic update leaves it alone.
 *
 * The effect itself carries no logic; the lock is applied when the effect starts and released when it ends, so the same
 * value is restored no matter how the effect disappears. Its icon is generated from the lucky apple texture, so both
 * always show the same apple.
 */
public class LuckChildEffect extends MobEffect
{
    public LuckChildEffect(int color)
    {
        super(MobEffectCategory.BENEFICIAL, color);
    }
}
