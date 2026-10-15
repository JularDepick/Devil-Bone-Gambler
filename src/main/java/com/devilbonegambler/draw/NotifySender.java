/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.draw;

import com.devilbonegambler.config.NotifyConfig;
import com.devilbonegambler.config.PrizeEntry;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.Placeholders;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends every result text from the server, using the display mode chosen by the prize entry.
 * The client never draws a result toast of its own.
 */
public final class NotifySender
{
    private NotifySender()
    {
    }

    public static void sendEntryResult(ServerPlayer player, NotifyConfig notify)
    {
        send(player, notify.modeOrDefault(), entryText(player, notify));
    }

    public static void sendFailure(ServerPlayer player, String reasonKey)
    {
        Component text = Component.translatable("gui." + ModConstants.MOD_ID + ".reason." + reasonKey);
        send(player, ModConstants.NOTIFY_MODE_ACTIONBAR, text);
    }

    /**
     * Major event entries are announced to every online player, in addition to the normal result text.
     */
    public static void broadcastMajorEvent(ServerPlayer source, PrizeEntry entry)
    {
        MinecraftServer server = source.getServer();

        if (server == null)
        {
            return;
        }

        Component text = Component.translatable("gui." + ModConstants.MOD_ID + ".major_event",
                source.getGameProfile().getName())
                .append(Component.literal(" "))
                .append(entryText(source, entry.notifyOrDefault()));

        for (ServerPlayer target : server.getPlayerList().getPlayers())
        {
            target.sendSystemMessage(text);
        }
    }

    /**
     * Chat line used by the wager and economy flows, kept in one place so the wording stays consistent.
     */
    public static void sendChat(ServerPlayer player, String key, Object... args)
    {
        player.sendSystemMessage(Component.translatable(key, args));
    }

    public static void broadcastChat(List<ServerPlayer> players, Component text)
    {
        for (ServerPlayer player : players)
        {
            player.sendSystemMessage(text);
        }
    }

    public static void send(ServerPlayer player, String mode, Component text)
    {
        if (ModConstants.NOTIFY_MODE_NONE.equals(mode))
        {
            return;
        }

        if (ModConstants.NOTIFY_MODE_TITLE.equals(mode))
        {
            player.connection.send(new ClientboundSetTitleTextPacket(text));
            return;
        }

        if (ModConstants.NOTIFY_MODE_SUBTITLE.equals(mode))
        {
            player.connection.send(new ClientboundSetSubtitleTextPacket(text));
            return;
        }

        if (ModConstants.NOTIFY_MODE_TELLRAW.equals(mode))
        {
            player.sendSystemMessage(text);
            return;
        }

        player.displayClientMessage(text, true);
    }

    /**
     * The configured text is a vanilla text component in JSON form; anything that does not parse stays a literal.
     */
    public static Component entryText(ServerPlayer player, NotifyConfig notify)
    {
        String raw = Placeholders.expand(notify.textOrEmpty(), player);

        if (raw.isEmpty())
        {
            return Component.translatable("gui." + ModConstants.MOD_ID + ".notify.default");
        }

        try
        {
            MutableComponent parsed = Component.Serializer.fromJson(raw);

            if (parsed != null)
            {
                return parsed;
            }
        }
        catch (RuntimeException exception)
        {
            return Component.literal(raw);
        }

        return Component.literal(raw);
    }
}
