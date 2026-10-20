/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.block;

import com.devilbonegambler.altar.AltarService;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.PlacedDiceUsePacket;
import com.devilbonegambler.registry.ModBlocks;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Placing a die on the ground and breaking it again.
 *
 * Holding the placement key while right clicking puts the held die down as a block. Breaking that block with an empty
 * hand and without the key behaves as if the die had been used: the server puts one die into the hand (or the inventory
 * when the devil's die opens the altar screen), then tells the client to start the normal action. With the key held the
 * block simply breaks and drops the die.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlacedDiceService
{
    private static final Map<UUID, Boolean> MODIFIER = new ConcurrentHashMap<UUID, Boolean>();
    private static final double PLACE_RANGE_SQR = 36.0D;

    private PlacedDiceService()
    {
    }

    public static void setModifier(ServerPlayer player, boolean held)
    {
        if (player == null)
        {
            return;
        }

        if (held)
        {
            MODIFIER.put(player.getUUID(), Boolean.TRUE);
        }
        else
        {
            MODIFIER.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        MODIFIER.remove(event.getEntity().getUUID());
    }

    public static void place(ServerPlayer player, BlockPos clickedPos, Direction face, boolean offhand)
    {
        if (player == null || clickedPos == null || face == null)
        {
            return;
        }

        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);
        boolean devils = stack.is(ModItems.DEVILS_DICE.get());

        if (!devils && !stack.is(ModItems.GAMBLERS_DICE.get()))
        {
            return;
        }

        Level level = player.level();
        BlockPos target = clickedPos.relative(face);

        if (!level.isInWorldBounds(target) || !level.mayInteract(player, target)
                || !player.mayUseItemAt(target, face, stack))
        {
            return;
        }

        if (player.distanceToSqr(Vec3.atCenterOf(target)) > PLACE_RANGE_SQR)
        {
            return;
        }

        BlockState existing = level.getBlockState(target);

        if (!existing.canBeReplaced())
        {
            return;
        }

        Block block = devils ? ModBlocks.PLACED_DEVILS_DICE.get() : ModBlocks.PLACED_GAMBLERS_DICE.get();
        BlockState placed = block.defaultBlockState();

        if (!level.isUnobstructed(placed, target, CollisionContext.empty())
                || !level.setBlockAndUpdate(target, placed))
        {
            return;
        }

        player.swing(hand, true);

        if (!player.getAbilities().instabuild)
        {
            stack.shrink(1);
        }

        level.playSound(null, target, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        if (!(event.getState().getBlock() instanceof PlacedDiceBlock dice))
        {
            return;
        }

        if (!(event.getPlayer() instanceof ServerPlayer player))
        {
            return;
        }

        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())
        {
            return;
        }

        if (isModifierHeld(player))
        {
            return;
        }

        event.setCanceled(true);

        if (!(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }

        if (!level.removeBlock(event.getPos(), false))
        {
            return;
        }

        useFromBreak(player, level, event.getPos(), dice.devils());
    }

    private static boolean isModifierHeld(ServerPlayer player)
    {
        return MODIFIER.containsKey(player.getUUID());
    }

    private static void useFromBreak(ServerPlayer player, ServerLevel level, BlockPos pos, boolean devils)
    {
        ItemStack dice = new ItemStack(devils ? ModItems.DEVILS_DICE.get() : ModItems.GAMBLERS_DICE.get());

        /* In range the devil's die opens the altar screen; the die waits in the inventory, so cancelling keeps it. */
        if (devils && AltarService.governingValue(player) >= 0L)
        {
            if (!player.getInventory().add(dice))
            {
                Block.popResource(level, pos, dice);
            }

            ModNetwork.sendToPlayer(player, new PlacedDiceUsePacket(PlacedDiceUsePacket.DEVILS_ALTAR));
            return;
        }

        player.setItemInHand(InteractionHand.MAIN_HAND, dice);
        ModNetwork.sendToPlayer(player, new PlacedDiceUsePacket(devils
                ? PlacedDiceUsePacket.DEVILS_DRAW : PlacedDiceUsePacket.GAMBLERS_DRAW));
    }
}
