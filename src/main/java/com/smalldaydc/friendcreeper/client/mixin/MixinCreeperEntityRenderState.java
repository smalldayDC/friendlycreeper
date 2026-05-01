package com.smalldaydc.friendcreeper.client.mixin;

import com.smalldaydc.friendcreeper.client.IFriendlyCreeperRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.CreeperEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin(CreeperEntityRenderState.class)
public class MixinCreeperEntityRenderState implements IFriendlyCreeperRenderState {

    @Unique private boolean friendcreeper$tamed = false;
    @Unique private boolean friendcreeper$sitting = false;
    @Unique private boolean friendcreeper$lowHealth = false;
    @Unique private boolean friendcreeper$hasTarget = false;
    @Unique private boolean friendcreeper$fleeing = false;
    @Unique private final ItemRenderState friendcreeper$poppyRenderState = new ItemRenderState();

    @Override
    public boolean friendcreeper$isTamed() {
        return friendcreeper$tamed;
    }

    @Override
    public void friendcreeper$setTamed(boolean tamed) {
        this.friendcreeper$tamed = tamed;
    }

    @Override
    public boolean friendcreeper$isSitting() {
        return friendcreeper$sitting;
    }

    @Override
    public void friendcreeper$setSitting(boolean sitting) {
        this.friendcreeper$sitting = sitting;
    }

    @Override
    public boolean friendcreeper$isLowHealth() {
        return friendcreeper$lowHealth;
    }

    @Override
    public void friendcreeper$setLowHealth(boolean lowHealth) {
        this.friendcreeper$lowHealth = lowHealth;
    }

    @Override
    public boolean friendcreeper$hasTarget() {
        return friendcreeper$hasTarget;
    }

    @Override
    public void friendcreeper$setHasTarget(boolean hasTarget) {
        this.friendcreeper$hasTarget = hasTarget;
    }

    @Override
    public boolean friendcreeper$isFleeing() {
        return friendcreeper$fleeing;
    }

    @Override
    public void friendcreeper$setFleeing(boolean fleeing) {
        this.friendcreeper$fleeing = fleeing;
    }

    @Override
    public ItemRenderState friendcreeper$getPoppyRenderState() {
        return friendcreeper$poppyRenderState;
    }
}
