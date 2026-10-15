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
 * Server to client: the balances, the asset counters and the VIP state of one player,
 * plus the two economy config domains the screens need.
 */
public class EconomyDataPacket
{
    private static final int MAX_PAYLOAD = 262144;

    private final long coin;
    private final long tear;
    private final long counterCoin;
    private final long counterTear;
    private final long vipUntil;
    private final int pendingCount;
    private final int tickCount;
    private final String shopJson;
    private final String economyJson;

    public EconomyDataPacket(long coin, long tear, long counterCoin, long counterTear, long vipUntil, int pendingCount,
            int tickCount, String shopJson, String economyJson)
    {
        this.coin = coin;
        this.tear = tear;
        this.counterCoin = counterCoin;
        this.counterTear = counterTear;
        this.vipUntil = vipUntil;
        this.pendingCount = pendingCount;
        this.tickCount = tickCount;
        this.shopJson = shopJson == null ? "{}" : shopJson;
        this.economyJson = economyJson == null ? "{}" : economyJson;
    }

    public long coin() { return this.coin; }

    public long tear() { return this.tear; }

    public long counterCoin() { return this.counterCoin; }

    public long counterTear() { return this.counterTear; }

    public long vipUntil() { return this.vipUntil; }

    public int pendingCount() { return this.pendingCount; }

    public int tickCount() { return this.tickCount; }

    public String shopJson() { return this.shopJson; }

    public String economyJson() { return this.economyJson; }

    public static void encode(EconomyDataPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarLong(message.coin);
        buffer.writeVarLong(message.tear);
        buffer.writeVarLong(message.counterCoin);
        buffer.writeVarLong(message.counterTear);
        buffer.writeVarLong(message.vipUntil);
        buffer.writeVarInt(message.pendingCount);
        buffer.writeVarInt(message.tickCount);
        buffer.writeUtf(message.shopJson, MAX_PAYLOAD);
        buffer.writeUtf(message.economyJson, MAX_PAYLOAD);
    }

    public static EconomyDataPacket decode(FriendlyByteBuf buffer)
    {
        long coin = buffer.readVarLong();
        long tear = buffer.readVarLong();
        long counterCoin = buffer.readVarLong();
        long counterTear = buffer.readVarLong();
        long vipUntil = buffer.readVarLong();
        int pendingCount = buffer.readVarInt();
        int tickCount = buffer.readVarInt();
        String shopJson = buffer.readUtf(MAX_PAYLOAD);
        String economyJson = buffer.readUtf(MAX_PAYLOAD);
        return new EconomyDataPacket(coin, tear, counterCoin, counterTear, vipUntil, pendingCount, tickCount, shopJson,
                economyJson);
    }

    public static void handle(EconomyDataPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleEconomyData(message)));
        context.setPacketHandled(true);
    }
}
