/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.luck;

import com.devilbonegambler.data.LuckData;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.LuckSyncPacket;
import com.devilbonegambler.util.ModConstants;
import java.util.Random;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps the luck value of every online player moving.
 *
 * A player receives the birth instant and the base value on the first join. Every update interval the increment is the
 * hidden fate value plus a bounded random term, seeded from the instant, the player identity, the current value and the
 * subjective behaviour variable, plus that behaviour variable itself. The result is clamped into 0..100, the behaviour
 * round is reset, and the value is pushed to that player for the corner display.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LuckService
{
    private static int countdown;

    private LuckService()
    {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            ensureAndSync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            ensureAndSync(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        MinecraftServer server = event.getServer();

        if (server == null || (++countdown < ModConstants.LUCK_UPDATE_INTERVAL_TICKS))
        {
            return;
        }

        countdown = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            advance(player);
        }
    }

    private static void ensureAndSync(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return;
        }

        long now = System.currentTimeMillis();
        LuckData data = LuckData.get(server);
        LuckData.Entry entry = data.ensure(player.getUUID(), now);
        ModNetwork.sendToPlayer(player, new LuckSyncPacket(entry.value()));
    }

    private static void advance(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return;
        }

        long now = System.currentTimeMillis();
        LuckData data = LuckData.get(server);
        LuckData.Entry entry = data.ensure(player.getUUID(), now);
        long birth = entry.birth();
        int current = entry.value();
        int behavior = SubjectiveBehaviorService.value(player);
        double fate = FateModel.value(birth, now);
        int random = new Random(seed(now, player.getUUID(), birth, current, behavior))
                .nextInt(ModConstants.LUCK_INCREMENT_RANDOM_SPAN * 2 + 1)
                - ModConstants.LUCK_INCREMENT_RANDOM_SPAN;
        int next = Mth.clamp(current + (int) Math.round(fate + random + behavior),
                ModConstants.MIN_LUCK, ModConstants.MAX_LUCK);

        if (next != current)
        {
            data.setLuck(player.getUUID(), next);
        }

        SubjectiveBehaviorService.finishRound(player);
        ModNetwork.sendToPlayer(player, new LuckSyncPacket(next));
    }

    /**
     * The random term is fixed by the instant, the player identity, the birth instant, the current value and the
     * behaviour variable, so a replay of the same state cannot quietly reroll a different meal.
     */
    private static long seed(long now, UUID playerId, long birth, int current, int behavior)
    {
        long seed = now;
        seed = seed * 1000003L + playerId.getMostSignificantBits();
        seed = seed * 1000003L + playerId.getLeastSignificantBits();
        seed = seed * 1000003L + birth;
        seed = seed * 1000003L + current;
        seed = seed * 1000003L + behavior;
        return seed;
    }
}
