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
 * Server to client: the config domains the admin panel edits, sent when the panel opens.
 */
public class AdminPanelDataPacket
{
    private static final int MAX_PAYLOAD = 262144;

    private final String prizePoolJson;
    private final String itemValuesJson;

    public AdminPanelDataPacket(String prizePoolJson, String itemValuesJson)
    {
        this.prizePoolJson = prizePoolJson == null ? "{}" : prizePoolJson;
        this.itemValuesJson = itemValuesJson == null ? "{}" : itemValuesJson;
    }

    public String prizePoolJson()
    {
        return this.prizePoolJson;
    }

    public String itemValuesJson()
    {
        return this.itemValuesJson;
    }

    public static void encode(AdminPanelDataPacket message, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(message.prizePoolJson, MAX_PAYLOAD);
        buffer.writeUtf(message.itemValuesJson, MAX_PAYLOAD);
    }

    public static AdminPanelDataPacket decode(FriendlyByteBuf buffer)
    {
        return new AdminPanelDataPacket(buffer.readUtf(MAX_PAYLOAD), buffer.readUtf(MAX_PAYLOAD));
    }

    public static void handle(AdminPanelDataPacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleAdminPanelData(message)));
        context.setPacketHandled(true);
    }
}
