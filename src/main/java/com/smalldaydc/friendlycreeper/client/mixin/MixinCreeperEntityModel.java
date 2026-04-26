package com.smalldaydc.friendlycreeper.client.mixin;

import com.smalldaydc.friendlycreeper.client.IFriendlyCreeperRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
import net.minecraft.client.render.entity.state.CreeperEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(CreeperEntityModel.class)
public class MixinCreeperEntityModel {

    @Unique private static final float DEF_HEAD_Y = 6f;
    @Unique private static final float DEF_BODY_Y = 6f;
    @Unique private static final float DEF_LEG_Y  = 18f;

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void friendlycreeper$applySitPose(CreeperEntityRenderState state, CallbackInfo ci) {

        CreeperEntityModelAccessor acc = (CreeperEntityModelAccessor)(Object) this;
        ModelPart head       = acc.friendlycreeper$getHead();
        ModelPart body       = ((net.minecraft.client.model.Model)(Object) this).getRootPart().getChild("body");
        ModelPart leftFront  = acc.friendlycreeper$getLeftFrontLeg();
        ModelPart rightFront = acc.friendlycreeper$getRightFrontLeg();
        ModelPart leftHind   = acc.friendlycreeper$getLeftHindLeg();
        ModelPart rightHind  = acc.friendlycreeper$getRightHindLeg();

        // Always reset originY to defaults (model instances are shared)
        head.originY       = DEF_HEAD_Y;
        body.originY       = DEF_BODY_Y;
        leftFront.originY  = DEF_LEG_Y;
        rightFront.originY = DEF_LEG_Y;
        leftHind.originY   = DEF_LEG_Y;
        rightHind.originY  = DEF_LEG_Y;
        // NOTE: do NOT reset pitch here — vanilla setAngles already set walking animation

        IFriendlyCreeperRenderState fcState = (IFriendlyCreeperRenderState) state;
        if (!fcState.friendlycreeper$isSitting()) return;

        // Sitting pose: head+body sink together, legs fold UP
        head.originY       = DEF_HEAD_Y + 1f;
        body.originY       = DEF_BODY_Y + 1f;
        leftFront.originY  = DEF_LEG_Y  - 4f;
        rightFront.originY = DEF_LEG_Y  - 4f;
        leftHind.originY   = DEF_LEG_Y  - 4f;
        rightHind.originY  = DEF_LEG_Y  - 4f;
        leftFront.pitch    = -0.8f;
        rightFront.pitch   = -0.8f;
        leftHind.pitch     =  0.8f;
        rightHind.pitch    =  0.8f;
    }
}
