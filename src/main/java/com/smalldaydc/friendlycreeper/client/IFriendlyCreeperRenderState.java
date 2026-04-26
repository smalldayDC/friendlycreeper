package com.smalldaydc.friendlycreeper.client;

import net.minecraft.client.render.item.ItemRenderState;

/**
 * Duck interface for injecting custom fields into {@code CreeperEntityRenderState}.
 * Implemented via {@code MixinCreeperEntityRenderState}.
 */
public interface IFriendlyCreeperRenderState {
    boolean friendlycreeper$isTamed();
    void friendlycreeper$setTamed(boolean tamed);

    boolean friendlycreeper$isSitting();
    void friendlycreeper$setSitting(boolean sitting);

    ItemRenderState friendlycreeper$getPoppyRenderState();
}
