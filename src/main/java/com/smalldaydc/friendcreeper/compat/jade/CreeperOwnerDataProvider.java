package com.smalldaydc.friendcreeper.compat.jade;

import com.smalldaydc.friendcreeper.ITamedCreeper;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.Optional;
import java.util.UUID;

public enum CreeperOwnerDataProvider implements IServerDataProvider<EntityAccessor> {
    INSTANCE;

    static final String TAG_OWNER_NAME = "FriendCreeperOwnerName";

    @Override
    public boolean shouldRequestData(EntityAccessor accessor) {
        return accessor.getEntity() instanceof CreeperEntity creeper
                && ((ITamedCreeper) creeper).friendcreeper$isTamed();
    }

    @Override
    public void appendServerData(NbtCompound data, EntityAccessor accessor) {
        CreeperEntity creeper = (CreeperEntity) accessor.getEntity();
        UUID ownerUUID = ((ITamedCreeper) creeper).friendcreeper$getOwnerUUID();
        if (ownerUUID == null) return;

        MinecraftServer server = creeper.getEntityWorld().getServer();
        if (server == null) return;

        // Try online player first
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(ownerUUID);
        if (player != null) {
            data.putString(TAG_OWNER_NAME, player.getName().getString());
            return;
        }

        // Fall back to NameToIdCache for offline players
        Optional<PlayerConfigEntry> entry = server.getApiServices().nameToIdCache().getByUuid(ownerUUID);
        entry.ifPresent(e -> data.putString(TAG_OWNER_NAME, e.name()));
    }

    @Override
    public Identifier getUid() {
        return FriendCreeperJadePlugin.CREEPER_OWNER;
    }
}
