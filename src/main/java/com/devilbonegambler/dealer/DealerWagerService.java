/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.dealer;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.config.ItemValueConfig;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.data.LuckData;
import com.devilbonegambler.draw.DiceRoll;
import com.devilbonegambler.draw.DrawEngine;
import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.menu.DealerBetMenu;
import com.devilbonegambler.item.DiceProvenance;
import com.devilbonegambler.mob.GamblersDealer;
import com.devilbonegambler.network.RequestGuard;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.DealerPanelPacket;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Server authoritative dealer wager: one independent session per dealer, a recruit clock, the bet boxes, the pull in
 * of the last seconds and the settlement that picks one member by the dice point and pays by the rolled colour.
 * The sessions are independent of the gambler's wager, so both can run at the same time.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DealerWagerService
{
    private static final String LANG = "gui." + ModConstants.MOD_ID + ".dealer.";
    private static final String KEY_CREATED = LANG + "created";
    private static final String KEY_JOINED = LANG + "joined";
    private static final String KEY_LEFT = LANG + "left";
    private static final String KEY_YOU_LEFT = LANG + "you_left";
    private static final String KEY_REMIND = LANG + "remind";
    private static final String KEY_PULL = LANG + "pull";
    private static final String KEY_KICK_NO_BET = LANG + "kick_no_bet";
    private static final String KEY_CANCELLED = LANG + "cancelled";
    private static final String KEY_ABORTED = LANG + "aborted";
    private static final String KEY_STARTED = LANG + "started";
    private static final String KEY_COUNTDOWN = LANG + "countdown";
    private static final String KEY_POINT = LANG + "point";
    private static final String KEY_COLOR_RED = LANG + "color_red";
    private static final String KEY_COLOR_BLACK = LANG + "color_black";
    private static final String KEY_SELECTED = LANG + "selected";
    private static final String KEY_WIN_RED = LANG + "win_red";
    private static final String KEY_WIN_BLACK = LANG + "win_black";
    private static final String KEY_BET_TITLE = LANG + "bet_title";
    private static final String KEY_BET_TITLE_OF = LANG + "bet_title_of";
    private static final String KEY_OFFLINE_MEMBER = LANG + "offline_member";

    /*
     * Design detail: the slot index and the dice point are both reduced modulo six before comparison, the same rule
     * the gambler's wager uses for its candidate list.
     */
    private static final int SELECT_MODULUS = 6;

    private static final Map<UUID, DealerWagerSession> SESSIONS = new HashMap<UUID, DealerWagerSession>();
    private static final Map<UUID, BusyLock> BUSY = new HashMap<UUID, BusyLock>();
    private static final Map<UUID, PullState> PULLS = new HashMap<UUID, PullState>();
    /*
     * Last known name of every member, so a member who is offline is listed under the name the session already knows
     * instead of a raw identifier.
     */
    private static final Map<UUID, String> MEMBER_NAMES = new HashMap<UUID, String>();

    private DealerWagerService()
    {
    }

    /*
     * Client requests
     */

    /**
     * Right click on a dealer with an empty hand: join the running recruit, or open the panel of an idle dealer.
     * The busy lock only covers the creation talk of an idle dealer, never a running recruit.
     */
    public static void open(ServerPlayer player, int dealerEntityId)
    {
        MinecraftServer server = player.getServer();
        GamblersDealer dealer = resolveDealer(server, dealerEntityId);

        if (server == null || dealer == null)
        {
            return;
        }

        DealerWagerSession session = SESSIONS.get(dealer.getUUID());

        if (session != null)
        {
            if (session.phase() == DealerWagerSession.Phase.RUNNING)
            {
                NotifySender.sendFailure(player, "dealer_hosting");
                return;
            }

            if (!session.isMember(player.getUUID()))
            {
                if (isInAnyWager(player))
                {
                    NotifySender.sendFailure(player, "dealer_in_other_wager");
                    return;
                }

                rememberName(player);

                if (!session.addMember(player.getUUID()))
                {
                    NotifySender.sendFailure(player, "dealer_wager_full");
                    return;
                }

                dealer.showRecruitText(session.memberCount(),
                        Math.max(0, remainingSeconds(session, server.getTickCount())));
                broadcastMembers(server, session, Component.translatable(KEY_JOINED, player.getGameProfile().getName()));
            }

            sendPanel(player, dealer, session, true);
            return;
        }

        long now = server.getTickCount();
        BusyLock lock = BUSY.get(dealer.getUUID());

        if (lock != null && !lock.playerId.equals(player.getUUID()) && lock.expiryTick > now)
        {
            NotifySender.sendFailure(player, "dealer_busy");
            return;
        }

        BUSY.put(dealer.getUUID(), new BusyLock(player.getUUID(), now + ModConstants.DEALER_BUSY_TIMEOUT_TICKS));
        sendPanel(player, dealer, null, true);
    }

    /**
     * Creates the dealer wager: one gambler's dice is consumed by the creator, who becomes the first member.
     */
    public static void create(ServerPlayer player, int dealerEntityId)
    {
        MinecraftServer server = player.getServer();
        GamblersDealer dealer = resolveDealer(server, dealerEntityId);

        if (server == null || dealer == null)
        {
            return;
        }

        if (SESSIONS.containsKey(dealer.getUUID()))
        {
            NotifySender.sendFailure(player, "dealer_wager_exists");
            return;
        }

        if (isInAnyWager(player))
        {
            NotifySender.sendFailure(player, "dealer_in_other_wager");
            return;
        }

        if (RequestGuard.isSettling(player))
        {
            NotifySender.sendFailure(player, "settling");
            return;
        }

        if (!DrawEngine.consumeDice(player, gamblersDiceId()))
        {
            NotifySender.sendFailure(player, "not_enough_items");
            return;
        }

        DealerRewardService.recordDiceUse(player);
        /* The dealer hosts from the first second of the recruit on, which also feeds it for the whole wager. */
        dealer.setHosting(true);
        dealer.holdWagerDice();

        DealerWagerSession session = new DealerWagerSession(dealer.getUUID(), player.getUUID(), server.getTickCount());
        session.addMember(player.getUUID());
        rememberName(player);
        SESSIONS.put(dealer.getUUID(), session);
        BUSY.remove(dealer.getUUID());
        dealer.showRecruitText(session.memberCount(), ModConstants.DEALER_WAGER_RECRUIT_SECONDS);
        NotifySender.sendChat(player, KEY_CREATED);
        DevilBoneGambler.LOGGER.info("Dealer wager created, dealer={} owner={}", dealer.getUUID(),
                player.getGameProfile().getName());
    }

    /**
     * Manual start of the creator. The zero bet members are dropped first, exactly like the automatic start.
     */
    public static void start(ServerPlayer player, int dealerEntityId)
    {
        MinecraftServer server = player.getServer();
        GamblersDealer dealer = resolveDealer(server, dealerEntityId);

        if (server == null || dealer == null)
        {
            return;
        }

        DealerWagerSession session = SESSIONS.get(dealer.getUUID());

        if (session == null || session.phase() != DealerWagerSession.Phase.RECRUIT)
        {
            return;
        }

        if (!session.ownerId().equals(player.getUUID()))
        {
            NotifySender.sendFailure(player, "dealer_not_owner");
            return;
        }

        kickMissingBets(server, session, dealer);

        if (session.memberCount() < ModConstants.DEALER_WAGER_MIN_PLAYERS)
        {
            cancel(server, session, dealer, KEY_CANCELLED);
            return;
        }

        settle(server, session, dealer);
    }

    /**
     * Leaves the recruit and returns the own bet. Leaving never happens by distance.
     */
    public static void leave(ServerPlayer player, int dealerEntityId)
    {
        MinecraftServer server = player.getServer();
        GamblersDealer dealer = resolveDealer(server, dealerEntityId);

        if (server == null || dealer == null)
        {
            return;
        }

        DealerWagerSession session = SESSIONS.get(dealer.getUUID());

        if (session == null || session.phase() != DealerWagerSession.Phase.RECRUIT
                || !session.isMember(player.getUUID()))
        {
            return;
        }

        boolean owner = session.ownerId().equals(player.getUUID());
        closeBetMenu(player);
        session.removeMember(player.getUUID());
        DealerBetBox box = session.takeBox(player.getUUID());
        PULLS.remove(player.getUUID());

        if (box != null)
        {
            box.giveBack(player);
        }

        if (owner || session.memberCount() <= 0)
        {
            cancel(server, session, dealer, KEY_CANCELLED);
            return;
        }

        dealer.showRecruitText(session.memberCount(),
                Math.max(0, remainingSeconds(session, server.getTickCount())));
        broadcastMembers(server, session, Component.translatable(KEY_LEFT, player.getGameProfile().getName()));
        NotifySender.sendChat(player, KEY_YOU_LEFT);
        sendPanels(server, session, dealer);
    }

    /**
     * Opens the bet box of one member. Only a member of the session of this dealer may see a box, so a box can never
     * be used as a second inventory by an outsider, and the box of another member is opened read only, which the menu
     * enforces through its slots.
     */
    public static void openBetMenu(ServerPlayer player, int dealerEntityId, UUID boxOwnerId)
    {
        MinecraftServer server = player.getServer();
        GamblersDealer dealer = resolveDealer(server, dealerEntityId);

        if (server == null || dealer == null)
        {
            return;
        }

        DealerWagerSession session = SESSIONS.get(dealer.getUUID());
        UUID targetId = boxOwnerId == null ? player.getUUID() : boxOwnerId;

        if (session == null || !session.isMember(player.getUUID()) || !session.isMember(targetId))
        {
            return;
        }

        DealerBetBox box = session.box(targetId);

        if (box == null)
        {
            return;
        }

        rememberName(player);
        boolean editable = targetId.equals(player.getUUID());
        NetworkHooks.openScreen(player, DealerBetMenu.provider(dealer.getId(), box, session,
                betBoxTitle(server, targetId, editable), editable, () -> server.getTickCount()),
                buffer -> buffer.writeVarInt(dealer.getId()));
    }

    /**
     * Title of the bet screen: the own box keeps the plain wording, the box of another member names that member.
     */
    private static Component betBoxTitle(MinecraftServer server, UUID boxOwnerId, boolean editable)
    {
        if (editable)
        {
            return Component.translatable(KEY_BET_TITLE);
        }

        String name = memberName(server.getPlayerList().getPlayer(boxOwnerId), boxOwnerId);
        Component owner = name.isEmpty() ? Component.translatable(KEY_OFFLINE_MEMBER) : Component.literal(name);
        return Component.translatable(KEY_BET_TITLE_OF, owner);
    }

    /**
     * Closes the bet menu of a player who is still looking at the box of a session that just ended, so a box that is
     * already settled can never be used to move a phantom item.
     */
    private static void closeBetMenu(ServerPlayer player)
    {
        if (player != null && player.containerMenu instanceof DealerBetMenu)
        {
            player.closeContainer();
        }
    }

    /**
     * Remembers the name of a member while it is known, so the panel can keep showing it after the member went offline.
     */
    private static void rememberName(ServerPlayer player)
    {
        if (player != null)
        {
            MEMBER_NAMES.put(player.getUUID(), player.getGameProfile().getName());
        }
    }

    /**
     * Name of one member: the live name while the member is online, the last known name afterwards, and an empty string
     * when the session never saw the member, which the screen draws as the offline wording.
     */
    private static String memberName(ServerPlayer member, UUID memberId)
    {
        if (member != null)
        {
            rememberName(member);
            return member.getGameProfile().getName();
        }

        String known = MEMBER_NAMES.get(memberId);
        return known == null ? "" : known;
    }

    public static boolean isInAnyWager(ServerPlayer player)
    {
        for (DealerWagerSession session : SESSIONS.values())
        {
            if (session.isMember(player.getUUID()))
            {
                return true;
            }
        }

        return false;
    }

    /*
     * Server tick
     */

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        MinecraftServer server = event.getServer();
        long now = server.getTickCount();
        tickPulls(server);
        tickSessions(server, now);
        tickBusy(now);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        SESSIONS.clear();
        BUSY.clear();
        PULLS.clear();
        MEMBER_NAMES.clear();
    }

    /**
     * A member who leaves the server keeps the bet in the session, so the box stays the only place the bet lives. The
     * menu is closed here so a client that disconnects in the middle of a move can not leave a phantom stack behind.
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            closeBetMenu(player);
        }
    }

    private static void tickBusy(long now)
    {
        Iterator<Map.Entry<UUID, BusyLock>> iterator = BUSY.entrySet().iterator();

        while (iterator.hasNext())
        {
            if (iterator.next().getValue().expiryTick <= now)
            {
                iterator.remove();
            }
        }
    }

    private static void tickSessions(MinecraftServer server, long now)
    {
        for (DealerWagerSession session : new ArrayList<DealerWagerSession>(SESSIONS.values()))
        {
            GamblersDealer dealer = resolveDealerByUuid(server, session.dealerId());

            if (dealer == null || !dealer.isAlive())
            {
                cancel(server, session, dealer, KEY_ABORTED);
                continue;
            }

            if (session.phase() == DealerWagerSession.Phase.RECRUIT)
            {
                tickRecruit(server, session, dealer, now);
            }
        }
    }

    private static void tickRecruit(MinecraftServer server, DealerWagerSession session, GamblersDealer dealer, long now)
    {
        int remaining = remainingSeconds(session, now);

        if (remaining != session.lastSecond())
        {
            session.setLastSecond(remaining);
            dealer.showRecruitText(session.memberCount(), Math.max(0, remaining));
            sendPanels(server, session, dealer);

            /*
             * The last few seconds are counted down in chat, one line per second and only for the players that already
             * joined, so nobody has to watch the name tag to know how long is left.
             */
            if (remaining > 0 && remaining <= ModConstants.DEALER_WAGER_COUNTDOWN_SECONDS)
            {
                broadcastMembers(server, session, Component.translatable(KEY_COUNTDOWN, Integer.valueOf(remaining)));
            }

            /*
             * The chime rings only when the wager opens and again in its last seconds, never once per second for the
             * whole recruit.
             */
            int elapsed = ModConstants.DEALER_WAGER_RECRUIT_SECONDS - remaining;

            if (elapsed < ModConstants.DEALER_WAGER_CHIME_HEAD_SECONDS
                    || remaining <= ModConstants.DEALER_WAGER_CHIME_TAIL_SECONDS)
            {
                dealer.dealingParticles();
            }
        }

        if (remaining <= ModConstants.DEALER_WAGER_REMIND_SECONDS)
        {
            remindMissingBets(server, session);
        }

        if (remaining <= ModConstants.DEALER_WAGER_PULL_SECONDS && !session.isPullStarted())
        {
            session.setPullStarted(true);
            broadcastMembers(server, session, Component.translatable(KEY_PULL));
            startPulls(server, session, dealer);
        }

        if (remaining > 0)
        {
            return;
        }

        kickMissingBets(server, session, dealer);

        if (session.memberCount() < ModConstants.DEALER_WAGER_MIN_PLAYERS)
        {
            cancel(server, session, dealer, KEY_CANCELLED);
            return;
        }

        settle(server, session, dealer);
    }

    private static void remindMissingBets(MinecraftServer server, DealerWagerSession session)
    {
        for (UUID memberId : new ArrayList<UUID>(session.members()))
        {
            DealerBetBox box = session.box(memberId);

            if (box == null || !box.isEmpty() || !session.markReminded(memberId))
            {
                continue;
            }

            ServerPlayer member = server.getPlayerList().getPlayer(memberId);

            if (member != null)
            {
                NotifySender.sendChat(member, KEY_REMIND, Integer.valueOf(ModConstants.DEALER_WAGER_REMIND_SECONDS));
            }
        }
    }

    /**
     * A member without a bet does not take part: it is dropped before the wager starts, on both start paths.
     */
    private static void kickMissingBets(MinecraftServer server, DealerWagerSession session, GamblersDealer dealer)
    {
        for (UUID memberId : new ArrayList<UUID>(session.members()))
        {
            DealerBetBox box = session.box(memberId);

            if (box == null || !box.isEmpty())
            {
                continue;
            }

            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            session.removeMember(memberId);
            session.takeBox(memberId);
            PULLS.remove(memberId);
            closeBetMenu(member);

            if (member != null)
            {
                NotifySender.sendChat(member, KEY_KICK_NO_BET);
            }
        }

        if (dealer != null && session.memberCount() > 0)
        {
            sendPanels(server, session, dealer);
        }
    }

    private static void startPulls(MinecraftServer server, DealerWagerSession session, GamblersDealer dealer)
    {
        for (UUID memberId : new ArrayList<UUID>(session.members()))
        {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);

            if (member == null || member.level() != dealer.level())
            {
                continue;
            }

            if (dealer.distanceTo(member) <= ModConstants.DEALER_WAGER_RADIUS)
            {
                continue;
            }

            Vec3 target = pullTarget(dealer);
            PULLS.put(memberId, new PullState(member.position(), target));
        }
    }

    /**
     * The pull is a fast walk, not a teleport: the player is moved along the line in small steps and keeps the
     * view and the controls for the whole two seconds.
     */
    private static void tickPulls(MinecraftServer server)
    {
        Iterator<Map.Entry<UUID, PullState>> iterator = PULLS.entrySet().iterator();

        while (iterator.hasNext())
        {
            Map.Entry<UUID, PullState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

            if (player == null)
            {
                iterator.remove();
                continue;
            }

            PullState state = entry.getValue();
            state.elapsed++;
            double ratio = Math.min(1.0D, state.elapsed / (double) ModConstants.DEALER_WAGER_PULL_TICKS);
            Vec3 pos = state.start.add(state.target.subtract(state.start).scale(ratio));
            player.connection.teleport(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO);

            if (state.elapsed >= ModConstants.DEALER_WAGER_PULL_TICKS)
            {
                iterator.remove();
            }
        }
    }

    private static Vec3 pullTarget(GamblersDealer dealer)
    {
        ServerLevel level = (ServerLevel) dealer.level();
        RandomSource random = level.getRandom();
        BlockPos origin = dealer.blockPosition();

        for (int attempt = 0; attempt < 16; attempt++)
        {
            BlockPos candidate = origin.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);

            if (isStandable(level, candidate))
            {
                return new Vec3(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
            }
        }

        return dealer.position();
    }

    /*
     * Settlement
     */

    /**
     * The point picks one member by the index rule, the colour is rolled independently and then decides the payout.
     */
    private static void settle(MinecraftServer server, DealerWagerSession session, GamblersDealer dealer)
    {
        session.setPhase(DealerWagerSession.Phase.RUNNING);
        dealer.setHosting(true);
        dealer.clearRecruitText();
        PULLS.keySet().removeAll(session.members());

        List<ServerPlayer> members = onlineMembers(server, session);

        if (members.size() < ModConstants.DEALER_WAGER_MIN_PLAYERS)
        {
            cancel(server, session, dealer, KEY_CANCELLED);
            return;
        }

        LuckData luckData = LuckData.get(server);
        RandomSource random = RandomSource.create();
        DiceRoll roll = DiceRoll.roll(random, averageLuck(session, luckData), 0L);
        ServerPlayer winner = selectWinner(session, members, roll.point(), luckData);

        broadcastMembers(server, session, Component.translatable(KEY_STARTED));
        broadcastMembers(server, session, Component.translatable(KEY_POINT, Integer.valueOf(roll.point())));
        broadcastMembers(server, session, Component.translatable(roll.isRed() ? KEY_COLOR_RED : KEY_COLOR_BLACK));

        if (winner == null)
        {
            cancel(server, session, dealer, KEY_CANCELLED);
            return;
        }

        DealerBetBox winnerBox = session.box(winner.getUUID());
        List<ItemStack> ownBet = winnerBox == null ? new ArrayList<ItemStack>() : winnerBox.copyStacks();
        List<ItemStack> pool = new ArrayList<ItemStack>();

        for (UUID memberId : new ArrayList<UUID>(session.members()))
        {
            closeBetMenu(server.getPlayerList().getPlayer(memberId));
            DealerBetBox box = session.takeBox(memberId);

            if (box != null)
            {
                pool.addAll(box.takeAll());
            }
        }

        if (roll.isRed())
        {
            for (ItemStack stack : ownBet)
            {
                if (!stack.isEmpty())
                {
                    pool.add(stack.copy());
                }
            }
        }

        for (ItemStack stack : pool)
        {
            if (stack.isEmpty())
            {
                continue;
            }

            if (!winner.getInventory().add(stack))
            {
                winner.drop(stack, false);
            }
        }

        broadcastMembers(server, session, Component.translatable(KEY_SELECTED, winner.getGameProfile().getName()));
        broadcastMembers(server, session,
                Component.translatable(roll.isRed() ? KEY_WIN_RED : KEY_WIN_BLACK, winner.getGameProfile().getName()));

        /* The payment lands in the pocket first, so a dealer that dies of the wager still leaves it behind. */
        dealer.receiveWagerDice();
        dealer.consumeForWager(members.size());
        dealer.dealingParticles();
        DevilBoneGambler.LOGGER.info("Dealer wager settled, dealer={} point={} red={} winner={} members={}",
                dealer.getUUID(), Integer.valueOf(roll.point()), Boolean.valueOf(roll.isRed()),
                winner.getGameProfile().getName(), Integer.valueOf(members.size()));

        List<UUID> settled = new ArrayList<UUID>();

        for (ServerPlayer member : members)
        {
            settled.add(member.getUUID());
        }

        SESSIONS.remove(session.dealerId());
        session.members().clear();
        dealer.setHosting(false);
        closePanels(server, dealer, settled);
    }

    /**
     * A session that ends tells every member and every player near the dealer to drop the dealer panel, so a recruit
     * that times out never leaves a stale screen open.
     */
    private static void closePanels(MinecraftServer server, GamblersDealer dealer, List<UUID> memberIds)
    {
        Set<UUID> told = new HashSet<UUID>(memberIds);
        DealerPanelPacket closing = DealerPanelPacket.closing(dealer == null ? -1 : dealer.getId());

        for (UUID memberId : memberIds)
        {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);

            if (member != null)
            {
                ModNetwork.sendToPlayer(member, closing);
            }
        }

        if (dealer == null)
        {
            return;
        }

        double reach = ModConstants.DEALER_WAGER_RADIUS * ModConstants.DEALER_WAGER_RADIUS;

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (player.level() != dealer.level() || told.contains(player.getUUID()))
            {
                continue;
            }

            if (dealer.distanceToSqr(player) <= reach)
            {
                ModNetwork.sendToPlayer(player, closing);
            }
        }
    }

    /**
     * After a wager ends the members get one last panel without a wager, so an open screen shows the idle state.
     */
    private static void refreshIdlePanels(MinecraftServer server, GamblersDealer dealer, List<UUID> memberIds)
    {
        for (UUID memberId : memberIds)
        {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);

            if (member != null)
            {
                sendPanel(member, dealer, null, false);
            }
        }
    }

    /**
     * Cancel and abort share one path: every bet and the dice of the creator go back as the very same stacks.
     */
    private static void cancel(MinecraftServer server, DealerWagerSession session, GamblersDealer dealer, String reasonKey)
    {
        List<UUID> members = new ArrayList<UUID>(session.members());
        broadcastMembers(server, session, Component.translatable(reasonKey));
        ServerLevel level = dealer == null ? server.overworld() : (ServerLevel) dealer.level();
        Vec3 fallback = dealer == null ? Vec3.atBottomCenterOf(server.overworld().getSharedSpawnPos())
                : dealer.position();

        for (UUID memberId : members)
        {
            DealerBetBox box = session.takeBox(memberId);
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            PULLS.remove(memberId);
            closeBetMenu(member);

            if (box == null)
            {
                continue;
            }

            for (ItemStack stack : box.takeAll())
            {
                giveBack(level, fallback, member, stack);
            }
        }

        ServerPlayer owner = server.getPlayerList().getPlayer(session.ownerId());
        giveBack(level, fallback, owner, new ItemStack(ModItems.GAMBLERS_DICE.get()));

        SESSIONS.remove(session.dealerId());
        session.members().clear();

        if (dealer != null)
        {
            dealer.setHosting(false);
            dealer.clearWagerDice();
            dealer.clearRecruitText();
            /* A refused wager ends with the villager "no", so the dealer answers every outcome out loud. */
            dealer.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
            closePanels(server, dealer, members);
        }

        DevilBoneGambler.LOGGER.info("Dealer wager ended, dealer={} members={} reason={}", session.dealerId(),
                Integer.valueOf(members.size()), reasonKey);
    }

    private static void giveBack(ServerLevel level, Vec3 fallback, ServerPlayer player, ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return;
        }

        if (player != null)
        {
            DiceProvenance.mark(stack, player.getUUID());

            if (!player.getInventory().add(stack))
            {
                player.drop(stack, false);
            }

            return;
        }

        if (level != null && fallback != null)
        {
            level.addFreshEntity(new ItemEntity(level, fallback.x, fallback.y, fallback.z, stack));
        }
    }

    private static ServerPlayer selectWinner(DealerWagerSession session, List<ServerPlayer> members, int point,
            LuckData luckData)
    {
        int target = Math.floorMod(point, SELECT_MODULUS);
        ServerPlayer best = null;
        int bestLuck = Integer.MIN_VALUE;

        for (ServerPlayer member : members)
        {
            int slot = session.slotOf(member.getUUID());

            if (slot < 0 || Math.floorMod(slot, SELECT_MODULUS) != target)
            {
                continue;
            }

            int luck = luckData.luckOf(member.getUUID());

            if (best == null || luck > bestLuck)
            {
                best = member;
                bestLuck = luck;
            }
        }

        return best;
    }

    private static int averageLuck(DealerWagerSession session, LuckData luckData)
    {
        List<UUID> members = session.members();

        if (members.isEmpty())
        {
            return ModConstants.BASE_LUCK;
        }

        long total = 0L;

        for (UUID memberId : members)
        {
            total += luckData.luckOf(memberId);
        }

        return (int) (total / members.size());
    }

    /*
     * Panel and helpers
     */

    private static void sendPanels(MinecraftServer server, DealerWagerSession session, GamblersDealer dealer)
    {
        for (UUID memberId : new ArrayList<UUID>(session.members()))
        {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);

            if (member != null)
            {
                sendPanel(member, dealer, session, false);
            }
        }
    }

    /**
     * Periodic and event driven panel push. Only the answer to an explicit interaction asks the client to open
     * the screen, so a closed panel stays closed while the recruit clock keeps updating.
     */
    public static void sendPanel(ServerPlayer player, GamblersDealer dealer, DealerWagerSession session,
            boolean openScreen)
    {
        MinecraftServer server = player.getServer();

        if (server == null || dealer == null)
        {
            return;
        }

        boolean hasWager = session != null;
        boolean member = hasWager && session.isMember(player.getUUID());
        boolean owner = hasWager && session.ownerId().equals(player.getUUID());
        int phase = !hasWager ? DealerBetMenu.PHASE_NONE
                : (session.phase() == DealerWagerSession.Phase.RECRUIT ? DealerBetMenu.PHASE_RECRUIT
                        : DealerBetMenu.PHASE_RUNNING);
        int remaining = hasWager ? remainingSeconds(session, server.getTickCount()) : 0;
        ItemValueConfig values = ServerConfigManager.itemValues();
        List<DealerPanelPacket.MemberEntry> entries = new ArrayList<DealerPanelPacket.MemberEntry>();

        if (hasWager)
        {
            for (UUID memberId : new ArrayList<UUID>(session.members()))
            {
                DealerBetBox box = session.box(memberId);
                String name = memberName(server.getPlayerList().getPlayer(memberId), memberId);
                long value = box == null ? 0L : box.totalValue(values);
                int count = box == null ? 0 : box.itemCount();
                entries.add(new DealerPanelPacket.MemberEntry(memberId, name, value, count));
            }

        }

        boolean canCreate = !hasWager && !isInAnyWager(player);
        boolean canStart = owner && session.memberCount() >= ModConstants.DEALER_WAGER_MIN_PLAYERS;
        ModNetwork.sendToPlayer(player, new DealerPanelPacket(dealer.getId(), dealer.getHealth(),
                dealer.getMaxHealth(), (int) Math.round(dealer.satiety()), dealer.maxSatiety(), dealer.experience(),
                Math.max(0, dealer.pocketCapacity() - dealer.pocketDice()), hasWager, phase, remaining,
                hasWager ? session.ownerId() : null, owner, member, canCreate, canStart,
                openScreen, entries));
    }

    private static void broadcastMembers(MinecraftServer server, DealerWagerSession session, Component text)
    {
        for (UUID memberId : new ArrayList<UUID>(session.members()))
        {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);

            if (member != null)
            {
                member.sendSystemMessage(text);
            }
        }
    }

    private static List<ServerPlayer> onlineMembers(MinecraftServer server, DealerWagerSession session)
    {
        List<ServerPlayer> result = new ArrayList<ServerPlayer>();

        for (UUID memberId : new ArrayList<UUID>(session.members()))
        {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);

            if (member != null)
            {
                result.add(member);
            }
        }

        return result;
    }

    /**
     * Seconds left on the recruit clock. Shared with the bet menu, so the container header and the panel show the same
     * number on the same tick.
     */
    public static int remainingSeconds(DealerWagerSession session, long now)
    {
        long elapsed = Math.max(0L, now - session.startTick());
        return ModConstants.DEALER_WAGER_RECRUIT_SECONDS
                - (int) (elapsed / ModConstants.TICKS_PER_SECOND);
    }

    private static GamblersDealer resolveDealer(MinecraftServer server, int entityId)
    {
        if (server == null)
        {
            return null;
        }

        for (ServerLevel level : server.getAllLevels())
        {
            Entity entity = level.getEntity(entityId);

            if (entity instanceof GamblersDealer dealer)
            {
                return dealer;
            }
        }

        return null;
    }

    private static GamblersDealer resolveDealerByUuid(MinecraftServer server, UUID dealerId)
    {
        if (server == null || dealerId == null)
        {
            return null;
        }

        for (ServerLevel level : server.getAllLevels())
        {
            Entity entity = level.getEntity(dealerId);

            if (entity instanceof GamblersDealer dealer)
            {
                return dealer;
            }
        }

        return null;
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos)
    {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above());
    }

    private static String gamblersDiceId()
    {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(ModItems.GAMBLERS_DICE.get());
        return key == null ? ModConstants.MOD_ID + ":gamblers_dice" : key.toString();
    }

    private static final class BusyLock
    {
        private final UUID playerId;
        private final long expiryTick;

        private BusyLock(UUID playerId, long expiryTick)
        {
            this.playerId = playerId;
            this.expiryTick = expiryTick;
        }
    }

    private static final class PullState
    {
        private final Vec3 start;
        private final Vec3 target;
        private int elapsed;

        private PullState(Vec3 start, Vec3 target)
        {
            this.start = start;
            this.target = target;
        }
    }
}
