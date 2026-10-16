/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.menu;

import com.devilbonegambler.config.ItemValueConfig;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.dealer.DealerBetBox;
import com.devilbonegambler.dealer.DealerWagerService;
import com.devilbonegambler.dealer.DealerWagerSession;
import com.devilbonegambler.registry.ModMenus;
import com.devilbonegambler.util.ModConstants;
import java.util.UUID;
import java.util.function.LongSupplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The bet box of one member as a real container menu: the nine bet slots on top, the inventory and the hotbar below.
 * The nine slots are the server owned box itself, so a stack that sits in one of them has already left the inventory
 * and is returned by the wager service when the session ends. The header numbers and the button flags travel in the
 * container data, which keeps the client screen in step with the session without a message of its own.
 * The layout constants live here so the screen never guesses where a slot sits.
 */
public class DealerBetMenu extends AbstractContainerMenu
{
    /* Slot indices of this menu. */
    public static final int BET_SLOTS = ModConstants.DEALER_BET_SLOTS;
    public static final int INVENTORY_FIRST = BET_SLOTS;
    public static final int HOTBAR_FIRST = BET_SLOTS + 27;
    public static final int TOTAL_SLOTS = BET_SLOTS + 36;

    /* Slot layout, shared with the screen. */
    public static final int SLOT_SIZE = 18;
    public static final int BET_COLUMNS = 3;
    public static final int BET_X = 61;
    public static final int BET_Y = 18;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 100;
    public static final int HOTBAR_Y = 158;
    public static final int PANEL_WIDTH = 176;
    public static final int PANEL_HEIGHT = 234;

    /* Phase values of the container data, shared with the dealer panel so both describe the session the same way. */
    public static final int PHASE_NONE = 0;
    public static final int PHASE_RECRUIT = 1;
    public static final int PHASE_RUNNING = 2;

    /* Button flags of the container data. */
    public static final int FLAG_OWNER = 1;
    public static final int FLAG_MEMBER = 2;
    public static final int FLAG_CAN_START = 4;
    public static final int FLAG_CAN_CREATE = 8;
    public static final int FLAG_EDITABLE = 16;
    public static final int FLAG_VIEW_ONLY = 32;

    private static final int DATA_PHASE = 0;
    private static final int DATA_REMAINING = 1;
    private static final int DATA_VALUE_LOW = 2;
    private static final int DATA_VALUE_HIGH = 3;
    private static final int DATA_COUNT = 4;
    private static final int DATA_FLAGS = 5;
    private static final int DATA_SIZE = 6;

    private static final int NO_DEALER = -1;
    private static final long LOW_MASK = 0xFFFFFFFFL;
    private static final int INVENTORY_ROWS = 3;
    private static final int INVENTORY_COLUMNS = 9;

    private final DealerBetBox box;
    private final int dealerEntityId;
    private final ContainerData data;

    /**
     * Constructor of the menu type: the dealer entity id arrives with the opening packet and the box is a local
     * placeholder that the slot updates of the server fill.
     */
    public DealerBetMenu(int windowId, Inventory inventory, FriendlyByteBuf data)
    {
        this(windowId, inventory, data.readVarInt(), new DealerBetBox(), new SimpleContainerData(DATA_SIZE));
    }

    /**
     * Constructor without extra data, used when the menu is built without an opening packet.
     */
    public DealerBetMenu(int windowId, Inventory inventory)
    {
        this(windowId, inventory, NO_DEALER, new DealerBetBox(), new SimpleContainerData(DATA_SIZE));
    }

