/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.manual;

import com.devilbonegambler.config.AccessConfig;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.data.ManualGrantData;
import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hands the manuals to a player on the first join the mod sees.
 * The gameplay manual goes to everyone, the admin manual to a granted admin that is not the creator, and the creator
 * manual to the creator. The grant is recorded per player, so it happens once and never duplicates an item.
 * The listener runs last, because the creator is recorded by the config manager when the first player joins.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ManualService
{
    private static final String KEY_RECEIVED = "gui." + ModConstants.MOD_ID + ".manual.received";

    private ManualService()
    {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        ManualGrantData granted = ManualGrantData.get(player.server);

        if (granted.isGranted(player.getUUID()))
        {
            return;
        }

        String playerName = player.getGameProfile().getName();
        AccessConfig access = ServerConfigManager.access();
        String creator = access == null ? "" : access.creatorOrEmpty();
        boolean isCreator = !creator.isEmpty() && creator.equalsIgnoreCase(playerName);

        give(player, ManualKind.GAMEPLAY);

        if (isCreator)
        {
            give(player, ManualKind.CREATOR);
        }
        else if (ServerConfigManager.isManager(playerName))
        {
            give(player, ManualKind.ADMIN);
        }

        granted.markGranted(player.getUUID());
    }

    /**
     * The stack goes into the inventory and is dropped at the player position when it does not fit.
     */
    private static void give(ServerPlayer player, ManualKind kind)
    {
        ItemStack stack = new ItemStack(ModItems.manualOf(kind));

        if (!player.getInventory().add(stack))
        {
            player.drop(stack, false);
        }

        NotifySender.sendChat(player, KEY_RECEIVED, stack.getHoverName());
    }
}
