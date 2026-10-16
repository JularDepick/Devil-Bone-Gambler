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
 * Client to server: one bet box move.
 * The move is described by item and count instead of by inventory slot, because the panel fills the box from the
 * cursor: the client says what it wants to move and the server resolves it against the real inventory.
 */
public class DealerBetMovePacket
{
    public static final int ACTION_TO_BOX = 0;
    public static final int ACTION_TO_INVENTORY = 1;

    /** Any cell, used when the player did not click one specific cell. */
    public static final int ANY_INDEX = -1;

    public static final int MAX_COUNT = 64;
    private static final int MAX_ITEM_ID = 128;

    private final int dealerEntityId;
    private final int action;
    private final int index;
    private final String itemId;
    private final int count;

    public DealerBetMovePacket(int dealerEntityId, int action, int index, String itemId, int count)
    {
        this.dealerEntityId = dealerEntityId;
        this.action = action;
        this.index = index;
        this.itemId = itemId == null ? "" : itemId;
        this.count = Math.max(0, Math.min(MAX_COUNT, count));
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

    public String itemId()
    {
        return this.itemId;
    }

    public int count()
    {
        return this.count;
    }

    public static void encode(DealerBetMovePacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.dealerEntityId);
        buffer.writeVarInt(message.action);
        buffer.writeVarInt(message.index);
        buffer.writeUtf(message.itemId, MAX_ITEM_ID);
        buffer.writeVarInt(message.count);
    }

    public static DealerBetMovePacket decode(FriendlyByteBuf buffer)
    {
        return new DealerBetMovePacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readUtf(MAX_ITEM_ID), buffer.readVarInt());
    }

    public static void handle(DealerBetMovePacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleDealerBetMove(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