    private DealerBetMenu(int windowId, Inventory inventory, int dealerEntityId, DealerBetBox box, ContainerData data)
    {
        super(ModMenus.DEALER_BET.get(), windowId);
        this.box = box;
        this.dealerEntityId = dealerEntityId;
        this.data = data;

        for (int index = 0; index < BET_SLOTS; index++)
        {
            this.addSlot(new BetSlot(box, index, BET_X + index % BET_COLUMNS * SLOT_SIZE,
                    BET_Y + index / BET_COLUMNS * SLOT_SIZE));
        }

        for (int row = 0; row < INVENTORY_ROWS; row++)
        {
            for (int column = 0; column < INVENTORY_COLUMNS; column++)
            {
                this.addSlot(new Slot(inventory, INVENTORY_FIRST + row * INVENTORY_COLUMNS + column,
                        INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + row * SLOT_SIZE));
            }
        }

        for (int column = 0; column < INVENTORY_COLUMNS; column++)
        {
            this.addSlot(new Slot(inventory, column, INVENTORY_X + column * SLOT_SIZE, HOTBAR_Y));
        }

        this.addDataSlots(data);
    }

    /**
     * Server side menu of one member: the real box of the session and the live header data.
     * The viewer is the player who looks at the box while the box itself may belong to another member; the box of
     * another member stays read only, which the slots and the shift click rule enforce.
     */
    public static DealerBetMenu createServer(int windowId, Inventory inventory, int dealerEntityId, DealerBetBox box,
            DealerWagerSession session, UUID viewerId, boolean editable, LongSupplier nowTicks)
    {
        return new DealerBetMenu(windowId, inventory, dealerEntityId, box,
                new SessionData(box, session, viewerId, editable, nowTicks));
    }

    /**
     * Provider used by the server to open the box of one member. The client needs the dealer entity id for the action
     * buttons, so it travels in the extra data of the opening packet, and the display name and the editability come
     * from the service because only the server knows whose box this is.
     */
    public static MenuProvider provider(int dealerEntityId, DealerBetBox box, DealerWagerSession session,
            Component displayName, boolean editable, LongSupplier nowTicks)
    {
        return new BetMenuProvider(dealerEntityId, box, session, displayName, editable, nowTicks);
    }

    public int dealerEntityId()
    {
        return this.dealerEntityId;
    }

    public int phase()
    {
        return this.data.get(DATA_PHASE);
    }

    public int remainingSeconds()
    {
        return this.data.get(DATA_REMAINING);
    }

    public long betValue()
    {
        return (long) this.data.get(DATA_VALUE_HIGH) << 32 | (this.data.get(DATA_VALUE_LOW) & LOW_MASK);
    }

    public int betCount()
    {
        return this.data.get(DATA_COUNT);
    }

    public int flags()
    {
        return this.data.get(DATA_FLAGS);
    }

    /**
     * The bet grid only accepts a move while the session is still recruiting. The flag is part of the synced data, so
     * the client refuses the very same moves the server would refuse.
     */
    private boolean canEditBet()
    {
        return (this.flags() & FLAG_EDITABLE) != 0;
    }

