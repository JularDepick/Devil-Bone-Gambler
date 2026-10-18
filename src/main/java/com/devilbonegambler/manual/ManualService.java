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
import com.devilbonegambler.util.InventoryHandover;
import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Keeps the manuals of a player in step with the role of that player.
 * The service never needs to know who dropped a manual or where it went: on a fixed interval it compares what a player
 * owns with what the role entitles them to, grants a manual that is missing and clears every copy beyond the first.
 * A manual that sits on the cursor of an open screen, in the crafting grid or in a container that is open counts as
 * owned, so picking one up with the mouse is never mistaken for having lost it, and a copy left in a chest is not
 * replaced by a second one.
 * The gameplay manual is free to keep, store or throw away and is handed out on every join and every respawn.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ManualService
{
    private static final String KEY_RECEIVED = "gui." + ModConstants.MOD_ID + ".manual.received";

    /*
     * Ticks between two calibration passes over the online players, which is also the delay after which a bound manual
     * that was dropped is gone from the world and back in the inventory of its holder.
     */
    private static int countdown;

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
            welcome(player);
        }
    }

    /**
     * A respawn is a fresh inventory as far as the manuals are concerned, so it is treated like a join.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            welcome(player);
        }
    }

    /**
     * One calibration pass over the online players and one sweep over the bound manuals that lie in a loaded chunk,
     * every {@link ModConstants#MANUAL_CALIBRATION_INTERVAL_TICKS} game ticks.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        if (++countdown < ModConstants.MANUAL_CALIBRATION_INTERVAL_TICKS)
        {
            return;
        }

        countdown = 0;
        MinecraftServer server = event.getServer();

        if (server == null)
        {
            return;
        }

        discardBoundManuals(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            /* A pass that runs on its own never speaks: the copy simply appears again. */
            calibrate(player, false);
        }
    }

    /**
     * Death never leaves a bound manual behind: the copies are taken out before the death drops are collected, so
     * nothing falls on the ground, and the next calibration grants them again.
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        clear(copies(player, ManualKind.CREATOR));
        clear(copies(player, ManualKind.ADMIN));
    }

    /**
     * A bound manual may not stay in a container, because a copy waiting in a chest is invisible to the calibration
     * pass and would be replaced by a second one. Whatever is left in a container slot when the screen closes goes back
     * into the inventory; a full inventory keeps it where it is and the next close tries again.
     */
    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        for (Slot slot : event.getContainer().slots)
        {
            if (slot.container == player.getInventory() || slot.container == player.getEnderChestInventory())
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
     * Called after the access file changed, so a role that was granted or revoked online shows at once instead of on
     * the next calibration pass.
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
            calibrate(player, true);
        }
    }

    /**
     * Join and respawn both hand out the gameplay manual, whether one is already there or not, and calibrate the bound
     * manuals on the spot instead of on the next pass.
     */
    private static void welcome(ServerPlayer player)
    {
        Component name = grant(player, ManualKind.GAMEPLAY);

        if (name != null)
        {
            NotifySender.sendChat(player, KEY_RECEIVED, name);
        }

        calibrate(player, true);
    }

    /**
     * Compares what one player owns with what the current role entitles them to.
     * A join, a respawn and a change of the role announce what they hand out, while the periodic pass stays silent, so
     * a manual that was dropped comes back without turning the chat box into a log of every drop.
     */
    private static void calibrate(ServerPlayer player, boolean announce)
    {
        String playerName = player.getGameProfile().getName();
        boolean creator = isCreator(playerName);

        hold(player, ManualKind.CREATOR, creator, announce);
        hold(player, ManualKind.ADMIN, !creator && ServerConfigManager.isManager(playerName), announce);
    }

    /**
     * One kind of bound manual: nothing to do when the player owns exactly one, one granted when there is none, and
     * every copy beyond the first cleared when there are more, and all of them when the role does not earn any.
     */
    private static void hold(ServerPlayer player, ManualKind kind, boolean entitled, boolean announce)
    {
        List<Runnable> copies = copies(player, kind);

        if (!entitled)
        {
            clear(copies);
            return;
        }

        if (copies.isEmpty())
        {
            /*
             * While the cursor carries anything the player is in the middle of moving items around, and a grant would
             * race with the screen, so the copy waits for a later pass.
             */
            if (!player.containerMenu.getCarried().isEmpty())
            {
                return;
            }

            Component name = grant(player, kind);

            if (name != null && announce)
            {
                NotifySender.sendChat(player, KEY_RECEIVED, name);
            }

            return;
        }

        /* The cursor is first in the list, so the copy the player is holding with the mouse is the one that stays. */
        clear(copies.subList(1, copies.size()));
    }

    /**
     * Every place a bound manual can sit while the player owns it, together with the action that empties that place.
     * The cursor of the open screen comes first, then the inventory, the armour and off hand slots, the ender chest
     * and finally the slots of the open screen that belong to neither, for example a chest or the crafting grid.
     */
    private static List<Runnable> copies(ServerPlayer player, ManualKind kind)
    {
        List<Runnable> found = new ArrayList<Runnable>();
        AbstractContainerMenu menu = player.containerMenu;

        if (kindOf(menu.getCarried()) == kind)
        {
            found.add(() -> menu.setCarried(ItemStack.EMPTY));
        }

        collect(found, player.getInventory(), kind);
        collect(found, player.getInventory().armor, kind);
        collect(found, player.getInventory().offhand, kind);
        collect(found, player.getEnderChestInventory(), kind);

        for (Slot slot : menu.slots)
        {
            if (slot.container != player.getInventory() && slot.container != player.getEnderChestInventory()
                    && kindOf(slot.getItem()) == kind)
            {
                found.add(() -> slot.set(ItemStack.EMPTY));
            }
        }

        return found;
    }

    private static void collect(List<Runnable> found, Container container, ManualKind kind)
    {
        for (int index = 0; index < container.getContainerSize(); index++)
        {
            final int slot = index;

            if (kindOf(container.getItem(slot)) == kind)
            {
                found.add(() -> container.setItem(slot, ItemStack.EMPTY));
            }
        }
    }

    private static void collect(List<Runnable> found, List<ItemStack> stacks, ManualKind kind)
    {
        for (int index = 0; index < stacks.size(); index++)
        {
            final int slot = index;

            if (kindOf(stacks.get(slot)) == kind)
            {
                found.add(() -> stacks.set(slot, ItemStack.EMPTY));
            }
        }
    }

    private static void clear(List<Runnable> copies)
    {
        for (Runnable copy : copies)
        {
            copy.run();
        }
    }

    /**
     * Removes every bound manual that lies in a loaded chunk of any level, which is what keeps a dropped copy out of the
     * hands of a player who is not entitled to it.
     */
    private static void discardBoundManuals(MinecraftServer server)
    {
        List<ItemEntity> found = new ArrayList<ItemEntity>();

        for (ServerLevel level : server.getAllLevels())
        {
            for (Entity entity : level.getAllEntities())
            {
                if (entity instanceof ItemEntity item && !item.isRemoved() && isBoundManual(item.getItem()))
                {
                    found.add(item);
                }
            }
        }

        for (ItemEntity item : found)
        {
            item.discard();
        }
    }

    /**
     * Puts one manual into the inventory and reports its name, or null when there is no room for it.
     * The name comes from the shared hand over helper, which reads it before the inventory can empty the stack.
     */
    private static Component grant(ServerPlayer player, ManualKind kind)
    {
        Component name = InventoryHandover.handOver(player, new ItemStack(ModItems.manualOf(kind)));

        if (name == null)
        {
            /*
             * Nothing is taken from the player here: the manual simply waits for a later pass with room to spare,
             * which keeps a bound manual out of the world instead of dropping it.
             */
            DevilBoneGambler.LOGGER.warn("No room for the {} of {}, it is granted on a later pass",
                    kind.itemId(), player.getGameProfile().getName());
        }

        return name;
    }

    private static boolean isCreator(String playerName)
    {
        AccessConfig access = ServerConfigManager.access();
        String creator = access == null ? "" : access.creatorOrEmpty();
        return !creator.isEmpty() && creator.equalsIgnoreCase(playerName);
    }

    private static boolean isBoundManual(ItemStack stack)
    {
        ManualKind kind = kindOf(stack);
        return kind != null && kind.bound();
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
}
