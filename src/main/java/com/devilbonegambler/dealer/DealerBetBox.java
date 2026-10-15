/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.dealer;

import com.devilbonegambler.config.ItemValueConfig;
import com.devilbonegambler.util.ItemValuation;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Server owned bet box of one member: nine slots that never enter the world.
 * The box is the only place a bet lives while the wager runs, so leaving, cancelling and aborting all return the
 * very same stacks to the player.
 */
public final class DealerBetBox
{
    private final List<ItemStack> slots = new ArrayList<ItemStack>();

    public DealerBetBox()
    {
        for (int index = 0; index < ModConstants.DEALER_BET_SLOTS; index++)
        {
            this.slots.add(ItemStack.EMPTY);
        }
    }

    public ItemStack get(int index)
    {
        return index < 0 || index >= this.slots.size() ? ItemStack.EMPTY : this.slots.get(index);
    }

    public List<ItemStack> stacks()
    {
        return this.slots;
    }

    public List<ItemStack> copyStacks()
    {
        List<ItemStack> copy = new ArrayList<ItemStack>();

        for (ItemStack stack : this.slots)
        {
            copy.add(stack.copy());
        }

        return copy;
    }

    public boolean isEmpty()
    {
        for (ItemStack stack : this.slots)
        {
            if (!stack.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    public int itemCount()
    {
        int count = 0;

        for (ItemStack stack : this.slots)
        {
            count += stack.getCount();
        }

        return count;
    }

    /**
     * Moves up to the given amount out of the source stack, merging into matching slots before using empty ones.
     * Returns how many items were moved, zero when the box has no room at all.
     */
    public int insert(ItemStack source, int amount)
    {
        int remaining = Math.min(Math.max(0, amount), source.getCount());
        int moved = 0;

        for (int index = 0; index < this.slots.size() && remaining > 0; index++)
        {
            ItemStack slot = this.slots.get(index);

            if (slot.isEmpty() || !ItemStack.isSameItemSameTags(slot, source))
            {
                continue;
            }

            int room = slot.getMaxStackSize() - slot.getCount();
            int step = Math.min(room, remaining);

            if (step <= 0)
            {
                continue;
            }

            slot.grow(step);
            remaining -= step;
            moved += step;
        }

        for (int index = 0; index < this.slots.size() && remaining > 0; index++)
        {
            if (!this.slots.get(index).isEmpty())
            {
                continue;
            }

            int step = Math.min(source.getMaxStackSize(), remaining);
            ItemStack copy = source.copy();
            copy.setCount(step);
            this.slots.set(index, copy);
            remaining -= step;
            moved += step;
        }

        return moved;
    }

    /**
     * Empties the box and hands the stacks over, which is what settlement, leaving and cancelling all use.
     */
    public List<ItemStack> takeAll()
    {
        List<ItemStack> taken = new ArrayList<ItemStack>();

        for (int index = 0; index < this.slots.size(); index++)
        {
            ItemStack stack = this.slots.get(index);

            if (!stack.isEmpty())
            {
                taken.add(stack);
                this.slots.set(index, ItemStack.EMPTY);
            }
        }

        return taken;
    }

    /**
     * Total value of the box under the same instance valuation as the altar: base value, enchantment bonus, then
     * the remaining durability share, multiplied by the stack size.
     */
    public long totalValue(ItemValueConfig values)
    {
        if (values == null)
        {
            return 0L;
        }

        int enchantBase = values.enchantValueBaseOrDefault();
        long total = 0L;

        for (ItemStack stack : this.slots)
        {
            if (stack.isEmpty())
            {
                continue;
            }

            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());

            if (key == null)
            {
                continue;
            }

            int base = values.itemValueOf(key.toString());
            total += (long) ItemValuation.stackValue(stack, base, enchantBase) * stack.getCount();
        }

        return total;
    }

    public void giveBack(ServerPlayer player)
    {
        for (ItemStack stack : this.takeAll())
        {
            if (!player.getInventory().add(stack))
            {
                player.drop(stack, false);
            }
        }
    }
}
