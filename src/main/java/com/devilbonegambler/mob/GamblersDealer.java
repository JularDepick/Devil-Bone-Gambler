/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.devilbonegambler.registry.ModBlocks;
import com.devilbonegambler.dealer.DealerWagerService;
import com.devilbonegambler.registry.ModEntities;
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
import net.minecraft.world.phys.Vec3;

/**
 * Gambler's Dealer: a villager shaped host that owns one wager at a time.
 * The satiety bar is a mod owned value, the health bar is the vanilla one; both are shown on the dealer panel
 * without telling the player how the satiety shortage is paid with health.
 */
public class GamblersDealer extends PathfinderMob
{
    private static final String TAG_SATIETY = "DealerSatiety";
    private static final String TAG_STAY = "DealerStayTicks";
    private static final String TAG_HURT_DROP_START = "DealerHurtDropStart";
    private static final String TAG_HURT_DROP_COUNT = "DealerHurtDropCount";
    private static final String TAG_POCKET = "DealerPocket";
    private static final String TAG_EXPERIENCE = "DealerExperience";
    private static final String KEY_RECRUIT_TITLE = "gui." + ModConstants.MOD_ID + ".dealer.recruit_title";
    private static final String KEY_RECRUIT_COUNT = "gui." + ModConstants.MOD_ID + ".dealer.recruit_count";
    private static final String KEY_IDLE_NAME = "gui." + ModConstants.MOD_ID + ".dealer.name_tag";

    private GamblersDealer rival;
    private int territoryCheckTicks;
    private int territoryAttackTicks;
    /* The block the dealer is currently lighting, and the countdown until the next look at it. */
    private BlockPos glowAt;
    private int glowTicks;
    private double satiety = ModConstants.DEALER_MAX_SATIETY;
    /* The dice in the pocket and the experience gathered from the wasted part of a meal. */
    private int pocketDice = ModConstants.DEALER_POCKET_INITIAL_DICE;
    private double experience;
    /* The one minute window of hurt drops, opened by the hit that first frees dice, and what it has already handed out. */
    private long hurtDropWindowStart = -1L;
    private int hurtDropWindowCount;
    private int stayTicks = ModConstants.DEALER_STAY_SECONDS * ModConstants.TICKS_PER_SECOND;
    private int eatTicks;
    private int eatCooldown;
    private int pickupTicks = ModConstants.DEALER_PICKUP_INTERVAL_TICKS;
    /* Countdown to the next look for a shore while the dealer is standing in liquid. */
    private BlockPos escapeShore;
    /* Countdown to the next drain or recovery step of the two bars. */
    private int decayTicks = ModConstants.DEALER_DECAY_INTERVAL_TICKS;
    /* The tick of the last player hit that really landed, for the one second window of an idle dealer. */
    private long lastPlayerHitTick = -ModConstants.DEALER_PLAYER_HIT_COOLDOWN_TICKS;
    private boolean hosting;
    /* Business names (hosting, recruiting) are shown to everyone, the idle name only to the player who looks at it. */
    private boolean showNameToEveryone;
    /* The recruit name is two centred lines, so the renderer draws it instead of the vanilla single line name tag. */
    private boolean recruitLines;
    private int recruitMembers;
    private int recruitSeconds;

