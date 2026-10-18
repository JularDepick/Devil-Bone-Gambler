/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.draw;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.config.NotifyConfig;
import com.devilbonegambler.config.PrizeEntry;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.Placeholders;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends every result text from the server, using the display mode chosen by the prize entry.
 * The client never draws a result toast of its own.
 */
public final class NotifySender
{
    /* When each player was last told about each kind of notice, keyed by the notice itself. */
    private static final Map<ServerPlayer, Map<String, Long>> LAST_NOTICE =
            new WeakHashMap<ServerPlayer, Map<String, Long>>();

    private NotifySender()
    {
    }

    public static void sendEntryResult(ServerPlayer player, NotifyConfig notify)
    {
        send(player, notify.modeOrDefault(), entryText(player, notify));
    }

    public static void sendFailure(ServerPlayer player, String reasonKey)
    {
        if (suppressNotice(player, "reason." + reasonKey))
        {
            return;
        }

        Component text = Component.translatable("gui." + ModConstants.MOD_ID + ".reason." + reasonKey);
        send(player, ModConstants.NOTIFY_MODE_TELLRAW, text);
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

    /**
     * A refusal or an invalid action, held back when the same one was already shown a moment ago. Results, rewards and
     * state changes stay on {@link #sendChat} so no real news is ever swallowed by the cooldown.
     */
    public static void sendChatThrottled(ServerPlayer player, String key, Object... args)
    {
        if (suppressNotice(player, key))
        {
            return;
        }

        player.sendSystemMessage(Component.translatable(key, args));
    }

    /**
     * True while the same notice was already shown inside the cooldown. The stored time only moves when a notice is
     * actually sent, so repeated attempts still produce one line per second instead of silence.
     */
    private static boolean suppressNotice(ServerPlayer player, String notice)
    {
        long now = System.currentTimeMillis();

        synchronized (LAST_NOTICE)
        {
            Map<String, Long> seen = LAST_NOTICE.computeIfAbsent(player, key -> new HashMap<String, Long>());
            Long last = seen.get(notice);

            if (last != null && now - last < ModConstants.NOTICE_COOLDOWN_MS)
            {
                return true;
            }

            seen.put(notice, now);
            return false;
        }
    }

    /**
     * Draw text only ever goes to the chat box, so a message above the hotbar can never be mistaken for something
     * other than a draw, and the above hotbar area stays free for the game own prompts.
     */
    public static void send(ServerPlayer player, String mode, Component text)
    {
        if (ModConstants.NOTIFY_MODE_NONE.equals(mode))
        {
            return;
        }

        player.sendSystemMessage(text);
    }

    /**
     * The configured text is either plain text or a vanilla text component in JSON form.
     * A text that looks like a component but does not parse falls back to the default wording, so a broken
     * configuration never prints its raw JSON, with registry names and command syntax, into the chat box.
     */
    public static Component entryText(ServerPlayer player, NotifyConfig notify)
    {
        String raw = Placeholders.expand(notify.textOrEmpty(), player);

        if (raw == null || raw.trim().isEmpty())
        {
            return defaultText();
        }

        String trimmed = raw.trim();

        if (trimmed.startsWith("{") || trimmed.startsWith("["))
        {
            try
            {
                MutableComponent parsed = Component.Serializer.fromJson(trimmed);

                if (parsed != null)
                {
                    return parsed;
                }
            }
            catch (RuntimeException exception)
            {
                DevilBoneGambler.LOGGER.warn("The result text of a prize entry is not a valid text component, "
                        + "the default wording is used instead");
            }

            return defaultText();
        }

        return Component.literal(raw);
    }

    private static Component defaultText()
    {
        return Component.translatable("gui." + ModConstants.MOD_ID + ".notify.default");
    }
}
