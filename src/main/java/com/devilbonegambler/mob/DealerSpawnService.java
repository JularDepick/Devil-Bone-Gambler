/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.mob;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.registry.ModEntities;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Silent spawn rule: at the sunset and sunrise ticks of the overworld one dealer appears near one player who holds
 * the gambler's dice. The rule is server only and never reaches a player visible text or a client bound field.
 * A player whose chunk already holds a dealer is skipped, so one chunk keeps one dealer at a time.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DealerSpawnService
{
    private static final int SPAWN_ATTEMPTS = 32;

    private static long lastDayTime = -1L;

    private DealerSpawnService()
    {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        ServerLevel level = event.getServer().overworld();
        long dayTime = level.getDayTime() % ModConstants.DEALER_DAY_TICKS;
        long previous = lastDayTime;
        lastDayTime = dayTime;

        if (previous < 0L)
        {
            return;
        }

        if (crossed(previous, dayTime, ModConstants.DEALER_SUNSET_TICK)
                || crossed(previous, dayTime, ModConstants.DEALER_SUNRISE_TICK))
        {
            spawnOne(event.getServer(), level);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        lastDayTime = -1L;
    }

    private static boolean crossed(long previous, long current, long mark)
    {
        return previous < mark && current >= mark;
    }

    /**
     * One dealer per trigger: candidates are shuffled, a candidate whose chunk already holds a dealer is skipped,
     * and the first usable position wins.
     */
    private static void spawnOne(MinecraftServer server, ServerLevel level)
    {
        List<ServerPlayer> candidates = new ArrayList<ServerPlayer>();

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (player.level() == level && holdsGamblersDice(player))
            {
                candidates.add(player);
            }
        }

        if (candidates.isEmpty())
        {
            return;
        }

        Collections.shuffle(candidates);

        for (ServerPlayer target : candidates)
        {
            ChunkPos chunk = new ChunkPos(target.blockPosition());

            if (hasDealerInChunk(level, chunk))
            {
                continue;
            }

            BlockPos pos = findSpawnPos(level, target.blockPosition());

            if (pos == null)
            {
                continue;
            }

            GamblersDealer dealer = ModEntities.GAMBLERS_DEALER.get().create(level);

            if (dealer == null)
            {
                return;
            }

            dealer.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, target.getYRot(), 0.0F);
            dealer.setPersistenceRequired();
            level.addFreshEntity(dealer);
            DevilBoneGambler.LOGGER.info("Dealer spawned silently near {}, pos={}",
                    target.getGameProfile().getName(), pos);
            return;
        }
    }

    private static boolean holdsGamblersDice(Player player)
    {
        return player.getMainHandItem().is(ModItems.GAMBLERS_DICE.get())
                || player.getOffhandItem().is(ModItems.GAMBLERS_DICE.get());
    }

    private static boolean hasDealerInChunk(ServerLevel level, ChunkPos chunk)
    {
        AABB box = new AABB(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX() + 1.0D, level.getMaxBuildHeight(), chunk.getMaxBlockZ() + 1.0D);

        for (GamblersDealer dealer : level.getEntitiesOfClass(GamblersDealer.class, box))
        {
            if (new ChunkPos(dealer.blockPosition()).equals(chunk))
            {
                return true;
            }
        }

        return false;
    }

    private static BlockPos findSpawnPos(ServerLevel level, BlockPos origin)
    {
        RandomSource random = level.getRandom();
        int radius = ModConstants.DEALER_SPAWN_RADIUS;
        int vertical = ModConstants.DEALER_SPAWN_VERTICAL;

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++)
        {
            BlockPos candidate = origin.offset(random.nextInt(radius * 2 + 1) - radius,
                    random.nextInt(vertical * 2 + 1) - vertical, random.nextInt(radius * 2 + 1) - radius);

            if (isStandable(level, candidate))
            {
                return candidate;
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
}
