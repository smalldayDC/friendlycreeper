package com.smalldaydc.friendlycreeper.mixin;

import com.smalldaydc.friendlycreeper.FriendlyCreeperConfig;
import com.smalldaydc.friendlycreeper.ITamedCreeper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public class MixinMobEntity {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void friendlycreeper$preventSnowGolemTargeting(LivingEntity target, CallbackInfo ci) {
        if (!((Object) this instanceof SnowGolemEntity)) return;
        if (FriendlyCreeperConfig.get().snowGolemAttack) return;
        if (!(target instanceof CreeperEntity creeper)) return;
        if (((ITamedCreeper)(Object) creeper).friendlycreeper$isTamed()) ci.cancel();
    }
}
