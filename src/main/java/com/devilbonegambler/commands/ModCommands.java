/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.commands;

import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.network.ServerRequestHandler;
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
                        .requires(ModCommands::mayManage)
                        .executes(ModCommands::openPanel))
                .then(Commands.literal(RELOAD_LITERAL)
                        .requires(ModCommands::mayManage)
                        .executes(ModCommands::reload)));
    }

    /**
     * The command surface follows the same rule as the panel: the creator and the granted admins, not the vanilla
     * permission level, so an operator without an entry in the admin list cannot reach the config.
     */
    private static boolean mayManage(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();
        return player != null && ServerConfigManager.isManager(player.getGameProfile().getName());
    }

    private static int openPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();

        if (!ServerConfigManager.isReady())
        {
            player.sendSystemMessage(Component.translatable("gui." + ModConstants.MOD_ID + ".reason.not_ready"));
            return 0;
        }

        ServerRequestHandler.sendPanel(player);
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
        context.getSource().sendSuccess(
                () -> Component.translatable("gui." + ModConstants.MOD_ID + ".command.reloaded"), true);
        return 1;
    }
}
