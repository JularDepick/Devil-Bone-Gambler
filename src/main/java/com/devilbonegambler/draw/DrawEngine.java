/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.draw;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.altar.AltarService;
import com.devilbonegambler.config.ItemValueConfig;
import com.devilbonegambler.config.PrizeEntry;
import com.devilbonegambler.config.PrizePoolConfig;
import com.devilbonegambler.config.PrizeRegion;
import com.devilbonegambler.config.ServerConfigManager;
import com.devilbonegambler.data.LuckData;
import com.devilbonegambler.dealer.DealerRewardService;
import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.RequestGuard;
import com.devilbonegambler.network.packet.DrawResultPacket;
import com.devilbonegambler.network.packet.PointDisplayPacket;
import com.devilbonegambler.commands.CommandRunner;
import com.devilbonegambler.util.ItemValuation;
import com.devilbonegambler.util.ModConstants;
import com.devilbonegambler.util.Placeholders;
import com.devilbonegambler.util.ServerScheduler;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Server authoritative draw engine.
 * Validation, the roll and the region hit run inside one server thread task; the command, the deduction and the
 * result are then scheduled after the point display delay so that the player sees the roll before the outcome.
 */
public final class DrawEngine
{
    private static final double FOOD_LEVEL_MAX = 20.0D;

    private DrawEngine()
    {
    }

    public static DrawResult perform(ServerPlayer player, String itemId, OfferingBundle bundle, int sequence)
    {
        if (!ServerConfigManager.isReady())
        {
            return DrawResult.failure("not_ready");
        }

        Item dice = resolveItem(itemId);

        if (dice == null || countItem(player, dice) < 1)
        {
            return DrawResult.failure("item_mismatch");
        }

        OfferingBundle offeringBundle = bundle == null ? OfferingBundle.empty() : bundle;
        boolean sacrifice = !offeringBundle.isEmpty();

        if (sacrifice && !hasOfferings(player, offeringBundle))
        {
            return DrawResult.failure("not_enough_items");
        }

        PrizePoolConfig pool = ServerConfigManager.prizePool();
        List<PrizeRegion> regions = pool.sortedRegions();

        if (regions.isEmpty())
        {
            return DrawResult.failure("empty_pool");
        }

        long totalValue = totalValue(player, offeringBundle);
        long seed = DrawSeed.of(player, System.currentTimeMillis(), sequence, offeringBundle.items());
        RandomSource random = RandomSource.create(seed);
        int luck = currentLuck(player);
        DiceRoll roll = DiceRoll.roll(random, luck, altarValueFor(player));

        int baseTier = RegionSelector.baseTier(regions, pool.maxTier(), roll.point());
        int targetTier = RegionSelector.clampTier(baseTier, pool.minTier(), pool.maxTier());
        int overflow = RegionSelector.overflow(roll.point(), targetTier);
        PrizeRegion region = RegionSelector.select(regions, targetTier);

        if (region == null)
        {
            return DrawResult.failure("empty_pool");
        }

        PrizeEntry hit = RegionSelector.pick(random, region, roll.isRed(), overflow);

        if (hit == null)
        {
            return DrawResult.failure("zero_weight");
        }

        double weightSum = RegionSelector.totalWeight(region, roll.isRed(), overflow);

        ModNetwork.sendToPlayer(player, new PointDisplayPacket(sequence, roll.point(), roll.isRed(), overflow));
        RequestGuard.markBusy(player, ModConstants.POINT_DISPLAY_TICKS);

        PendingDraw pending = new PendingDraw(player.getUUID(), sequence, itemId, roll, baseTier, targetTier, overflow,
                region.tierOrZero(), hit, weightSum, totalValue, offeringBundle, luck, seed);
        ServerScheduler.schedule(ModConstants.POINT_DISPLAY_TICKS, server -> resolve(server, pending));

        return DrawResult.accepted();
    }

