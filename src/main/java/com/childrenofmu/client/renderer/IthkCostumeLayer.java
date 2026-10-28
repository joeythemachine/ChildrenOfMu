package com.childrenofmu.client.renderer;

import com.childrenofmu.client.model.IthkCostumeModel;
import com.childrenofmu.client.model.IthkModel;
import com.childrenofmu.entity.ithk.IthkEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Draws each Ithk's profession costume over the eye. */
public class IthkCostumeLayer extends RenderLayer<IthkEntity, IthkModel> {
    private final IthkCostumeModel costumes;

    public IthkCostumeLayer(RenderLayerParent<IthkEntity, IthkModel> parent, EntityModelSet models) {
        super(parent);
        this.costumes = new IthkCostumeModel(models.bakeLayer(IthkCostumeModel.LAYER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, IthkEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        this.costumes.render(entity.getProfession(), poseStack, buffers, packedLight, overlay,
                ageInTicks, netHeadYaw, entity.isEthereal(), entity.getId());
    }
}
