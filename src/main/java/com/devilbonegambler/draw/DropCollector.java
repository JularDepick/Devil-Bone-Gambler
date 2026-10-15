/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.draw;

import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Recovers the drops of mobs that were killed as offerings.
 * The death event fires before the drops enter the world, so cancelling it keeps them out of the level.
 * Player deaths also fire the same event, so the collector only reacts to registered entities.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DropCollector
{
    private static final Map<UUID, List<ItemStack>> PENDING = new HashMap<UUID, List<ItemStack>>();

    private DropCollector()
    {
    }

    public static void begin(UUID entityId)
    {
        PENDING.put(entityId, new ArrayList<ItemStack>());
    }

    public static List<ItemStack> end(UUID entityId)
    {
        List<ItemStack> result = PENDING.remove(entityId);
        return result == null ? new ArrayList<ItemStack>() : result;
    }

    public static void clear()
    {
        PENDING.clear();
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event)
    {
        if (PENDING.isEmpty() || event.getEntity() instanceof ServerPlayer)
        {
            return;
        }

        List<ItemStack> sink = PENDING.get(event.getEntity().getUUID());

        if (sink == null)
        {
            return;
        }

        for (ItemEntity drop : event.getDrops())
        {
            sink.add(drop.getItem().copy());
        }

        event.setCanceled(true);
    }
}
