/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.item;

import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.manual.ManualKind;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * A manual: a book shaped item whose whole content is client side, so opening it never needs a server round trip.
 * The item itself only carries which manual it is; the text lives in the shipped markdown.
 * Which player may hold which bound manual is decided by the calibration pass of the manual service and by nothing
 * else, so the item does not react to being dropped: a dropped copy lies in the world for at most one pass, which
 * clears it and grants the holder a new one without a prompt of its own.
 */
public class ManualItem extends Item
{
    private final ManualKind kind;

    public ManualItem(ManualKind kind, Item.Properties properties)
    {
        super(properties);
        this.kind = kind;
    }

    public ManualKind kind()
    {
        return this.kind;
    }

    /**
     * The name carries the colour of its manual, which is the one place every display reads it from: the tooltip, the
     * hotbar label and the chat prompt of a granted copy all follow this.
     */
    @Override
    public Component getName(ItemStack stack)
    {
        return super.getName(stack).copy().withStyle(style -> style.withColor(this.kind.nameColor()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide)
        {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientActions.openManualScreen(this.kind));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
