/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.client.ClientPacketHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to client: who the requester is and what they may edit. The two config domains themselves follow as
 * {@link ConfigPayloadPacket} parts, because a pool of thousands of entries never fits into one packet.
 */
public class AdminPanelDataPacket
{
    private static final int MAX_NAME = 64;
    private static final int MAX_ADMINS = 256;

    private final String creatorName;
    private final List<String> admins;
    private final boolean canManageAdmins;

    public AdminPanelDataPacket(String creatorName, List<String> admins, boolean canManageAdmins)
    {
        this.creatorName = creatorName == null ? "" : creatorName;
        this.admins = admins == null ? new ArrayList<String>() : admins;
        this.canManageAdmins = canManageAdmins;
    }

    public String creatorName()
    {
        return this.creatorName;
    }

    public List<String> admins()
    {
        return this.admins;
    }

    public boolean canManageAdmins()
    {
        return this.canManageAdmins;
    }

    public static void encode(AdminPanelDataPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(message.creatorName, MAX_NAME);
        buffer.writeVarInt(message.admins.size());

        for (String admin : message.admins)
        {
            buffer.writeUtf(admin, MAX_NAME);
        }

        buffer.writeBoolean(message.canManageAdmins);
    }

    public static AdminPanelDataPacket decode(FriendlyByteBuf buffer)
    {
        String creatorName = buffer.readUtf(MAX_NAME);
        int size = Math.min(buffer.readVarInt(), MAX_ADMINS);
        List<String> admins = new ArrayList<String>(size);

        for (int index = 0; index < size; index++)
        {
            admins.add(buffer.readUtf(MAX_NAME));
        }

        return new AdminPanelDataPacket(creatorName, admins, buffer.readBoolean());
    }

    public static void handle(AdminPanelDataPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleAdminPanelData(message)));
        context.setPacketHandled(true);
    }
}
