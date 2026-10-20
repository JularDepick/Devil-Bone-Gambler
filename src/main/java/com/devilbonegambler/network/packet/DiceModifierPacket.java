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
 * Client to server: whether the placement key is held right now. The server needs it when a placed die is broken
 * with an empty hand, because the same break either destroys the block or performs the normal die use.
 */
public class DiceModifierPacket
{
    private final boolean held;

    public DiceModifierPacket(boolean held)
    {
        this.held = held;
    }

    public boolean held()
    {
        return this.held;
    }

    public static void encode(DiceModifierPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(message.held);
    }

    public static DiceModifierPacket decode(FriendlyByteBuf buffer)
    {
        return new DiceModifierPacket(buffer.readBoolean());
    }

    public static void handle(DiceModifierPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleDiceModifier(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
