package com.smalldaydc.friendlycreeper.mixin;

import com.smalldaydc.friendlycreeper.ITamedCreeper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.CreeperEntity;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void friendlycreeper$onInteract(Entity entity, Hand hand,
                                             CallbackInfoReturnable<ActionResult> cir) {
        if (!(entity instanceof CreeperEntity creeper)) return;

        PlayerEntity player = (PlayerEntity)(Object) this;
        ITamedCreeper tc = (ITamedCreeper)(Object) creeper;
        ItemStack stack = player.getStackInHand(hand);

        if (tc.friendlycreeper$isTamed()) {
            // Only handle main hand to avoid double-firing
            if (hand != Hand.MAIN_HAND) return;
            if (!player.getUuid().equals(tc.friendlycreeper$getOwnerUUID())) return;

            // Sneak+right-click → toggle sit
            if (player.isSneaking()) {
                if (!player.getWorld().isClient()) tc.friendlycreeper$toggleSit();
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

            // Any other right-click → toggle sit, prevents usable items from firing
            if (!player.getWorld().isClient()) tc.friendlycreeper$toggleSit();
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
                creeper.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.POPPY));
                creeper.setEquipmentDropChance(EquipmentSlot.HEAD, 0.0f);
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
