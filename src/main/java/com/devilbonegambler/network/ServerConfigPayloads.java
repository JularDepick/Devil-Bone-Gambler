/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network;

import com.devilbonegambler.network.packet.ConfigPayloadWritePacket;
import com.devilbonegambler.util.ModConstants;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server side config receiver: assembles the parts of one save request per player and domain, then applies it once the
 * last part is in. A new request for the same domain replaces the half finished one, so a retry never mixes two saves.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerConfigPayloads
{
    private static final Map<UUID, Map<String, Pending>> PENDING = new ConcurrentHashMap<UUID, Map<String, Pending>>();

    private ServerConfigPayloads()
    {
    }

    public static void begin(ServerPlayer player, String domain, int revision, int sequence, int partCount)
    {
        if (player == null || domain == null || !ConfigPayloadWritePacket.isPlausible(0, partCount))
        {
            return;
        }

        PENDING.computeIfAbsent(player.getUUID(), key -> new ConcurrentHashMap<String, Pending>())
                .put(domain, new Pending(revision, sequence, partCount));
    }

    public static void accept(ServerPlayer player, ConfigPayloadWritePacket message)
    {
        if (player == null)
        {
            return;
        }

        Map<String, Pending> byDomain = PENDING.get(player.getUUID());
        Pending pending = byDomain == null ? null : byDomain.get(message.domain());

        if (pending == null || pending.partCount != message.partCount()
                || !ConfigPayloadWritePacket.isPlausible(message.partIndex(), message.partCount()))
        {
            return;
        }

        pending.parts[message.partIndex()] = message.chunk();

        for (String part : pending.parts)
        {
            if (part == null)
            {
                return;
            }
        }

        byDomain.remove(message.domain());
        StringBuilder builder = new StringBuilder();

        for (String part : pending.parts)
        {
            builder.append(part);
        }

        ServerRequestHandler.applyConfigWrite(player, pending.sequence, message.domain(), pending.revision,
                builder.toString());
    }

    /**
     * A player who leaves loses the parts that never arrived. A player who joins starts from an empty buffer, so a save
     * cut off by a disconnect can not be completed by the parts of the next session.
     */
    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        PENDING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        PENDING.remove(event.getEntity().getUUID());
    }

    private static final class Pending
    {
        private final int revision;
        private final int sequence;
        private final int partCount;
        private final String[] parts;

        private Pending(int revision, int sequence, int partCount)
        {
            this.revision = revision;
            this.sequence = sequence;
            this.partCount = partCount;
            this.parts = new String[partCount];
        }
    }
}
