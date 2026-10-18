/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.mob;

import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Pathing preference of the dealer: a brighter spot wins over a drier one, and both win over a distant one.
 * The goal only starts once the previous stay is over, and it hands the dealer back to the stay countdown as soon
 * as the walk ends or times out.
 */
public class DealerWanderGoal extends Goal
{
    private final GamblersDealer dealer;
    private BlockPos target;
    private int runningTicks;

    public DealerWanderGoal(GamblersDealer dealer)
    {
        this.dealer = dealer;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        if (this.dealer.isHosting() || this.dealer.stayTicks() > 0 || this.dealer.isDefendingTerritory()
                || this.dealer.isInLiquid())
        {
            return false;
        }

        if (this.dealer.getNavigation().isInProgress())
        {
            return false;
        }

        BlockPos picked = this.pickTarget();

        if (picked == null)
        {
            return false;
        }

        this.target = picked;
        return true;
    }

    @Override
    public void start()
    {
        this.runningTicks = 0;
        this.dealer.getNavigation().moveTo(this.target.getX() + 0.5D, this.target.getY(), this.target.getZ() + 0.5D,
                0.6D);
    }

    @Override
    public boolean canContinueToUse()
    {
        return !this.dealer.isHosting() && !this.dealer.isDefendingTerritory() && !this.dealer.isInLiquid()
                && this.runningTicks < ModConstants.DEALER_PATH_TIMEOUT_TICKS
                && !this.dealer.getNavigation().isDone();
    }

    @Override
    public void tick()
    {
        this.runningTicks++;
    }

    @Override
    public void stop()
    {
        this.dealer.getNavigation().stop();
        this.dealer.beginStay();
    }

    private BlockPos pickTarget()
    {
        Level level = this.dealer.level();
        BlockPos origin = this.dealer.blockPosition();
        RandomSource random = this.dealer.getRandom();
        BlockPos best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int radius = ModConstants.DEALER_PATH_RADIUS;

        /*
         * A dealer that still has room in its pocket walks to the nearest dice on the ground instead of a random spot,
         * so the walk itself brings it within reach of the pickup.
         */
        if (this.dealer.pocketDice() < this.dealer.pocketCapacity())
        {
            BlockPos lure = this.lureToDice(level);

            if (lure != null)
            {
                return lure;
            }
        }

        for (int index = 0; index < ModConstants.DEALER_PATH_SAMPLES; index++)
        {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dy = random.nextInt(3) - 1;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            BlockPos candidate = origin.offset(dx, dy, dz);

            if (!isStandable(level, candidate))
            {
                continue;
            }

            double score = score(level, candidate);

            if (score > bestScore)
            {
                bestScore = score;
                best = candidate;
            }
        }

        if (best == null || best.equals(origin))
        {
            return null;
        }

        return best;
    }

    /**
     * A standable block right next to the nearest gambler's dice, or null when nothing lies within the lure radius.
     */
    private BlockPos lureToDice(Level level)
    {
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, this.dealer.getBoundingBox().inflate(
                ModConstants.DEALER_DICE_LURE_RADIUS));
        ItemEntity nearest = null;
        double bestDistance = Double.MAX_VALUE;

        for (ItemEntity drop : drops)
        {
            ItemStack stack = drop.getItem();

            if (!stack.is(ModItems.GAMBLERS_DICE.get()))
            {
                continue;
            }

            double distance = drop.distanceToSqr(this.dealer);

            if (distance < bestDistance)
            {
                bestDistance = distance;
                nearest = drop;
            }
        }

        if (nearest == null)
        {
            return null;
        }

        BlockPos dropPos = nearest.blockPosition();

        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            BlockPos candidate = dropPos.relative(direction);

            if (this.isStandable(level, candidate))
            {
                return candidate;
            }
        }

        return this.isStandable(level, dropPos) ? dropPos : null;
    }

    private boolean isStandable(Level level, BlockPos pos)
    {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above());
    }

    private double score(Level level, BlockPos pos)
    {
        double light = level.getMaxLocalRawBrightness(pos);
        double liquid = countLiquid(level, pos);
        double distance = Math.sqrt(pos.distSqr(this.dealer.blockPosition()));
        return light * ModConstants.DEALER_LIGHT_WEIGHT - liquid * ModConstants.DEALER_LIQUID_WEIGHT
                - distance * ModConstants.DEALER_DISTANCE_WEIGHT;
    }

    private int countLiquid(Level level, BlockPos pos)
    {
        int radius = ModConstants.DEALER_LIQUID_SCAN_RADIUS;
        int count = 0;

        for (BlockPos probe : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius),
                pos.offset(radius, 1, radius)))
        {
            if (!level.getFluidState(probe).isEmpty())
            {
                count++;
            }
        }

        return count;
    }
}
