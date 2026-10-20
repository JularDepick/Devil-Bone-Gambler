/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.luck;

import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.BehaviorStatePacket;
import com.devilbonegambler.util.ModConstants;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Subjective behaviour variable of one luck round.
 *
 * A behaviour changes the variable by a fixed delta and marks the round as triggered. Every behaviour shares the
 * trigger, so a round accepts at most one behaviour no matter how many actions happen. The value is clamped into
 * [-10, 10], sent to the client as a mirror, and consumed and reset by the next periodic luck update.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SubjectiveBehaviorService
{
    public static final int BEHAVIOR_ATTACK_DEALER = 1;
    public static final int BEHAVIOR_DEALER_PICKUP_DICE = 2;

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<UUID, State>();

    private SubjectiveBehaviorService()
    {
    }

    public static void recordAttackDealer(ServerPlayer player)
    {
        record(player, BEHAVIOR_ATTACK_DEALER);
    }

    /**
     * A dealer picked up dice that came from this player: every die adds its fixed delta, but the whole pickup still
     * shares the one trigger of the round with every other behaviour.
     */
    public static void recordDealerPickup(ServerPlayer player, int dice)
    {
        recordAmount(player, BEHAVIOR_DEALER_PICKUP_DICE, dice);
    }

    /**
     * Applies one behaviour if this round has not triggered yet and sends the mirror to the player.
     */
    public static void record(ServerPlayer player, int behavior)
    {
        recordAmount(player, behavior, 1);
    }

    private static void recordAmount(ServerPlayer player, int behavior, int amount)
    {
        if (player == null || amount <= 0)
        {
            return;
        }

        int delta = deltaOf(behavior);

        if (delta == 0)
        {
            return;
        }

        State state = STATES.computeIfAbsent(player.getUUID(), key -> new State());

        if (state.triggered)
        {
            return;
        }

        state.triggered = true;
        state.value = Mth.clamp(state.value + delta * amount, -ModConstants.SUBJECTIVE_BEHAVIOR_LIMIT,
                ModConstants.SUBJECTIVE_BEHAVIOR_LIMIT);
        ModNetwork.sendToPlayer(player, new BehaviorStatePacket(true, state.value));
    }

    /**
     * The variable of the current round without releasing it, so the update can use it first.
     */
    public static int value(ServerPlayer player)
    {
        if (player == null)
        {
            return 0;
        }

        State state = STATES.get(player.getUUID());
        return state == null ? 0 : state.value;
    }

    /**
     * Releases the trigger and resets the variable after the update, so the next round starts empty.
     */
    public static void finishRound(ServerPlayer player)
    {
        if (player == null)
        {
            return;
        }

        STATES.remove(player.getUUID());
        ModNetwork.sendToPlayer(player, new BehaviorStatePacket(false, 0));
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        STATES.remove(event.getEntity().getUUID());
    }

    private static int deltaOf(int behavior)
    {
        if (behavior == BEHAVIOR_ATTACK_DEALER)
        {
            return ModConstants.ATTACK_DEALER_BEHAVIOR_DELTA;
        }

        if (behavior == BEHAVIOR_DEALER_PICKUP_DICE)
        {
            return ModConstants.DEALER_PICKUP_BEHAVIOR_DELTA;
        }

        return 0;
    }

    private static final class State
    {
        private int value;
        private boolean triggered;
    }
}
