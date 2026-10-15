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
import com.devilbonegambler.registry.ModEffects;
import com.devilbonegambler.registry.ModEntities;
import com.devilbonegambler.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
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
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(ModNetwork::register);
    }
}
