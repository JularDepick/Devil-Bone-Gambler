/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.item;

import com.devilbonegambler.client.ClientActions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Devil's Dice: opens the offering screen.
 * The screen is fully client side and never touches the real inventory, so no server round trip is needed to open it.
 */
public class DevilsDiceItem extends Item
{
    public DevilsDiceItem(Item.Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide)
        {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientActions.onDevilsDiceUse());
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
