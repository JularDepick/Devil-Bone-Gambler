/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.render;

import com.devilbonegambler.client.model.GamblersDealerModel;
import com.devilbonegambler.mob.GamblersDealer;
import com.devilbonegambler.registry.ModModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

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

}
