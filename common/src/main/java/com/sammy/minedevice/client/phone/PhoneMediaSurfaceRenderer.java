package com.sammy.minedevice.client.phone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

final class PhoneMediaSurfaceRenderer {
    private PhoneMediaSurfaceRenderer() {
    }

    static void renderGallerySurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        List<PhotoEntry> photos = screen.getPhotos();
        int pageCount = screen.getGalleryPageCount();
        UiRect contentBounds = screen.getMediaSurfaceBounds();
        int displayLeft = screen.displayX;
        int displayRight = screen.displayX + screen.displayWidth;
        int displayTop = screen.displayY;
        int displayBottom = screen.displayY + screen.displayHeight;

        int contentLeft = contentBounds.left;
        int contentRight = contentBounds.right();
        int contentTop = contentBounds.top;
        int contentBottom = contentBounds.bottom();
        int headerHeight = screen.getGalleryHeaderHeight();
        int headerBottom = Math.min(contentBottom, contentTop + headerHeight);
        int headerInset = Math.max(1, Math.round(2 * screen.scale));
        int headerBleed = Math.max(2, Math.round(4 * screen.scale));
        int headerLeft = contentLeft - headerBleed;
        int headerTop = contentTop + headerInset;
        int headerRight = contentRight + headerBleed;

        guiGraphics.fill(displayLeft, displayTop, displayRight, displayBottom, 0xFFFFFFFF);

        Component titleText = Component.translatable("screen.minedevice.phone.gallery.title")
                .append(Component.literal(" " + (screen.galleryPage + 1) + "/" + pageCount));
        int titleX = headerLeft + Math.round(6 * screen.scale);
        int titleY = headerTop + Math.round(7 * screen.scale);

