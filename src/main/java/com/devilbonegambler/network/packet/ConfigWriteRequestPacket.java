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
 * Client to server: submit one config domain with the revision the client edited.
 */
public class ConfigWriteRequestPacket
{
    private static final int MAX_PAYLOAD = 262144;

    private final int sequence;
    private final String domain;
    private final int revision;
    private final String payload;

    public ConfigWriteRequestPacket(int sequence, String domain, int revision, String payload)
    {
        this.sequence = sequence;
        this.domain = domain;
        this.revision = revision;
        this.payload = payload == null ? "{}" : payload;
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

    public String payload()
    {
        return this.payload;
    }

    public static void encode(ConfigWriteRequestPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
        buffer.writeUtf(message.domain, 32);
        buffer.writeVarInt(message.revision);
        buffer.writeUtf(message.payload, MAX_PAYLOAD);
    }

    public static ConfigWriteRequestPacket decode(FriendlyByteBuf buffer)
    {
        int sequence = buffer.readVarInt();
        String domain = buffer.readUtf(32);
        int revision = buffer.readVarInt();
        String payload = buffer.readUtf(MAX_PAYLOAD);
        return new ConfigWriteRequestPacket(sequence, domain, revision, payload);
    }

    public static void handle(ConfigWriteRequestPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleConfigWrite(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
