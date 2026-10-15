/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.mob;

import com.devilbonegambler.dealer.DealerWagerService;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Gambler's Dealer: a villager shaped host that owns one wager at a time.
 * The satiety bar is a mod owned value, the health bar is the vanilla one; both are shown on the dealer panel
 * without telling the player how the satiety shortage is paid with health.
 */
public class GamblersDealer extends PathfinderMob
{
    private static final String TAG_SATIETY = "DealerSatiety";
    private static final String TAG_STAY = "DealerStayTicks";
    private static final String KEY_RECRUITING = "gui." + ModConstants.MOD_ID + ".dealer.recruiting";

    private int satiety = ModConstants.DEALER_MAX_SATIETY;
    private int stayTicks = ModConstants.DEALER_STAY_SECONDS * ModConstants.TICKS_PER_SECOND;
    private int feedCooldown = ModConstants.DEALER_FEED_INTERVAL_TICKS;
    private boolean hosting;

    public GamblersDealer(EntityType<? extends GamblersDealer> type, Level level)
    {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, ModConstants.DEALER_MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DealerWanderGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    /**
     * Feeding and the stay countdown run next to the goal machine, so neither of them can be starved by a goal.
     */
    @Override
    protected void customServerAiStep()
    {
        super.customServerAiStep();

        if (this.feedCooldown > 0)
        {
            this.feedCooldown--;
        }
        else
        {
            this.feedCooldown = ModConstants.DEALER_FEED_INTERVAL_TICKS;
            this.tryFeed();
        }

        if (this.hosting)
        {
            this.stayTicks = ModConstants.DEALER_STAY_SECONDS * ModConstants.TICKS_PER_SECOND;
        }
        else if (this.stayTicks > 0)
        {
            this.stayTicks--;
        }
    }

    /**
     * One gambler's dice drop is eaten and refills both bars at once. A full dealer ignores nearby drops.
     */
    private void tryFeed()
    {
        if (this.getHealth() >= this.getMaxHealth() && this.satiety >= ModConstants.DEALER_MAX_SATIETY)
        {
            return;
        }

        List<ItemEntity> drops = this.level().getEntitiesOfClass(ItemEntity.class,
                this.getBoundingBox().inflate(ModConstants.DEALER_FEED_RADIUS));

        for (ItemEntity drop : drops)
        {
            ItemStack stack = drop.getItem();

            if (!stack.is(ModItems.GAMBLERS_DICE.get()))
            {
                continue;
            }

            stack.shrink(1);

            if (stack.isEmpty())
            {
                drop.discard();
            }

            this.setHealth(this.getMaxHealth());
            this.satiety = ModConstants.DEALER_MAX_SATIETY;
            this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
            return;
        }
    }

    public int satiety()
    {
        return this.satiety;
    }

    public void setSatiety(int value)
    {
        this.satiety = Math.max(0, Math.min(ModConstants.DEALER_MAX_SATIETY, value));
    }

    public boolean isHosting()
    {
        return this.hosting;
    }

    public void setHosting(boolean hosting)
    {
        this.hosting = hosting;

        if (hosting)
        {
            this.beginStay();
        }
    }

    public int stayTicks()
    {
        return this.stayTicks;
    }

    public void beginStay()
    {
        this.stayTicks = ModConstants.DEALER_STAY_SECONDS * ModConstants.TICKS_PER_SECOND;
    }

    public void showRecruitText(int seconds)
    {
        this.setCustomName(Component.translatable(KEY_RECRUITING, Integer.valueOf(seconds)));
        this.setCustomNameVisible(true);
    }

    public void clearRecruitText()
    {
        this.setCustomName(null);
        this.setCustomNameVisible(false);
    }

    /**
     * One successful wager costs one satiety point per member; the shortage is paid with health, one health per two
     * satiety, which can kill the dealer.
     */
    public void consumeForWager(int players)
    {
        int cost = Math.max(0, players);

        if (this.satiety >= cost)
        {
            this.satiety -= cost;
            return;
        }

        int rest = cost - this.satiety;
        this.satiety = 0;
        int healthCost = (int) Math.ceil(rest / (double) ModConstants.DEALER_SATIETY_PER_HEALTH);
        this.invulnerableTime = 0;
        this.hurt(this.damageSources().magic(), (float) healthCost);
    }

    /**
     * An empty hand right click opens the panel or joins the running recruit. A filled hand keeps the vanilla
     * behaviour, so placing a block against the dealer is untouched.
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty())
        {
            return super.mobInteract(player, hand);
        }

        if (this.level().isClientSide)
        {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer)
        {
            DealerWagerService.open(serverPlayer, this.getId());
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_SATIETY, this.satiety);
        tag.putInt(TAG_STAY, this.stayTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.satiety = tag.contains(TAG_SATIETY) ? tag.getInt(TAG_SATIETY) : ModConstants.DEALER_MAX_SATIETY;
        this.stayTicks = tag.contains(TAG_STAY) ? tag.getInt(TAG_STAY)
                : ModConstants.DEALER_STAY_SECONDS * ModConstants.TICKS_PER_SECOND;
        this.hosting = false;
        this.clearRecruitText();
    }

    /**
     * A dealer never despawns, it is either placed by a spawn egg or created by the silent spawn rule.
     */
    @Override
    public boolean removeWhenFarAway(double distance)
    {
        return false;
    }
}