        UiRect importButton = screen.getGalleryImportButtonBounds();
        int maxTitleWidth = Math.max(24, importButton.left - titleX - Math.max(4, Math.round(6 * screen.scale)));
        float titleScale = PhoneScreenDraw.textScaleToFit(minecraft.font, titleText, maxTitleWidth, 0.6F);
        titleScale = Math.min(titleScale, 1.25F);
        int titleHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, titleScale);

        int headerBandBottom = Math.min(headerBottom,
                titleY + titleHeight + Math.max(6, Math.round(8 * screen.scale)));

        guiGraphics.fill(headerLeft, headerTop, headerRight, headerBandBottom, 0xFF6C92F4);

        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, titleText, titleX, titleY, 0xFFFFFFFF, false, titleScale);

        // Import-from-file button (top-right of the header band)
        guiGraphics.fill(importButton.left, importButton.top, importButton.right(), importButton.bottom(), 0x40FFFFFF);
        Component importText = Component.translatable("screen.minedevice.phone.gallery.import");
        float importScale = Math.min(0.55F, PhoneScreenDraw.textScaleToFit(minecraft.font, importText, importButton.width - 4, 0.4F));
        int importTextWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, importText, importScale);
        int importTextHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, importScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, importText,
                importButton.left + (importButton.width - importTextWidth) / 2,
                importButton.top + (importButton.height - importTextHeight) / 2, 0xFFFFFFFF, false, importScale);

        if (photos.isEmpty()) {
            Component emptyText = Component.translatable("screen.minedevice.phone.gallery.empty");
            float emptyScale = PhoneScreenDraw.textScaleToFit(minecraft.font, emptyText,
                    contentRight - contentLeft - Math.round(20 * screen.scale), 0.8F);
            emptyScale = Math.min(emptyScale, 1.15F);
            int emptyWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, emptyText, emptyScale);
            int emptyHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, emptyScale);
            int availableEmptyHeight = contentBottom - headerBandBottom;
            int emptyX = contentLeft + (contentRight - contentLeft - emptyWidth) / 2;
            int emptyY = headerBandBottom + (availableEmptyHeight - emptyHeight) / 2 - Math.round(10 * screen.scale);

            PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, emptyText, emptyX, emptyY, 0xFFAAAAAA, false, emptyScale);
            return;
        }

        int pageStart = screen.galleryPage * PhoneScreen.GALLERY_PAGE_SIZE;
        int pageEnd = Math.min(pageStart + PhoneScreen.GALLERY_PAGE_SIZE, photos.size());
        int pageItemCount = pageEnd - pageStart;

        for (int photoIndex = pageStart; photoIndex < pageEnd; photoIndex++) {
            int localIndex = photoIndex - pageStart;
            UiRect slotBounds = screen.getGallerySlotBounds(localIndex, pageItemCount);
            int cardX = slotBounds.left;
            int cardY = slotBounds.top;
            int cardRight = slotBounds.right();
            int cardBottom = slotBounds.bottom();

            guiGraphics.fill(cardX, cardY, cardRight, cardBottom, 0xFFE5E5EA);

            PhotoEntry photoEntry = photos.get(photoIndex);
            boolean renderedImage = false;
            if (photoEntry.hasFile()) {
                PhotoTexture photoTexture = screen.getOrLoadPhotoTexture(photoEntry.fileName, true);
                if (photoTexture != null) {
                    PhoneScreenDraw.drawTextureInArea(guiGraphics, photoTexture.textureId, photoTexture.width, photoTexture.height,
                            cardX, cardY, slotBounds.width, slotBounds.height, 1.0F, false);
                    renderedImage = true;
                }
            }

            if (!renderedImage && !photoEntry.itemStack.isEmpty()) {
                int itemX = cardX + (slotBounds.width - 16) / 2;
                int itemY = cardY + (slotBounds.height - 16) / 2;
                guiGraphics.renderItem(photoEntry.itemStack, itemX, itemY);
                guiGraphics.renderItemDecorations(minecraft.font, photoEntry.itemStack, itemX, itemY);
            }
        }
    }

    static void renderPhotoViewerSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        List<PhotoEntry> photos = screen.getPhotos();
        if (photos.isEmpty()) {
            screen.photoViewerMode = false;
            screen.galleryMode = true;
            screen.viewerPhotoIndex = -1;
            screen.rebuildWidgets();
            return;
        }

        screen.viewerPhotoIndex = Mth.clamp(screen.viewerPhotoIndex, 0, photos.size() - 1);
        PhotoEntry photoEntry = photos.get(screen.viewerPhotoIndex);
        ViewerLayout viewerLayout = screen.getViewerLayout();
        UiRect contentBounds = screen.getMediaSurfaceBounds();
        int displayLeft = screen.displayX;
        int displayRight = screen.displayX + screen.displayWidth;
        int displayTop = screen.displayY;
        int displayBottom = screen.displayY + screen.displayHeight;

        Component pageText = Component.translatable("screen.minedevice.phone.viewer.page",
                screen.viewerPhotoIndex + 1, photos.size());

        int contentLeft = contentBounds.left + Math.round(5 * screen.scale);
        int contentRight = contentBounds.right() - Math.round(5 * screen.scale);
        int contentTop = contentBounds.top + Math.round(8 * screen.scale);
        int contentBottom = contentBounds.bottom();
        int headerBottom = viewerLayout.areaY;

        guiGraphics.fill(displayLeft, displayTop, displayRight, displayBottom, 0xFF000000);
        guiGraphics.fill(contentLeft, contentTop, contentRight, headerBottom, 0xFF000000);

        UiRect exportButton = screen.getViewerExportButtonBounds();
        int titleX = contentLeft + Math.round(7 * screen.scale);
        int titleY = contentTop + Math.round(6 * screen.scale);
        // Shrink instead of overflowing into the Save button when space is tight.
        int maxTitleWidth = Math.max(24, exportButton.left - titleX - Math.round(8 * screen.scale));
        float titleScale = PhoneScreenDraw.textScaleToFit(minecraft.font, pageText, maxTitleWidth, 0.45F);
        titleScale = Math.min(titleScale, 1.0F);

        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, pageText, titleX, titleY, 0xFFFFFFFF, false, titleScale);

        // Export (save-to-disk) button
        guiGraphics.fill(exportButton.left, exportButton.top, exportButton.right(), exportButton.bottom(), 0x40FFFFFF);
        Component exportText = Component.translatable("screen.minedevice.phone.gallery.export");
        float exportScale = Math.min(0.55F, PhoneScreenDraw.textScaleToFit(minecraft.font, exportText, exportButton.width - 4, 0.4F));
        int exportTextWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, exportText, exportScale);
        int exportTextHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, exportScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, exportText,
                exportButton.left + (exportButton.width - exportTextWidth) / 2,
                exportButton.top + (exportButton.height - exportTextHeight) / 2, 0xFFFFFFFF, false, exportScale);

        guiGraphics.fill(viewerLayout.areaX, viewerLayout.areaY,
                viewerLayout.areaX + viewerLayout.areaWidth, viewerLayout.areaY + viewerLayout.areaHeight, 0xFF000000);

        boolean renderedImage = false;
        if (photoEntry.hasFile()) {
            PhotoTexture photoTexture = screen.getOrLoadPhotoTexture(photoEntry.fileName, false);
            if (photoTexture != null) {
                PhoneScreenDraw.drawPhotoInArea(guiGraphics, photoTexture,
                        viewerLayout.areaX, viewerLayout.areaY, viewerLayout.areaWidth, viewerLayout.areaHeight);
                renderedImage = true;
            }
        }

        if (!renderedImage) {
            if (!photoEntry.itemStack.isEmpty()) {
                int itemX = viewerLayout.areaX + (viewerLayout.areaWidth - 16) / 2;
                int itemY = viewerLayout.areaY + (viewerLayout.areaHeight - 16) / 2;
                guiGraphics.renderItem(photoEntry.itemStack, itemX, itemY);
                guiGraphics.renderItemDecorations(minecraft.font, photoEntry.itemStack, itemX, itemY);
            } else {
                Component missingText = Component.translatable("screen.minedevice.phone.viewer.missing");
                int missingMaxWidth = Math.max(28, viewerLayout.areaWidth - Math.round(12 * screen.scale));
                float missingScale = PhoneScreenDraw.textScaleToFit(minecraft.font, missingText, missingMaxWidth);
                int missingWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, missingText, missingScale);
                int missingHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, missingScale);
                int missingX = viewerLayout.areaX + (viewerLayout.areaWidth - missingWidth) / 2;
                int missingY = viewerLayout.areaY + (viewerLayout.areaHeight - missingHeight) / 2;
                PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, missingText, missingX, missingY,
                        0xFF8E8E93, false, missingScale);
            }
        }
    }

    static void renderCameraSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        if (screen.capturePending) {
            return;
        }

        if (screen.isBankScanCameraActive() || screen.isChatScanCameraMode()) {
            return;
        }

        renderCameraStorageWarning(screen, guiGraphics);
        renderCameraZoomIndicator(screen, guiGraphics);

        UiRect previewBounds = screen.getCameraPreviewBounds();
        PhotoEntry latestPhoto = screen.getLatestPhoto();
        boolean renderedPreview = false;
        if (latestPhoto != null && latestPhoto.hasFile()) {
            PhotoTexture previewTexture = screen.getOrLoadPhotoTexture(latestPhoto.fileName, true);
            if (previewTexture != null) {
                guiGraphics.fill(previewBounds.left, previewBounds.top,
                        previewBounds.right(), previewBounds.bottom(), 0xFFE7EBF2);
                boolean rotatePreview = screen.landscapeMode;
                if (rotatePreview) {
                    guiGraphics.pose().pushPose();
                    float centerX = (previewBounds.left + previewBounds.right()) / 2.0F;
                    float centerY = (previewBounds.top + previewBounds.bottom()) / 2.0F;
                    guiGraphics.pose().translate(centerX, centerY, 0.0F);
                    guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
                    guiGraphics.pose().translate(-centerX, -centerY, 0.0F);
                }
                guiGraphics.blit(previewTexture.textureId, previewBounds.left + 1, previewBounds.top + 1,
                        previewBounds.width - 2, previewBounds.height - 2,
                        0.0F, 0.0F, previewTexture.width, previewTexture.height,
                        previewTexture.width, previewTexture.height);
                if (rotatePreview) {
                    guiGraphics.pose().popPose();
                }
                renderedPreview = true;
            }
        }

        if (!renderedPreview) {
            int left = previewBounds.left;
            int top = previewBounds.top;
            int right = previewBounds.right();
            int bottom = previewBounds.bottom();
            guiGraphics.fill(left, top, right, top + 1, 0xFFE7EBF2);
            guiGraphics.fill(left, bottom - 1, right, bottom, 0xFFE7EBF2);
            guiGraphics.fill(left, top, left + 1, bottom, 0xFFE7EBF2);
            guiGraphics.fill(right - 1, top, right, bottom, 0xFFE7EBF2);
        }
    }

    private static void renderCameraStorageWarning(PhoneScreen screen, GuiGraphics guiGraphics) {
        if (!screen.isPhotoStorageFull()) {
            return;
        }

        UiRect shutterBounds = screen.getCameraShutterButtonBounds();
        Minecraft minecraft = Minecraft.getInstance();
        var font = minecraft.font;
        Component warningText = Component.translatable("screen.minedevice.phone.camera.storage_full");
        int maxTextWidth = Math.max(Math.round(90 * screen.scale), shutterBounds.width + Math.round(36 * screen.scale));
        float textScale = PhoneScreenDraw.textScaleToFit(font, warningText, maxTextWidth, 0.45F);
        textScale = Math.min(textScale, 0.9F);
        int textWidth = PhoneScreenDraw.scaledTextWidth(font, warningText, textScale);
        int textHeight = PhoneScreenDraw.scaledTextHeight(font, textScale);
        int textX = shutterBounds.left + (shutterBounds.width - textWidth) / 2;
        int textY = shutterBounds.top - textHeight - Math.max(8, Math.round(10 * screen.scale));

        PhoneScreenDraw.drawScaledText(guiGraphics, font, warningText, textX, textY, 0xFFF7F9FF, true, textScale);
    }

    private static void renderCameraZoomIndicator(PhoneScreen screen, GuiGraphics guiGraphics) {
        if (!screen.shouldShowCameraZoomIndicator()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        var font = minecraft.font;
        Component zoomText = Component.literal(screen.getCameraZoomLabel());
        float textScale = 0.8F;
        int textWidth = PhoneScreenDraw.scaledTextWidth(font, zoomText, textScale);
        int textHeight = PhoneScreenDraw.scaledTextHeight(font, textScale);
        int indicatorPaddingX = Math.max(6, Math.round(7 * screen.scale));
        int indicatorPaddingY = Math.max(4, Math.round(5 * screen.scale));
        int indicatorWidth = textWidth + (indicatorPaddingX * 2);
        int indicatorHeight = textHeight + (indicatorPaddingY * 2) + Math.max(4, Math.round(5 * screen.scale));
        int indicatorX = screen.displayX + (screen.displayWidth - indicatorWidth) / 2;
        int indicatorY = screen.displayY + Math.max(10, Math.round(12 * screen.scale));

        int barInset = Math.max(4, Math.round(5 * screen.scale));
        int barHeight = Math.max(2, Math.round(3 * screen.scale));
        int barY = indicatorY + indicatorHeight - barInset - barHeight;
        int barWidth = Math.max(12, indicatorWidth - (barInset * 2));
        int minZoomLevel = screen.getMinCameraZoomLevel();
        int maxZoomLevel = screen.getMaxCameraZoomLevel();
        float zoomProgress = maxZoomLevel <= minZoomLevel
                ? 1.0F
                : (float) (screen.getCameraZoomLevel() - minZoomLevel) / (float) (maxZoomLevel - minZoomLevel);
        int fillWidth = Math.max(barHeight, Math.round(barWidth * Mth.clamp(zoomProgress, 0.0F, 1.0F)));

        boolean rotateIndicator = screen.landscapeMode;
        if (rotateIndicator) {
            guiGraphics.pose().pushPose();
            float centerX = indicatorX + indicatorWidth / 2.0F;
            float centerY = indicatorY + indicatorHeight / 2.0F;
            guiGraphics.pose().translate(centerX, centerY, 0.0F);
            guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            guiGraphics.pose().translate(-centerX, -centerY, 0.0F);
        }

        guiGraphics.fill(indicatorX, indicatorY, indicatorX + indicatorWidth, indicatorY + indicatorHeight, 0x99000000);
        guiGraphics.fill(indicatorX + barInset, barY, indicatorX + barInset + barWidth, barY + barHeight, 0x33FFFFFF);
        guiGraphics.fill(indicatorX + barInset, barY, indicatorX + barInset + fillWidth, barY + barHeight, 0xFFE7EBF2);

        int textX = indicatorX + (indicatorWidth - textWidth) / 2;
        int textY = indicatorY + indicatorPaddingY;
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, zoomText, textX, textY, 0xFFFFFFFF, true, textScale);

        if (rotateIndicator) {
            guiGraphics.pose().popPose();
        }
    }

    static void renderCameraOverlayHints(PhoneScreen screen, GuiGraphics guiGraphics) {
        int hintX = screen.frameX + screen.frameWidth + Math.max(8, Math.round(10 * screen.scale));
        int hintY = screen.frameY + Math.max(28, Math.round(34 * screen.scale));
        int hintWidth = Math.max(48, screen.width - hintX - Math.max(4, Math.round(6 * screen.scale)));
        Component dragHint = Component.translatable("screen.minedevice.phone.camera.hint_drag");
        Component zoomHint = Component.translatable("screen.minedevice.phone.camera.hint_zoom");
        Component captureHint = Component.translatable("screen.minedevice.phone.camera.hint_capture");
        Component exitHint = Component.translatable("screen.minedevice.phone.camera.hint_exit");
        Minecraft minecraft = Minecraft.getInstance();
        var font = minecraft.font;
        float dragHintScale = PhoneScreenDraw.textScaleToFit(font, dragHint, hintWidth);
        float zoomHintScale = PhoneScreenDraw.textScaleToFit(font, zoomHint, hintWidth);
        float captureHintScale = PhoneScreenDraw.textScaleToFit(font, captureHint, hintWidth);
        float exitHintScale = PhoneScreenDraw.textScaleToFit(font, exitHint, hintWidth);
        int hintGap = Math.max(1, Math.round(2 * screen.scale));
        int zoomHintY = hintY + PhoneScreenDraw.scaledTextHeight(font, dragHintScale) + hintGap;
        int captureHintY = zoomHintY + PhoneScreenDraw.scaledTextHeight(font, zoomHintScale) + hintGap;
        int exitHintY = captureHintY + PhoneScreenDraw.scaledTextHeight(font, captureHintScale) + hintGap;

        PhoneScreenDraw.drawScaledText(guiGraphics, font, dragHint, hintX, hintY, 0xFFF7F9FF, true, dragHintScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, zoomHint, hintX, zoomHintY, 0xFFF7F9FF, true, zoomHintScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, captureHint, hintX, captureHintY, 0xFFF7F9FF, true, captureHintScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, exitHint, hintX, exitHintY, 0xFFF7F9FF, true, exitHintScale);
    }
}