    /**
     * Shift click between the bet box and the inventory. The box is only a destination while the session recruits.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        Slot slot = index < 0 || index >= this.slots.size() ? null : this.slots.get(index);

        if (slot == null || !slot.hasItem())
        {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < BET_SLOTS)
        {
            /* A box that is only viewed never carries an item out of it. */
            if (!this.canEditBet() || !this.moveItemStackTo(stack, INVENTORY_FIRST, TOTAL_SLOTS, true))
            {
                return ItemStack.EMPTY;
            }
        }
        else
        {
            if (!this.canEditBet() || !this.moveItemStackTo(stack, 0, BET_SLOTS, false))
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

        if (stack.getCount() == original.getCount())
        {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return original;
    }

    /**
     * Central guard of a box that may not be changed: the slots already refuse a take and a put, and this rejects every
     * click that targets a bet slot as well, so no click path a client may send can move an item of another member.
     * Only the bet slot range is filtered, so the own inventory rows of the viewer keep working.
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player)
    {
        if (!this.canEditBet() && slotId >= 0 && slotId < BET_SLOTS)
        {
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player)
    {
        /*
         * The box has no block to walk away from. The service closes this menu on the server when the session ends,
         * which is the only moment the menu may stop working.
         */
        return true;
    }

    /**
     * The nine bet slots: they are the box itself, so they refuse a move once the session stopped recruiting.
     */
    private final class BetSlot extends Slot
    {
        private BetSlot(Container container, int index, int x, int y)
        {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return DealerBetMenu.this.canEditBet();
        }

        @Override
        public boolean mayPickup(Player player)
        {
            return DealerBetMenu.this.canEditBet();
        }
    }

    /**
     * Server side view of the session for the container data. The menu reads the value and the count of a broadcast in
     * the same tick, so both are computed once per tick instead of once per read.
     */
    private static final class SessionData implements ContainerData
    {
        private final DealerBetBox box;
        private final DealerWagerSession session;
        private final UUID viewerId;
        private final boolean editable;
        private final LongSupplier nowTicks;
        private long cachedTick = -1L;
        private long cachedValue;
        private int cachedCount;

        private SessionData(DealerBetBox box, DealerWagerSession session, UUID viewerId, boolean editable,
                LongSupplier nowTicks)
        {
            this.box = box;
            this.session = session;
            this.viewerId = viewerId;
            this.editable = editable;
            this.nowTicks = nowTicks;
        }

        @Override
        public int get(int index)
        {
            switch (index)
            {
                case DATA_PHASE:
                    return this.session.phase() == DealerWagerSession.Phase.RECRUIT ? PHASE_RECRUIT : PHASE_RUNNING;
                case DATA_REMAINING:
                    return this.session.phase() == DealerWagerSession.Phase.RECRUIT
                            ? DealerWagerService.remainingSeconds(this.session, this.nowTicks.getAsLong()) : 0;
                case DATA_VALUE_LOW:
                    return (int) (this.refresh() & LOW_MASK);
                case DATA_VALUE_HIGH:
                    return (int) (this.refresh() >>> 32);
                case DATA_COUNT:
                    this.refresh();
                    return this.cachedCount;
                case DATA_FLAGS:
                    return this.flags();
                default:
                    return 0;
            }
        }

        @Override
        public void set(int index, int value)
        {
            /* The data of a running session is read only: the server owns the numbers, the client only shows them. */
        }

        @Override
        public int getCount()
        {
            return DATA_SIZE;
        }

        private long refresh()
        {
            long now = this.nowTicks.getAsLong();

            if (now != this.cachedTick)
            {
                this.cachedTick = now;
                ItemValueConfig values = ServerConfigManager.itemValues();
                this.cachedValue = this.box.totalValue(values);
                this.cachedCount = this.box.itemCount();
            }

            return this.cachedValue;
        }

        private int flags()
        {
            boolean recruit = this.session.phase() == DealerWagerSession.Phase.RECRUIT;
            int flags = FLAG_MEMBER;

            if (this.session.ownerId().equals(this.viewerId))
            {
                flags |= FLAG_OWNER;

                if (recruit && this.session.memberCount() >= ModConstants.DEALER_WAGER_MIN_PLAYERS)
                {
                    flags |= FLAG_CAN_START;
                }
            }

            if (!this.editable)
            {
                /* The box of another member is opened as a view, so the screen can say so and the slots stay shut. */
                flags |= FLAG_VIEW_ONLY;
            }
            else if (recruit)
            {
                flags |= FLAG_EDITABLE;
            }

            return flags;
        }
    }

    private static final class BetMenuProvider implements MenuProvider
    {
        private final int dealerEntityId;
        private final DealerBetBox box;
        private final DealerWagerSession session;
        private final Component displayName;
        private final boolean editable;
        private final LongSupplier nowTicks;

        private BetMenuProvider(int dealerEntityId, DealerBetBox box, DealerWagerSession session,
                Component displayName, boolean editable, LongSupplier nowTicks)
        {
            this.dealerEntityId = dealerEntityId;
            this.box = box;
            this.session = session;
            this.displayName = displayName;
            this.editable = editable;
            this.nowTicks = nowTicks;
        }

        @Override
        public Component getDisplayName()
        {
            return this.displayName;
        }

        @Override
        public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player)
        {
            return createServer(windowId, inventory, this.dealerEntityId, this.box, this.session, player.getUUID(),
                    this.editable, this.nowTicks);
        }
    }
}
