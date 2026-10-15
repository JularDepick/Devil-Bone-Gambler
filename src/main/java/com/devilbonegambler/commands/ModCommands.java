/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.commands;

import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.AdminPanelDataPacket;
import com.devilbonegambler.util.ModConstants;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The single admin entry point of the mod.
 * The panel is opened by a permission gated command, never by an item.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModCommands
{
    private static final String ROOT_LITERAL = "devilbonegambler";
    private static final String CONFIG_LITERAL = "config";
    private static final String RELOAD_LITERAL = "reload";

    private ModCommands()
    {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal(ROOT_LITERAL)
                .then(Commands.literal(CONFIG_LITERAL)
                        .requires(source -> source.hasPermission(ModConstants.ADMIN_PERMISSION_LEVEL))
                        .executes(ModCommands::openPanel))
                .then(Commands.literal(RELOAD_LITERAL)
                        .requires(source -> source.hasPermission(ModConstants.ADMIN_PERMISSION_LEVEL))
                        .executes(ModCommands::reload)));
    }

    private static int openPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();

        if (!ServerConfigManager.isReady())
        {
            player.displayClientMessage(Component.translatable("gui." + ModConstants.MOD_ID + ".reason.not_ready"), true);
            return 0;
        }

        ModNetwork.sendToPlayer(player, new AdminPanelDataPacket(
                ServerConfigManager.prizePoolJson(), ServerConfigManager.itemValuesJson(),
                ServerConfigManager.shopJson()));
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        MinecraftServer server = context.getSource().getServer();

        if (server == null)
        {
            return 0;
        }

        ServerConfigManager.load(server);
        context.getSource().sendSuccess(() -> Component.literal("Devil Bone Gambler config reloaded"), true);
        return 1;
    }
}
