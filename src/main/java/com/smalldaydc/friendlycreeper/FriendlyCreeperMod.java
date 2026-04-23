package com.smalldaydc.friendlycreeper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

import java.util.UUID;

public class FriendlyCreeperMod implements ModInitializer {
    public static final String NBT_TAMED    = "FriendlyTamed";
    public static final String NBT_OWNER    = "FriendlyOwner";
    public static final String NBT_SITTING  = "FriendlySitting";
    public static final String NBT_ATTEMPTS = "FriendlyTameAttempts";

    private static final double SEARCH_RADIUS = 64;
    private static final double SEARCH_HEIGHT = 32;

    @Override
    public void onInitialize() {
        FriendlyCreeperConfig.load();

        // Handle all creeper interactions BEFORE item use to prevent usable items from firing
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof CreeperEntity creeper)) return ActionResult.PASS;

            ITamedCreeper tc = (ITamedCreeper) creeper;
            ItemStack stack = player.getStackInHand(hand);

            if (tc.friendlycreeper$isTamed()) {
                // Only handle main hand to avoid double-firing
                if (hand != Hand.MAIN_HAND) return ActionResult.SUCCESS;
                if (!player.getUuid().equals(tc.friendlycreeper$getOwnerUUID())) return ActionResult.PASS;

                if (player.isSneaking()) {
                    if (!world.isClient()) tc.friendlycreeper$toggleSit();
                } else if (stack.isOf(Items.GUNPOWDER) && creeper.getHealth() < creeper.getMaxHealth()) {
                    if (!world.isClient()) {
                        if (!player.getAbilities().creativeMode) stack.decrement(1);
                        creeper.heal(4.0f);
                        if (world instanceof ServerWorld sw) {
                            sw.spawnParticles(ParticleTypes.HEART,
                                    creeper.getX(), creeper.getBodyY(0.5), creeper.getZ(),
                                    5, 0.4, 0.4, 0.4, 0.05);
                        }
                    }
                } else {
                    if (!world.isClient()) tc.friendlycreeper$toggleSit();
                }
                return ActionResult.SUCCESS;
            }

            // Untamed: gunpowder → tame attempt (both hands allowed)
            if (!stack.isOf(Items.GUNPOWDER)) return ActionResult.PASS;
            if (hand == Hand.OFF_HAND && player.getMainHandStack().isOf(Items.GUNPOWDER)) return ActionResult.PASS;

            if (!world.isClient()) {
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
                    if (world instanceof ServerWorld sw) {
                        sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                                creeper.getX(), creeper.getBodyY(0.5), creeper.getZ(),
                                20, 0.5, 0.5, 0.5, 0.1);
                    }
                } else {
                    tc.friendlycreeper$setTameAttempts(attempts);
                    if (world instanceof ServerWorld sw) {
                        sw.spawnParticles(ParticleTypes.SMOKE,
                                creeper.getX(), creeper.getBodyY(0.5), creeper.getZ(),
                                10, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            }
            return ActionResult.SUCCESS;
        });

        // Cancel damage from owner (covers melee + projectiles)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof CreeperEntity creeper)) return true;
            ITamedCreeper tc = (ITamedCreeper) creeper;
            if (!tc.friendlycreeper$isTamed()) return true;
            LivingEntity attacker = source.getAttacker() instanceof LivingEntity l ? l : null;
            if (!(attacker instanceof PlayerEntity player)) return true;
            if (FriendlyCreeperConfig.get().allowOwnerDamage) return true;
            UUID ownerUUID = tc.friendlycreeper$getOwnerUUID();
            return ownerUUID == null || !ownerUUID.equals(player.getUuid());
        });

        // Owner attacked → creeper targets attacker
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, absorbed) -> {
            if (!(entity instanceof PlayerEntity owner)) return;
            if (damage <= 0) return;
            if (!FriendlyCreeperConfig.get().revengeOwner) return;
            LivingEntity attacker = source.getAttacker() instanceof LivingEntity l ? l : null;
            if (attacker == null || attacker == owner) return;
            PlayerEntity attackerPlayer = attacker instanceof PlayerEntity ap ? ap : null;
            if (attackerPlayer != null && (attackerPlayer.isCreative() || owner.isTeammate(attackerPlayer))) return;

            Box searchBox = Box.of(owner.getPos(), SEARCH_RADIUS, SEARCH_HEIGHT, SEARCH_RADIUS);
            UUID attackerUUID = attackerPlayer != null ? attackerPlayer.getUuid() : null;
            owner.getWorld().getEntitiesByClass(CreeperEntity.class, searchBox, c -> {
                ITamedCreeper tc = (ITamedCreeper) c;
                return tc.friendlycreeper$isTamed()
                        && owner.getUuid().equals(tc.friendlycreeper$getOwnerUUID())
                        && !tc.friendlycreeper$isSitting();
            }).forEach(c -> {
                ITamedCreeper tc = (ITamedCreeper) c;
                tc.friendlycreeper$setAvengeTargetUUID(attackerUUID);
                c.setTarget(attacker);
            });
        });

        // Clear avenge target when that player dies
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof PlayerEntity dead)) return;
            Box searchBox = Box.of(entity.getPos(), SEARCH_RADIUS, SEARCH_HEIGHT, SEARCH_RADIUS);
            entity.getWorld().getEntitiesByClass(CreeperEntity.class, searchBox, c -> {
                ITamedCreeper tc = (ITamedCreeper) c;
                UUID av = tc.friendlycreeper$getAvengeTargetUUID();
                return av != null && av.equals(dead.getUuid());
            }).forEach(c -> ((ITamedCreeper) c).friendlycreeper$setAvengeTargetUUID(null));
        });
    }
}
