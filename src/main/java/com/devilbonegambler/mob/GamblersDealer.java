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
import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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
    private static final String KEY_IDLE_NAME = "gui." + ModConstants.MOD_ID + ".dealer.name_tag";

    private GamblersDealer rival;
    private int territoryCheckTicks;
    private int territoryAttackTicks;
    private int satiety = ModConstants.DEALER_MAX_SATIETY;
    private int stayTicks = ModConstants.DEALER_STAY_SECONDS * ModConstants.TICKS_PER_SECOND;
    private int feedCooldown = ModConstants.DEALER_FEED_INTERVAL_TICKS;
    private boolean hosting;

    public GamblersDealer(EntityType<? extends GamblersDealer> type, Level level)
    {
        super(type, level);
        this.setPersistenceRequired();
        /* Every dealer carries its idle name from the start, no matter how it was created. */
        this.showIdleName();
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
     * Whether the dealer is currently driving an intruder out of its territory.
     */
    public boolean isDefendingTerritory()
    {
        return this.rival != null;
    }

    /**
     * A dealer claims the space around it: another dealer inside its own radius is chased and attacked until one of
     * the two dies. The check runs on a fixed interval instead of every tick, and the pursuit itself runs every tick
     * so a fight never stalls between two checks.
     */
    private void tickTerritory()
    {
        if (this.territoryCheckTicks > 0)
        {
            this.territoryCheckTicks--;
        }
        else
        {
            this.territoryCheckTicks = ModConstants.DEALER_TERRITORY_CHECK_INTERVAL_TICKS;
            this.rival = this.findIntruder();
        }

        if (this.rival == null)
        {
            return;
        }

        if (!this.rival.isAlive() || this.rival.level() != this.level())
        {
            this.rival = null;
            this.getNavigation().stop();
            return;
        }

        /*
         * Two dealers inside reach push each other apart, so the aim vector flips between opposite directions while
         * they overlap; the aim is left untouched there instead of making the head spin every tick.
         */
        if (this.distanceToSqr(this.rival) > ModConstants.DEALER_TERRITORY_LOOK_MIN_SQR)
        {
            this.getLookControl().setLookAt(this.rival, 30.0F, 30.0F);
        }

        double reach = ModConstants.DEALER_TERRITORY_ATTACK_REACH;

        if (this.distanceToSqr(this.rival) > reach * reach)
        {
            this.getNavigation().moveTo(this.rival, ModConstants.DEALER_TERRITORY_CHASE_SPEED);
            return;
        }

        this.getNavigation().stop();

        if (this.territoryAttackTicks > 0)
        {
            this.territoryAttackTicks--;
            return;
        }

        this.territoryAttackTicks = ModConstants.DEALER_TERRITORY_ATTACK_INTERVAL_TICKS;
        this.swing(InteractionHand.MAIN_HAND);
        this.rival.hurt(this.damageSources().mobAttack(this), this.rollTerritoryDamage());
    }

    /**
     * Nearest other living dealer inside the own territory, or null when the territory is clear.
     */
    private GamblersDealer findIntruder()
    {
        List<GamblersDealer> others = this.level().getEntitiesOfClass(GamblersDealer.class,
                this.getBoundingBox().inflate(ModConstants.DEALER_TERRITORY_RADIUS),
                other -> other != this && other.isAlive());
        double limit = ModConstants.DEALER_TERRITORY_RADIUS * ModConstants.DEALER_TERRITORY_RADIUS;
        GamblersDealer nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (GamblersDealer other : others)
        {
            double distance = this.distanceToSqr(other);

            if (distance <= limit && distance < nearestDistance)
            {
                nearest = other;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    /**
     * Damage of one territory hit, somewhere between zero and five points.
     * The seed mixes the timestamp with the own health, satiety and position, so every hit rolls its own number and
     * the fight decides a winner instead of ending with both dealers dead.
     */
    private float rollTerritoryDamage()
    {
        long seed = System.nanoTime()
                ^ ((long) this.getHealth() << 40)
                ^ ((long) this.satiety << 24)
                ^ Double.doubleToLongBits(this.getX()) * 0x9E3779B97F4A7C15L
                ^ Double.doubleToLongBits(this.getY()) * 0xC2B2AE3D27D4EB4FL
                ^ Double.doubleToLongBits(this.getZ()) * 0x165667B19E3779F9L
                ^ (long) this.getId();
        return new Random(seed).nextFloat() * ModConstants.DEALER_TERRITORY_MAX_DAMAGE;
    }

    /**
     * Feeding and the stay countdown run next to the goal machine, so neither of them can be starved by a goal.
     */
    @Override
    protected void customServerAiStep()
    {
        super.customServerAiStep();
        this.tickTerritory();

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

    /**
     * The recruit counter on the name tag. It never makes a sound of its own, because the panel and the dealer would
     * otherwise chirp once per second for the whole recruit.
     */
    public void showRecruitText(int seconds)
    {
        this.setCustomName(Component.translatable(KEY_RECRUITING, Integer.valueOf(seconds)));
        this.setCustomNameVisible(true);
    }

    public void clearRecruitText()
    {
        this.setCustomName(Component.translatable(KEY_IDLE_NAME));
        this.setCustomNameVisible(true);
    }

    /**
     * The idle name tag carries the running joke, the recruit text takes it over while a wager is open.
     */
    public void showIdleName()
    {
        this.setCustomName(Component.translatable(KEY_IDLE_NAME));
        this.setCustomNameVisible(true);
    }

    /**
     * A small heart burst, used when the dealer starts recruiting and when a wager settles.
     */
    public void dealingParticles()
    {
        if (!(this.level() instanceof ServerLevel level))
        {
            return;
        }

        level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 2.0D, this.getZ(),
                ModConstants.DEALER_PARTICLE_COUNT, 0.35D, 0.35D, 0.35D, 0.02D);
        level.playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
                SoundSource.NEUTRAL, ModConstants.DEALER_SOUND_VOLUME, 1.4F);
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

        /* The dealer answers with the villager voice, the same family as its ambient and hurt sounds. */
        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);

        if (player instanceof ServerPlayer serverPlayer)
        {
            DealerWagerService.open(serverPlayer, this.getId());
        }

        return InteractionResult.CONSUME;
    }

    /**
     * A well fed dealer leaves gambler dice behind: the count scales with the satiety left, a starving dealer leaves
     * nothing and a full one leaves the whole handful.
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int count = this.deathDiceDrops();

        if (count > 0)
        {
            this.spawnAtLocation(new ItemStack(ModItems.GAMBLERS_DICE.get(), count));
        }
    }

    /**
     * Linear in the satiety and rounded down, so half a bar leaves half the handful.
     */
    public int deathDiceDrops()
    {
        int clamped = Math.max(0, Math.min(ModConstants.DEALER_MAX_SATIETY, this.satiety));
        return (int) (ModConstants.DEALER_DEATH_DICE_DROPS
                * (clamped / (double) ModConstants.DEALER_MAX_SATIETY));
    }

    /*
     * The dealer speaks with the villager voice set: ambient, hurt and death are the vanilla ones, so a dealer sounds
     * like the village host it is meant to be.
     */
    @Override
    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source)
    {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return SoundEvents.VILLAGER_DEATH;
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
        this.showIdleName();
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
