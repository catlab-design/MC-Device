package com.sammy.minedevice.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sammy.minedevice.ModBlocks;
import com.sammy.minedevice.block.HomePhoneBlock;
import com.sammy.minedevice.client.render.HomePhoneOutlineRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void minedevice$renderHomePhoneOutline(PoseStack poseStack, VertexConsumer vertexConsumer,
                                                   Entity entity, double cameraX, double cameraY, double cameraZ,
                                                   BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        if (!blockState.is(ModBlocks.HOME_PHONE.get())) {
            return;
        }

        HomePhoneOutlineRenderer.render(
                poseStack,
                vertexConsumer,
                blockPos,
                blockState.getValue(HomePhoneBlock.FACING),
                cameraX,
                cameraY,
                cameraZ);
        ci.cancel();
    }
}
