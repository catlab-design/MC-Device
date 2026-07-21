package com.sammy.minedevice.client.phone;

import com.sammy.minedevice.phone.PhoneChatMessage;
import com.sammy.minedevice.phone.PhoneContact;
import com.sammy.minedevice.phone.PhoneData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

final class PhoneChatSurfaceRenderer {
    private static final int PAGE_FILL = 0xFFF9FAFB;
    private static final int PAGE_TINT = 0xFFF3F4F6;
    private static final int HEADER_FILL = 0xFF00A2E8;
    private static final int HEADER_FILL_DARK = 0xFF0081BA;
    private static final int HEADER_CHIP_FILL = 0x22FFFFFF;
    private static final int CARD_FILL = 0xFFFFFFFF;
    private static final int CARD_BORDER = 0xFFE5E7EB;
    private static final int FIELD_FILL = 0xFFFFFFFF;
    private static final int FIELD_BORDER = 0xFFD1D5DB;
    private static final int BUBBLE_OUTGOING = 0xFF00A2E8;
    private static final int BUBBLE_OUTGOING_BORDER = 0xFF0081BA;
    private static final int BUBBLE_INCOMING = 0xFFF3F4F6;
    private static final int BUBBLE_INCOMING_BORDER = 0xFFE5E7EB;
    private static final int ACTION_FILL = 0xFF0081BA;
    private static final int ACTION_FILL_DARK = 0xFF006A96;
    private static final int ACTION_DISABLED_FILL = 0xFFE5E7EB;
    private static final int ACTION_DISABLED_DARK = 0xFFD1D5DB;
    private static final int DELETE_FILL = 0xFFEF4444;
    private static final int DELETE_FILL_DARK = 0xFFDC2626;
    private static final int TEXT_PRIMARY = 0xFF111827;
    private static final int TEXT_MUTED = 0xFF4B5563;
    private static final int TEXT_FAINT = 0xFF9CA3AF;
    private static final int TEXT_LIGHT = 0xFFFFFFFF;
    private static final int SHADOW_SOFT = 0x0A000000;

    private PhoneChatSurfaceRenderer() {
    }

