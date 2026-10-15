/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.commands.CommandRunner;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.RequestGuard;
import com.devilbonegambler.network.packet.ValueTableSyncPacket;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.ValueTableBook;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Owns the server side config files and keeps the single authoritative in memory copy.
 * All access happens on the server thread.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerConfigManager
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static PrizePoolConfig prizePool;
    private static ItemValueConfig itemValues;
    private static Path configDir;
    private static MinecraftServer serverRef;

    private ServerConfigManager()
    {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        load(event.getServer());
    }

    /**
     * Pushes the current value table to every online player, used when the table changes at runtime.
     */
    public static void pushValueTable()
    {
        CompoundTag snapshot = ValueTableBook.snapshotTag();
        MinecraftServer server = serverRef;

        if (snapshot == null || server == null)
        {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            ModNetwork.sendToPlayer(player, new ValueTableSyncPacket(snapshot));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        CompoundTag snapshot = ValueTableBook.snapshotTag();

        if (snapshot != null)
        {
            ModNetwork.sendToPlayer(player, new ValueTableSyncPacket(snapshot));
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        clear();
        RequestGuard.clear();
    }

    public static boolean isReady()
    {
        return prizePool != null && itemValues != null;
    }

    public static PrizePoolConfig prizePool()
    {
        return prizePool;
    }

    public static ItemValueConfig itemValues()
    {
        return itemValues;
    }

    public static String prizePoolJson()
    {
        return prizePool == null ? "{}" : GSON.toJson(prizePool);
    }

    public static String itemValuesJson()
    {
        return itemValues == null ? "{}" : GSON.toJson(itemValues);
    }

    public static void clear()
    {
        prizePool = null;
        itemValues = null;
        configDir = null;
        serverRef = null;
    }

    /**
     * Loads both config files, creating them with their presets when they are missing.
     */
    public static void load(MinecraftServer server)
    {
        Path root = server.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig")
                .resolve(ModConstants.CONFIG_DIR_NAME);

        try
        {
            Files.createDirectories(root);
        }
        catch (IOException exception)
        {
            DevilBoneGambler.LOGGER.error("Failed to create the config directory {}", root, exception);
        }

        configDir = root;
        prizePool = readPrizePool(root.resolve(ModConstants.PRIZE_POOL_FILE));
        itemValues = readItemValues(root.resolve(ModConstants.ITEM_VALUES_FILE));
        pushValueTable();

        if (scanValues())
        {
            writeItemValues();
        }

        serverRef = server;
        refreshCommandValidity();

        DevilBoneGambler.LOGGER.info("Prize pool loaded: {} regions, {} entries, revision {}",
                Integer.valueOf(prizePool.regionsOrEmpty().size()),
                Integer.valueOf(prizePool.totalEntryCount()),
                Integer.valueOf(prizePool.revisionOrZero()));
        DevilBoneGambler.LOGGER.info("Value table loaded: {} items, {} entities, revision {}",
                Integer.valueOf(itemValues.itemsOrEmpty().size()),
                Integer.valueOf(itemValues.entitiesOrEmpty().size()),
                Integer.valueOf(itemValues.revisionOrZero()));
    }

    /**
     * Applies a prize pool write, returning null on success or a reason key on rejection.
     */
    public static String applyPrizePoolWrite(int clientRevision, String payload)
    {
        if (prizePool == null)
        {
            return "not_ready";
        }

        PrizePoolConfig candidate;

        try
        {
            candidate = GSON.fromJson(payload, PrizePoolConfig.class);
        }
        catch (RuntimeException exception)
        {
            DevilBoneGambler.LOGGER.warn("Rejected an unparsable prize pool payload", exception);
            return "invalid_config";
        }

        if (candidate == null)
        {
            return "invalid_config";
        }

        candidate.normalize();
        List<String> problems = candidate.validate();

        if (!problems.isEmpty())
        {
            DevilBoneGambler.LOGGER.warn("Rejected the prize pool write: {}", problems);
            return "invalid_config";
        }

        if (clientRevision != prizePool.revisionOrZero())
        {
            return "revision_conflict";
        }

        candidate.schemaVersion = Integer.valueOf(PrizePoolConfig.CURRENT_SCHEMA);
        candidate.revision = Integer.valueOf(prizePool.revisionOrZero() + 1);
        prizePool = candidate;
        refreshCommandValidity();
        writePrizePool();
        return null;
    }

    /**
     * Applies an item value write, returning null on success or a reason key on rejection.
     */
    public static String applyItemValuesWrite(int clientRevision, String payload)
    {
        if (itemValues == null)
        {
            return "not_ready";
        }

        ItemValueConfig candidate;

        try
        {
            candidate = GSON.fromJson(payload, ItemValueConfig.class);
        }
        catch (RuntimeException exception)
        {
            DevilBoneGambler.LOGGER.warn("Rejected an unparsable item value payload", exception);
            return "invalid_config";
        }

        if (candidate == null)
        {
            return "invalid_config";
        }

        candidate.normalize();
        List<String> problems = candidate.validate();

        if (!problems.isEmpty())
        {
            DevilBoneGambler.LOGGER.warn("Rejected the item value write: {}", problems);
            return "invalid_config";
        }

        if (clientRevision != itemValues.revisionOrZero())
        {
            return "revision_conflict";
        }

        candidate.schemaVersion = Integer.valueOf(ItemValueConfig.CURRENT_SCHEMA);
        candidate.revision = Integer.valueOf(itemValues.revisionOrZero() + 1);
        itemValues = candidate;
        writeItemValues();
        return null;
    }

    /**
     * Recomputes the command syntax validity of every prize entry. An invalid entry stays in the file so that an
     * administrator keeps the written command, but it is never selected.
     */
    private static void refreshCommandValidity()
    {
        List<PrizeRegion> regions = prizePool == null
                ? Collections.<PrizeRegion>emptyList()
                : prizePool.regionsOrEmpty();
        CommandDispatcher<CommandSourceStack> dispatcher = serverRef == null
                ? null
                : serverRef.getCommands().getDispatcher();
        CommandSourceStack source = serverRef == null ? null : serverRef.createCommandSourceStack();
        int invalid = 0;

        for (PrizeRegion region : regions)
        {
            for (PrizeEntry entry : region.entriesOrEmpty())
            {
                String problem = CommandRunner.problem(dispatcher, source, entry.commandOrEmpty());
                entry.invalidCommand = problem != null;

                if (problem != null)
                {
                    invalid++;
                    DevilBoneGambler.LOGGER.warn(
                            "Prize entry excluded, its command is not selectable, entry={}, problem={}",
                            entry.id, problem);
                }
            }
        }

        if (invalid > 0)
        {
            DevilBoneGambler.LOGGER.warn("Prize entries excluded by command validation: {}", Integer.valueOf(invalid));
        }
    }

    /**
     * Adds every registered item and entity type that is missing from the value table.
     * Vanilla entries take the built in preset, every other namespace takes the default value.
     */
    private static boolean scanValues()
    {
        if (itemValues == null)
        {
            return false;
        }

        boolean changed = false;
        changed |= scanChapter(itemValues.items, ForgeRegistries.ITEMS.getKeys(),
                itemValues.defaultItemValueOrDefault(), false);
        changed |= scanChapter(itemValues.entities, ForgeRegistries.ENTITY_TYPES.getKeys(),
                itemValues.defaultEntityValueOrDefault(), true);
        return changed;
    }

    private static boolean scanChapter(Map<String, Number> target, Set<ResourceLocation> keys, int fallback,
            boolean entityChapter)
    {
        boolean changed = false;

        for (ResourceLocation key : keys)
        {
            String id = key.toString();

            if (target.containsKey(id))
            {
                continue;
            }

            int value = fallback;

            if (ItemValuePresets.VANILLA_NAMESPACE.equals(key.getNamespace()))
            {
                value = entityChapter
                        ? ItemValuePresets.presetEntityOf(id, fallback)
                        : ItemValuePresets.presetItemOf(id, fallback);
            }

            target.put(id, Integer.valueOf(value));
            changed = true;
        }

        return changed;
    }

    private static PrizePoolConfig readPrizePool(Path path)
    {
        if (!Files.isRegularFile(path))
        {
            PrizePoolConfig created = DefaultPrizePool.create();
            created.normalize();
            writeJson(path, created);
            return created;
        }

        try
        {
            PrizePoolConfig loaded = GSON.fromJson(readText(path), PrizePoolConfig.class);

            if (loaded == null)
            {
                throw new IllegalStateException("empty prize pool file");
            }

            loaded.normalize();
            return loaded;
        }
        catch (RuntimeException exception)
        {
            DevilBoneGambler.LOGGER.error("Failed to read {}, the preset pool is used instead", path, exception);
            backup(path);
            PrizePoolConfig created = DefaultPrizePool.create();
            created.normalize();
            writeJson(path, created);
            return created;
        }
    }

    private static ItemValueConfig readItemValues(Path path)
    {
        if (!Files.isRegularFile(path))
        {
            ItemValueConfig created = ItemValueConfig.empty();
            created.normalize();
            return created;
        }

        try
        {
            ItemValueConfig loaded = GSON.fromJson(readText(path), ItemValueConfig.class);

            if (loaded == null)
            {
                throw new IllegalStateException("empty item value file");
            }

            loaded.normalize();
            return loaded;
        }
        catch (RuntimeException exception)
        {
            DevilBoneGambler.LOGGER.error("Failed to read {}, the built in values are used instead", path, exception);
            backup(path);
            ItemValueConfig created = ItemValueConfig.empty();
            created.normalize();
            return created;
        }
    }

    private static String readText(Path path)
    {
        try
        {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("unreadable file " + path, exception);
        }
    }

    private static void writePrizePool()
    {
        if (configDir == null || prizePool == null)
        {
            return;
        }

        prizePool.normalize();
        writeJson(configDir.resolve(ModConstants.PRIZE_POOL_FILE), prizePool);
    }

    private static void writeItemValues()
    {
        if (configDir == null || itemValues == null)
        {
            return;
        }

        itemValues.normalize();
        writeJson(configDir.resolve(ModConstants.ITEM_VALUES_FILE), itemValues);
    }

    /**
     * Writes through a temporary file so that an interrupted write cannot corrupt the live config.
     */
    private static void writeJson(Path path, Object value)
    {
        try
        {
            Path parent = path.getParent();

            if (parent != null)
            {
                Files.createDirectories(parent);
            }

            Path temporary = path.resolveSibling(path.getFileName().toString() + ".tmp");
            Files.writeString(temporary, GSON.toJson(value), StandardCharsets.UTF_8);
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException exception)
        {
            DevilBoneGambler.LOGGER.error("Failed to write {}", path, exception);
        }
    }

    private static void backup(Path path)
    {
        try
        {
            Path broken = path.resolveSibling(path.getFileName().toString() + ".broken");
            Files.move(path, broken, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException exception)
        {
            DevilBoneGambler.LOGGER.error("Failed to back up {}", path, exception);
        }
    }

    /**
     * Registry lookup helper shared by the value table book and the panel.
     */
    public static boolean isRegisteredItem(String itemId)
    {
        ResourceLocation key = ResourceLocation.tryParse(itemId);
        return key != null && ForgeRegistries.ITEMS.containsKey(key);
    }
}
