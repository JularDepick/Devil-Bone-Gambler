/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.registry;

import com.devilbonegambler.DevilBoneGambler;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * Client only model layer keys.
 * The location sits next to the other registries so the client setup and the renderer agree on one key.
 */
public final class ModModelLayers
{
    public static final ModelLayerLocation GAMBLERS_DEALER = new ModelLayerLocation(
            ResourceLocation.tryParse(DevilBoneGambler.MODID + ":gamblers_dealer"), "main");

    private ModModelLayers()
    {
    }
}
