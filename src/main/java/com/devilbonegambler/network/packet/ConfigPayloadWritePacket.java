/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.network.ConfigChunks;
import com.devilbonegambler.network.ServerRequestHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * One slice of a config save sent from the client to the server.
 *
 * It mirrors {@link ConfigPayloadPacket} on the wire but is a separate class on purpose: Forge keeps one handler per
 * message class, so registering a single class for both directions would silently drop one of them.
 */
public class ConfigPayloadWritePacket
{
    private final String domain;
    private final int partIndex;
    private final int partCount;
    private final String chunk;

    public ConfigPayloadWritePacket(String domain, int partIndex, int partCount, String chunk)
    {
        this.domain = domain == null ? "" : domain;
        this.partIndex = partIndex;
        this.partCount = partCount;
        this.chunk = chunk == null ? "" : chunk;
    }

    public String domain()
    {
        return this.domain;
    }

    public int partIndex()
    {
        return this.partIndex;
    }

    public int partCount()
    {
        return this.partCount;
    }

    public String chunk()
    {
        return this.chunk;
    }

    public static boolean isPlausible(int partIndex, int partCount)
    {
        return ConfigChunks.isPlausible(partIndex, partCount);
    }

    public static void encode(ConfigPayloadWritePacket message, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(message.domain, 32);
        buffer.writeVarInt(message.partIndex);
        buffer.writeVarInt(message.partCount);
        buffer.writeUtf(message.chunk, ConfigChunks.MAX_PACKET_CHARS);
    }

    public static ConfigPayloadWritePacket decode(FriendlyByteBuf buffer)
    {
        String domain = buffer.readUtf(32);
        int partIndex = buffer.readVarInt();
        int partCount = buffer.readVarInt();
        String chunk = buffer.readUtf(ConfigChunks.MAX_PACKET_CHARS);
        return new ConfigPayloadWritePacket(domain, partIndex, partCount, chunk);
    }

    public static void handleFromClient(ConfigPayloadWritePacket message,
            Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleConfigPayload(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
