/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.client.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to client: a placed die was broken by hand, so the client should behave as if the die had been used.
 * The server has already moved the die into the hand or the inventory; the client only starts the usual action.
 */
public class PlacedDiceUsePacket
{
    public static final int GAMBLERS_DRAW = 0;
    public static final int DEVILS_DRAW = 1;
    public static final int DEVILS_ALTAR = 2;

    private final int mode;

    public PlacedDiceUsePacket(int mode)
    {
        this.mode = mode;
    }

    public int mode()
    {
        return this.mode;
    }

    public static void encode(PlacedDiceUsePacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.mode);
    }

    public static PlacedDiceUsePacket decode(FriendlyByteBuf buffer)
    {
        return new PlacedDiceUsePacket(buffer.readVarInt());
    }

    public static void handle(PlacedDiceUsePacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handlePlacedDiceUse(message)));
        context.setPacketHandled(true);
    }
}
