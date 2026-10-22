/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.client.ClientPacketHandler;
import com.devilbonegambler.network.ConfigChunks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * One slice of a config payload pushed from the server to the client.
 *
 * A prize pool of thousands of entries does not fit into a single packet, so the payload is cut into parts and the
 * client assembles them per domain. The save in the other direction uses {@link ConfigPayloadWritePacket}: Forge keys
 * a channel's message handlers by message class, so one class registered for both directions would only ever encode
 * the last registration and every part would arrive at the wrong handler.
 */
public class ConfigPayloadPacket
{
    private final String domain;
    private final int partIndex;
    private final int partCount;
    private final String chunk;

    public ConfigPayloadPacket(String domain, int partIndex, int partCount, String chunk)
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

    public static void encode(ConfigPayloadPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(message.domain, 32);
        buffer.writeVarInt(message.partIndex);
        buffer.writeVarInt(message.partCount);
        buffer.writeUtf(message.chunk, ConfigChunks.MAX_PACKET_CHARS);
    }

    public static ConfigPayloadPacket decode(FriendlyByteBuf buffer)
    {
        String domain = buffer.readUtf(32);
        int partIndex = buffer.readVarInt();
        int partCount = buffer.readVarInt();
        String chunk = buffer.readUtf(ConfigChunks.MAX_PACKET_CHARS);
        return new ConfigPayloadPacket(domain, partIndex, partCount, chunk);
    }

    public static void handleFromServer(ConfigPayloadPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleConfigPayload(message)));
        context.setPacketHandled(true);
    }
}
