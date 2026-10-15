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
 * Dealer renderer: the vanilla villager model and the vanilla villager texture.
 * A later skin and model replacement only touches this class and its texture path.
 */
public class GamblersDealerRenderer extends MobRenderer<GamblersDealer, VillagerModel<GamblersDealer>>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryParse("minecraft:textures/entity/villager/villager.png");

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
