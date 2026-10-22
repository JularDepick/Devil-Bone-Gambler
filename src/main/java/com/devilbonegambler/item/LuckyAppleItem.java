/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.item;

import com.devilbonegambler.data.LuckData;
import com.devilbonegambler.luck.LuckLockService;
import com.devilbonegambler.luck.LuckService;
import com.devilbonegambler.luck.SubjectiveBehaviorService;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.LuckSyncPacket;
import com.devilbonegambler.registry.ModEffects;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The lucky apple.
 *
 * Eating it fills both the hunger bar and the health bar, and then one of two things happens. Eaten while standing, it
 * grants 幸运之子 for fifteen seconds; taken while the effect already runs, the bite extends the remaining time
 * instead of replacing it, and the lock keeps the value the first bite remembered. Eaten while sneaking, it raises the
 * luck value by ten points at once; from the point the value reaches eighty on, a sneaking bite stops paying luck and
 * grants the effect instead, so the apple stays worth eating at the top of the scale. The meal itself counts as a
 * behaviour worth five points, standing or sneaking, and it shares the one trigger of the round like every other
 * behaviour.
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
        clearHarmfulEffects(player);

        if (!sneakingBite(player))
        {
            grantLuckChild(player);
        }

        SubjectiveBehaviorService.recordLuckyApple(player);
        return result;
    }

    /**
     * Every harmful effect ends at once. The apple is a full reset of the body, so a meal also wipes the poison, the
     * wither and the rest instead of only refilling the two bars. The beneficial effect it grants itself is untouched,
     * and the lock is not involved because the lock only answers for the child of fortune.
     */
    private static void clearHarmfulEffects(ServerPlayer player)
    {
        List<MobEffectInstance> harmful = new ArrayList<MobEffectInstance>();

        for (MobEffectInstance effect : player.getActiveEffects())
        {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL)
            {
                harmful.add(effect);
            }
        }

        for (MobEffectInstance effect : harmful)
        {
            player.removeEffect(effect.getEffect());
        }
    }

    /**
     * The sneaking bite pays luck until the value reaches the limit, and reports whether it paid anything: once the
     * value is at the limit the bite falls through to the effect instead.
     */
    private static boolean sneakingBite(ServerPlayer player)
    {
        if (!player.isShiftKeyDown())
        {
            return false;
        }

        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return false;
        }

        LuckData data = LuckData.get(server);
        int current = data.ensure(player.getUUID(), LuckService.nowTick(server)).value();

        if (current >= ModConstants.LUCK_APPLE_SNEAK_LIMIT)
        {
            return false;
        }

        int raised = Math.min(ModConstants.MAX_LUCK, current + ModConstants.LUCK_APPLE_SNEAK_GAIN);
        data.setLuck(player.getUUID(), raised);
        ModNetwork.sendToPlayer(player, new LuckSyncPacket(raised));
        return true;
    }

    /**
     * Grants 幸运之子, or extends the remaining time when it already runs. The lock remembers only the first value, so
     * an extension never moves the value that comes back once the effect ends.
     */
    private static void grantLuckChild(ServerPlayer player)
    {
        MobEffectInstance running = player.getEffect(ModEffects.LUCK_CHILD.get());
        int ticks = ModConstants.LUCK_CHILD_TICKS;

        if (running != null)
        {
            ticks += running.getDuration();
        }

        player.addEffect(new MobEffectInstance(ModEffects.LUCK_CHILD.get(), ticks, 0, false, true, true));
        LuckLockService.lock(player);
    }
}
