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
 * Client to server: refresh the creature list of the open altar screen.
 * The screen asks again on a fixed interval while it is open, because the server only pushes when the list changed.
 */
public class MobListRequestPacket
{
    private final int sequence;

    public MobListRequestPacket(int sequence)
    {
        this.sequence = sequence;
    }

    public int sequence()
    {
        return this.sequence;
    }

    public static void encode(MobListRequestPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
    }

    public static MobListRequestPacket decode(FriendlyByteBuf buffer)
    {
        return new MobListRequestPacket(buffer.readVarInt());
    }

    public static void handle(MobListRequestPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleMobListRequest(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
