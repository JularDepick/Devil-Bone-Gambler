/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.economy.EconomyOperation;
import com.devilbonegambler.economy.CurrencyType;
import com.devilbonegambler.network.EconomyRequestHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client to server: one economy operation.
 * The payload is generic and interpreted per operation type; the server re-validates every field.
 */
public class EconomyRequestPacket
{
    private static final int MAX_TEXT = 128;

    private final int sequence;
    private final EconomyOperation operation;
    private final CurrencyType currency;
    private final long amount;
    private final long amountB;
    private final int count;
    private final String textA;
    private final String textB;

    public EconomyRequestPacket(int sequence, EconomyOperation operation, CurrencyType currency, long amount,
            long amountB, int count, String textA, String textB)
    {
        this.sequence = sequence;
        this.operation = operation;
        this.currency = currency;
        this.amount = amount;
        this.amountB = amountB;
        this.count = count;
        this.textA = textA == null ? "" : textA;
        this.textB = textB == null ? "" : textB;
    }

    public int sequence() { return this.sequence; }

    public EconomyOperation operation() { return this.operation; }

    public CurrencyType currency() { return this.currency; }

    public long amount() { return this.amount; }

    public long amountB() { return this.amountB; }

    public int count() { return this.count; }

    public String textA() { return this.textA; }

    public String textB() { return this.textB; }

    public static void encode(EconomyRequestPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
        buffer.writeVarInt(message.operation.ordinal());
        buffer.writeVarInt(message.currency.ordinal());
        buffer.writeVarLong(message.amount);
        buffer.writeVarLong(message.amountB);
        buffer.writeVarInt(message.count);
        buffer.writeUtf(message.textA, MAX_TEXT);
        buffer.writeUtf(message.textB, MAX_TEXT);
    }

    public static EconomyRequestPacket decode(FriendlyByteBuf buffer)
    {
        int sequence = buffer.readVarInt();
        EconomyOperation operation = EconomyOperation.byOrdinal(buffer.readVarInt());
        CurrencyType currency = CurrencyType.values()[Math.max(0,
                Math.min(CurrencyType.count() - 1, buffer.readVarInt()))];
        long amount = buffer.readVarLong();
        long amountB = buffer.readVarLong();
        int count = buffer.readVarInt();
        String textA = buffer.readUtf(MAX_TEXT);
        String textB = buffer.readUtf(MAX_TEXT);
        return new EconomyRequestPacket(sequence, operation, currency, amount, amountB, count, textA, textB);
    }

    public static void handle(EconomyRequestPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> EconomyRequestHandler.handle(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
