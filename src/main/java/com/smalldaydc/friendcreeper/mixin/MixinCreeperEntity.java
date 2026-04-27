package com.smalldaydc.friendcreeper.mixin;

import com.smalldaydc.friendcreeper.FriendlyCreeperConfig;
import com.smalldaydc.friendcreeper.FriendlyCreeperMod;
import com.smalldaydc.friendcreeper.ITamedCreeper;
import com.smalldaydc.friendcreeper.goal.CreeperFollowOwnerGoal;
import com.smalldaydc.friendcreeper.goal.CreeperSitGoal;
import com.smalldaydc.friendcreeper.goal.CreeperSuppressTargetGoal;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.OcelotEntity;
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
    private static final TrackedData<Boolean> FRIENDCREEPER_TAMED =
            DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Unique
    private static final TrackedData<Boolean> FRIENDCREEPER_SITTING =
            DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // OPTIONAL_UUID removed in 1.21.11; store owner UUID as String instead
    @Unique
    private static final TrackedData<String> FRIENDCREEPER_OWNER =
            DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.STRING);

    @Unique private static final double CHASE_RANGE_SQ = 16.0 * 16.0;
    @Unique private @Nullable UUID friendcreeper$avengeTargetUUID = null;
    @Unique private int friendcreeper$tameAttempts = 0;
    @Unique private int friendcreeper$hurtSoundCooldown = 0;

    protected MixinCreeperEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    // ── ITamedCreeper ─────────────────────────────────────────────────────────

    @Override public boolean friendcreeper$isTamed() {
        return this.dataTracker.get(FRIENDCREEPER_TAMED);
    }

    @Override public void friendcreeper$setTamed(boolean tamed) {
        this.dataTracker.set(FRIENDCREEPER_TAMED, tamed);
    }

    @Override public boolean friendcreeper$isSitting() {
        return this.dataTracker.get(FRIENDCREEPER_SITTING);
    }

    @Override public void friendcreeper$toggleSit() {
        boolean nowSitting = !friendcreeper$isSitting();
        this.dataTracker.set(FRIENDCREEPER_SITTING, nowSitting);
        this.setPose(nowSitting ? EntityPose.CROUCHING : EntityPose.STANDING);
        if (!this.getEntityWorld().isClient()) {
            this.setTarget(null);
            this.getNavigation().stop();
            setFuseSpeed(-1);
        }
    }

    @Override public @Nullable UUID friendcreeper$getOwnerUUID() {
        String s = this.dataTracker.get(FRIENDCREEPER_OWNER);
        if (s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override public void friendcreeper$setOwnerUUID(@Nullable UUID uuid) {
        this.dataTracker.set(FRIENDCREEPER_OWNER, uuid == null ? "" : uuid.toString());
    }

    @Override public @Nullable UUID friendcreeper$getAvengeTargetUUID() {
        return friendcreeper$avengeTargetUUID;
    }

    @Override public void friendcreeper$setAvengeTargetUUID(@Nullable UUID uuid) {
        this.friendcreeper$avengeTargetUUID = uuid;
    }

    @Override public int friendcreeper$getTameAttempts() {
        return friendcreeper$tameAttempts;
    }

    @Override public void friendcreeper$setTameAttempts(int attempts) {
        this.friendcreeper$tameAttempts = attempts;
    }

    // ── DataTracker ───────────────────────────────────────────────────────────

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void friendcreeper$initDataTracker(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(FRIENDCREEPER_TAMED, false);
        builder.add(FRIENDCREEPER_SITTING, false);
        builder.add(FRIENDCREEPER_OWNER, "");
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void friendcreeper$initGoals(CallbackInfo ci) {
        CreeperEntity self = (CreeperEntity) (Object) this;
        this.goalSelector.add(1, new CreeperSitGoal(self));
        this.goalSelector.add(2, new CreeperFollowOwnerGoal(self));
        this.targetSelector.add(0, new CreeperSuppressTargetGoal(self));

        // Replace vanilla flee goals with conditional ones (respects afraidOfCats config)
        this.goalSelector.clear(goal -> goal instanceof FleeEntityGoal);
        this.goalSelector.add(3, new FleeEntityGoal<>(self, OcelotEntity.class, 6.0F, 1.0, 1.2) {
            @Override
            public boolean canStart() {
                if (friendcreeper$isTamed() && !FriendlyCreeperConfig.get().afraidOfCats) return false;
                return super.canStart();
            }
        });
        this.goalSelector.add(3, new FleeEntityGoal<>(self, CatEntity.class, 6.0F, 1.0, 1.2) {
            @Override
            public boolean canStart() {
                if (friendcreeper$isTamed() && !FriendlyCreeperConfig.get().afraidOfCats) return false;
                return super.canStart();
            }
        });
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Inject(method = "tick", at = @At("TAIL"))
    private void friendcreeper$onTick(CallbackInfo ci) {
        // Client-side: handle hurt sound
        if (this.getEntityWorld().isClient()) {
            if (!friendcreeper$isTamed()) return;
            if (friendcreeper$hurtSoundCooldown > 0) {
                friendcreeper$hurtSoundCooldown--;
            } else if (FriendlyCreeperConfig.get().hurtSound
                    && this.getHealth() / this.getMaxHealth() < 0.25f) {
                if (this.getRandom().nextInt(300) == 0) {
                    this.getEntityWorld().playSoundClient(
                            this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ENTITY_CREEPER_HURT, this.getSoundCategory(),
                            0.8f, 0.9f + this.getRandom().nextFloat() * 0.2f, false);
                    friendcreeper$hurtSoundCooldown = 160;
                }
            }
            return;
        }

        if (!friendcreeper$isTamed()) {
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
                && !friendcreeper$isSitting()
                && target == null
                && this.canSee(recentDamager)) {
            this.setTarget(recentDamager);
        }

        if (target != null && !target.isDead() && this.squaredDistanceTo(target) > CHASE_RANGE_SQ) {
            this.setTarget(null);
        }

        if ((target == null || target.isDead()) && getFuseSpeed() > 0) {
            setFuseSpeed(-1);
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void friendcreeper$writeNbt(WriteView view, CallbackInfo ci) {
        view.putBoolean(FriendlyCreeperMod.NBT_TAMED,    friendcreeper$isTamed());
        view.putBoolean(FriendlyCreeperMod.NBT_SITTING,  friendcreeper$isSitting());
        view.putInt(    FriendlyCreeperMod.NBT_ATTEMPTS, friendcreeper$tameAttempts);
        UUID ownerUUID = friendcreeper$getOwnerUUID();
        if (ownerUUID != null) {
            view.put(FriendlyCreeperMod.NBT_OWNER, Uuids.INT_STREAM_CODEC, ownerUUID);
        }
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void friendcreeper$readNbt(ReadView view, CallbackInfo ci) {
        this.dataTracker.set(FRIENDCREEPER_TAMED, view.getBoolean(FriendlyCreeperMod.NBT_TAMED, false));
        if (view.getBoolean(FriendlyCreeperMod.NBT_SITTING, false)) {
            this.dataTracker.set(FRIENDCREEPER_SITTING, true);
            this.setPose(EntityPose.CROUCHING);
        }
        friendcreeper$tameAttempts = view.getInt(FriendlyCreeperMod.NBT_ATTEMPTS, 0);
        Optional<UUID> ownerOpt = view.read(FriendlyCreeperMod.NBT_OWNER, Uuids.INT_STREAM_CODEC);
        ownerOpt.ifPresent(this::friendcreeper$setOwnerUUID);
    }
}
