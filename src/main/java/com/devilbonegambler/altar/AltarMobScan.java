/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.altar;

import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.MobListPacket;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.OfferEligibility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Server side scan behind the creature list of the altar screen.
 * The list always describes the range of the altar that governs the player, and only identifiers and display data
 * travel: the server re-locates every creature when an offering is submitted, so a client can never name a creature
 * that is not really there.
 */
public final class AltarMobScan
{
    private AltarMobScan()
    {
    }

    /**
     * Scans the governing altar range and pushes the rows to the asking player.
     */
    public static void send(ServerPlayer player)
    {
        if (player == null)
        {
            return;
        }

        ModNetwork.sendToPlayer(player, new MobListPacket(scan(player)));
    }

    private static List<MobListPacket.MobEntry> scan(ServerPlayer player)
    {
        List<MobListPacket.MobEntry> entries = new ArrayList<MobListPacket.MobEntry>();
        Vec3 centre = AltarService.governingCentre(player);
        long value = AltarService.governingValue(player);

        if (centre == null || value < 0L)
        {
            return entries;
        }

        int radius = ModConstants.altarRadiusFor(value);
        ServerLevel level = player.serverLevel();
        AABB area = new AABB(centre, centre).inflate(radius);
        double limit = (double) radius * (double) radius;
        List<LivingEntity> found = new ArrayList<LivingEntity>();

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> !(candidate instanceof Player) && candidate.isAlive()
                        && !OfferEligibility.excluded(candidate)))
        {
            if (entity.distanceToSqr(centre) <= limit)
            {
                found.add(entity);
            }
        }

        found.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(centre)));

        for (LivingEntity entity : found)
        {
            if (entries.size() >= ModConstants.ALTAR_MAX_MOB_ENTRIES)
            {
                break;
            }

            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

            if (key == null)
            {
                continue;
            }

            entries.add(new MobListPacket.MobEntry(entity.getUUID(), key.toString(), entity.getDisplayName(),
                    entity.getHealth(), entity.getMaxHealth(), false));
        }

        return entries;
    }
}
