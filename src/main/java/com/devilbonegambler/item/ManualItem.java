/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.item;

import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.manual.ManualKind;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * A manual: a book shaped item whose whole content is client side, so opening it never needs a server round trip.
 * The item itself only carries which manual it is; the text lives in the shipped markdown.
 * A bound manual may never leave its holder as a dropped item. This refusal covers the hotbar drop key, which is the
 * one path that asks the item before the stack leaves the inventory; every other drop path takes the stack out first
 * and is caught by the manual service on the toss event, where the thrower is known for certain.
 */
public class ManualItem extends Item
{
    private static final String KEY_KEPT = "gui." + ModConstants.MOD_ID + ".manual.kept";

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
     * Refuses the hotbar drop key before the stack leaves the inventory, so that path never produces a dropped manual
     * in the first place. The other drop paths are refused on the toss event by the manual service.
     */
    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player)
    {
        if (!this.kind.bound())
        {
            return true;
        }

        if (player instanceof ServerPlayer serverPlayer)
        {
            NotifySender.sendChat(serverPlayer, KEY_KEPT, stack.getHoverName());
        }

        return false;
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
