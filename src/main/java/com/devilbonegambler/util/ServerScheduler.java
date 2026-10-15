/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.util;

import com.devilbonegambler.DevilBoneGambler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Delayed server tasks driven by the server tick.
 * The tick event fires twice per tick, so only the end phase is handled.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerScheduler
{
    private static final List<Task> TASKS = new ArrayList<Task>();

    private ServerScheduler()
    {
    }

    public static void schedule(int delayTicks, Consumer<MinecraftServer> action)
    {
        TASKS.add(new Task(Math.max(0, delayTicks), action));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || TASKS.isEmpty())
        {
            return;
        }

        MinecraftServer server = event.getServer();
        Iterator<Task> iterator = TASKS.iterator();

        while (iterator.hasNext())
        {
            Task task = iterator.next();

            if (task.remaining > 0)
            {
                task.remaining--;
                continue;
            }

            iterator.remove();

            try
            {
                task.action.accept(server);
            }
            catch (RuntimeException exception)
            {
                DevilBoneGambler.LOGGER.error("A scheduled server task threw an exception", exception);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        TASKS.clear();
    }

    private static final class Task
    {
        private int remaining;
        private final Consumer<MinecraftServer> action;

        private Task(int delayTicks, Consumer<MinecraftServer> action)
        {
            this.remaining = delayTicks;
            this.action = action;
        }
    }
}
