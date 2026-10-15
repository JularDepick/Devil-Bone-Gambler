/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.draw.Offering;
import com.devilbonegambler.draw.OfferingBundle;
import com.devilbonegambler.network.ServerRequestHandler;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client to server: sacrifice draw request of the devil's dice.
 * The payload carries item offerings, mob offerings and numeric offerings, and the server re-validates all three.
 */
public class DevilsDiceRequestPacket
{
    private final int sequence;
    private final String playerName;
    private final String itemId;
    private final OfferingBundle bundle;

    public DevilsDiceRequestPacket(int sequence, String playerName, String itemId, OfferingBundle bundle)
    {
        this.sequence = sequence;
        this.playerName = playerName;
        this.itemId = itemId;
        this.bundle = bundle == null ? OfferingBundle.empty() : bundle;
    }

    public int sequence()
    {
        return this.sequence;
    }

    public String playerName()
    {
        return this.playerName;
    }

    public String itemId()
    {
        return this.itemId;
    }

    public OfferingBundle bundle()
    {
        return this.bundle;
    }

    public static void encode(DevilsDiceRequestPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.sequence);
        buffer.writeUtf(message.playerName);
        buffer.writeUtf(message.itemId);

        List<Offering> items = message.bundle.items();
        buffer.writeVarInt(items.size());

        for (Offering offering : items)
        {
            buffer.writeUtf(offering.itemId());
            buffer.writeVarInt(offering.count());
            buffer.writeVarInt(offering.damage());
            buffer.writeVarInt(offering.enchantLevels());
        }

        List<UUID> mobs = message.bundle.mobs();
        buffer.writeVarInt(mobs.size());

        for (UUID mob : mobs)
        {
            buffer.writeUUID(mob);
        }

        buffer.writeVarInt(message.bundle.health());
        buffer.writeVarInt(message.bundle.food());
        buffer.writeBoolean(message.bundle.allHealth());
        buffer.writeBoolean(message.bundle.deathDrop());
    }

    public static DevilsDiceRequestPacket decode(FriendlyByteBuf buffer)
    {
        int sequence = buffer.readVarInt();
        String playerName = buffer.readUtf(64);
        String itemId = buffer.readUtf(128);
        int itemCount = Math.min(buffer.readVarInt(), ModConstants.MAX_TOTAL_ENTRIES);
        List<Offering> items = new ArrayList<Offering>(itemCount);

        for (int index = 0; index < itemCount; index++)
        {
            String offeringId = buffer.readUtf(128);
            int offeringCount = buffer.readVarInt();
            int damage = buffer.readVarInt();
            int enchantLevels = buffer.readVarInt();
            items.add(new Offering(offeringId, offeringCount, damage, enchantLevels));
        }

        int mobCount = Math.min(buffer.readVarInt(), ModConstants.ALTAR_MAX_MOB_ENTRIES);
        List<UUID> mobs = new ArrayList<UUID>(mobCount);

        for (int index = 0; index < mobCount; index++)
        {
            mobs.add(buffer.readUUID());
        }

        int health = buffer.readVarInt();
        int food = buffer.readVarInt();
        boolean allHealth = buffer.readBoolean();
        boolean deathDrop = buffer.readBoolean();

        return new DevilsDiceRequestPacket(sequence, playerName, itemId,
                new OfferingBundle(items, mobs, health, food, allHealth, deathDrop));
    }

    public static void handle(DevilsDiceRequestPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerRequestHandler.handleDevilsDice(context.getSender(), message));
        context.setPacketHandled(true);
    }
}
