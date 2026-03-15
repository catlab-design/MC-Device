package com.sammy.minedevice.client.phone;

import com.sammy.minedevice.phone.PhoneChatMessage;
import com.sammy.minedevice.phone.PhoneContact;
import com.sammy.minedevice.phone.PhoneData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

final class PhoneChatSurfaceRenderer {
    private static final int PAGE_FILL = 0xFFF9FAFB; // Gray 50
    private static final int PAGE_TINT = 0xFFF3F4F6; // Gray 100
    private static final int HEADER_FILL = 0xFF6366F1; // Indigo 500
    private static final int HEADER_FILL_DARK = 0xFF4F46E5; // Indigo 600
    private static final int HEADER_CHIP_FILL = 0x22FFFFFF;
    private static final int CARD_FILL = 0xFFFFFFFF;
    private static final int CARD_BORDER = 0xFFE5E7EB; // Gray 200
    private static final int FIELD_FILL = 0xFFFFFFFF;
    private static final int FIELD_BORDER = 0xFFD1D5DB; // Gray 300
    private static final int BUBBLE_OUTGOING = 0xFF6366F1; // Indigo 500
    private static final int BUBBLE_OUTGOING_BORDER = 0xFF4F46E5; // Indigo 600
    private static final int BUBBLE_INCOMING = 0xFFF3F4F6; // Gray 100
    private static final int BUBBLE_INCOMING_BORDER = 0xFFE5E7EB; // Gray 200
    private static final int ACTION_FILL = 0xFF4F46E5; // Indigo 600
    private static final int ACTION_FILL_DARK = 0xFF4338CA; // Indigo 700
    private static final int ACTION_DISABLED_FILL = 0xFFE5E7EB; // Gray 200
    private static final int ACTION_DISABLED_DARK = 0xFFD1D5DB; // Gray 300
    private static final int DELETE_FILL = 0xFFEF4444; // Red 500
    private static final int DELETE_FILL_DARK = 0xFFDC2626; // Red 600
    private static final int TEXT_PRIMARY = 0xFF111827; // Gray 900
    private static final int TEXT_MUTED = 0xFF4B5563; // Gray 600
    private static final int TEXT_FAINT = 0xFF9CA3AF; // Gray 400
    private static final int TEXT_LIGHT = 0xFFFFFFFF;
    private static final int SHADOW_SOFT = 0x0A000000;

    private PhoneChatSurfaceRenderer() {
    }

