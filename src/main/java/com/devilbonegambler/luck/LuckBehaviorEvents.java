/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.luck;

import com.devilbonegambler.util.ModConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The violence side of the subjective behaviour variable: kills and deaths.
 *
 * Killing a hostile mob is a good deed and killing anything else, another player included, is a bad one; both go
 * through the shared round trigger, so a round still accepts at most one behaviour. Dying does not touch the behaviour
 * variable at all: it moves the luck value a step towards the comfortable band instead.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LuckBehaviorEvents
{
    private LuckBehaviorEvents()
    {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer victim)
        {
            LuckService.nudgeAfterDeath(victim);
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer killer))
        {
            return;
        }

        boolean hostile = !(event.getEntity() instanceof Player)
                && event.getEntity().getType().getCategory() == MobCategory.MONSTER;
        SubjectiveBehaviorService.recordKill(killer, hostile);
    }
}
