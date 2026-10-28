package com.childrenofmu.client.renderer;

import com.childrenofmu.ChildrenOfMu;
import com.childrenofmu.client.model.FolkModel;
import com.childrenofmu.entity.folk.FolkEntity;
import com.childrenofmu.entity.folk.FolkRace;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/** Draws any of the walking folk, shrunk to its race's size, with whatever it's holding. */
public class FolkRenderer extends MobRenderer<FolkEntity, FolkModel> {
    private final FolkRace race;

    public FolkRenderer(EntityRendererProvider.Context context, FolkRace race) {
        super(context, new FolkModel(context.bakeLayer(FolkModel.layer(race)), race), race.width() * 0.6F);
        this.race = race;
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(FolkEntity entity, PoseStack poseStack, float partialTick) {
        float s = this.race.modelScale() * com.childrenofmu.entity.folk.FolkRace.variantScale(entity.getVariantName());
        poseStack.scale(s, s, s);
    }

    /** A bat asleep in its roost hangs upside down from its post. */
    @Override
    protected void setupRotations(FolkEntity entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick,
                                  float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        if (this.race == FolkRace.BAT && entity.isResting()) {
            poseStack.translate(0.0F, entity.getBbHeight(), 0.0F);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(FolkEntity entity) {
        return ChildrenOfMu.id("textures/entity/folk/" + this.race.id() + "_" + entity.getVariantName() + ".png");
    }
}
