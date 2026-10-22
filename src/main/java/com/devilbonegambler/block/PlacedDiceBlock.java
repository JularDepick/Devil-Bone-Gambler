/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.block;

import com.devilbonegambler.registry.ModItems;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * A die placed on the ground. The block carries no state beyond its kind, and its model slices one six cell sheet by
 * face, so every side of the placed die shows its own number. The loot path only serves sources that are not a player:
 * a player break goes through {@link PlacedDiceService}, which either uses the die or drops it on purpose, so a player
 * break can never turn a use into a silent drop.
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

    /**
     * Nothing but a player may take a placed die back: a piston cannot shove it, and the block tags keep the dragon
     * and the wither from breaking it. Blast resistance is set at registration to a value no explosion can beat.
     */
    @Override
    public PushReaction getPistonPushReaction(BlockState state)
    {
        return PushReaction.BLOCK;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params)
    {
        /*
         * A player break never drops through the loot path: the service hands the die over itself, either as a use or as
         * the explicit drop of a key held break. Other sources, an explosion, a piston or a command, still drop the die.
         */
        if (params.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player)
        {
            return Collections.emptyList();
        }

        return Collections.singletonList(new ItemStack(this.devils ? ModItems.DEVILS_DICE.get() : ModItems.GAMBLERS_DICE.get()));
    }
}
