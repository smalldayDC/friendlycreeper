package com.smalldaydc.friendlycreeper.client.render;

import com.smalldaydc.friendlycreeper.client.IFriendlyCreeperRenderState;
import com.smalldaydc.friendlycreeper.client.mixin.CreeperEntityModelAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
import net.minecraft.client.render.entity.state.CreeperEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class CreeperPoppyFeature extends FeatureRenderer<CreeperEntityRenderState, CreeperEntityModel> {

    // Head cube is 8px tall; head top = -8/16 relative to head pivot in render space
    private static final float HEAD_TOP_OFFSET = -8.0f / 16.0f;

    public CreeperPoppyFeature(
            FeatureRendererContext<CreeperEntityRenderState, CreeperEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue,
                       int light, CreeperEntityRenderState state,
                       float limbAngle, float limbDistance) {

        IFriendlyCreeperRenderState fcState = (IFriendlyCreeperRenderState) state;
        if (!fcState.friendlycreeper$isTamed()) return;

        ItemRenderState poppyState = fcState.friendlycreeper$getPoppyRenderState();
        if (poppyState.isEmpty()) return;

        // Get the head model part to render in its local coordinate space
        CreeperEntityModelAccessor modelAcc = (CreeperEntityModelAccessor)(Object) getContextModel();
        ModelPart head = modelAcc.friendlycreeper$getHead();

        matrices.push();
        // Follow the head's pivot position and all rotations (yaw + pitch)
        head.applyTransform(matrices);
        // Translate to the top of the head in head's local space, slightly above surface
        matrices.translate(0.0, HEAD_TOP_OFFSET - 0.08, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f));
        matrices.scale(0.5f, 0.5f, 0.5f);

        poppyState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);

        matrices.pop();
    }
}
