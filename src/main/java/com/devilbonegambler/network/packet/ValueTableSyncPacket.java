/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.client.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to client: the authoritative value table snapshot, pushed when a player joins so that every client
 * can show values without holding the value table item.
 */
public class ValueTableSyncPacket
{
    private final CompoundTag snapshot;

    public ValueTableSyncPacket(CompoundTag snapshot)
    {
        this.snapshot = snapshot;
    }

    public CompoundTag snapshot()
    {
        return this.snapshot;
    }

    public static void encode(ValueTableSyncPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeNbt(message.snapshot);
    }

    public static ValueTableSyncPacket decode(FriendlyByteBuf buffer)
    {
        return new ValueTableSyncPacket(buffer.readNbt());
    }

    public static void handle(ValueTableSyncPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleValueTableSync(message)));
        context.setPacketHandled(true);
    }
}
