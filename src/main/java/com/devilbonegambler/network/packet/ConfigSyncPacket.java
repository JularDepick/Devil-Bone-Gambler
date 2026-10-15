/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.client.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to client: one config domain was written by some admin, refresh the local copy.
 */
public class ConfigSyncPacket
{
    private static final int MAX_PAYLOAD = 262144;

    private final String domain;
    private final int revision;
    private final String payload;

    public ConfigSyncPacket(String domain, int revision, String payload)
    {
        this.domain = domain == null ? "" : domain;
        this.revision = revision;
        this.payload = payload == null ? "{}" : payload;
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

    public static void encode(ConfigSyncPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(message.domain, 32);
        buffer.writeVarInt(message.revision);
        buffer.writeUtf(message.payload, MAX_PAYLOAD);
    }

    public static ConfigSyncPacket decode(FriendlyByteBuf buffer)
    {
        return new ConfigSyncPacket(buffer.readUtf(32), buffer.readVarInt(), buffer.readUtf(MAX_PAYLOAD));
    }

    public static void handle(ConfigSyncPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleConfigSync(message)));
        context.setPacketHandled(true);
    }
}
