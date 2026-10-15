/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.block;

import com.devilbonegambler.client.ClientActions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * The four economy blocks share one implementation: a plain stateless block whose right click opens a client screen.
 * None of them carries a block entity, and none of them keeps any data of its own.
 * The whole BlockBehaviour override surface is marked deprecated by vanilla, so the warning is suppressed here.
 */
@SuppressWarnings("deprecation")
public class EconomyBlock extends Block
{
    public enum Kind
    {
        BANK,
        SHOP,
        EXCHANGE,
        RECYCLER
    }

    private final Kind kind;

    public EconomyBlock(Kind kind, Properties properties)
    {
        super(properties);
        this.kind = kind;
    }

    public Kind kind()
    {
        return this.kind;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit)
    {
        if (level.isClientSide)
        {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientActions.openEconomyScreen(this.kind));
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
