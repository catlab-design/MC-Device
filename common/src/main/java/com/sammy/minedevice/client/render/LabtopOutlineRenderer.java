package com.sammy.minedevice.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class LabtopOutlineRenderer {
    private static final float COLOR = 0.0F;
    private static final float ALPHA = 0.4F;

    // Simplified screen panel (one single cuboid), rotated 20° around X at origin [8, 0.2, 12]
    private static final Cuboid[] SCREEN_CUBOIDS = new Cuboid[]{
            new Cuboid(2.0F, 0.2F, 12.0F, 14.0F, 9.2F, 12.2F, 20.0F, 8.0F, 0.2F, 12.0F),
    };

    // Base elements (keyboard/body), no rotation
    private static final Cuboid[] BASE_CUBOIDS = new Cuboid[]{
            // Simplified base body
            new Cuboid(2.0F, 0.0F, 3.0F, 14.0F, 0.2F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F),
    };

    // Closed state: single outline covering base + screen lying flat
    private static final Cuboid[] CLOSED_CUBOIDS = new Cuboid[]{
            new Cuboid(2.0F, 0.0F, 3.0F, 14.0F, 0.4F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F),
    };

    private static final int[][] EDGES = new int[][]{
            {0, 1}, {0, 2}, {0, 4},
            {1, 3}, {1, 5},
            {2, 3}, {2, 6},
            {3, 7},
            {4, 5}, {4, 6},
            {5, 7},
            {6, 7}
    };

    private LabtopOutlineRenderer() {
    }

    public static void render(PoseStack poseStack, VertexConsumer consumer, BlockPos pos, Direction facing,
                              boolean monitorOpen, double cameraX, double cameraY, double cameraZ) {
        poseStack.pushPose();
        poseStack.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);

        PoseStack.Pose pose = poseStack.last();

        if (monitorOpen) {
            // Render base
            for (Cuboid cuboid : BASE_CUBOIDS) {
                renderCuboid(pose, consumer, cuboid, facing);
            }
            // Render screen (rotated 20°)
            for (Cuboid cuboid : SCREEN_CUBOIDS) {
                renderCuboid(pose, consumer, cuboid, facing);
            }
        } else {
            // Render single closed outline
            for (Cuboid cuboid : CLOSED_CUBOIDS) {
                renderCuboid(pose, consumer, cuboid, facing);
            }
        }

        poseStack.popPose();
    }

    private static void renderCuboid(PoseStack.Pose pose, VertexConsumer consumer, Cuboid cuboid, Direction facing) {
        Vector3f[] corners = cuboid.corners();
        for (Vector3f corner : corners) {
            if (cuboid.xRotationDegrees != 0.0F) {
                rotateAroundX(corner, cuboid.xRotationDegrees, cuboid.originX, cuboid.originY, cuboid.originZ);
            }
            rotateForFacing(corner, facing);
            corner.mul(1.0F / 16.0F);
        }

        for (int[] edge : EDGES) {
            drawLine(pose, consumer, corners[edge[0]], corners[edge[1]]);
        }
    }

    private static void drawLine(PoseStack.Pose pose, VertexConsumer consumer, Vector3f start, Vector3f end) {
        Vector3f direction = new Vector3f(end).sub(start);
        if (direction.lengthSquared() <= 1.0E-6F) {
            return;
        }
        direction.normalize();

        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        consumer.vertex(poseMatrix, start.x(), start.y(), start.z())
                .color(COLOR, COLOR, COLOR, ALPHA)
                .normal(normalMatrix, direction.x(), direction.y(), direction.z())
                .endVertex();
        consumer.vertex(poseMatrix, end.x(), end.y(), end.z())
                .color(COLOR, COLOR, COLOR, ALPHA)
                .normal(normalMatrix, direction.x(), direction.y(), direction.z())
                .endVertex();
    }

    private static void rotateAroundX(Vector3f point, float degrees, float originX, float originY, float originZ) {
        float radians = (float) Math.toRadians(degrees);
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);
        float translatedY = point.y - originY;
        float translatedZ = point.z - originZ;
        float rotatedY = translatedY * cos - translatedZ * sin;
        float rotatedZ = translatedY * sin + translatedZ * cos;
        point.set(point.x, rotatedY + originY, rotatedZ + originZ);
    }

    private static void rotateForFacing(Vector3f point, Direction facing) {
        float x = point.x;
        float z = point.z;
        switch (facing) {
            case EAST -> point.set(16.0F - z, point.y, x);
            case SOUTH -> point.set(16.0F - x, point.y, 16.0F - z);
            case WEST -> point.set(z, point.y, 16.0F - x);
            default -> {
            }
        }
    }

    private record Cuboid(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                          float xRotationDegrees, float originX, float originY, float originZ) {
        private Vector3f[] corners() {
            return new Vector3f[]{
                    new Vector3f(minX, minY, minZ),
                    new Vector3f(maxX, minY, minZ),
                    new Vector3f(minX, maxY, minZ),
                    new Vector3f(maxX, maxY, minZ),
                    new Vector3f(minX, minY, maxZ),
                    new Vector3f(maxX, minY, maxZ),
                    new Vector3f(minX, maxY, maxZ),
                    new Vector3f(maxX, maxY, maxZ)
            };
        }
    }
}
