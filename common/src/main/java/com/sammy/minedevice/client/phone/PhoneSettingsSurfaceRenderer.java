package com.sammy.minedevice.client.phone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PhoneSettingsSurfaceRenderer {
    private static final int PAGE_FILL = 0xFFF8FAFC;
    private static final int HEADER_FILL = 0xFF1E293B;
    private static final int CARD_FILL = 0xFFFFFFFF;
    private static final int CARD_EDGE = 0xFFE2E8F0;
    private static final int CARD_HOVER = 0xFFF1F5F9;

    private static final int TEXT_PRIMARY = 0xFF111827;
    private static final int TEXT_SECONDARY = 0xFF64748B;
    private static final int TEXT_HEADER = 0xFFFFFFFF;

    private static final int ICON_BLUE = 0xFF3B82F6;
    private static final int ICON_GREEN = 0xFF10B981;
    private static final int ICON_ORANGE = 0xFFF97316;
    private static final int ICON_PURPLE = 0xFF8B5CF6;

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
        UiRect contentBounds = screen.getMediaSurfaceBounds();

        // Extend header to touch phone frame
        int headerExpand = Math.max(4, Math.round(6 * screen.scale));
        int headerLeft = contentBounds.left - headerExpand;
        int headerRight = contentBounds.right() + headerExpand;
        int contentLeft = contentBounds.left;
        int contentRight = contentBounds.right();
        int contentTop = contentBounds.top;
        int contentBottom = contentBounds.bottom();

        // Temporarily disabled scissor to test if it blocks navigation buttons
        // guiGraphics.enableScissor(headerLeft, contentTop, headerRight, contentBottom);

        guiGraphics.fill(headerLeft, contentTop, headerRight, contentBottom, PAGE_FILL);

        int headerHeight = Math.max(28, Math.round(34 * screen.scale));
        int headerBottom = Math.min(contentBottom, contentTop + headerHeight);

        guiGraphics.fill(headerLeft, contentTop, headerRight, headerBottom, HEADER_FILL);

        Component title = Component.translatable("screen.minedevice.phone.title.settings");
        int titleX = headerLeft + Math.max(4, Math.round(12 * screen.scale));
        int titleY = contentTop + Math.max(1, (headerHeight - font.lineHeight) / 2);
        guiGraphics.drawString(font, title, titleX, titleY, TEXT_HEADER, false);

        int paddingX = Math.max(4, Math.round(8 * screen.scale));
        int itemLeft = contentLeft + paddingX;
        int itemRight = contentRight - paddingX;

        // Show "Coming Soon" message
        Component comingSoonText = Component.translatable("screen.minedevice.phone.settings.coming_soon");
        Component hintText = Component.translatable("screen.minedevice.phone.settings.coming_soon_hint");
        
        int textMaxWidth = itemRight - itemLeft;
        float comingSoonScale = PhoneScreenDraw.textScaleToFit(font, comingSoonText, textMaxWidth, 0.8F);
        float hintScale = PhoneScreenDraw.textScaleToFit(font, hintText, textMaxWidth, 0.6F);
        
        int comingSoonWidth = PhoneScreenDraw.scaledTextWidth(font, comingSoonText, comingSoonScale);
        int comingSoonHeight = PhoneScreenDraw.scaledTextHeight(font, comingSoonScale);
        int hintWidth = PhoneScreenDraw.scaledTextWidth(font, hintText, hintScale);
        int hintHeight = PhoneScreenDraw.scaledTextHeight(font, hintScale);
        
        int centerX = contentLeft + (contentRight - contentLeft) / 2;
        int centerY = contentTop + (contentBottom - contentTop) / 2;
        
        int comingSoonX = centerX - comingSoonWidth / 2;
        int comingSoonY = centerY - comingSoonHeight - Math.max(4, Math.round(6 * screen.scale));
        int hintX = centerX - hintWidth / 2;
        int hintY = centerY + Math.max(4, Math.round(6 * screen.scale));
        
        PhoneScreenDraw.drawScaledText(guiGraphics, font, comingSoonText, comingSoonX, comingSoonY, TEXT_PRIMARY, false, comingSoonScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, hintText, hintX, hintY, TEXT_SECONDARY, false, hintScale);

        // guiGraphics.disableScissor();
    }

    static boolean handleClick(PhoneScreen screen, double mouseX, double mouseY) {
        // No clickable items in settings yet, return false to allow navigation buttons to work
        return false;
    }

    private static void renderSettingsItem(
            GuiGraphics guiGraphics,
            Font font,
            int left,
            int top,
            int right,
            int height,
            String icon,
            String title,
            String subtitle,
            int iconColor,
            double mouseX,
            double mouseY
    ) {
        boolean hovering = mouseX >= left && mouseX < right && mouseY >= top && mouseY < top + height;

        guiGraphics.fill(left, top, right, top + height, hovering ? CARD_HOVER : CARD_FILL);
        guiGraphics.fill(left, top, right, top + 1, CARD_EDGE);
        guiGraphics.fill(left, top + height - 1, right, top + height, CARD_EDGE);
        guiGraphics.fill(left, top, left + 1, top + height, CARD_EDGE);
        guiGraphics.fill(right - 1, top, right, top + height, CARD_EDGE);

        int iconSize = Math.min(22, Math.max(16, height - 16));
        int iconLeft = left + 8;
        int iconTop = top + (height - iconSize) / 2;

        guiGraphics.fill(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize, iconColor);

        Component iconText = Component.literal(icon);
        int iconTextX = iconLeft + (iconSize - font.width(iconText)) / 2;
        int iconTextY = iconTop + (iconSize - font.lineHeight) / 2;
        guiGraphics.drawString(font, iconText, iconTextX, iconTextY, 0xFFFFFFFF, false);

        int textLeft = iconLeft + iconSize + 8;
        int textRight = right - 8;
        int textWidth = Math.max(1, textRight - textLeft);

        String fittedTitle = fitText(font, title, textWidth);
        String fittedSubtitle = fitText(font, subtitle, textWidth);

        int titleY = top + 8;
        int subtitleY = titleY + font.lineHeight + 2;

        guiGraphics.drawString(font, Component.literal(fittedTitle), textLeft, titleY, TEXT_PRIMARY, false);
        guiGraphics.drawString(font, Component.literal(fittedSubtitle), textLeft, subtitleY, TEXT_SECONDARY, false);
    }

    private static String fitText(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);

        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        String fitted = text;
        while (!fitted.isEmpty() && font.width(fitted) + ellipsisWidth > maxWidth) {
            fitted = fitted.substring(0, fitted.length() - 1);
        }

        return fitted + ellipsis;
    }
}
