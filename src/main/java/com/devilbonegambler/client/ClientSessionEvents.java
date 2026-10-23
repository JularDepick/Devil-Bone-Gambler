/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.util.ModConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Drops the state that belongs to one connection when that connection ends or the next one begins.
 *
 * A config payload travels in parts, so a save or a push that is cut off by a disconnect leaves a half built buffer
 * behind. The buffer is dropped on both edges of the connection instead of only on the way out, because a client that
 * never sees a clean logout still has to start the next server from an empty cache. The last complete payloads go with
 * it, otherwise the panel could open on the data of a server the player has already left.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientSessionEvents
{
    private ClientSessionEvents()
    {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event)
    {
        ClientConfigCache.clear();
        ClientPacketHandler.resetServerState();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ClientConfigCache.clear();
        ClientPacketHandler.resetServerState();
    }
}
