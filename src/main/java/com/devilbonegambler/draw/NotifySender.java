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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
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
        sendEntryResult(player, notify, null);
    }

    /**
     * Same result line, with the names of the samples the command rolled appended after the configured text.
     */
    public static void sendEntryResult(ServerPlayer player, NotifyConfig notify, List<String> sampledNames)
    {
        Component text = entryText(player, notify, sampledNames);

        if (text == null)
        {
            return;
        }

        send(player, notify.modeOrDefault(), Component.translatable("gui." + ModConstants.MOD_ID + ".draw.reward",
                text));
    }

    public static void sendFailure(ServerPlayer player, String reasonKey)
    {
        /* A throttled request is rejected in silence: the client must not scold the player for clicking again. */
        if (ModConstants.REASON_TOO_FREQUENT.equals(reasonKey) || suppressNotice(player, "reason." + reasonKey))
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
        broadcastMajorEvent(source, entry, null);
    }

    public static void broadcastMajorEvent(ServerPlayer source, PrizeEntry entry, List<String> sampledNames)
    {
        MinecraftServer server = source.getServer();

        if (server == null)
        {
            return;
        }

        Component result = entryText(source, entry.notifyOrDefault(), sampledNames);
        Component text = Component.translatable("gui." + ModConstants.MOD_ID + ".major_event",
                source.getGameProfile().getName());

        if (result != null)
        {
            text = text.copy().append(Component.literal(" ")).append(result);
        }

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
     * The display text of an entry is a plain string stored in the config file. Placeholders are expanded, and an entry
     * without text has no result line at all instead of a placeholder wording.
     */
    public static Component entryText(ServerPlayer player, NotifyConfig notify)
    {
        return entryText(player, notify, null);
    }

    /**
     * A sample name is appended with a single space, in the order the placeholders were expanded. Entries without a
     * sample placeholder pass no names and keep exactly their configured text.
     */
    public static Component entryText(ServerPlayer player, NotifyConfig notify, List<String> sampledNames)
    {
        String raw = Placeholders.expand(notify.textOrEmpty(), player);

        if (raw == null || raw.trim().isEmpty())
        {
            return null;
        }

        Component text = Component.literal(raw.trim());
        List<String> names = new ArrayList<String>();

        if (sampledNames != null)
        {
            for (String name : sampledNames)
            {
                if (name != null && !name.isEmpty())
                {
                    names.add(name);
                }
            }
        }

        if (names.isEmpty())
        {
            return text;
        }

        return text.copy().append(Component.literal(" " + String.join(" ", names)));
    }
}
