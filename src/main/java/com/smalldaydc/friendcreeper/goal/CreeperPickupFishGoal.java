package com.smalldaydc.friendcreeper.goal;

import com.smalldaydc.friendcreeper.FriendCreeperConfig;
import com.smalldaydc.friendcreeper.FriendCreeperMod;
import com.smalldaydc.friendcreeper.ITamedCreeper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;

import java.util.EnumSet;
import java.util.List;

public class CreeperPickupFishGoal extends Goal {

    private static final double FISH_SEARCH_RANGE = 10.0;

    private final CreeperEntity creeper;
    private ItemEntity targetFish;
    private int updateCountdownTicks;

    public CreeperPickupFishGoal(CreeperEntity creeper) {
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
        if (!tc.friendcreeper$getHeldFish().isEmpty()) return false;
        if (!FriendCreeperConfig.get().feedOwnerCat) return false;
        if (FriendCreeperConfig.get().afraidOfCats) return false;
        if (creeper.getHealth() / creeper.getMaxHealth() < FriendCreeperMod.LOW_HEALTH_THRESHOLD) return false;
        if (creeper.getTarget() != null && !creeper.getTarget().isDead()) return false;

        // Only pick up fish when there is a nearby reachable hurt cat belonging to the same owner
        if (FriendCreeperMod.findNearestReachableHurtOwnerCat(creeper) == null) return false;

        targetFish = findNearestReachableFish();
        return targetFish != null;
    }

    @Override
    public boolean shouldContinue() {
        if (targetFish == null || !targetFish.isAlive()) return false;
        if (!asTamed().friendcreeper$isTamed()) return false;
        if (asTamed().friendcreeper$isSitting()) return false;
        if (!asTamed().friendcreeper$getHeldFish().isEmpty()) return false;
        if (creeper.getTarget() != null && !creeper.getTarget().isDead()) return false;
        return true;
    }

    @Override
    public void start() {
        this.updateCountdownTicks = 0;
    }

    @Override
    public void tick() {
        if (targetFish == null || !targetFish.isAlive()) return;

        creeper.getLookControl().lookAt(targetFish, 10.0f, creeper.getMaxLookPitchChange());

        if (--this.updateCountdownTicks <= 0 || creeper.getNavigation().isIdle()) {
            this.updateCountdownTicks = this.getTickCount(10);
            boolean pathFound = creeper.getNavigation().startMovingTo(targetFish, FriendCreeperMod.INTERACTION_MOVE_SPEED);
            if (!pathFound) {
                // Path became invalid mid-travel, give up immediately
                targetFish = null;
                return;
            }
        }

        // Vanilla-style pickup: bounding box overlap with pickup reach expansion
        Box pickupBox = creeper.getBoundingBox().expand(
                FriendCreeperMod.INTERACTION_REACH_XZ, FriendCreeperMod.INTERACTION_REACH_Y, FriendCreeperMod.INTERACTION_REACH_XZ);
        if (pickupBox.intersects(targetFish.getBoundingBox())) {
            asTamed().friendcreeper$setHeldFish(targetFish.getStack().copyWithCount(1));
            if (targetFish.getStack().getCount() <= 1) {
                targetFish.discard();
            } else {
                // Must call setStack() to trigger DataTracker sync to client
                targetFish.setStack(targetFish.getStack().copyWithCount(targetFish.getStack().getCount() - 1));
            }
            targetFish = null;
        }
    }

    @Override
    public void stop() {
        targetFish = null;
        creeper.getNavigation().stop();
    }

    private ItemEntity findNearestReachableFish() {
        Box searchBox = creeper.getBoundingBox().expand(FISH_SEARCH_RANGE);
        List<ItemEntity> items = creeper.getEntityWorld().getEntitiesByClass(
                ItemEntity.class, searchBox,
                item -> item.isAlive()
                        && (item.getStack().isOf(Items.COD) || item.getStack().isOf(Items.SALMON)));

        // Sort by distance so we try the closest fish first
        items.sort((a, b) -> Double.compare(
                creeper.squaredDistanceTo(a), creeper.squaredDistanceTo(b)));

        for (ItemEntity item : items) {
            // Pre-check path reachability — findPathTo can return partial paths
            Path path = creeper.getNavigation().findPathTo(item, 1);
            if (path != null && path.reachesTarget()) {
                return item;
            }
        }
        return null;
    }
}
