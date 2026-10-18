/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.wager;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.commands.CommandRunner;
import com.devilbonegambler.config.PrizeEntry;
import com.devilbonegambler.config.PrizePoolConfig;
import com.devilbonegambler.config.PrizeRegion;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.data.LuckData;
import com.devilbonegambler.draw.DiceRoll;
import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.draw.RegionSelector;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.Placeholders;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server authoritative wager service: one zone around the initiator, a countdown, the sneak based joining and the
 * settlement that picks the players by the dice point and their luck value.
 * Every text goes through the language file, every command runs with the draw permission level.
 * The dice itself is never deducted here, the caller owns the consumption.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WagerService
{
    private static final String LANG_PREFIX = "gui." + ModConstants.MOD_ID + ".wager.";
    private static final String KEY_NO_PLAYER = LANG_PREFIX + "no_player";
    private static final String KEY_CREATED = LANG_PREFIX + "created";
    private static final String KEY_COUNTDOWN = LANG_PREFIX + "countdown";
    private static final String KEY_JOINED = LANG_PREFIX + "joined";
    private static final String KEY_LEFT = LANG_PREFIX + "left";
    private static final String KEY_YOU_LEFT = LANG_PREFIX + "you_left";
    private static final String KEY_OUT_OF_RANGE = LANG_PREFIX + "out_of_range";
    private static final String KEY_ENTERED = LANG_PREFIX + "entered";
    private static final String KEY_STARTED = LANG_PREFIX + "started";
    private static final String KEY_POINT = LANG_PREFIX + "point";
    private static final String KEY_SELECTED = LANG_PREFIX + "selected";
    private static final String KEY_YOU_SELECTED = LANG_PREFIX + "you_selected";
    private static final String KEY_YOU_SELECTED_MULTI = LANG_PREFIX + "you_selected_multi";
    private static final String KEY_NO_CANDIDATE = LANG_PREFIX + "no_candidate";
    private static final String KEY_FINISHED = LANG_PREFIX + "finished";
    private static final String KEY_ABORTED = LANG_PREFIX + "aborted";

    /*
     * Design detail: the joined list index and the dice point are both reduced modulo six before comparison.
     */
    private static final int SELECT_MODULUS = 6;
    private static final int TICKS_PER_SECOND = 20;
    private static final String NAME_SEPARATOR = "\u3001";
    private static final String TEXT_SEPARATOR = " ";

    private static final Map<UUID, PlayerState> STATES = new HashMap<UUID, PlayerState>();
    private static WagerSession session;

    private WagerService()
    {
    }

    /**
     * True when at least one other player stands inside the sphere around the given player.
     */
    public static boolean hasOtherPlayersNearby(ServerPlayer player)
    {
        if (player == null)
        {
            return false;
        }

        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return false;
        }

        WagerArea area = WagerArea.around(player);
        int count = 0;

        for (ServerPlayer other : server.getPlayerList().getPlayers())
        {
            if (other == player || !sameLevel(player, other))
            {
                continue;
            }

            if (area.contains(other))
            {
                count++;
            }
        }

        return count >= ModConstants.WAGER_MIN_PLAYERS;
    }

    /**
     * Validates the surroundings, creates the zone and the wager, shuffles the other members, makes the initiator a
     * member right away, broadcasts the creation notice and remembers the start tick. Returns false without any side
     * effect when the validation fails.
     */
    public static boolean startWager(ServerPlayer player)
    {
        if (player == null)
        {
            return false;
        }

        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return false;
        }

        if (session != null)
        {
            DevilBoneGambler.LOGGER.info("Rejected a wager request, another wager is still running, player={}",
                    player.getGameProfile().getName());
            return false;
        }

        if (!hasOtherPlayersNearby(player))
        {
            NotifySender.sendChatThrottled(player, KEY_NO_PLAYER);
            return false;
        }

        WagerArea area = WagerArea.around(player);
        List<ServerPlayer> inside = playersInArea(player, area);
        List<UUID> others = new ArrayList<UUID>();

        for (ServerPlayer target : inside)
        {
            if (!target.getUUID().equals(player.getUUID()))
            {
                others.add(target.getUUID());
            }
        }

        Collections.shuffle(others);

        List<UUID> areaPlayers = new ArrayList<UUID>();
        areaPlayers.add(player.getUUID());
        areaPlayers.addAll(others);

        WagerSession created = new WagerSession(player.getUUID(), area, areaPlayers, server.getTickCount(),
                ModConstants.WAGER_COUNTDOWN_SECONDS);

        /*
         * The initiator is a member from the start, so the countdown sync and the gambler's dice lock cover the
         * initiator without a second crouch confirmation. No join announcement is emitted for this implicit join.
         */
        created.join(player.getUUID());
        created.markNotified(player.getUUID());

        Component notice = Component.translatable(KEY_CREATED, player.getGameProfile().getName(),
                Integer.valueOf(ModConstants.WAGER_COUNTDOWN_SECONDS));

        for (ServerPlayer target : inside)
        {
            created.markNotified(target.getUUID());
            target.sendSystemMessage(notice);
        }

        session = created;
        STATES.clear();

        for (ServerPlayer target : server.getPlayerList().getPlayers())
        {
            PlayerState state = new PlayerState();
            state.inside = sameLevel(player, target) && area.contains(target);
            state.sneaking = target.isShiftKeyDown();
            STATES.put(target.getUUID(), state);
        }

        DevilBoneGambler.LOGGER.info("Wager started, owner={} zonePlayers={} countdown={}s",
                player.getGameProfile().getName(), Integer.valueOf(inside.size()),
                Integer.valueOf(ModConstants.WAGER_COUNTDOWN_SECONDS));
        return true;
    }

    /**
     * True when the player takes part in the running wager, either as a zone member, as a joined member or by standing
     * inside the zone.
     */
    public static boolean isInWager(ServerPlayer player)
    {
        return sessionOf(player) != null;
    }

    /**
     * True when the player may not use the gambler's dice right now, which is exactly the joined membership of the
     * running wager and therefore includes the initiator.
     * Leaving the wager and the wager ending both drop the membership, so both release the lock.
     */
    public static boolean isGamblersDiceLocked(ServerPlayer player)
    {
        WagerSession active = session;

        if (active == null || player == null)
        {
            return false;
        }

        return active.isJoined(player.getUUID());
    }

    /**
     * The running wager the player belongs to, or null when the player has nothing to do with it.
     */
    public static WagerSession sessionOf(ServerPlayer player)
    {
        WagerSession active = session;

        if (active == null || player == null)
        {
            return null;
        }

        UUID playerId = player.getUUID();

        if (active.isJoined(playerId) || active.isInAreaList(playerId))
        {
            return active;
        }

        ServerPlayer owner = ownerOf(player.getServer(), active);

        if (sameLevel(owner, player) && active.area().contains(player))
        {
            return active;
        }

        return null;
    }

    /**
     * Drops the running wager and every per player tick state.
     */
    public static void clear()
    {
        session = null;
        STATES.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        WagerSession active = session;

        if (active == null)
        {
            return;
        }

        MinecraftServer server = event.getServer();
        ServerPlayer owner = ownerOf(server, active);

        if (owner == null || !owner.isAlive() || owner.isDeadOrDying())
        {
            abort(server, active);
            return;
        }

        active.setArea(active.area().refresh(owner));
        long now = server.getTickCount();

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            tickPlayer(active, owner, player, now);
        }

        if (session != active)
        {
            return;
        }

        tickCountdown(server, active, owner, now);
    }

    /**
     * One player, one tick: presence transitions drive the join and leave flow, the sneak edge drives the double crouch.
     */
    private static void tickPlayer(WagerSession active, ServerPlayer owner, ServerPlayer player, long now)
    {
        PlayerState state = STATES.get(player.getUUID());

        if (state == null)
        {
            state = new PlayerState();
            state.sneaking = player.isShiftKeyDown();
            STATES.put(player.getUUID(), state);
        }

        boolean inside = sameLevel(owner, player) && active.area().contains(player);

        if (inside != state.inside)
        {
            state.inside = inside;

            if (inside)
            {
                onEnter(active, owner, player, now);
            }
            else
            {
                onLeave(active, owner, player);
            }
        }

        boolean sneaking = player.isShiftKeyDown();

        if (sneaking != state.sneaking)
        {
            state.sneaking = sneaking;

            if (sneaking)
            {
                onCrouch(active, owner, player, state, now);
            }
        }
    }

    /**
     * A player walked into the zone, so the creation notice is sent again with the remaining seconds.
     */
    private static void onEnter(WagerSession active, ServerPlayer owner, ServerPlayer player, long now)
    {
        if (active.phase() != WagerSession.Phase.COUNTDOWN)
        {
            return;
        }

        active.markNotified(player.getUUID());
        NotifySender.sendChatThrottled(player, KEY_ENTERED, owner.getGameProfile().getName(),
                Integer.valueOf(remainingSeconds(active, now)));
    }

    /**
     * A joined player walked out of the zone, which counts as leaving the wager. Other players stay silent.
     */
    private static void onLeave(WagerSession active, ServerPlayer owner, ServerPlayer player)
    {
        if (!active.isJoined(player.getUUID()))
        {
            return;
        }

        active.leave(player.getUUID());
        broadcastToJoined(player.getServer(), active,
                Component.translatable(KEY_LEFT, player.getGameProfile().getName()));
        NotifySender.sendChat(player, KEY_YOU_LEFT, owner.getGameProfile().getName());
    }

    /**
     * Sneak toggle machine: two crouch downs inside the window count as one double crouch, and the state machine only
     * registers a crouch down after a complete stand up, so the middle stand up is mandatory.
     */
    private static void onCrouch(WagerSession active, ServerPlayer owner, ServerPlayer player, PlayerState state,
            long now)
    {
        boolean second = state.lastCrouchTick >= 0L
                && now - state.lastCrouchTick <= ModConstants.WAGER_SNEAK_WINDOW_TICKS;
        state.lastCrouchTick = second ? -1L : now;

        if (!second)
        {
            return;
        }

        if (!state.inside)
        {
            String ownerName = owner.getGameProfile().getName();
            NotifySender.sendChatThrottled(player, KEY_OUT_OF_RANGE, ownerName, ownerName);
            return;
        }

        if (active.phase() != WagerSession.Phase.COUNTDOWN || active.isJoined(player.getUUID()))
        {
            return;
        }

        joinWager(player.getServer(), active, player);
    }

    private static void joinWager(MinecraftServer server, WagerSession active, ServerPlayer player)
    {
        active.join(player.getUUID());
        broadcastToJoinedExcept(server, active, player.getUUID(),
                Component.translatable(KEY_JOINED, player.getGameProfile().getName()));
    }

    /**
     * One countdown sync per second, and the settlement once the remaining seconds reach zero.
     */
    private static void tickCountdown(MinecraftServer server, WagerSession active, ServerPlayer owner, long now)
    {
        int remaining = remainingSeconds(active, now);

        if (remaining == active.lastCountdownSecond())
        {
            return;
        }

        active.setLastCountdownSecond(remaining);

        if (remaining > 0)
        {
            broadcastToJoined(server, active, Component.translatable(KEY_COUNTDOWN,
                    owner.getGameProfile().getName(), Integer.valueOf(remaining)));
            return;
        }

        settle(server, active, owner);
    }

    /**
     * The dice are rolled with the average luck of the joined members, the entry is pre drawn from the region the
     * point lands on, the candidates are taken by the index rule and the best or worst luck among them is executed.
     */
    private static void settle(MinecraftServer server, WagerSession active, ServerPlayer owner)
    {
        active.setPhase(WagerSession.Phase.RUNNING);
        broadcastToJoined(server, active, Component.translatable(KEY_STARTED));

        LuckData luckData = LuckData.get(server);
        RandomSource random = RandomSource.create();
        DiceRoll roll = DiceRoll.roll(random, averageLuck(active, luckData), 0L);
        broadcastToJoined(server, active, Component.translatable(KEY_POINT, Integer.valueOf(roll.point())));

        PrizeEntry entry = pickEntry(random, roll);
        List<ServerPlayer> candidates = candidatesOf(server, active, roll.point());

        if (entry == null || candidates.isEmpty())
        {
            broadcastToAudience(server, active, Component.translatable(KEY_NO_CANDIDATE));
            finish(server, active, owner);
            return;
        }

        List<ServerPlayer> selected = selectByLuck(candidates, entry, luckData);

        for (ServerPlayer target : selected)
        {
            executeEntry(server, target, entry);
            NotifySender.sendEntryResult(target, entry.notifyOrDefault());

            if (entry.isMajorEvent())
            {
                NotifySender.broadcastMajorEvent(target, entry);
            }
        }

        broadcastSelection(server, active, entry, selected);
        DevilBoneGambler.LOGGER.info("Wager settled, owner={} point={} entry={} candidates={} selected={}",
                owner.getGameProfile().getName(), Integer.valueOf(roll.point()), entry.id,
                Integer.valueOf(candidates.size()), Integer.valueOf(selected.size()));
        finish(server, active, owner);
    }

    /**
     * Region hit and entry pre draw of the wager, following the same rules as the single player draw.
     */
    private static PrizeEntry pickEntry(RandomSource random, DiceRoll roll)
    {
        PrizePoolConfig pool = ServerConfigManager.prizePool();

        if (pool == null)
        {
            return null;
        }

        List<PrizeRegion> regions = pool.sortedRegions();

        if (regions.isEmpty())
        {
            return null;
        }

        int baseTier = RegionSelector.baseTier(regions, pool.maxTier(), roll.point());
        int targetTier = RegionSelector.clampTier(baseTier, pool.minTier(), pool.maxTier());
        int overflow = RegionSelector.overflow(roll.point(), targetTier);
        PrizeRegion region = RegionSelector.select(regions, targetTier);

        if (region == null)
        {
            return null;
        }

        return RegionSelector.pick(random, region, roll.isRed(), overflow);
    }

    /**
     * Index rule: the joined list index and the point are reduced modulo six, and offline members are skipped.
     */
    private static List<ServerPlayer> candidatesOf(MinecraftServer server, WagerSession active, int point)
    {
        List<ServerPlayer> result = new ArrayList<ServerPlayer>();
        List<UUID> joined = active.joinedPlayers();
        int target = Math.floorMod(point, SELECT_MODULUS);

        for (int index = 0; index < joined.size(); index++)
        {
            if (Math.floorMod(index, SELECT_MODULUS) != target)
            {
                continue;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(joined.get(index));

            if (player != null)
            {
                result.add(player);
            }
        }

        return result;
    }

    /**
     * A red entry takes the highest luck, a black entry the lowest, and every tied player is selected.
     */
    private static List<ServerPlayer> selectByLuck(List<ServerPlayer> candidates, PrizeEntry entry, LuckData luckData)
    {
        boolean red = entry.isRed();
        int best = red ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (ServerPlayer candidate : candidates)
        {
            int luck = luckData.luckOf(candidate.getUUID());
            best = red ? Math.max(best, luck) : Math.min(best, luck);
        }

        List<ServerPlayer> result = new ArrayList<ServerPlayer>();

        for (ServerPlayer candidate : candidates)
        {
            if (luckData.luckOf(candidate.getUUID()) == best)
            {
                result.add(candidate);
            }
        }

        return result;
    }

    private static int averageLuck(WagerSession active, LuckData luckData)
    {
        List<UUID> joined = active.joinedPlayers();

        if (joined.isEmpty())
        {
            return ModConstants.BASE_LUCK;
        }

        long total = 0L;

        for (UUID playerId : joined)
        {
            total += luckData.luckOf(playerId);
        }

        return (int) (total / joined.size());
    }

    private static void executeEntry(MinecraftServer server, ServerPlayer player, PrizeEntry entry)
    {
        CommandRunner.run(server, player, entry.commandOrEmpty());
    }

    /**
     * The third person result goes to the whole zone and wager audience, the second person result to the selected ones.
     */
    private static void broadcastSelection(MinecraftServer server, WagerSession active, PrizeEntry entry,
            List<ServerPlayer> selected)
    {
        Set<UUID> selectedIds = new LinkedHashSet<UUID>();

        for (ServerPlayer player : selected)
        {
            selectedIds.add(player.getUUID());
        }

        Component thirdPerson = Component.translatable(KEY_SELECTED, nameList(selected));

        for (ServerPlayer target : audienceOf(server, active))
        {
            if (selectedIds.contains(target.getUUID()))
            {
                continue;
            }

            target.sendSystemMessage(withEntryText(thirdPerson, target, entry));
        }

        for (ServerPlayer target : selected)
        {
            List<String> others = namesExcept(selected, target);
            Component secondPerson = others.isEmpty()
                    ? Component.translatable(KEY_YOU_SELECTED)
                    : Component.translatable(KEY_YOU_SELECTED_MULTI, joinNames(others));
            target.sendSystemMessage(withEntryText(secondPerson, target, entry));
        }
    }

    /**
     * Destroys the zone and the lists, then tells every player who received a creation notice but stands outside the
     * zone that the wager has ended, without disclosing the result.
     */
    private static void finish(MinecraftServer server, WagerSession active, ServerPlayer owner)
    {
        String ownerName = owner.getGameProfile().getName();
        ServerPlayer currentOwner = ownerOf(server, active);
        List<UUID> waiting = new ArrayList<UUID>();

        for (UUID playerId : active.notified())
        {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);

            if (player == null)
            {
                continue;
            }

            boolean inside = sameLevel(currentOwner, player) && active.area().contains(player);

            if (!inside)
            {
                waiting.add(playerId);
            }
        }

        clear();

        for (UUID playerId : waiting)
        {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);

            if (player != null)
            {
                NotifySender.sendChat(player, KEY_FINISHED, ownerName);
            }
        }
    }

    /**
     * The initiator left or died, so the wager stops without a settlement and every aware player is told about it.
     */
    private static void abort(MinecraftServer server, WagerSession active)
    {
        Set<UUID> targets = new LinkedHashSet<UUID>(active.notified());
        targets.addAll(active.joinedPlayers());
        DevilBoneGambler.LOGGER.info("Wager aborted, the initiator is gone, owner={}", active.ownerId());
        clear();

        for (UUID playerId : targets)
        {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);

            if (player != null)
            {
                NotifySender.sendChat(player, KEY_ABORTED);
            }
        }
    }

    /**
     * The zone itself only knows the sphere, so the dimension is always checked by the caller.
     */
    private static List<ServerPlayer> playersInArea(ServerPlayer owner, WagerArea area)
    {
        List<ServerPlayer> result = new ArrayList<ServerPlayer>();
        MinecraftServer server = owner.getServer();

        if (server == null)
        {
            return result;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (sameLevel(owner, player) && area.contains(player))
            {
                result.add(player);
            }
        }

        return result;
    }

    /**
     * Audience of the result texts: every joined member plus every player standing inside the zone.
     */
    private static List<ServerPlayer> audienceOf(MinecraftServer server, WagerSession active)
    {
        List<ServerPlayer> result = new ArrayList<ServerPlayer>();
        ServerPlayer owner = ownerOf(server, active);

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (active.isJoined(player.getUUID()))
            {
                result.add(player);
                continue;
            }

            if (sameLevel(owner, player) && active.area().contains(player))
            {
                result.add(player);
            }
        }

        return result;
    }

    private static void broadcastToJoined(MinecraftServer server, WagerSession active, Component text)
    {
        for (UUID playerId : active.joinedPlayers())
        {
            ServerPlayer target = server.getPlayerList().getPlayer(playerId);

            if (target != null)
            {
                target.sendSystemMessage(text);
            }
        }
    }

    private static void broadcastToJoinedExcept(MinecraftServer server, WagerSession active, UUID excluded,
            Component text)
    {
        for (UUID playerId : active.joinedPlayers())
        {
            if (playerId.equals(excluded))
            {
                continue;
            }

            ServerPlayer target = server.getPlayerList().getPlayer(playerId);

            if (target != null)
            {
                target.sendSystemMessage(text);
            }
        }
    }

    private static void broadcastToAudience(MinecraftServer server, WagerSession active, Component text)
    {
        for (ServerPlayer target : audienceOf(server, active))
        {
            target.sendSystemMessage(text);
        }
    }

    private static Component withEntryText(Component prefix, ServerPlayer player, PrizeEntry entry)
    {
        Component result = NotifySender.entryText(player, entry.notifyOrDefault());
        return result == null ? prefix.copy()
                : prefix.copy().append(Component.literal(TEXT_SEPARATOR)).append(result);
    }

    private static List<String> namesExcept(List<ServerPlayer> players, ServerPlayer excluded)
    {
        List<String> result = new ArrayList<String>();

        for (ServerPlayer player : players)
        {
            if (!player.getUUID().equals(excluded.getUUID()))
            {
                result.add(player.getGameProfile().getName());
            }
        }

        return result;
    }

    private static String nameList(List<ServerPlayer> players)
    {
        List<String> names = new ArrayList<String>();

        for (ServerPlayer player : players)
        {
            names.add(player.getGameProfile().getName());
        }

        return joinNames(names);
    }

    private static String joinNames(List<String> names)
    {
        return String.join(NAME_SEPARATOR, names);
    }

    private static int remainingSeconds(WagerSession active, long now)
    {
        long elapsed = Math.max(0L, now - active.startTick());
        int remaining = ModConstants.WAGER_COUNTDOWN_SECONDS - (int) (elapsed / TICKS_PER_SECOND);
        return Math.max(0, remaining);
    }

    private static ServerPlayer ownerOf(MinecraftServer server, WagerSession active)
    {
        return server == null ? null : server.getPlayerList().getPlayer(active.ownerId());
    }

    private static boolean sameLevel(ServerPlayer first, ServerPlayer second)
    {
        return first != null && second != null && first.level() == second.level();
    }

    /**
     * Per player tick state: presence in the zone and the sneak toggle machine.
     */
    private static final class PlayerState
    {
        private boolean inside;
        private boolean sneaking;
        private long lastCrouchTick = -1L;
    }
}
