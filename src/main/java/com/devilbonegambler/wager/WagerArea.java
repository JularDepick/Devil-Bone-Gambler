/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.wager;

import com.devilbonegambler.util.ModConstants;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Immutable wager zone: a sphere around the initiator whose centre follows the initiator every tick.
 * The zone is a pure server side predicate and never produces any world change.
 * The dimension is not part of the value object, so the owner of the zone has to compare dimensions itself.
 */
public final class WagerArea
{
    private final UUID ownerId;
    private final Vec3 center;

    public WagerArea(UUID ownerId, Vec3 center)
    {
        this.ownerId = ownerId;
        this.center = center;
    }

    /**
     * Zone centred on the current position of the given player.
     */
    public static WagerArea around(ServerPlayer player)
    {
        return new WagerArea(player.getUUID(), player.position());
    }

    public UUID ownerId()
    {
        return this.ownerId;
    }

    public Vec3 center()
    {
        return this.center;
    }

    /**
     * Sphere test on the full three dimensional distance, never on the horizontal distance alone.
     */
    public boolean contains(ServerPlayer player)
    {
        return player != null && contains(player.position());
    }

    public boolean contains(Vec3 position)
    {
        if (position == null)
        {
            return false;
        }

        double radius = ModConstants.WAGER_RADIUS;
        return position.distanceToSqr(this.center) <= radius * radius;
    }

    /**
     * The zone is immutable, so following the initiator returns a new instance instead of mutating this one.
     */
    public WagerArea refresh(ServerPlayer player)
    {
        return new WagerArea(this.ownerId, player.position());
    }
}
