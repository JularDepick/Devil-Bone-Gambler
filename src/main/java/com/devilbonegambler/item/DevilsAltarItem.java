/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * The altar as an item. It still places the altar block, and it can also be worn in the head slot, where it
 * counts as an altar placed at the wearer position.
 * The head slot of the vanilla inventory only accepts armour, so the slot check is widened here.
 */
public class DevilsAltarItem extends BlockItem
{
    public DevilsAltarItem(Block block, Properties properties)
    {
        super(block, properties);
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, Entity entity)
    {
        return slot == EquipmentSlot.HEAD;
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack)
    {
        return EquipmentSlot.HEAD;
    }
}
