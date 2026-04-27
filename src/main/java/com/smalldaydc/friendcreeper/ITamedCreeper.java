package com.smalldaydc.friendcreeper;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public interface ITamedCreeper {
    boolean friendcreeper$isTamed();
    void friendcreeper$setTamed(boolean tamed);

    boolean friendcreeper$isSitting();
    void friendcreeper$toggleSit();

    @Nullable UUID friendcreeper$getOwnerUUID();
    void friendcreeper$setOwnerUUID(@Nullable UUID uuid);

    @Nullable UUID friendcreeper$getAvengeTargetUUID();
    void friendcreeper$setAvengeTargetUUID(@Nullable UUID uuid);

    int friendcreeper$getTameAttempts();
    void friendcreeper$setTameAttempts(int attempts);
}
