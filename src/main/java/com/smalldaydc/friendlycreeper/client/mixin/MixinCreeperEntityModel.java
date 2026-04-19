package com.smalldaydc.friendlycreeper.client.mixin;

import com.smalldaydc.friendlycreeper.ITamedCreeper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(CreeperEntityModel.class)
public class MixinCreeperEntityModel {

    @Shadow private ModelPart head;

    @Unique private static final float DEF_HEAD_Y = 6f;
    @Unique private static final float DEF_LEG_Y  = 18f;

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void friendlycreeper$applySitPose(net.minecraft.entity.Entity entity,
                                               float limbAngle, float limbDistance,
                                               float animationProgress, float headYaw,
                                               float headPitch, CallbackInfo ci) {

        CreeperEntityModelAccessor acc = (CreeperEntityModelAccessor)(Object) this;
        ModelPart leftFront  = acc.friendlycreeper$getLeftFrontLeg();
        ModelPart rightFront = acc.friendlycreeper$getRightFrontLeg();
        ModelPart leftHind   = acc.friendlycreeper$getLeftHindLeg();
        ModelPart rightHind  = acc.friendlycreeper$getRightHindLeg();

        // Always reset pivotY to defaults (model instances are shared)
        head.pivotY       = DEF_HEAD_Y;
        leftFront.pivotY  = DEF_LEG_Y;
        rightFront.pivotY = DEF_LEG_Y;
        leftHind.pivotY   = DEF_LEG_Y;
        rightHind.pivotY  = DEF_LEG_Y;
        // NOTE: do NOT reset pitch here — vanilla setAngles already set walking animation

        if (!(entity instanceof CreeperEntity creeper)) return;
        if (!((ITamedCreeper)(Object) creeper).friendlycreeper$isSitting()) return;

        // Sitting pose: legs fold UP, override vanilla pitch
        head.pivotY       = DEF_HEAD_Y  + 2f;
        leftFront.pivotY  = DEF_LEG_Y   - 4f;
        rightFront.pivotY = DEF_LEG_Y   - 4f;
        leftHind.pivotY   = DEF_LEG_Y   - 4f;
        rightHind.pivotY  = DEF_LEG_Y   - 4f;
        leftFront.pitch   = -0.8f;
        rightFront.pitch  = -0.8f;
        leftHind.pitch    =  0.8f;
        rightHind.pitch   =  0.8f;
    }
}
