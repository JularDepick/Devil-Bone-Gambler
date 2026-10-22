/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.network.ServerRequestHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client to server: announces one config save, which is then delivered in parts by {@link ConfigPayloadPacket}.
 *
 * The announcement carries the revision the client edited and how many parts follow, so the server can reject the
 * save on permissions or a stale revision before the bulk arrives.
 */
public class ConfigWriteRequestPacket
{
    private final int sequence;
    private final String domain;
    private final int revision;
    private final int partCount;

    public ConfigWriteRequestPacket(int sequence, String domain, int revision, int partCount)
    {
        this.sequence = sequence;
        this.domain = domain == null ? "" : domain;
        this.revision = revision;
        this.partCount = partCount;
    }

    public int sequence()
    {
        return this.sequence;
    }

    public String domain()
    {
        return this.domain;
    }

    public int revision()
    {
        return this.revision;
    }

    public int partCount()
    {
        return this.partCount;
    }

    public static void encode(ConfigWriteRequestPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
        buffer.writeUtf(message.domain, 32);
        buffer.writeVarInt(message.revision);
        buffer.writeVarInt(message.partCount);
    }

    public static ConfigWriteRequestPacket decode(FriendlyByteBuf buffer)
    {
        int sequence = buffer.readVarInt();
        String domain = buffer.readUtf(32);
        int revision = buffer.readVarInt();
        int partCount = buffer.readVarInt();
        return new ConfigWriteRequestPacket(sequence, domain, revision, partCount);
    }

    public static void handle(ConfigWriteRequestPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleConfigWrite(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
