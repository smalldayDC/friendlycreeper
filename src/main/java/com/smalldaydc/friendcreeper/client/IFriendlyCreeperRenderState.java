package com.smalldaydc.friendcreeper.client;

import net.minecraft.client.render.item.ItemRenderState;

/**
 * Duck interface for injecting custom fields into {@code CreeperEntityRenderState}.
 * Implemented via {@code MixinCreeperEntityRenderState}.
 */
public interface IFriendlyCreeperRenderState {
    boolean friendcreeper$isTamed();
    void friendcreeper$setTamed(boolean tamed);

    boolean friendcreeper$isSitting();
    void friendcreeper$setSitting(boolean sitting);

    ItemRenderState friendcreeper$getPoppyRenderState();
}
