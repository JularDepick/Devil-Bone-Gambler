/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.commands;

import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.data.LuckData;
import com.devilbonegambler.draw.NotifySender;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.ServerRequestHandler;
import com.devilbonegambler.network.packet.LuckSyncPacket;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.SampleContext;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;
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
    private static final String SHORT_LITERAL = "dbgr";
    private static final String CONFIG_LITERAL = "config";
    private static final String RELOAD_LITERAL = "reload";
    private static final String RUN_LITERAL = "run";
    private static final String RUN_ARGUMENT = "command";
    private static final String LUCK_LITERAL = "luck";
    private static final String LUCK_TARGET_ARGUMENT = "player";
    private static final String LUCK_VALUE_ARGUMENT = "value";
    private static final String KEY_LUCK_SET = "gui." + ModConstants.MOD_ID + ".command.luck_set";
    private static final String KEY_LUCK_UNKNOWN = "gui." + ModConstants.MOD_ID + ".command.luck_unknown";

    private ModCommands()
    {
    }

    /**
     * The full name and the short name both hang off the same tree, so the two commands accept exactly the same
     * sub commands and the permission rule is written once.
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(tree(ROOT_LITERAL));
        event.getDispatcher().register(tree(SHORT_LITERAL));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tree(String name)
    {
        return Commands.literal(name)
                .then(Commands.literal(CONFIG_LITERAL)
                        .requires(ModCommands::mayManage)
                        .executes(ModCommands::openPanel))
                .then(Commands.literal(RELOAD_LITERAL)
                        .requires(ModCommands::mayManage)
                        .executes(ModCommands::reload))
                .then(Commands.literal(RUN_LITERAL)
                        .requires(ModCommands::mayManage)
                        .then(Commands.argument(RUN_ARGUMENT, StringArgumentType.greedyString())
                                .executes(ModCommands::runLine)))
                .then(Commands.literal(LUCK_LITERAL)
                        .requires(ModCommands::mayManage)
                        .then(Commands.argument(LUCK_TARGET_ARGUMENT, StringArgumentType.word())
                                .then(Commands.argument(LUCK_VALUE_ARGUMENT,
                                        IntegerArgumentType.integer(ModConstants.MIN_LUCK, ModConstants.MAX_LUCK))
                                        .executes(ModCommands::setLuck))));
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
            NotifySender.sendFailure(player, "not_ready");
            return 0;
        }

        ServerRequestHandler.sendPanel(player);
        return 1;
    }

    /**
     * Runs one command line with the sample space placeholders of the mod, in chat, so an admin can try a line before
     * putting it into a prize entry. The line is sampled without point or colour bias.
     */
    private static int runLine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MinecraftServer server = context.getSource().getServer();

        if (server == null)
        {
            return 0;
        }

        String raw = StringArgumentType.getString(context, RUN_ARGUMENT);
        /* The trial keeps the vanilla feedback of every line, so the admin sees what the command really did. */
        int executed = CommandRunner.run(server, player, raw, SampleContext.DEFAULT, null, true);

        if (executed <= 0)
        {
            NotifySender.sendFailure(player, "command_failed");
            return 0;
        }

        context.getSource().sendSuccess(
                () -> Component.translatable("gui." + ModConstants.MOD_ID + ".command.ran", Integer.valueOf(executed)),
                false);
        return 1;
    }

    /**
     * Sets the luck of one player and pushes the value to the client, so an admin can correct a value without waiting
     * for the next periodic update. The phase origin is kept, so the hidden curve stays where it was. An offline
     * player keeps the value until the next join.
     */
    private static int setLuck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        MinecraftServer server = context.getSource().getServer();

        if (server == null)
        {
            return 0;
        }

        String name = StringArgumentType.getString(context, LUCK_TARGET_ARGUMENT);
        int value = IntegerArgumentType.getInteger(context, LUCK_VALUE_ARGUMENT);
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        UUID targetId = online != null ? online.getUUID() : profileId(server, name);

        if (targetId == null)
        {
            context.getSource().sendFailure(Component.translatable(KEY_LUCK_UNKNOWN, name));
            return 0;
        }

        LuckData.get(server).setLuck(targetId, value);

        if (online != null)
        {
            ModNetwork.sendToPlayer(online, new LuckSyncPacket(value));
        }

        context.getSource().sendSuccess(
                () -> Component.translatable(KEY_LUCK_SET, name, Integer.valueOf(value)), false);
        return 1;
    }

    /**
     * The identity of a player who is not online, taken from the profile cache of the server.
     */
    private static UUID profileId(MinecraftServer server, String name)
    {
        return server.getProfileCache().get(name).map(GameProfile::getId).orElse(null);
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
