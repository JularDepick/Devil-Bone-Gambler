/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.menu;

import com.devilbonegambler.draw.OfferingBundle;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.OfferEligibility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Container menu of the altar screen: the 3x3 offering grid on top and the real player inventory, main rows plus
 * hotbar, below it.
 * The offering grid is a session container owned by this menu, so the vanilla slot protocol moves the stacks and the
 * server reads back whatever really sits in the grid when a sacrifice is submitted. Every player owns an own container,
 * because a menu instance is created per open screen. Leftovers return to the player the moment the menu closes, so a
 * closed, disconnected or died screen can never swallow an offering.
 */
public class AltarMenu extends AbstractContainerMenu
{
    public static final int OFFERING_SLOTS = ModConstants.ALTAR_SLOTS;
    public static final int SLOT_SIZE = 18;

    /*
     * Layout of the grid and of the player inventory, relative to the top left corner of the screen.
     * The screen reads these constants, so the drawn labels and the real slots can never drift apart.
     */
    public static final int OFFERING_X = 132;
    public static final int OFFERING_Y = 24;
    public static final int INVENTORY_X = 132;
    public static final int INVENTORY_Y = 90;
    public static final int HOTBAR_Y = 148;

    public static final int OFFERING_COLUMNS = 3;
    public static final int INVENTORY_COLUMNS = 9;
    public static final int INVENTORY_ROWS = 3;
    public static final int HOTBAR_SLOTS = 9;
    public static final int PLAYER_SLOTS = INVENTORY_COLUMNS * INVENTORY_ROWS;

    /*
     * Slot order of the menu: the offering grid first, then the main inventory rows, then the hotbar.
     */
    public static final int OFFERING_START = 0;
    public static final int INVENTORY_START = OFFERING_SLOTS;
    public static final int HOTBAR_START = INVENTORY_START + PLAYER_SLOTS;
    public static final int SLOT_COUNT = HOTBAR_START + HOTBAR_SLOTS;

    /*
     * Registry name of the menu type. It is looked up by name instead of being referenced as a static field, so this
     * class forms no initialisation cycle with the registry that creates it.
     */
    private static final ResourceLocation MENU_ID = ResourceLocation.tryParse(ModConstants.MOD_ID + ":altar");

    private final Container offerings;

    /**
     * Server side constructor: the caller hands in the session container that holds the offering grid.
     */
    public AltarMenu(int windowId, Inventory inventory, Container offerings)
    {
        super(menuType(), windowId);
        this.offerings = offerings == null ? new SimpleContainer(OFFERING_SLOTS) : offerings;
        addOfferingSlots();
        addPlayerSlots(inventory);
    }

    /**
     * Convenience constructor for a locally built menu, used by the client side screen factory.
     */
    public AltarMenu(int windowId, Inventory inventory)
    {
        this(windowId, inventory, new SimpleContainer(OFFERING_SLOTS));
    }

    /**
     * Network factory constructor, reached through the menu type on the client.
     * The grid of the client is a local display container; its contents arrive with the ordinary slot synchronisation
     * right after the screen is opened, so nothing is read from the buffer.
     */
    public AltarMenu(int windowId, Inventory inventory, FriendlyByteBuf data)
    {
        this(windowId, inventory, new SimpleContainer(OFFERING_SLOTS));
    }

    /**
     * The registered menu type of this menu, or null while the registry is still being filled.
     */
    private static MenuType<?> menuType()
    {
        return ForgeRegistries.MENU_TYPES.getValue(MENU_ID);
    }

    private void addOfferingSlots()
    {
        for (int row = 0; row < OFFERING_SLOTS / OFFERING_COLUMNS; row++)
        {
            for (int column = 0; column < OFFERING_COLUMNS; column++)
            {
                addSlot(new OfferingSlot(this.offerings, column + row * OFFERING_COLUMNS,
                        OFFERING_X + column * SLOT_SIZE, OFFERING_Y + row * SLOT_SIZE));
            }
        }
    }

    /**
     * The offering grid refuses the items of this mod: they are not goods, and offering one would hand it back through
     * a reward. The gate sits on the slot, so placing, shift moving and dragging are all refused the same way.
     */
    private static final class OfferingSlot extends Slot
    {
        private OfferingSlot(Container container, int index, int x, int y)
        {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return !OfferEligibility.excluded(stack);
        }
    }

    private void addPlayerSlots(Inventory inventory)
    {
        for (int row = 0; row < INVENTORY_ROWS; row++)
        {
            for (int column = 0; column < INVENTORY_COLUMNS; column++)
            {
                addSlot(new Slot(inventory, INVENTORY_COLUMNS + column + row * INVENTORY_COLUMNS,
                        INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + row * SLOT_SIZE));
            }
        }

        for (int column = 0; column < HOTBAR_SLOTS; column++)
        {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * SLOT_SIZE, HOTBAR_Y));
        }
    }

    /**
     * The offering grid, read by the client screen to estimate the value of the current offering.
     */
    public Container offerings()
    {
        return this.offerings;
    }

    /**
     * Empties the grid and hands the stacks that really sat in it to the bundle. Server side only.
     * The item part of the client bundle is deliberately ignored: the container is the authority on what is offered.
     */
    public OfferingBundle takeOfferings(OfferingBundle clientBundle)
    {
        List<ItemStack> stacks = new ArrayList<ItemStack>(OFFERING_SLOTS);

        for (int index = 0; index < OFFERING_SLOTS; index++)
        {
            stacks.add(this.offerings.getItem(index));
            this.offerings.setItem(index, ItemStack.EMPTY);
        }

        return OfferingBundle.fromContainer(stacks, clientBundle);
    }

    /**
     * Hands everything still in the grid back to the player, dropping whatever does not fit at the player position.
     * The client copy of the menu is skipped, because its container is only a mirror of the server contents.
     */
    @Override
    public void removed(Player player)
    {
        super.removed(player);

        if (player.level().isClientSide())
        {
            return;
        }

        for (int index = 0; index < OFFERING_SLOTS; index++)
        {
            ItemStack stack = this.offerings.getItem(index);

            if (stack.isEmpty())
            {
                continue;
            }

            this.offerings.setItem(index, ItemStack.EMPTY);

            if (!player.getInventory().add(stack))
            {
                player.drop(stack, false);
            }
        }
    }

    /**
     * A worn altar governs the range exactly like a placed one and the server re-checks the range when a sacrifice is
     * submitted, so the menu stays open while the player walks around. A menu that closes early returns its leftovers.
     */
    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    /**
     * Vanilla shift click: a grid stack travels to the player inventory, a player stack travels into the grid and falls
     * back to the other player block when the grid is full.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        if (index < 0 || index >= this.slots.size())
        {
            return ItemStack.EMPTY;
        }

        Slot slot = this.slots.get(index);

        if (!slot.hasItem())
        {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index < OFFERING_SLOTS)
        {
            if (!moveItemStackTo(stack, INVENTORY_START, SLOT_COUNT, true))
            {
                return ItemStack.EMPTY;
            }
        }
        else if (!moveItemStackTo(stack, OFFERING_START, OFFERING_SLOTS, false))
        {
            if (index < HOTBAR_START)
            {
                if (!moveItemStackTo(stack, HOTBAR_START, SLOT_COUNT, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false))
            {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty())
        {
            slot.setByPlayer(ItemStack.EMPTY);
        }
        else
        {
            slot.setChanged();
        }

        if (stack.getCount() == result.getCount())
        {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return result;
    }
}
