/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Hands one stack of the mod to a player.
 * The name of the stack has to be read before the hand over: an inventory that takes the whole stack empties it, and an
 * emptied stack reports itself as air, so a message that reads the name afterwards tells the player about air instead
 * of the item. Every grant that also names the item goes through this helper so the order cannot be lost again.
 */
public final class InventoryHandover
{
    private InventoryHandover()
    {
    }

    /**
     * Puts the whole stack into the inventory and reports the name it had before the hand over, or null when the
     * inventory had no room for it and the stack was left untouched.
     */
    public static Component handOver(ServerPlayer player, ItemStack stack)
    {
        Component name = stack.getHoverName();
        return player.getInventory().add(stack) ? name : null;
    }
}
