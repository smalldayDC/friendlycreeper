package com.smalldaydc.friendlycreeper.client.render;

import com.smalldaydc.friendlycreeper.ITamedCreeper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class CreeperPoppyFeature extends FeatureRenderer<CreeperEntity, CreeperEntityModel<CreeperEntity>> {

    // Cached once — no need to create every frame
    private static final ItemStack POPPY_STACK = new ItemStack(Items.POPPY);

    // head.pivotY = 6px (standing), head cube height = 8px → head top = (6-8)/16 = -0.125f
    // sitting adds +2f to head.pivotY → (8-8)/16 = 0f
    private static final float HEAD_TOP_STANDING = (6f - 8f) / 16.0f;
    private static final float HEAD_TOP_SITTING  = (8f - 8f) / 16.0f;

    private final ItemRenderer itemRenderer;

    public CreeperPoppyFeature(
            FeatureRendererContext<CreeperEntity, CreeperEntityModel<CreeperEntity>> context,
            ItemRenderer itemRenderer) {
        super(context);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, CreeperEntity entity,
                       float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {

        ITamedCreeper tc = (ITamedCreeper)(Object) entity;
        if (!tc.friendlycreeper$isTamed()) return;

        float headTopY = tc.friendlycreeper$isSitting() ? HEAD_TOP_SITTING : HEAD_TOP_STANDING;

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) Math.toRadians(-headYaw)));
        matrices.translate(0.0, headTopY - 0.08, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f));
        matrices.scale(0.5f, 0.5f, 0.5f);

        itemRenderer.renderItem(POPPY_STACK, ModelTransformationMode.GROUND, false,
                matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV,
                itemRenderer.getModel(POPPY_STACK, entity.getWorld(), null, entity.getId()));

        matrices.pop();
    }
}
