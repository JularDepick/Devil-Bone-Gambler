/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Shared slot interaction helpers for the screens that place items into a grid by hand.
 * The screens draw a client side view of the inventory instead of the live one, and the view is the live inventory
 * minus whatever the player already moved onto the cursor or into a staging area. Because items are only ever moved
 * between those three places and never copied, a staging list can not grow past what the player really holds.
 */
public final class SlotMoves
{
    /*
     * Main inventory plus the hotbar. The armour and offhand slots are not part of the drawn grid, so they are left
     * out of the view and can never absorb a subtraction that belongs to a drawn slot.
     */
    public static final int VIEW_SLOTS = 36;

    private SlotMoves()
    {
    }

    public static boolean sameStack(ItemStack first, ItemStack second)
    {
        return !first.isEmpty() && !second.isEmpty() && ItemStack.isSameItemSameTags(first, second);
    }

    /**
     * Copies the live inventory and subtracts everything that is currently on the cursor or already staged.
     * The copies are safe to mutate, so a screen can hand slots around without touching the real inventory.
     */
    public static List<ItemStack> visibleInventory(Inventory inventory, ItemStack carried, Collection<ItemStack> staged)
    {
        List<ItemStack> view = new ArrayList<ItemStack>();

        int slots = Math.min(VIEW_SLOTS, inventory.getContainerSize());

        for (int index = 0; index < slots; index++)
        {
            view.add(inventory.getItem(index).copy());
        }

        for (ItemStack stack : staged)
        {
            consume(view, stack);
        }

        consume(view, carried);
        return view;
    }

    /**
     * Removes the counts of one stack from a view, earliest slot first.
     */
    private static void consume(List<ItemStack> view, ItemStack target)
    {
        if (target == null || target.isEmpty())
        {
            return;
        }

        int remaining = target.getCount();

        for (ItemStack slot : view)
        {
            if (remaining <= 0)
            {
                return;
            }

            if (slot.isEmpty() || !sameStack(slot, target))
            {
                continue;
            }

            int taken = Math.min(slot.getCount(), remaining);
            slot.shrink(taken);
            remaining -= taken;
        }
    }

    /**
     * Places up to the given amount into the slot list, filling matching slots first and empty slots after.
     * Returns how many items could not be placed.
     */
    public static int insert(List<ItemStack> slots, ItemStack source, int amount)
    {
        int remaining = amount;

        for (ItemStack slot : slots)
        {
            if (remaining <= 0)
            {
                break;
            }

            if (slot.isEmpty() || !sameStack(slot, source))
            {
                continue;
            }

            int space = slot.getMaxStackSize() - slot.getCount();

            if (space <= 0)
            {
                continue;
            }

            int moved = Math.min(space, remaining);
            slot.grow(moved);
            remaining -= moved;
        }

        for (int index = 0; index < slots.size() && remaining > 0; index++)
        {
            if (!slots.get(index).isEmpty())
            {
                continue;
            }

            int moved = Math.min(source.getMaxStackSize(), remaining);
            ItemStack copy = source.copy();
            copy.setCount(moved);
            slots.set(index, copy);
            remaining -= moved;
        }

        return remaining;
    }

    /**
     * Empties the cursor into the slot list, used when a screen closes so nothing is silently lost.
     */
    public static ItemStack returnToSlots(List<ItemStack> slots, ItemStack carried)
    {
        if (carried.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        int remaining = insert(slots, carried, carried.getCount());
        ItemStack leftover = ItemStack.EMPTY;

        if (remaining > 0)
        {
            leftover = carried.copy();
            leftover.setCount(remaining);
        }

        return leftover;
    }
}
