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
 * Server to client: the outcome of one config write.
 */
public class ConfigWriteResponsePacket
{
    private final int sequence;
    private final String domain;
    private final int revision;
    private final boolean success;
    private final String reasonKey;

    public ConfigWriteResponsePacket(int sequence, String domain, int revision, boolean success, String reasonKey)
    {
        this.sequence = sequence;
        this.domain = domain == null ? "" : domain;
        this.revision = revision;
        this.success = success;
        this.reasonKey = reasonKey == null ? "" : reasonKey;
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

    public boolean success()
    {
        return this.success;
    }

    public String reasonKey()
    {
        return this.reasonKey;
    }

    public static void encode(ConfigWriteResponsePacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
        buffer.writeUtf(message.domain, 32);
        buffer.writeVarInt(message.revision);
        buffer.writeBoolean(message.success);
        buffer.writeUtf(message.reasonKey, 64);
    }

    public static ConfigWriteResponsePacket decode(FriendlyByteBuf buffer)
    {
        int sequence = buffer.readVarInt();
        String domain = buffer.readUtf(32);
        int revision = buffer.readVarInt();
        boolean success = buffer.readBoolean();
        String reasonKey = buffer.readUtf(64);
        return new ConfigWriteResponsePacket(sequence, domain, revision, success, reasonKey);
    }

    public static void handle(ConfigWriteResponsePacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleConfigWriteResponse(message)));
        context.setPacketHandled(true);
    }
}