    /**
     * Consumes one dice of the given item, used by flows that do not run through the delayed resolution.
     */
    public static boolean consumeDice(ServerPlayer player, String itemId)
    {
        Item dice = resolveItem(itemId);
        return dice != null && deductItem(player, dice, 1);
    }

    /**
     * Second half of the draw, running after the point display delay. An offline player costs nothing.
     */
    private static void resolve(MinecraftServer server, PendingDraw pending)
    {
        ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId);

        if (player == null)
        {
            DevilBoneGambler.LOGGER.info("Draw aborted, the player left before the result, player={}",
                    pending.playerId);
            return;
        }

        int commandResult = CommandRunner.run(server, player, pending.hit.commandOrEmpty());

        if (commandResult <= 0)
        {
            audit(player, pending, commandResult, false, 0L);
            NotifySender.sendFailure(player, "command_failed");
            ModNetwork.sendToPlayer(player, new DrawResultPacket(pending.sequence, false, "command_failed",
                    pending.itemId, 1));
            return;
        }

        Item dice = resolveItem(pending.itemId);
        boolean consumed = dice != null && deductItem(player, dice, 1);

        if (consumed)
        {
            DealerRewardService.recordDiceUse(player);
        }

        long extraValue = 0L;

        if (consumed)
        {
            consumed = deductOfferings(player, pending.bundle);
            extraValue = collectDropValue(player, pending.bundle);
        }

        if (!consumed)
        {
            DevilBoneGambler.LOGGER.error(
                    "Deduction failed after a successful draw, player={} item={} value={} entry={}",
                    player.getGameProfile().getName(), pending.itemId, Long.valueOf(pending.totalValue),
                    pending.hit.id);
        }

        NotifySender.sendEntryResult(player, pending.hit.notifyOrDefault());

        if (consumed)
        {
            settleOfferingValue(player, pending.totalValue);

            if (pending.totalValue > 0L)
            {
                NotifySender.sendChat(player, "gui.devil_bone_gambler.altar.offered_total",
                        Long.valueOf(pending.totalValue));
            }
        }

        if (pending.hit.isMajorEvent())
        {
            NotifySender.broadcastMajorEvent(player, pending.hit);
        }

