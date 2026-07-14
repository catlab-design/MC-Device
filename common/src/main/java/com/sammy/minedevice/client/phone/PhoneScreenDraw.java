package com.sammy.minedevice.client.phone;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class PhoneScreenDraw {
    private PhoneScreenDraw() {
    }

    static float textScaleToFit(Font font, Component text, int maxWidth) {
        return textScaleToFit(font, text, maxWidth, 0.45F);
    }

    static float textScaleToFit(Font font, Component text, int maxWidth, float minScale) {
        int textWidth = Math.max(1, font.width(text));
        return Math.min(1.0F, Math.max(minScale, (float) maxWidth / textWidth));
    }

    static int scaledTextWidth(Font font, Component text, float textScale) {
        return Math.max(1, Math.round(font.width(text) * textScale));
    }

    static int scaledTextHeight(Font font, float textScale) {
        return Math.max(1, Math.round(font.lineHeight * textScale));
    }

    static void drawScaledText(GuiGraphics guiGraphics, Font font, Component text,
                               int x, int y, int color, boolean shadow, float textScale) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(textScale, textScale, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, color, shadow);
        guiGraphics.pose().popPose();
    }

    static void drawPhotoInArea(GuiGraphics guiGraphics, PhotoTexture photoTexture,
                                int areaX, int areaY, int areaWidth, int areaHeight) {
        drawTextureInArea(guiGraphics, photoTexture.textureId, photoTexture.width, photoTexture.height,
                areaX, areaY, areaWidth, areaHeight, 1.03F, true);
    }

    static void drawTextureInArea(GuiGraphics guiGraphics, ResourceLocation textureId, int textureWidth, int textureHeight,
                                  int areaX, int areaY, int areaWidth, int areaHeight, float zoomMultiplier) {
        drawTextureInArea(guiGraphics, textureId, textureWidth, textureHeight, areaX, areaY, areaWidth, areaHeight, zoomMultiplier, true);
    }

    static void drawTextureInArea(GuiGraphics guiGraphics, ResourceLocation textureId, int textureWidth, int textureHeight,
                                  int areaX, int areaY, int areaWidth, int areaHeight, float zoomMultiplier, boolean fit) {
        if (textureWidth <= 0 || textureHeight <= 0) {
            return;
        }

        float scaleFactor = fit
                ? Math.min((float) areaWidth / textureWidth, (float) areaHeight / textureHeight)
                : Math.max((float) areaWidth / textureWidth, (float) areaHeight / textureHeight);
        scaleFactor *= zoomMultiplier;
        int drawWidth = Math.max(1, Math.round(textureWidth * scaleFactor));
        int drawHeight = Math.max(1, Math.round(textureHeight * scaleFactor));
        int drawX = areaX + (areaWidth - drawWidth) / 2;
        int drawY = areaY + (areaHeight - drawHeight) / 2;

        guiGraphics.enableScissor(areaX, areaY, areaX + areaWidth, areaY + areaHeight);
        guiGraphics.blit(textureId, drawX, drawY, drawWidth, drawHeight,
                0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight);
        guiGraphics.disableScissor();
    }

    /**
     * Draws a simple unlocked-padlock silhouette (body + open shackle) as solid fills
     * in a caller-chosen color. Used instead of a texture blit because the baked
     * unlock icon PNG is pure black and can't be recolored with a shader tint.
     */
    static void drawPadlockIcon(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        int bodyWidth = Math.round(size * 0.60F);
        int bodyHeight = Math.round(size * 0.44F);
        int bodyLeft = x + (size - bodyWidth) / 2;
        int bodyTop = y + size - bodyHeight - Math.round(size * 0.12F);
        int bodyBottom = bodyTop + bodyHeight;

        int thickness = Math.max(1, Math.round(size * 0.12F));
        int shackleWidth = Math.round(bodyWidth * 0.62F);
        int shackleLeft = x + (size - shackleWidth) / 2;
        int shackleTop = y + Math.round(size * 0.08F);

        // Shackle: left bar, top bar, right bar — open ring above the lock body.
        guiGraphics.fill(shackleLeft, shackleTop, shackleLeft + thickness, bodyTop + thickness, color);
        guiGraphics.fill(shackleLeft, shackleTop, shackleLeft + shackleWidth, shackleTop + thickness, color);
        guiGraphics.fill(shackleLeft + shackleWidth - thickness, shackleTop, shackleLeft + shackleWidth, bodyTop + thickness, color);

        // Lock body.
        guiGraphics.fill(bodyLeft, bodyTop, bodyLeft + bodyWidth, bodyBottom, color);

        // Keyhole: a translucent dark dot that reads as a punched hole regardless of accent color.
        int dotSize = Math.max(1, Math.round(size * 0.10F));
        int dotX = bodyLeft + (bodyWidth - dotSize) / 2;
        int dotY = bodyTop + Math.round(bodyHeight * 0.30F);
        guiGraphics.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, 0x77000000);
    }
}
