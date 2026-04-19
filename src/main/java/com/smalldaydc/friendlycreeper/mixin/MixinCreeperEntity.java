package com.smalldaydc.friendlycreeper.mixin;

import com.smalldaydc.friendlycreeper.FriendlyCreeperConfig;
import com.smalldaydc.friendlycreeper.FriendlyCreeperMod;
import com.smalldaydc.friendlycreeper.ITamedCreeper;
import com.smalldaydc.friendlycreeper.goal.CreeperDefendOwnerGoal;
import com.smalldaydc.friendlycreeper.goal.CreeperFollowOwnerGoal;
import com.smalldaydc.friendlycreeper.goal.CreeperSuppressTargetGoal;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(CreeperEntity.class)
public abstract class MixinCreeperEntity extends HostileEntity implements ITamedCreeper {

    @Shadow public abstract void setFuseSpeed(int fuseSpeed);
    @Shadow public abstract int getFuseSpeed();

    @Unique
    private static final TrackedData<Boolean> FRIENDLYCREEPER_TAMED =
            DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Unique
    private static final TrackedData<Boolean> FRIENDLYCREEPER_SITTING =
            DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Unique private @Nullable UUID friendlycreeper$ownerUUID = null;
    @Unique private @Nullable UUID friendlycreeper$avengeTargetUUID = null;
    @Unique private int friendlycreeper$tameAttempts = 0;
    @Unique private int friendlycreeper$hurtSoundCooldown = 0;

    protected MixinCreeperEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    // ── ITamedCreeper ─────────────────────────────────────────────────────────

    @Override public boolean friendlycreeper$isTamed() {
        return this.dataTracker.get(FRIENDLYCREEPER_TAMED);
    }

    @Override public void friendlycreeper$setTamed(boolean tamed) {
        this.dataTracker.set(FRIENDLYCREEPER_TAMED, tamed);
    }

    @Override public boolean friendlycreeper$isSitting() {
        return this.dataTracker.get(FRIENDLYCREEPER_SITTING);
    }

    @Override public void friendlycreeper$toggleSit() {
        boolean nowSitting = !friendlycreeper$isSitting();
        this.dataTracker.set(FRIENDLYCREEPER_SITTING, nowSitting);
        this.setPose(nowSitting ? EntityPose.CROUCHING : EntityPose.STANDING);
        if (!this.getWorld().isClient()) {
            this.setTarget(null);
            this.getNavigation().stop();
            setFuseSpeed(-1);
        }
    }

    @Override public @Nullable UUID friendlycreeper$getOwnerUUID() {
        return friendlycreeper$ownerUUID;
    }

    @Override public void friendlycreeper$setOwnerUUID(@Nullable UUID uuid) {
        this.friendlycreeper$ownerUUID = uuid;
    }

    @Override public @Nullable UUID friendlycreeper$getAvengeTargetUUID() {
        return friendlycreeper$avengeTargetUUID;
    }

    @Override public void friendlycreeper$setAvengeTargetUUID(@Nullable UUID uuid) {
        this.friendlycreeper$avengeTargetUUID = uuid;
    }

    @Override public int friendlycreeper$getTameAttempts() {
        return friendlycreeper$tameAttempts;
    }

    @Override public void friendlycreeper$setTameAttempts(int attempts) {
        this.friendlycreeper$tameAttempts = attempts;
    }

