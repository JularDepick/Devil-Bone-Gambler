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
 * Server to client: the outcome of one draw, paired with the request sequence.
 * The result text itself is sent by the server through the configured display mode.
 */
public class DrawResultPacket
{
    private final int sequence;
    private final boolean success;
    private final String reasonKey;
    private final String itemId;
    private final int count;

    public DrawResultPacket(int sequence, boolean success, String reasonKey, String itemId, int count)
    {
        this.sequence = sequence;
        this.success = success;
        this.reasonKey = reasonKey == null ? "" : reasonKey;
        this.itemId = itemId == null ? "" : itemId;
        this.count = count;
    }

    public int sequence()
    {
        return this.sequence;
    }

    public boolean success()
    {
        return this.success;
    }

    public String reasonKey()
    {
        return this.reasonKey;
    }

    public String itemId()
    {
        return this.itemId;
    }

    public int count()
    {
        return this.count;
    }

    public static void encode(DrawResultPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
        buffer.writeBoolean(message.success);
        buffer.writeUtf(message.reasonKey, 64);
        buffer.writeUtf(message.itemId, 128);
        buffer.writeVarInt(message.count);
    }

    public static DrawResultPacket decode(FriendlyByteBuf buffer)
    {
        int sequence = buffer.readVarInt();
        boolean success = buffer.readBoolean();
        String reasonKey = buffer.readUtf(64);
        String itemId = buffer.readUtf(128);
        int count = buffer.readVarInt();
        return new DrawResultPacket(sequence, success, reasonKey, itemId, count);
    }

    public static void handle(DrawResultPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleDrawResult(message)));
        context.setPacketHandled(true);
    }
}
