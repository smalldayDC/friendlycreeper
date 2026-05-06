package com.smalldaydc.friendcreeper.mixin;

import com.smalldaydc.friendcreeper.FriendCreeperConfig;
import com.smalldaydc.friendcreeper.ITamedCreeper;
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
    private void friendcreeper$preventTargeting(LivingEntity target, CallbackInfo ci) {
        // Prevent snow golems from targeting tamed creepers
        if ((Object) this instanceof SnowGolemEntity && !FriendCreeperConfig.get().snowGolemAttack) {
            if (target instanceof CreeperEntity creeper
                    && ((ITamedCreeper)(Object) creeper).friendcreeper$isTamed()) {
                ci.cancel();
                return;
            }
        }

        // Prevent untamed creepers from targeting gunpowder-holding players
        if ((Object) this instanceof CreeperEntity creeper) {
            ITamedCreeper tc = (ITamedCreeper)(Object) creeper;
            if (!tc.friendcreeper$isTamed() && target instanceof PlayerEntity player
                    && (player.getMainHandStack().isOf(Items.GUNPOWDER)
                        || player.getOffHandStack().isOf(Items.GUNPOWDER))) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void friendcreeper$onInteract(PlayerEntity player, Hand hand,
                                             CallbackInfoReturnable<ActionResult> cir) {
        if (!((Object) this instanceof CreeperEntity creeper)) return;

        ITamedCreeper tc = (ITamedCreeper)(Object) creeper;
        ItemStack stack = player.getStackInHand(hand);

        if (tc.friendcreeper$isTamed()) {
            // Only handle main hand for tamed interactions to avoid double-firing
            if (hand != Hand.MAIN_HAND) return;

            // Gunpowder when hurt → heal (any player, like vanilla wolves)
            if (stack.isOf(Items.GUNPOWDER) && creeper.getHealth() < creeper.getMaxHealth()) {
                if (!player.getEntityWorld().isClient()) {
                    if (!player.getAbilities().creativeMode) stack.decrement(1);
                    creeper.heal(4.0f);
                    if (creeper.getEntityWorld() instanceof ServerWorld sw) {
                        sw.spawnParticles(ParticleTypes.HEART,
                                creeper.getX(), creeper.getBodyY(0.5), creeper.getZ(),
                                5, 0.4, 0.4, 0.4, 0.05);
                    }
                }
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            // Non-owner cannot do other interactions (sit/toggle)
            if (!player.getUuid().equals(tc.friendcreeper$getOwnerUUID())) return;

            // Sneak+right-click → toggle sit (not while in vehicle)
            if (player.isSneaking()) {
                if (!creeper.hasVehicle() && !player.getEntityWorld().isClient()) tc.friendcreeper$toggleSit();
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            // Any other right-click → toggle sit (also prevents usable items from firing, not while in vehicle)
            if (!creeper.hasVehicle() && !player.getEntityWorld().isClient()) tc.friendcreeper$toggleSit();
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // Untamed: gunpowder → tame attempt (both hands allowed)
        if (!stack.isOf(Items.GUNPOWDER)) return;
        // If off-hand triggers but main hand also has gunpowder, skip to avoid double-firing
        if (hand == Hand.OFF_HAND && player.getMainHandStack().isOf(Items.GUNPOWDER)) return;

        if (!player.getEntityWorld().isClient()) {
            if (!player.getAbilities().creativeMode) stack.decrement(1);

            int attempts = tc.friendcreeper$getTameAttempts() + 1;
            boolean success = attempts >= 5 || creeper.getRandom().nextInt(3) == 0;

            if (success) {
                tc.friendcreeper$setTamed(true);
                tc.friendcreeper$setOwnerUUID(player.getUuid());
                tc.friendcreeper$setTameAttempts(0);
                creeper.setPersistent();
                if (creeper.getEntityWorld() instanceof ServerWorld sw) {
                    sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                            creeper.getX(), creeper.getBodyY(0.5), creeper.getZ(),
                            20, 0.5, 0.5, 0.5, 0.1);
                }
            } else {
                tc.friendcreeper$setTameAttempts(attempts);
                if (creeper.getEntityWorld() instanceof ServerWorld sw) {
                    sw.spawnParticles(ParticleTypes.SMOKE,
                            creeper.getX(), creeper.getBodyY(0.5), creeper.getZ(),
                            10, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}