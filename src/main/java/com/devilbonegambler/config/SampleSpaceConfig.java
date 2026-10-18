/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.config;

import com.devilbonegambler.util.ModConstants;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Sample spaces of the ^(TYPE)[namespace] placeholders.
 * The spaces are discovered from the live registries on every world load and written to their own config file, so a
 * game update or a new mod only changes the content of that file instead of the source of this mod.
 * The kinds maps keep the small amount of classification that the registries do not expose directly: which effects
 * are positive, negative or neutral, and which enchantments are buffs or curses.
 */
public class SampleSpaceConfig
{
    public static final int CURRENT_SCHEMA = 1;
    public static final String KIND_BUFF = "BUFF";
    public static final String KIND_DEBUFF = "DEBUFF";
    public static final String KIND_NEUBUFF = "NEUBUFF";
    public static final String KIND_CURSE = "CURSE";

    public Integer schemaVersion;
    public Map<String, List<String>> spaces;
    public Map<String, String> effectKinds;
    public Map<String, String> enchantmentKinds;

    public SampleSpaceConfig()
    {
    }

    public static SampleSpaceConfig empty()
    {
        SampleSpaceConfig result = new SampleSpaceConfig();
        result.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);
        result.spaces = new TreeMap<String, List<String>>();
        result.effectKinds = new TreeMap<String, String>();
        result.enchantmentKinds = new TreeMap<String, String>();
        return result;
    }

    public Map<String, List<String>> spacesOrEmpty()
    {
        return this.spaces == null ? new TreeMap<String, List<String>>() : this.spaces;
    }

    public Map<String, String> effectKindsOrEmpty()
    {
        return this.effectKinds == null ? new TreeMap<String, String>() : this.effectKinds;
    }

    public Map<String, String> enchantmentKindsOrEmpty()
    {
        return this.enchantmentKinds == null ? new TreeMap<String, String>() : this.enchantmentKinds;
    }

    /**
     * Ids of one space, or an empty list when the type is unknown, so a caller never sees null.
     */
    public List<String> spaceOf(String type)
    {
        List<String> entries = this.spacesOrEmpty().get(type);
        return entries == null ? new ArrayList<String>() : entries;
    }

    /**
     * True when the type is one of the spaces this file knows, either a registry space or a derived one.
     */
    public boolean knows(String type)
    {
        return this.spacesOrEmpty().containsKey(type) || ModConstants.SAMPLE_DERIVED_TYPES.contains(type);
    }

    public int totalEntries()
    {
        int total = 0;

        for (List<String> entries : this.spacesOrEmpty().values())
        {
            total += entries.size();
        }

        return total;
    }

    public void normalize()
    {
        this.schemaVersion = Integer.valueOf(CURRENT_SCHEMA);

        if (this.spaces == null)
        {
            this.spaces = new TreeMap<String, List<String>>();
        }

        TreeMap<String, List<String>> sorted = new TreeMap<String, List<String>>();

        for (Map.Entry<String, List<String>> entry : this.spaces.entrySet())
        {
            if (entry.getKey() == null || entry.getValue() == null)
            {
                continue;
            }

            sorted.put(entry.getKey(), new ArrayList<String>(new TreeSet<String>(entry.getValue())));
        }

        this.spaces = sorted;

        if (this.effectKinds == null)
        {
            this.effectKinds = new TreeMap<String, String>();
        }

        if (this.enchantmentKinds == null)
        {
            this.enchantmentKinds = new TreeMap<String, String>();
        }
    }

    /**
     * Compares the scanned spaces with the stored ones and reports whether anything moved, so the file is only
     * rewritten when the installed content really changed.
     */
    public boolean replaceFrom(SampleSpaceConfig scanned)
    {
        boolean changed = !this.spacesOrEmpty().equals(scanned.spacesOrEmpty())
                || !this.effectKindsOrEmpty().equals(scanned.effectKindsOrEmpty())
                || !this.enchantmentKindsOrEmpty().equals(scanned.enchantmentKindsOrEmpty());

        if (changed)
        {
            this.spaces = new LinkedHashMap<String, List<String>>(scanned.spacesOrEmpty());
            this.effectKinds = new LinkedHashMap<String, String>(scanned.effectKindsOrEmpty());
            this.enchantmentKinds = new LinkedHashMap<String, String>(scanned.enchantmentKindsOrEmpty());
        }

        return changed;
    }

    /**
     * Discovers every space from the live registries. The world registry access is needed for the data pack
     * registries, which are not reachable through the static Forge registry view.
     */
    public static SampleSpaceConfig scan(MinecraftServer server)
    {
        SampleSpaceConfig result = empty();
        List<String> items = new ArrayList<String>();

        for (ResourceLocation key : ForgeRegistries.ITEMS.getKeys())
        {
            if (!ModConstants.RANDOM_AIR_ID.equals(key.toString()))
            {
                items.add(key.toString());
            }
        }

        result.spaces.put(ModConstants.SAMPLE_TYPE_ITEM, items);
        result.spaces.put(ModConstants.SAMPLE_TYPE_BLOCK, keys(ForgeRegistries.BLOCKS.getKeys(), null));
        result.spaces.put(ModConstants.SAMPLE_TYPE_ENTITY, keys(ForgeRegistries.ENTITY_TYPES.getKeys(), null));
        List<String> mobs = new ArrayList<String>();

        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues())
        {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);

            if (key != null && type.getCategory() != MobCategory.MISC)
            {
                mobs.add(key.toString());
            }
        }

        result.spaces.put(ModConstants.SAMPLE_TYPE_MOB, mobs);

        for (MobEffect effect : ForgeRegistries.MOB_EFFECTS.getValues())
        {
            ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);

            if (key == null)
            {
                continue;
            }

            MobEffectCategory category = effect.getCategory();
            String kind = category == MobEffectCategory.BENEFICIAL ? KIND_BUFF
                    : category == MobEffectCategory.HARMFUL ? KIND_DEBUFF : KIND_NEUBUFF;
            result.effectKinds.put(key.toString(), kind);
        }

        result.spaces.put(ModConstants.SAMPLE_TYPE_EFFECT, new ArrayList<String>(result.effectKinds.keySet()));
        List<String> enchantments = new ArrayList<String>();

        for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS.getValues())
        {
            ResourceLocation key = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);

            if (key == null)
            {
                continue;
            }

            enchantments.add(key.toString());
            result.enchantmentKinds.put(key.toString(), enchantment.isCurse() ? KIND_CURSE : KIND_BUFF);
        }

        result.spaces.put(ModConstants.SAMPLE_TYPE_ENCHANTMENT, enchantments);
        result.spaces.put(ModConstants.SAMPLE_TYPE_POTION, keys(ForgeRegistries.POTIONS.getKeys(), null));
        result.spaces.put(ModConstants.SAMPLE_TYPE_SOUND, keys(ForgeRegistries.SOUND_EVENTS.getKeys(), null));
        result.spaces.put(ModConstants.SAMPLE_TYPE_PARTICLE, keys(ForgeRegistries.PARTICLE_TYPES.getKeys(), null));
        result.spaces.put(ModConstants.SAMPLE_TYPE_PAINTING, keys(ForgeRegistries.PAINTING_VARIANTS.getKeys(),
                null));
        result.spaces.put(ModConstants.SAMPLE_TYPE_ATTRIBUTE, keys(ForgeRegistries.ATTRIBUTES.getKeys(), null));
        result.spaces.put(ModConstants.SAMPLE_TYPE_FLUID, keys(ForgeRegistries.FLUIDS.getKeys(),
                "minecraft:empty"));

        if (server != null)
        {
            result.spaces.put(ModConstants.SAMPLE_TYPE_BIOME, registryKeys(server, Registries.BIOME));
            result.spaces.put(ModConstants.SAMPLE_TYPE_STRUCTURE, registryKeys(server, Registries.STRUCTURE));
            result.spaces.put(ModConstants.SAMPLE_TYPE_DAMAGE_TYPE, registryKeys(server, Registries.DAMAGE_TYPE));
        }

        result.normalize();
        return result;
    }

    private static List<String> keys(Iterable<ResourceLocation> keys, String skip)
    {
        List<String> result = new ArrayList<String>();

        for (ResourceLocation key : keys)
        {
            if (skip == null || !skip.equals(key.toString()))
            {
                result.add(key.toString());
            }
        }

        return result;
    }

    private static <T> List<String> registryKeys(MinecraftServer server,
            net.minecraft.resources.ResourceKey<? extends Registry<T>> key)
    {
        List<String> result = new ArrayList<String>();
        Registry<T> registry = server.registryAccess().registryOrThrow(key);

        for (ResourceLocation id : registry.keySet())
        {
            result.add(id.toString());
        }

        return result;
    }
}
