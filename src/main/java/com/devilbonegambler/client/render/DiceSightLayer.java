/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.render;

import com.devilbonegambler.client.model.GamblersDealerModel;
import com.devilbonegambler.mob.GamblersDealer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Dealer sight capture: an extra render layer that never draws anything itself.
 * It runs while the dealer model is already posed and placed, which is the only point where both the world
 * matrix and the animation parameters of a single dealer are available; the collected sighting is drawn later
 * as a white silhouette without a depth test, so the shape survives every block in between.
 */
public final class DiceSightLayer extends RenderLayer<GamblersDealer, GamblersDealerModel>
{
    public DiceSightLayer(RenderLayerParent<GamblersDealer, GamblersDealerModel> parent)
    {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int packedLight, GamblersDealer dealer,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch)
    {
        DiceSightRenderer.captureDealer(dealer, pose, getParentModel(), limbSwing, limbSwingAmount, partialTick,
                ageInTicks, netHeadYaw, headPitch);
    }
}
