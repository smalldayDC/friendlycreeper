package com.smalldaydc.friendcreeper.goal;

import com.smalldaydc.friendcreeper.FriendlyCreeperConfig;
import com.smalldaydc.friendcreeper.FriendlyCreeperMod;
import com.smalldaydc.friendcreeper.ITamedCreeper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;

import java.util.EnumSet;
import java.util.List;

public class CreeperFeedCatGoal extends Goal {

    private final CreeperEntity creeper;
    private CatEntity targetCat;
    private int updateCountdownTicks;

    public CreeperFeedCatGoal(CreeperEntity creeper) {
        this.creeper = creeper;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private ITamedCreeper asTamed() {
        return (ITamedCreeper) (Object) creeper;
    }

    @Override
    public boolean canStart() {
        ITamedCreeper tc = asTamed();
        if (!tc.friendcreeper$isTamed()) return false;
        if (tc.friendcreeper$isSitting()) return false;
        if (tc.friendcreeper$getHeldFish().isEmpty()) return false;
        if (!FriendlyCreeperConfig.get().feedOwnerCat) return false;
        if (FriendlyCreeperConfig.get().afraidOfCats) return false;
        if (creeper.getHealth() / creeper.getMaxHealth() < FriendlyCreeperMod.LOW_HEALTH_THRESHOLD) return false;
        if (creeper.getEntityWorld().isClient()) return false;
        if (creeper.getTarget() != null && !creeper.getTarget().isDead()) return false;

        targetCat = findHungryOwnerCat();
        return targetCat != null;
    }

    @Override
    public boolean shouldContinue() {
        if (targetCat == null || !targetCat.isAlive()) return false;
        if (asTamed().friendcreeper$getHeldFish().isEmpty()) return false;
        if (!asTamed().friendcreeper$isTamed()) return false;
        if (asTamed().friendcreeper$isSitting()) return false;
        if (creeper.getTarget() != null && !creeper.getTarget().isDead()) return false;
        // Stop if cat is fully healed
        if (targetCat.getHealth() >= targetCat.getMaxHealth()) return false;
        return true;
    }

    @Override
    public void start() {
        this.updateCountdownTicks = 0;
    }

    @Override
    public void tick() {
        if (targetCat == null || !targetCat.isAlive()) return;

        creeper.getLookControl().lookAt(targetCat, 10.0f, creeper.getMaxLookPitchChange());

        if (--this.updateCountdownTicks <= 0 || creeper.getNavigation().isIdle()) {
            this.updateCountdownTicks = this.getTickCount(10);
            creeper.getNavigation().startMovingTo(targetCat, FriendlyCreeperMod.INTERACTION_MOVE_SPEED);
        }

        // Bounding box overlap check for feeding
        Box feedBox = creeper.getBoundingBox().expand(
                FriendlyCreeperMod.INTERACTION_REACH_XZ, FriendlyCreeperMod.INTERACTION_REACH_Y, FriendlyCreeperMod.INTERACTION_REACH_XZ);
        if (feedBox.intersects(targetCat.getBoundingBox())) {
            feedCat();
        }
    }

    @Override
    public void stop() {
        targetCat = null;
        creeper.getNavigation().stop();
    }

    private void feedCat() {
        if (targetCat == null) return;

        ItemStack fish = asTamed().friendcreeper$getHeldFish();
        // Heal using the food's nutrition value, matching vanilla Cat.mobInteract logic
        FoodComponent food = fish.get(DataComponentTypes.FOOD);
        targetCat.heal(food != null ? (float) food.nutrition() : 1.0f);
        asTamed().friendcreeper$setHeldFish(ItemStack.EMPTY);

        targetCat.playSound(SoundEvents.ENTITY_CAT_EAT, 1.0f, 1.0f);
        if (creeper.getEntityWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.HEART,
                    targetCat.getX(), targetCat.getBodyY(0.5), targetCat.getZ(),
                    3, 0.3, 0.3, 0.3, 0.05);
        }

        targetCat = null;
    }

    private CatEntity findHungryOwnerCat() {
        List<CatEntity> cats = FriendlyCreeperMod.findHurtOwnerCats(creeper, FriendlyCreeperMod.CAT_SEARCH_RANGE);

        CatEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (CatEntity cat : cats) {
            double distSq = creeper.squaredDistanceTo(cat);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = cat;
            }
        }
        return nearest;
    }
}
