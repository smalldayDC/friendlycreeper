package com.smalldaydc.friendlycreeper.mixin;

import com.smalldaydc.friendlycreeper.FriendlyCreeperConfig;
import com.smalldaydc.friendlycreeper.ITamedCreeper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public class MixinMobEntity {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void friendlycreeper$preventSnowGolemTargeting(LivingEntity target, CallbackInfo ci) {
        if (!((Object) this instanceof SnowGolemEntity)) return;
        if (FriendlyCreeperConfig.get().snowGolemAttack) return;
        if (!(target instanceof CreeperEntity creeper)) return;
        if (((ITamedCreeper)(Object) creeper).friendlycreeper$isTamed()) ci.cancel();
    }

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void friendlycreeper$onInteract(PlayerEntity player, Hand hand,
                                             CallbackInfoReturnable<ActionResult> cir) {
        if (!((Object) this instanceof CreeperEntity creeper)) return;

        ITamedCreeper tc = (ITamedCreeper)(Object) creeper;
        ItemStack stack = player.getStackInHand(hand);

        if (tc.friendlycreeper$isTamed()) {
            // Only handle main hand for tamed interactions to avoid double-firing
            if (hand != Hand.MAIN_HAND) return;
            if (!player.getUuid().equals(tc.friendlycreeper$getOwnerUUID())) return;

            // Sneak+right-click → toggle sit (not while in vehicle)
            if (player.isSneaking()) {
                if (!creeper.hasVehicle() && !player.getWorld().isClient()) tc.friendlycreeper$toggleSit();
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            // Gunpowder when hurt → heal
            if (stack.isOf(Items.GUNPOWDER) && creeper.getHealth() < creeper.getMaxHealth()) {
                if (!player.getWorld().isClient()) {
                    if (!player.getAbilities().creativeMode) stack.decrement(1);
                    creeper.heal(4.0f);
                    if (creeper.getWorld() instanceof ServerWorld sw) {
                        sw.spawnParticles(ParticleTypes.HEART,
                                creeper.getX(), creeper.getBodyY(0.5), creeper.getZ(),
                                5, 0.4, 0.4, 0.4, 0.05);
                    }
                }
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            // Any other right-click → toggle sit (also prevents usable items from firing, not while in vehicle)
            if (!creeper.hasVehicle() && !player.getWorld().isClient()) tc.friendlycreeper$toggleSit();
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // Untamed: gunpowder → tame attempt (both hands allowed)
        if (!stack.isOf(Items.GUNPOWDER)) return;
        // If off-hand triggers but main hand also has gunpowder, skip to avoid double-firing
        if (hand == Hand.OFF_HAND && player.getMainHandStack().isOf(Items.GUNPOWDER)) return;

        if (!player.getWorld().isClient()) {
            if (!player.getAbilities().creativeMode) stack.decrement(1);

            int attempts = tc.friendlycreeper$getTameAttempts() + 1;
            boolean success = attempts >= 5 || creeper.getRandom().nextInt(3) == 0;

            if (success) {
                tc.friendlycreeper$setTamed(true);
                tc.friendlycreeper$setOwnerUUID(player.getUuid());
                tc.friendlycreeper$setTameAttempts(0);
                creeper.setPersistent();
                if (creeper.getWorld() instanceof ServerWorld sw) {
                    sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                            creeper.getX(), creeper.getBodyY(0.5), creeper.getZ(),
                            20, 0.5, 0.5, 0.5, 0.1);
                }
            } else {
                tc.friendlycreeper$setTameAttempts(attempts);
                if (creeper.getWorld() instanceof ServerWorld sw) {
                    sw.spawnParticles(ParticleTypes.SMOKE,
                            creeper.getX(), creeper.getBodyY(0.5), creeper.getZ(),
                            10, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
