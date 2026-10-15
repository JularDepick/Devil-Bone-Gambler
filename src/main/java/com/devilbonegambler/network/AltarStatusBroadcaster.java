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
 * Pushes the nearest altar status to every player, but only when it changes.
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
            DevilsAltarBlockEntity altar = AltarService.nearestAltar(player);
            Status status;

            if (altar == null)
            {
                status = new Status(false, BlockPos.ZERO, 0, ModConstants.ALTAR_BASE_RADIUS, false);
            }
            else
            {
                long value = altar.offeringValue();
                int radius = ModConstants.altarRadiusFor(value);
                BlockPos pos = altar.getBlockPos();
                boolean inRange = player.blockPosition().distSqr(pos) <= (double) radius * radius;
                status = new Status(true, pos, value, radius, inRange);
            }

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
