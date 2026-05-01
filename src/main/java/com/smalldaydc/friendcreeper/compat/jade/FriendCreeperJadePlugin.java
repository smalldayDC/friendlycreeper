package com.smalldaydc.friendcreeper.compat.jade;

import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.Identifier;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class FriendCreeperJadePlugin implements IWailaPlugin {

    public static final Identifier CREEPER_OWNER = Identifier.of("friendcreeper", "creeper_owner");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(CreeperOwnerDataProvider.INSTANCE, CreeperEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(CreeperOwnerProvider.INSTANCE, CreeperEntity.class);
    }
}
