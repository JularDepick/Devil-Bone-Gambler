/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network;

import com.devilbonegambler.altar.AltarService;
import com.devilbonegambler.block.DevilsAltarBlockEntity;
import com.devilbonegambler.network.packet.AltarStatusPacket;
import com.devilbonegambler.util.ModConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Pushes the governing altar status to every player, but only when it changes.
 * An altar worn in the head slot is a source like a placed altar, so the red range mask follows the wearer.
 * The client only uses it as a branch hint; the server re-checks the range before any sacrifice.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AltarStatusBroadcaster
{
    private static final int INTERVAL_TICKS = 20;

    private static final Map<UUID, Status> LAST_SENT = new HashMap<UUID, Status>();
    private static int counter = 0;

    private AltarStatusBroadcaster()
    {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        counter++;

        if (counter < INTERVAL_TICKS)
        {
            return;
        }

        counter = 0;
        MinecraftServer server = event.getServer();

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            Status status = statusOf(server, player);
            Status previous = LAST_SENT.get(player.getUUID());

            if (previous != null && previous.same(status))
            {
                continue;
            }

            LAST_SENT.put(player.getUUID(), status);
            ModNetwork.sendToPlayer(player, new AltarStatusPacket(status.hasAltar, status.pos, status.value,
                    status.radius, status.inRange));
        }
    }

    /**
     * The altar that governs the player: the altar they wear, a stronger altar range that covers them, or the
     * nearest placed altar as a hint when no range covers them.
     */
    private static Status statusOf(MinecraftServer server, ServerPlayer player)
    {
        long wornValue = AltarService.wornAltarValue(player);
        long best = wornValue;
        BlockPos bestPos = wornValue < 0L ? null : player.blockPosition();

        for (ServerPlayer other : server.getPlayerList().getPlayers())
        {
            if (other == player)
            {
                continue;
            }

            long value = AltarService.wornAltarValue(other);

            if (value < 0L || value <= best)
            {
                continue;
            }

            int radius = ModConstants.altarRadiusFor(value);

            if (player.distanceToSqr(other) <= (double) radius * (double) radius)
            {
                best = value;
                bestPos = other.blockPosition();
            }
        }

        DevilsAltarBlockEntity nearest = AltarService.nearestAltar(player);

        if (nearest != null)
        {
            long value = nearest.offeringValue();
            int radius = ModConstants.altarRadiusFor(value);

            if (value > best && player.blockPosition().distSqr(nearest.getBlockPos()) <= (double) radius * radius)
            {
                best = value;
                bestPos = nearest.getBlockPos();
            }
        }

        if (bestPos != null)
        {
            return new Status(true, bestPos, best, ModConstants.altarRadiusFor(best), true);
        }

        if (nearest != null)
        {
            long value = nearest.offeringValue();
            return new Status(true, nearest.getBlockPos(), value, ModConstants.altarRadiusFor(value), false);
        }

        return new Status(false, BlockPos.ZERO, 0, ModConstants.ALTAR_BASE_RADIUS, false);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        LAST_SENT.clear();
        counter = 0;
    }

    private static final class Status
    {
        private final boolean hasAltar;
        private final BlockPos pos;
        private final long value;
        private final int radius;
        private final boolean inRange;

        private Status(boolean hasAltar, BlockPos pos, long value, int radius, boolean inRange)
        {
            this.hasAltar = hasAltar;
            this.pos = pos;
            this.value = value;
            this.radius = radius;
            this.inRange = inRange;
        }

        private boolean same(Status other)
        {
            return this.hasAltar == other.hasAltar && this.value == other.value && this.radius == other.radius
                    && this.inRange == other.inRange && this.pos.equals(other.pos);
        }
    }
}
