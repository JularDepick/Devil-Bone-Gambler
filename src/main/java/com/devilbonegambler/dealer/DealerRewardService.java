/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.dealer;

import com.devilbonegambler.data.DealerUsageData;
import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Dice usage reward: every threshold of consumed gambler's dice hands out one dealer spawn egg.
 * The counter only moves on paths that really consumed a dice, and a full inventory drops the egg at the feet.
 */
public final class DealerRewardService
{
    private static final String KEY_EGG_REWARD = "gui." + ModConstants.MOD_ID + ".dealer.egg_reward";

    private DealerRewardService()
    {
    }

    public static void recordDiceUse(ServerPlayer player)
    {
        if (player == null || player.getServer() == null)
        {
            return;
        }

        DealerUsageData data = DealerUsageData.get(player.getServer());
        int rewards = data.addUsage(player.getUUID(), ModConstants.DEALER_USAGE_PER_EGG);

        for (int index = 0; index < rewards; index++)
        {
            grantEgg(player);
        }
    }

    private static void grantEgg(ServerPlayer player)
    {
        ItemStack egg = new ItemStack(ModItems.GAMBLERS_DEALER_SPAWN_EGG.get());

        if (!player.getInventory().add(egg))
        {
            player.drop(egg, false);
        }

        NotifySender.sendChat(player, KEY_EGG_REWARD);
    }
}
