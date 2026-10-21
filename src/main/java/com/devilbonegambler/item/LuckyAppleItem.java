/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.item;

import com.devilbonegambler.luck.LuckLockService;
import com.devilbonegambler.luck.SubjectiveBehaviorService;
import com.devilbonegambler.registry.ModEffects;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The lucky apple.
 *
 * Eating it fills both the hunger bar and the health bar, then grants 幸运之子 for ten seconds: the luck value is held
 * at 100 for that time and returns to the value it had before the meal once the effect ends. The meal itself also counts
 * as a behaviour, because it restores hunger.
 */
public class LuckyAppleItem extends Item
{
    public LuckyAppleItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity)
    {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!(entity instanceof ServerPlayer player))
        {
            return result;
        }

        FoodData food = player.getFoodData();
        food.setFoodLevel(ModConstants.MAX_FOOD_LEVEL);
        food.setSaturation(ModConstants.MAX_FOOD_LEVEL);
        player.setHealth(player.getMaxHealth());
        player.addEffect(new MobEffectInstance(ModEffects.LUCK_CHILD.get(), ModConstants.LUCK_CHILD_TICKS, 0,
                false, true, true));
        LuckLockService.lock(player);
        SubjectiveBehaviorService.recordEating(player);
        return result;
    }
}
