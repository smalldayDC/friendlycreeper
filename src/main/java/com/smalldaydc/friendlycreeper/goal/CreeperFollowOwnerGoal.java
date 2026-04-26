package com.smalldaydc.friendlycreeper.goal;

import com.smalldaydc.friendlycreeper.FriendlyCreeperConfig;
import com.smalldaydc.friendlycreeper.ITamedCreeper;
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

    public CreeperFollowOwnerGoal(CreeperEntity creeper) {
        this.creeper = creeper;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private ITamedCreeper asTamed() {
        return (ITamedCreeper) (Object) creeper;
    }

    @Override
    public boolean canStart() {
        if (!asTamed().friendlycreeper$isTamed()) return false;
        if (asTamed().friendlycreeper$isSitting()) return false;
        if (!FriendlyCreeperConfig.get().followOwner) return false;
        if (creeper.getEntityWorld().isClient()) return false;
        // Don't follow if currently attacking something
        if (creeper.getTarget() != null && !creeper.getTarget().isDead()) return false;

        UUID ownerUUID = asTamed().friendlycreeper$getOwnerUUID();
        if (ownerUUID == null) return false;

        owner = creeper.getEntityWorld().getPlayerByUuid(ownerUUID);
        if (owner == null || owner.isDead()) return false;

        return creeper.squaredDistanceTo(owner) > START_SQ;
    }

    @Override
    public boolean shouldContinue() {
        if (owner == null || owner.isDead()) return false;
        if (!asTamed().friendlycreeper$isTamed()) return false;
        if (!FriendlyCreeperConfig.get().followOwner) return false;
        // Stop following if a target appears
        if (creeper.getTarget() != null && !creeper.getTarget().isDead()) return false;
        return creeper.squaredDistanceTo(owner) > STOP_SQ;
    }

    @Override
    public void tick() {
        creeper.getLookControl().lookAt(owner, 10.0f, creeper.getMaxLookPitchChange());
        creeper.getNavigation().startMovingTo(owner, MOVE_SPEED);
    }

    @Override
    public void stop() {
        owner = null;
        creeper.getNavigation().stop();
    }
}
