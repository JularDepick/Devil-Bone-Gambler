/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.block;

import com.devilbonegambler.mob.GamblersDealer;
import com.devilbonegambler.util.ModConstants;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.material.PushReaction;

/**
 * The glow a dealer carries around, placed as a block at the feet of the dealer.
 * Minecraft has no light on an entity, so the glow has to be a block: it behaves like air to everything else, it is
 * replaceable, it drops nothing and it never shows up in a creative tab, because it is a working prop rather than a
 * block a player can hold.
 * A glow is also written into the chunk like any other block, so it checks every now and then that a dealer is still
 * standing in it; a copy that was left behind by an unloaded dealer or by a restart puts itself out.
 */
public final class DealerGlowBlock extends AirBlock
{
    public DealerGlowBlock()
    {
        /*
         * Air is the closest thing to nothing at all, so the glow inherits its shape, its lack of collision and its
         * transparency, and only changes the light it emits and the fact that anything may replace it.
         */
        super(BlockBehaviour.Properties.of()
                .replaceable()
                .noCollission()
                .noOcclusion()
                .noLootTable()
                .randomTicks()
                .pushReaction(PushReaction.DESTROY)
                .lightLevel(state -> ModConstants.DEALER_LIGHT_LEVEL));
    }

    /**
     * A glow without a dealer in it is a leftover and goes out by itself, which keeps a chunk clean even when the
     * dealer was unloaded before it could take its glow with it.
     */
    /* Vanilla marks the random tick hook deprecated, but it is still the way a block reacts to a random tick. */
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (!this.holdsDealer(level, pos))
        {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        }
    }

    /**
     * True while a dealer stands in this very block, so a glow under a dealer is kept and every other copy is not.
     */
    private boolean holdsDealer(Level level, BlockPos pos)
    {
        List<GamblersDealer> dealers = level.getEntitiesOfClass(GamblersDealer.class,
                new AABB(pos).inflate(0.5D), dealer -> dealer.isAlive() && dealer.blockPosition().equals(pos));
        return !dealers.isEmpty();
    }

    /**
     * Never kept by a player: a glow that is broken or overwritten simply stops existing, which is what happens when
     * the dealer walks on and the glow is moved along.
     */
    @Override
    public boolean isPossibleToRespawnInThis(BlockState state)
    {
        return false;
    }
}
