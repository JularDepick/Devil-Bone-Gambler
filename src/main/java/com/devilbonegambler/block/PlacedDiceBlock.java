/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.block;

import com.devilbonegambler.registry.ModItems;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * A die placed on the ground. The block carries no state beyond its kind and drops the matching die when broken.
 * Breaking it by hand is intercepted by {@link PlacedDiceService} when the modifier key is not held.
 */
public class PlacedDiceBlock extends Block
{
    private final boolean devils;

    public PlacedDiceBlock(boolean devils, Properties properties)
    {
        super(properties);
        this.devils = devils;
    }

    public boolean devils()
    {
        return this.devils;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params)
    {
        return Collections.singletonList(new ItemStack(this.devils ? ModItems.DEVILS_DICE.get() : ModItems.GAMBLERS_DICE.get()));
    }
}
