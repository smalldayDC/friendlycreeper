package com.smalldaydc.friendcreeper.mixin;

import com.smalldaydc.friendcreeper.FriendlyCreeperConfig;
import com.smalldaydc.friendcreeper.FriendlyCreeperMod;
import com.smalldaydc.friendcreeper.ITamedCreeper;
import com.smalldaydc.friendcreeper.goal.CreeperFeedCatGoal;
import com.smalldaydc.friendcreeper.goal.CreeperFollowOwnerGoal;
import com.smalldaydc.friendcreeper.goal.CreeperPickupFishGoal;
import com.smalldaydc.friendcreeper.goal.CreeperSitGoal;
import com.smalldaydc.friendcreeper.goal.CreeperSuppressTargetGoal;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Box;
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
    @Unique private static final float FISH_DROP_HEALTH_THRESHOLD = 0.25f;
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
        this.goalSelector.add(3, new CreeperFeedCatGoal(self));
        this.goalSelector.add(4, new CreeperFollowOwnerGoal(self));
        this.goalSelector.add(5, new CreeperPickupFishGoal(self));
        this.targetSelector.add(0, new CreeperSuppressTargetGoal(self));

        // Replace vanilla flee goals with conditional ones (respects afraidOfCats config)
        this.goalSelector.clear(goal -> goal instanceof FleeEntityGoal);
        this.goalSelector.add(3, new FleeEntityGoal<>(self, OcelotEntity.class, 6.0F, 1.0, 1.2) {
            @Override
            public boolean canStart() {
                if (friendcreeper$isTamed() && !FriendlyCreeperConfig.get().afraidOfCats) return false;
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
                friendcreeper$setFleeing(false);
            }
        });
        this.goalSelector.add(3, new FleeEntityGoal<>(self, CatEntity.class, 6.0F, 1.0, 1.2) {
            @Override
            public boolean canStart() {
                if (friendcreeper$isTamed() && !FriendlyCreeperConfig.get().afraidOfCats) return false;
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
                friendcreeper$setFleeing(false);
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

        // Natural regeneration: heal 1 HP every 200 ticks (~190 seconds from 1 HP to full)
        if (FriendlyCreeperConfig.get().naturalRegeneration && this.age % 200 == 0) {
            this.heal(1.0f);
        }

        // Drop held fish: low health / afraidOfCats / no hurt owner cat nearby
        ItemStack heldFish = friendcreeper$getHeldFish();
        if (!heldFish.isEmpty()) {
            boolean lowHealth = this.getHealth() / this.getMaxHealth() < FISH_DROP_HEALTH_THRESHOLD;
            boolean shouldDrop = lowHealth || FriendlyCreeperConfig.get().afraidOfCats;

            // Check for nearby hurt owner cat every 20 ticks (1 second) to reduce overhead
            if (!shouldDrop && this.age % 20 == 0) {
                UUID ownerUUID = friendcreeper$getOwnerUUID();
                boolean hasHurtOwnerCat = false;
                if (ownerUUID != null) {
                    Box searchBox = this.getBoundingBox().expand(16.0);
                    hasHurtOwnerCat = !this.getEntityWorld().getEntitiesByClass(
                            CatEntity.class, searchBox,
                            cat -> cat.isAlive()
                                    && cat.isTamed()
                                    && cat.getOwner() != null
                                    && ownerUUID.equals(cat.getOwner().getUuid())
                                    && cat.getHealth() < cat.getMaxHealth()).isEmpty();
                }
                if (!hasHurtOwnerCat) {
                    shouldDrop = true;
                }
            }

            if (shouldDrop) {
                ItemEntity drop = new ItemEntity(
                        this.getEntityWorld(),
                        this.getX(), this.getY() + 0.5, this.getZ(),
                        heldFish.copy());
                this.getEntityWorld().spawnEntity(drop);
                friendcreeper$setHeldFish(ItemStack.EMPTY);
            }
        }

        // Sync hasTarget to client for texture switching
        boolean hasTarget = this.getTarget() != null && !this.getTarget().isDead();
        if (this.dataTracker.get(FRIENDCREEPER_HAS_TARGET) != hasTarget) {
            this.dataTracker.set(FRIENDCREEPER_HAS_TARGET, hasTarget);
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
        // Save held fish: 0=none, 1=cod, 2=salmon
        ItemStack fish = friendcreeper$getHeldFish();
        if (!fish.isEmpty()) {
            view.putInt(FriendlyCreeperMod.NBT_HELD_FISH, fish.isOf(Items.COD) ? 1 : 2);
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
        // Load held fish
        int fishType = view.getInt(FriendlyCreeperMod.NBT_HELD_FISH, 0);
        if (fishType == 1) {
            this.dataTracker.set(FRIENDCREEPER_HELD_FISH, new ItemStack(Items.COD));
        } else if (fishType == 2) {
            this.dataTracker.set(FRIENDCREEPER_HELD_FISH, new ItemStack(Items.SALMON));
        }
    }
}
