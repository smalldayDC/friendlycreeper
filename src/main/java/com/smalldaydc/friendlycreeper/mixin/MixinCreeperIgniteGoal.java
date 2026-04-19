package com.smalldaydc.friendlycreeper.mixin;

import com.smalldaydc.friendlycreeper.ITamedCreeper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.CreeperIgniteGoal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreeperIgniteGoal.class)
public class MixinCreeperIgniteGoal {

    @Shadow private CreeperEntity creeper;

    @Inject(method = "canStart", at = @At("RETURN"), cancellable = true)
    private void friendlycreeper$canStart(CallbackInfoReturnable<Boolean> cir) {
        ITamedCreeper tc = (ITamedCreeper)(Object) creeper;
        if (!tc.friendlycreeper$isTamed()) return;

        // If sitting — never ignite
        if (tc.friendlycreeper$isSitting()) {
            cir.setReturnValue(false);
            return;
        }

        LivingEntity target = creeper.getTarget();

        // No valid target → never ignite (prevents explosion after target dies)
        if (target == null || target.isDead()) {
            cir.setReturnValue(false);
            return;
        }

        // Non-player threat nearby → ignite
        if (!(target instanceof PlayerEntity)
                && creeper.squaredDistanceTo(target) < 9.0) {
            cir.setReturnValue(true);
        }
    }
}
