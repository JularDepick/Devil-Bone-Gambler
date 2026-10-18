/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.menu.DealerBetMenu;
import com.devilbonegambler.client.ClientPacketHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to client: everything the dealer panel draws, plus the own bet box of the receiver.
 * The server owns the wager, so the panel is pushed on every change instead of being computed on the client.
 */
public class DealerPanelPacket
{
    /**
     * One occupied slot of the nine grid panel: the member, the slot order and the value of the own bet.
     */
    public static final class MemberEntry
    {
        private final UUID playerId;
        private final String name;
        private final long betValue;
        private final int betCount;

        public MemberEntry(UUID playerId, String name, long betValue, int betCount)
        {
            this.playerId = playerId;
            this.name = name;
            this.betValue = betValue;
            this.betCount = betCount;
        }

        public UUID playerId()
        {
            return this.playerId;
        }

        public String name()
        {
            return this.name;
        }

        public long betValue()
        {
            return this.betValue;
        }

        public int betCount()
        {
            return this.betCount;
        }
    }

    private final int dealerEntityId;
    private final float dealerHealth;
    private final float dealerMaxHealth;
    private final int dealerSatiety;
    private final int dealerMaxSatiety;
    private final double dealerExperience;
    private final boolean hasWager;
    private final int phase;
    private final int remainingSeconds;
    private final UUID ownerId;
    private final boolean selfIsOwner;
    private final boolean selfIsMember;
    private final boolean canCreate;
    private final boolean canStart;
    private final boolean openScreen;
    private final boolean closeScreen;
    private final List<MemberEntry> members;

    public DealerPanelPacket(int dealerEntityId, float dealerHealth, float dealerMaxHealth, int dealerSatiety,
            int dealerMaxSatiety, double dealerExperience, boolean hasWager, int phase, int remainingSeconds,
            UUID ownerId, boolean selfIsOwner, boolean selfIsMember, boolean canCreate, boolean canStart,
            boolean openScreen, List<MemberEntry> members)
    {
        this(dealerEntityId, dealerHealth, dealerMaxHealth, dealerSatiety, dealerMaxSatiety, dealerExperience,
                hasWager, phase, remainingSeconds, ownerId, selfIsOwner, selfIsMember, canCreate, canStart,
                openScreen, false, members);
    }

    /**
     * A panel that only tells the client to drop the dealer screen, used when the session behind it has ended.
     */
    public static DealerPanelPacket closing(int dealerEntityId)
    {
        return new DealerPanelPacket(dealerEntityId, 0.0F, 0.0F, 0, 0, 0.0D, false, DealerBetMenu.PHASE_NONE, 0,
                null, false, false, false, false, false, true, java.util.Collections.<MemberEntry>emptyList());
    }

    private DealerPanelPacket(int dealerEntityId, float dealerHealth, float dealerMaxHealth, int dealerSatiety,
            int dealerMaxSatiety, double dealerExperience, boolean hasWager, int phase, int remainingSeconds,
            UUID ownerId, boolean selfIsOwner, boolean selfIsMember, boolean canCreate, boolean canStart,
            boolean openScreen, boolean closeScreen, List<MemberEntry> members)
    {
        this.dealerEntityId = dealerEntityId;
        this.dealerHealth = dealerHealth;
        this.dealerMaxHealth = dealerMaxHealth;
        this.dealerSatiety = dealerSatiety;
        this.dealerMaxSatiety = dealerMaxSatiety;
        this.dealerExperience = dealerExperience;
        this.hasWager = hasWager;
        this.phase = phase;
        this.remainingSeconds = remainingSeconds;
        this.ownerId = ownerId;
        this.selfIsOwner = selfIsOwner;
        this.selfIsMember = selfIsMember;
        this.canCreate = canCreate;
        this.canStart = canStart;
        this.openScreen = openScreen;
        this.closeScreen = closeScreen;
        this.members = members;
    }

