package com.smalldaydc.friendcreeper.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(CreeperEntityModel.class)
public interface CreeperEntityModelAccessor {
    @Accessor("head")          ModelPart friendcreeper$getHead();
    @Accessor("leftFrontLeg")  ModelPart friendcreeper$getLeftFrontLeg();
    @Accessor("rightFrontLeg") ModelPart friendcreeper$getRightFrontLeg();
    @Accessor("leftHindLeg")   ModelPart friendcreeper$getLeftHindLeg();
    @Accessor("rightHindLeg")  ModelPart friendcreeper$getRightHindLeg();
}
