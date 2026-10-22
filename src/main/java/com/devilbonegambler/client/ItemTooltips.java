/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.util.ModConstants;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Tooltip lines of the items of this mod.
 * The lines live in one table keyed by registry path and are added for every item of the mod, so an item that is built
 * from a vanilla class (the spawn egg or the value table book) gets its lines without a subclass.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, value = Dist.CLIENT)
public final class ItemTooltips
{
    private static final Map<String, String[]> LINES = new HashMap<String, String[]>();

    static
    {
        LINES.put("devils_dice", new String[] {
                "tooltip.devil_bone_gambler.devils_dice",
                "tooltip.devil_bone_gambler.devils_dice_range",
                "tooltip.devil_bone_gambler.devils_dice_sight",
                "tooltip.devil_bone_gambler.dice_place" });
        LINES.put("gamblers_dice", new String[] {
                "tooltip.devil_bone_gambler.gamblers_dice",
                "tooltip.devil_bone_gambler.gamblers_dice_wager",
                "tooltip.devil_bone_gambler.gamblers_dice_locked",
                "tooltip.devil_bone_gambler.gamblers_dice_sight",
                "tooltip.devil_bone_gambler.dice_place" });
        LINES.put("devils_altar", new String[] {
                "tooltip.devil_bone_gambler.devils_altar",
                "tooltip.devil_bone_gambler.devils_altar_worn" });
        LINES.put("lucky_apple", new String[] {
                "tooltip.devil_bone_gambler.lucky_apple",
                "tooltip.devil_bone_gambler.lucky_apple_child",
                "tooltip.devil_bone_gambler.lucky_apple_sneak",
                "tooltip.devil_bone_gambler.lucky_apple_lock" });
        LINES.put("offering_value_table", new String[] {
                "tooltip.devil_bone_gambler.offering_value_table",
                "tooltip.devil_bone_gambler.offering_value_table_search" });
        LINES.put("gamblers_dealer_spawn_egg", new String[] {
                "tooltip.devil_bone_gambler.gamblers_dealer_spawn_egg",
                "tooltip.devil_bone_gambler.gamblers_dealer_spawn_egg_territory",
                "tooltip.devil_bone_gambler.gamblers_dealer_spawn_egg_drop" });
        LINES.put("gameplay_manual", new String[] {
                "tooltip.devil_bone_gambler.gameplay_manual",
                "tooltip.devil_bone_gambler.gameplay_manual_regrant" });
        LINES.put("admin_manual", new String[] {
                "tooltip.devil_bone_gambler.admin_manual",
                "tooltip.devil_bone_gambler.admin_manual_bound" });
        LINES.put("creator_manual", new String[] {
                "tooltip.devil_bone_gambler.creator_manual",
                "tooltip.devil_bone_gambler.creator_manual_bound" });
    }

    private ItemTooltips()
    {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event)
    {
        ItemStack stack = event.getItemStack();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());

        /* Judged by namespace, so a new item of this mod is covered as soon as it is registered. */
        if (key == null || !ModConstants.MOD_ID.equals(key.getNamespace()))
        {
            return;
        }

        String[] keys = LINES.get(key.getPath());

        if (keys == null)
        {
            return;
        }

        for (String line : keys)
        {
            event.getToolTip().add(Component.translatable(line).withStyle(ChatFormatting.GRAY));
        }
    }
}
