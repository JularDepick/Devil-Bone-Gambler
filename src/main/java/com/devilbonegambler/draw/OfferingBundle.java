/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.draw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Everything a devil's dice sacrifice can contain: item offerings, mob offerings and numeric offerings.
 * Any of the three parts may be empty; the server re-validates every part before deducting.
 */
public class OfferingBundle
{
    private final List<Offering> items;
    private final List<UUID> mobs;
    private final int health;
    private final int food;
    private final boolean allHealth;
    private final boolean deathDrop;

    /*
     * Server side only. The stacks an altar container already gave up, so a refused or aborted draw hands back the
     * very same stacks instead of rebuilding them from registry names.
     */
    private List<ItemStack> takenStacks = new ArrayList<ItemStack>();

    public OfferingBundle(List<Offering> items, List<UUID> mobs, int health, int food, boolean allHealth,
            boolean deathDrop)
    {
        this.items = items == null ? new ArrayList<Offering>() : items;
        this.mobs = mobs == null ? new ArrayList<UUID>() : mobs;
        this.health = Math.max(0, health);
        this.food = Math.max(0, food);
        this.allHealth = allHealth;
        this.deathDrop = deathDrop;
    }

    public static OfferingBundle empty()
    {
        return new OfferingBundle(Collections.<Offering>emptyList(), Collections.<UUID>emptyList(), 0, 0, false, false);
    }

    public static OfferingBundle itemsOnly(List<Offering> items)
    {
        return new OfferingBundle(items, Collections.<UUID>emptyList(), 0, 0, false, false);
    }

    /**
     * Bundle whose item part was taken out of an altar container, keeping the creature and numeric parts the client
     * sent.
     */
    public static OfferingBundle fromContainer(List<ItemStack> stacks, OfferingBundle source)
    {
        List<ItemStack> held = stacks == null ? new ArrayList<ItemStack>() : stacks;
        OfferingBundle bundle = new OfferingBundle(offeringsOf(held),
                source == null ? Collections.<UUID>emptyList() : source.mobs(),
                source == null ? 0 : source.health(),
                source == null ? 0 : source.food(),
                source != null && source.allHealth(),
                source != null && source.deathDrop());
        bundle.takenStacks = held;
        return bundle;
    }

    /**
     * The registry names, amounts, damage and enchantment levels behind the taken stacks, which is what the value of
     * the sacrifice is computed from.
     */
    private static List<Offering> offeringsOf(List<ItemStack> stacks)
    {
        List<Offering> result = new ArrayList<Offering>();

        for (ItemStack stack : stacks)
        {
            if (stack.isEmpty())
            {
                continue;
            }

            net.minecraft.resources.ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());

            if (key == null)
            {
                continue;
            }

            int levels = 0;

            for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stack).entrySet())
            {
                levels += entry.getValue().intValue();
            }

            result.add(new Offering(key.toString(), stack.getCount(), stack.getDamageValue(), levels));
        }

        return result;
    }

    /**
     * True when the item part already left an altar container, so the server must not deduct it from the inventory a
     * second time.
     */
    public boolean isTaken()
    {
        return !this.takenStacks.isEmpty();
    }

    public List<ItemStack> takenStacks()
    {
        return this.takenStacks;
    }

    public List<Offering> items()
    {
        return this.items;
    }

    public List<UUID> mobs()
    {
        return this.mobs;
    }

    public int health()
    {
        return this.health;
    }

    public int food()
    {
        return this.food;
    }

    public boolean allHealth()
    {
        return this.allHealth;
    }

    public boolean deathDrop()
    {
        return this.deathDrop;
    }

    /**
     * A bundle counts as empty when it carries no offering at all, including no numeric offering.
     */
    public boolean isEmpty()
    {
        return this.items.isEmpty() && this.mobs.isEmpty() && this.health <= 0 && this.food <= 0 && !this.allHealth;
    }

    public int itemEntryCount()
    {
        return this.items.size();
    }
}