    public int dealerEntityId() { return this.dealerEntityId; }
    public float dealerHealth() { return this.dealerHealth; }
    public float dealerMaxHealth() { return this.dealerMaxHealth; }
    public int dealerSatiety() { return this.dealerSatiety; }
    public int dealerMaxSatiety() { return this.dealerMaxSatiety; }
    public double dealerExperience() { return this.dealerExperience; }
    public boolean hasWager() { return this.hasWager; }
    public int phase() { return this.phase; }
    public int remainingSeconds() { return this.remainingSeconds; }
    public UUID ownerId() { return this.ownerId; }
    public boolean selfIsOwner() { return this.selfIsOwner; }
    public boolean selfIsMember() { return this.selfIsMember; }
    public boolean canCreate() { return this.canCreate; }
    public boolean canStart() { return this.canStart; }

    /**
     * True only for the answer to an explicit interaction, so periodic pushes never reopen a closed screen.
     */
    public boolean openScreen() { return this.openScreen; }

    /**
     * True when the session behind the screen has ended and the client must drop the screen.
     */
    public boolean closeScreen() { return this.closeScreen; }
    public List<MemberEntry> members() { return this.members; }

    public static void encode(DealerPanelPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.dealerEntityId);
        buffer.writeFloat(message.dealerHealth);
        buffer.writeFloat(message.dealerMaxHealth);
        buffer.writeVarInt(message.dealerSatiety);
        buffer.writeVarInt(message.dealerMaxSatiety);
        buffer.writeDouble(message.dealerExperience);
        buffer.writeBoolean(message.hasWager);
        buffer.writeVarInt(message.phase);
        buffer.writeVarInt(message.remainingSeconds);
        buffer.writeBoolean(message.ownerId != null);

        if (message.ownerId != null)
        {
            buffer.writeUUID(message.ownerId);
        }

        buffer.writeBoolean(message.selfIsOwner);
        buffer.writeBoolean(message.selfIsMember);
        buffer.writeBoolean(message.canCreate);
        buffer.writeBoolean(message.canStart);
        buffer.writeBoolean(message.openScreen);
        buffer.writeBoolean(message.closeScreen);
        buffer.writeVarInt(message.members.size());

        for (MemberEntry entry : message.members)
        {
            buffer.writeUUID(entry.playerId());
            buffer.writeUtf(entry.name());
            buffer.writeVarLong(entry.betValue());
            buffer.writeVarInt(entry.betCount());
        }

    }

    public static DealerPanelPacket decode(FriendlyByteBuf buffer)
    {
        int dealerEntityId = buffer.readVarInt();
        float dealerHealth = buffer.readFloat();
        float dealerMaxHealth = buffer.readFloat();
        int dealerSatiety = buffer.readVarInt();
        int dealerMaxSatiety = buffer.readVarInt();
        double dealerExperience = buffer.readDouble();
        boolean hasWager = buffer.readBoolean();
        int phase = buffer.readVarInt();
        int remainingSeconds = buffer.readVarInt();
        UUID ownerId = buffer.readBoolean() ? buffer.readUUID() : null;
        boolean selfIsOwner = buffer.readBoolean();
        boolean selfIsMember = buffer.readBoolean();
        boolean canCreate = buffer.readBoolean();
        boolean canStart = buffer.readBoolean();
        boolean openScreen = buffer.readBoolean();
        boolean closeScreen = buffer.readBoolean();
        int memberCount = buffer.readVarInt();
        List<MemberEntry> members = new ArrayList<MemberEntry>();

        for (int index = 0; index < memberCount; index++)
        {
            UUID playerId = buffer.readUUID();
            String name = buffer.readUtf(32);
            long betValue = buffer.readVarLong();
            int betCount = buffer.readVarInt();
            members.add(new MemberEntry(playerId, name, betValue, betCount));
        }

        return new DealerPanelPacket(dealerEntityId, dealerHealth, dealerMaxHealth, dealerSatiety, dealerMaxSatiety,
                dealerExperience, hasWager, phase, remainingSeconds, ownerId, selfIsOwner, selfIsMember, canCreate,
                canStart, openScreen, closeScreen, members);
    }

    public static void handle(DealerPanelPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleDealerPanel(message)));
        context.setPacketHandled(true);
    }
}
