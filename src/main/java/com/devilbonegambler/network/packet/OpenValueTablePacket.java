/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.network.packet;

import com.devilbonegambler.client.ClientPacketHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server to client: the refreshed value table pages, sent so that the book screen shows fresh data
 * without waiting for the inventory slot to sync.
 */
public class OpenValueTablePacket
{
    private static final int MAX_PAGE = 32767;
    private static final int MAX_PAGES = 100;

    private final String title;
    private final String author;
    private final List<String> pages;

    public OpenValueTablePacket(String title, String author, List<String> pages)
    {
        this.title = title == null ? "" : title;
        this.author = author == null ? "" : author;
        this.pages = pages == null ? new ArrayList<String>() : pages;
    }

    public String title()
    {
        return this.title;
    }

    public String author()
    {
        return this.author;
    }

    public List<String> pages()
    {
        return this.pages;
    }

    public static void encode(OpenValueTablePacket message, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(message.title, 64);
        buffer.writeUtf(message.author, 64);
        buffer.writeVarInt(message.pages.size());

        for (String page : message.pages)
        {
            buffer.writeUtf(page, MAX_PAGE);
        }
    }

    public static OpenValueTablePacket decode(FriendlyByteBuf buffer)
    {
        String title = buffer.readUtf(64);
        String author = buffer.readUtf(64);
        int size = Math.min(buffer.readVarInt(), MAX_PAGES);
        List<String> pages = new ArrayList<String>(size);

        for (int index = 0; index < size; index++)
        {
            pages.add(buffer.readUtf(MAX_PAGE));
        }

        return new OpenValueTablePacket(title, author, pages);
    }

    public static void handle(OpenValueTablePacket message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleOpenValueTable(message)));
        context.setPacketHandled(true);
    }
}
