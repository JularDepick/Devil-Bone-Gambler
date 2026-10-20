/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.item;

import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Player provenance of a gambler's die.
 *
 * A die thrown by a player or dropped on death carries the owner UUID, so a dealer that picks it up can attribute the
 * feed to the player who put it into the world. A stack carries one owner; a later player who throws the same stack
 * replaces it.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DiceProvenance
{
    private DiceProvenance()
    {
    }

    public static void mark(ItemStack stack, UUID owner)
    {
        if (!stack.isEmpty() && stack.is(ModItems.GAMBLERS_DICE.get()) && owner != null)
        {
            stack.getOrCreateTag().putUUID(ModConstants.DICE_OWNER_TAG, owner);
        }
    }

    /**
     * The tagged owner of the stack, or null when the die did not come from a recorded player drop.
     */
    public static UUID owner(ItemStack stack)
    {
        if (stack == null || stack.isEmpty() || !stack.is(ModItems.GAMBLERS_DICE.get()))
        {
            return null;
        }

        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(ModConstants.DICE_OWNER_TAG)
                ? tag.getUUID(ModConstants.DICE_OWNER_TAG) : null;
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event)
    {
        Player player = event.getPlayer();

        if (player instanceof ServerPlayer serverPlayer && !player.level().isClientSide)
        {
            mark(event.getEntity().getItem(), serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        for (ItemEntity drop : event.getDrops())
        {
            mark(drop.getItem(), player.getUUID());
        }
    }
}
