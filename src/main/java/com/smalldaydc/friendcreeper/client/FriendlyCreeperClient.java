package com.smalldaydc.friendcreeper.client;

import com.smalldaydc.friendcreeper.ITamedCreeper;
import com.smalldaydc.friendcreeper.client.render.CreeperFishFeature;
import com.smalldaydc.friendcreeper.client.render.CreeperPoppyFeature;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.CreeperEntityRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class FriendlyCreeperClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Prevent item use when right-clicking owned tamed Creeper
        UseItemCallback.EVENT.register((player, world, hand) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.crosshairTarget == null) return ActionResult.PASS;
            if (client.crosshairTarget.getType() != HitResult.Type.ENTITY) return ActionResult.PASS;

            EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;
            if (!(entityHit.getEntity() instanceof CreeperEntity creeper)) return ActionResult.PASS;

            ITamedCreeper tc = (ITamedCreeper)(Object) creeper;
            if (!tc.friendcreeper$isTamed()) return ActionResult.PASS;

            UUID ownerUUID = tc.friendcreeper$getOwnerUUID();
            if (ownerUUID == null || !ownerUUID.equals(player.getUuid())) return ActionResult.PASS;

            return ActionResult.FAIL;
        });

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
            (entityType, entityRenderer, registrationHelper, context) -> {
                if (entityType == EntityType.CREEPER && entityRenderer instanceof CreeperEntityRenderer) {
                    registrationHelper.register(
                        new CreeperPoppyFeature(
                            (CreeperEntityRenderer) entityRenderer
                        )
                    );
                    registrationHelper.register(
                        new CreeperFishFeature(
                            (CreeperEntityRenderer) entityRenderer
                        )
                    );
                }
            }
        );
    }
}
