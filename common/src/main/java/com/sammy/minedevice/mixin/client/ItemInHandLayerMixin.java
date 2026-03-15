package com.sammy.minedevice.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.client.phone.PhoneClientHooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Inject(
            method = "renderArmWithItem",
            at = @At("HEAD"),
            cancellable = true)
    private void minedevice$hidePhoneModelInSelfie(LivingEntity entity, ItemStack stack,
                                                   ItemDisplayContext displayContext,
                                                   HumanoidArm arm, PoseStack poseStack,
                                                   MultiBufferSource bufferSource, int packedLight,
                                                   CallbackInfo ci) {
        if (!PhoneClientHooks.shouldUseSelfieCamera(entity)) {
            return;
        }

        if (!stack.is(ModItems.PHONE.get())) {
            return;
        }

        if (PhoneClientHooks.getRaisedPhoneArm(entity) != arm) {
            return;
        }

        ci.cancel();
    }
}
