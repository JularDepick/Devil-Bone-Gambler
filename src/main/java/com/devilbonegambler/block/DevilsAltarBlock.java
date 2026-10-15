/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.block;

import com.devilbonegambler.altar.AltarService;
import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.util.ModConstants;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;

/**
 * The devil's altar: a single decorative block that carries the accumulated offering value.
 * The block never opens a screen, never drops through the loot table while a value is stored,
 * and cannot be broken at all without the correct tool.
 * The whole BlockBehaviour override surface is marked deprecated by vanilla, so the warning is suppressed here.
 */
@SuppressWarnings("deprecation")
public class DevilsAltarBlock extends Block implements EntityBlock
{
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D,
            ModConstants.ALTAR_MODEL_HEIGHT, 16.0D);

    public DevilsAltarBlock(Properties properties)
    {
        super(properties);
    }

    /**
     * The model stops at ALTAR_MODEL_HEIGHT pixels, so the collision and the occlusion box stop there too
     * instead of filling the whole block.
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new DevilsAltarBlockEntity(pos, state);
    }

    /**
     * Declaring the correct tool requirement only slows the wrong tool down and cancels the drop,
     * so the destroy progress itself has to be blocked for every non correct tool.
     */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos)
    {
        if (!ForgeHooks.isCorrectToolForDrops(state, player))
        {
            return 0.0F;
        }

        return super.getDestroyProgress(state, player, level, pos);
    }

    /**
     * The altar has no interaction at all, so the click always falls through to the held item.
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit)
    {
        return InteractionResult.PASS;
    }

    /**
     * A stored offering value travels with the dropped item, so breaking and replacing the altar
     * never loses the accumulated value.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params)
    {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (!(blockEntity instanceof DevilsAltarBlockEntity altar) || altar.offeringValue() <= 0L)
        {
            return super.getDrops(state, params);
        }

        ItemStack drop = new ItemStack(this);
        drop.getOrCreateTag().putLong(ModConstants.ALTAR_VALUE_TAG, altar.offeringValue());
        return Collections.singletonList(drop);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
        super.setPlacedBy(level, pos, state, placer, stack);

        CompoundTag tag = stack.getTag();

        if (tag != null && tag.contains(ModConstants.ALTAR_VALUE_TAG))
        {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof DevilsAltarBlockEntity altar)
            {
                altar.setOfferingValue(tag.getLong(ModConstants.ALTAR_VALUE_TAG));
            }
        }

        /*
         * Two altar ranges may never overlap, so an altar placed inside another range is destroyed right away
         * and lands on the ground as an item, which carries the accumulated offering value back out.
         * The altar at this position is excluded because its own range always covers itself.
         */
        if (!(level instanceof ServerLevel serverLevel))
        {
            return;
        }

        if (!AltarService.rangeCovers(serverLevel, pos, pos))
        {
            return;
        }

        level.removeBlock(pos, false);

        if (placer instanceof ServerPlayer player)
        {
            if (!player.getAbilities().instabuild)
            {
                Block.popResource(level, pos, stack.copy());
            }

            NotifySender.sendFailure(player, "altar_range_overlap");
        }
    }
}