        audit(player, pending, commandResult, consumed, extraValue);
        ModNetwork.sendToPlayer(player, new DrawResultPacket(pending.sequence, true, "", pending.itemId, 1));
    }

    /**
     * Luck curve hook. The luck value is read but does not change the entry weights.
     */
    private static int currentLuck(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return ModConstants.BASE_LUCK;
        }

        return LuckData.get(server).luckOf(player.getUUID());
    }

    /**
     * Highest altar value in range, or zero when no altar is nearby.
     */
    private static long altarValueFor(ServerPlayer player)
    {
        return AltarService.altarValueFor(player);
    }

    /**
     * Value of one unit of an offering, including its enchantment bonus and its remaining durability.
     */
    private static int offeringValue(ItemValueConfig values, Offering offering)
    {
        return ItemValuation.value(values.itemValueOf(offering.itemId()), offering.damage(),
                maxDamageOf(offering.itemId()), offering.enchantLevels(), values.enchantValueBaseOrDefault());
    }

    /**
     * Maximum durability of the item behind a registry name, zero when the item has none.
     */
    private static int maxDamageOf(String itemId)
    {
        ResourceLocation key = ResourceLocation.tryParse(itemId);

        if (key == null)
        {
            return 0;
        }

        Item item = ForgeRegistries.ITEMS.getValue(key);
        return item == null ? 0 : new ItemStack(item).getMaxDamage();
    }

    private static long totalValue(ServerPlayer player, OfferingBundle bundle)
    {
        ItemValueConfig values = ServerConfigManager.itemValues();
        long total = 0L;

        for (Offering offering : bundle.items())
        {
            total += (long) offeringValue(values, offering) * (long) offering.count();
        }

        total += (long) bundle.health() * 2L;
        total += bundle.food();

        if (hasNumericOffering(bundle))
        {
            total += experienceValue(player);
        }

        for (UUID mobId : bundle.mobs())
        {
            Entity entity = player.serverLevel().getEntity(mobId);

            if (entity != null)
            {
                total += values.entityValueOf(entityTypeId(entity));
            }
        }

        return Math.min(total, ModConstants.MAX_TOTAL_VALUE);
    }

    /**
     * Experience levels are offered in full once any numeric offering is selected.
     */
    private static long experienceValue(ServerPlayer player)
    {
        double healthRatio = player.getMaxHealth() <= 0.0F
                ? 0.0D
                : player.getHealth() / (double) player.getMaxHealth();
        double foodRatio = player.getFoodData().getFoodLevel() / FOOD_LEVEL_MAX;
        return (long) (player.experienceLevel * (1.0D + healthRatio * foodRatio));
    }

    private static boolean hasNumericOffering(OfferingBundle bundle)
    {
        return bundle.health() > 0 || bundle.food() > 0 || bundle.allHealth();
    }

    private static String entityTypeId(Entity entity)
    {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key == null ? "" : key.toString();
    }

    private static boolean hasOfferings(ServerPlayer player, OfferingBundle bundle)
    {
        for (Offering offering : bundle.items())
        {
            Item item = resolveItem(offering.itemId());

            if (item == null || countItem(player, item) < offering.count())
            {
                return false;
            }
        }

        for (UUID mobId : bundle.mobs())
        {
            Entity entity = player.serverLevel().getEntity(mobId);

            if (entity == null || entity instanceof Player)
            {
                return false;
            }
        }

        if (bundle.health() > availableHealth(player, bundle))
        {
            return false;
        }

        return bundle.food() <= player.getFoodData().getFoodLevel();
    }

    /**
     * A partial health offering always keeps one point; only the full sacrifice option may kill the player.
     */
    private static int availableHealth(ServerPlayer player, OfferingBundle bundle)
    {
        int health = (int) Math.ceil(player.getHealth());
        return bundle.allHealth() ? health : Math.max(0, health - 1);
    }

    private static boolean deductOfferings(ServerPlayer player, OfferingBundle bundle)
    {
        boolean result = true;

        for (Offering offering : bundle.items())
        {
            Item item = resolveItem(offering.itemId());

            if (item == null || !deductItem(player, item, offering.count()))
            {
                result = false;
            }
        }

        for (UUID mobId : bundle.mobs())
        {
            if (!killMob(player, mobId))
            {
                result = false;
            }
        }

        if (bundle.allHealth())
        {
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        }
        else if (bundle.health() > 0)
        {
            player.setHealth(Math.max(1.0F, player.getHealth() - bundle.health()));
        }

        if (bundle.food() > 0)
        {
            player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - bundle.food()));
        }

        if (hasNumericOffering(bundle) && player.experienceLevel > 0)
        {
            player.giveExperienceLevels(-player.experienceLevel);
        }

        return result;
    }

    /**
     * Kills one mob offering and recovers its drops so that they never enter the level.
     */
    private static boolean killMob(ServerPlayer player, UUID mobId)
    {
        Entity entity = player.serverLevel().getEntity(mobId);

        if (entity == null || entity instanceof Player)
        {
            return false;
        }

        DropCollector.begin(mobId);

        if (entity instanceof LivingEntity living)
        {
            living.kill();
        }
        else
        {
            entity.kill();
        }

        return true;
    }

    /**
     * Value of the recovered drops, added to the altar as extra value and never told to the client.
     */
    private static long collectDropValue(ServerPlayer player, OfferingBundle bundle)
    {
        ItemValueConfig values = ServerConfigManager.itemValues();
        long total = 0L;

        for (UUID mobId : bundle.mobs())
        {
            for (ItemStack drop : DropCollector.end(mobId))
            {
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(drop.getItem());

                if (key != null)
                {
                    total += (long) ItemValuation.stackValue(drop, values.itemValueOf(key.toString()),
                            values.enchantValueBaseOrDefault()) * (long) drop.getCount();
                }
            }
        }

        return total;
    }

    /**
     * Records the value of the completed sacrifice into the altar the player used.
     */
    public static void settleOfferingValue(ServerPlayer player, long total)
    {
        if (total > 0L)
        {
            AltarService.accumulateOfferingValue(player, total);
        }
    }

    private static int countItem(ServerPlayer player, Item item)
    {
        Inventory inventory = player.getInventory();
        int total = 0;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);

            if (!stack.isEmpty() && stack.getItem() == item)
            {
                total += stack.getCount();
            }
        }

        return total;
    }

    private static boolean deductItem(ServerPlayer player, Item item, int count)
    {
        if (count <= 0)
        {
            return true;
        }

        if (countItem(player, item) < count)
        {
            return false;
        }

        Inventory inventory = player.getInventory();
        int remaining = count;

        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++)
        {
            ItemStack stack = inventory.getItem(slot);

            if (stack.isEmpty() || stack.getItem() != item)
            {
                continue;
            }

            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
        }

        return remaining == 0;
    }

    private static Item resolveItem(String itemId)
    {
        ResourceLocation key = ResourceLocation.tryParse(itemId);
        return key == null ? null : ForgeRegistries.ITEMS.getValue(key);
    }

    private static void audit(ServerPlayer player, PendingDraw pending, int commandResult, boolean consumed,
            long extraValue)
    {
        DevilBoneGambler.LOGGER.info(
                "Draw player={} item={} value={} extraValue={} luck={} seed={} point={} color={} overflow={} baseTier={} targetTier={} hitTier={} entry={} entryColor={} weight={} regionWeight={} majorEvent={} commandResult={} consumed={}",
                player.getGameProfile().getName(), pending.itemId, Long.valueOf(pending.totalValue),
                Long.valueOf(extraValue), Integer.valueOf(pending.luck), Long.valueOf(pending.seed),
                Integer.valueOf(pending.roll.point()), pending.roll.colorId(), Integer.valueOf(pending.overflow),
                Integer.valueOf(pending.baseTier), Integer.valueOf(pending.targetTier),
                Integer.valueOf(pending.hitTier), pending.hit.id, pending.hit.colorOrDefault(),
                Double.valueOf(pending.hit.weightOrZero()), Double.valueOf(pending.weightSum),
                Boolean.valueOf(pending.hit.isMajorEvent()), Integer.valueOf(commandResult),
                Boolean.valueOf(consumed));
    }

    /**
     * Everything the delayed resolution needs, captured while the player is still online.
     */
    private static final class PendingDraw
    {
        private final UUID playerId;
        private final int sequence;
        private final String itemId;
        private final DiceRoll roll;
        private final int baseTier;
        private final int targetTier;
        private final int overflow;
        private final int hitTier;
        private final PrizeEntry hit;
        private final double weightSum;
        private final long totalValue;
        private final OfferingBundle bundle;
        private final int luck;
        private final long seed;

        private PendingDraw(UUID playerId, int sequence, String itemId, DiceRoll roll, int baseTier, int targetTier,
                int overflow, int hitTier, PrizeEntry hit, double weightSum, long totalValue, OfferingBundle bundle,
                int luck, long seed)
        {
            this.playerId = playerId;
            this.sequence = sequence;
            this.itemId = itemId;
            this.roll = roll;
            this.baseTier = baseTier;
            this.targetTier = targetTier;
            this.overflow = overflow;
            this.hitTier = hitTier;
            this.hit = hit;
            this.weightSum = weightSum;
            this.totalValue = totalValue;
            this.bundle = bundle;
            this.luck = luck;
            this.seed = seed;
        }
    }
}
