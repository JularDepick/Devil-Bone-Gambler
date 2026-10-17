/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.render;

import com.devilbonegambler.block.DevilsAltarBlockEntity;
import com.devilbonegambler.mob.GamblersDealer;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dice sight: holding the Devil's Dice marks the altars around the player and holding the Gambler's Dice marks the
 * dealers, both as an outline drawn without a depth test so the marks stay visible through walls.
 * The altar spots come from the block entities of the loaded chunks, so the scan never walks every block of the area.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DiceSightRenderer
{
    private static final List<BlockPos> ALTARS = new ArrayList<BlockPos>();
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

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            return;
        }

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

        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        if (altarSight)
        {
            for (BlockPos pos : ALTARS)
            {
                drawBox(pose, lines, new AABB(pos), ModConstants.DICE_SIGHT_ALTAR_COLOR);
            }
        }

        if (dealerSight)
        {
            AABB area = player.getBoundingBox().inflate(ModConstants.DICE_SIGHT_DEALER_RADIUS);

            for (GamblersDealer dealer : level.getEntitiesOfClass(GamblersDealer.class, area))
            {
                if (dealer.isAlive())
                {
                    drawBox(pose, lines, dealer.getBoundingBox(), ModConstants.DICE_SIGHT_DEALER_COLOR);
                }
            }
        }

        buffers.endBatch(RenderType.lines());
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        pose.popPose();
    }

    /**
     * Two shells a hair apart, because the core profile clamps the line width to one pixel.
     */
    private static void drawBox(PoseStack pose, VertexConsumer lines, AABB box, int color)
    {
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        LevelRenderer.renderLineBox(pose, lines, box.inflate(0.002D), red, green, blue, 1.0F);
        LevelRenderer.renderLineBox(pose, lines, box.inflate(0.012D), red, green, blue, 0.55F);
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
}
