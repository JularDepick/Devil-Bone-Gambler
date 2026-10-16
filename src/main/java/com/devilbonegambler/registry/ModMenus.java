/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.registry;

import com.devilbonegambler.menu.AltarMenu;
import com.devilbonegambler.menu.DealerBetMenu;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Container menus of the mod.
 * A container menu carries the real slots of a screen, so the altar offering grid and the dealer bet box move items
 * with the vanilla container rules instead of a hand written synchronisation.
 */
public final class ModMenus
{
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ModConstants.MOD_ID);

    public static final RegistryObject<MenuType<AltarMenu>> ALTAR =
            MENUS.register("altar", () -> IForgeMenuType.create(AltarMenu::new));

    public static final RegistryObject<MenuType<DealerBetMenu>> DEALER_BET =
            MENUS.register("dealer_bet", () -> IForgeMenuType.create(DealerBetMenu::new));

    private ModMenus()
    {
    }
}
