/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.render;

import com.devilbonegambler.mob.GamblersDealer;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Dealer renderer: the vanilla villager model with the dealer uniform texture.
 * A later skin or model replacement only touches this class, the texture file and the model layer.
 */
public class GamblersDealerRenderer extends MobRenderer<GamblersDealer, VillagerModel<GamblersDealer>>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.tryParse(
            "devil_bone_gambler:textures/entity/gamblers_dealer.png");

    public GamblersDealerRenderer(EntityRendererProvider.Context context)
    {
        super(context, new VillagerModel<GamblersDealer>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(GamblersDealer entity)
    {
        return TEXTURE;
    }
}
