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
 * Server to client: a request to open the searchable value table view.
 * The rows come from the snapshot that was pushed earlier, so the packet carries no data of its own.
 */
public class OpenValueTablePacket
{
    public OpenValueTablePacket()
    {
    }

    public static void encode(OpenValueTablePacket message, FriendlyByteBuf buffer)
    {
    }

    public static OpenValueTablePacket decode(FriendlyByteBuf buffer)
    {
        return new OpenValueTablePacket();
    }

    public static void handle(OpenValueTablePacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleOpenValueTable(message)));
        context.setPacketHandled(true);
    }
}
