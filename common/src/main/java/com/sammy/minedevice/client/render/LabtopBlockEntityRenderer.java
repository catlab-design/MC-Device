package com.sammy.minedevice.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sammy.minedevice.ModBlocks;
import com.sammy.minedevice.block.LabtopBlock;
import com.sammy.minedevice.block.entity.LabtopBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class LabtopBlockEntityRenderer implements BlockEntityRenderer<LabtopBlockEntity> {
    public LabtopBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LabtopBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!state.is(ModBlocks.LABTOP.get())) {
            return;
        }

        // Get the screen model by querying the blockstate with IS_SCREEN = true
        BlockState screenState = state.setValue(LabtopBlock.IS_SCREEN, true);
        BakedModel screenModel = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(screenState);

        poseStack.pushPose();

        // 1. Move to block center (8, 0, 8)
        poseStack.translate(0.5D, 0.0D, 0.5D);

        // 2. Rotate based on block FACING
        Direction facing = state.getValue(LabtopBlock.FACING);
        float yRot = switch (facing) {
            case EAST -> -90.0F;
            case SOUTH -> -180.0F;
            case WEST -> -270.0F;
            default -> 0.0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));

        // 3. Move back relative to block center
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        // 4. Perform the screen rotation (animation) around the hinge axis [8, 0.2, 12]
        poseStack.translate(0.5D, 0.0125D, 0.75D);

        // Interpolate opening progress
        float progress = Mth.lerp(partialTick, blockEntity.prevOpenProgress, blockEntity.openProgress);
        
        // Apply cubic ease-out: 1 - (1 - x)^3
        float easedProgress = 1.0F - (1.0F - progress) * (1.0F - progress) * (1.0F - progress);
        
        // Closed angle = -90.0F (lying flat)
        // Open angle = 20.0F (tilted back 20 degrees)
        float angle = -90.0F * (1.0F - easedProgress) + 20.0F * easedProgress;
        poseStack.mulPose(Axis.XP.rotationDegrees(angle));

        // Move back from the hinge
        poseStack.translate(-0.5D, -0.0125D, -0.75D);

        // Render the screen model with per-face directional shading
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
        PoseStack.Pose pose = poseStack.last();
        RandomSource random = RandomSource.create();

        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            float shade = getRotatedShade(direction, yRot, angle);
            List<BakedQuad> quads = screenModel.getQuads(screenState, direction, random);
            for (BakedQuad quad : quads) {
                consumer.putBulkData(pose, quad, shade, shade, shade, 1.0F, packedLight, packedOverlay);
            }
        }
        random.setSeed(42L);
        List<BakedQuad> unculledQuads = screenModel.getQuads(screenState, null, random);
        for (BakedQuad quad : unculledQuads) {
            float shade = getRotatedShade(quad.getDirection(), yRot, angle);
            consumer.putBulkData(pose, quad, shade, shade, shade, 1.0F, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    private static float getRotatedShade(Direction originalDir, float yRot, float xRot) {
        // 1. Get original normal
        float nx = originalDir.getStepX();
        float ny = originalDir.getStepY();
        float nz = originalDir.getStepZ();

        // 2. Rotate around X axis by xRot (screen tilt angle)
        float xRad = (float) Math.toRadians(xRot);
        float cosX = (float) Math.cos(xRad);
        float sinX = (float) Math.sin(xRad);
        float ry = ny * cosX - nz * sinX;
        float rz = ny * sinX + nz * cosX;
        float rx = nx;

        // 3. Rotate around Y axis by yRot (block facing angle)
        // yRot is in degrees, but we need to match the PoseStack rotation direction
        float yRad = (float) Math.toRadians(yRot);
        float cosY = (float) Math.cos(yRad);
        float sinY = (float) Math.sin(yRad);
        float rry = ry;
        // Standard Y-rotation transformation matrix:
        // x' = x * cos(theta) + z * sin(theta)
        // z' = -x * sin(theta) + z * cos(theta)
        float rrx = rx * cosY + rz * sinY;
        float rrz = -rx * sinY + rz * cosY;

        // 4. Calculate diffuse factor based on rotated normal:
        // UP = 1.0, DOWN = 0.5, NORTH/SOUTH = 0.8, EAST/WEST = 0.6
        float nxSq = rrx * rrx;
        float nySq = rry * rry;
        float nzSq = rrz * rrz;
        
        return nxSq * 0.6F + (rry > 0 ? nySq * 1.0F : nySq * 0.5F) + nzSq * 0.8F;
    }
}
