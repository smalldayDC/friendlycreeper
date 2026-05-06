package com.smalldaydc.friendcreeper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class FriendCreeperMod implements ModInitializer {
    public static final String NBT_TAMED    = "FriendlyTamed";
    public static final String NBT_OWNER    = "FriendlyOwner";
    public static final String NBT_SITTING  = "FriendlySitting";
    public static final String NBT_ATTEMPTS = "FriendlyTameAttempts";
    public static final String NBT_HELD_FISH = "FriendlyHeldFish";

    /** Health ratio threshold shared across all low-health checks (goals, tick, renderer). */
    public static final float LOW_HEALTH_THRESHOLD = 0.25f;
    /** Bounding box reach for fish pickup and cat feeding interactions. */
    public static final double INTERACTION_REACH_XZ = 1.3;
    public static final double INTERACTION_REACH_Y = 0.5;
    /** Movement speed for pickup/feed goals. */
    public static final double INTERACTION_MOVE_SPEED = 1.0;
    /** Search range for finding owner's cats. */
    private static final double CAT_SEARCH_RANGE = 16.0;

    private static final double SEARCH_RADIUS = 64;
    private static final double SEARCH_HEIGHT = 32;
    private static final double REVENGE_RANGE_SQ = 16.0 * 16.0;

    @Override
    public void onInitialize() {
        FriendCreeperConfig.load();

        // Cancel damage from owner + self-defense against non-player attackers
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof CreeperEntity creeper)) return true;
            ITamedCreeper tc = (ITamedCreeper) creeper;
            if (!tc.friendcreeper$isTamed()) return true;
            LivingEntity attacker = source.getAttacker() instanceof LivingEntity l ? l : null;

            // Non-player attacker → allow damage + trigger self-defense
            if (!(attacker instanceof PlayerEntity player)) {
                if (attacker != null
                        && (creeper.getTarget() == null || !creeper.getTarget().isAlive())
                        && creeper.canSee(attacker)
                        && creeper.squaredDistanceTo(attacker) <= REVENGE_RANGE_SQ) {
                    dropHeldFish(creeper);
                    creeper.setTarget(attacker);
                }
                return true;
            }

            // Owner damage protection
            if (FriendCreeperConfig.get().allowOwnerDamage) return true;
            UUID ownerUUID = tc.friendcreeper$getOwnerUUID();
            return ownerUUID == null || !ownerUUID.equals(player.getUuid());
        });

        // Owner attacked → creeper targets attacker (only if creeper can see them)
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, absorbed) -> {
            if (!(entity instanceof PlayerEntity owner)) return;
            if (damage <= 0) return;
            if (!FriendCreeperConfig.get().revengeOwner) return;
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
                // Drop held fish before engaging
                dropHeldFish(c);
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
                    dropHeldFish(creeper);
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

    /**
     * Drop the creeper's held fish as an item entity on the ground.
     * Does nothing if the creeper is not holding a fish.
     */
    public static void dropHeldFish(CreeperEntity creeper) {
        ITamedCreeper tc = (ITamedCreeper) creeper;
        ItemStack fish = tc.friendcreeper$getHeldFish();
        if (fish.isEmpty()) return;
        ItemEntity drop = new ItemEntity(
                creeper.getEntityWorld(),
                creeper.getX(), creeper.getY() + 0.3, creeper.getZ(),
                fish.copy());
        creeper.getEntityWorld().spawnEntity(drop);
        tc.friendcreeper$setHeldFish(ItemStack.EMPTY);
    }

    /**
     * Find hurt cats belonging to the same owner within the given range.
     */
    private static List<CatEntity> findHurtOwnerCats(CreeperEntity creeper, double range) {
        UUID ownerUUID = ((ITamedCreeper) creeper).friendcreeper$getOwnerUUID();
        if (ownerUUID == null) return List.of();
        Box searchBox = creeper.getBoundingBox().expand(range);
        return creeper.getEntityWorld().getEntitiesByClass(
                CatEntity.class, searchBox,
                cat -> cat.isAlive()
                        && cat.isTamed()
                        && cat.getOwner() != null
                        && ownerUUID.equals(cat.getOwner().getUuid())
                        && cat.getHealth() < cat.getMaxHealth());
    }

    /**
     * Find the nearest reachable hurt cat belonging to the same owner.
     * Returns null if no reachable hurt cat is found.
     */
    @Nullable
    public static CatEntity findNearestReachableHurtOwnerCat(CreeperEntity creeper) {
        List<CatEntity> cats = findHurtOwnerCats(creeper, CAT_SEARCH_RANGE);
        cats.sort((a, b) -> Double.compare(
                creeper.squaredDistanceTo(a), creeper.squaredDistanceTo(b)));
        for (CatEntity cat : cats) {
            Path path = creeper.getNavigation().findPathTo(cat, 1);
            if (path != null && path.reachesTarget()) {
                return cat;
            }
        }
        return null;
    }
}
