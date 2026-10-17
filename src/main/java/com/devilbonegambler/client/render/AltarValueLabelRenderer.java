/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.render;

import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.util.ModConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * The offering value of the governing altar, written over its head.
 * Only the altar that actually governs the player is labelled, and only while the player stands close and looks at it,
 * so the number is there when it matters and out of the way the rest of the time. The text is a billboard: it always
 * faces the viewer and keeps a readable size, because a number that turns edge on is a number nobody can read.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Bus.FORGE, value = Dist.CLIENT)
public final class AltarValueLabelRenderer
{
    private static final float TEXT_SCALE = 0.025F;
    private static final float BACKGROUND_PADDING = 2.0F;
    private static final int BACKGROUND_COLOR = (int) (0.6F * 255.0F) << 24 | 0x101010;
    private static final float LABEL_HEIGHT = (float) ModConstants.ALTAR_MODEL_HEIGHT / 16.0F + 0.35F;

    private AltarValueLabelRenderer()
    {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null)
        {
            return;
        }

        BlockPos pos = ClientActions.altarPosition();

        if (pos == null || !ClientActions.isInAltarRange())
        {
            return;
        }

        if (minecraft.player.distanceToSqr(Vec3.atCenterOf(pos))
                > ModConstants.ALTAR_LABEL_RANGE * ModConstants.ALTAR_LABEL_RANGE)
        {
            return;
        }

        if (!faces(minecraft.player, pos))
        {
            return;
        }

        drawLabel(event, pos, ClientActions.altarValue());
    }

    /**
     * True while the altar sits in front of the player, so a label never appears over an altar they are walking away
     * from.
     */
    private static boolean faces(net.minecraft.world.entity.player.Player player, BlockPos pos)
    {
        Vec3 centre = Vec3.atCenterOf(pos);
        Vec3 toAltar = centre.subtract(player.getEyePosition()).normalize();
        Vec3 sight = player.getLookAngle().normalize();
        return toAltar.dot(sight) > ModConstants.ALTAR_LABEL_LOOK_DOT;
    }

    /**
     * Draws the number as a billboard above the altar, with a soft plate behind it so it stays readable against snow,
     * sandstone or the night.
     */
    private static void drawLabel(RenderLevelStageEvent event, BlockPos pos, long value)
    {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        Component text = Component.translatable("gui." + ModConstants.MOD_ID + ".altar.value_label",
                Long.valueOf(value));
        float width = font.width(text);

        Camera camera = event.getCamera();
        Vec3 anchor = Vec3.atBottomCenterOf(pos).add(0.0D, LABEL_HEIGHT, 0.0D).subtract(camera.getPosition());

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(anchor.x, anchor.y, anchor.z);
        pose.mulPose(new Quaternionf().rotationY((float) -Mth.DEG_TO_RAD * camera.getYRot()));
        pose.mulPose(new Quaternionf().rotationX((float) Mth.DEG_TO_RAD * camera.getXRot()));
        pose.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        Matrix4f matrix = pose.last().pose();
        float half = width / 2.0F;
        float top = -font.lineHeight / 2.0F;

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer background = buffers.getBuffer(RenderType.textBackground());
        background.vertex(matrix, -half - BACKGROUND_PADDING, top - BACKGROUND_PADDING, 0.0F)
                .color(BACKGROUND_COLOR)
                .uv(0.0F, 0.0F)
                .endVertex();
        background.vertex(matrix, -half - BACKGROUND_PADDING, -top + BACKGROUND_PADDING, 0.0F)
                .color(BACKGROUND_COLOR)
                .uv(0.0F, 1.0F)
                .endVertex();
        background.vertex(matrix, half + BACKGROUND_PADDING, -top + BACKGROUND_PADDING, 0.0F)
                .color(BACKGROUND_COLOR)
                .uv(1.0F, 1.0F)
                .endVertex();
        background.vertex(matrix, half + BACKGROUND_PADDING, top - BACKGROUND_PADDING, 0.0F)
                .color(BACKGROUND_COLOR)
                .uv(1.0F, 0.0F)
                .endVertex();
        buffers.endBatch(RenderType.textBackground());

        font.drawInBatch(text, -half, top, 0xFFFFFF, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH,
                0x000000, 15728880);
        buffers.endBatch();

        pose.popPose();
    }
}
