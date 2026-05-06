package com.smalldaydc.friendcreeper.goal;

import com.smalldaydc.friendcreeper.ITamedCreeper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;
import java.util.UUID;

public class CreeperSuppressTargetGoal extends Goal {

    private final CreeperEntity creeper;

    public CreeperSuppressTargetGoal(CreeperEntity creeper) {
        this.creeper = creeper;
        this.setControls(EnumSet.of(Control.TARGET));
    }

    private ITamedCreeper asTamed() {
        return (ITamedCreeper)(Object) creeper;
    }

    @Override
    public boolean canStart() {
        return asTamed().friendcreeper$isTamed();
    }

    @Override
    public boolean shouldContinue() {
        return canStart();
    }

    @Override
    public void tick() {
        LivingEntity target = creeper.getTarget();
        if (target == null) return;

        // Keep non-player targets (mobs for defending owner/self)
        if (!(target instanceof PlayerEntity)) return;

        // Keep avenge target
        UUID avengeUUID = asTamed().friendcreeper$getAvengeTargetUUID();
        if (avengeUUID != null && avengeUUID.equals(target.getUuid())) return;

        // Clear all other player targets
        creeper.setTarget(null);
    }
}