    public GamblersDealer(EntityType<? extends GamblersDealer> type, Level level)
    {
        super(type, level);
        this.setPersistenceRequired();
        /* Every dealer carries its idle name from the start, no matter how it was created. */
        this.showIdleName();
        this.applyProgress();
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, ModConstants.DEALER_MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    /**
     * The dealer is neutral. Neither its type nor its behaviour makes it a monster, and no mob counts it as an enemy,
     * so hostile creatures never pick it up as a target; only another dealer fights it, and that fight calls the damage
     * directly instead of asking the target selectors.
     */
    @Override
    public boolean canBeSeenAsEnemy()
    {
        return false;
    }

    /**
     * The dealer attacks dealers and nothing else, even if a future goal asks whether some other creature is fair game.
     */
    @Override
    public boolean canAttackType(EntityType<?> type)
    {
        return type == ModEntities.GAMBLERS_DEALER.get();
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
        /* A dealer that recruits or hosts a wager sits out of the dealer against dealer fights. */
        return this.rival != null && !this.isHosting();
    }

    /**
     * A dealer claims the space around it: another dealer inside its own radius is chased and attacked until one of
     * the two dies. The check runs on a fixed interval instead of every tick, and the pursuit itself runs every tick
     * so a fight never stalls between two checks.
     */
    private void tickTerritory()
    {
        /*
         * Business comes first: while a wager recruits or runs the dealer neither chases a rival nor keeps one, so a
         * fight cannot start or continue around a table that is in use.
         */
        if (this.isHosting())
        {
            this.rival = null;
            this.territoryCheckTicks = 0;
            this.getNavigation().stop();
            return;
        }

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

        if (!this.rival.isAlive() || this.rival.level() != this.level() || this.rival.isHosting())
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

        /*
         * A swing is paid for with the bar: half a percent of the ceiling each time, and an empty bar leaves the dealer
         * standing in front of its rival without attacking, so a long fight slows down and then stops.
         */
        if (this.satiety <= 0)
        {
            return;
        }

        this.territoryAttackTicks = ModConstants.DEALER_TERRITORY_ATTACK_INTERVAL_TICKS;
        this.swing(InteractionHand.MAIN_HAND);
        this.rival.hurt(this.damageSources().mobAttack(this), this.rollTerritoryDamage());
        this.satiety = Math.max(0.0D,
                this.satiety - this.maxSatiety() * ModConstants.DEALER_SWING_COST_PERCENT / 100.0D);
    }

    /**
     * Nearest other living dealer inside the own territory, or null when the territory is clear.
     */
    private GamblersDealer findIntruder()
    {
        List<GamblersDealer> others = this.level().getEntitiesOfClass(GamblersDealer.class,
                this.getBoundingBox().inflate(ModConstants.DEALER_TERRITORY_RADIUS),
                other -> other != this && other.isAlive() && !other.isHosting());
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
     * The dealer carries its own glow, so the block under its feet is kept lit and the block it left behind goes dark
     * again. The check runs on an interval rather than every tick, because a dealer rarely leaves a block behind and
     * the light engine only needs to be told once per move.
     */
    private void tickGlow()
    {
        if (this.level().isClientSide)
        {
            return;
        }

        if (this.glowTicks > 0)
        {
            this.glowTicks--;
            return;
        }

        this.glowTicks = ModConstants.DEALER_GLOW_INTERVAL_TICKS;
        BlockPos standing = this.blockPosition();

        if (!this.isAlive())
        {
            this.clearGlow();
            return;
        }

        if (standing.equals(this.glowAt))
        {
            return;
        }

        this.clearGlow();
        BlockState current = this.level().getBlockState(standing);

        /*
         * Only empty space is taken: a glow never pushes a block out of the world, and a block that is solid enough to
         * stand on simply stays, because the dealer is lit by whatever is around it there anyway.
         */
        if (current.isAir() || current.canBeReplaced())
        {
            this.level().setBlock(standing, ModBlocks.DEALER_GLOW.get().defaultBlockState(), Block.UPDATE_NONE);
            this.glowAt = standing.immutable();
        }
    }

    /**
     * Puts the last lit block out. Only a block that is still the own glow is removed, so a player who placed
     * something there keeps it.
     */
    private void clearGlow()
    {
        if (this.glowAt == null)
        {
            return;
        }

        if (this.level().getBlockState(this.glowAt).is(ModBlocks.DEALER_GLOW.get()))
        {
            this.level().setBlock(this.glowAt, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        }

        this.glowAt = null;
    }

    /**
     * The glow is not part of the loot and never outlives its dealer.
     */
    @Override
    public void remove(Entity.RemovalReason reason)
    {
        this.clearGlow();
        super.remove(reason);
    }

    /**
     * Damage of one territory hit: the current health ceiling divided by the player base, plus a roll between zero and
     * five. Experience lifts the ceiling, so a grown dealer also hits harder.
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
        float base = this.getMaxHealth() / ModConstants.PLAYER_BASE_MAX_HEALTH;
        return base + new Random(seed).nextFloat() * ModConstants.DEALER_TERRITORY_MAX_DAMAGE;
    }

    /**
     * True while the dealer stands in water, a bubble column or lava. The dealer hates liquid, so this is also the
     * switch that puts everything else on hold.
     */
    public boolean isInLiquid()
    {
        return this.isInWaterOrBubble() || this.isInLava();
    }

    /**
     * A dealer in liquid leaves it at once. The nearest dry spot is searched once and then kept until it holds, so the
     * move control can drive straight at it every tick without rescanning the ring by ring search, and neither a stray
     * path nor a rival can keep it in the water; the float goal keeps its head above the surface in the meantime.
     */
    private void tickLiquidEscape()
    {
        this.getNavigation().stop();

        if (this.escapeShore == null || !this.isDryStand(this.escapeShore))
        {
            this.escapeShore = this.findShore();
        }

        if (this.escapeShore == null)
        {
            return;
        }

        double targetX = this.escapeShore.getX() + 0.5D;
        double targetY = this.escapeShore.getY();
        double targetZ = this.escapeShore.getZ() + 0.5D;

        this.getMoveControl().setWantedPosition(targetX, targetY, targetZ,
                ModConstants.DEALER_LIQUID_ESCAPE_SPEED);
        this.getLookControl().setLookAt(targetX, targetY + 0.5D, targetZ, 30.0F, 30.0F);

        /*
         * Liquid swallows a walk, so once the shore is within reach the dealer shoves itself out with a short upward
         * burst instead of letting the drag decide how long it stays in the lava or the water.
         */
        if (this.distanceToSqr(targetX, this.getY(), targetZ) < 9.0D)
        {
            Vec3 direction = new Vec3(targetX - this.getX(), 0.0D, targetZ - this.getZ());

            if (direction.lengthSqr() > 1.0E-4D)
            {
                direction = direction.normalize();
                this.setDeltaMovement(this.getDeltaMovement().add(direction.x * 0.09D, 0.14D, direction.z * 0.09D));
                this.hasImpulse = true;
            }
        }
    }

    /**
     * Nearest dry spot around the dealer, searched ring by ring so the closest shore wins. A spot may sit one or two
     * blocks below or above, which covers a bank next to the water and a hole the dealer fell into.
     */
    private BlockPos findShore()
    {
        BlockPos origin = this.blockPosition();
        int[] heights = {0, -1, 1, -2, 2};

        for (int distance = 1; distance <= ModConstants.DEALER_LIQUID_ESCAPE_RADIUS; distance++)
        {
            for (int dx = -distance; dx <= distance; dx++)
            {
                for (int dz = -distance; dz <= distance; dz++)
                {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != distance)
                    {
                        continue;
                    }

                    for (int dy : heights)
                    {
                        BlockPos candidate = origin.offset(dx, dy, dz);

                        if (this.isDryStand(candidate))
                        {
                            return candidate;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isDryStand(BlockPos pos)
    {
        Level level = this.level();
        BlockPos below = pos.below();
        BlockState floor = level.getBlockState(below);

        /* The spot needs solid ground that does not burn, and air (or a plant) instead of liquid at feet and head. */
        if (!floor.isFaceSturdy(level, below, Direction.UP) || floor.is(Blocks.MAGMA_BLOCK))
        {
            return false;
        }

        BlockState feet = level.getBlockState(pos);

        if (!feet.getFluidState().isEmpty() || feet.is(Blocks.FIRE) || feet.is(Blocks.SOUL_FIRE))
        {
            return false;
        }

        BlockState head = level.getBlockState(pos.above());

        return head.getFluidState().isEmpty()
                && feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty();
    }

    /**
     * Feeding and the stay countdown run next to the goal machine, so neither of them can be starved by a goal.
     */
    @Override
    protected void customServerAiStep()
    {
        super.customServerAiStep();

        if (this.isInLiquid())
        {
            this.tickLiquidEscape();
        }
        else
        {
            this.escapeShore = null;
            this.tickTerritory();
        }

        this.tickGlow();
        this.tickSatiety();

        this.tickEating();

        if (this.pickupTicks > 0)
        {
            this.pickupTicks--;
        }
        else
        {
            this.pickupTicks = ModConstants.DEALER_PICKUP_INTERVAL_TICKS;
            this.tryPickup();
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
     * The bars drain with time: one whole satiety bar per day, checked once per second. A recruit or a wager stops the
     * drain and refills both bars at the same rate, because the players are feeding the dealer while it hosts. A
     * satiety bar that runs dry spills into health, two percent of satiety for one percent of health.
     */
    private void tickSatiety()
    {
        if (this.decayTicks > 0)
        {
            this.decayTicks--;
            return;
        }

        this.decayTicks = ModConstants.DEALER_DECAY_INTERVAL_TICKS;
        double maxSatiety = this.maxSatiety();
        double stepPercent = ModConstants.DEALER_DECAY_PERCENT_PER_INTERVAL;
        double satietyStep = maxSatiety * stepPercent / 100.0D;

        if (this.hosting)
        {
            this.satiety = Math.min(maxSatiety, this.satiety + satietyStep);
            float healthStep = (float) (this.getMaxHealth() * stepPercent / 100.0D);
            this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + healthStep));
            return;
        }

        if (this.satiety >= satietyStep)
        {
            this.satiety -= satietyStep;
            return;
        }

        double shortfall = satietyStep - this.satiety;
        this.satiety = 0.0D;
        double shortfallPercent = shortfall * 100.0D / maxSatiety;
        this.drainHealth((float) (this.getMaxHealth()
                * (shortfallPercent / ModConstants.DEALER_SATIETY_PERCENT_PER_HEALTH_PERCENT) / 100.0D));
    }

    /**
     * Health taken by a drained bar, straight on the number: no hurt flash and no hurt sound for a drain that small,
     * but a bar that reaches zero still kills the dealer.
     */
    private void drainHealth(float amount)
    {
        if (!(amount > 0.0F))
        {
            return;
        }

        float left = this.getHealth() - amount;

        if (left > 0.0F)
        {
            this.setHealth(left);
        }
        else
        {
            this.setHealth(0.0F);
            this.die(this.damageSources().starve());
        }
    }

    /**
     * A meal runs as an animation: the dealer chews for one second and only then takes the dice out of its pocket, so
     * the bar moves exactly when the dice visibly disappears. After that it rests for one second before the next dice.
     * A fight or liquid comes first: the meal neither starts nor continues then.
     */
    private void tickEating()
    {
        if (this.eatCooldown > 0)
        {
            this.eatCooldown--;
        }

        if (this.eatTicks > 0)
        {
            this.eatTicks--;

            if (this.eatTicks == 0)
            {
                this.eatFromPocket();
                this.eatCooldown = ModConstants.DEALER_EAT_COOLDOWN_TICKS;
            }

            return;
        }

        if (this.eatCooldown <= 0 && this.pocketDice > 0 && this.isHungry() && !this.isInLiquid()
                && !this.isDefendingTerritory())
        {
            this.eatTicks = ModConstants.DEALER_EAT_ANIMATION_TICKS;
            this.getNavigation().stop();
        }
    }

    /**
     * True while the one second chewing animation runs, which the model turns into a hand to the mouth motion.
     */
    public boolean isEating()
    {
        return this.eatTicks > 0;
    }

    /**
     * A dealer with room picks up a pair of dice per interval and only returns for the next pair after the cooldown,
     * so a pile on the ground is walked through at a visible pace instead of vanishing in one tick. Only the gambler's
     * dice is picked up, the devil's dice is left where it lies.
     */
    private void tryPickup()
    {
        if (this.isInLiquid() || this.isDefendingTerritory())
        {
            return;
        }

        int room = this.pocketCapacity() - this.pocketDice;

        if (room <= 0)
        {
            return;
        }

        int wanted = Math.min(ModConstants.DEALER_PICKUP_PER_ACTION, room);
        List<ItemEntity> drops = this.level().getEntitiesOfClass(ItemEntity.class,
                this.getBoundingBox().inflate(ModConstants.DEALER_FEED_RADIUS));

        for (ItemEntity drop : drops)
        {
            ItemStack stack = drop.getItem();

            if (!stack.is(ModItems.GAMBLERS_DICE.get()))
            {
                continue;
            }

            while (wanted > 0 && !stack.isEmpty())
            {
                stack.shrink(1);
                this.pocketDice++;
                wanted--;
            }

            if (stack.isEmpty())
            {
                drop.discard();
            }

            if (wanted <= 0)
            {
                return;
            }
        }
    }

    /**
     * True when either bar fell to the share where a dealer starts eating again.
     */
    public boolean isHungry()
    {
        return this.getHealth() <= this.getMaxHealth() * ModConstants.DEALER_FEED_THRESHOLD
                || this.satiety <= this.maxSatiety() * ModConstants.DEALER_FEED_THRESHOLD;
    }

    /**
     * One dice leaves the pocket and refills half of each bar, so it takes two of them to come back from empty.
     */
    public void eatFromPocket()
    {
        if (this.pocketDice <= 0)
        {
            return;
        }

        this.pocketDice--;
        this.eatOneDice();
    }

    /**
     * The shared meal: half of each bar, and whatever the bars cannot take turns into experience. Eating two dice while
     * already full therefore turns into a whole point of experience, which is the reward for hosting a wager with a
     * pocket that has no room left.
     */
    private void eatOneDice()
    {
        float maxHealth = this.getMaxHealth();
        float restoreHealth = (float) (maxHealth * ModConstants.DEALER_DICE_RESTORE_SHARE);
        int maxSatiety = this.maxSatiety();
        int restoreSatiety = Math.max(1, (int) Math.round(maxSatiety * ModConstants.DEALER_DICE_RESTORE_SHARE));

        float healed = Math.min(restoreHealth, maxHealth - this.getHealth());
        this.setHealth(this.getHealth() + healed);
        double fed = Math.min(restoreSatiety, maxSatiety - this.satiety);
        this.satiety += fed;

        double overflowHealth = restoreHealth - healed;
        double overflowSatiety = restoreSatiety - fed;
        this.gainExperience(0.5D * (overflowHealth / restoreHealth + overflowSatiety / restoreSatiety));
        this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
    }

    private void gainExperience(double amount)
    {
        if (!(amount > 0.0D))
        {
            return;
        }

        this.experience += amount;
        this.applyProgress();
    }

    /**
     * Applies the experience to the two bars. A raise never heals, it only lifts the ceiling, so a dealer that grows
     * while hurt stays hurt until its next meal.
     */
    private void applyProgress()
    {
        double multiplier = ModConstants.DEALER_STAT_BASE_MULTIPLIER
                + this.experience * ModConstants.DEALER_STAT_PER_EXPERIENCE;
        float maxHealth = (float) (ModConstants.PLAYER_BASE_MAX_HEALTH * multiplier);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);

        if (this.getHealth() > maxHealth)
        {
            this.setHealth(maxHealth);
        }

        this.satiety = Math.min(this.satiety, this.maxSatiety());
    }

    public int pocketDice()
    {
        return this.pocketDice;
    }

    /**
     * Room in the pocket: the base 32 plus one slot per whole experience point.
     */
    public int pocketCapacity()
    {
        return ModConstants.DEALER_POCKET_BASE + (int) Math.floor(Math.max(0.0D, this.experience));
    }

    public double experience()
    {
        return this.experience;
    }

    public double satiety()
    {
        return this.satiety;
    }

    /**
     * The satiety ceiling follows the experience the same way the health ceiling does.
     */
    public int maxSatiety()
    {
        double multiplier = ModConstants.DEALER_STAT_BASE_MULTIPLIER
                + this.experience * ModConstants.DEALER_STAT_PER_EXPERIENCE;
        return (int) Math.round(ModConstants.PLAYER_BASE_MAX_SATIETY * multiplier);
    }

    public void setSatiety(double value)
    {
        this.satiety = Math.max(0.0D, Math.min(this.maxSatiety(), value));
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

    /**
     * The dice the creator paid goes into the dealer hand while the wager runs. It is not consumed by the draw, it is
     * the payment for the draw, so it stays a real dice the whole time.
     */
    public void holdWagerDice()
    {
        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GAMBLERS_DICE.get()));
    }

    /**
     * A settled wager hands the held dice over: into the pocket, or straight into the mouth when the pocket is full.
     */
    public void receiveWagerDice()
    {
        ItemStack held = this.getItemInHand(InteractionHand.MAIN_HAND);
        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        if (held.is(ModItems.GAMBLERS_DICE.get()))
        {
            this.absorbDice(Math.max(1, held.getCount()));
        }
    }

    /**
     * A cancelled wager gives the dice back to its owner through the caller, so the hand is simply emptied.
     */
    public void clearWagerDice()
    {
        if (this.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.GAMBLERS_DICE.get()))
        {
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    /**
     * Dice that arrive from a finished wager or a reloaded hand: into the pocket while it has room, otherwise eaten.
     */
    private void absorbDice(int count)
    {
        for (int i = 0; i < count; i++)
        {
            if (this.pocketDice < this.pocketCapacity())
            {
                this.pocketDice++;
            }
            else
            {
                this.eatOneDice();
            }
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
     * The recruit counter on the name tag: how many members the wager already has and how many seconds are left.
     * It never makes a sound of its own, because the panel and the dealer would otherwise chirp once per second for
     * the whole recruit. A business name stays over the head for everyone.
     */
    public void showRecruitText(int members, int seconds)
    {
        this.recruitMembers = members;
        this.recruitSeconds = seconds;
        this.recruitLines = true;
        this.setCustomName(Component.translatable(KEY_RECRUIT_TITLE));
        this.setCustomNameVisible(true);
        this.showNameToEveryone = true;
    }

    public void clearRecruitText()
    {
        this.recruitLines = false;
        this.setCustomName(Component.translatable(KEY_IDLE_NAME));
        this.setCustomNameVisible(true);
        this.showNameToEveryone = false;
    }

    /**
     * The recruit name is two centred lines instead of one, so the renderer asks for the two parts and draws them
     * itself while the vanilla name tag stays in charge of every other state.
     */
    public boolean showsRecruitLines()
    {
        return this.recruitLines;
    }

    public Component recruitTitle()
    {
        return Component.translatable(KEY_RECRUIT_TITLE);
    }

    public Component recruitCount()
    {
        return Component.translatable(KEY_RECRUIT_COUNT, Integer.valueOf(this.recruitMembers),
                Integer.valueOf(this.recruitSeconds));
    }

    /**
     * The idle name is private business: it is readable only for a player who stands close and looks at the dealer.
     * The hosting and recruiting names are set through the methods above and stay visible for everyone.
     */
    @Override
    public boolean shouldShowName()
    {
        if (this.showNameToEveryone || this.getCustomName() == null)
        {
            return super.shouldShowName();
        }

        Player viewer = this.level().getNearestPlayer(this, ModConstants.DEALER_NAME_REVEAL_RANGE);
        return viewer != null && viewer.isAlive() && this.isLookingAtMe(viewer);
    }

    /**
     * True when the viewer has the dealer inside the cone in front of it, so a name does not appear for a player who
     * happens to walk past with the dealer behind their back.
     */
    private boolean isLookingAtMe(Player viewer)
    {
        Vec3 toDealer = this.position().subtract(viewer.position()).normalize();
        Vec3 sight = viewer.getLookAngle().normalize();
        return toDealer.dot(sight) > ModConstants.DEALER_NAME_LOOK_DOT;
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
     * One settled wager costs a share of the satiety ceiling: twelve percent plus one more per member. What the bar
     * cannot pay is eaten back with the dice the wager itself paid out, and only a debt that survives that meal is paid
     * with health, two percent of satiety for one percent of health.
     */
    public void consumeForWager(int players)
    {
        double costPercent = ModConstants.DEALER_WAGER_COST_BASE_PERCENT
                + ModConstants.DEALER_WAGER_COST_PER_MEMBER_PERCENT * Math.max(0, players);
        double debt = this.paySatiety(this.maxSatiety() * costPercent / 100.0D);

        if (debt > 0.0D && this.pocketDice > 0)
        {
            this.eatFromPocket();
            debt = this.paySatiety(debt);
        }

        if (debt <= 0.0D)
        {
            return;
        }

        double debtPercent = debt * 100.0D / this.maxSatiety();
        float healthCost = (float) (this.getMaxHealth()
                * (debtPercent / ModConstants.DEALER_SATIETY_PERCENT_PER_HEALTH_PERCENT) / 100.0D);
        this.invulnerableTime = 0;
        this.hurt(this.damageSources().magic(), healthCost);
    }

    /**
     * Takes as much of the cost out of the bar as it can hold and hands back what is still owed.
     */
    private double paySatiety(double cost)
    {
        double paid = Math.min(this.satiety, cost);
        this.satiety -= paid;
        return cost - paid;
    }

    /**
     * An empty hand right click opens the panel or joins the running recruit. A filled hand keeps the vanilla
     * behaviour, so placing a block against the dealer is untouched, and a dealer that is busy with a rival ignores
     * everyone, because it has other business on its mind.
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

        if (this.isDefendingTerritory() || this.rival != null)
        {
            this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
            return InteractionResult.CONSUME;
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
     * A fallen dealer leaves its whole pocket behind, plus the dice it still holds for an open wager. An empty pocket
     * leaves nothing.
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(source, looting, recentlyHit);

        if (this.pocketDice > 0)
        {
            this.spawnAtLocation(new ItemStack(ModItems.GAMBLERS_DICE.get(), this.pocketDice));
            this.pocketDice = 0;
        }
    }

    /**
     * The dice of an open wager belongs to the creator, not to the corpse: the session ends with a cancel that hands it
     * back, so the hand is emptied before the vanilla equipment drop can roll for it.
     */
    @Override
    public void die(DamageSource source)
    {
        this.clearWagerDice();
        super.die(source);
    }

    /**
     * A dealer that really loses health fumbles dice out of its pockets, once per wound and never on top of the death
     * loot, which stays the only guaranteed handful. A hit that is shrugged off without losing a point, for example one
     * cancelled by resistance, frees nothing. Most wounds free nothing at all, so a fight over a dealer is not a dice
     * farm.
     * A player can only hurt an idle dealer: while it recruits, hosts or fights a rival every player hit is swallowed
     * without a sound, and an idle dealer takes at most one player hit per second.
     */
    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        boolean fromPlayer = source.getEntity() instanceof Player;

        /* A rival cannot land a hit on a dealer that is busy with a wager; it has to wait until the table is free. */
        if (this.isHosting() && source.getEntity() instanceof GamblersDealer)
        {
            return false;
        }

        if (fromPlayer)
        {
            if (this.isHosting() || this.isDefendingTerritory())
            {
                return false;
            }

            if (this.level().getGameTime() - this.lastPlayerHitTick
                    < ModConstants.DEALER_PLAYER_HIT_COOLDOWN_TICKS)
            {
                return false;
            }
        }

        float healthBefore = this.getHealth();
        boolean taken = super.hurt(source, amount);

        if (taken && this.getHealth() < healthBefore && !this.isDeadOrDying())
        {
            this.dropHurtDice();
        }

        if (taken && fromPlayer)
        {
            this.lastPlayerHitTick = this.level().getGameTime();
        }

        return taken;
    }

    private void dropHurtDice()
    {
        if (this.pocketDice <= 0)
        {
            return;
        }

        double roll = this.level().getRandom().nextDouble();
        int count = 0;

        if (roll < ModConstants.DEALER_HURT_DROP_CHANCE_THREE)
        {
            count = ModConstants.DEALER_HURT_DROP_THREE;
        }
        else if (roll < ModConstants.DEALER_HURT_DROP_CHANCE_THREE + ModConstants.DEALER_HURT_DROP_CHANCE_TWO)
        {
            count = ModConstants.DEALER_HURT_DROP_TWO;
        }
        else if (roll < ModConstants.DEALER_HURT_DROP_CHANCE_THREE + ModConstants.DEALER_HURT_DROP_CHANCE_TWO
                + ModConstants.DEALER_HURT_DROP_CHANCE_ONE)
        {
            count = ModConstants.DEALER_HURT_DROP_ONE;
        }

        if (count <= 0)
        {
            return;
        }

        long now = this.level().getGameTime();
        this.openHurtDropWindow(now);
        int remaining = ModConstants.DEALER_HURT_DROP_WINDOW_MAX - this.hurtDropWindowCount;
        /* The dice come out of the pocket, so an empty pocket frees nothing no matter what the roll says. */
        count = Math.min(count, Math.min(remaining, this.pocketDice));

        if (count <= 0)
        {
            return;
        }

        /* A roll that would cross the limit hands out what is left of the window instead of nothing. */
        this.pocketDice -= count;
        this.hurtDropWindowCount += count;
        this.spawnAtLocation(new ItemStack(ModItems.GAMBLERS_DICE.get(), count));
    }

    /*
     * The window runs for one minute from the first hit that frees dice and is only reopened afterwards, so the cap is
     * a real ceiling on how fast one dealer can be farmed rather than a per hit cooldown that a fast hitter can ignore.
     */
    private void openHurtDropWindow(long now)
    {
        if (this.hurtDropWindowStart < 0L
                || now - this.hurtDropWindowStart >= ModConstants.DEALER_HURT_DROP_WINDOW_TICKS)
        {
            this.hurtDropWindowStart = now;
            this.hurtDropWindowCount = 0;
        }
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
        tag.putDouble(TAG_SATIETY, this.satiety);
        tag.putInt(TAG_STAY, this.stayTicks);
        tag.putLong(TAG_HURT_DROP_START, this.hurtDropWindowStart);
        tag.putInt(TAG_HURT_DROP_COUNT, this.hurtDropWindowCount);
        tag.putInt(TAG_POCKET, this.pocketDice);
        tag.putDouble(TAG_EXPERIENCE, this.experience);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.satiety = tag.contains(TAG_SATIETY) ? tag.getDouble(TAG_SATIETY) : ModConstants.DEALER_MAX_SATIETY;
        this.stayTicks = tag.contains(TAG_STAY) ? tag.getInt(TAG_STAY)
                : ModConstants.DEALER_STAY_SECONDS * ModConstants.TICKS_PER_SECOND;
        this.hurtDropWindowStart = tag.contains(TAG_HURT_DROP_START) ? tag.getLong(TAG_HURT_DROP_START) : -1L;
        this.hurtDropWindowCount = tag.contains(TAG_HURT_DROP_COUNT) ? tag.getInt(TAG_HURT_DROP_COUNT) : 0;
        this.experience = tag.contains(TAG_EXPERIENCE) ? Math.max(0.0D, tag.getDouble(TAG_EXPERIENCE)) : 0.0D;
        this.pocketDice = tag.contains(TAG_POCKET) ? Math.max(0, tag.getInt(TAG_POCKET))
                : ModConstants.DEALER_POCKET_INITIAL_DICE;
        /*
         * The vanilla read clamps the saved health to the ceiling of a fresh dealer, so the raw value is kept and put
         * back after the experience lifted the ceiling again.
         */
        float savedHealth = tag.contains("Health") ? tag.getFloat("Health") : this.getHealth();
        this.applyProgress();
        this.setHealth(Math.max(0.0F, Math.min(savedHealth, this.getMaxHealth())));
        this.pocketDice = Math.min(this.pocketDice, this.pocketCapacity());
        /* A wager is not saved, so a dice left in the hand by a lost session goes back into the pocket or is eaten. */
        ItemStack held = this.getItemInHand(InteractionHand.MAIN_HAND);

        if (held.is(ModItems.GAMBLERS_DICE.get()))
        {
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.absorbDice(held.getCount());
        }

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
