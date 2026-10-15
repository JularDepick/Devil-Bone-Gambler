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
 * Client to server: free draw request of the gambler's dice.
 */
public class GamblersDiceRequestPacket
{
    private final int sequence;
    private final String playerName;
    private final String itemId;

    public GamblersDiceRequestPacket(int sequence, String playerName, String itemId)
    {
        this.sequence = sequence;
        this.playerName = playerName;
        this.itemId = itemId;
    }

    public int sequence()
    {
        return this.sequence;
    }

    public String playerName()
    {
        return this.playerName;
    }

    public String itemId()
    {
        return this.itemId;
    }

    public static void encode(GamblersDiceRequestPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
        buffer.writeUtf(message.playerName);
        buffer.writeUtf(message.itemId);
    }

    public static GamblersDiceRequestPacket decode(FriendlyByteBuf buffer)
    {
        return new GamblersDiceRequestPacket(buffer.readVarInt(), buffer.readUtf(64), buffer.readUtf(128));
    }

    public static void handle(GamblersDiceRequestPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleGamblersDice(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