    private static void renderRoundedPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int borderColor, int fillColor) {
        // Main fill
        guiGraphics.fill(x + 1, y, x + width - 1, y + height, fillColor);
        guiGraphics.fill(x, y + 1, x + width, y + height - 1, fillColor);

        // Border lines
        guiGraphics.fill(x + 1, y, x + width - 1, y + 1, borderColor); // Top
        guiGraphics.fill(x + 1, y + height - 1, x + width - 1, y + height, borderColor); // Bottom
        guiGraphics.fill(x, y + 1, x + 1, y + height - 1, borderColor); // Left
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height - 1, borderColor); // Right
    }

    private static void renderElevatedRoundedPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int borderColor, int fillColor, int shadowColor) {
        guiGraphics.fill(x + 1, y + 1, x + width + 1, y + height + 1, shadowColor);
        renderRoundedPanel(guiGraphics, x, y, width, height, borderColor, fillColor);
    }

    static void renderChatAppSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        UiRect contentBounds = screen.getChatSurfaceBounds();
        UiRect inputBounds = screen.getChatFriendInputBounds();
        UiRect addButtonBounds = screen.getChatAddButtonBounds();
        List<PhoneContact> friends = screen.getChatFriends();
        String ownNumber = screen.getOwnPhoneNumber();
        int rowTop = contentBounds.top + Math.max(5, Math.round(6 * screen.scale));
        int rowHeight = Math.max(14, Math.round(16 * screen.scale));
        int chipPadding = Math.max(4, Math.round(5 * screen.scale));
        int ownChipWidth = getChipWidth(font, ownNumber, chipPadding, screen.scale);
        int ownChipX = contentBounds.right() - ownChipWidth;
        int titlePaddingLeft = Math.max(5, Math.round(7 * screen.scale));
        int heroBottom = inputBounds.bottom() + Math.max(4, Math.round(5 * screen.scale));
        int headerBleed = Math.max(3, Math.round(6 * screen.scale));
        int headerLeft = contentBounds.left - headerBleed;
        int headerRight = contentBounds.right() + headerBleed;

        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.bottom(), PAGE_FILL);
        
        // Header with more depth
        guiGraphics.fill(headerLeft, contentBounds.top, headerRight, heroBottom, HEADER_FILL);
        guiGraphics.fill(headerLeft, heroBottom - 1, headerRight, heroBottom, 0x15000000); 
        
        guiGraphics.fill(contentBounds.left, heroBottom, contentBounds.right(), contentBounds.bottom(), PAGE_TINT);

        Component titleText = Component.translatable("screen.minedevice.phone.title.chat");
        renderPill(guiGraphics, font, ownNumber, ownChipX, rowTop, ownChipWidth, rowHeight,
                screen.scale, HEADER_CHIP_FILL, 0x11FFFFFF, TEXT_LIGHT, 0.72F);
        int titleMaxWidth = Math.max(24, ownChipX - contentBounds.left - titlePaddingLeft - Math.round(8 * screen.scale));
        drawFittedText(guiGraphics, font, titleText.copy().withStyle(s -> s.withBold(true)), contentBounds.left + titlePaddingLeft, rowTop,
                titleMaxWidth, TEXT_LIGHT, 0.40F);

        renderElevatedRoundedPanel(guiGraphics, inputBounds.left, inputBounds.top, inputBounds.width, inputBounds.height, FIELD_BORDER, FIELD_FILL, SHADOW_SOFT);
        Component inputText = screen.chatFriendNumber.isEmpty()
                ? Component.translatable("screen.minedevice.phone.chat.add_placeholder")
                : Component.literal(screen.chatFriendNumber);
        int inputColor = screen.chatFriendNumber.isEmpty() ? TEXT_FAINT : TEXT_PRIMARY;
        drawFittedText(guiGraphics, font, inputText, inputBounds.left + Math.max(4, Math.round(5 * screen.scale)),
                inputBounds.top + Math.max(4, Math.round(4 * screen.scale)),
                inputBounds.width - Math.max(8, Math.round(10 * screen.scale)), inputColor, 0.35F);

        boolean canAdd = screen.canAddChatFriend(screen.chatFriendNumber);
        int addFill = canAdd ? ACTION_FILL : ACTION_DISABLED_FILL;
        int addBorder = canAdd ? ACTION_FILL_DARK : ACTION_DISABLED_DARK;
        renderElevatedRoundedPanel(guiGraphics, addButtonBounds.left, addButtonBounds.top, addButtonBounds.width, addButtonBounds.height, addBorder, addFill, SHADOW_SOFT);
        drawCenteredFittedText(guiGraphics, font,
                Component.translatable("screen.minedevice.phone.chat.add_action"),
                addButtonBounds.left + (addButtonBounds.width / 2),
                addButtonBounds.top + Math.max(4, Math.round(4 * screen.scale)),
                addButtonBounds.width - Math.max(6, Math.round(8 * screen.scale)),
                TEXT_LIGHT, 0.35F, 0.78F);

        if (friends.isEmpty()) {
            UiRect rowsBounds = screen.getChatFriendRowsBounds();
            renderEmptyStateDecoration(guiGraphics, rowsBounds, screen.scale);
            Component emptyText = Component.translatable("screen.minedevice.phone.chat.empty");
            Component hintText = Component.translatable("screen.minedevice.phone.chat.hint");
            int centerX = rowsBounds.left + (rowsBounds.width / 2);
            int emptyY = rowsBounds.top + Math.max(10, rowsBounds.height / 2 - Math.round(12 * screen.scale));
            drawCenteredFittedText(guiGraphics, font, emptyText.copy().withStyle(s -> s.withBold(true)), centerX, emptyY,
                    rowsBounds.width - Math.round(10 * screen.scale), TEXT_PRIMARY, 0.35F);
            drawCenteredFittedText(guiGraphics, font, hintText, centerX,
                    emptyY + Math.max(10, Math.round(14 * screen.scale)),
                    rowsBounds.width - Math.round(10 * screen.scale), TEXT_MUTED, 0.32F);
            return;
        }

        for (int index = 0; index < friends.size(); index++) {
            PhoneContact friend = friends.get(index);
            UiRect rowBounds = screen.getChatFriendRowBounds(index, friends.size());
            boolean showDeleteButton = screen.isChatDeleteMenuOpenFor(friend.number());
            renderElevatedRoundedPanel(guiGraphics, rowBounds.left, rowBounds.top, rowBounds.width, rowBounds.height, CARD_BORDER, CARD_FILL, SHADOW_SOFT);

            int avatarSize = Math.max(10, Math.min(rowBounds.height - Math.max(10, Math.round(12 * screen.scale)),
                    Math.round(12 * screen.scale)));
            int avatarX = rowBounds.left + Math.max(4, Math.round(5 * screen.scale));
            int avatarY = rowBounds.top + (rowBounds.height - avatarSize) / 2;
            renderProfileFace(guiGraphics, screen.getChatProfileTexture(friend.number()), avatarX, avatarY, avatarSize);

            int textLeft = avatarX + avatarSize + Math.max(4, Math.round(5 * screen.scale));
            int textRight = rowBounds.right() - Math.max(5, Math.round(6 * screen.scale));
            if (showDeleteButton) {
                UiRect deleteBounds = screen.getChatDeleteButtonBounds(index, friends.size());
                textRight = Math.min(textRight, deleteBounds.left - Math.max(4, Math.round(5 * screen.scale)));
                renderElevatedRoundedPanel(guiGraphics, deleteBounds.left, deleteBounds.top, deleteBounds.width, deleteBounds.height,
                        DELETE_FILL_DARK, DELETE_FILL, SHADOW_SOFT);
                drawCenteredFittedText(guiGraphics, font,
                        Component.translatable("screen.minedevice.phone.chat.delete"),
                        deleteBounds.left + (deleteBounds.width / 2),
                        deleteBounds.top + Math.max(2, Math.round(2 * screen.scale)),
                        deleteBounds.width - Math.max(4, Math.round(6 * screen.scale)),
                        TEXT_LIGHT, 0.28F, 0.70F);
            }
            int textWidth = Math.max(18, textRight - textLeft);
            int nameY = rowBounds.top + Math.max(3, Math.round(3 * screen.scale));
            int previewY = nameY + Math.max(8, Math.round(9 * screen.scale));
            float nameMaxScale = PhoneData.isValidPhoneNumber(friend.displayName()) ? 0.70F : 0.84F;
            drawFittedText(guiGraphics, font, Component.literal(friend.displayName()), textLeft,
                    nameY, textWidth, TEXT_PRIMARY, 0.34F, nameMaxScale);

            PhoneChatMessage previewMessage = screen.getChatPreviewMessage(friend.number());
            Component previewText = previewMessage == null
                    ? Component.translatable("screen.minedevice.phone.chat.preview.empty")
                    : Component.translatable(
                    previewMessage.incoming()
                            ? "screen.minedevice.phone.chat.preview.incoming"
                            : "screen.minedevice.phone.chat.preview.outgoing",
                    previewMessage.text());
            drawFittedText(guiGraphics, font, previewText, textLeft, previewY, textWidth,
                    previewMessage == null ? TEXT_FAINT : TEXT_MUTED, 0.26F, 0.66F);
        }
    }

    static void renderChatThreadSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        UiRect contentBounds = screen.getChatSurfaceBounds();
        UiRect topBounds = screen.getChatThreadTopBounds();
        UiRect messagesBounds = screen.getChatMessagesBounds();
        UiRect draftBounds = screen.getChatDraftBounds();
        UiRect sendBounds = screen.getChatSendButtonBounds();
        String activeName = screen.getActiveChatDisplayName();
        String activeNumber = screen.activeChatNumber;
        int avatarSize = Math.max(10, Math.min(topBounds.height - Math.max(4, Math.round(6 * screen.scale)),
                Math.round(12 * screen.scale)));
        int avatarX = topBounds.left + Math.max(4, Math.round(5 * screen.scale));
        int avatarY = topBounds.top + Math.max(0, (topBounds.height - avatarSize) / 2);
        int headerTextLeft = avatarX + avatarSize + Math.max(4, Math.round(5 * screen.scale));
        int headerTextWidth = topBounds.right() - headerTextLeft - Math.max(5, Math.round(6 * screen.scale));

        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.bottom(), PAGE_FILL);
        renderElevatedRoundedPanel(guiGraphics, topBounds.left, topBounds.top, topBounds.width, topBounds.height, HEADER_FILL_DARK, HEADER_FILL, SHADOW_SOFT);
        
        // Slightly darker message area background for contrast
        renderRoundedPanel(guiGraphics, messagesBounds.left, messagesBounds.top, messagesBounds.width, messagesBounds.height, CARD_BORDER, 0xEEFFFFFF);

        renderProfileFace(guiGraphics, screen.getChatProfileTexture(activeNumber), avatarX, avatarY, avatarSize);
        Component headerName = Component.literal(activeName.isBlank() ? activeNumber : activeName).copy().withStyle(s -> s.withBold(true));
        float headerNameScale = Math.min(PhoneScreenDraw.textScaleToFit(font, headerName, headerTextWidth, 0.34F), 0.78F);
        int headerTextHeight = PhoneScreenDraw.scaledTextHeight(font, headerNameScale);
        int headerTextY = topBounds.top + Math.max(0, (topBounds.height - headerTextHeight) / 2);
        drawFittedText(guiGraphics, font, headerName,
                headerTextLeft,
                headerTextY,
                headerTextWidth, TEXT_LIGHT, 0.34F, 0.78F);

        renderMessages(screen, guiGraphics, font, messagesBounds);

        renderElevatedRoundedPanel(guiGraphics, draftBounds.left, draftBounds.top, draftBounds.width, draftBounds.height, FIELD_BORDER, FIELD_FILL, SHADOW_SOFT);
        Component draftText = screen.chatDraft.isEmpty()
                ? Component.translatable("screen.minedevice.phone.chat.message_placeholder")
                : Component.literal(screen.chatDraft);
        int draftColor = screen.chatDraft.isEmpty() ? TEXT_FAINT : TEXT_PRIMARY;
        drawFittedText(guiGraphics, font, draftText, draftBounds.left + Math.max(4, Math.round(5 * screen.scale)),
                draftBounds.top + Math.max(2, Math.round(2 * screen.scale)),
                draftBounds.width - Math.max(8, Math.round(10 * screen.scale)), draftColor, 0.32F);

        boolean canSend = screen.canSendChatMessage();
        int sendFill = canSend ? ACTION_FILL : ACTION_DISABLED_FILL;
        int sendBorder = canSend ? ACTION_FILL_DARK : ACTION_DISABLED_DARK;
        renderElevatedRoundedPanel(guiGraphics, sendBounds.left, sendBounds.top, sendBounds.width, sendBounds.height, sendBorder, sendFill, SHADOW_SOFT);
        drawCenteredFittedText(guiGraphics, font,
                Component.translatable("screen.minedevice.phone.chat.send"),
                sendBounds.left + (sendBounds.width / 2),
                sendBounds.top + Math.max(2, Math.round(2 * screen.scale)),
                sendBounds.width - Math.max(6, Math.round(8 * screen.scale)),
                TEXT_LIGHT, 0.35F, 0.78F);
    }

    private static void renderMessages(PhoneScreen screen, GuiGraphics guiGraphics, Font font, UiRect messagesBounds) {
        List<PhoneChatMessage> messages = screen.getActiveChatMessages();
        guiGraphics.fill(messagesBounds.left + 1, messagesBounds.top + 1, messagesBounds.right() - 1,
                messagesBounds.top + Math.max(8, Math.round(10 * screen.scale)), 0x11FFFFFF);
        if (messages.isEmpty()) {
            renderEmptyStateDecoration(guiGraphics, messagesBounds, screen.scale);
            int centerX = messagesBounds.left + (messagesBounds.width / 2);
            int centerY = messagesBounds.top + Math.max(8, messagesBounds.height / 2 - Math.round(10 * screen.scale));
            drawCenteredFittedText(guiGraphics, font,
                    Component.translatable("screen.minedevice.phone.chat.thread_empty"),
                    centerX, centerY,
                    messagesBounds.width - Math.round(10 * screen.scale), TEXT_PRIMARY, 0.35F);
            drawCenteredFittedText(guiGraphics, font,
                    Component.translatable("screen.minedevice.phone.chat.thread_hint"),
                    centerX, centerY + Math.max(10, Math.round(14 * screen.scale)),
                    messagesBounds.width - Math.round(10 * screen.scale), TEXT_MUTED, 0.32F);
            return;
        }

        int bubblePaddingX = Math.max(4, Math.round(5 * screen.scale));
        int bubblePaddingY = Math.max(3, Math.round(4 * screen.scale));
        int bubbleGap = Math.max(4, Math.round(5 * screen.scale));
        int lineHeight = Math.max(7, Math.round(font.lineHeight * 0.65F));
        int sideInset = Math.max(1, Math.round(2 * screen.scale));
        int maxBubbleWidth = Math.max(38, Math.round(messagesBounds.width * 0.80F));
        int cursorY = messagesBounds.bottom() - sideInset;

        for (int index = messages.size() - 1; index >= 0; index--) {
            PhoneChatMessage message = messages.get(index);
            List<FormattedCharSequence> lines = font.split(Component.literal(message.text()),
                    Math.max(18, maxBubbleWidth - (bubblePaddingX * 2)));
            int widestLine = 0;
            for (FormattedCharSequence line : lines) {
                widestLine = Math.max(widestLine, font.width(line));
            }

            int bubbleWidth = Math.min(maxBubbleWidth, widestLine + (bubblePaddingX * 2));
            int bubbleHeight = (lines.size() * lineHeight) + (bubblePaddingY * 2);
            int bubbleY = cursorY - bubbleHeight;
            if (bubbleY < messagesBounds.top + sideInset) {
                break;
            }

            int bubbleX = message.incoming()
                    ? messagesBounds.left + sideInset
                    : messagesBounds.right() - bubbleWidth - sideInset;
            int bubbleBorder = message.incoming() ? BUBBLE_INCOMING_BORDER : BUBBLE_OUTGOING_BORDER;
            int bubbleColor = message.incoming() ? BUBBLE_INCOMING : BUBBLE_OUTGOING;
            int textColor = message.incoming() ? TEXT_PRIMARY : TEXT_LIGHT;
            renderElevatedRoundedPanel(guiGraphics, bubbleX, bubbleY, bubbleWidth, bubbleHeight, bubbleBorder, bubbleColor, SHADOW_SOFT);

            int textY = bubbleY + bubblePaddingY;
            for (FormattedCharSequence line : lines) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(bubbleX + bubblePaddingX, textY, 0.0F);
                guiGraphics.pose().scale(0.65F, 0.65F, 1.0F);
                guiGraphics.drawString(font, line, 0, 0, textColor, false);
                guiGraphics.pose().popPose();
                textY += lineHeight;
            }

            cursorY = bubbleY - bubbleGap;
        }
    }

    private static int getChipWidth(Font font, String text, int chipPadding, float scale) {
        return Math.max(Math.round(30 * scale), font.width(text) + (chipPadding * 2));
    }

    private static void renderPill(GuiGraphics guiGraphics, Font font, String text,
                                   int chipX, int chipY, int chipWidth, int chipHeight,
                                   float scale, int fillColor, int borderColor, int textColor) {
        renderPill(guiGraphics, font, text, chipX, chipY, chipWidth, chipHeight, scale, fillColor, borderColor, textColor, 1.0F);
    }

    private static void renderPill(GuiGraphics guiGraphics, Font font, String text,
                                   int chipX, int chipY, int chipWidth, int chipHeight,
                                   float scale, int fillColor, int borderColor, int textColor, float maxScale) {
        Component chipText = Component.literal(text);
        renderRoundedPanel(guiGraphics, chipX, chipY, chipWidth, chipHeight, borderColor, fillColor);
        int textScaleWidth = Math.max(1, chipWidth - Math.max(4, Math.round(6 * scale)));
        float textScale = Math.min(PhoneScreenDraw.textScaleToFit(font, chipText, textScaleWidth, 0.35F), maxScale);
        int textWidth = PhoneScreenDraw.scaledTextWidth(font, chipText, textScale);
        int textHeight = PhoneScreenDraw.scaledTextHeight(font, textScale);
        int textX = chipX + (chipWidth - textWidth) / 2;
        int textY = chipY + (chipHeight - textHeight) / 2;
        PhoneScreenDraw.drawScaledText(guiGraphics, font, chipText, textX, textY, textColor, false, textScale);
    }

    private static void renderAvatarBadge(GuiGraphics guiGraphics, Font font, int x, int y, int size, String label) {
        renderElevatedRoundedPanel(guiGraphics, x, y, size, size, ACTION_FILL_DARK, ACTION_FILL, SHADOW_SOFT);
        Component text = Component.literal(label);
        float textScale = Mth.clamp(PhoneScreenDraw.textScaleToFit(font, text, Math.max(6, size - 4), 0.35F), 0.35F, 0.9F);
        int textWidth = PhoneScreenDraw.scaledTextWidth(font, text, textScale);
        int textHeight = PhoneScreenDraw.scaledTextHeight(font, textScale);
        int textX = x + (size - textWidth) / 2;
        int textY = y + (size - textHeight) / 2;
        PhoneScreenDraw.drawScaledText(guiGraphics, font, text.copy().withStyle(s -> s.withBold(true)), textX, textY, TEXT_LIGHT, false, textScale);
    }

    private static void renderProfileFace(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int size) {
        renderRoundedPanel(guiGraphics, x, y, size, size, CARD_BORDER, CARD_FILL);
        PlayerFaceRenderer.draw(guiGraphics, texture, x + 1, y + 1, Math.max(2, size - 2));
    }

    private static void drawFittedText(GuiGraphics guiGraphics, Font font, Component text, int leftX, int topY,
                                       int maxWidth, int color, float minScale) {
        drawFittedText(guiGraphics, font, text, leftX, topY, maxWidth, color, minScale, 1.0F);
    }

    private static void drawFittedText(GuiGraphics guiGraphics, Font font, Component text, int leftX, int topY,
                                       int maxWidth, int color, float minScale, float maxScale) {
        float textScale = PhoneScreenDraw.textScaleToFit(font, text, maxWidth, minScale);
        textScale = Math.min(textScale, maxScale);
        int maxUnscaledWidth = Math.max(1, Mth.floor(maxWidth / Math.max(0.01F, textScale)));
        Component fitted = clipToWidth(font, text, maxUnscaledWidth);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, fitted, leftX, topY, color, false, textScale);
    }

    private static void drawCenteredFittedText(GuiGraphics guiGraphics, Font font, Component text,
                                               int centerX, int topY, int maxWidth, int color,
                                               float minScale) {
        drawCenteredFittedText(guiGraphics, font, text, centerX, topY, maxWidth, color, minScale, 1.0F);
    }

    private static void drawCenteredFittedText(GuiGraphics guiGraphics, Font font, Component text,
                                               int centerX, int topY, int maxWidth, int color,
                                               float minScale, float maxScale) {
        float textScale = Mth.clamp(PhoneScreenDraw.textScaleToFit(font, text, maxWidth, minScale), minScale, maxScale);
        int maxUnscaledWidth = Math.max(1, Mth.floor(maxWidth / Math.max(0.01F, textScale)));
        Component fitted = clipToWidth(font, text, maxUnscaledWidth);
        int textWidth = PhoneScreenDraw.scaledTextWidth(font, fitted, textScale);
        int textX = centerX - (textWidth / 2);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, fitted, textX, topY, color, false, textScale);
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

    private static String getAvatarLabel(String primary, String fallback) {
        String source = primary == null ? "" : primary.strip();
        if (source.isEmpty()) {
            source = fallback == null ? "" : fallback.strip();
        }
        if (source.isEmpty()) {
            return "?";
        }

        return String.valueOf(Character.toUpperCase(source.charAt(0)));
    }

    private static void renderEmptyStateDecoration(GuiGraphics guiGraphics, UiRect bounds, float scale) {
        int bubbleWidth = Math.max(18, Math.round(bounds.width * 0.22F));
        int bubbleHeight = Math.max(10, Math.round(12 * scale));
        int centerX = bounds.left + (bounds.width / 2);
        int topY = bounds.top + Math.max(8, Math.round(bounds.height * 0.24F));

        renderRoundedPanel(guiGraphics,
                centerX - bubbleWidth - Math.max(6, Math.round(8 * scale)),
                topY + Math.max(8, Math.round(9 * scale)),
                bubbleWidth, bubbleHeight, CARD_BORDER, 0x66FFFFFF);
        renderRoundedPanel(guiGraphics,
                centerX - (bubbleWidth / 2),
                topY,
                bubbleWidth + Math.max(8, Math.round(10 * scale)),
                bubbleHeight + Math.max(2, Math.round(3 * scale)),
                0x99CAD7F2, 0x99FFFFFF);
    }
}
