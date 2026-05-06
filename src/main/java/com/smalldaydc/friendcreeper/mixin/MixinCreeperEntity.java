package com.smalldaydc.friendcreeper.mixin;

import com.smalldaydc.friendcreeper.FriendCreeperConfig;
import com.smalldaydc.friendcreeper.FriendCreeperMod;
import com.smalldaydc.friendcreeper.ITamedCreeper;
import com.smalldaydc.friendcreeper.goal.CreeperFeedCatGoal;
import com.smalldaydc.friendcreeper.goal.CreeperFollowOwnerGoal;
import com.smalldaydc.friendcreeper.goal.CreeperPickupFishGoal;
import com.smalldaydc.friendcreeper.goal.CreeperSitGoal;
import com.smalldaydc.friendcreeper.goal.CreeperSuppressTargetGoal;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

    @Unique
    private static final TrackedData<String> FRIENDCREEPER_OWNER =
            DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.STRING);

    @Unique
    private static final TrackedData<Boolean> FRIENDCREEPER_HAS_TARGET =
            DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Unique
    private static final TrackedData<Boolean> FRIENDCREEPER_IS_FLEEING =
            DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Unique
    private static final TrackedData<ItemStack> FRIENDCREEPER_HELD_FISH =
            DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);

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
        this.getNavigation().stop();
        setFuseSpeed(-1);
        if (nowSitting) {
            FriendCreeperMod.dropHeldFish((CreeperEntity) (Object) this);
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

    @Override public boolean friendcreeper$hasTarget() {
        return this.dataTracker.get(FRIENDCREEPER_HAS_TARGET);
    }

    @Override public boolean friendcreeper$isFleeing() {
        return this.dataTracker.get(FRIENDCREEPER_IS_FLEEING);
    }

    @Override public void friendcreeper$setFleeing(boolean fleeing) {
        this.dataTracker.set(FRIENDCREEPER_IS_FLEEING, fleeing);
    }

    @Override public ItemStack friendcreeper$getHeldFish() {
        return this.dataTracker.get(FRIENDCREEPER_HELD_FISH);
    }

    @Override public void friendcreeper$setHeldFish(ItemStack stack) {
        this.dataTracker.set(FRIENDCREEPER_HELD_FISH, stack);
    }

    // ── DataTracker ───────────────────────────────────────────────────────────

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void friendcreeper$initDataTracker(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(FRIENDCREEPER_TAMED, false);
        builder.add(FRIENDCREEPER_SITTING, false);
        builder.add(FRIENDCREEPER_OWNER, "");
        builder.add(FRIENDCREEPER_HAS_TARGET, false);
        builder.add(FRIENDCREEPER_IS_FLEEING, false);
        builder.add(FRIENDCREEPER_HELD_FISH, ItemStack.EMPTY);
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void friendcreeper$initGoals(CallbackInfo ci) {
        CreeperEntity self = (CreeperEntity) (Object) this;
        this.goalSelector.add(1, new CreeperSitGoal(self));
        this.goalSelector.add(2, new CreeperFeedCatGoal(self));
        this.goalSelector.add(3, new CreeperPickupFishGoal(self));
        this.goalSelector.add(4, new CreeperFollowOwnerGoal(self));
        this.targetSelector.add(0, new CreeperSuppressTargetGoal(self));

        // Replace vanilla ActiveTargetGoal with one that filters out gunpowder-holding players
        this.targetSelector.clear(goal -> goal instanceof ActiveTargetGoal);
        this.targetSelector.add(1, new ActiveTargetGoal<>(self, PlayerEntity.class, true,
                (target, world) -> !(target instanceof PlayerEntity p
                        && (p.getMainHandStack().isOf(Items.GUNPOWDER)
                            || p.getOffHandStack().isOf(Items.GUNPOWDER)))));

        // Replace vanilla flee goals with conditional ones (respects afraidOfCats config)
        this.goalSelector.clear(goal -> goal instanceof FleeEntityGoal);
        this.goalSelector.add(3, new FleeEntityGoal<>(self, OcelotEntity.class, 6.0F, 1.0, 1.2) {
            @Override
            public boolean canStart() {
                if (friendcreeper$isTamed() && !FriendCreeperConfig.get().afraidOfCats) return false;
                return super.canStart();
            }
            @Override
            public void start() {
                super.start();
                friendcreeper$setFleeing(true);
            }
            @Override
            public void stop() {
                super.stop();
                // Don't reset fleeing when sitting — tick check handles it
                if (!friendcreeper$isSitting()) friendcreeper$setFleeing(false);
            }
        });
        this.goalSelector.add(3, new FleeEntityGoal<>(self, CatEntity.class, 6.0F, 1.0, 1.2) {
            @Override
            public boolean canStart() {
                if (friendcreeper$isTamed() && !FriendCreeperConfig.get().afraidOfCats) return false;
                return super.canStart();
            }
            @Override
            public void start() {
                super.start();
                friendcreeper$setFleeing(true);
            }
            @Override
            public void stop() {
                super.stop();
                // Don't reset fleeing when sitting — tick check handles it
                if (!friendcreeper$isSitting()) friendcreeper$setFleeing(false);
            }
        });
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Inject(method = "tick", at = @At("HEAD"))
    private void friendcreeper$preTick(CallbackInfo ci) {
        // Force-stop fuse before CreeperEntity.tick() processes it
        if (friendcreeper$isTamed() && friendcreeper$isSitting() && getFuseSpeed() > 0) {
            setFuseSpeed(-1);
        }
        // Force-stop fuse when untamed target picks up gunpowder
        if (!friendcreeper$isTamed()
                && this.getTarget() instanceof PlayerEntity player
                && (player.getMainHandStack().isOf(Items.GUNPOWDER)
                    || player.getOffHandStack().isOf(Items.GUNPOWDER))
                && getFuseSpeed() > 0) {
            this.setTarget(null);
            setFuseSpeed(-1);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void friendcreeper$onTick(CallbackInfo ci) {
        // Client-side: handle hurt sound
        if (this.getEntityWorld().isClient()) {
            if (!friendcreeper$isTamed()) return;
            if (friendcreeper$hurtSoundCooldown > 0) {
                friendcreeper$hurtSoundCooldown--;
            } else if (FriendCreeperConfig.get().hurtSound
                    && this.getHealth() / this.getMaxHealth() < FriendCreeperMod.LOW_HEALTH_THRESHOLD) {
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
            // Clear target if current target picked up gunpowder
            if (this.getTarget() instanceof PlayerEntity player
                    && (player.getMainHandStack().isOf(Items.GUNPOWDER)
                        || player.getOffHandStack().isOf(Items.GUNPOWDER))) {
                this.setTarget(null);
            }

            // Fallback: stop fuse if target is gone
            if (getFuseSpeed() > 0 && (this.getTarget() == null || !this.getTarget().isAlive())) {
                setFuseSpeed(-1);
            }
            return;
        }

        CreeperEntity self = (CreeperEntity) (Object) this;
        LivingEntity target = this.getTarget();

        // Force-reset fleeing state when afraidOfCats is disabled
        if (friendcreeper$isFleeing() && !FriendCreeperConfig.get().afraidOfCats) {
            friendcreeper$setFleeing(false);
            this.getNavigation().stop();
        }

        if (target != null && target.isAlive() && this.squaredDistanceTo(target) > CHASE_RANGE_SQ) {
            this.setTarget(null);
        }

        if ((target == null || !target.isAlive()) && getFuseSpeed() > 0) {
            setFuseSpeed(-1);
        }

        // Natural regeneration: heal 1 HP every 200 ticks (~190 seconds from 1 HP to full)
        if (FriendCreeperConfig.get().naturalRegeneration && this.age % 200 == 0) {
            this.heal(1.0f);
        }

        // Drop held fish: low health / afraidOfCats / no reachable hurt owner cat nearby
        if (!friendcreeper$getHeldFish().isEmpty()) {
            boolean lowHealth = this.getHealth() / this.getMaxHealth() < FriendCreeperMod.LOW_HEALTH_THRESHOLD;
            boolean shouldDrop = lowHealth || FriendCreeperConfig.get().afraidOfCats;

            // Check for nearby reachable hurt owner cat every 20 ticks (1 second) to reduce overhead
            if (!shouldDrop && this.age % 20 == 0) {
                if (FriendCreeperMod.findNearestReachableHurtOwnerCat(self) == null) {
                    shouldDrop = true;
                }
            }

            if (shouldDrop) {
                FriendCreeperMod.dropHeldFish(self);
            }
        }

        // Sync hasTarget to client for texture switching
        boolean hasTarget = this.getTarget() != null && this.getTarget().isAlive();
        if (this.dataTracker.get(FRIENDCREEPER_HAS_TARGET) != hasTarget) {
            this.dataTracker.set(FRIENDCREEPER_HAS_TARGET, hasTarget);
        }

        // Reset fleeing state when cat leaves (flee goal can't run while sitting)
        if (friendcreeper$isSitting() && friendcreeper$isFleeing() && this.age % 20 == 0) {
            boolean catNearby = !self.getEntityWorld().getEntitiesByClass(
                    CatEntity.class, self.getBoundingBox().expand(6.0), cat -> cat.isAlive()).isEmpty()
                || !self.getEntityWorld().getEntitiesByClass(
                    OcelotEntity.class, self.getBoundingBox().expand(6.0), ocelot -> ocelot.isAlive()).isEmpty();
            if (!catNearby) {
                this.dataTracker.set(FRIENDCREEPER_IS_FLEEING, false);
            }
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void friendcreeper$writeNbt(WriteView view, CallbackInfo ci) {
        view.putBoolean(FriendCreeperMod.NBT_TAMED,    friendcreeper$isTamed());
        view.putBoolean(FriendCreeperMod.NBT_SITTING,  friendcreeper$isSitting());
        view.putInt(    FriendCreeperMod.NBT_ATTEMPTS, friendcreeper$tameAttempts);
        UUID ownerUUID = friendcreeper$getOwnerUUID();
        if (ownerUUID != null) {
            view.put(FriendCreeperMod.NBT_OWNER, Uuids.INT_STREAM_CODEC, ownerUUID);
        }
        // Save held fish: 0=none, 1=cod, 2=salmon
        ItemStack fish = friendcreeper$getHeldFish();
        if (!fish.isEmpty()) {
            view.putInt(FriendCreeperMod.NBT_HELD_FISH, fish.isOf(Items.COD) ? 1 : 2);
        }
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void friendcreeper$readNbt(ReadView view, CallbackInfo ci) {
        this.dataTracker.set(FRIENDCREEPER_TAMED, view.getBoolean(FriendCreeperMod.NBT_TAMED, false));
        if (view.getBoolean(FriendCreeperMod.NBT_SITTING, false)) {
            this.dataTracker.set(FRIENDCREEPER_SITTING, true);
            this.setPose(EntityPose.CROUCHING);
        }
        friendcreeper$tameAttempts = view.getInt(FriendCreeperMod.NBT_ATTEMPTS, 0);
        Optional<UUID> ownerOpt = view.read(FriendCreeperMod.NBT_OWNER, Uuids.INT_STREAM_CODEC);
        ownerOpt.ifPresent(this::friendcreeper$setOwnerUUID);
        // Load held fish
        int fishType = view.getInt(FriendCreeperMod.NBT_HELD_FISH, 0);
        if (fishType == 1) {
            this.dataTracker.set(FRIENDCREEPER_HELD_FISH, new ItemStack(Items.COD));
        } else if (fishType == 2) {
            this.dataTracker.set(FRIENDCREEPER_HELD_FISH, new ItemStack(Items.SALMON));
        }
    }
}