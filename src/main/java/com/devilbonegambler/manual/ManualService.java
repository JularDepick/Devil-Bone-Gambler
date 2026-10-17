/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.manual;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.config.AccessConfig;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Keeps the manuals of a player in step with the role of that player.
 * Every join compares the inventory against the role instead of remembering a first grant, so a lost manual is handed
 * out again and a manual the role no longer earns is taken back. The gameplay manual is free to keep, store or throw
 * away, while the creator and the admin manual follow the role: a drop is refused, and an item entity of one is
 * removed from the world and put back into the inventory of its holder right away.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ManualService
{
    private static final String KEY_RECEIVED = "gui." + ModConstants.MOD_ID + ".manual.received";
    private static final String KEY_KEPT = "gui." + ModConstants.MOD_ID + ".manual.kept";

    private ManualService()
    {
    }

    /**
     * The creator is recorded by the config manager when the first player joins, so this listener runs last.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            syncRole(player);
        }
    }

    /**
     * Brings the manuals of one player in line with the role: the gameplay manual is topped up when it is missing, and
     * the creator or the admin manual is granted when it is deserved and taken back when it is not.
     */
    public static void syncRole(ServerPlayer player)
    {
        String playerName = player.getGameProfile().getName();
        boolean creator = isCreator(playerName);
        boolean admin = !creator && ServerConfigManager.isManager(playerName);

        giveIfMissing(player, ManualKind.GAMEPLAY);

        if (creator)
        {
            takeBack(player, ManualKind.ADMIN);
            giveIfMissing(player, ManualKind.CREATOR);
        }
        else if (admin)
        {
            takeBack(player, ManualKind.CREATOR);
            giveIfMissing(player, ManualKind.ADMIN);
        }
        else
        {
            takeBack(player, ManualKind.CREATOR);
            takeBack(player, ManualKind.ADMIN);
        }
    }

    /**
     * Called after the access file changed: an online player is synced on the spot, and a player who is away is synced
     * on the next join because the login check never remembers a first grant.
     */
    public static void onRoleChanged(String playerName)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null)
        {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);

        if (player != null)
        {
            syncRole(player);
        }
    }

    /**
     * Grants one manual when the inventory does not hold it any more. A bound manual may not be stored in a container,
     * so the inventory is the only place it can be, and the ender chest is deliberately not counted.
     */
    public static void giveIfMissing(ServerPlayer player, ManualKind kind)
    {
        if (count(player, kind) > 0)
        {
            return;
        }

        if (kind.bound())
        {
            /*
             * A copy left in the ender chest by an older version would otherwise become a second manual next to the
             * fresh one, so it is dropped there before the grant.
             */
            remove(player.getEnderChestInventory(), kind);
        }

        ItemStack stack = new ItemStack(ModItems.manualOf(kind));
        boolean stored = player.getInventory().add(stack);

        if (!stored)
        {
            /*
             * Nothing is taken from the player here: the manual simply waits for a later join with room to spare,
             * which keeps a bound manual out of the world instead of dropping it.
             */
            DevilBoneGambler.LOGGER.warn("No room for the {} of {}, it is granted again on a later join", kind.itemId(),
                    player.getGameProfile().getName());
            return;
        }

        NotifySender.sendChat(player, KEY_RECEIVED, stack.getHoverName());
    }

    /**
     * Takes every manual of one kind back out of the inventory and the ender chest and reports how many were removed.
     */
    public static int takeBack(ServerPlayer player, ManualKind kind)
    {
        return remove(player.getInventory(), kind) + remove(player.getEnderChestInventory(), kind);
    }

    /**
     * A bound manual may never exist as a dropped item: the entity is stopped before it joins the level and its owner
     * gets the manual back when that owner still holds the role.
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item))
        {
            return;
        }

        ManualKind kind = kindOf(item.getItem());

        if (kind == null || !kind.bound())
        {
            return;
        }

        event.setCanceled(true);
        recover(item, kind);
    }

    /**
     * Safety net for every drop path that does not pass the drop refusal of the item, such as throwing a hovered slot
     * out of a container screen: a bound manual that is not in the inventory and not on the carried stack is granted
     * again, so a manual can never stay lost for longer than one check interval.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))
        {
            return;
        }

        if (player.tickCount % ModConstants.MANUAL_RECHECK_INTERVAL_TICKS != 0 || !player.isAlive()
                || player.getInventory().getFreeSlot() < 0)
        {
            return;
        }

        ItemStack carried = player.containerMenu.getCarried();

        for (ManualKind kind : ManualKind.values())
        {
            if (kind.bound() && holdsRole(player, kind) && count(player, kind) == 0
                    && !carried.is(ModItems.manualOf(kind)))
            {
                giveIfMissing(player, kind);
            }
        }
    }

    /**
     * A bound manual may not be stored in a container: whatever the player left in one of the container slots when the
     * screen closes goes back into the inventory. A full inventory keeps the manual where it is and the next close
     * tries again, so the manual is never destroyed.
     */
    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        AbstractContainerMenu menu = event.getContainer();

        for (Slot slot : menu.slots)
        {
            if (slot.container == player.getInventory())
            {
                continue;
            }

            ManualKind kind = kindOf(slot.getItem());

            if (kind == null || !kind.bound())
            {
                continue;
            }

            ItemStack taken = slot.getItem().copy();
            slot.set(ItemStack.EMPTY);

            if (!player.getInventory().add(taken))
            {
                slot.set(taken);
                DevilBoneGambler.LOGGER.warn("{} has no room for the {} taken back out of a container",
                        player.getGameProfile().getName(), kind.itemId());
            }
        }
    }

    /**
     * Hands a recovered bound manual back: first to the player who dropped it, then to the one online holder of the
     * role, and otherwise nowhere, because the next join grants it again.
     */
    private static void recover(ItemEntity item, ManualKind kind)
    {
        MinecraftServer server = item.level().getServer();

        if (server == null)
        {
            return;
        }

        ServerPlayer holder = null;
        Entity owner = item.getOwner();

        if (owner instanceof ServerPlayer candidate && holdsRole(candidate, kind))
        {
            holder = candidate;
        }

        if (holder == null)
        {
            holder = findRoleHolder(server, kind);
        }

        if (holder == null)
        {
            return;
        }

        ItemStack restored = item.getItem().copy();

        if (holder.getInventory().add(restored))
        {
            NotifySender.sendChat(holder, KEY_KEPT, restored.getHoverName());
            return;
        }

        DevilBoneGambler.LOGGER.warn("Recovered {} had no room in {}, it is granted again once there is room",
                restored.getHoverName().getString(), holder.getGameProfile().getName());
    }

    /**
     * The one online player the manual belongs to, used when the thrower is unknown or lost the role meanwhile.
     */
    private static ServerPlayer findRoleHolder(MinecraftServer server, ManualKind kind)
    {
        if (kind == ManualKind.CREATOR)
        {
            for (ServerPlayer player : server.getPlayerList().getPlayers())
            {
                if (isCreator(player.getGameProfile().getName()))
                {
                    return player;
                }
            }

            return null;
        }

        List<ServerPlayer> admins = server.getPlayerList().getPlayers().stream()
                .filter(player -> holdsRole(player, ManualKind.ADMIN))
                .toList();
        return admins.size() == 1 ? admins.get(0) : null;
    }

    private static boolean holdsRole(ServerPlayer player, ManualKind kind)
    {
        String playerName = player.getGameProfile().getName();

        if (kind == ManualKind.CREATOR)
        {
            return isCreator(playerName);
        }

        return kind == ManualKind.ADMIN && !isCreator(playerName) && ServerConfigManager.isManager(playerName);
    }

    private static boolean isCreator(String playerName)
    {
        AccessConfig access = ServerConfigManager.access();
        String creator = access == null ? "" : access.creatorOrEmpty();
        return !creator.isEmpty() && creator.equalsIgnoreCase(playerName);
    }

    private static ManualKind kindOf(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return null;
        }

        for (ManualKind kind : ManualKind.values())
        {
            if (stack.is(ModItems.manualOf(kind)))
            {
                return kind;
            }
        }

        return null;
    }

    private static int count(ServerPlayer player, ManualKind kind)
    {
        return count(player.getInventory(), kind);
    }

    private static int count(Container container, ManualKind kind)
    {
        int total = 0;

        for (int slot = 0; slot < container.getContainerSize(); slot++)
        {
            ItemStack stack = container.getItem(slot);

            if (!stack.isEmpty() && kindOf(stack) == kind)
            {
                total += stack.getCount();
            }
        }

        return total;
    }

    private static int remove(Container container, ManualKind kind)
    {
        int removed = 0;

        for (int slot = 0; slot < container.getContainerSize(); slot++)
        {
            ItemStack stack = container.getItem(slot);

            if (stack.isEmpty() || kindOf(stack) != kind)
            {
                continue;
            }

            removed += stack.getCount();
            container.setItem(slot, ItemStack.EMPTY);
        }

        return removed;
    }
}
