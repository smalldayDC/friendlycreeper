package com.smalldaydc.friendlycreeper.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(CreeperEntityModel.class)
public interface CreeperEntityModelAccessor {
    @Accessor("head")          ModelPart friendlycreeper$getHead();
    @Accessor("leftFrontLeg")  ModelPart friendlycreeper$getLeftFrontLeg();
    @Accessor("rightFrontLeg") ModelPart friendlycreeper$getRightFrontLeg();
    @Accessor("leftHindLeg")   ModelPart friendlycreeper$getLeftHindLeg();
    @Accessor("rightHindLeg")  ModelPart friendlycreeper$getRightHindLeg();
}
