/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.wager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Mutable state of one wager: the zone, the ordered zone list, the joined sub sequence, the notified set,
 * the start tick and the phase.
 * The zone list is the index source and keeps its order for the whole wager, so the joined list stays a
 * sub sequence of the zone list at every moment.
 */
public final class WagerSession
{
    /**
     * Counting down covers the fixed countdown, running means the dice have been rolled already.
     */
    public enum Phase
    {
        COUNTDOWN,
        RUNNING
    }

    private final UUID ownerId;
    private final List<UUID> areaPlayers;
    private final Set<UUID> joinedIds = new LinkedHashSet<UUID>();
    private final List<UUID> joinedPlayers = new ArrayList<UUID>();
    private final Set<UUID> notified = new LinkedHashSet<UUID>();
    private final long startTick;
    private WagerArea area;
    private Phase phase = Phase.COUNTDOWN;
    private int lastCountdownSecond;

    public WagerSession(UUID ownerId, WagerArea area, List<UUID> areaPlayers, long startTick, int countdownSeconds)
    {
        this.ownerId = ownerId;
        this.area = area;
        this.areaPlayers = new ArrayList<UUID>(areaPlayers);
        this.startTick = startTick;
        this.lastCountdownSecond = countdownSeconds;
    }

    public UUID ownerId()
    {
        return this.ownerId;
    }

    public WagerArea area()
    {
        return this.area;
    }

    public void setArea(WagerArea area)
    {
        this.area = area;
    }

    /**
     * Ordered zone list: the initiator is at index zero, every later joiner is appended at the end.
     */
    public List<UUID> areaPlayers()
    {
        return this.areaPlayers;
    }

    /**
     * Ordered joined list, always a sub sequence of the zone list in zone list order.
     */
    public List<UUID> joinedPlayers()
    {
        return this.joinedPlayers;
    }

    public Set<UUID> notified()
    {
        return this.notified;
    }

    public long startTick()
    {
        return this.startTick;
    }

    public Phase phase()
    {
        return this.phase;
    }

    public void setPhase(Phase phase)
    {
        this.phase = phase;
    }

    public int lastCountdownSecond()
    {
        return this.lastCountdownSecond;
    }

    public void setLastCountdownSecond(int second)
    {
        this.lastCountdownSecond = second;
    }

    public boolean isInAreaList(UUID playerId)
    {
        return this.areaPlayers.contains(playerId);
    }

    public boolean isJoined(UUID playerId)
    {
        return this.joinedIds.contains(playerId);
    }

    /**
     * Returns true only when the player was not part of the zone list yet.
     */
    public boolean addAreaPlayer(UUID playerId)
    {
        if (this.areaPlayers.contains(playerId))
        {
            return false;
        }

        this.areaPlayers.add(playerId);
        return true;
    }

    /**
     * Adds the player to the joined list, appending the player to the zone list when needed.
     */
    public void join(UUID playerId)
    {
        addAreaPlayer(playerId);
        this.joinedIds.add(playerId);
        refreshJoinedPlayers();
    }

    public void leave(UUID playerId)
    {
        this.joinedIds.remove(playerId);
        refreshJoinedPlayers();
    }

    /**
     * Returns true only when the player received the creation notice for the first time.
     */
    public boolean markNotified(UUID playerId)
    {
        return this.notified.add(playerId);
    }

    public boolean isNotified(UUID playerId)
    {
        return this.notified.contains(playerId);
    }

    private void refreshJoinedPlayers()
    {
        this.joinedPlayers.clear();

        for (UUID playerId : this.areaPlayers)
        {
            if (this.joinedIds.contains(playerId))
            {
                this.joinedPlayers.add(playerId);
            }
        }
    }
}
