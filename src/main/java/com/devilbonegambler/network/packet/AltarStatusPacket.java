/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.client.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to client: the nearest altar position, its accumulated offering value and whether the player is in range.
 * The client only uses this as a branch hint; the server re-checks the range before any sacrifice.
 */
public class AltarStatusPacket
{
    private final boolean hasAltar;
    private final BlockPos pos;
    private final long value;
    private final int radius;
    private final boolean inRange;

    public AltarStatusPacket(boolean hasAltar, BlockPos pos, long value, int radius, boolean inRange)
    {
        this.hasAltar = hasAltar;
        this.pos = pos == null ? BlockPos.ZERO : pos;
        this.value = value;
        this.radius = radius;
        this.inRange = inRange;
    }

    public boolean hasAltar()
    {
        return this.hasAltar;
    }

    public BlockPos pos()
    {
        return this.pos;
    }

    public long value()
    {
        return this.value;
    }

    public int radius()
    {
        return this.radius;
    }

    public boolean inRange()
    {
        return this.inRange;
    }

    public static void encode(AltarStatusPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(message.hasAltar);
        buffer.writeBlockPos(message.pos);
        buffer.writeVarLong(message.value);
        buffer.writeVarInt(message.radius);
        buffer.writeBoolean(message.inRange);
    }

    public static AltarStatusPacket decode(FriendlyByteBuf buffer)
    {
        boolean hasAltar = buffer.readBoolean();
        BlockPos pos = buffer.readBlockPos();
        long value = buffer.readVarLong();
        int radius = buffer.readVarInt();
        boolean inRange = buffer.readBoolean();
        return new AltarStatusPacket(hasAltar, pos, value, radius, inRange);
    }

    public static void handle(AltarStatusPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleAltarStatus(message)));
        context.setPacketHandled(true);
    }
}
