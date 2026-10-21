/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.luck;

import com.devilbonegambler.data.LuckData;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.LuckSyncPacket;
import com.devilbonegambler.registry.ModEffects;
import com.devilbonegambler.util.ModConstants;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The luck lock of 幸运之子.
 *
 * Locking remembers the value the player had, writes the ceiling and pushes it to the client. Releasing writes the
 * remembered value back. The lock lives in memory only, like the behaviour variable, and the periodic update keeps its
 * cadence while a lock is held: it only stops writing a new value.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LuckLockService
{
    private static final Map<UUID, Integer> RESTORE = new ConcurrentHashMap<UUID, Integer>();

    private LuckLockService()
    {
    }

    public static boolean isLocked(ServerPlayer player)
    {
        return player != null && RESTORE.containsKey(player.getUUID());
    }

    /**
     * The value a lock holds, which is the ceiling of the scale.
     */
    public static int lockedValue()
    {
        return ModConstants.LUCK_LOCKED_VALUE;
    }

    /**
     * Remembers the current value and moves the player to the ceiling. Locking twice keeps the first remembered value.
     */
    public static void lock(ServerPlayer player)
    {
        MinecraftServer server = player == null ? null : player.getServer();

        if (server == null)
        {
            return;
        }

        LuckData data = LuckData.get(server);
        int current = data.ensure(player.getUUID(), LuckService.nowTick(server)).value();
        RESTORE.putIfAbsent(player.getUUID(), Integer.valueOf(current));
        data.setLuck(player.getUUID(), lockedValue());
        ModNetwork.sendToPlayer(player, new LuckSyncPacket(lockedValue()));
    }

    /**
     * Writes the remembered value back and pushes it. A player without a remembered value is left as it is.
     */
    public static void release(ServerPlayer player)
    {
        Integer previous = player == null ? null : RESTORE.remove(player.getUUID());
        MinecraftServer server = player == null ? null : player.getServer();

        if (previous == null || server == null)
        {
            return;
        }

        LuckData.get(server).setLuck(player.getUUID(), previous.intValue());
        ModNetwork.sendToPlayer(player, new LuckSyncPacket(previous.intValue()));
    }

    /**
     * The effect ran out: the pre meal value comes back.
     */
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event)
    {
        handleEffectEnd(event);
    }

    /**
     * The effect was removed early, by milk or a command: the pre meal value comes back as well.
     */
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event)
    {
        handleEffectEnd(event);
    }

    private static void handleEffectEnd(MobEffectEvent event)
    {
        MobEffectInstance instance = event.getEffectInstance();

        if (instance != null && instance.getEffect() == ModEffects.LUCK_CHILD.get()
                && event.getEntity() instanceof ServerPlayer player)
        {
            release(player);
        }
    }

    /**
     * A lock never outlives the session, so a player who leaves while the effect runs gets the stored value back on the
     * next join and the effect, if it is still there, locks again on top of it.
     */
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        RESTORE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player && player.hasEffect(ModEffects.LUCK_CHILD.get()))
        {
            lock(player);
        }
    }
}
