/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.altar;

import com.devilbonegambler.block.DevilsAltarBlockEntity;
import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.registry.ModBlocks;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server side altar service: locates the nearest altar of a player, drives the periodic range effect and
 * owns the range exclusivity rules.
 * An altar worn in the head slot counts as an altar placed at the wearer position, so the two share the same
 * value, radius and effect, and two altar ranges may never overlap.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AltarService
{
    /**
     * Returned instead of a value when no altar applies to a player.
     */
    public static final long NO_ALTAR = -1L;

    private static final String KEY_INTRUSION = "gui." + ModConstants.MOD_ID + ".altar.intrusion";

    private static int effectCounter;
    private static int intrusionCounter;

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
     * The altar item worn in the head slot, or an empty stack when none is worn.
     */
    public static ItemStack wornAltar(ServerPlayer player)
    {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        return stack.is(ModBlocks.DEVILS_ALTAR_ITEM.get()) ? stack : ItemStack.EMPTY;
    }

    /**
     * Offering value carried by the worn altar, or NO_ALTAR when nothing is worn.
     */
    public static long wornAltarValue(ServerPlayer player)
    {
        ItemStack stack = wornAltar(player);

        if (stack.isEmpty())
        {
            return NO_ALTAR;
        }

        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : Math.max(0L, tag.getLong(ModConstants.ALTAR_VALUE_TAG));
    }

    /**
     * Value of the altar that governs the player: the altar they wear, or an altar whose range covers them.
     * Returns NO_ALTAR when no altar applies. While two ranges briefly overlap the stronger one wins.
     */
    public static long governingValue(ServerPlayer player)
    {
        long best = wornAltarValue(player);
        DevilsAltarBlockEntity nearest = nearestAltar(player);

        if (nearest != null)
        {
            long value = nearest.offeringValue();

            if (inRange(player, ModConstants.altarRadiusFor(value)))
            {
                best = Math.max(best, value);
            }
        }

        MinecraftServer server = player.getServer();

        if (server != null)
        {
            for (ServerPlayer other : server.getPlayerList().getPlayers())
            {
                if (other == player)
                {
                    continue;
                }

                long value = wornAltarValue(other);

                if (value < 0L)
                {
                    continue;
                }

                int radius = ModConstants.altarRadiusFor(value);

                if (player.distanceToSqr(other) <= (double) radius * (double) radius)
                {
                    best = Math.max(best, value);
                }
            }
        }

        return best;
    }

    /**
     * Value of the nearest altar regardless of the range, which is the level the halved carried effect keeps.
     */
    public static long nearestAltarValue(ServerPlayer player)
    {
        DevilsAltarBlockEntity altar = nearestAltar(player);
        return altar == null ? 0L : altar.offeringValue();
    }

    /**
     * Offering value used by the draw pipeline, zero when no altar governs the player.
     */
    public static long altarValueFor(ServerPlayer player)
    {
        long value = governingValue(player);
        return value < 0L ? 0L : value;
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
     * True when any altar range covers the position, ignoring the altar at the excluded position.
     * Both placed altars and altars worn by players are sources, which is what keeps two ranges apart.
     */
    public static boolean rangeCovers(ServerLevel level, BlockPos pos, BlockPos excluded)
    {
        int reach = ModConstants.ALTAR_MAX_RADIUS;
        BlockPos min = pos.offset(-reach, -reach, -reach);
        BlockPos max = pos.offset(reach, reach, reach);

        for (BlockPos cursor : BlockPos.betweenClosed(min, max))
        {
            if (excluded != null && cursor.equals(excluded))
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

            int radius = ModConstants.altarRadiusFor(altar.offeringValue());

            if (cursor.distSqr(pos) <= (double) radius * (double) radius)
            {
                return true;
            }
        }

        MinecraftServer server = level.getServer();

        if (server == null)
        {
            return false;
        }

        for (ServerPlayer other : server.getPlayerList().getPlayers())
        {
            if (other.level() != level)
            {
                continue;
            }

            long value = wornAltarValue(other);

            if (value < 0L)
            {
                continue;
            }

            int radius = ModConstants.altarRadiusFor(value);

            if (other.blockPosition().distSqr(pos) <= (double) radius * (double) radius)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * True when the player wears an altar and an altar range other than that worn altar covers them.
     */
    public static boolean isIntruding(ServerPlayer player)
    {
        if (wornAltar(player).isEmpty())
        {
            return false;
        }

        MinecraftServer server = player.getServer();

        if (server != null)
        {
            for (ServerPlayer other : server.getPlayerList().getPlayers())
            {
                if (other == player)
                {
                    continue;
                }

                long value = wornAltarValue(other);

                if (value < 0L)
                {
                    continue;
                }

                int radius = ModConstants.altarRadiusFor(value);

                if (player.distanceToSqr(other) <= (double) radius * (double) radius)
                {
                    return true;
                }
            }
        }

        DevilsAltarBlockEntity nearest = nearestAltar(player);

        if (nearest == null)
        {
            return false;
        }

        return inRange(player, ModConstants.altarRadiusFor(nearest.offeringValue()));
    }

    /**
     * Stores an accumulated offering value into the worn altar, or into the nearest altar that covers the player.
     * Returns false when the player owns no altar that may accumulate the value.
     */
    public static boolean accumulateOfferingValue(ServerPlayer player, long value)
    {
        ItemStack worn = wornAltar(player);

        if (!worn.isEmpty())
        {
            long total = Math.min(ModConstants.ALTAR_MAX_VALUE, wornAltarValue(player) + Math.max(0L, value));
            worn.getOrCreateTag().putLong(ModConstants.ALTAR_VALUE_TAG, total);
            player.setItemSlot(EquipmentSlot.HEAD, worn);
            return true;
        }

        DevilsAltarBlockEntity altar = nearestAltar(player);

        if (altar == null || !inRange(player, ModConstants.altarRadiusFor(altar.offeringValue())))
        {
            return false;
        }

        altar.addValue(value);
        return true;
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

        MinecraftServer server = event.getServer();

        effectCounter++;

        if (effectCounter >= ModConstants.ALTAR_EFFECT_INTERVAL_TICKS)
        {
            effectCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers())
            {
                applyRangeEffect(player);
            }
        }

        intrusionCounter++;

        if (intrusionCounter >= ModConstants.ALTAR_INTRUSION_INTERVAL_TICKS)
        {
            intrusionCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers())
            {
                applyIntrusion(player);
            }
        }
    }

    /**
     * The altar range grants the vanilla damage boost, whose level follows the altar level.
     * The effect is refreshed once per interval and overwritten while the player stays in range, so it simply
     * expires after the player leaves.
     */
    private static void applyRangeEffect(ServerPlayer player)
    {
        long value = governingValue(player);

        if (value >= 0L)
        {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                    ModConstants.ALTAR_EFFECT_DURATION_TICKS, ModConstants.altarLevelFor(value)));
            return;
        }

        /*
         * Carrying an altar item keeps half of the effect anywhere in the world, with the level of the
         * nearest altar. The inventory is scanned by item identity, because a recovered altar carries an
         * NBT tag and a tag comparing lookup would miss it.
         */
        if (carriesAltar(player))
        {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                    ModConstants.ALTAR_EFFECT_DURATION_TICKS / ModConstants.ALTAR_CARRIED_EFFECT_DIVISOR,
                    ModConstants.altarLevelFor(nearestAltarValue(player))));
        }
    }

    /**
     * A wearer inside another altar range is pushed out by damage and a repeated chat warning.
     */
    private static void applyIntrusion(ServerPlayer player)
    {
        if (!isIntruding(player))
        {
            return;
        }

        player.hurt(player.damageSources().magic(), ModConstants.ALTAR_INTRUSION_DAMAGE);
        NotifySender.sendChat(player, KEY_INTRUSION);
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
