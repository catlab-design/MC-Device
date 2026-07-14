package com.sammy.minedevice.client.phone;

import com.sammy.minedevice.phone.PhoneAppearanceData;
import com.sammy.minedevice.phone.PhoneWallpaperData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Settings app surface, styled after a real phone's settings app: grouped white
 * lists of compact rows with colored icon squares, current values in gray on the
 * right, chevrons, and small uppercase section labels. Top level is a category
 * menu (Appearance / System); Appearance nests the wallpaper and lock-color
 * pickers, System holds the clock source/format rows (tap a row to toggle).
 */
final class PhoneSettingsSurfaceRenderer {
    private static final int PAGE_FILL = 0xFFF2F2F7;
    private static final int HEADER_FILL = 0xFF1E293B;
    private static final int CARD_FILL = 0xFFFFFFFF;
    private static final int CARD_HOVER = 0xFFEEF2FF;
    private static final int ROW_DIVIDER = 0xFFE5E5EA;
    private static final int CARD_EDGE = 0xFFE2E8F0;
    private static final int THUMB_FILL = 0xFF1F2937;
    private static final int SELECT_RING = 0xFF3B82F6;

    private static final int TEXT_PRIMARY = 0xFF111827;
    private static final int TEXT_SECONDARY = 0xFF64748B;
    private static final int TEXT_VALUE = 0xFF8E8E93;
    private static final int TEXT_CHEVRON = 0xFFC7C7CC;
    private static final int TEXT_HEADER = 0xFFFFFFFF;

    // Category-menu icon colors (initials shown only there).
    private static final int ICON_APPEARANCE = 0xFF8B5CF6;
    private static final int ICON_SYSTEM = 0xFF64748B;

    // Built-in wallpaper textures are 144x266.
    private static final int WALLPAPER_TEX_W = 144;
    private static final int WALLPAPER_TEX_H = 266;
    private static final int GRID_COLUMNS = 2;
    private static final int COLOR_GRID_COLUMNS = 4;

    private PhoneSettingsSurfaceRenderer() {
    }