    // ── DataTracker ───────────────────────────────────────────────────────────

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void friendlycreeper$initDataTracker(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(FRIENDLYCREEPER_TAMED, false);
        builder.add(FRIENDLYCREEPER_SITTING, false);
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void friendlycreeper$initGoals(CallbackInfo ci) {
        CreeperEntity self = (CreeperEntity) (Object) this;
        this.goalSelector.add(2, new CreeperFollowOwnerGoal(self));
        this.goalSelector.add(1, new CreeperDefendOwnerGoal(self));
        this.targetSelector.add(0, new CreeperSuppressTargetGoal(self));
    }

    // ── Tick: gunpowder fuse + sit lock + hurt sound + fuse reset ────────────

    @Inject(method = "tick", at = @At("TAIL"))
    private void friendlycreeper$onTick(CallbackInfo ci) {
        if (this.getWorld().isClient()) return;

        // Untamed: if nearby player holds gunpowder, stop ignition
        if (!friendlycreeper$isTamed()) {
            var nearest = this.getWorld().getClosestPlayer(this, 8.0);
            if (nearest != null
                    && (nearest.getMainHandStack().isOf(Items.GUNPOWDER)
                        || nearest.getOffHandStack().isOf(Items.GUNPOWDER))
                    && getFuseSpeed() > 0) {
                setFuseSpeed(-1);
            }
            return;
        }

        // Tamed below
        ITamedCreeper tc = this;
        LivingEntity target = this.getTarget();
        LivingEntity recentDamager = this.getAttacker();

        // Self-defense: if attacked by a non-player mob, target it back
        if (recentDamager != null
                && !(recentDamager instanceof PlayerEntity)
                && !tc.friendlycreeper$isSitting()
                && target == null) {
            this.setTarget(recentDamager);
        }

        // Range check: stop chasing if target is too far, but keep the grudge
        // Resume chasing when target comes back into range
        if (target != null && !target.isDead()) {
            double distSq = this.squaredDistanceTo(target);
            if (distSq > 16.0 * 16.0) {
                // Too far — stop moving but keep target alive
                this.getNavigation().stop();
            }
            // If within range, vanilla MeleeAttackGoal handles movement automatically
        }

        // Ghost-explode guard: if no valid target but fuse is counting, stop
        if ((target == null || target.isDead()) && getFuseSpeed() > 0) {
            setFuseSpeed(-1);
        }

        // Sitting: stop everything
        if (friendlycreeper$isSitting()) {
            this.getNavigation().stop();
            this.setVelocity(0, this.getVelocity().y, 0);
            if (getFuseSpeed() > 0) setFuseSpeed(-1);
        }

        // Low health hurt sound (throttled)
        if (friendlycreeper$hurtSoundCooldown > 0) {
            friendlycreeper$hurtSoundCooldown--;
        } else if (FriendlyCreeperConfig.get().hurtSound
                && this.getHealth() / this.getMaxHealth() < 0.25f) {
            if (this.getRandom().nextInt(300) == 0) {
                this.playSound(SoundEvents.ENTITY_CREEPER_HURT, 0.8f,
                        0.9f + this.getRandom().nextFloat() * 0.2f);
                friendlycreeper$hurtSoundCooldown = 160;
            }
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void friendlycreeper$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean(FriendlyCreeperMod.NBT_TAMED,    friendlycreeper$isTamed());
        nbt.putBoolean(FriendlyCreeperMod.NBT_SITTING,  friendlycreeper$isSitting());
        nbt.putInt(    FriendlyCreeperMod.NBT_ATTEMPTS, friendlycreeper$tameAttempts);
        if (friendlycreeper$ownerUUID != null) {
            nbt.put(FriendlyCreeperMod.NBT_OWNER, NbtHelper.fromUuid(friendlycreeper$ownerUUID));
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void friendlycreeper$readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.getBoolean(FriendlyCreeperMod.NBT_TAMED)) {
            this.dataTracker.set(FRIENDLYCREEPER_TAMED, true);
        }
        if (nbt.getBoolean(FriendlyCreeperMod.NBT_SITTING)) {
            this.dataTracker.set(FRIENDLYCREEPER_SITTING, true);
            this.setPose(EntityPose.CROUCHING);
        }
        friendlycreeper$tameAttempts = nbt.getInt(FriendlyCreeperMod.NBT_ATTEMPTS);
        if (nbt.contains(FriendlyCreeperMod.NBT_OWNER)) {
            try {
                friendlycreeper$ownerUUID = NbtHelper.toUuid(nbt.get(FriendlyCreeperMod.NBT_OWNER));
            } catch (Exception ignored) {}
        }
    }
}
