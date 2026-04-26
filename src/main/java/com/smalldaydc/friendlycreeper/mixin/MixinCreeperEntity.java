package com.smalldaydc.friendlycreeper.mixin;

import com.smalldaydc.friendlycreeper.FriendlyCreeperConfig;
import com.smalldaydc.friendlycreeper.FriendlyCreeperMod;
import com.smalldaydc.friendlycreeper.ITamedCreeper;
import com.smalldaydc.friendlycreeper.goal.CreeperFollowOwnerGoal;
import com.smalldaydc.friendlycreeper.goal.CreeperSitGoal;
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
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
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

    // OPTIONAL_UUID removed in 1.21.11; store owner UUID as String instead
    @Unique
    private static final TrackedData<String> FRIENDLYCREEPER_OWNER =
            DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.STRING);

    @Unique private static final double CHASE_RANGE_SQ = 16.0 * 16.0;
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
        if (!this.getEntityWorld().isClient()) {
            this.setTarget(null);
            this.getNavigation().stop();
            setFuseSpeed(-1);
        }
    }

    @Override public @Nullable UUID friendlycreeper$getOwnerUUID() {
        String s = this.dataTracker.get(FRIENDLYCREEPER_OWNER);
        if (s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override public void friendlycreeper$setOwnerUUID(@Nullable UUID uuid) {
        this.dataTracker.set(FRIENDLYCREEPER_OWNER, uuid == null ? "" : uuid.toString());
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
        builder.add(FRIENDLYCREEPER_OWNER, "");
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void friendlycreeper$initGoals(CallbackInfo ci) {
        CreeperEntity self = (CreeperEntity) (Object) this;
        this.goalSelector.add(1, new CreeperSitGoal(self));
        this.goalSelector.add(2, new CreeperFollowOwnerGoal(self));
        this.targetSelector.add(0, new CreeperSuppressTargetGoal(self));
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Inject(method = "tick", at = @At("TAIL"))
    private void friendlycreeper$onTick(CallbackInfo ci) {
        if (this.getEntityWorld().isClient()) return;

        if (!friendlycreeper$isTamed()) {
            LivingEntity target = this.getTarget();
            LivingEntity recentDamager = this.getAttacker();

            // Retaliate against non-player attackers (bypasses goal system)
            if (recentDamager != null && !(recentDamager instanceof PlayerEntity) && target == null) {
                this.setTarget(recentDamager);
            }

            // Stop fuse and clear target if targeting a gunpowder-holding player
            if (this.getTarget() instanceof PlayerEntity player
                    && (player.getMainHandStack().isOf(Items.GUNPOWDER)
                        || player.getOffHandStack().isOf(Items.GUNPOWDER))) {
                this.setTarget(null);
                setFuseSpeed(-1);
            }

            // Fallback: stop fuse if target is gone
            if (getFuseSpeed() > 0 && (this.getTarget() == null || this.getTarget().isDead())) {
                setFuseSpeed(-1);
            }
            return;
        }

        LivingEntity target = this.getTarget();
        LivingEntity recentDamager = this.getAttacker();

        if (recentDamager != null
                && !(recentDamager instanceof PlayerEntity)
                && !friendlycreeper$isSitting()
                && target == null) {
            this.setTarget(recentDamager);
        }

        if (target != null && !target.isDead() && this.squaredDistanceTo(target) > CHASE_RANGE_SQ) {
            this.getNavigation().stop();
        }

        if ((target == null || target.isDead()) && getFuseSpeed() > 0) {
            setFuseSpeed(-1);
        }

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

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void friendlycreeper$writeNbt(WriteView view, CallbackInfo ci) {
        view.putBoolean(FriendlyCreeperMod.NBT_TAMED,    friendlycreeper$isTamed());
        view.putBoolean(FriendlyCreeperMod.NBT_SITTING,  friendlycreeper$isSitting());
        view.putInt(    FriendlyCreeperMod.NBT_ATTEMPTS, friendlycreeper$tameAttempts);
        UUID ownerUUID = friendlycreeper$getOwnerUUID();
        if (ownerUUID != null) {
            view.put(FriendlyCreeperMod.NBT_OWNER, Uuids.INT_STREAM_CODEC, ownerUUID);
        }
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void friendlycreeper$readNbt(ReadView view, CallbackInfo ci) {
        this.dataTracker.set(FRIENDLYCREEPER_TAMED, view.getBoolean(FriendlyCreeperMod.NBT_TAMED, false));
        if (view.getBoolean(FriendlyCreeperMod.NBT_SITTING, false)) {
            this.dataTracker.set(FRIENDLYCREEPER_SITTING, true);
            this.setPose(EntityPose.CROUCHING);
        }
        friendlycreeper$tameAttempts = view.getInt(FriendlyCreeperMod.NBT_ATTEMPTS, 0);
        Optional<UUID> ownerOpt = view.read(FriendlyCreeperMod.NBT_OWNER, Uuids.INT_STREAM_CODEC);
        ownerOpt.ifPresent(this::friendlycreeper$setOwnerUUID);
    }
}
