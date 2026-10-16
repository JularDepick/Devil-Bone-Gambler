/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.model;

import com.devilbonegambler.mob.GamblersDealer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Dealer model: a self built humanoid rig carrying the casino uniform, the face features and the hair.
 * The geometry mirrors the vanilla humanoid proportions, so the entity hitbox of 0.6 by 1.95 blocks still fits,
 * and the model extends HumanoidModel so the standard walk, head turn and hit animations keep working.
 * The texture is 64x64 and every cuboid owns a non overlapping UV region, see design.md for the table.
 */
public class GamblersDealerModel extends HumanoidModel<GamblersDealer>
{
    /**
     * Arm swing amplitude while the dealer stands still, the forearms keep a forward rest angle and only drift.
     */
    private static final float IDLE_ARM_SWING = 0.72F;

    /**
     * Arm swing amplitude while the dealer walks, below the vanilla 1.0 so the forward rest angle survives.
     */
    private static final float WALK_ARM_SWING = 0.95F;

    /**
     * Forward tilt of both arms at rest, which reads as a dealer ready to reach across the table.
     */
    private static final float ARM_REST_TILT = 0.30F;

    /**
     * Vertical drift of the arms while idling, in radians.
     */
    private static final float IDLE_BREATH = 0.035F;

    /**
     * Sideways drift of the arms while idling, in radians.
     */
    private static final float IDLE_ARM_Z = 0.055F;

    /**
     * Head nod amplitude while idling, in radians.
     */
    private static final float IDLE_HEAD_PITCH = 0.055F;

    /**
     * Head sway amplitude while idling, in radians.
     */
    private static final float IDLE_HEAD_YAW = 0.075F;

    /**
     * Hit reaction amplitude, in radians, scaled by the remaining hurt time.
     */
    private static final float HURT_HEAD_SHAKE = 0.45F;

    public GamblersDealerModel(ModelPart root)
    {
        super(root);
    }

    /**
     * Builds the layer definition against a 64x64 texture space.
     * Every part keeps its vanilla humanoid name so the inherited animation can look the parts up by name.
     */
    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        /*
         * HumanoidModel looks the hat part up on the root while it is built, so the rig always carries it.
         * The dealer wears hair instead of a hat, so the part stays empty and the hair cube covers the same head.
         */
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.ZERO);

        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.0F, -6.0F, -5.0F, 2.0F, 2.0F, 1.0F),
                PartPose.ZERO);

        head.addOrReplaceChild("hair",
                CubeListBuilder.create().texOffs(23, 7)
                        .addBox(-4.5F, -8.6F, -4.5F, 9.0F, 3.0F, 9.0F),
                PartPose.ZERO);

        head.addOrReplaceChild("fringe",
                CubeListBuilder.create().texOffs(24, 0)
                        .addBox(-4.5F, -8.5F, -4.35F, 9.0F, 3.0F, 1.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.ZERO);

        /*
         * The limbs keep the vanilla humanoid pivots, because the inherited animation swings them around exactly those
         * points: an arm turns at the shoulder and a leg at the hip. The box sizes and the texture regions are this
         * model's own, and only the pivots follow the vanilla rig, so the dealer stands on the ground line y = 24.
         */
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(16, 36)
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(24, 19)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 13.0F, 4.0F),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(40, 19)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 13.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * The inherited animation already turns the head toward the look target and swings the limbs with the walk.
     * This override only reshapes the arm swing into the dealer stance and adds the idle drift and the hit shake.
     */
    @Override
    public void setupAnim(GamblersDealer entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch)
    {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float moving = Mth.clamp(entity.walkAnimation.speed(), 0.0F, 1.0F);
        float idle = 1.0F - moving;
        float phase = entity.walkAnimation.position() * 0.6662F;
        float swing = Mth.lerp(moving, IDLE_ARM_SWING, WALK_ARM_SWING);
        float breath = ARM_REST_TILT + IDLE_BREATH * Mth.sin(ageInTicks * 0.10F);

        this.rightArm.xRot = -Mth.cos(phase) * swing * limbSwingAmount * 0.5F - breath * idle;
        this.leftArm.xRot = Mth.cos(phase) * swing * limbSwingAmount * 0.5F
                - (ARM_REST_TILT + IDLE_BREATH * Mth.sin(ageInTicks * 0.10F + Mth.PI)) * idle;
        this.rightArm.zRot = -IDLE_ARM_Z * idle;
        this.leftArm.zRot = IDLE_ARM_Z * idle;

        this.head.xRot += IDLE_HEAD_PITCH * Mth.sin(ageInTicks * 0.07F) * idle;
        this.head.yRot += IDLE_HEAD_YAW * Mth.sin(ageInTicks * 0.05F) * idle;

        if (entity.hurtTime > 0.0F)
        {
            float hurt = entity.hurtTime / (float) entity.hurtDuration;
            this.head.zRot += HURT_HEAD_SHAKE * hurt * Mth.sin(entity.hurtTime * 1.7F);
            this.head.xRot -= HURT_HEAD_SHAKE * 0.5F * hurt;
        }
    }
}