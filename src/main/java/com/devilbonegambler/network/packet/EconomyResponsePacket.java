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
 * Server to client: the outcome of one economy operation, paired with the request sequence.
 */
public class EconomyResponsePacket
{
    private final int sequence;
    private final boolean success;
    private final String reasonKey;

    public EconomyResponsePacket(int sequence, boolean success, String reasonKey)
    {
        this.sequence = sequence;
        this.success = success;
        this.reasonKey = reasonKey == null ? "" : reasonKey;
    }

    public int sequence() { return this.sequence; }

    public boolean success() { return this.success; }

    public String reasonKey() { return this.reasonKey; }

    public static void encode(EconomyResponsePacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
        buffer.writeBoolean(message.success);
        buffer.writeUtf(message.reasonKey, 64);
    }

    public static EconomyResponsePacket decode(FriendlyByteBuf buffer)
    {
        return new EconomyResponsePacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readUtf(64));
    }

    public static void handle(EconomyResponsePacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleEconomyResponse(message)));
        context.setPacketHandled(true);
    }
}
