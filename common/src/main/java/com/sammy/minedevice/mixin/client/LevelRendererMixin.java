package com.sammy.minedevice.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sammy.minedevice.ModBlocks;
import com.sammy.minedevice.block.HomePhoneBlock;
import com.sammy.minedevice.block.LabtopBlock;
import com.sammy.minedevice.client.airstrike.AirstrikeClientState;
import com.sammy.minedevice.client.render.HomePhoneOutlineRenderer;
import com.sammy.minedevice.client.render.LabtopOutlineRenderer;
import com.sammy.minedevice.client.render.AirstrikeTargetRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
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
        if (blockState.is(ModBlocks.HOME_PHONE.get())) {
            HomePhoneOutlineRenderer.render(
                    poseStack,
                    vertexConsumer,
                    blockPos,
                    blockState.getValue(HomePhoneBlock.FACING),
                    cameraX,
                    cameraY,
                    cameraZ);
            ci.cancel();
            return;
        }

        if (blockState.is(ModBlocks.LABTOP.get())) {
            LabtopOutlineRenderer.render(
                    poseStack,
                    vertexConsumer,
                    blockPos,
                    blockState.getValue(LabtopBlock.FACING),
                    blockState.getValue(LabtopBlock.MONITOR_OPEN),
                    cameraX,
                    cameraY,
                    cameraZ);
            ci.cancel();
        }
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void minedevice$renderAirstrikeTarget(DeltaTracker deltaTracker, boolean renderBlockOutline,
                                                  Camera camera, GameRenderer gameRenderer,
                                                  LightTexture lightTexture, Matrix4f frustumMatrix,
                                                  Matrix4f projectionMatrix,
                                                  CallbackInfo ci) {
        java.util.List<Vec3> renderPositions = AirstrikeClientState.getRenderPositions(Minecraft.getInstance());
        if (renderPositions.isEmpty()) {
            return;
        }

        PoseStack poseStack = new PoseStack();
        for (Vec3 renderPosition : renderPositions) {
            AirstrikeTargetRenderer.render(poseStack, camera, renderPosition.x, renderPosition.y, renderPosition.z);
        }
    }
}
