/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.luck;

import com.devilbonegambler.altar.AltarService;
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
 * A player receives the phase origin and the base value on the first join. Every update interval the increment is the
 * hidden fate value, read from the game ticks since that origin, plus a bounded random term, seeded from the ticks, the
 * player identity, the origin, the current value and the subjective behaviour variable, plus that behaviour variable
 * itself. The increment is capped at the configured limit, the result is clamped into 0..100, the behaviour round is
 * reset, and the value is pushed to that player for the corner display.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LuckService
{
    private static int countdown;
    /* The quick pull runs on its own counter, so the two cadences never share a phase. */
    private static int driftCountdown;

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

        if (server == null)
        {
            return;
        }

        if (++driftCountdown >= ModConstants.LUCK_DRIFT_INTERVAL_TICKS)
        {
            driftCountdown = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers())
            {
                drift(player);
            }
        }

        if (++countdown < ModConstants.LUCK_UPDATE_INTERVAL_TICKS)
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

        long ticks = nowTick(server);
        LuckData data = LuckData.get(server);
        data.ensureCreated(System.currentTimeMillis());
        LuckData.Entry entry = data.ensure(player.getUUID(), ticks);
        ModNetwork.sendToPlayer(player, new LuckSyncPacket(entry.value()));
    }

    private static void advance(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return;
        }

        long now = nowTick(server);
        LuckData data = LuckData.get(server);
        long origin = data.ensureCreated(System.currentTimeMillis());
        LuckData.Entry entry = data.ensure(player.getUUID(), now);
        long birth = entry.birth();
        int current = entry.value();
        int behavior = SubjectiveBehaviorService.value(player);
        double fate = FateModel.value(birth, now, phaseSeed(origin, player, current));
        int random = new Random(seed(now, player.getUUID(), birth, current, behavior))
                .nextInt(ModConstants.LUCK_INCREMENT_RANDOM_SPAN * 2 + 1)
                - ModConstants.LUCK_INCREMENT_RANDOM_SPAN;
        if (LuckLockService.isLocked(player))
        {
            /*
             * The lock holds the value at the ceiling. The periodic pass still runs on its own cadence and still closes
             * the behaviour round, so the update rhythm is untouched; only the write is skipped.
             */
            SubjectiveBehaviorService.finishRound(player);
            ModNetwork.sendToPlayer(player, new LuckSyncPacket(LuckLockService.lockedValue()));
            return;
        }

        int increment = Mth.clamp((int) Math.round(fate + random + behavior + preferenceOf(current)),
                -ModConstants.LUCK_INCREMENT_LIMIT, ModConstants.LUCK_INCREMENT_LIMIT);
        int next = Mth.clamp(current + increment, ModConstants.MIN_LUCK, ModConstants.MAX_LUCK);

        if (next != current)
        {
            data.setLuck(player.getUUID(), next);
        }

        SubjectiveBehaviorService.finishRound(player);
        ModNetwork.sendToPlayer(player, new LuckSyncPacket(next));
    }

    /**
     * The quick pull: outside the comfortable band the value steps one point towards it every drift interval, so a
     * value the hidden curve keeps pushing the wrong way still walks back instead of staying pinned. A locked value is
     * left alone, because the effect owns it for the duration.
     */
    private static void drift(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();

        if (server == null || LuckLockService.isLocked(player))
        {
            return;
        }

        LuckData data = LuckData.get(server);
        int current = data.ensure(player.getUUID(), nowTick(server)).value();
        int step = 0;

        if (current < ModConstants.LUCK_LOW_BAND)
        {
            step = ModConstants.LUCK_DRIFT_STEP;
        }
        else if (current > ModConstants.LUCK_HIGH_BAND)
        {
            step = -ModConstants.LUCK_DRIFT_STEP;
        }

        if (step == 0)
        {
            return;
        }

        int next = Mth.clamp(current + step, ModConstants.MIN_LUCK, ModConstants.MAX_LUCK);
        data.setLuck(player.getUUID(), next);
        ModNetwork.sendToPlayer(player, new LuckSyncPacket(next));
    }

    /**
     * Death does not cost the behaviour variable. The value moves a fixed step towards the comfortable band instead, so
     * a low value rises, a high value falls, and a value already inside the band stays where it is. The new value is
     * written and pushed right away, so the respawn carries it.
     */
    public static void nudgeAfterDeath(ServerPlayer player)
    {
        MinecraftServer server = player == null ? null : player.getServer();

        if (server == null)
        {
            return;
        }

        LuckData data = LuckData.get(server);
        int current = data.ensure(player.getUUID(), nowTick(server)).value();
        int step = deathStepOf(current);

        if (step == 0)
        {
            return;
        }

        int next = Mth.clamp(current + step, ModConstants.MIN_LUCK, ModConstants.MAX_LUCK);
        data.setLuck(player.getUUID(), next);
        ModNetwork.sendToPlayer(player, new LuckSyncPacket(next));
    }

    /**
     * The death step points at the comfortable band and never crosses its nearer edge.
     */
    private static int deathStepOf(int luck)
    {
        if (luck < ModConstants.LUCK_LOW_BAND)
        {
            return Math.min(ModConstants.LUCK_DEATH_STEP, ModConstants.LUCK_LOW_BAND - luck);
        }

        if (luck > ModConstants.LUCK_HIGH_BAND)
        {
            return -Math.min(ModConstants.LUCK_DEATH_STEP, luck - ModConstants.LUCK_HIGH_BAND);
        }

        return 0;
    }

    /**
     * The banded preference of one update: a low value is pulled up, a high value is pulled down and the middle band
     * stays neutral. The pull is added like the behaviour variable, so it shifts the average without overriding the
     * fate value or the random term, and a value inside the middle band keeps the plain model.
     */
    private static int preferenceOf(int luck)
    {
        if (luck < ModConstants.LUCK_LOW_BAND)
        {
            return ModConstants.LUCK_LOW_DRIFT;
        }

        if (luck > ModConstants.LUCK_HIGH_BAND)
        {
            return ModConstants.LUCK_HIGH_DRIFT;
        }

        return 0;
    }

    /**
     * The overworld game time is the only clock the model reads: it is saved with the level, keeps counting while the
     * server runs and is never rewound by the day time command.
     */
    public static long nowTick(MinecraftServer server)
    {
        return server.overworld().getGameTime();
    }

    /**
     * The phase seed mixes the save creation instant with the player identity and the coarse player state, so two
     * players in one save run different curves and a single curve shifts as that state changes.
     */
    private static long phaseSeed(long origin, ServerPlayer player, int luck)
    {
        long seed = origin;
        seed = seed * 1000003L + player.getUUID().getMostSignificantBits();
        seed = seed * 1000003L + player.getUUID().getLeastSignificantBits();
        seed = seed * 1000003L + stateOf(player, luck);
        return seed;
    }

    /**
     * The coarse player state: half hearts, food points, dimension, chunk coordinates, whether an altar governs the
     * player and the luck value in tens. Finer gradations are left out so the phase cannot jitter every tick.
     */
    private static long stateOf(ServerPlayer player, int luck)
    {
        long health = Math.round(player.getHealth() * 2.0F);
        long food = player.getFoodData().getFoodLevel();
        long dimension = player.level().dimension().location().hashCode();
        long chunkX = player.blockPosition().getX() >> 4;
        long chunkZ = player.blockPosition().getZ() >> 4;
        long altar = AltarService.governingValue(player) >= 0L ? 1L : 0L;
        long luckTens = luck / 10;

        long seed = health;
        seed = seed * 31L + food;
        seed = seed * 131L + dimension;
        seed = seed * 977L + chunkX;
        seed = seed * 1013L + chunkZ;
        seed = seed * 4099L + altar;
        seed = seed * 6151L + luckTens;
        return seed;
    }

    /**
     * The random term is fixed by the game tick, the player identity, the phase origin, the current value and the
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
