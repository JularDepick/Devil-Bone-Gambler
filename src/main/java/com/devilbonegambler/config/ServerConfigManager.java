/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.network.RequestGuard;
import com.devilbonegambler.util.ModConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
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
    private static ShopConfig shop;
    private static EconomyConfig economy;
    private static Path configDir;

    private ServerConfigManager()
    {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        load(event.getServer());
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

    public static ShopConfig shop()
    {
        return shop;
    }

    public static EconomyConfig economy()
    {
        return economy;
    }

    public static String shopJson()
    {
        return shop == null ? "{}" : GSON.toJson(shop);
    }

    public static String economyJson()
    {
        return economy == null ? "{}" : GSON.toJson(economy);
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
        shop = null;
        economy = null;
        configDir = null;
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
        shop = readShop(root.resolve(ModConstants.SHOP_FILE));
        economy = readEconomy(root.resolve(ModConstants.ECONOMY_FILE));

        if (scanValues())
        {
            writeItemValues();
        }

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

    /**
     * Applies a shop write, returning null on success or a reason key on rejection.
     */
    public static String applyShopWrite(int clientRevision, String payload)
    {
        if (shop == null)
        {
            return "not_ready";
        }

        ShopConfig candidate;

        try
        {
            candidate = GSON.fromJson(payload, ShopConfig.class);
        }
        catch (RuntimeException exception)
        {
            DevilBoneGambler.LOGGER.warn("Rejected an unparsable shop payload", exception);
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
            DevilBoneGambler.LOGGER.warn("Rejected the shop write: {}", problems);
            return "invalid_config";
        }

        if (clientRevision != shop.revisionOrZero())
        {
            return "revision_conflict";
        }

        candidate.schemaVersion = Integer.valueOf(ShopConfig.CURRENT_SCHEMA);
        candidate.revision = Integer.valueOf(shop.revisionOrZero() + 1);
        shop = candidate;
        writeShop();
        return null;
    }

    /**
     * Applies an economy write, returning null on success or a reason key on rejection.
     */
    public static String applyEconomyWrite(int clientRevision, String payload)
    {
        if (economy == null)
        {
            return "not_ready";
        }

        EconomyConfig candidate;

        try
        {
            candidate = GSON.fromJson(payload, EconomyConfig.class);
        }
        catch (RuntimeException exception)
        {
            DevilBoneGambler.LOGGER.warn("Rejected an unparsable economy payload", exception);
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
            DevilBoneGambler.LOGGER.warn("Rejected the economy write: {}", problems);
            return "invalid_config";
        }

        if (clientRevision != economy.revisionOrZero())
        {
            return "revision_conflict";
        }

        candidate.schemaVersion = Integer.valueOf(EconomyConfig.CURRENT_SCHEMA);
        candidate.revision = Integer.valueOf(economy.revisionOrZero() + 1);
        economy = candidate;
        writeEconomy();
        return null;
    }

    private static ShopConfig readShop(Path path)
    {
        if (!Files.isRegularFile(path))
        {
            ShopConfig created = ShopConfig.create();
            created.normalize();
            writeJson(path, created);
            return created;
        }

        try
        {
            ShopConfig loaded = GSON.fromJson(readText(path), ShopConfig.class);

            if (loaded == null)
            {
                throw new IllegalStateException("empty shop file");
            }

            loaded.normalize();
            return loaded;
        }
        catch (RuntimeException exception)
        {
            DevilBoneGambler.LOGGER.error("Failed to read {}, the preset shop is used instead", path, exception);
            backup(path);
            ShopConfig created = ShopConfig.create();
            created.normalize();
            writeJson(path, created);
            return created;
        }
    }

    private static EconomyConfig readEconomy(Path path)
    {
        if (!Files.isRegularFile(path))
        {
            EconomyConfig created = EconomyConfig.empty();
            created.normalize();
            writeJson(path, created);
            return created;
        }

        try
        {
            EconomyConfig loaded = GSON.fromJson(readText(path), EconomyConfig.class);

            if (loaded == null)
            {
                throw new IllegalStateException("empty economy file");
            }

            loaded.normalize();
            return loaded;
        }
        catch (RuntimeException exception)
        {
            DevilBoneGambler.LOGGER.error("Failed to read {}, the built in rates are used instead", path, exception);
            backup(path);
            EconomyConfig created = EconomyConfig.empty();
            created.normalize();
            writeJson(path, created);
            return created;
        }
    }

    private static void writeShop()
    {
        if (configDir == null || shop == null)
        {
            return;
        }

        shop.normalize();
        writeJson(configDir.resolve(ModConstants.SHOP_FILE), shop);
    }

    private static void writeEconomy()
    {
        if (configDir == null || economy == null)
        {
            return;
        }

        economy.normalize();
        writeJson(configDir.resolve(ModConstants.ECONOMY_FILE), economy);
    }

    private static String readText(Path path)    {
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
