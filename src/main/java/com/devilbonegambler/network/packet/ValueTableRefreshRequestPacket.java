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
 * Client to server: refresh the value table snapshot of the held book.
 */
public class ValueTableRefreshRequestPacket
{
    private final int sequence;

    public ValueTableRefreshRequestPacket(int sequence)
    {
        this.sequence = sequence;
    }

    public int sequence()
    {
        return this.sequence;
    }

    public static void encode(ValueTableRefreshRequestPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
    }

    public static ValueTableRefreshRequestPacket decode(FriendlyByteBuf buffer)
    {
        return new ValueTableRefreshRequestPacket(buffer.readVarInt());
    }

    public static void handle(ValueTableRefreshRequestPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleValueTableRefresh(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
