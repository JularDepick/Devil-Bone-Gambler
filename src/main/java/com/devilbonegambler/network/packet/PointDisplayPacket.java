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
 * Server to client: the rolled dice point and colour, sent before the result so that the player sees the roll first.
 */
public class PointDisplayPacket
{
    private final int sequence;
    private final int point;
    private final boolean red;
    private final int overflow;

    public PointDisplayPacket(int sequence, int point, boolean red, int overflow)
    {
        this.sequence = sequence;
        this.point = point;
        this.red = red;
        this.overflow = overflow;
    }

    public int sequence()
    {
        return this.sequence;
    }

    public int point()
    {
        return this.point;
    }

    public boolean red()
    {
        return this.red;
    }

    public int overflow()
    {
        return this.overflow;
    }

    public static void encode(PointDisplayPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
        buffer.writeVarInt(message.point);
        buffer.writeBoolean(message.red);
        buffer.writeVarInt(message.overflow);
    }

    public static PointDisplayPacket decode(FriendlyByteBuf buffer)
    {
        int sequence = buffer.readVarInt();
        int point = buffer.readVarInt();
        boolean red = buffer.readBoolean();
        int overflow = buffer.readVarInt();
        return new PointDisplayPacket(sequence, point, red, overflow);
    }

    public static void handle(PointDisplayPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handlePointDisplay(message)));
        context.setPacketHandled(true);
    }
}
