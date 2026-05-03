package com.smalldaydc.friendcreeper.client.mixin;

import com.smalldaydc.friendcreeper.FriendlyCreeperConfig;
import com.smalldaydc.friendcreeper.FriendlyCreeperMod;
import com.smalldaydc.friendcreeper.ITamedCreeper;
import com.smalldaydc.friendcreeper.client.IFriendlyCreeperRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.CreeperEntityRenderer;
import net.minecraft.client.render.entity.state.CreeperEntityRenderState;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(CreeperEntityRenderer.class)
public class MixinCreeperEntityRenderer {

    @Unique private static final ItemStack POPPY_STACK = new ItemStack(Items.POPPY);
    @Unique private static final ItemStack WITHER_ROSE_STACK = new ItemStack(Items.WITHER_ROSE);
    @Unique private static final Identifier HAPPY_TEXTURE = Identifier.of("friendcreeper", "textures/entity/creeper/happy.png");
    @Unique private static final Identifier SAD_TEXTURE = Identifier.of("friendcreeper", "textures/entity/creeper/sad.png");

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void friendcreeper$updateRenderState(CreeperEntity entity,
                                                    CreeperEntityRenderState state,
                                                    float tickDelta,
                                                    CallbackInfo ci) {
        ITamedCreeper tc = (ITamedCreeper) (Object) entity;
        IFriendlyCreeperRenderState fcState = (IFriendlyCreeperRenderState) state;
        fcState.friendcreeper$setTamed(tc.friendcreeper$isTamed());
        fcState.friendcreeper$setSitting(tc.friendcreeper$isSitting());

        boolean lowHealth = entity.getHealth() / entity.getMaxHealth() < FriendlyCreeperMod.LOW_HEALTH_THRESHOLD;
        fcState.friendcreeper$setLowHealth(lowHealth);
        fcState.friendcreeper$setHasTarget(tc.friendcreeper$hasTarget());
        fcState.friendcreeper$setFleeing(tc.friendcreeper$isFleeing());

        if (tc.friendcreeper$isTamed()) {
            FriendlyCreeperConfig config = FriendlyCreeperConfig.get();
            // Show wither rose when low health (if enabled), poppy otherwise
            ItemStack flowerStack = (config.witherRoseOnLowHealth && lowHealth)
                    ? WITHER_ROSE_STACK : POPPY_STACK;
            MinecraftClient.getInstance().getItemModelManager()
                    .updateForNonLivingEntity(fcState.friendcreeper$getPoppyRenderState(),
                            flowerStack, ItemDisplayContext.GROUND, entity);

            // Update fish render state
            ItemStack heldFish = tc.friendcreeper$getHeldFish();
            if (!heldFish.isEmpty()) {
                MinecraftClient.getInstance().getItemModelManager()
                        .updateForNonLivingEntity(fcState.friendcreeper$getFishRenderState(),
                                heldFish, ItemDisplayContext.GROUND, entity);
            } else {
                fcState.friendcreeper$getFishRenderState().clear();
            }
        } else {
            fcState.friendcreeper$getPoppyRenderState().clear();
            fcState.friendcreeper$getFishRenderState().clear();
        }
    }

    @Inject(method = "getTexture", at = @At("RETURN"), cancellable = true)
    private void friendcreeper$getTexture(CreeperEntityRenderState state,
                                           CallbackInfoReturnable<Identifier> cir) {
        IFriendlyCreeperRenderState fcState = (IFriendlyCreeperRenderState) state;
        if (!fcState.friendcreeper$isTamed()) return;
        if (!FriendlyCreeperConfig.get().tamedCreeperTexture) return;
        // Revert to vanilla face when targeting an enemy
        if (fcState.friendcreeper$hasTarget()) return;

        // Show sad face when fleeing from cats (if enabled)
        if (fcState.friendcreeper$isFleeing() && FriendlyCreeperConfig.get().scaredFace) {
            cir.setReturnValue(SAD_TEXTURE);
            return;
        }

        cir.setReturnValue(fcState.friendcreeper$isLowHealth() ? SAD_TEXTURE : HAPPY_TEXTURE);
    }
}
