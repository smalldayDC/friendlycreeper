package com.smalldaydc.friendcreeper.compat.jade;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CreeperOwnerProvider implements IEntityComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        String ownerName = accessor.getServerData().getString(CreeperOwnerDataProvider.TAG_OWNER_NAME, "");
        if (!ownerName.isEmpty()) {
            tooltip.add(Text.translatable("jade.friendcreeper.owner", ownerName));
        }
    }

    @Override
    public Identifier getUid() {
        return FriendCreeperJadePlugin.CREEPER_OWNER;
    }
}
