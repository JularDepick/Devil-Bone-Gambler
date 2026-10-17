/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/**
 * White silhouette painter: the geometry of a block model or of an entity model is collected as colour quads and
 * uploaded as one immediate mode batch, which the caller draws with the depth test switched off.
 * The batch ignores textures and light, so every collected shape becomes the same flat highlight colour.
 * Placement belongs to the pose stack, because the consumer receives already transformed positions.
 */
public final class WhiteSilhouette implements VertexConsumer, MultiBufferSource
{
    private final BufferBuilder builder;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    private float x;
    private float y;
    private float z;

    private WhiteSilhouette(int color, float alpha)
    {
        this.red = (color >> 16 & 0xFF) / 255.0F;
        this.green = (color >> 8 & 0xFF) / 255.0F;
        this.blue = (color & 0xFF) / 255.0F;
        this.alpha = alpha;
        this.builder = Tesselator.getInstance().getBuilder();
        this.builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    /**
     * Opens one batch; the caller owns the depth, blend and shader colour state around {@link #finish()}.
     */
    public static WhiteSilhouette begin(int color, float alpha)
    {
        return new WhiteSilhouette(color, alpha);
    }

    /**
     * Adds a block model as a silhouette, with the block position already applied to the given pose stack.
     */
    public void block(BlockRenderDispatcher dispatcher, BlockState state, PoseStack pose)
    {
        dispatcher.renderSingleBlock(state, pose, this, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, RenderType.solid());
    }

    /**
     * Adds an entity model as a silhouette, with the entity transform already applied to the given pose stack.
     */
    public void model(PoseStack pose, EntityModel<?> model)
    {
        model.renderToBuffer(pose, this, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F,
                1.0F);
    }

    /**
     * Uploads the batch through the colour shader; the caller must have switched the depth test off beforehand,
     * otherwise the shader state of a render type would re-enable it.
     */
    public void finish()
    {
        BufferBuilder.RenderedBuffer rendered = this.builder.endOrDiscardIfEmpty();

        if (rendered == null)
        {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(rendered);
    }

    @Override
    public VertexConsumer getBuffer(RenderType type)
    {
        return this;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z)
    {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v)
    {
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v)
    {
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v)
    {
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z)
    {
        return this;
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha)
    {
    }

    @Override
    public void unsetDefaultColor()
    {
    }

    @Override
    public void endVertex()
    {
        this.builder.vertex(this.x, this.y, this.z).color(this.red, this.green, this.blue, this.alpha).endVertex();
    }
}
