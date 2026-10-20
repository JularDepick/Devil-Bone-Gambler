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
 * Server to client mirror of the subjective behaviour state. The client learns whether this round already saw a
 * behaviour and what the current variable is, and learns when the round reset releases the trigger.
 */
public class BehaviorStatePacket
{
    private final boolean triggered;
    private final int value;

    public BehaviorStatePacket(boolean triggered, int value)
    {
        this.triggered = triggered;
        this.value = value;
    }

    public boolean triggered()
    {
        return this.triggered;
    }

    public int value()
    {
        return this.value;
    }

    public static void encode(BehaviorStatePacket message, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(message.triggered);
        buffer.writeVarInt(message.value);
    }

    public static BehaviorStatePacket decode(FriendlyByteBuf buffer)
    {
        return new BehaviorStatePacket(buffer.readBoolean(), buffer.readVarInt());
    }

    public static void handle(BehaviorStatePacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleBehaviorState(message)));
        context.setPacketHandled(true);
    }
}
