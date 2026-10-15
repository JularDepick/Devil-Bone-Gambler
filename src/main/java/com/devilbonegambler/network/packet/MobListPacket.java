/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.client.ClientPacketHandler;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to client: the mobs currently inside the altar range, pushed while the altar screen is open.
 * Only identifiers and display data travel; the server re-locates every mob when an offering is submitted.
 */
public class MobListPacket
{
    private final List<MobEntry> entries;

    public MobListPacket(List<MobEntry> entries)
    {
        this.entries = entries == null ? new ArrayList<MobEntry>() : entries;
    }

    public List<MobEntry> entries()
    {
        return this.entries;
    }

    public static void encode(MobListPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.entries.size());

        for (MobEntry entry : message.entries)
        {
            buffer.writeUUID(entry.id());
            buffer.writeUtf(entry.typeId(), 128);
            buffer.writeComponent(entry.name());
            buffer.writeFloat(entry.health());
            buffer.writeFloat(entry.maxHealth());
            buffer.writeBoolean(entry.selected());
        }
    }

    public static MobListPacket decode(FriendlyByteBuf buffer)
    {
        int size = Math.min(buffer.readVarInt(), ModConstants.ALTAR_MAX_MOB_ENTRIES);
        List<MobEntry> entries = new ArrayList<MobEntry>(size);

        for (int index = 0; index < size; index++)
        {
            entries.add(new MobEntry(buffer.readUUID(), buffer.readUtf(128), buffer.readComponent(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readBoolean()));
        }

        return new MobListPacket(entries);
    }

    public static void handle(MobListPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleMobList(message)));
        context.setPacketHandled(true);
    }

    /**
     * One mob row of the altar screen.
     */
    public static final class MobEntry
    {
        private final UUID id;
        private final String typeId;
        private final net.minecraft.network.chat.Component name;
        private final float health;
        private final float maxHealth;
        private final boolean selected;

        public MobEntry(UUID id, String typeId, net.minecraft.network.chat.Component name, float health, float maxHealth,
                boolean selected)
        {
            this.id = id;
            this.typeId = typeId;
            this.name = name;
            this.health = health;
            this.maxHealth = maxHealth;
            this.selected = selected;
        }

        public UUID id()
        {
            return this.id;
        }

        public String typeId()
        {
            return this.typeId;
        }

        public net.minecraft.network.chat.Component name()
        {
            return this.name;
        }

        public float health()
        {
            return this.health;
        }

        public float maxHealth()
        {
            return this.maxHealth;
        }

        public boolean selected()
        {
            return this.selected;
        }
    }
}
