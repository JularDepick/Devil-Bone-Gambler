/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.altar;

import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * Turns a pile of gameplay manuals dropped inside an altar range into dice.
 *
 * A player receives one gameplay manual on every join and respawn, so the copies pile up with nothing useful to do
 * with them. When at least {@link ModConstants#MANUAL_CONVERSION_MIN_COUNT} manuals lie inside an altar range at the
 * same time, the altar consumes them and drops the same number of dice, split between the two kinds at a random ratio
 * so a pile never converts to a predictable mix.
 *
 * Only gameplay manuals take part: the administrator and creator manuals follow their holder and are never consumed.
 */
public final class AltarManualConversion
{
    private AltarManualConversion()
    {
    }

    public static void onServerTick(MinecraftServer server)
    {
        for (ServerLevel level : server.getAllLevels())
        {
            convert(level);
        }
    }

    private static void convert(ServerLevel level)
    {
        List<ItemEntity> manuals = new ArrayList<ItemEntity>();
        int total = 0;

        for (Entity entity : level.getEntities().getAll())
        {
            if (!(entity instanceof ItemEntity item) || !item.isAlive())
            {
                continue;
            }

            if (!item.getItem().is(ModItems.GAMEPLAY_MANUAL.get()))
            {
                continue;
            }

            manuals.add(item);
            total += item.getItem().getCount();
        }

        if (total < ModConstants.MANUAL_CONVERSION_MIN_COUNT)
        {
            return;
        }

        List<ItemEntity> inside = new ArrayList<ItemEntity>();
        int insideCount = 0;
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;

        for (ItemEntity item : manuals)
        {
            if (!AltarService.rangeSource(level, item.blockPosition(), null).covers())
            {
                continue;
            }

            inside.add(item);
            insideCount += item.getItem().getCount();
            x += item.getX();
            y += item.getY();
            z += item.getZ();
        }

        if (insideCount < ModConstants.MANUAL_CONVERSION_MIN_COUNT)
        {
            return;
        }

        double span = ModConstants.MANUAL_CONVERSION_RATIO_MAX - ModConstants.MANUAL_CONVERSION_RATIO_MIN;
        double ratio = ModConstants.MANUAL_CONVERSION_RATIO_MIN + level.random.nextDouble() * span;
        int gamblers = Mth.clamp((int) Math.round(insideCount * ratio), 1, insideCount - 1);
        int devils = insideCount - gamblers;
        Vec3 centre = new Vec3(x / inside.size(), y / inside.size(), z / inside.size());
        BlockPos pos = BlockPos.containing(centre);

        for (ItemEntity item : inside)
        {
            item.discard();
        }

        if (gamblers > 0)
        {
            Block.popResource(level, pos, new ItemStack(ModItems.GAMBLERS_DICE.get(), gamblers));
        }

        if (devils > 0)
        {
            Block.popResource(level, pos, new ItemStack(ModItems.DEVILS_DICE.get(), devils));
        }

        for (ServerPlayer player : level.players())
        {
            if (player.distanceToSqr(centre) <= 64.0D)
            {
                NotifySender.sendChat(player, "gui." + ModConstants.MOD_ID + ".altar.manual_conversion",
                        Integer.valueOf(insideCount), Integer.valueOf(gamblers), Integer.valueOf(devils));
            }
        }
    }
}
