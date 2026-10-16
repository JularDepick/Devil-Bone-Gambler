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
 * Client to server: open the altar container.
 * Only the server opens it, because the offering container of a player lives on the server and the altar range is
 * re-checked there before the screen is handed out.
 */
public class AltarOpenRequestPacket
{
    private final int sequence;

    public AltarOpenRequestPacket(int sequence)
    {
        this.sequence = sequence;
    }

    public int sequence()
    {
        return this.sequence;
    }

    public static void encode(AltarOpenRequestPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
    }

    public static AltarOpenRequestPacket decode(FriendlyByteBuf buffer)
    {
        return new AltarOpenRequestPacket(buffer.readVarInt());
    }

    public static void handle(AltarOpenRequestPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleAltarOpenRequest(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
