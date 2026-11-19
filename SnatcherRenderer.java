package com.horrormod.client;

import com.horrormod.HorrorMod;
import com.horrormod.entity.SnatcherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class SnatcherRenderer extends EntityRenderer<SnatcherEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(HorrorMod.MODID, "textures/entity/snatcher.png");

    /** The baked model is ~0.91 units tall; 2.0 makes it ~1.8 blocks (matches the 1.8 hitbox). */
    private static final float SCALE = 2.0F;

    public SnatcherRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(SnatcherEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(SnatcherEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        SnatcherMesh mesh = SnatcherMesh.get();

        poseStack.pushPose();

        // The model faces +Z, which is Minecraft's "yaw 0" direction.
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));

        // fall over sideways when dying (same curve as vanilla)
        if (entity.deathTime > 0) {
            float f = ((float) entity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
            f = Mth.sqrt(f);
            if (f > 1.0F) f = 1.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(f * 90.0F));
        }

        poseStack.scale(SCALE, SCALE, SCALE);

        float animTicks = Mth.lerp(partialTick, entity.animTimeO, entity.animTime);
        float frame = animTicks / 20.0F * mesh.fps;

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        mesh.render(poseStack.last(), vc, frame, packedLight, overlay);

        poseStack.popPose();

        // draws the name tag if needed
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
