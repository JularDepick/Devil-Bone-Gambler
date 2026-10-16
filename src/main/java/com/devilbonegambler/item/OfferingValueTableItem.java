/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.item;

import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.util.ValueTableBook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Offering Value Table: an item carrying a snapshot of the server side value table.
 * Right clicking refreshes the snapshot on the server and then opens the searchable value table view on the client,
 * which lists the localised names and offers a search box.
 */
public class OfferingValueTableItem extends WrittenBookItem
{
    public OfferingValueTableItem(Item.Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide)
        {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientActions.sendValueTableRefreshRequest());
        }
        else if (player instanceof ServerPlayer serverPlayer)
        {
            ValueTableBook.refreshSnapshot(stack, serverPlayer);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
