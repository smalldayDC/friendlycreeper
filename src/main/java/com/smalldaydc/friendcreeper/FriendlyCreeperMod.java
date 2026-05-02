package com.smalldaydc.friendcreeper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;

import java.util.UUID;

public class FriendlyCreeperMod implements ModInitializer {
    public static final String NBT_TAMED    = "FriendlyTamed";
    public static final String NBT_OWNER    = "FriendlyOwner";
    public static final String NBT_SITTING  = "FriendlySitting";
    public static final String NBT_ATTEMPTS = "FriendlyTameAttempts";
    public static final String NBT_HELD_FISH = "FriendlyHeldFish";

    private static final double SEARCH_RADIUS = 64;
    private static final double SEARCH_HEIGHT = 32;
    private static final double REVENGE_RANGE_SQ = 16.0 * 16.0;

    @Override
    public void onInitialize() {
        FriendlyCreeperConfig.load();

        // Cancel damage from owner (covers melee + projectiles)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof CreeperEntity creeper)) return true;
            ITamedCreeper tc = (ITamedCreeper) creeper;
            if (!tc.friendcreeper$isTamed()) return true;
            LivingEntity attacker = source.getAttacker() instanceof LivingEntity l ? l : null;
            if (!(attacker instanceof PlayerEntity player)) return true;
            if (FriendlyCreeperConfig.get().allowOwnerDamage) return true;
            UUID ownerUUID = tc.friendcreeper$getOwnerUUID();
            return ownerUUID == null || !ownerUUID.equals(player.getUuid());
        });

        // Owner attacked → creeper targets attacker (only if creeper can see them)
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, absorbed) -> {
            if (!(entity instanceof PlayerEntity owner)) return;
            if (damage <= 0) return;
            if (!FriendlyCreeperConfig.get().revengeOwner) return;
            LivingEntity attacker = source.getAttacker() instanceof LivingEntity l ? l : null;
            if (attacker == null || attacker == owner) return;
            PlayerEntity attackerPlayer = attacker instanceof PlayerEntity ap ? ap : null;
            if (attackerPlayer != null && (attackerPlayer.isCreative() || owner.isTeammate(attackerPlayer))) return;

            Box searchBox = Box.of(owner.getEntityPos(), SEARCH_RADIUS, SEARCH_HEIGHT, SEARCH_RADIUS);
            UUID attackerUUID = attackerPlayer != null ? attackerPlayer.getUuid() : null;
            owner.getEntityWorld().getEntitiesByClass(CreeperEntity.class, searchBox, c -> {
                ITamedCreeper tc = (ITamedCreeper) c;
                return tc.friendcreeper$isTamed()
                        && owner.getUuid().equals(tc.friendcreeper$getOwnerUUID())
                        && !tc.friendcreeper$isSitting();
            }).forEach(c -> {
                if (!c.canSee(attacker)) return;
                if (c.squaredDistanceTo(attacker) > REVENGE_RANGE_SQ) return;
                ITamedCreeper tc = (ITamedCreeper) c;
                tc.friendcreeper$setAvengeTargetUUID(attackerUUID);
                c.setTarget(attacker);
            });
        });

        // Clear avenge target when that player dies + drop held fish on creeper death
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            // Drop held fish when a tamed creeper dies
            if (entity instanceof CreeperEntity creeper) {
                ITamedCreeper tc = (ITamedCreeper) creeper;
                if (tc.friendcreeper$isTamed()) {
                    ItemStack fish = tc.friendcreeper$getHeldFish();
                    if (!fish.isEmpty()) {
                        ItemEntity itemEntity = new ItemEntity(
                                creeper.getEntityWorld(),
                                creeper.getX(), creeper.getY() + 0.5, creeper.getZ(),
                                fish.copy());
                        creeper.getEntityWorld().spawnEntity(itemEntity);
                        tc.friendcreeper$setHeldFish(ItemStack.EMPTY);
                    }
                }
            }

            // Clear avenge target when that player dies
            if (!(entity instanceof PlayerEntity dead)) return;
            Box searchBox = Box.of(entity.getEntityPos(), SEARCH_RADIUS, SEARCH_HEIGHT, SEARCH_RADIUS);
            entity.getEntityWorld().getEntitiesByClass(CreeperEntity.class, searchBox, c -> {
                ITamedCreeper tc = (ITamedCreeper) c;
                UUID av = tc.friendcreeper$getAvengeTargetUUID();
                return av != null && av.equals(dead.getUuid());
            }).forEach(c -> ((ITamedCreeper) c).friendcreeper$setAvengeTargetUUID(null));
        });
    }
}
