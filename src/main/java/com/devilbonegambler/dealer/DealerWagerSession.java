/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.dealer;

import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mutable state of one dealer wager: the members in join order, their bet boxes, the recruit clock and the phase.
 * The member order is the slot order of the dealer panel, so index zero always sits on the first free slot.
 */
public final class DealerWagerSession
{
    public enum Phase
    {
        RECRUIT,
        RUNNING
    }

    private final UUID dealerId;
    private final UUID ownerId;
    private final List<UUID> members = new ArrayList<UUID>();
    private final Map<UUID, DealerBetBox> boxes = new HashMap<UUID, DealerBetBox>();
    private final Set<UUID> reminded = new HashSet<UUID>();
    private final long startTick;
    private Phase phase = Phase.RECRUIT;
    private int lastSecond = ModConstants.DEALER_WAGER_RECRUIT_SECONDS;
    private boolean pullStarted;

    public DealerWagerSession(UUID dealerId, UUID ownerId, long startTick)
    {
        this.dealerId = dealerId;
        this.ownerId = ownerId;
        this.startTick = startTick;
    }

    public UUID dealerId()
    {
        return this.dealerId;
    }

    public UUID ownerId()
    {
        return this.ownerId;
    }

    public List<UUID> members()
    {
        return this.members;
    }

    public int memberCount()
    {
        return this.members.size();
    }

    public boolean isMember(UUID playerId)
    {
        return this.members.contains(playerId);
    }

    public boolean isFull()
    {
        return this.members.size() >= ModConstants.DEALER_WAGER_MAX_PLAYERS;
    }

    /**
     * Adds a member and creates the empty bet box. Returns false when the wager is already full.
     */
    public boolean addMember(UUID playerId)
    {
        if (this.members.contains(playerId))
        {
            return true;
        }

        if (this.isFull())
        {
            return false;
        }

        this.members.add(playerId);
        this.boxes.put(playerId, new DealerBetBox());
        return true;
    }

    public boolean removeMember(UUID playerId)
    {
        this.reminded.remove(playerId);
        return this.members.remove(playerId);
    }

    public DealerBetBox box(UUID playerId)
    {
        return this.boxes.get(playerId);
    }

    public DealerBetBox takeBox(UUID playerId)
    {
        return this.boxes.remove(playerId);
    }

    public int slotOf(UUID playerId)
    {
        return this.members.indexOf(playerId);
    }

    public Phase phase()
    {
        return this.phase;
    }

    public void setPhase(Phase phase)
    {
        this.phase = phase;
    }

    public long startTick()
    {
        return this.startTick;
    }

    public int lastSecond()
    {
        return this.lastSecond;
    }

    public void setLastSecond(int second)
    {
        this.lastSecond = second;
    }

    public boolean isPullStarted()
    {
        return this.pullStarted;
    }

    public void setPullStarted(boolean pullStarted)
    {
        this.pullStarted = pullStarted;
    }

    /**
     * Returns true only the first time this member is reminded about a missing bet.
     */
    public boolean markReminded(UUID playerId)
    {
        return this.reminded.add(playerId);
    }
}
