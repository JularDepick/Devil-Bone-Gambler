/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.network.ServerRequestHandler;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client to server: the held die is placed as a block instead of being used, because the placement key is held.
 */
public class PlaceDicePacket
{
    private final BlockPos pos;
    private final Direction face;
    private final boolean offhand;

    public PlaceDicePacket(BlockPos pos, Direction face, boolean offhand)
    {
        this.pos = pos;
        this.face = face;
        this.offhand = offhand;
    }

    public BlockPos pos()
    {
        return this.pos;
    }

    public Direction face()
    {
        return this.face;
    }

    public boolean offhand()
    {
        return this.offhand;
    }

    public static void encode(PlaceDicePacket message, FriendlyByteBuf buffer)
    {
        buffer.writeBlockPos(message.pos);
        buffer.writeEnum(message.face);
        buffer.writeBoolean(message.offhand);
    }

    public static PlaceDicePacket decode(FriendlyByteBuf buffer)
    {
        return new PlaceDicePacket(buffer.readBlockPos(), buffer.readEnum(Direction.class), buffer.readBoolean());
    }

    public static void handle(PlaceDicePacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handlePlaceDice(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
