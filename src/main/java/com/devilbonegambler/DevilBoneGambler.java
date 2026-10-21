/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler;

import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.registry.ModBlockEntities;
import com.devilbonegambler.registry.ModBlocks;
import com.devilbonegambler.registry.ModCreativeTabs;
import com.devilbonegambler.registry.ModEntities;
import com.devilbonegambler.registry.ModEffects;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.registry.ModMenus;
import com.devilbonegambler.client.ModConfigScreen;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Mod entry point.
 * Owns the mod id, the shared logger and the registration of every deferred registry.
 */
@Mod(DevilBoneGambler.MODID)
public class DevilBoneGambler
{
    public static final String MODID = "devil_bone_gambler";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DevilBoneGambler(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        modEventBus.addListener(this::onCommonSetup);

        /*
         * The mod list config button is a client side entry point to the same admin panel as the in game key. The call
         * is wrapped so the client only class is never loaded on a dedicated server.
         */
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ModConfigScreen::register);
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(ModNetwork::register);
    }
}
