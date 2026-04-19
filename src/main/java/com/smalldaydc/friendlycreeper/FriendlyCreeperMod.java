package com.smalldaydc.friendlycreeper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.util.UUID;

public class FriendlyCreeperMod implements ModInitializer {
    public static final String MOD_ID = "friendlycreeper";
    public static final String NBT_TAMED    = "FriendlyTamed";
    public static final String NBT_OWNER    = "FriendlyOwner";
    public static final String NBT_SITTING  = "FriendlySitting";
    public static final String NBT_ATTEMPTS = "FriendlyTameAttempts";

    @Override
    public void onInitialize() {
        FriendlyCreeperConfig.load();
        // Cancel damage from owner (covers melee + projectiles)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof CreeperEntity creeper)) return true;
            ITamedCreeper tc = (ITamedCreeper) creeper;
            if (!tc.friendlycreeper$isTamed()) return true;
            LivingEntity attacker = source.getAttacker() instanceof LivingEntity l ? l : null;
            if (!(attacker instanceof PlayerEntity player)) return true;
            // Check config: if allowOwnerDamage is true, owner can hurt their creeper
            if (FriendlyCreeperConfig.get().allowOwnerDamage) return true;
            UUID ownerUUID = tc.friendlycreeper$getOwnerUUID();
            return ownerUUID == null || !ownerUUID.equals(player.getUuid());
        });

        // Owner attacked → creeper targets attacker
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, absorbed) -> {
            if (!(entity instanceof PlayerEntity owner)) return;
            if (damage <= 0) return;
            LivingEntity attacker = source.getAttacker() instanceof LivingEntity l ? l : null;
            if (attacker == null || attacker == owner) return;
            if (attacker instanceof PlayerEntity ap && ap.isCreative()) return;
            if (attacker instanceof PlayerEntity ap && owner.isTeammate(ap)) return;

            Box searchBox = Box.of(owner.getPos(), 64, 32, 64);
            owner.getWorld().getEntitiesByClass(CreeperEntity.class, searchBox, c -> {
                ITamedCreeper tc = (ITamedCreeper) c;
                return tc.friendlycreeper$isTamed()
                        && owner.getUuid().equals(tc.friendlycreeper$getOwnerUUID())
                        && !tc.friendlycreeper$isSitting();
            }).forEach(c -> {
                ITamedCreeper tc = (ITamedCreeper) c;
                tc.friendlycreeper$setAvengeTargetUUID(
                        attacker instanceof PlayerEntity ap ? ap.getUuid() : null);
                c.setTarget(attacker);
            });
        });

        // Clear avenge target when that player dies
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof PlayerEntity dead)) return;
            entity.getWorld().getEntitiesByClass(CreeperEntity.class,
                    Box.of(entity.getPos(), 64, 32, 64), c -> {
                ITamedCreeper tc = (ITamedCreeper) c;
                UUID av = tc.friendlycreeper$getAvengeTargetUUID();
                return av != null && av.equals(dead.getUuid());
            }).forEach(c -> ((ITamedCreeper) c).friendlycreeper$setAvengeTargetUUID(null));
        });
    }
}
