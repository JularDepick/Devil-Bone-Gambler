/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.render;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.client.ClientActions;
import com.devilbonegambler.util.ModConstants;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.io.IOException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Red tint over the altar range, implemented as a screen space post pass.
 * The pass runs after the world is drawn and before the interface, which is the only window where a custom
 * post chain can run without a mixin. The sphere is tested in view space, so only the depth buffer is needed.
 * The green and blue channels are attenuated instead of raising red, because the colour attachment is already
 * clamped to the unit interval before the pass runs.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AltarTintHandler
{
    private static final String POST_CHAIN = ModConstants.MOD_ID + ":post/altar_tint";
    private static final String PASS_NAME = ModConstants.MOD_ID + ":altar_tint";
    private static final String BLIT_PASS = "minecraft:blit";
    private static final String DEPTH_SAMPLER = "DepthSampler";

    private static final float RED_GAIN = 0.8F;
    private static final float LUMA_GAIN = 0.8F;
    private static final float EDGE_FADE = 1.0F;

    private static PostChain chain;
    private static PostPass pass;
    private static int chainWidth = -1;
    private static int chainHeight = -1;
    private static boolean enabled = true;
    private static boolean failed = false;

    private AltarTintHandler()
    {
    }

    public static boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Client side downgrade switch, kept here so a later config screen can flip it.
     */
    public static void setEnabled(boolean value)
    {
        enabled = value;
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !enabled || failed)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null || !ClientActions.hasAltar())
        {
            return;
        }

        BlockPos pos = ClientActions.altarPosition();

        if (pos == null)
        {
            return;
        }

        buildChain(minecraft);

        if (chain == null || pass == null)
        {
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();

        if (main.width != chainWidth || main.height != chainHeight)
        {
            chain.resize(main.width, main.height);
            chainWidth = main.width;
            chainHeight = main.height;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        Vector3f relative = new Vector3f((float) (pos.getX() + 0.5D - cameraPos.x),
                (float) (pos.getY() + 0.5D - cameraPos.y), (float) (pos.getZ() + 0.5D - cameraPos.z));
        relative.rotate(camera.rotation().conjugate(new Quaternionf()));

        EffectInstance effect = pass.getEffect();
        effect.safeGetUniform("InvProjMat").set(new Matrix4f(event.getProjectionMatrix()).invert());
        effect.safeGetUniform("SphereCenterView").set(relative.x, relative.y, relative.z);
        effect.safeGetUniform("Radius").set((float) ClientActions.altarRadius());
        effect.safeGetUniform("RedGain").set(RED_GAIN);
        effect.safeGetUniform("LumaGain").set(LUMA_GAIN);

        chain.process(event.getPartialTick());
    }

    /**
     * The chain is built once, on the client thread, on the first frame that needs it.
     */
    private static void buildChain(Minecraft minecraft)
    {
        if (chain != null || failed)
        {
            return;
        }

        ResourceLocation location = ResourceLocation.tryParse(POST_CHAIN);

        if (location == null)
        {
            failed = true;
            return;
        }

        try
        {
            RenderTarget main = minecraft.getMainRenderTarget();
            PostChain created = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), main,
                    location);
            RenderTarget swap = created.getTempTarget("swap");
            PostPass custom = created.addPass(PASS_NAME, main, swap);
            custom.addAuxAsset(DEPTH_SAMPLER, main::getDepthTextureId, main.width, main.height);
            created.addPass(BLIT_PASS, swap, main);

            /*
             * addPass does not set the ortho matrix while resize does, so a resize right after the passes
             * is required or the first frame throws.
             */
            created.resize(main.width, main.height);

            chain = created;
            pass = custom;
            chainWidth = main.width;
            chainHeight = main.height;
        }
        catch (IOException | JsonSyntaxException exception)
        {
            DevilBoneGambler.LOGGER.error("Failed to build the altar tint post chain, the tint is disabled",
                    exception);
            failed = true;
            chain = null;
            pass = null;
        }
    }
}
