/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.render;

import com.devilbonegambler.client.model.GamblersDealerModel;
import com.devilbonegambler.mob.GamblersDealer;
import com.devilbonegambler.registry.ModModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Dealer renderer: the mod owned dealer model with the dealer uniform texture.
 * The layer comes from the shared model layer registry, so the model is baked once and reused by every dealer.
 */
public class GamblersDealerRenderer extends MobRenderer<GamblersDealer, GamblersDealerModel>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.tryParse(
            "devil_bone_gambler:textures/entity/gamblers_dealer.png");

    public GamblersDealerRenderer(EntityRendererProvider.Context context)
    {
        super(context, new GamblersDealerModel(context.bakeLayer(ModModelLayers.GAMBLERS_DEALER)), 0.5F);
        this.addLayer(new DiceSightLayer(this));
        /* The dice of an open wager is a real item in the hand, so it is drawn like one. */
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(GamblersDealer entity)
    {
        return TEXTURE;
    }

    /**
     * The recruit name is two centred lines, which a vanilla name tag cannot draw: the single line tag has no line
     * break. The two parts are therefore drawn here in the same camera facing space as the vanilla tag, each line
     * centred on its own width, and every other state still goes through the inherited name tag.
     */
    @Override
    protected void renderNameTag(GamblersDealer dealer, Component name, PoseStack pose, MultiBufferSource buffer,
            int packedLight)
    {
        if (!dealer.showsRecruitLines())
        {
            super.renderNameTag(dealer, name, pose, buffer, packedLight);
            return;
        }

        if (dealer.position().distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())
                > 4096.0D)
        {
            return;
        }

        boolean throughWalls = !dealer.isDiscrete();
        pose.pushPose();
        pose.translate(0.0F, dealer.getBbHeight() / 2.0F + 0.5F, 0.0F);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix = pose.last().pose();
        float opacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int background = (int) (opacity * 255.0F) << 24;
        Font font = this.getFont();
        Component title = dealer.recruitTitle();
        Component count = dealer.recruitCount();
        float titleX = -font.width(title) / 2.0F;
        float countX = -font.width(count) / 2.0F;
        int gap = font.lineHeight + 1;

        Font.DisplayMode mode = throughWalls ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;
        font.drawInBatch(title, titleX, 0.0F, 553648127, false, matrix, buffer, mode, background, packedLight);
        font.drawInBatch(count, countX, (float) gap, 553648127, false, matrix, buffer, mode, background,
                packedLight);

        if (throughWalls)
        {
            font.drawInBatch(title, titleX, 0.0F, -1, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
            font.drawInBatch(count, countX, (float) gap, -1, false, matrix, buffer, Font.DisplayMode.NORMAL, 0,
                    packedLight);
        }

        pose.popPose();
    }
}
