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
import net.minecraft.world.phys.Vec3;
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
        Governing governing = governing(player);
        return governing == null ? NO_ALTAR : governing.value();
    }

    /**
     * Range centre of the altar that governs a player: an altar block whose range covers the player, the altar the
     * player wears at the own position, or the altar another player wears at that wearer position.
     * Null when no altar governs the player.
     */
    public static Vec3 governingCentre(ServerPlayer player)
    {
        Governing governing = governing(player);
        return governing == null ? null : governing.centre();
    }

    private static Governing governing(ServerPlayer player)
    {
        long best = wornAltarValue(player);
        Vec3 centre = best < 0L ? null : player.position();
        DevilsAltarBlockEntity nearest = nearestAltar(player);

        if (nearest != null)
        {
            long value = nearest.offeringValue();

            if (inRange(player, ModConstants.altarRadiusFor(value)) && (centre == null || value >= best))
            {
                best = value;
                centre = Vec3.atCenterOf(nearest.getBlockPos());
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

                if (player.distanceToSqr(other) <= (double) radius * (double) radius
                        && (centre == null ? value >= 0L : value > best))
                {
                    best = value;
                    centre = other.position();
                }
            }
        }

        return centre == null ? null : new Governing(best, centre);
    }

    /**
     * The value of one governing altar together with the centre of its range.
     */
    private record Governing(long value, Vec3 centre)
    {
    }

    /**
     * Value of the nearest altar regardless of the range, which is one of the candidates of the carried effect.
     */
    public static long nearestAltarValue(ServerPlayer player)
    {
        DevilsAltarBlockEntity altar = nearestAltar(player);
        return altar == null ? 0L : altar.offeringValue();
    }

    /**
     * The largest offering value among the altar items the player holds, the worn one included, or NO_ALTAR when the
     * inventory holds none. Every altar is a separate candidate, they are never added up.
     */
    public static long carriedAltarValue(ServerPlayer player)
    {
        Inventory inventory = player.getInventory();
        long best = NO_ALTAR;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);

            if (stack.isEmpty() || !stack.is(ModBlocks.DEVILS_ALTAR_ITEM.get()))
            {
                continue;
            }

            CompoundTag tag = stack.getTag();
            long value = tag == null ? 0L : Math.max(0L, tag.getLong(ModConstants.ALTAR_VALUE_TAG));

            if (value > best)
            {
                best = value;
            }
        }

        return best;
    }

    /**
     * The value that decides the strength of the altar effect of one player, or NO_ALTAR when no altar applies.
     * The governing range, the altars the player holds and the nearest altar of the world compete as separate
     * candidates and the strongest single one wins, so carrying several altars, wearing one on top of a range or
     * standing in a range while wearing an altar never adds up into a stronger effect.
     */
    public static long effectValue(ServerPlayer player)
    {
        long governing = governingValue(player);
        long carried = carriedAltarValue(player);

        /*
         * Only a governing range or an altar held by the player makes the effect apply at all; the nearest altar of
         * the world is a level candidate alone, exactly as it was for the carried effect before.
         */
        if (governing < 0L && carried < 0L)
        {
            return NO_ALTAR;
        }

        return Math.max(Math.max(governing, carried), nearestAltarValue(player));
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
     * Which kind of altar already covers the position, ignoring the altar at the excluded position.
     * Both placed altars and altars worn by players are sources, which is what keeps two ranges apart, and the kind is
     * reported as well, because a refused placement explains where the covering range comes from.
     */
    public enum RangeSource
    {
        NONE,
        PLACED,
        WORN,
        BOTH;

        public boolean covers()
        {
            return this != NONE;
        }
    }

    /**
     * The covering range of a position, or NONE when no other altar range reaches it.
     */
    public static RangeSource rangeSource(ServerLevel level, BlockPos pos, BlockPos excluded)
    {
        boolean placed = placedRangeCovers(level, pos, excluded);
        boolean worn = wornRangeCovers(level, pos);

        if (placed && worn)
        {
            return RangeSource.BOTH;
        }

        if (placed)
        {
            return RangeSource.PLACED;
        }

        return worn ? RangeSource.WORN : RangeSource.NONE;
    }

    /**
     * True when a placed altar other than the excluded one covers the position.
     */
    private static boolean placedRangeCovers(ServerLevel level, BlockPos pos, BlockPos excluded)
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

        return false;
    }

    /**
     * True when an altar worn by a player covers the position.
     */
    private static boolean wornRangeCovers(ServerLevel level, BlockPos pos)
    {
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
     * The altar grants the vanilla damage boost, whose level follows the strongest single altar that applies to the
     * player and never the sum of several, while the duration tells whether the player stands in a range.
     * The effect is refreshed once per interval and overwritten while it applies, so it simply expires afterwards.
     */
    private static void applyRangeEffect(ServerPlayer player)
    {
        long value = effectValue(player);

        if (value < 0L)
        {
            return;
        }

        /*
         * Standing in a range, or wearing an altar which is a range at the wearer, keeps the full duration;
         * only carrying an altar keeps half of it. The level always comes from the strongest single candidate.
         */
        boolean governed = governingValue(player) >= 0L;
        int duration = governed ? ModConstants.ALTAR_EFFECT_DURATION_TICKS
                : ModConstants.ALTAR_EFFECT_DURATION_TICKS / ModConstants.ALTAR_CARRIED_EFFECT_DIVISOR;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration,
                ModConstants.altarLevelFor(value)));
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
        NotifySender.sendChatThrottled(player, KEY_INTRUSION);
    }
}
