/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.network.ServerRequestHandler;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client to server: the player asked for the bet box of one member of one dealer.
 * The box is a container menu owned by the server, so the client only states the wish and the server decides whether
 * the requester is a member of that wager before it opens anything; the target member may be the requester himself or
 * another member, and the server marks the box of another member read only.
 */
public class DealerBetOpenRequestPacket
{
    private final int sequence;
    private final int dealerEntityId;
    private final UUID targetId;

    public DealerBetOpenRequestPacket(int sequence, int dealerEntityId, UUID targetId)
    {
        this.sequence = sequence;
        this.dealerEntityId = dealerEntityId;
        this.targetId = targetId;
    }

    public int sequence()
    {
        return this.sequence;
    }

    public int dealerEntityId()
    {
        return this.dealerEntityId;
    }

    /**
     * Member whose box is asked for; the requester himself when the panel cell of the own row was clicked.
     */
    public UUID targetId()
    {
        return this.targetId;
    }

    public static void encode(DealerBetOpenRequestPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
        buffer.writeVarInt(message.dealerEntityId);
        buffer.writeUUID(message.targetId);
    }

    public static DealerBetOpenRequestPacket decode(FriendlyByteBuf buffer)
    {
        return new DealerBetOpenRequestPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readUUID());
    }

    public static void handle(DealerBetOpenRequestPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleDealerBetOpenRequest(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
