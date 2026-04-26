package com.smalldaydc.friendlycreeper.client.mixin;

import com.smalldaydc.friendlycreeper.client.IFriendlyCreeperRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.CreeperEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin(CreeperEntityRenderState.class)
public class MixinCreeperEntityRenderState implements IFriendlyCreeperRenderState {

    @Unique private boolean friendlycreeper$tamed = false;
    @Unique private boolean friendlycreeper$sitting = false;
    @Unique private final ItemRenderState friendlycreeper$poppyRenderState = new ItemRenderState();

    @Override
    public boolean friendlycreeper$isTamed() {
        return friendlycreeper$tamed;
    }

    @Override
    public void friendlycreeper$setTamed(boolean tamed) {
        this.friendlycreeper$tamed = tamed;
    }

    @Override
    public boolean friendlycreeper$isSitting() {
        return friendlycreeper$sitting;
    }

    @Override
    public void friendlycreeper$setSitting(boolean sitting) {
        this.friendlycreeper$sitting = sitting;
    }

    @Override
    public ItemRenderState friendlycreeper$getPoppyRenderState() {
        return friendlycreeper$poppyRenderState;
    }
}
