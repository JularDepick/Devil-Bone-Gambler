/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.altar;

import com.devilbonegambler.block.DevilsAltarBlockEntity;
import com.devilbonegambler.registry.ModBlocks;
import com.devilbonegambler.registry.ModEffects;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server side altar service: locates the nearest altar of a player and drives the periodic
 * range effect. Only the closest altar counts when several altars are nearby.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AltarService
{
    private static int tickCounter;

    private AltarService()
    {
    }

    /**
     * Returns the altar closest to the player inside the maximum altar radius, or null when none exists.
     * The search is a cubic distance search, horizontal distance alone is never used.
     */
    public static DevilsAltarBlockEntity nearestAltar(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        AABB area = new AABB(origin).inflate(ModConstants.ALTAR_MAX_RADIUS);
        BlockPos min = BlockPos.containing(area.minX, area.minY, area.minZ);
        BlockPos max = BlockPos.containing(area.maxX, area.maxY, area.maxZ);
        DevilsAltarBlockEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        double searchDistance = (double) ModConstants.ALTAR_MAX_RADIUS * (double) ModConstants.ALTAR_MAX_RADIUS;

        for (BlockPos cursor : BlockPos.betweenClosed(min, max))
        {
            /*
             * The scan box is a cube while the altar range is a sphere, so drop the corners first
             * to avoid a block lookup for roughly half of the scanned positions.
             */
            if (cursor.distSqr(origin) > searchDistance)
            {
                continue;
            }

            if (!level.getBlockState(cursor).is(ModBlocks.DEVILS_ALTAR.get()))
            {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(cursor);

            if (!(blockEntity instanceof DevilsAltarBlockEntity altar))
            {
                continue;
            }

            double distance = player.distanceToSqr(cursor.getX() + 0.5D, cursor.getY() + 0.5D, cursor.getZ() + 0.5D);

            if (distance < nearestDistance)
            {
                nearestDistance = distance;
                nearest = altar;
            }
        }

        return nearest;
    }

    /**
     * Offering value of the nearest altar, zero when the player has no altar nearby.
     */
    public static long altarValueFor(ServerPlayer player)
    {
        DevilsAltarBlockEntity altar = nearestAltar(player);
        return altar == null ? 0L : altar.offeringValue();
    }

    /**
     * Whether the nearest altar sits inside the given cubic distance.
     */
    public static boolean inRange(ServerPlayer player, int radius)
    {
        DevilsAltarBlockEntity altar = nearestAltar(player);

        if (altar == null)
        {
            return false;
        }

        BlockPos pos = altar.getBlockPos();
        double distance = player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        return distance <= (double) radius * (double) radius;
    }

    /**
     * The tick event fires twice per tick, so only the end phase is counted.
     * The effect refresh runs once per configured interval and overwrites the running effect,
     * which means the effect simply expires after the player leaves the range.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        tickCounter++;

        if (tickCounter < ModConstants.ALTAR_EFFECT_INTERVAL_TICKS)
        {
            return;
        }

        tickCounter = 0;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers())
        {
            applyRangeEffect(player);
        }
    }

    private static void applyRangeEffect(ServerPlayer player)
    {
        DevilsAltarBlockEntity altar = nearestAltar(player);

        if (altar != null)
        {
            long value = altar.offeringValue();

            if (inRange(player, ModConstants.altarRadiusFor(value)))
            {
                player.addEffect(new MobEffectInstance(ModEffects.BLOODLUST.get(),
                        ModConstants.ALTAR_EFFECT_DURATION_TICKS, ModConstants.altarLevelFor(value)));
            }
        }

        /*
         * Carrying an altar item keeps half of the effect anywhere in the world, with the level
         * of the nearest altar. It is applied after the range effect so the shorter duration can
         * never cut the range effect short.
         * The inventory is scanned by item identity, because a recovered altar carries an NBT tag
         * and a tag comparing lookup would miss it.
         */
        if (carriesAltar(player))
        {
            player.addEffect(new MobEffectInstance(ModEffects.BLOODLUST.get(),
                    ModConstants.ALTAR_EFFECT_DURATION_TICKS / ModConstants.ALTAR_CARRIED_EFFECT_DIVISOR,
                    ModConstants.altarLevelFor(altarValueFor(player))));
        }
    }

    /**
     * Whether any inventory slot holds an altar item, ignoring its accumulated offering value tag.
     */
    private static boolean carriesAltar(ServerPlayer player)
    {
        Inventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);

            if (!stack.isEmpty() && stack.is(ModBlocks.DEVILS_ALTAR_ITEM.get()))
            {
                return true;
            }
        }

        return false;
    }
}
