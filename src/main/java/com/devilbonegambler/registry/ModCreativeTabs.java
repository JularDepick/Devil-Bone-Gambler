/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.registry;

import com.devilbonegambler.DevilBoneGambler;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * The mod owns a single dedicated creative tab.
 * Items are fed by the tab display generator, not by the vanilla tab contents event.
 */
public final class ModCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DevilBoneGambler.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.devil_bone_gambler"))
            .icon(() -> new ItemStack(ModItems.GAMBLERS_DICE.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.GAMBLERS_DICE.get());
                output.accept(ModItems.DEVILS_DICE.get());
                output.accept(ModBlocks.DEVILS_ALTAR_ITEM.get());
                output.accept(ModItems.OFFERING_VALUE_TABLE.get());
            })
            .build());

    private ModCreativeTabs()
    {
    }
}
