package com.smalldaydc.friendcreeper.goal;

import com.smalldaydc.friendcreeper.FriendCreeperConfig;
import com.smalldaydc.friendcreeper.ITamedCreeper;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;
import java.util.UUID;

public class CreeperFollowOwnerGoal extends Goal {

    private static final double FOLLOW_START_DISTANCE = 10.0;
    private static final double FOLLOW_STOP_DISTANCE  = 3.0;
    private static final double MOVE_SPEED            = 1.0;
    private static final double START_SQ = FOLLOW_START_DISTANCE * FOLLOW_START_DISTANCE;
    private static final double STOP_SQ  = FOLLOW_STOP_DISTANCE  * FOLLOW_STOP_DISTANCE;

    private final CreeperEntity creeper;
    private PlayerEntity owner;
    private int updateCountdownTicks;
    private double lastDistanceSq;
    private int noProgressCount;

    public CreeperFollowOwnerGoal(CreeperEntity creeper) {
        this.creeper = creeper;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private ITamedCreeper asTamed() {
        return (ITamedCreeper) (Object) creeper;
    }

    @Override
    public boolean canStart() {
        if (!asTamed().friendcreeper$isTamed()) return false;
        if (asTamed().friendcreeper$isSitting()) return false;
        if (!FriendCreeperConfig.get().followOwner) return false;
        if (creeper.getTarget() != null && creeper.getTarget().isAlive()) return false;

        UUID ownerUUID = asTamed().friendcreeper$getOwnerUUID();
        if (ownerUUID == null) return false;

        owner = creeper.getEntityWorld().getPlayerByUuid(ownerUUID);
        if (owner == null || !owner.isAlive() || owner.isSpectator()) return false;

        return creeper.squaredDistanceTo(owner) > START_SQ;
    }

    @Override
    public boolean shouldContinue() {
        if (owner == null || !owner.isAlive() || owner.isSpectator()) return false;
        if (!asTamed().friendcreeper$isTamed()) return false;
        if (!FriendCreeperConfig.get().followOwner) return false;
        if (creeper.getTarget() != null && creeper.getTarget().isAlive()) return false;
        // No progress for 5 consecutive recalculations → owner unreachable
        if (noProgressCount >= 5) return false;
        return creeper.squaredDistanceTo(owner) > STOP_SQ;
    }

    @Override
    public void start() {
        this.updateCountdownTicks = 0;
        this.lastDistanceSq = creeper.squaredDistanceTo(owner);
        this.noProgressCount = 0;
    }

    @Override
    public void tick() {
        creeper.getLookControl().lookAt(owner, 10.0f, creeper.getMaxLookPitchChange());

        // Recalculate on timer OR immediately when path ends (fixes stale path target)
        if (--this.updateCountdownTicks <= 0 || creeper.getNavigation().isIdle()) {
            this.updateCountdownTicks = this.getTickCount(10);
            creeper.getNavigation().startMovingTo(owner, MOVE_SPEED);

            // Track progress: if distance hasn't decreased, owner may be unreachable
            double currentDistSq = creeper.squaredDistanceTo(owner);
            if (currentDistSq < lastDistanceSq - 1.0) {
                lastDistanceSq = currentDistSq;
                noProgressCount = 0;
            } else {
                noProgressCount++;
            }
        }
    }

    @Override
    public void stop() {
        owner = null;
        creeper.getNavigation().stop();
    }
}
