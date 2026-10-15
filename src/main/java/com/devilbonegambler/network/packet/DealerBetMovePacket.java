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
 * Client to server: one bet box move. The action says which side the item comes from, the server moves it.
 */
public class DealerBetMovePacket
{
    public static final int FROM_INVENTORY = 0;
    public static final int FROM_BET_BOX = 1;

    private final int dealerEntityId;
    private final int action;
    private final int index;
    private final boolean wholeStack;

    public DealerBetMovePacket(int dealerEntityId, int action, int index, boolean wholeStack)
    {
        this.dealerEntityId = dealerEntityId;
        this.action = action;
        this.index = index;
        this.wholeStack = wholeStack;
    }

    public int dealerEntityId()
    {
        return this.dealerEntityId;
    }

    public int action()
    {
        return this.action;
    }

    public int index()
    {
        return this.index;
    }

    public boolean wholeStack()
    {
        return this.wholeStack;
    }

    public static void encode(DealerBetMovePacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.dealerEntityId);
        buffer.writeVarInt(message.action);
        buffer.writeVarInt(message.index);
        buffer.writeBoolean(message.wholeStack);
    }

    public static DealerBetMovePacket decode(FriendlyByteBuf buffer)
    {
        return new DealerBetMovePacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean());
    }

    public static void handle(DealerBetMovePacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleDealerBetMove(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
