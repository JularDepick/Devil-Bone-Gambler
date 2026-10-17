/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.render;

import com.devilbonegambler.block.DevilsAltarBlock;
import com.devilbonegambler.block.DevilsAltarBlockEntity;
import com.devilbonegambler.client.model.GamblersDealerModel;
import com.devilbonegambler.mob.GamblersDealer;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Dice sight: holding the Devil's Dice marks the altars around the player and holding the Gambler's Dice marks the
 * dealers, both as a white silhouette of the real geometry drawn without a depth test, so the highlight is visible
 * through walls instead of being hidden by the blocks in front of it.
 * The altar spots come from the block entities of the loaded chunks, so the scan never walks every block of the area.
 * The dealer geometry is collected by {@link DiceSightLayer} while the dealer model is posed in the entity pass, and
 * both kinds of geometry share one immediate mode batch that is uploaded last.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DiceSightRenderer
{
    private static final List<BlockPos> ALTARS = new ArrayList<BlockPos>();
    private static final List<DealerSighting> DEALERS = new ArrayList<DealerSighting>();
    private static int scanCooldown;

    private DiceSightRenderer()
    {
    }

    /**
     * Rescans the altars while the Devil's Dice is held and forgets them as soon as it is put away.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.level == null || !holds(player, ModItems.DEVILS_DICE.get()))
        {
            ALTARS.clear();
            return;
        }

        if (scanCooldown > 0)
        {
            scanCooldown--;
            return;
        }

        scanCooldown = ModConstants.DICE_SIGHT_SCAN_INTERVAL_TICKS;
        scanAltars(minecraft.level, player);
    }

    /**
     * Called by the dealer render layer once the dealer model of this frame is posed and placed.
     * Only the dealers in reach of a held Gambler's Dice are collected, and only for the frame that draws them.
     */
    public static void captureDealer(GamblersDealer dealer, PoseStack pose, GamblersDealerModel model, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch)
    {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || !dealer.isAlive() || !holds(player, ModItems.GAMBLERS_DICE.get()))
        {
            return;
        }

        double radius = ModConstants.DICE_SIGHT_DEALER_RADIUS;

        if (dealer.distanceToSqr(player) > radius * radius)
        {
            return;
        }

        DEALERS.add(new DealerSighting(dealer, model, new Matrix4f(pose.last().pose()), limbSwing, limbSwingAmount,
                partialTick, ageInTicks, netHeadYaw, headPitch));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            return;
        }

        try
        {
            drawSight(event);
        }
        finally
        {
            DEALERS.clear();
        }
    }

    /**
     * Draws both sights as one white batch. Nothing else is drawn at this stage afterwards, and the batch runs
     * without a depth test, so no block in front of an altar or a dealer can cover the highlight.
     */
    private static void drawSight(RenderLevelStageEvent event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;

        if (player == null || level == null)
        {
            return;
        }

        boolean altarSight = holds(player, ModItems.DEVILS_DICE.get());
        boolean dealerSight = holds(player, ModItems.GAMBLERS_DICE.get());

        if (!altarSight && !dealerSight)
        {
            return;
        }

        float[] shaderColor = RenderSystem.getShaderColor().clone();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        WhiteSilhouette silhouette = WhiteSilhouette.begin(ModConstants.DICE_SIGHT_HIGHLIGHT_COLOR,
                ModConstants.DICE_SIGHT_HIGHLIGHT_ALPHA);

        try
        {
            if (altarSight)
            {
                drawAltars(silhouette, level, event.getCamera().getPosition());
            }

            if (dealerSight)
            {
                drawDealers(silhouette);
            }
        }
        finally
        {
            silhouette.finish();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
        }
    }

    /**
     * The altar model itself is the highlight, so the player sees the altar shape and not a box around it.
     */
    private static void drawAltars(WhiteSilhouette silhouette, ClientLevel level, Vec3 camera)
    {
        for (BlockPos pos : ALTARS)
        {
            BlockState state = level.getBlockState(pos);

            if (!(state.getBlock() instanceof DevilsAltarBlock))
            {
                continue;
            }

            PoseStack pose = new PoseStack();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            silhouette.block(Minecraft.getInstance().getBlockRenderer(), state, pose);
        }
    }

    /**
     * Every collected dealer is reposed into this frame before it is drawn, because one model instance is shared by
     * all dealers and it keeps only the pose of the last dealer the entity pass rendered.
     */
    private static void drawDealers(WhiteSilhouette silhouette)
    {
        for (DealerSighting sighting : DEALERS)
        {
            GamblersDealer dealer = sighting.dealer;

            if (!dealer.isAlive())
            {
                continue;
            }

            GamblersDealerModel model = sighting.model;
            model.attackTime = dealer.getAttackAnim(sighting.partialTick);
            model.riding = dealer.isPassenger();
            model.young = dealer.isBaby();
            model.setupAnim(dealer, sighting.limbSwing, sighting.limbSwingAmount, sighting.ageInTicks,
                    sighting.netHeadYaw, sighting.headPitch);

            PoseStack pose = new PoseStack();
            pose.mulPoseMatrix(sighting.pose);
            silhouette.model(pose, model);
        }
    }

    private static void scanAltars(ClientLevel level, LocalPlayer player)
    {
        ALTARS.clear();
        double radius = ModConstants.DICE_SIGHT_ALTAR_RADIUS;
        double limit = radius * radius;
        ChunkPos centre = player.chunkPosition();
        int chunks = (int) Math.ceil(radius / 16.0D);

        for (int dx = -chunks; dx <= chunks; dx++)
        {
            for (int dz = -chunks; dz <= chunks; dz++)
            {
                LevelChunk chunk = level.getChunkSource().getChunkNow(centre.x + dx, centre.z + dz);

                if (chunk == null)
                {
                    continue;
                }

                for (BlockEntity entity : chunk.getBlockEntities().values())
                {
                    if (entity instanceof DevilsAltarBlockEntity
                            && entity.getBlockPos().distToCenterSqr(player.getX(), player.getY(), player.getZ())
                                    <= limit)
                    {
                        ALTARS.add(entity.getBlockPos().immutable());
                    }
                }
            }
        }
    }

    private static boolean holds(Player player, Item item)
    {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    /**
     * One dealer collected for the current frame: the world matrix of its model and the animation parameters that
     * produced the pose, so the silhouette repeats exactly what the entity pass drew.
     */
    private static final class DealerSighting
    {
        private final GamblersDealer dealer;
        private final GamblersDealerModel model;
        private final Matrix4f pose;
        private final float limbSwing;
        private final float limbSwingAmount;
        private final float partialTick;
        private final float ageInTicks;
        private final float netHeadYaw;
        private final float headPitch;

        private DealerSighting(GamblersDealer dealer, GamblersDealerModel model, Matrix4f pose, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch)
        {
            this.dealer = dealer;
            this.model = model;
            this.pose = pose;
            this.limbSwing = limbSwing;
            this.limbSwingAmount = limbSwingAmount;
            this.partialTick = partialTick;
            this.ageInTicks = ageInTicks;
            this.netHeadYaw = netHeadYaw;
            this.headPitch = headPitch;
        }
    }
}
