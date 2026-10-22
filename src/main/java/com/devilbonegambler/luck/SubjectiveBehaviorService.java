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
    public static final int BEHAVIOR_OFFERING = 3;
    public static final int BEHAVIOR_WAGER_JOIN = 4;
    public static final int BEHAVIOR_WAGER_LOSS = 5;
    public static final int BEHAVIOR_KILL_HOSTILE = 6;
    public static final int BEHAVIOR_KILL_INNOCENT = 7;
    public static final int BEHAVIOR_EATING = 8;
    public static final int BEHAVIOR_DICE_USE = 9;
    public static final int BEHAVIOR_LEVEL_UP = 10;
    public static final int BEHAVIOR_MANUAL_CONVERSION = 11;

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

    public static void recordOffering(ServerPlayer player)
    {
        record(player, BEHAVIOR_OFFERING);
    }

    public static void recordWagerJoin(ServerPlayer player)
    {
        record(player, BEHAVIOR_WAGER_JOIN);
    }

    public static void recordWagerLoss(ServerPlayer player)
    {
        record(player, BEHAVIOR_WAGER_LOSS);
    }

    /**
     * A kill of a hostile mob counts as a good deed, every other victim, another player included, counts as a bad one.
     */
    /**
     * A meal that restores hunger counts once, no matter how many hunger points it restores.
     */
    public static void recordEating(ServerPlayer player)
    {
        record(player, BEHAVIOR_EATING);
    }

    /**
     * Rerolling a die is a behaviour of its own. The charge is paid when the request is accepted, so a refused request
     * never counts.
     */
    public static void recordDiceUse(ServerPlayer player)
    {
        record(player, BEHAVIOR_DICE_USE);
    }

    /**
     * Gaining an experience level counts as a behaviour of its own, once per round like every other action.
     */
    public static void recordLevelUp(ServerPlayer player)
    {
        record(player, BEHAVIOR_LEVEL_UP);
    }

    /**
     * A pile of gameplay manuals that the altar turned into dice counts once for the player standing next to it.
     */
    public static void recordManualConversion(ServerPlayer player)
    {
        record(player, BEHAVIOR_MANUAL_CONVERSION);
    }

    /**
     * The result of one roll moves the next increment: a red roll costs one point for every two points rolled and a
     * black roll pays the same, so a bad result drags the next update down and a good one lifts it. Unlike the
     * behaviours, results do not share the one-per-round trigger: up to the configured number of rolls per round is
     * summed, so several rolls inside one round add up instead of only the first one counting.
     */
    public static void recordDiceResult(ServerPlayer player, boolean red, int point)
    {
        if (player == null)
        {
            return;
        }

        int steps = point / ModConstants.DICE_RESULT_POINT_DIVISOR;

        if (steps <= 0)
        {
            return;
        }

        State state = STATES.computeIfAbsent(player.getUUID(), key -> new State());

        if (state.resultCount >= ModConstants.DICE_RESULT_MAX_COUNT)
        {
            return;
        }

        state.resultCount++;
        int signed = (red ? ModConstants.DICE_RESULT_RED_SIGN : ModConstants.DICE_RESULT_BLACK_SIGN) * steps;
        state.value = Mth.clamp(state.value + signed, -ModConstants.SUBJECTIVE_BEHAVIOR_LIMIT,
                ModConstants.SUBJECTIVE_BEHAVIOR_LIMIT);
        ModNetwork.sendToPlayer(player, new BehaviorStatePacket(true, state.value));
    }

    public static void recordKill(ServerPlayer player, boolean hostile)
    {
        record(player, hostile ? BEHAVIOR_KILL_HOSTILE : BEHAVIOR_KILL_INNOCENT);
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

        if (behavior == BEHAVIOR_OFFERING)
        {
            return ModConstants.OFFERING_BEHAVIOR_DELTA;
        }

        if (behavior == BEHAVIOR_WAGER_JOIN)
        {
            return ModConstants.WAGER_JOIN_BEHAVIOR_DELTA;
        }

        if (behavior == BEHAVIOR_WAGER_LOSS)
        {
            return ModConstants.WAGER_LOSS_BEHAVIOR_DELTA;
        }

        if (behavior == BEHAVIOR_KILL_HOSTILE)
        {
            return ModConstants.KILL_HOSTILE_BEHAVIOR_DELTA;
        }

        if (behavior == BEHAVIOR_KILL_INNOCENT)
        {
            return ModConstants.KILL_INNOCENT_BEHAVIOR_DELTA;
        }

        if (behavior == BEHAVIOR_EATING)
        {
            return ModConstants.EATING_BEHAVIOR_DELTA;
        }

        if (behavior == BEHAVIOR_DICE_USE)
        {
            return ModConstants.DICE_USE_BEHAVIOR_DELTA;
        }

        if (behavior == BEHAVIOR_LEVEL_UP)
        {
            return ModConstants.LEVEL_UP_BEHAVIOR_DELTA;
        }

        if (behavior == BEHAVIOR_MANUAL_CONVERSION)
        {
            return ModConstants.MANUAL_CONVERSION_BEHAVIOR_DELTA;
        }

        return 0;
    }

    private static final class State
    {
        private int value;
        private boolean triggered;
        /* Rolls whose result already moved this round, so the result channel stops after the configured count. */
        private int resultCount;
    }
}
