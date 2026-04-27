package com.smalldaydc.friendcreeper.goal;

import com.smalldaydc.friendcreeper.FriendlyCreeperConfig;
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
    private int cooldown;

    public CreeperFollowOwnerGoal(CreeperEntity creeper) {
        this.creeper = creeper;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private ITamedCreeper asTamed() {
        return (ITamedCreeper) (Object) creeper;
    }

    @Override
    public boolean canStart() {
        if (cooldown > 0) { cooldown--; return false; }
        if (!asTamed().friendcreeper$isTamed()) return false;
        if (asTamed().friendcreeper$isSitting()) return false;
        if (!FriendlyCreeperConfig.get().followOwner) return false;
        if (creeper.getEntityWorld().isClient()) return false;
        // Don't follow if currently attacking something
        if (creeper.getTarget() != null && !creeper.getTarget().isDead()) return false;

        UUID ownerUUID = asTamed().friendcreeper$getOwnerUUID();
        if (ownerUUID == null) return false;

        owner = creeper.getEntityWorld().getPlayerByUuid(ownerUUID);
        if (owner == null || owner.isDead() || owner.isSpectator()) return false;

        return creeper.squaredDistanceTo(owner) > START_SQ;
    }

    @Override
    public boolean shouldContinue() {
        if (owner == null || owner.isDead() || owner.isSpectator()) return false;
        if (!asTamed().friendcreeper$isTamed()) return false;
        if (!FriendlyCreeperConfig.get().followOwner) return false;
        // Stop following if a target appears
        if (creeper.getTarget() != null && !creeper.getTarget().isDead()) return false;
        // Stop if navigation gave up (owner unreachable)
        if (creeper.getNavigation().isIdle()) return false;
        return creeper.squaredDistanceTo(owner) > STOP_SQ;
    }

    @Override
    public void start() {
        this.updateCountdownTicks = 0;
    }

    @Override
    public void tick() {
        creeper.getLookControl().lookAt(owner, 10.0f, creeper.getMaxLookPitchChange());
        if (--this.updateCountdownTicks <= 0) {
            this.updateCountdownTicks = this.getTickCount(10);
            creeper.getNavigation().startMovingTo(owner, MOVE_SPEED);
        }
    }

    @Override
    public void stop() {
        // If stopped while still far from owner, add cooldown to prevent rapid restart
        if (owner != null && creeper.squaredDistanceTo(owner) > START_SQ) {
            cooldown = 40; // 2 seconds
        }
        owner = null;
        creeper.getNavigation().stop();
    }
}
