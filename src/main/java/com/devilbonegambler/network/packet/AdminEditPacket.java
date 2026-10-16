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
 * Client to server: one change of the admin list, requested from the admin panel by the creator.
 */
public class AdminEditPacket
{
    public static final int ACTION_ADD = 0;
    public static final int ACTION_REMOVE = 1;

    private static final int MAX_NAME = 64;

    private final int sequence;
    private final int action;
    private final String name;

    public AdminEditPacket(int sequence, int action, String name)
    {
        this.sequence = sequence;
        this.action = action;
        this.name = name == null ? "" : name;
    }

    public int sequence()
    {
        return this.sequence;
    }

    public int action()
    {
        return this.action;
    }

    public String name()
    {
        return this.name;
    }

    public static void encode(AdminEditPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
        buffer.writeVarInt(message.action);
        buffer.writeUtf(message.name, MAX_NAME);
    }

    public static AdminEditPacket decode(FriendlyByteBuf buffer)
    {
        int sequence = buffer.readVarInt();
        int action = buffer.readVarInt();
        return new AdminEditPacket(sequence, action, buffer.readUtf(MAX_NAME));
    }

    public static void handle(AdminEditPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleAdminEdit(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
