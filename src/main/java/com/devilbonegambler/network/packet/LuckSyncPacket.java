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
 * Server to client: the current luck value of the receiving player, sent on join and after every periodic update.
 */
public class LuckSyncPacket
{
    private final int value;

    public LuckSyncPacket(int value)
    {
        this.value = value;
    }

    public int value()
    {
        return this.value;
    }

    public static void encode(LuckSyncPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.value);
    }

    public static LuckSyncPacket decode(FriendlyByteBuf buffer)
    {
        return new LuckSyncPacket(buffer.readVarInt());
    }

    public static void handle(LuckSyncPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleLuckSync(message)));
        context.setPacketHandled(true);
    }
}
