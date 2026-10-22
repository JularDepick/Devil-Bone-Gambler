/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.block;

import com.devilbonegambler.altar.AltarService;
import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.RequestGuard;
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
 * Holding the placement key while right clicking puts the held die down as a block. Breaking that block without the key
 * behaves as if the die had been used, no matter what the player is holding: the server moves the die into the
 * inventory and tells the client to start the normal action, so the stack in hand is never replaced. That branch never
 * creates a drop; with the key held the block breaks normally and drops the die instead.
 *
 * The key state travels with the break request itself, so a click is decided by the key as it was at that moment. The
 * mirrored key state and the break event hook stay only as a fallback for breaks that do not come from this client.
 *
 * A break that would use the die is refused before the block is removed while a previous settlement still runs or the
 * minimum interval between draws has not elapsed, so a throttled click never turns a placed die into a free one.
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

    /**
     * A placed die is broken by a player. The key state comes with the request, so the outcome never depends on a
     * mirror that may have gone stale: with the key held the die breaks into a drop, without it the same click uses the
     * die and nothing is dropped.
     */
    public static void breakAt(ServerPlayer player, BlockPos pos, boolean modifierHeld)
    {
        if (player == null || pos == null || !(player.level() instanceof ServerLevel level))
        {
            return;
        }

        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof PlacedDiceBlock dice))
        {
            return;
        }

        if (!level.mayInteract(player, pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > PLACE_RANGE_SQR)
        {
            return;
        }

        /*
         * The draw this click would start is gated before the block disappears, so a refused draw leaves the block
         * where it is instead of taking the die out of the world first.
         */
        if (!modifierHeld)
        {
            String refusal = RequestGuard.peekDraw(player);

            if (refusal != null)
            {
                NotifySender.sendFailure(player, refusal);
                return;
            }
        }

        if (!level.removeBlock(pos, false))
        {
            return;
        }

        if (modifierHeld)
        {
            /*
             * Holding the key is the only player driven way to get the die back. Creative keeps the vanilla rule of
             * breaking without a drop.
             */
            if (!player.getAbilities().instabuild)
            {
                Block.popResource(level, pos, new ItemStack(dice.devils()
                        ? ModItems.DEVILS_DICE.get() : ModItems.GAMBLERS_DICE.get()));
            }

            return;
        }

        useFromBreak(player, level, pos, state, dice.devils());
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

    /**
     * Fallback for breaks that do not come through the break request of this mod, for example another mod removing the
     * block for a player. The mirrored key state decides here, and the branch never creates a drop on its own.
     */
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

        if (isModifierHeld(player))
        {
            return;
        }

        event.setCanceled(true);

        if (!(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }

        String refusal = RequestGuard.peekDraw(player);

        if (refusal != null)
        {
            NotifySender.sendFailure(player, refusal);
            return;
        }

        if (!level.removeBlock(event.getPos(), false))
        {
            return;
        }

        useFromBreak(player, level, event.getPos(), event.getState(), dice.devils());
    }

    private static boolean isModifierHeld(ServerPlayer player)
    {
        return MODIFIER.containsKey(player.getUUID());
    }

    /*
     * The use is charged from the inventory like any other dice use, so the die goes there first. A break that triggers
     * a use never spawns a drop item: when the inventory has no room the block simply goes back where it was.
     */
    private static void useFromBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState broken,
            boolean devils)
    {
        ItemStack dice = new ItemStack(devils ? ModItems.DEVILS_DICE.get() : ModItems.GAMBLERS_DICE.get());

        if (!player.getInventory().add(dice))
        {
            level.setBlockAndUpdate(pos, broken);
            return;
        }

        /* In range the devil's die opens the altar screen, so the die waits in the inventory for the offering grid. */
        if (devils && AltarService.governingValue(player) >= 0L)
        {
            ModNetwork.sendToPlayer(player, new PlacedDiceUsePacket(PlacedDiceUsePacket.DEVILS_ALTAR));
            return;
        }

        ModNetwork.sendToPlayer(player, new PlacedDiceUsePacket(devils
                ? PlacedDiceUsePacket.DEVILS_DRAW : PlacedDiceUsePacket.GAMBLERS_DRAW));
    }
}
