package com.smalldaydc.friendlycreeper.client;

import com.smalldaydc.friendlycreeper.client.render.CreeperPoppyFeature;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.entity.CreeperEntityRenderer;
import net.minecraft.entity.EntityType;

@Environment(EnvType.CLIENT)
public class FriendlyCreeperClient implements ClientModInitializer, ModMenuApi {

    @Override
    public void onInitializeClient() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
            (entityType, entityRenderer, registrationHelper, context) -> {
                if (entityType == EntityType.CREEPER && entityRenderer instanceof CreeperEntityRenderer) {
                    registrationHelper.register(
                        new CreeperPoppyFeature(
                            (CreeperEntityRenderer) entityRenderer,
                            context.getItemRenderer()
                        )
                    );
                }
            }
        );
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return FriendlyCreeperConfigScreen::create;
        } else {
            return FriendlyCreeperNoConfigScreen::new;
        }
    }
}
