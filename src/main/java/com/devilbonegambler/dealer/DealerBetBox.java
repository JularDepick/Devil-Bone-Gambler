/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.dealer;

import com.devilbonegambler.config.ItemValueConfig;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ItemValuation;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Server owned bet box of one member: nine slots that never enter the world.
 * The box is the only place a bet lives while the wager runs, so leaving, cancelling and aborting all return the
 * very same stacks to the player.
 * The box is also the container behind the nine bet slots of the bet menu, which is why it implements the vanilla
 * container contract: the slots of the menu read and write these nine cells directly, so a bet can never exist in two
 * places at once.
 */
public final class DealerBetBox implements Container
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

    @Override
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
     * A bet may hold anything but the manuals that follow a role: those are bound to their holder and turn up again in
     * the hands of the same player, so offering one as a stake would only create an item the winner cannot keep.
     */
    public static boolean isAllowedBet(ItemStack stack)
    {
        return stack.isEmpty() || !(stack.is(ModItems.ADMIN_MANUAL.get()) || stack.is(ModItems.CREATOR_MANUAL.get()));
    }

    /**
     * Moves up to the given amount out of the source stack, merging into matching slots before using empty ones.
     * Returns how many items were moved, zero when the box has no room at all.
     */
    public int insert(ItemStack source, int amount)
    {
        if (!isAllowedBet(source))
        {
            return 0;
        }

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
     * Same as insert, but the preferred cell is filled first so a click can target one cell of the grid.
     * A cell that holds a different item is skipped and the remainder falls back to the normal order.
     */
    public int insert(ItemStack source, int amount, int preferredIndex)
    {
        if (!isAllowedBet(source))
        {
            return 0;
        }

        int remaining = Math.min(Math.max(0, amount), source.getCount());

        if (remaining <= 0 || preferredIndex < 0 || preferredIndex >= this.slots.size())
        {
            return insert(source, amount);
        }

        ItemStack slot = this.slots.get(preferredIndex);
        int room;

        if (slot.isEmpty())
        {
            room = source.getMaxStackSize();
        }
        else if (ItemStack.isSameItemSameTags(slot, source))
        {
            room = slot.getMaxStackSize() - slot.getCount();
        }
        else
        {
            room = 0;
        }

        if (room <= 0)
        {
            return insert(source, amount);
        }

        int step = Math.min(room, remaining);

        if (slot.isEmpty())
        {
            ItemStack copy = source.copy();
            copy.setCount(step);
            this.slots.set(preferredIndex, copy);
        }
        else
        {
            slot.grow(step);
        }

        ItemStack rest = source.copy();
        rest.setCount(remaining - step);
        return step + insert(rest, remaining - step);
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

    /*
     * Container: the nine cells are the same nine cells the wager service settles, so the menu and the settlement
     * always talk about the very same stacks.
     */

    @Override
    public int getContainerSize()
    {
        return this.slots.size();
    }

    @Override
    public ItemStack getItem(int index)
    {
        return this.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int amount)
    {
        ItemStack slot = this.get(index);

        if (slot.isEmpty() || amount <= 0)
        {
            return ItemStack.EMPTY;
        }

        ItemStack taken = slot.split(amount);

        if (slot.isEmpty())
        {
            this.setItem(index, ItemStack.EMPTY);
        }

        this.setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index)
    {
        ItemStack slot = this.get(index);

        if (slot.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        this.slots.set(index, ItemStack.EMPTY);
        return slot;
    }

    @Override
    public void setItem(int index, ItemStack stack)
    {
        if (index < 0 || index >= this.slots.size() || !isAllowedBet(stack))
        {
            return;
        }

        this.slots.set(index, stack);
        this.setChanged();
    }

    @Override
    public void setChanged()
    {
        /*
         * The box is the in memory authority of the wager, so a change is already live for whoever reads the box and
         * the menu detects it by comparing the slots. There is no state to flush and no listener to wake up.
         */
    }

    @Override
    public boolean stillValid(Player player)
    {
        /*
         * The box has no block to walk away from, so it stays valid for as long as it exists. The only way a box may
         * stop being usable is the end of its session, and the service then closes the menu on the server.
         */
        return true;
    }

    @Override
    public void clearContent()
    {
        for (int index = 0; index < this.slots.size(); index++)
        {
            this.slots.set(index, ItemStack.EMPTY);
        }
    }
}
