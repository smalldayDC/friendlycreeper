package com.smalldaydc.friendlycreeper.goal;

import com.smalldaydc.friendlycreeper.ITamedCreeper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

public class CreeperDefendOwnerGoal extends Goal {

    private final CreeperEntity creeper;

    public CreeperDefendOwnerGoal(CreeperEntity creeper) {
        this.creeper = creeper;
        this.setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        ITamedCreeper tc = (ITamedCreeper)(Object) creeper;
        if (!tc.friendlycreeper$isTamed()) return false;
        if (tc.friendlycreeper$isSitting()) return false;
        LivingEntity target = creeper.getTarget();
        return target != null && !(target instanceof PlayerEntity) && !target.isDead();
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity target = creeper.getTarget();
        return target != null && !target.isDead() && !creeper.isDead();
    }
}
