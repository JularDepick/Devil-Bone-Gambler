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
import com.devilbonegambler.mob.GamblersDealer;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.DealerBetMovePacket;
import com.devilbonegambler.network.packet.DealerPanelPacket;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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
    private static final String KEY_POINT = LANG + "point";
    private static final String KEY_COLOR_RED = LANG + "color_red";
    private static final String KEY_COLOR_BLACK = LANG + "color_black";
    private static final String KEY_SELECTED = LANG + "selected";
    private static final String KEY_WIN_RED = LANG + "win_red";
    private static final String KEY_WIN_BLACK = LANG + "win_black";

    /*
     * Design detail: the slot index and the dice point are both reduced modulo six before comparison, the same rule
     * the gambler's wager uses for its candidate list.
     */
    private static final int SELECT_MODULUS = 6;
    private static final int PHASE_NONE = 0;
    private static final int PHASE_RECRUIT = 1;
    private static final int PHASE_RUNNING = 2;

    private static final Map<UUID, DealerWagerSession> SESSIONS = new HashMap<UUID, DealerWagerSession>();
    private static final Map<UUID, BusyLock> BUSY = new HashMap<UUID, BusyLock>();
    private static final Map<UUID, PullState> PULLS = new HashMap<UUID, PullState>();

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

                if (!session.addMember(player.getUUID()))
                {
                    NotifySender.sendFailure(player, "dealer_wager_full");
                    return;
                }

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

        if (!DrawEngine.consumeDice(player, gamblersDiceId()))
        {
            NotifySender.sendFailure(player, "not_enough_items");
            return;
        }

        DealerRewardService.recordDiceUse(player);

        DealerWagerSession session = new DealerWagerSession(dealer.getUUID(), player.getUUID(), server.getTickCount());
        session.addMember(player.getUUID());
        SESSIONS.put(dealer.getUUID(), session);
        BUSY.remove(dealer.getUUID());
        dealer.showRecruitText(ModConstants.DEALER_WAGER_RECRUIT_SECONDS);
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

        broadcastMembers(server, session, Component.translatable(KEY_LEFT, player.getGameProfile().getName()));
        NotifySender.sendChat(player, KEY_YOU_LEFT);
        sendPanels(server, session, dealer);
    }

    /**
     * One bet box move. The client states the item and the count, the server resolves it against the real inventory.
     */
    public static void moveBet(ServerPlayer player, int dealerEntityId, int action, int index, String itemId,
            int count)
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

        DealerBetBox box = session.box(player.getUUID());

        if (box == null)
        {
            return;
        }

        ResourceLocation key = ResourceLocation.tryParse(itemId);
        Item item = key == null ? null : ForgeRegistries.ITEMS.getValue(key);

        if (item == null)
        {
            return;
        }

        if (action == DealerBetMovePacket.ACTION_TO_BOX)
        {
            /*
             * Take the items out first and hand back whatever the box did not accept, so a full box can never leave a
             * second copy behind in the inventory.
             */
            ItemStack taken = takeFromInventory(player, item, count);

            if (taken.isEmpty())
            {
                return;
            }

            int moved = box.insert(taken, taken.getCount(), index);

            if (moved < taken.getCount())
            {
                taken.shrink(moved);
                NotifySender.sendFailure(player, "dealer_bet_full");

                if (!player.getInventory().add(taken))
                {
                    player.drop(taken, false);
                }
            }

            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        else if (action == DealerBetMovePacket.ACTION_TO_INVENTORY)
        {
            if (index < 0 || index >= ModConstants.DEALER_BET_SLOTS)
            {
                return;
            }

            ItemStack source = box.get(index);

            if (source.isEmpty())
            {
                return;
            }

            int amount = Math.min(Math.max(1, count), source.getCount());
            ItemStack taken = source.copy();
            taken.setCount(amount);
            source.shrink(amount);

            if (source.isEmpty())
            {
                box.stacks().set(index, ItemStack.EMPTY);
            }

            if (!player.getInventory().add(taken))
            {
                player.drop(taken, false);
            }

            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        else
        {
            return;
        }

        sendPanels(server, session, dealer);
    }

    /**
     * Removes up to the given amount of one item from the player inventory, earliest slot first.
     * The returned stack is what was really removed, so the caller can give back what the box refused.
     */
    private static ItemStack takeFromInventory(ServerPlayer player, Item item, int amount)
    {
        int wanted = Math.max(1, amount);
        ItemStack taken = ItemStack.EMPTY;
        Inventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getContainerSize() && wanted > 0; slot++)
        {
            ItemStack stack = inventory.getItem(slot);

            if (stack.isEmpty() || stack.getItem() != item)
            {
                continue;
            }

            int step = Math.min(wanted, stack.getCount());

            if (taken.isEmpty())
            {
                taken = stack.copy();
                taken.setCount(step);
            }
            else
            {
                taken.grow(step);
            }

            stack.shrink(step);
            wanted -= step;
        }

        return taken;
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
            dealer.showRecruitText(Math.max(0, remaining));
            sendPanels(server, session, dealer);
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

            session.removeMember(memberId);
            session.takeBox(memberId);
            PULLS.remove(memberId);
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);

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
        refreshIdlePanels(server, dealer, settled);
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
            dealer.clearRecruitText();
            refreshIdlePanels(server, dealer, members);
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
        int phase = !hasWager ? PHASE_NONE
                : (session.phase() == DealerWagerSession.Phase.RECRUIT ? PHASE_RECRUIT : PHASE_RUNNING);
        int remaining = hasWager ? remainingSeconds(session, server.getTickCount()) : 0;
        ItemValueConfig values = ServerConfigManager.itemValues();
        List<DealerPanelPacket.MemberEntry> entries = new ArrayList<DealerPanelPacket.MemberEntry>();
        List<ItemStack> ownBet = new ArrayList<ItemStack>();

        if (hasWager)
        {
            for (UUID memberId : new ArrayList<UUID>(session.members()))
            {
                ServerPlayer memberPlayer = server.getPlayerList().getPlayer(memberId);
                String name = memberPlayer != null ? memberPlayer.getGameProfile().getName()
                        : memberId.toString().substring(0, 8);
                DealerBetBox box = session.box(memberId);
                long value = box == null ? 0L : box.totalValue(values);
                int count = box == null ? 0 : box.itemCount();
                entries.add(new DealerPanelPacket.MemberEntry(memberId, name, value, count));
            }

            if (member)
            {
                DealerBetBox box = session.box(player.getUUID());
                ownBet = box == null ? new ArrayList<ItemStack>() : box.copyStacks();
            }
        }

        boolean canCreate = !hasWager && !isInAnyWager(player);
        boolean canStart = owner && session.memberCount() >= ModConstants.DEALER_WAGER_MIN_PLAYERS;
        ModNetwork.sendToPlayer(player, new DealerPanelPacket(dealer.getId(), dealer.getHealth(),
                dealer.getMaxHealth(), dealer.satiety(), ModConstants.DEALER_MAX_SATIETY, hasWager, phase, remaining,
                hasWager ? session.ownerId() : null, owner, member, canCreate, canStart, openScreen, entries, ownBet));
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

    private static int remainingSeconds(DealerWagerSession session, long now)
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