    static void renderSettingsSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        renderSettingsSurface(screen, guiGraphics, -1, -1);
    }

    static void renderSettingsSurface(PhoneScreen screen, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        Font font = minecraft.font;
        UiRect content = screen.getMediaSurfaceBounds();

        // Background + header band (bleed slightly so it reaches the phone frame).
        int headerBleed = Math.max(4, Math.round(6 * screen.scale));
        int headerLeft = content.left - headerBleed;
        int headerRight = content.right() + headerBleed;
        guiGraphics.fill(headerLeft, content.top, headerRight, content.bottom(), PAGE_FILL);

        int headerHeight = headerHeight(screen);
        int headerBottom = content.top + headerHeight;
        guiGraphics.fill(headerLeft, content.top, headerRight, headerBottom, HEADER_FILL);

        boolean showBack = screen.settingsPage != 0;
        boolean showImport = screen.settingsPage == 1 && !screen.settingsColorPickerOpen && screen.settingsWallpaperPage != 0;

        int titleLeft = content.left + Math.max(6, Math.round(10 * screen.scale));
        if (showBack) {
            UiRect back = backButtonBounds(screen);
            drawFitted(guiGraphics, font, Component.literal("<"), back.left, back.top + (back.height - lineH(font, 0.6F)) / 2,
                    back.width, TEXT_HEADER, 0.6F);
            titleLeft = back.right() + Math.max(3, Math.round(4 * screen.scale));
        }
        int titleRightLimit = headerRight - headerBleed;
        if (showImport) {
            UiRect importBtn = importButtonBounds(screen);
            boolean hover = importBtn.contains(mouseX, mouseY);
            guiGraphics.fill(importBtn.left, importBtn.top, importBtn.right(), importBtn.bottom(),
                    hover ? 0xFF3B82F6 : 0x33FFFFFF);
            drawFittedCentered(guiGraphics, font,
                    Component.translatable("screen.minedevice.phone.settings.wallpaper.from_file"),
                    importBtn.left + importBtn.width / 2,
                    importBtn.top + (importBtn.height - lineH(font, 0.55F)) / 2, importBtn.width - 4, TEXT_HEADER, 0.55F);
            titleRightLimit = importBtn.left - Math.max(3, Math.round(4 * screen.scale));
        }
        int titleY = content.top + Math.max(1, (headerHeight - lineH(font, 0.8F)) / 2);
        drawFittedMin(guiGraphics, font, resolveTitle(screen).copy().withStyle(s -> s.withBold(true)), titleLeft, titleY,
                titleRightLimit - titleLeft, TEXT_HEADER, 0.8F, 0.55F);

        if (screen.settingsPage == 0) {
            renderCategoryMenu(screen, guiGraphics, font, mouseX, mouseY);
        } else if (screen.settingsPage == 2) {
            renderSystemPage(screen, guiGraphics, font, mouseX, mouseY);
        } else if (screen.settingsColorPickerOpen) {
            renderColorPickerPage(screen, guiGraphics, mouseX, mouseY);
        } else if (screen.settingsWallpaperPage != 0) {
            renderWallpaperPickerPage(screen, guiGraphics, mouseX, mouseY);
        } else {
            renderAppearancePage(screen, guiGraphics, font, mouseX, mouseY);
        }
    }

    private static Component resolveTitle(PhoneScreen screen) {
        if (screen.settingsPage == 0) {
            return Component.translatable("screen.minedevice.phone.title.settings");
        }
        if (screen.settingsPage == 2) {
            return Component.translatable("screen.minedevice.phone.settings.category.system");
        }
        if (screen.settingsColorPickerOpen) {
            return Component.translatable("screen.minedevice.phone.settings.appearance.lock_color");
        }
        if (screen.settingsWallpaperPage == 1) {
            return Component.translatable("screen.minedevice.phone.settings.wallpaper.lock");
        }
        if (screen.settingsWallpaperPage == 2) {
            return Component.translatable("screen.minedevice.phone.settings.wallpaper.home");
        }
        return Component.translatable("screen.minedevice.phone.settings.category.appearance");
    }

    // ── Category menu (top level) ────────────────────────────────────────────────

    private static void renderCategoryMenu(PhoneScreen screen, GuiGraphics guiGraphics, Font font,
                                           double mouseX, double mouseY) {
        renderProfileCard(screen, guiGraphics, font, mouseX, mouseY);

        UiRect row0 = categoryRowBounds(screen, 0);
        UiRect row1 = categoryRowBounds(screen, 1);
        renderGroupRow(screen, guiGraphics, font, row0, ICON_APPEARANCE,
                Component.translatable("screen.minedevice.phone.settings.category.appearance"),
                null, false, mouseX, mouseY);
        renderGroupRow(screen, guiGraphics, font, row1, ICON_SYSTEM,
                Component.translatable("screen.minedevice.phone.settings.category.system"),
                null, true, mouseX, mouseY);
    }

    /** Big card at the top of the settings menu: the player's head + display name, tap to rename. */
    private static void renderProfileCard(PhoneScreen screen, GuiGraphics guiGraphics, Font font,
                                          double mouseX, double mouseY) {
        UiRect card = profileCardBounds(screen);
        boolean hover = !screen.settingsNameEditMode && card.contains(mouseX, mouseY);
        guiGraphics.fill(card.left, card.top, card.right(), card.bottom(), hover ? CARD_HOVER : CARD_FILL);
        guiGraphics.fill(card.left, card.top, card.right(), card.top + 1, CARD_EDGE);
        guiGraphics.fill(card.left, card.bottom() - 1, card.right(), card.bottom(), CARD_EDGE);

        int inset = Math.max(5, Math.round(7 * screen.scale));
        int headSize = card.height - (inset * 2);
        int headLeft = card.left + inset;
        int headTop = card.top + inset;
        guiGraphics.fill(headLeft - 1, headTop - 1, headLeft + headSize + 1, headTop + headSize + 1, CARD_EDGE);
        net.minecraft.client.gui.components.PlayerFaceRenderer.draw(
                guiGraphics, screen.getOwnSkinTexture(), headLeft, headTop, headSize);

        int textLeft = headLeft + headSize + inset;
        int textRight = card.right() - inset;
        int textWidth = Math.max(10, textRight - textLeft);

        if (screen.settingsNameEditMode) {
            int fieldTop = card.top + (card.height - Math.max(12, Math.round(14 * screen.scale))) / 2;
            int fieldHeight = Math.max(12, Math.round(14 * screen.scale));
            guiGraphics.fill(textLeft, fieldTop, textRight, fieldTop + fieldHeight, 0xFFF1F5F9);
            guiGraphics.fill(textLeft, fieldTop, textRight, fieldTop + 1, SELECT_RING);
            guiGraphics.fill(textLeft, fieldTop + fieldHeight - 1, textRight, fieldTop + fieldHeight, SELECT_RING);
            Component draft = Component.literal(screen.settingsNameDraft + "_");
            drawFittedMin(guiGraphics, font, draft, textLeft + Math.max(2, Math.round(3 * screen.scale)),
                    fieldTop + (fieldHeight - lineH(font, 0.7F)) / 2, textWidth - 4, TEXT_PRIMARY, 0.7F, 0.5F);
        } else {
            int labelY = card.top + Math.max(5, Math.round(7 * screen.scale));
            drawFittedMin(guiGraphics, font, Component.literal(screen.getOwnDisplayName()).copy().withStyle(s -> s.withBold(true)),
                    textLeft, labelY, textWidth, TEXT_PRIMARY, 0.8F, 0.5F);
            drawFittedMin(guiGraphics, font, Component.translatable("screen.minedevice.phone.settings.profile.tap_rename"),
                    textLeft, labelY + lineH(font, 0.8F) + Math.max(2, Math.round(2 * screen.scale)),
                    textWidth, TEXT_VALUE, 0.6F, 0.45F);
        }
    }

    // ── Appearance page ──────────────────────────────────────────────────────────

    // Appearance keeps the original card design (large preview + "Tap to change"),
    // not the compact iOS rows used by the category menu.
    private static void renderAppearancePage(PhoneScreen screen, GuiGraphics guiGraphics, Font font,
                                             double mouseX, double mouseY) {
        renderChoiceCard(screen, guiGraphics, font, appearanceCardBounds(screen, 0),
                Component.translatable("screen.minedevice.phone.settings.wallpaper.lock"), true, mouseX, mouseY);
        renderChoiceCard(screen, guiGraphics, font, appearanceCardBounds(screen, 1),
                Component.translatable("screen.minedevice.phone.settings.wallpaper.home"), false, mouseX, mouseY);
        renderLockColorCard(screen, guiGraphics, font, appearanceCardBounds(screen, 2), mouseX, mouseY);
    }

    private static void renderChoiceCard(PhoneScreen screen, GuiGraphics guiGraphics, Font font, UiRect card,
                                         Component label, boolean lockScreen, double mouseX, double mouseY) {
        boolean hover = card.contains(mouseX, mouseY);
        renderCard(guiGraphics, card, hover);

        int inset = Math.max(4, Math.round(6 * screen.scale));
        int thumbH = card.height - (inset * 2);
        int thumbW = Math.round(thumbH * (float) WALLPAPER_TEX_W / WALLPAPER_TEX_H);
        UiRect thumb = new UiRect(card.left + inset, card.top + inset, thumbW, thumbH);
        drawWallpaperPreview(screen, guiGraphics, thumb, screen.getWallpaperKey(lockScreen));

        int textLeft = thumb.right() + Math.max(6, Math.round(8 * screen.scale));
        int textWidth = card.right() - textLeft - inset;
        int labelY = card.top + Math.max(6, Math.round(10 * screen.scale));
        drawFittedMin(guiGraphics, font, label.copy().withStyle(s -> s.withBold(true)), textLeft, labelY,
                textWidth, TEXT_PRIMARY, 0.9F, 0.5F);
        drawFittedMin(guiGraphics, font, Component.translatable("screen.minedevice.phone.settings.wallpaper.tap_change"),
                textLeft, labelY + lineH(font, 0.9F) + Math.max(2, Math.round(3 * screen.scale)),
                textWidth, TEXT_SECONDARY, 0.7F, 0.45F);
    }

    private static void renderLockColorCard(PhoneScreen screen, GuiGraphics guiGraphics, Font font, UiRect card,
                                            double mouseX, double mouseY) {
        boolean hover = card.contains(mouseX, mouseY);
        renderCard(guiGraphics, card, hover);

        int inset = Math.max(4, Math.round(6 * screen.scale));
        // Cap the swatch so a tall card doesn't make a huge square that eats the text width.
        int swatchSize = Math.min(card.height - (inset * 2), Math.max(20, Math.round(26 * screen.scale)));
        int swatchLeft = card.left + inset;
        int swatchTop = card.top + (card.height - swatchSize) / 2;
        int currentColor = screen.getLockAccentColor();
        guiGraphics.fill(swatchLeft - 1, swatchTop - 1, swatchLeft + swatchSize + 1, swatchTop + swatchSize + 1, CARD_EDGE);
        guiGraphics.fill(swatchLeft, swatchTop, swatchLeft + swatchSize, swatchTop + swatchSize, currentColor | 0xFF000000);

        int textLeft = swatchLeft + swatchSize + Math.max(6, Math.round(8 * screen.scale));
        int textWidth = card.right() - textLeft - inset;
        int labelY = card.top + Math.max(6, Math.round(10 * screen.scale));
        drawFittedMin(guiGraphics, font,
                Component.translatable("screen.minedevice.phone.settings.appearance.lock_color").copy().withStyle(s -> s.withBold(true)),
                textLeft, labelY, textWidth, TEXT_PRIMARY, 0.9F, 0.5F);
        drawFittedMin(guiGraphics, font, Component.translatable("screen.minedevice.phone.settings.wallpaper.tap_change"),
                textLeft, labelY + lineH(font, 0.9F) + Math.max(2, Math.round(3 * screen.scale)),
                textWidth, TEXT_SECONDARY, 0.7F, 0.45F);
    }

    private static void renderCard(GuiGraphics guiGraphics, UiRect card, boolean hover) {
        guiGraphics.fill(card.left, card.top, card.right(), card.bottom(), hover ? CARD_HOVER : CARD_FILL);
        guiGraphics.fill(card.left, card.top, card.right(), card.top + 1, CARD_EDGE);
        guiGraphics.fill(card.left, card.bottom() - 1, card.right(), card.bottom(), CARD_EDGE);
        guiGraphics.fill(card.left, card.top, card.left + 1, card.bottom(), CARD_EDGE);
        guiGraphics.fill(card.right() - 1, card.top, card.right(), card.bottom(), CARD_EDGE);
    }

    // ── System page ──────────────────────────────────────────────────────────────

    // System rows are icon-less (initials belong only on the category menu) and stack
    // the label over its current value so nothing gets truncated on the narrow screen.
    private static void renderSystemPage(PhoneScreen screen, GuiGraphics guiGraphics, Font font,
                                         double mouseX, double mouseY) {
        Component sourceValue = screen.isRealLifeTime()
                ? Component.translatable("screen.minedevice.phone.settings.system.time_source.real")
                : Component.translatable("screen.minedevice.phone.settings.system.time_source.game");
        Component formatValue = screen.is12HourTimeFormat()
                ? Component.translatable("screen.minedevice.phone.settings.system.time_format.12h")
                : Component.translatable("screen.minedevice.phone.settings.system.time_format.24h");
        renderStackedRow(screen, guiGraphics, font, systemRowBounds(screen, 0),
                Component.translatable("screen.minedevice.phone.settings.system.time_source"),
                sourceValue, false, mouseX, mouseY);
        renderStackedRow(screen, guiGraphics, font, systemRowBounds(screen, 1),
                Component.translatable("screen.minedevice.phone.settings.system.time_format"),
                formatValue, true, mouseX, mouseY);
    }

    /** Icon-less two-line row: bold label over a gray current value, chevron on the right. */
    private static void renderStackedRow(PhoneScreen screen, GuiGraphics guiGraphics, Font font, UiRect row,
                                         Component label, Component value, boolean lastInGroup,
                                         double mouseX, double mouseY) {
        boolean hover = row.contains(mouseX, mouseY);
        guiGraphics.fill(row.left, row.top, row.right(), row.bottom(), hover ? CARD_HOVER : CARD_FILL);

        int inset = Math.max(6, Math.round(8 * screen.scale));
        int chevronReserve = Math.max(9, Math.round(11 * screen.scale));
        drawFittedCentered(guiGraphics, font, Component.literal(">"),
                row.right() - inset - chevronReserve / 2,
                row.top + (row.height - lineH(font, 0.7F)) / 2, chevronReserve, TEXT_CHEVRON, 0.7F);

        int textLeft = row.left + inset;
        int textWidth = row.right() - inset - chevronReserve - textLeft;
        int labelY = row.top + Math.max(4, Math.round(5 * screen.scale));
        drawFittedMin(guiGraphics, font, label.copy().withStyle(s -> s.withBold(true)), textLeft, labelY,
                textWidth, TEXT_PRIMARY, 0.75F, 0.55F);
        drawFittedMin(guiGraphics, font, value, textLeft,
                labelY + lineH(font, 0.75F) + Math.max(1, Math.round(2 * screen.scale)),
                textWidth, TEXT_VALUE, 0.65F, 0.5F);

        if (!lastInGroup) {
            guiGraphics.fill(textLeft, row.bottom() - 1, row.right(), row.bottom(), ROW_DIVIDER);
        }
    }

    // ── iOS-style row rendering ───────────────────────────────────────────────────

    /**
     * One row of a grouped settings list: white fill, colored icon square with the
     * label's initial, label, optional gray value text before a chevron, and an
     * inset divider under every row but the group's last.
     */
    private static void renderGroupRow(PhoneScreen screen, GuiGraphics guiGraphics, Font font, UiRect row,
                                       int iconColor, Component label, Component valueText, boolean lastInGroup,
                                       double mouseX, double mouseY) {
        boolean hover = row.contains(mouseX, mouseY);
        guiGraphics.fill(row.left, row.top, row.right(), row.bottom(), hover ? CARD_HOVER : CARD_FILL);

        int inset = Math.max(5, Math.round(7 * screen.scale));
        int iconSize = Math.max(12, row.height - Math.max(8, Math.round(10 * screen.scale)));
        int iconLeft = row.left + inset;
        int iconTop = row.top + (row.height - iconSize) / 2;
        guiGraphics.fill(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize, iconColor);
        String labelString = label.getString();
        Component initial = Component.literal(labelString.isEmpty() ? "?"
                : labelString.substring(0, 1).toUpperCase(Locale.ROOT)).copy().withStyle(s -> s.withBold(true));
        drawFittedCentered(guiGraphics, font, initial, iconLeft + iconSize / 2,
                iconTop + (iconSize - lineH(font, 0.7F)) / 2, iconSize - 2, 0xFFFFFFFF, 0.7F);

        int textLeft = iconLeft + iconSize + inset;

        // Chevron on the far right, value text (if any) just before it.
        int chevronReserve = Math.max(9, Math.round(11 * screen.scale));
        drawFittedCentered(guiGraphics, font, Component.literal(">"),
                row.right() - inset - chevronReserve / 2,
                row.top + (row.height - lineH(font, 0.7F)) / 2, chevronReserve, TEXT_CHEVRON, 0.7F);
        int valueRight = row.right() - inset - chevronReserve - Math.max(2, Math.round(3 * screen.scale));

        int labelRightLimit = valueRight;
        if (valueText != null) {
            float valueScale = 0.7F;
            int valueWidth = Math.min(PhoneScreenDraw.scaledTextWidth(font, valueText, valueScale),
                    Math.max(20, (valueRight - textLeft) / 2));
            drawFittedMin(guiGraphics, font, valueText, valueRight - valueWidth,
                    row.top + (row.height - lineH(font, valueScale)) / 2,
                    valueWidth, TEXT_VALUE, valueScale, 0.5F);
            labelRightLimit = valueRight - valueWidth - Math.max(3, Math.round(4 * screen.scale));
        }

        drawFittedMin(guiGraphics, font, label, textLeft,
                row.top + (row.height - lineH(font, 0.8F)) / 2,
                labelRightLimit - textLeft, TEXT_PRIMARY, 0.8F, 0.55F);

        if (!lastInGroup) {
            // Inset divider starting after the icon, like a real settings list.
            guiGraphics.fill(textLeft, row.bottom() - 1, row.right(), row.bottom(), ROW_DIVIDER);
        }
    }

    // ── Lock-screen accent color picker ──────────────────────────────────────────

    private static void renderColorPickerPage(PhoneScreen screen, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        int currentColor = screen.getLockAccentColor();
        int[] choices = PhoneAppearanceData.ACCENT_COLOR_CHOICES;
        int ringBleed = 3;
        for (int i = 0; i < choices.length; i++) {
            UiRect cell = colorCellBounds(screen, i);
            boolean selected = (choices[i] | 0xFF000000) == (currentColor | 0xFF000000);
            boolean hover = cell.contains(mouseX, mouseY);
            if (selected || hover) {
                int ring = selected ? SELECT_RING : CARD_EDGE;
                guiGraphics.fill(cell.left - ringBleed, cell.top - ringBleed, cell.right() + ringBleed, cell.bottom() + ringBleed, ring);
            }
            guiGraphics.fill(cell.left, cell.top, cell.right(), cell.bottom(), ROW_DIVIDER);
            guiGraphics.fill(cell.left + 1, cell.top + 1, cell.right() - 1, cell.bottom() - 1, choices[i] | 0xFF000000);
        }
    }

    // ── Wallpaper picker (lock/home) ──────────────────────────────────────────────

    private static void renderWallpaperPickerPage(PhoneScreen screen, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        UiRect grid = gridBounds(screen);
        String currentKey = screen.getWallpaperKey(screen.settingsWallpaperPage == 1);
        int count = optionCount(screen);
        // The selection ring bleeds 2px past each cell; widen the scissor by the same
        // amount so the ring on edge cells (e.g. top-left) isn't clipped away.
        int ringBleed = 2;

        guiGraphics.enableScissor(grid.left - ringBleed, grid.top - ringBleed, grid.right() + ringBleed, grid.bottom() + ringBleed);
        for (int i = 0; i < count; i++) {
            UiRect cell = optionCellBounds(screen, i);
            if (cell.bottom() < grid.top || cell.top > grid.bottom()) {
                continue;
            }
            boolean selected = Objects.equals(currentKey, optionKey(screen, i));
            boolean hover = cell.contains(mouseX, mouseY);
            if (selected || hover) {
                int ring = selected ? SELECT_RING : CARD_EDGE;
                guiGraphics.fill(cell.left - ringBleed, cell.top - ringBleed, cell.right() + ringBleed, cell.bottom() + ringBleed, ring);
            }
            drawOptionThumb(screen, guiGraphics, cell, i);
        }
        guiGraphics.disableScissor();
    }

    private static void drawOptionThumb(PhoneScreen screen, GuiGraphics guiGraphics, UiRect cell, int index) {
        guiGraphics.fill(cell.left, cell.top, cell.right(), cell.bottom(), THUMB_FILL);
        int builtins = PhoneWallpaperData.BUILTIN_WALLPAPERS.length;
        if (index < builtins) {
            ResourceLocation tex = PhoneWallpaperData.resolveBuiltinTexture(PhoneWallpaperData.builtinKey(index));
            PhoneScreenDraw.drawTextureInArea(guiGraphics, tex, WALLPAPER_TEX_W, WALLPAPER_TEX_H,
                    cell.left, cell.top, cell.width, cell.height, 1.0F, false);
        } else {
            PhotoEntry photo = screen.getPhotos().get(index - builtins);
            if (photo.hasFile()) {
                PhotoTexture texture = screen.getOrLoadPhotoTexture(photo.fileName, true);
                if (texture != null) {
                    PhoneScreenDraw.drawTextureInArea(guiGraphics, texture.textureId, texture.width, texture.height,
                            cell.left, cell.top, cell.width, cell.height, 1.0F, false);
                }
            }
        }
    }

    private static void drawWallpaperPreview(PhoneScreen screen, GuiGraphics guiGraphics, UiRect area, String key) {
        guiGraphics.fill(area.left, area.top, area.right(), area.bottom(), THUMB_FILL);
        if (PhoneWallpaperData.isBuiltin(key)) {
            PhoneScreenDraw.drawTextureInArea(guiGraphics, PhoneWallpaperData.resolveBuiltinTexture(key),
                    WALLPAPER_TEX_W, WALLPAPER_TEX_H, area.left, area.top, area.width, area.height, 1.0F, false);
        } else {
            String fileName = PhoneWallpaperData.getPhotoFileName(key);
            if (fileName != null) {
                PhotoTexture texture = screen.getOrLoadPhotoTexture(fileName, true);
                if (texture != null) {
                    PhoneScreenDraw.drawTextureInArea(guiGraphics, texture.textureId, texture.width, texture.height,
                            area.left, area.top, area.width, area.height, 1.0F, false);
                }
            }
        }
    }

    // ── Click handling ─────────────────────────────────────────────────────────

    static boolean handleClick(PhoneScreen screen, double mouseX, double mouseY) {
        if (screen.settingsPage != 0 && backButtonBounds(screen).contains(mouseX, mouseY)) {
            screen.settingsGoBack();
            return true;
        }

        if (screen.settingsPage == 0) {
            return handleCategoryMenuClick(screen, mouseX, mouseY);
        }
        if (screen.settingsPage == 2) {
            return handleSystemClick(screen, mouseX, mouseY);
        }
        // settingsPage == 1 (Appearance)
        if (screen.settingsColorPickerOpen) {
            return handleColorPickerClick(screen, mouseX, mouseY);
        }
        if (screen.settingsWallpaperPage != 0) {
            return handleWallpaperPickerClick(screen, mouseX, mouseY);
        }
        return handleAppearanceClick(screen, mouseX, mouseY);
    }

    private static boolean handleCategoryMenuClick(PhoneScreen screen, double mouseX, double mouseY) {
        boolean onProfile = profileCardBounds(screen).contains(mouseX, mouseY);
        if (screen.settingsNameEditMode) {
            // Any click while editing commits the current name (tap the card again = save).
            screen.commitSettingsNameEdit();
            return true;
        }
        if (onProfile) {
            screen.beginSettingsNameEdit();
            return true;
        }
        if (categoryRowBounds(screen, 0).contains(mouseX, mouseY)) {
            screen.settingsPage = 1;
            screen.rebuildWidgets();
            return true;
        }
        if (categoryRowBounds(screen, 1).contains(mouseX, mouseY)) {
            screen.settingsPage = 2;
            screen.rebuildWidgets();
            return true;
        }
        return false;
    }

    private static boolean handleAppearanceClick(PhoneScreen screen, double mouseX, double mouseY) {
        if (appearanceCardBounds(screen, 0).contains(mouseX, mouseY)) {
            screen.settingsWallpaperPage = 1;
            screen.settingsWallpaperScroll = 0;
            screen.rebuildWidgets();
            return true;
        }
        if (appearanceCardBounds(screen, 1).contains(mouseX, mouseY)) {
            screen.settingsWallpaperPage = 2;
            screen.settingsWallpaperScroll = 0;
            screen.rebuildWidgets();
            return true;
        }
        if (appearanceCardBounds(screen, 2).contains(mouseX, mouseY)) {
            screen.settingsColorPickerOpen = true;
            screen.rebuildWidgets();
            return true;
        }
        return false;
    }

    private static boolean handleColorPickerClick(PhoneScreen screen, double mouseX, double mouseY) {
        int[] choices = PhoneAppearanceData.ACCENT_COLOR_CHOICES;
        for (int i = 0; i < choices.length; i++) {
            if (colorCellBounds(screen, i).contains(mouseX, mouseY)) {
                screen.setLockAccentColor(choices[i]);
                screen.settingsColorPickerOpen = false;
                screen.rebuildWidgets();
                return true;
            }
        }
        return false;
    }

    private static boolean handleWallpaperPickerClick(PhoneScreen screen, double mouseX, double mouseY) {
        if (importButtonBounds(screen).contains(mouseX, mouseY)) {
            screen.importWallpaperViaDialog();
            return true;
        }

        UiRect grid = gridBounds(screen);
        if (!grid.contains(mouseX, mouseY)) {
            return false;
        }
        boolean lock = screen.settingsWallpaperPage == 1;
        int count = optionCount(screen);
        int builtins = PhoneWallpaperData.BUILTIN_WALLPAPERS.length;
        for (int i = 0; i < count; i++) {
            if (!optionCellBounds(screen, i).contains(mouseX, mouseY)) {
                continue;
            }
            if (i < builtins) {
                screen.setWallpaperBuiltin(PhoneWallpaperData.BUILTIN_WALLPAPERS[i], lock, !lock);
            } else {
                screen.setWallpaperFromPhoto(screen.getPhotos().get(i - builtins).fileName, lock, !lock);
            }
            return true;
        }
        return false;
    }

    private static boolean handleSystemClick(PhoneScreen screen, double mouseX, double mouseY) {
        // Tapping a row toggles between its two values, like a compact settings list.
        if (systemRowBounds(screen, 0).contains(mouseX, mouseY)) {
            screen.setRealLifeTime(!screen.isRealLifeTime());
            screen.rebuildWidgets();
            return true;
        }
        if (systemRowBounds(screen, 1).contains(mouseX, mouseY)) {
            screen.set12HourTimeFormat(!screen.is12HourTimeFormat());
            screen.rebuildWidgets();
            return true;
        }
        return false;
    }

    // ── Geometry ───────────────────────────────────────────────────────────────

    private static int headerHeight(PhoneScreen screen) {
        return Math.max(28, Math.round(34 * screen.scale));
    }

    private static int rowHeight(PhoneScreen screen) {
        return Math.max(24, Math.round(28 * screen.scale));
    }

    private static int groupSidePad(PhoneScreen screen) {
        return Math.max(2, Math.round(3 * screen.scale));
    }


    private static UiRect importButtonBounds(PhoneScreen screen) {
        // Same size as the gallery/viewer's Import/Save buttons.
        UiRect content = screen.getMediaSurfaceBounds();
        int height = Math.max(11, Math.round(13 * screen.scale));
        int width = Math.max(26, Math.round(32 * screen.scale));
        int top = content.top + (headerHeight(screen) - height) / 2;
        int right = content.right() - Math.max(4, Math.round(6 * screen.scale));
        return new UiRect(right - width, top, width, height);
    }

    private static UiRect backButtonBounds(PhoneScreen screen) {
        UiRect content = screen.getMediaSurfaceBounds();
        int size = Math.max(14, Math.round(18 * screen.scale));
        int top = content.top + (headerHeight(screen) - size) / 2;
        return new UiRect(content.left + Math.max(4, Math.round(6 * screen.scale)), top, size, size);
    }

    private static UiRect profileCardBounds(PhoneScreen screen) {
        UiRect content = screen.getMediaSurfaceBounds();
        int pad = groupSidePad(screen);
        int top = content.top + headerHeight(screen) + Math.max(3, Math.round(5 * screen.scale));
        int height = Math.max(28, Math.round(34 * screen.scale));
        return new UiRect(content.left + pad, top, content.width - (pad * 2), height);
    }

    private static UiRect categoryRowBounds(PhoneScreen screen, int index) {
        UiRect profile = profileCardBounds(screen);
        UiRect content = screen.getMediaSurfaceBounds();
        int pad = groupSidePad(screen);
        int top = profile.bottom() + Math.max(4, Math.round(6 * screen.scale));
        int rowH = rowHeight(screen);
        return new UiRect(content.left + pad, top + index * rowH, content.width - (pad * 2), rowH);
    }

    /** Three compact cards (Lock wallpaper, Home wallpaper, Lock color) stacked from the top. */
    private static UiRect appearanceCardBounds(PhoneScreen screen, int index) {
        UiRect content = screen.getMediaSurfaceBounds();
        int pad = groupSidePad(screen);
        int top = content.top + headerHeight(screen) + Math.max(3, Math.round(5 * screen.scale));
        int gap = Math.max(4, Math.round(6 * screen.scale));
        int cardHeight = Math.max(40, Math.round(48 * screen.scale));
        return new UiRect(content.left + pad, top + index * (cardHeight + gap),
                content.width - (pad * 2), cardHeight);
    }

    private static UiRect systemRowBounds(PhoneScreen screen, int index) {
        UiRect content = screen.getMediaSurfaceBounds();
        int pad = groupSidePad(screen);
        // Taller than a menu row to fit the stacked label + value.
        int rowH = Math.max(30, Math.round(36 * screen.scale));
        int groupTop = content.top + headerHeight(screen) + Math.max(3, Math.round(5 * screen.scale));
        return new UiRect(content.left + pad, groupTop + index * rowH, content.width - (pad * 2), rowH);
    }

    private static UiRect colorGridBounds(PhoneScreen screen) {
        UiRect content = screen.getMediaSurfaceBounds();
        int pad = groupSidePad(screen);
        int top = content.top + headerHeight(screen) + Math.max(3, Math.round(5 * screen.scale));
        int bottom = content.bottom() - Math.max(6, Math.round(8 * screen.scale));
        return new UiRect(content.left + pad, top, content.width - (pad * 2), Math.max(24, bottom - top));
    }

    private static UiRect colorCellBounds(PhoneScreen screen, int index) {
        UiRect grid = colorGridBounds(screen);
        int cellGap = Math.max(5, Math.round(7 * screen.scale));
        int cellSize = (grid.width - cellGap * (COLOR_GRID_COLUMNS - 1)) / COLOR_GRID_COLUMNS;
        int column = index % COLOR_GRID_COLUMNS;
        int row = index / COLOR_GRID_COLUMNS;
        int x = grid.left + column * (cellSize + cellGap);
        int y = grid.top + row * (cellSize + cellGap);
        return new UiRect(x, y, cellSize, cellSize);
    }

    private static int cellWidth(PhoneScreen screen) {
        UiRect grid = gridBounds(screen);
        int cellGap = Math.max(4, Math.round(6 * screen.scale));
        return (grid.width - (cellGap * (GRID_COLUMNS - 1))) / GRID_COLUMNS;
    }

    private static int cellHeight(PhoneScreen screen) {
        return Math.round(cellWidth(screen) * 1.35F);
    }

    private static UiRect gridBounds(PhoneScreen screen) {
        UiRect content = screen.getMediaSurfaceBounds();
        int pad = groupSidePad(screen);
        int top = content.top + headerHeight(screen) + Math.max(3, Math.round(5 * screen.scale));
        int bottom = content.bottom() - Math.max(8, Math.round(10 * screen.scale));
        return new UiRect(content.left + pad, top, content.width - (pad * 2), Math.max(24, bottom - top));
    }

    private static UiRect optionCellBounds(PhoneScreen screen, int index) {
        UiRect grid = gridBounds(screen);
        int cellGap = Math.max(4, Math.round(6 * screen.scale));
        int cellW = cellWidth(screen);
        int cellH = cellHeight(screen);
        int column = index % GRID_COLUMNS;
        int row = index / GRID_COLUMNS;
        int x = grid.left + column * (cellW + cellGap);
        int y = grid.top + row * (cellH + cellGap) - screen.settingsWallpaperScroll;
        return new UiRect(x, y, cellW, cellH);
    }

    static int getMaxWallpaperScroll(PhoneScreen screen) {
        int count = optionCount(screen);
        int rows = (count + GRID_COLUMNS - 1) / GRID_COLUMNS;
        int cellGap = Math.max(4, Math.round(6 * screen.scale));
        int totalHeight = rows * cellHeight(screen) + Math.max(0, rows - 1) * cellGap;
        return Math.max(0, totalHeight - gridBounds(screen).height);
    }

    private static int optionCount(PhoneScreen screen) {
        return PhoneWallpaperData.BUILTIN_WALLPAPERS.length + screen.getPhotos().size();
    }

    private static String optionKey(PhoneScreen screen, int index) {
        int builtins = PhoneWallpaperData.BUILTIN_WALLPAPERS.length;
        if (index < builtins) {
            return PhoneWallpaperData.builtinKey(index);
        }
        List<PhotoEntry> photos = screen.getPhotos();
        return PhoneWallpaperData.photoKey(photos.get(index - builtins).fileName);
    }

    // ── Drawing helpers ──────────────────────────────────────────────────────────

    private static int lineH(Font font, float scale) {
        return Math.max(1, Math.round(font.lineHeight * scale));
    }

    private static void drawFitted(GuiGraphics guiGraphics, Font font, Component text, int x, int y,
                                   int maxWidth, int color, float maxScale) {
        float textScale = Math.min(maxScale, PhoneScreenDraw.textScaleToFit(font, text, Math.max(1, maxWidth), 0.35F));
        PhoneScreenDraw.drawScaledText(guiGraphics, font, text, x, y, color, false, textScale);
    }

    /**
     * Like {@link #drawFitted}, but never shrinks below {@code minScale} — instead the
     * text is truncated with an ellipsis so it can never overflow into whatever sits
     * to its right (e.g. a row's value text or chevron).
     */
    private static void drawFittedMin(GuiGraphics guiGraphics, Font font, Component text, int x, int y,
                                      int maxWidth, int color, float maxScale, float minScale) {
        if (maxWidth <= 0) {
            return;
        }
        float textScale = Math.min(maxScale, PhoneScreenDraw.textScaleToFit(font, text, maxWidth, minScale));
        int maxUnscaledWidth = Math.max(1, (int) Math.floor(maxWidth / Math.max(0.01F, textScale)));
        Component fitted = clipToWidth(font, text, maxUnscaledWidth);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, fitted, x, y, color, false, textScale);
    }

    private static Component clipToWidth(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String source = text.getString();
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return Component.literal("");
        }

        int end = source.length();
        while (end > 0 && font.width(source.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return Component.literal(source.substring(0, Math.max(0, end)) + ellipsis);
    }

    private static void drawFittedCentered(GuiGraphics guiGraphics, Font font, Component text, int centerX, int y,
                                           int maxWidth, int color, float maxScale) {
        float textScale = Math.min(maxScale, PhoneScreenDraw.textScaleToFit(font, text, Math.max(1, maxWidth), 0.35F));
        int textWidth = PhoneScreenDraw.scaledTextWidth(font, text, textScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, text, centerX - textWidth / 2, y, color, false, textScale);
    }
}
