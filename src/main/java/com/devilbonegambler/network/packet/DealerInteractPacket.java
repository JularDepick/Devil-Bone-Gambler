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
 * Client to server: one dealer panel intent, carried by the dealer entity id.
 * The action only states what the player wants, every rule is checked again on the server.
 */
public class DealerInteractPacket
{
    public static final int ACTION_OPEN = 0;
    public static final int ACTION_CREATE = 1;
    public static final int ACTION_START = 2;
    public static final int ACTION_LEAVE = 3;

    private final int dealerEntityId;
    private final int action;

    public DealerInteractPacket(int dealerEntityId, int action)
    {
        this.dealerEntityId = dealerEntityId;
        this.action = action;
    }

    public int dealerEntityId()
    {
        return this.dealerEntityId;
    }

    public int action()
    {
        return this.action;
    }

    public static void encode(DealerInteractPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.dealerEntityId);
        buffer.writeVarInt(message.action);
    }

    public static DealerInteractPacket decode(FriendlyByteBuf buffer)
    {
        return new DealerInteractPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(DealerInteractPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleDealerInteract(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