    private static void renderRoundedPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int borderColor, int fillColor) {
        guiGraphics.fill(x + 1, y, x + width - 1, y + height, fillColor);
        guiGraphics.fill(x, y + 1, x + width, y + height - 1, fillColor);

        guiGraphics.fill(x + 1, y, x + width - 1, y + 1, borderColor);
        guiGraphics.fill(x + 1, y + height - 1, x + width - 1, y + height, borderColor);
        guiGraphics.fill(x, y + 1, x + 1, y + height - 1, borderColor);
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height - 1, borderColor);
    }

    private static void renderElevatedRoundedPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int borderColor, int fillColor, int shadowColor) {
        guiGraphics.fill(x + 1, y + 1, x + width + 1, y + height + 1, shadowColor);
        renderRoundedPanel(guiGraphics, x, y, width, height, borderColor, fillColor);
    }

    static void renderChatAppSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        if (screen.chatQrShowMode) {
            renderChatQrSurface(screen, guiGraphics);
            return;
        }

        if (screen.chatAddSelectorMode) {
            renderChatAddSelectorSurface(screen, guiGraphics);
            return;
        }

        if (screen.chatAddMenuOpen) {
            renderChatAddMenuSurface(screen, guiGraphics);
            return;
        }

        if (screen.chatTab == 1) {
            renderProfileSurface(screen, guiGraphics);
            renderChatTabBar(screen, guiGraphics);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        UiRect contentBounds = screen.getChatSurfaceBounds();
        UiRect headerBounds = screen.getChatHeaderBounds();
        UiRect plusBounds = screen.getChatHeaderPlusBounds();
        List<PhoneContact> friends = screen.getChatFriends();
        int rowTop = contentBounds.top + Math.max(5, Math.round(6 * screen.scale));
        int titlePaddingLeft = Math.max(5, Math.round(7 * screen.scale));
        int heroBottom = headerBounds.bottom();
        int headerBleed = Math.max(3, Math.round(6 * screen.scale));
        int headerLeft = contentBounds.left - headerBleed;
        int headerRight = contentBounds.right() + headerBleed;

        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.bottom(), PAGE_FILL);
        
        guiGraphics.fill(headerLeft, contentBounds.top, headerRight, heroBottom, HEADER_FILL);
        guiGraphics.fill(headerLeft, heroBottom - 1, headerRight, heroBottom, 0x15000000); 
        
        guiGraphics.fill(contentBounds.left, heroBottom, contentBounds.right(), contentBounds.bottom(), PAGE_TINT);

        // Own phone number is shown on the Profile tab only.
        Component titleText = Component.translatable("screen.minedevice.phone.title.chat");
        int titleMaxWidth = Math.max(24, plusBounds.left - contentBounds.left - titlePaddingLeft - Math.round(8 * screen.scale));
        drawFittedText(guiGraphics, font, titleText.copy().withStyle(s -> s.withBold(true)), contentBounds.left + titlePaddingLeft, rowTop,
                titleMaxWidth, TEXT_LIGHT, 0.40F);

        // "+ Add" button styled like the gallery's "+ Import" button — precise
        // centering: fit the scale, measure the text, center on both axes.
        guiGraphics.fill(plusBounds.left, plusBounds.top, plusBounds.right(), plusBounds.bottom(), 0x40FFFFFF);
        Component addText = Component.translatable("phone.chat.add_button");
        float addScale = Math.min(0.55F, PhoneScreenDraw.textScaleToFit(font, addText, plusBounds.width - 4, 0.4F));
        int addTextWidth = PhoneScreenDraw.scaledTextWidth(font, addText, addScale);
        int addTextHeight = PhoneScreenDraw.scaledTextHeight(font, addScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, addText,
                plusBounds.left + (plusBounds.width - addTextWidth) / 2,
                plusBounds.top + (plusBounds.height - addTextHeight) / 2, TEXT_LIGHT, false, addScale);

        if (friends.isEmpty()) {
            UiRect rowsBounds = screen.getChatFriendRowsBounds();
            renderEmptyStateDecoration(guiGraphics, rowsBounds, screen.scale);
            Component emptyText = Component.translatable("screen.minedevice.phone.chat.empty");
            Component hintText = Component.translatable("screen.minedevice.phone.chat.hint");
            int centerX = rowsBounds.left + (rowsBounds.width / 2);
            int emptyY = rowsBounds.top + Math.max(10, rowsBounds.height / 2 - Math.round(12 * screen.scale));
            drawCenteredFittedText(guiGraphics, font, emptyText.copy().withStyle(s -> s.withBold(true)), centerX, emptyY,
                    rowsBounds.width - Math.round(10 * screen.scale), TEXT_PRIMARY, 0.45F);
            drawCenteredFittedText(guiGraphics, font, hintText, centerX,
                    emptyY + Math.max(10, Math.round(14 * screen.scale)),
                    rowsBounds.width - Math.round(6 * screen.scale), TEXT_MUTED, 0.38F);
        } else {
            UiRect rowsBounds = screen.getChatFriendRowsBounds();
            guiGraphics.enableScissor(rowsBounds.left, rowsBounds.top, rowsBounds.right(), rowsBounds.bottom());

            int scrollOffset = screen.chatFriendsScrollOffset;
            int visibleRows = screen.getChatFriendsVisibleRows();
            int end = Math.min(friends.size(), scrollOffset + visibleRows);
            for (int index = scrollOffset; index < end; index++) {
                PhoneContact friend = friends.get(index);
                UiRect rowBounds = screen.getChatFriendRowBounds(index, friends.size());
                if (rowBounds.bottom() < rowsBounds.top) continue;

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

            if (friends.size() > visibleRows) {
                int scrollBarLeft = rowsBounds.right() - Math.max(2, Math.round(3 * screen.scale));
                int scrollBarWidth = Math.max(2, Math.round(3 * screen.scale));
                int scrollBarHeight = rowsBounds.height;
                int thumbHeight = Math.max(8, scrollBarHeight * visibleRows / friends.size());
                int thumbTop = rowsBounds.top + (scrollBarHeight - thumbHeight) * scrollOffset / Math.max(1, friends.size() - visibleRows);
                guiGraphics.fill(scrollBarLeft, rowsBounds.top, scrollBarLeft + scrollBarWidth, rowsBounds.bottom(), 0x20AAAAAA);
                guiGraphics.fill(scrollBarLeft, thumbTop, scrollBarLeft + scrollBarWidth, thumbTop + thumbHeight, 0x60AAAAAA);
            }

            guiGraphics.disableScissor();
        }

        renderChatTabBar(screen, guiGraphics);
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
                    messagesBounds.width - Math.round(10 * screen.scale), TEXT_PRIMARY, 0.45F);
            drawCenteredFittedText(guiGraphics, font,
                    Component.translatable("screen.minedevice.phone.chat.thread_hint"),
                    centerX, centerY + Math.max(10, Math.round(14 * screen.scale)),
                    messagesBounds.width - Math.round(6 * screen.scale), TEXT_MUTED, 0.38F);
            return;
        }

        int bubblePaddingX = Math.max(4, Math.round(5 * screen.scale));
        int bubblePaddingY = Math.max(3, Math.round(4 * screen.scale));
        int bubbleGap = Math.max(4, Math.round(5 * screen.scale));
        int lineHeight = Math.max(7, Math.round(font.lineHeight * 0.65F));
        int sideInset = Math.max(1, Math.round(2 * screen.scale));
        int maxBubbleWidth = Math.max(38, Math.round(messagesBounds.width * 0.80F));
        int cursorY = messagesBounds.bottom() - sideInset + screen.chatScrollOffset;

        guiGraphics.enableScissor(messagesBounds.left + 1, messagesBounds.top + 1, messagesBounds.right() - 1, messagesBounds.bottom() - 1);

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
            if (bubbleY + bubbleHeight < messagesBounds.top + sideInset) {
                break;
            }

            if (bubbleY < messagesBounds.bottom() - sideInset && bubbleY + bubbleHeight > messagesBounds.top + sideInset) {
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
            }

            cursorY = bubbleY - bubbleGap;
        }

        guiGraphics.disableScissor();
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

    private static void renderChatAddSelectorSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        UiRect contentBounds = screen.getChatSurfaceBounds();
        UiRect rowsBounds = screen.getChatFriendRowsBounds();
        int rowTop = contentBounds.top + Math.max(5, Math.round(6 * screen.scale));
        int rowHeight = Math.max(14, Math.round(16 * screen.scale));
        int titlePaddingLeft = Math.max(5, Math.round(7 * screen.scale));

        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.bottom(), PAGE_FILL);
        
        // Draw Header
        int heroBottom = contentBounds.top + Math.max(22, Math.round(26 * screen.scale));
        int headerBleed = Math.max(3, Math.round(6 * screen.scale));
        int headerLeft = contentBounds.left - headerBleed;
        int headerRight = contentBounds.right() + headerBleed;
        guiGraphics.fill(headerLeft, contentBounds.top, headerRight, heroBottom, HEADER_FILL);
        guiGraphics.fill(headerLeft, heroBottom - 1, headerRight, heroBottom, 0x15000000); 
        guiGraphics.fill(contentBounds.left, heroBottom, contentBounds.right(), contentBounds.bottom(), PAGE_TINT);

        // Header Text
        Component titleText = Component.translatable("screen.minedevice.phone.chat.select_player");
        drawFittedText(guiGraphics, font, titleText.copy().withStyle(s -> s.withBold(true)), contentBounds.left + titlePaddingLeft, rowTop,
                contentBounds.width - Math.round(20 * screen.scale), TEXT_LIGHT, 0.40F);

        List<PlayerInfo> selectables = screen.getChatAddSelectablePlayers();
        if (selectables.isEmpty()) {
            renderEmptyStateDecoration(guiGraphics, rowsBounds, screen.scale);
            Component emptyText = Component.translatable("screen.minedevice.phone.chat.no_players");
            int centerX = rowsBounds.left + (rowsBounds.width / 2);
            int emptyY = rowsBounds.top + Math.max(10, rowsBounds.height / 2 - Math.round(12 * screen.scale));
            drawCenteredFittedText(guiGraphics, font, emptyText.copy().withStyle(s -> s.withBold(true)), centerX, emptyY,
                    rowsBounds.width - Math.round(10 * screen.scale), TEXT_PRIMARY, 0.35F);
            return;
        }

        guiGraphics.enableScissor(rowsBounds.left, rowsBounds.top, rowsBounds.right(), rowsBounds.bottom());

        int listRowHeight = Math.max(22, Math.round(26 * screen.scale));
        int rowGap = Math.max(2, Math.round(3 * screen.scale));
        int listTop = rowsBounds.top + screen.chatAddSelectorScrollOffset;

        for (int i = 0; i < selectables.size(); i++) {
            PlayerInfo playerInfo = selectables.get(i);
            int y = listTop + i * (listRowHeight + rowGap);
            if (y + listRowHeight < rowsBounds.top) {
                continue;
            }
            if (y > rowsBounds.bottom()) {
                break;
            }

            renderElevatedRoundedPanel(guiGraphics, rowsBounds.left, y, rowsBounds.width, listRowHeight, CARD_BORDER, CARD_FILL, SHADOW_SOFT);

            int avatarSize = Math.max(10, listRowHeight - Math.max(4, Math.round(6 * screen.scale)));
            int avatarX = rowsBounds.left + Math.max(4, Math.round(5 * screen.scale));
            int avatarY = y + (listRowHeight - avatarSize) / 2;
            renderProfileFace(guiGraphics, screen.getPlayerSkin(playerInfo), avatarX, avatarY, avatarSize);

            int textLeft = avatarX + avatarSize + Math.max(4, Math.round(5 * screen.scale));
            int textRight = rowsBounds.right() - Math.max(5, Math.round(6 * screen.scale));
            int textWidth = Math.max(18, textRight - textLeft);
            int nameY = y + (listRowHeight - Math.round(font.lineHeight * 0.7F)) / 2;
            drawFittedText(guiGraphics, font, Component.literal(playerInfo.getProfile().getName()), textLeft,
                    nameY, textWidth, TEXT_PRIMARY, 0.34F, 0.84F);
        }

        guiGraphics.disableScissor();
    }

    private static void renderChatTabBar(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        UiRect tabBounds = screen.getChatTabBarBounds();

        guiGraphics.fill(tabBounds.left, tabBounds.top, tabBounds.right(), tabBounds.bottom(), 0xFFFFFFFF);
        guiGraphics.fill(tabBounds.left, tabBounds.top, tabBounds.right(), tabBounds.top + 1, 0x1A000000);

        int halfWidth = tabBounds.width / 2;
        int activeColor = 0xFF0081BA;
        int inactiveColor = 0xFF8E8E93;

        int safeHeight = tabBounds.height - Math.max(4, Math.round(6 * screen.scale));

        Component label0 = Component.translatable("phone.chat.tab.chats");
        float scale0 = Math.min(1.0F, Math.max(0.35F, (float) (halfWidth - 4) / Math.max(1, font.width(label0))));
        int w0 = Math.round(font.width(label0) * scale0);
        int h0 = Math.round(font.lineHeight * scale0);
        int x0 = tabBounds.left + halfWidth / 2 - w0 / 2;
        int y0 = tabBounds.top + (safeHeight - h0) / 2;
        PhoneScreenDraw.drawScaledText(guiGraphics, font, label0.copy().withStyle(s -> s.withBold(screen.chatTab == 0)), x0, y0, screen.chatTab == 0 ? activeColor : inactiveColor, false, scale0);

        Component label1 = Component.translatable("phone.chat.tab.profile");
        float scale1 = Math.min(1.0F, Math.max(0.35F, (float) (halfWidth - 4) / Math.max(1, font.width(label1))));
        int w1 = Math.round(font.width(label1) * scale1);
        int h1 = Math.round(font.lineHeight * scale1);
        int x1 = tabBounds.left + halfWidth + halfWidth / 2 - w1 / 2;
        int y1 = tabBounds.top + (safeHeight - h1) / 2;
        PhoneScreenDraw.drawScaledText(guiGraphics, font, label1.copy().withStyle(s -> s.withBold(screen.chatTab == 1)), x1, y1, screen.chatTab == 1 ? activeColor : inactiveColor, false, scale1);
    }

    private static void renderProfileSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        UiRect contentBounds = screen.getChatSurfaceBounds();

        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.bottom(), PAGE_FILL);

        int heroHeight = Math.max(80, Math.round(90 * screen.scale));
        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.top + heroHeight, 0xFFF3F4F6);
        guiGraphics.fill(contentBounds.left, contentBounds.top + heroHeight - 1, contentBounds.right(), contentBounds.top + heroHeight, 0x15000000);

        ResourceLocation skinTexture = null;
        if (minecraft.getConnection() != null && minecraft.player != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
            if (info != null) {
                skinTexture = screen.getPlayerSkin(info);
            }
        }
        if (skinTexture == null) {
            skinTexture = net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(minecraft.player != null ? minecraft.player.getUUID() : java.util.UUID.randomUUID());
        }

        int avatarSize = Math.max(24, Math.round(30 * screen.scale));
        int avatarX = contentBounds.left + (contentBounds.width - avatarSize) / 2;
        int avatarY = contentBounds.top + Math.max(8, Math.round(10 * screen.scale));
        renderProfileFace(guiGraphics, skinTexture, avatarX, avatarY, avatarSize);

        String nickname = PhoneClientChatState.getOwnNickname();
        if (nickname.isEmpty() && minecraft.player != null) {
            nickname = minecraft.player.getGameProfile().getName();
        }

        int nicknameBand = Math.max(9, Math.round(font.lineHeight * 0.9F));
        int nicknameY = avatarY + avatarSize + Math.max(4, Math.round(6 * screen.scale));
        int textCenterY = nicknameY + Math.max(0, (nicknameBand - Math.round(font.lineHeight * 0.85F)) / 2);
        int centerX = contentBounds.left + (contentBounds.width / 2);

        if (screen.chatNicknameEditMode) {
            UiRect editBounds = screen.getChatNicknameEditBounds();
            renderElevatedRoundedPanel(guiGraphics, editBounds.left, editBounds.top, editBounds.width, editBounds.height,
                    FIELD_BORDER, FIELD_FILL, SHADOW_SOFT);

            Component textToDraw = screen.chatNicknameDraft.isEmpty()
                    ? Component.translatable("phone.chat.profile.nickname_placeholder")
                    : Component.literal(screen.chatNicknameDraft);
            int inputColor = screen.chatNicknameDraft.isEmpty() ? TEXT_FAINT : TEXT_PRIMARY;
            drawFittedText(guiGraphics, font, textToDraw, editBounds.left + Math.max(3, Math.round(4 * screen.scale)),
                    editBounds.top + (editBounds.height - Math.round(font.lineHeight * 0.35F)) / 2,
                    editBounds.width - Math.max(6, Math.round(8 * screen.scale)), inputColor, 0.35F);

            drawCenteredFittedText(guiGraphics, font, Component.translatable("phone.chat.profile.nickname_hint"),
                    centerX, editBounds.bottom() + Math.max(1, Math.round(1 * screen.scale)),
                    contentBounds.width - 20, TEXT_MUTED, 0.22F);
        } else {
            drawCenteredFittedText(guiGraphics, font, Component.literal(nickname).copy().withStyle(s -> s.withBold(true)),
                    centerX, textCenterY, contentBounds.width - 20, TEXT_PRIMARY, 0.44F, 0.85F);

            UiRect editBtnBounds = screen.getChatNicknameEditBounds();
            renderElevatedRoundedPanel(guiGraphics, editBtnBounds.left, editBtnBounds.top, editBtnBounds.width, editBtnBounds.height,
                    0xFFE5E7EB, 0xFFF3F4F6, SHADOW_SOFT);
            drawCenteredFittedText(guiGraphics, font, Component.translatable("phone.chat.profile.edit_nickname"),
                    centerX, editBtnBounds.top + (editBtnBounds.height - Math.round(font.lineHeight * 0.28F)) / 2,
                    editBtnBounds.width - 4, TEXT_MUTED, 0.28F);
        }

        UiRect detailsBounds = screen.getChatProfileDetailsBounds();
        renderElevatedRoundedPanel(guiGraphics, detailsBounds.left, detailsBounds.top, detailsBounds.width, detailsBounds.height, CARD_BORDER, CARD_FILL, SHADOW_SOFT);

        int labelY = detailsBounds.top + Math.max(3, Math.round(4 * screen.scale));
        drawFittedText(guiGraphics, font, Component.translatable("phone.chat.profile.my_number"),
                detailsBounds.left + Math.max(6, Math.round(8 * screen.scale)),
                labelY, detailsBounds.width - 20, TEXT_MUTED, 0.28F);

        int valY = labelY + Math.max(9, Math.round(10 * screen.scale));
        drawFittedText(guiGraphics, font, Component.literal(screen.getOwnPhoneNumber()).copy().withStyle(s -> s.withBold(true)),
                detailsBounds.left + Math.max(6, Math.round(8 * screen.scale)),
                valY, detailsBounds.width - 20, 0xFF0081BA, 0.38F);

        UiRect friendsBounds = screen.getChatProfileFriendsBounds();
        renderElevatedRoundedPanel(guiGraphics, friendsBounds.left, friendsBounds.top, friendsBounds.width, friendsBounds.height, CARD_BORDER, CARD_FILL, SHADOW_SOFT);

        int countNumberWidth = Math.max(20, Math.round(25 * screen.scale));
        int friendsLabelMaxWidth = friendsBounds.width - countNumberWidth - Math.max(12, Math.round(16 * screen.scale));
        drawFittedText(guiGraphics, font, Component.translatable("phone.chat.profile.friends_count"),
                friendsBounds.left + Math.max(6, Math.round(8 * screen.scale)),
                friendsBounds.top + (friendsBounds.height - Math.round(font.lineHeight * 0.32F)) / 2,
                friendsLabelMaxWidth, TEXT_PRIMARY, 0.32F);

        String countText = String.valueOf(PhoneClientChatState.getFriends().size());
        drawFittedText(guiGraphics, font, Component.literal(countText).copy().withStyle(s -> s.withBold(true)),
                friendsBounds.right() - countNumberWidth,
                friendsBounds.top + (friendsBounds.height - Math.round(font.lineHeight * 0.32F)) / 2,
                countNumberWidth, TEXT_MUTED, 0.32F);

        UiRect addFriendBounds = screen.getChatAddFriendButtonBounds();
        renderElevatedRoundedPanel(guiGraphics, addFriendBounds.left, addFriendBounds.top, addFriendBounds.width, addFriendBounds.height,
                0xFF04B04B, 0xFF06C755, SHADOW_SOFT);
        drawCenteredFittedText(guiGraphics, font, Component.translatable("phone.chat.add_menu.title"),
                addFriendBounds.left + (addFriendBounds.width / 2),
                addFriendBounds.top + (addFriendBounds.height - Math.round(font.lineHeight * 0.32F)) / 2,
                addFriendBounds.width - Math.max(4, Math.round(6 * screen.scale)), TEXT_LIGHT, 0.32F);
    }

    private static void renderChatAddMenuSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        UiRect contentBounds = screen.getChatSurfaceBounds();

        // 1. Fill background
        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.bottom(), PAGE_FILL);

        // 2. Draw Header
        int headerHeight = Math.max(24, Math.round(30 * screen.scale));
        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.top + headerHeight, 0xFFFFFFFF);
        guiGraphics.fill(contentBounds.left, contentBounds.top + headerHeight - 1, contentBounds.right(), contentBounds.top + headerHeight, 0x1A000000);

        // Back button <-
        UiRect backBounds = screen.getChatAddBackBtnBounds();
        drawCenteredFittedText(guiGraphics, font, Component.literal("<-"),
                backBounds.left + backBounds.width / 2,
                backBounds.top + (backBounds.height - Math.round(font.lineHeight * 0.55F)) / 2,
                backBounds.width, 0xFF0081BA, 0.55F);

        // Title
        int centerX = contentBounds.left + contentBounds.width / 2;
        drawCenteredFittedText(guiGraphics, font, Component.translatable("phone.chat.add_menu.title").copy().withStyle(s -> s.withBold(true)),
                centerX,
                contentBounds.top + (headerHeight - Math.round(font.lineHeight * 0.55F)) / 2,
                contentBounds.width - 60, TEXT_PRIMARY, 0.55F);

        // 3. Phone Number Input Area
        UiRect inputBounds = screen.getChatAddInputBounds();
        // Label
        int labelY = inputBounds.top - Math.max(10, Math.round(12 * screen.scale));
        drawFittedText(guiGraphics, font, Component.translatable("phone.chat.add_menu.by_number"),
                inputBounds.left + Math.max(2, Math.round(3 * screen.scale)),
                labelY, inputBounds.width, TEXT_MUTED, 0.45F);

        // Input Field Card
        renderElevatedRoundedPanel(guiGraphics, inputBounds.left, inputBounds.top, inputBounds.width, inputBounds.height,
                FIELD_BORDER, FIELD_FILL, SHADOW_SOFT);

        Component placeholder = screen.chatFriendNumber.isEmpty()
                ? Component.translatable("screen.minedevice.phone.chat.add_placeholder")
                : Component.literal(screen.chatFriendNumber);
        int inputColor = screen.chatFriendNumber.isEmpty() ? TEXT_FAINT : TEXT_PRIMARY;
        drawFittedText(guiGraphics, font, placeholder, inputBounds.left + Math.max(4, Math.round(6 * screen.scale)),
                inputBounds.top + (inputBounds.height - Math.round(font.lineHeight * 0.48F)) / 2,
                inputBounds.width - Math.max(8, Math.round(12 * screen.scale)), inputColor, 0.48F);

        // Confirm Button
        UiRect confirmBounds = screen.getChatAddConfirmBtnBounds();
        boolean canAdd = screen.canAddChatFriend(screen.chatFriendNumber);
        int addFill = canAdd ? ACTION_FILL : ACTION_DISABLED_FILL;
        int addBorder = canAdd ? ACTION_FILL_DARK : ACTION_DISABLED_DARK;
        renderElevatedRoundedPanel(guiGraphics, confirmBounds.left, confirmBounds.top, confirmBounds.width, confirmBounds.height,
                addBorder, addFill, SHADOW_SOFT);
        drawCenteredFittedText(guiGraphics, font, Component.translatable("phone.chat.add_menu.title"),
                confirmBounds.left + confirmBounds.width / 2,
                confirmBounds.top + (confirmBounds.height - Math.round(font.lineHeight * 0.5F)) / 2,
                confirmBounds.width - 6, TEXT_LIGHT, 0.5F);

        // Divider
        int dividerY = confirmBounds.bottom() + Math.max(8, Math.round(10 * screen.scale));
        guiGraphics.fill(contentBounds.left + Math.max(10, Math.round(12 * screen.scale)), dividerY,
                contentBounds.right() - Math.max(10, Math.round(12 * screen.scale)), dividerY + 1, 0x1A000000);

        // 4. Online Players Button Card
        UiRect onlineBounds = screen.getChatAddOptOnlineBounds();
        renderElevatedRoundedPanel(guiGraphics, onlineBounds.left, onlineBounds.top, onlineBounds.width, onlineBounds.height,
                CARD_BORDER, CARD_FILL, SHADOW_SOFT);

        // Icon / decoration
        int iconX = onlineBounds.left + Math.max(8, Math.round(12 * screen.scale));
        int iconReserve = Math.max(14, Math.round(20 * screen.scale));
        drawFittedText(guiGraphics, font, Component.literal("\uD83D\uDC64"),
                iconX,
                onlineBounds.top + (onlineBounds.height - Math.round(font.lineHeight * 0.5F)) / 2,
                30, 0xFF0081BA, 0.5F);

        int optionTextLeft = iconX + iconReserve;
        int optionTextWidth = onlineBounds.right() - optionTextLeft - Math.max(4, Math.round(6 * screen.scale));
        drawFittedText(guiGraphics, font, Component.translatable("phone.chat.add_menu.by_online").copy().withStyle(s -> s.withBold(true)),
                optionTextLeft,
                onlineBounds.top + (onlineBounds.height - Math.round(font.lineHeight * 0.5F)) / 2,
                optionTextWidth, TEXT_PRIMARY, 0.5F);

        // 5. Scan QR Code Button Card
        UiRect scanBounds = screen.getChatAddOptScanBounds();
        renderElevatedRoundedPanel(guiGraphics, scanBounds.left, scanBounds.top, scanBounds.width, scanBounds.height,
                0xFF04B04B, 0xFF06C755, SHADOW_SOFT);

        drawFittedText(guiGraphics, font, Component.literal("\uD83D\uDCF1"),
                iconX,
                scanBounds.top + (scanBounds.height - Math.round(font.lineHeight * 0.5F)) / 2,
                30, TEXT_LIGHT, 0.5F);

        drawFittedText(guiGraphics, font, Component.translatable("phone.chat.add_menu.by_scan").copy().withStyle(s -> s.withBold(true)),
                optionTextLeft,
                scanBounds.top + (scanBounds.height - Math.round(font.lineHeight * 0.5F)) / 2,
                scanBounds.right() - optionTextLeft - Math.max(4, Math.round(6 * screen.scale)), TEXT_LIGHT, 0.5F);
    }

    static void renderChatCameraOverlay(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        Font font = minecraft.font;
        UiRect cameraBounds = screen.getCameraViewBounds();
        int headerHeight = Math.max(42, Math.round(cameraBounds.height * 0.16F));
        int footerHeight = Math.max(58, Math.round(cameraBounds.height * 0.24F));
        int footerTop = cameraBounds.bottom() - footerHeight;

        int horizontalBleed = Math.max(4, Math.round(6 * screen.scale));
        int overlayLeft = cameraBounds.left - horizontalBleed;
        int overlayRight = cameraBounds.right() + horizontalBleed;
        int overlayFill = 0xB8141A22;

        guiGraphics.fill(overlayLeft, cameraBounds.top,
                overlayRight, cameraBounds.top + headerHeight, overlayFill);
        guiGraphics.fill(overlayLeft, footerTop,
                overlayRight, cameraBounds.bottom(), overlayFill);

        int centerX = cameraBounds.left + cameraBounds.width / 2;
        drawCenteredFittedText(guiGraphics, font, Component.translatable("phone.chat.add_menu.by_scan"),
                centerX,
                cameraBounds.top + Math.round(headerHeight * 0.33F),
                cameraBounds.width - 20, TEXT_LIGHT, 0.48F);

        int underlineWidth = Math.max(42, Math.round(cameraBounds.width * 0.23F));
        int underlineX = cameraBounds.left + (cameraBounds.width - underlineWidth) / 2;
        int underlineY = cameraBounds.top + Math.round(headerHeight * 0.64F);
        guiGraphics.fill(underlineX, underlineY, underlineX + underlineWidth,
                underlineY + Math.max(1, Math.round(1 * screen.scale)), 0xCCFFFFFF);

        drawCenteredFittedText(guiGraphics, font, Component.translatable("phone.chat.add_menu.scan_hint"),
                centerX,
                footerTop + Math.max(8, Math.round(10 * screen.scale)),
                cameraBounds.width - 8, TEXT_LIGHT, 0.4F);

        int availableTop = cameraBounds.top + headerHeight + Math.max(18, Math.round(22 * screen.scale));
        int availableBottom = cameraBounds.bottom() - footerHeight - Math.max(12, Math.round(14 * screen.scale));
        int availableHeight = Math.max(24, availableBottom - availableTop);
        int size = Math.min(cameraBounds.width - Math.max(28, Math.round(34 * screen.scale)),
                Math.min(availableHeight, Math.max(70, Math.round(92 * screen.scale))));
        int left = cameraBounds.left + (cameraBounds.width - size) / 2;
        int top = availableTop + Math.max(0, (availableHeight - size) / 2);
        int right = left + size;
        int bottom = top + size;
        int cornerLength = Math.max(12, Math.round(17 * screen.scale));
        int thickness = Math.max(2, Math.round(2 * screen.scale));
        int shadow = 0x66000000;
        int color = screen.chatScanHoverTicks > 0 ? 0xFF06C755 : 0xEFFFFFFF;

        drawCorner(guiGraphics, left + 1, top + 1, cornerLength, thickness, true, true, shadow);
        drawCorner(guiGraphics, right - 1, top + 1, cornerLength, thickness, false, true, shadow);
        drawCorner(guiGraphics, left + 1, bottom - 1, cornerLength, thickness, true, false, shadow);
        drawCorner(guiGraphics, right - 1, bottom - 1, cornerLength, thickness, false, false, shadow);
        drawCorner(guiGraphics, left, top, cornerLength, thickness, true, true, color);
        drawCorner(guiGraphics, right, top, cornerLength, thickness, false, true, color);
        drawCorner(guiGraphics, left, bottom, cornerLength, thickness, true, false, color);
        drawCorner(guiGraphics, right, bottom, cornerLength, thickness, false, false, color);

        if (screen.chatScanHoverTicks > 0) {
            float progress = screen.chatScanHoverTicks / 15.0F;
            int fillHeight = Math.round(size * progress);
            guiGraphics.fill(left + thickness, bottom - fillHeight - thickness,
                    right - thickness, bottom - thickness, 0x3306C755);
        }

        // Footer buttons, same layout as the bank's scan-to-pay overlay:
        // wide "My QR" button + eye button for free-look.
        UiRect myQrBounds = screen.getBankScanReceiveButtonBounds();
        renderElevatedRoundedPanel(guiGraphics, myQrBounds.left, myQrBounds.top, myQrBounds.width, myQrBounds.height,
                0xFF04B04B, 0xFF06C755, SHADOW_SOFT);
        drawCenteredFittedText(guiGraphics, font, Component.translatable("phone.chat.scan.my_qr"),
                myQrBounds.left + myQrBounds.width / 2,
                myQrBounds.top + (myQrBounds.height - Math.round(font.lineHeight * 0.5F)) / 2,
                myQrBounds.width - 6, TEXT_LIGHT, 0.5F);

        UiRect eyeBounds = screen.getBankScanEyeButtonBounds();
        renderElevatedRoundedPanel(guiGraphics, eyeBounds.left, eyeBounds.top, eyeBounds.width, eyeBounds.height,
                0x66FFFFFF, 0x33FFFFFF, SHADOW_SOFT);
        drawCenteredFittedText(guiGraphics, font, Component.literal("👁"),
                eyeBounds.left + eyeBounds.width / 2,
                eyeBounds.top + (eyeBounds.height - Math.round(font.lineHeight * 0.5F)) / 2,
                eyeBounds.width - 2, TEXT_LIGHT, 0.5F);
    }

    static void renderChatQrSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        UiRect contentBounds = screen.getChatSurfaceBounds();

        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.bottom(), PAGE_FILL);

        // Header (same style as the Add Friend page)
        int headerHeight = Math.max(24, Math.round(30 * screen.scale));
        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.top + headerHeight, 0xFFFFFFFF);
        guiGraphics.fill(contentBounds.left, contentBounds.top + headerHeight - 1, contentBounds.right(), contentBounds.top + headerHeight, 0x1A000000);

        UiRect backBounds = screen.getChatAddBackBtnBounds();
        drawCenteredFittedText(guiGraphics, font, Component.literal("<-"),
                backBounds.left + backBounds.width / 2,
                backBounds.top + (backBounds.height - Math.round(font.lineHeight * 0.55F)) / 2,
                backBounds.width, 0xFF0081BA, 0.55F);

        int centerX = contentBounds.left + contentBounds.width / 2;
        drawCenteredFittedText(guiGraphics, font, Component.translatable("phone.chat.qr.title").copy().withStyle(s -> s.withBold(true)),
                centerX,
                contentBounds.top + (headerHeight - Math.round(font.lineHeight * 0.55F)) / 2,
                contentBounds.width - 60, TEXT_PRIMARY, 0.55F);

        // White card with a decorative QR pattern derived from the phone number
        String ownNumber = screen.getOwnPhoneNumber();
        int cardSize = Math.min(contentBounds.width - Math.max(24, Math.round(30 * screen.scale)),
                Math.max(70, Math.round(96 * screen.scale)));
        int cardX = contentBounds.left + (contentBounds.width - cardSize) / 2;
        int cardY = contentBounds.top + headerHeight + Math.max(14, Math.round(18 * screen.scale));
        renderElevatedRoundedPanel(guiGraphics, cardX, cardY, cardSize, cardSize, CARD_BORDER, CARD_FILL, SHADOW_SOFT);

        int cells = 17;
        int quiet = Math.max(4, Math.round(6 * screen.scale));
        int cellSize = Math.max(2, (cardSize - quiet * 2) / cells);
        int gridSize = cellSize * cells;
        int gridX = cardX + (cardSize - gridSize) / 2;
        int gridY = cardY + (cardSize - gridSize) / 2;
        java.util.Random random = new java.util.Random(ownNumber.hashCode() * 31L + 7L);
        for (int row = 0; row < cells; row++) {
            for (int column = 0; column < cells; column++) {
                boolean finder = (row < 5 && column < 5) || (row < 5 && column >= cells - 5) || (row >= cells - 5 && column < 5);
                boolean dark;
                if (finder) {
                    int fr = row < 5 ? row : row - (cells - 5);
                    int fc = column < 5 ? column : column - (cells - 5);
                    dark = fr == 0 || fr == 4 || fc == 0 || fc == 4 || (fr >= 1 && fr <= 3 && fc >= 1 && fc <= 3 && fr != 1 && fr != 3 && fc != 1 && fc != 3)
                            || (fr == 2 && fc == 2);
                } else {
                    dark = random.nextBoolean();
                }
                if (dark) {
                    int x = gridX + column * cellSize;
                    int y = gridY + row * cellSize;
                    guiGraphics.fill(x, y, x + cellSize, y + cellSize, 0xFF111827);
                }
            }
        }

        // Own number + hint below the card
        int numberY = cardY + cardSize + Math.max(8, Math.round(10 * screen.scale));
        drawCenteredFittedText(guiGraphics, font, Component.literal(ownNumber).copy().withStyle(s -> s.withBold(true)),
                centerX, numberY, contentBounds.width - 20, 0xFF0081BA, 0.6F);

        drawCenteredFittedText(guiGraphics, font, Component.translatable("phone.chat.qr.hint"),
                centerX, numberY + Math.max(11, Math.round(13 * screen.scale)),
                contentBounds.width - Math.max(10, Math.round(14 * screen.scale)), TEXT_MUTED, 0.38F);

        // "Show to world" button — hold the phone up so a friend can scan you.
        UiRect showBtn = screen.getChatQrShowButtonBounds();
        renderElevatedRoundedPanel(guiGraphics, showBtn.left, showBtn.top, showBtn.width, showBtn.height,
                0xFF04B04B, 0xFF06C755, SHADOW_SOFT);
        drawCenteredFittedText(guiGraphics, font, Component.translatable("phone.chat.qr.show"),
                showBtn.left + showBtn.width / 2,
                showBtn.top + (showBtn.height - Math.round(font.lineHeight * 0.5F)) / 2,
                showBtn.width - 6, TEXT_LIGHT, 0.5F);
    }

    private static void drawCorner(GuiGraphics guiGraphics, int x, int y, int length,
                                   int thickness, boolean leftSide, boolean topSide, int color) {
        int horizontalX1 = leftSide ? x : x - length;
        int horizontalY1 = topSide ? y : y - thickness;
        guiGraphics.fill(horizontalX1, horizontalY1, horizontalX1 + length, horizontalY1 + thickness, color);

        int verticalX1 = leftSide ? x : x - thickness;
        int verticalY1 = topSide ? y : y - length;
        guiGraphics.fill(verticalX1, verticalY1, verticalX1 + thickness, verticalY1 + length, color);
    }
}
