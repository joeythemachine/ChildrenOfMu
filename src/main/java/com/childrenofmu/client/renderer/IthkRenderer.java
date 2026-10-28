package com.childrenofmu.client.renderer;

import javax.annotation.Nullable;

import com.childrenofmu.ChildrenOfMu;
import com.childrenofmu.client.model.IthkModel;
import com.childrenofmu.entity.ithk.IthkEntity;
import com.childrenofmu.entity.ithk.IthkProfession;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class IthkRenderer extends MobRenderer<IthkEntity, IthkModel> {
    private static final ResourceLocation TEXTURE = ChildrenOfMu.id("textures/entity/ithk/ithk.png");
    private static final ResourceLocation ETHEREAL_TEXTURE = ChildrenOfMu.id("textures/entity/ithk/ithk_ethereal.png");

    /** Base hover height above the ground, in blocks. */
    private static final float HOVER = 0.25F;
    /** Height of the sine-wave bob, in blocks. */
    private static final float BOB_AMPLITUDE = 0.08F;
    /** Speed of the bob. Lower is slower (0.07 is about one cycle every 4.5 seconds). */
    private static final float BOB_SPEED = 0.07F;
    /** Transmuters float higher so the cone beneath them clears the ground. */
    private static final float TRANSMUTER_EXTRA_HOVER = 0.7F;

    public IthkRenderer(EntityRendererProvider.Context context) {
        super(context, new IthkModel(context.bakeLayer(IthkModel.LAYER)), 0.35F);
        this.addLayer(new IthkCostumeLayer(this, context.getModelSet()));
    }

    @Override
    public void render(IthkEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // Each Ithk is offset by its id so they don't all bob in unison.
        float t = entity.tickCount + partialTicks + entity.getId() * 13.0F;
        float hover = HOVER + (entity.getProfession() == IthkProfession.TRANSMUTER ? TRANSMUTER_EXTRA_HOVER : 0.0F);
        poseStack.translate(0.0F, hover + Mth.sin(t * BOB_SPEED) * BOB_AMPLITUDE, 0.0F);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(IthkEntity entity) {
        return TEXTURE;
    }

    @Nullable
    @Override
    protected RenderType getRenderType(IthkEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (entity.isEthereal()) {
            // Translucent with no back-face culling, so the wireframe's far edges show through.
            return RenderType.entityTranslucent(ETHEREAL_TEXTURE);
        }
        return super.getRenderType(entity, bodyVisible, translucent, glowing);
    }
}
