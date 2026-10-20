/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.client.gui.AltarScreen;
import com.devilbonegambler.client.gui.DealerBetScreen;
import com.devilbonegambler.client.gui.LuckOverlay;
import com.devilbonegambler.client.model.GamblersDealerModel;
import com.devilbonegambler.client.render.GamblersDealerRenderer;
import com.devilbonegambler.registry.ModEntities;
import com.devilbonegambler.registry.ModMenus;
import com.devilbonegambler.registry.ModModelLayers;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client only registrations, kept on the mod bus and behind the client dist so no server class loads it.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup
{
    private ClientSetup()
    {
    }

    /**
     * The dealer model is built by the mod, so its layer must be registered before any renderer bakes it.
     */
    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(ModModelLayers.GAMBLERS_DEALER, GamblersDealerModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(ModEntities.GAMBLERS_DEALER.get(), GamblersDealerRenderer::new);
    }

    /**
     * The container menus of the mod carry their own screens; the altar screen is opened only by the server.
     */
    /**
     * The luck line sits in the top right corner, drawn after the vanilla hotbar so no later vanilla layer covers it.
     */
    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event)
    {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "luck", new LuckOverlay());
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.ALTAR.get(), AltarScreen::new);
            MenuScreens.register(ModMenus.DEALER_BET.get(), DealerBetScreen::new);
        });
    }
}
