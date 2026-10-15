/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.network.packet.AdminPanelDataPacket;
import com.devilbonegambler.network.packet.AdminPanelRequestPacket;
import com.devilbonegambler.network.packet.AltarStatusPacket;
import com.devilbonegambler.network.packet.ConfigSyncPacket;
import com.devilbonegambler.network.packet.ConfigWriteRequestPacket;
import com.devilbonegambler.network.packet.ConfigWriteResponsePacket;
import com.devilbonegambler.network.packet.DevilsDiceRequestPacket;
import com.devilbonegambler.network.packet.DrawResultPacket;
import com.devilbonegambler.network.packet.GamblersDiceRequestPacket;
import com.devilbonegambler.network.packet.MobListPacket;
import com.devilbonegambler.network.packet.OpenValueTablePacket;
import com.devilbonegambler.network.packet.PointDisplayPacket;
import com.devilbonegambler.network.packet.ValueTableRefreshRequestPacket;
import com.devilbonegambler.network.packet.ValueTableSyncPacket;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The single network channel of the mod, named after the documented channel name.
 * Every message is dispatched onto the game main thread by its own handler.
 */
public final class ModNetwork
{
    private static final String PROTOCOL = ModConstants.PROTOCOL_VERSION;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryParse(ModConstants.MOD_ID + ":" + ModConstants.NETWORK_CHANNEL),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private ModNetwork()
    {
    }

    public static void register()
    {
        int index = 0;

        CHANNEL.registerMessage(index++, GamblersDiceRequestPacket.class,
                GamblersDiceRequestPacket::encode, GamblersDiceRequestPacket::decode, GamblersDiceRequestPacket::handle);
        CHANNEL.registerMessage(index++, DevilsDiceRequestPacket.class,
                DevilsDiceRequestPacket::encode, DevilsDiceRequestPacket::decode, DevilsDiceRequestPacket::handle);
        CHANNEL.registerMessage(index++, ValueTableRefreshRequestPacket.class,
                ValueTableRefreshRequestPacket::encode, ValueTableRefreshRequestPacket::decode,
                ValueTableRefreshRequestPacket::handle);
        CHANNEL.registerMessage(index++, AdminPanelRequestPacket.class,
                AdminPanelRequestPacket::encode, AdminPanelRequestPacket::decode, AdminPanelRequestPacket::handle);
        CHANNEL.registerMessage(index++, ConfigWriteRequestPacket.class,
                ConfigWriteRequestPacket::encode, ConfigWriteRequestPacket::decode, ConfigWriteRequestPacket::handle);

        CHANNEL.registerMessage(index++, DrawResultPacket.class,
                DrawResultPacket::encode, DrawResultPacket::decode, DrawResultPacket::handle);
        CHANNEL.registerMessage(index++, AdminPanelDataPacket.class,
                AdminPanelDataPacket::encode, AdminPanelDataPacket::decode, AdminPanelDataPacket::handle);
        CHANNEL.registerMessage(index++, ConfigWriteResponsePacket.class,
                ConfigWriteResponsePacket::encode, ConfigWriteResponsePacket::decode,
                ConfigWriteResponsePacket::handle);
        CHANNEL.registerMessage(index++, ConfigSyncPacket.class,
                ConfigSyncPacket::encode, ConfigSyncPacket::decode, ConfigSyncPacket::handle);
        CHANNEL.registerMessage(index++, OpenValueTablePacket.class,
                OpenValueTablePacket::encode, OpenValueTablePacket::decode, OpenValueTablePacket::handle);
        CHANNEL.registerMessage(index++, PointDisplayPacket.class,
                PointDisplayPacket::encode, PointDisplayPacket::decode, PointDisplayPacket::handle);
        CHANNEL.registerMessage(index++, MobListPacket.class,
                MobListPacket::encode, MobListPacket::decode, MobListPacket::handle);
        CHANNEL.registerMessage(index++, AltarStatusPacket.class,
                AltarStatusPacket::encode, AltarStatusPacket::decode, AltarStatusPacket::handle);
        CHANNEL.registerMessage(index++, ValueTableSyncPacket.class,
                ValueTableSyncPacket::encode, ValueTableSyncPacket::decode, ValueTableSyncPacket::handle);

        DevilBoneGambler.LOGGER.info("Registered {} network messages on channel {}", Integer.valueOf(index),
                ModConstants.NETWORK_CHANNEL);
    }

    public static <MSG> void sendToServer(MSG message)
    {
        CHANNEL.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(ServerPlayer player, MSG message)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
