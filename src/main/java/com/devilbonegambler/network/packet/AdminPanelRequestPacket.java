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
 * Client to server: request the admin config panel payload.
 */
public class AdminPanelRequestPacket
{
    private final int sequence;

    public AdminPanelRequestPacket(int sequence)
    {
        this.sequence = sequence;
    }

    public int sequence()
    {
        return this.sequence;
    }

    public static void encode(AdminPanelRequestPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
    }

    public static AdminPanelRequestPacket decode(FriendlyByteBuf buffer)
    {
        return new AdminPanelRequestPacket(buffer.readVarInt());
    }

    public static void handle(AdminPanelRequestPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleAdminPanelRequest(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
