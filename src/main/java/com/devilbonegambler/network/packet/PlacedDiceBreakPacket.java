/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.network.ServerRequestHandler;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client to server: the player started breaking a placed die, and whether the placement key was held at that moment.
 *
 * The key state travels with the break instead of being mirrored beforehand, so the decision can never read a stale
 * mirror: holding the key breaks the die into a drop, releasing it turns the same click into a use of the die.
 */
public class PlacedDiceBreakPacket
{
    private final BlockPos pos;
    private final boolean modifierHeld;

    public PlacedDiceBreakPacket(BlockPos pos, boolean modifierHeld)
    {
        this.pos = pos;
        this.modifierHeld = modifierHeld;
    }

    public BlockPos pos()
    {
        return this.pos;
    }

    public boolean modifierHeld()
    {
        return this.modifierHeld;
    }

    public static void encode(PlacedDiceBreakPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeBlockPos(message.pos);
        buffer.writeBoolean(message.modifierHeld);
    }

    public static PlacedDiceBreakPacket decode(FriendlyByteBuf buffer)
    {
        return new PlacedDiceBreakPacket(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void handle(PlacedDiceBreakPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handlePlacedDiceBreak(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
