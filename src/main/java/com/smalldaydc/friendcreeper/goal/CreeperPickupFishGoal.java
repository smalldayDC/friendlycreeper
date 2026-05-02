package com.smalldaydc.friendcreeper.goal;

import com.smalldaydc.friendcreeper.FriendlyCreeperConfig;
import com.smalldaydc.friendcreeper.ITamedCreeper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class CreeperPickupFishGoal extends Goal {

    private static final double SEARCH_RANGE = 10.0;
    private static final double CAT_SEARCH_RANGE = 16.0;
    private static final double PICKUP_RANGE_SQ = 2.0 * 2.0;
    private static final double MOVE_SPEED = 1.0;

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
        if (!FriendlyCreeperConfig.get().feedOwnerCat) return false;
        if (FriendlyCreeperConfig.get().afraidOfCats) return false;
        if (creeper.getHealth() / creeper.getMaxHealth() < 0.25f) return false;
        if (creeper.getEntityWorld().isClient()) return false;
        if (creeper.getTarget() != null && !creeper.getTarget().isDead()) return false;

        // Only pick up fish when there is a nearby hurt cat belonging to the same owner
        if (!hasHurtOwnerCat()) return false;

        targetFish = findNearestFish();
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
            creeper.getNavigation().startMovingTo(targetFish, MOVE_SPEED);
        }

        // Check if close enough to pick up
        if (creeper.squaredDistanceTo(targetFish) <= PICKUP_RANGE_SQ) {
            asTamed().friendcreeper$setHeldFish(targetFish.getStack().copyWithCount(1));
            if (targetFish.getStack().getCount() > 1) {
                targetFish.getStack().decrement(1);
            } else {
                targetFish.discard();
            }
            targetFish = null;
        }
    }

    @Override
    public void stop() {
        targetFish = null;
        creeper.getNavigation().stop();
    }

    private boolean hasHurtOwnerCat() {
        UUID ownerUUID = asTamed().friendcreeper$getOwnerUUID();
        if (ownerUUID == null) return false;

        Box searchBox = creeper.getBoundingBox().expand(CAT_SEARCH_RANGE);
        List<CatEntity> cats = creeper.getEntityWorld().getEntitiesByClass(
                CatEntity.class, searchBox,
                cat -> cat.isAlive()
                        && cat.isTamed()
                        && cat.getOwner() != null
                        && ownerUUID.equals(cat.getOwner().getUuid())
                        && cat.getHealth() < cat.getMaxHealth());
        return !cats.isEmpty();
    }

    private ItemEntity findNearestFish() {
        Box searchBox = creeper.getBoundingBox().expand(SEARCH_RANGE);
        List<ItemEntity> items = creeper.getEntityWorld().getEntitiesByClass(
                ItemEntity.class, searchBox,
                item -> item.isAlive()
                        && (item.getStack().isOf(Items.COD) || item.getStack().isOf(Items.SALMON)));

        ItemEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            double distSq = creeper.squaredDistanceTo(item);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = item;
            }
        }
        return nearest;
    }
}
