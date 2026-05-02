package com.smalldaydc.friendcreeper.client.render;

import com.smalldaydc.friendcreeper.client.IFriendlyCreeperRenderState;
import com.smalldaydc.friendcreeper.client.mixin.CreeperEntityModelAccessor;
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
public class CreeperFishFeature extends FeatureRenderer<CreeperEntityRenderState, CreeperEntityModel> {

    // Creeper head cube: 8×8×8 pixels, from (-4,-8,-4) to (4,0,4) relative to pivot.
    // Front face is at z = -4/16 = -0.25 in head-local space.
    // Position the fish so it sticks out naturally from the mouth.
    private static final float MOUTH_Y = -0.0625f;  // near the bottom of the face
    private static final float MOUTH_Z = -0.375f;   // 2px in front of the face surface

    public CreeperFishFeature(
            FeatureRendererContext<CreeperEntityRenderState, CreeperEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue,
                       int light, CreeperEntityRenderState state,
                       float limbAngle, float limbDistance) {

        IFriendlyCreeperRenderState fcState = (IFriendlyCreeperRenderState) state;
        if (!fcState.friendcreeper$isTamed()) return;

        ItemRenderState fishState = fcState.friendcreeper$getFishRenderState();
        if (fishState.isEmpty()) return;

        // Get the head model part to render in its local coordinate space
        CreeperEntityModelAccessor modelAcc = (CreeperEntityModelAccessor)(Object) getContextModel();
        ModelPart head = modelAcc.friendcreeper$getHead();

        matrices.push();
        // Follow the head's pivot position and all rotations (yaw + pitch)
        head.applyTransform(matrices);
        // Translate to the mouth area on the front of the head
        matrices.translate(0.0, MOUTH_Y, MOUTH_Z);
        // Rotate the flat item sprite from lying flat (facing Y+) to vertical (facing Z-)
        // so the fish texture faces outward from the mouth
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));
        // Flip vertically so the fish isn't upside down
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f));
        matrices.scale(0.4f, 0.4f, 0.4f);

        fishState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);

        matrices.pop();
    }
}
