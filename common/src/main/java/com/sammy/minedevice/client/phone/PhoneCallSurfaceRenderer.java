package com.sammy.minedevice.client.phone;

import com.sammy.minedevice.phone.PhoneContact;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

final class PhoneCallSurfaceRenderer {
    private PhoneCallSurfaceRenderer() {
    }

    static void renderCallAppSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        UiRect contentBounds = screen.getCallSurfaceBounds();
        UiRect numberBounds = screen.getCallNumberDisplayBounds();
        String ownNumber = screen.getOwnPhoneNumber();
        int headerHeight = screen.getCallHeaderHeight();
        int rowTop = contentBounds.top + Math.max(5, Math.round(6 * screen.scale));
        int rowHeight = Math.max(14, Math.round(16 * screen.scale));
        int chipPadding = Math.max(4, Math.round(5 * screen.scale));
        int ownChipWidth = getOwnNumberChipWidth(minecraft.font, ownNumber, chipPadding, screen.scale);
        int ownChipX = contentBounds.right() - ownChipWidth;
        int titlePaddingLeft = Math.max(5, Math.round(7 * screen.scale));

        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.bottom(), 0x99FFFFFF);
        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.top + headerHeight, 0xDEF9F9FB);

        Component titleText = Component.translatable("screen.minedevice.phone.call.title");
        renderOwnNumberChip(guiGraphics, minecraft.font, ownNumber, ownChipX, rowTop, ownChipWidth, rowHeight, screen.scale);

        int titleMaxWidth = Math.max(24, ownChipX - contentBounds.left - titlePaddingLeft - Math.round(8 * screen.scale));
        float titleScale = PhoneScreenDraw.textScaleToFit(minecraft.font, titleText, titleMaxWidth, 0.35F);
        int titleHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, titleScale);
        int titleX = contentBounds.left + titlePaddingLeft;
        int titleY = rowTop + Math.max(0, (rowHeight - titleHeight) / 2);
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, titleText, titleX, titleY, 0xFF000000, false, titleScale);

        guiGraphics.fill(numberBounds.left, numberBounds.top, numberBounds.right(), numberBounds.bottom(), 0xFFEAEBEE);
        guiGraphics.fill(numberBounds.left + 1, numberBounds.top + 1, numberBounds.right() - 1, numberBounds.bottom() - 1, 0xFFF2F2F7);

        Component numberText = screen.dialedNumber.isEmpty()
                ? Component.translatable("screen.minedevice.phone.call.placeholder")
                : Component.literal(screen.dialedNumber);
        float numberScale = PhoneScreenDraw.textScaleToFit(minecraft.font, numberText, numberBounds.width - Math.round(10 * screen.scale));
        int numberTextWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, numberText, numberScale);
        int numberTextHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, numberScale);
        int numberTextX = numberBounds.left + (numberBounds.width - numberTextWidth) / 2;
        int numberTextY = numberBounds.top + (numberBounds.height - numberTextHeight) / 2;
        int numberTextColor = screen.dialedNumber.isEmpty() ? 0xFF8E8E93 : 0xFF000000;
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, numberText, numberTextX, numberTextY,
                numberTextColor, false, numberScale);

        for (int i = 0; i < PhoneScreen.CALL_DIAL_DIGITS.length; i++) {
            String digit = PhoneScreen.CALL_DIAL_DIGITS[i];
            if (digit.isEmpty()) {
                continue;
            }

            UiRect buttonBounds = screen.getDialPadCellBounds(i);
            renderDialPadButton(guiGraphics, minecraft.font, buttonBounds, Component.literal(digit),
                    0xFFEAEBEE, 0xFFFFFFFF, 0xFF000000, screen.scale);
        }

        renderDialPadButton(guiGraphics, minecraft.font, screen.getDialDeleteButtonBounds(),
                Component.translatable("screen.minedevice.phone.call.action.delete"),
                0xFFEAEBEE, 0xFFFFFFFF, 0xFFFF3B30, screen.scale);
        renderDialPadButton(guiGraphics, minecraft.font, screen.getDialCallButtonBounds(),
                Component.translatable("screen.minedevice.phone.call.action.call"),
                screen.dialedNumber.isEmpty() ? 0xFFD1D1D6 : 0xFF34C759,
                screen.dialedNumber.isEmpty() ? 0xFFE5E5EA : 0xFF4CD964,
                0xFF000000, screen.scale);
        renderCallMenuTabs(screen, guiGraphics, true);
    }

    static void renderCallContactsSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        UiRect contentBounds = screen.getCallSurfaceBounds();
        String ownNumber = screen.getOwnPhoneNumber();
        int headerHeight = screen.getCallHeaderHeight();
        int rowTop = contentBounds.top + Math.max(5, Math.round(6 * screen.scale));
        int rowHeight = Math.max(14, Math.round(16 * screen.scale));
        int chipPadding = Math.max(4, Math.round(5 * screen.scale));
        int ownChipWidth = getOwnNumberChipWidth(minecraft.font, ownNumber, chipPadding, screen.scale);
        int ownChipX = contentBounds.right() - ownChipWidth;
        int titlePaddingLeft = Math.max(5, Math.round(7 * screen.scale));
        int panelTop = contentBounds.top + headerHeight + Math.max(4, Math.round(5 * screen.scale));
        int panelBottom = screen.getCallDialMenuBounds().top - screen.getCallMenuBottomReserve();
        UiRect saveButtonBounds = screen.getContactSaveButtonBounds();
        String saveCandidateNumber = screen.getContactSaveCandidateNumber();
        boolean canSaveNumber = !saveCandidateNumber.isEmpty() && !screen.hasSavedContact(saveCandidateNumber);
        List<PhoneContact> contacts = screen.getPhoneContacts();

        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.bottom(), 0x99FFFFFF);
        guiGraphics.fill(contentBounds.left, contentBounds.top, contentBounds.right(), contentBounds.top + headerHeight, 0xDEF9F9FB);

        Component titleText = Component.translatable("screen.minedevice.phone.call.contacts.title");
        renderOwnNumberChip(guiGraphics, minecraft.font, ownNumber, ownChipX, rowTop, ownChipWidth, rowHeight, screen.scale);

        int titleMaxWidth = Math.max(24, ownChipX - contentBounds.left - titlePaddingLeft - Math.round(8 * screen.scale));
        float titleScale = PhoneScreenDraw.textScaleToFit(minecraft.font, titleText, titleMaxWidth, 0.35F);
        int titleHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, titleScale);
        int titleX = contentBounds.left + titlePaddingLeft;
        int titleY = rowTop + Math.max(0, (rowHeight - titleHeight) / 2);
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, titleText, titleX, titleY, 0xFF000000, false, titleScale);

        UiRect addButtonBounds = screen.getAddContactHeaderButtonBounds();
        guiGraphics.fill(addButtonBounds.left, addButtonBounds.top, addButtonBounds.right(), addButtonBounds.bottom(), 0xFFEAEBEE);
        guiGraphics.fill(addButtonBounds.left + 1, addButtonBounds.top + 1, addButtonBounds.right() - 1, addButtonBounds.bottom() - 1, 0xFFFFFFFF);
        Component addLabel = Component.translatable("screen.minedevice.phone.call.contacts.add_label");
        float addLabelScale = PhoneScreenDraw.textScaleToFit(minecraft.font, addLabel,
                addButtonBounds.width - Math.max(20, Math.round(24 * screen.scale)), 0.35F);
        int addLabelWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, addLabel, addLabelScale);
        int addLabelHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, addLabelScale);
        int addLabelX = addButtonBounds.left + Math.max(10, Math.round(12 * screen.scale));
        int addLabelY = addButtonBounds.top + (addButtonBounds.height - addLabelHeight) / 2;
        Component addIcon = Component.literal("+");
        float addIconScale = PhoneScreenDraw.textScaleToFit(minecraft.font, addIcon,
                addButtonBounds.height - Math.max(4, Math.round(6 * screen.scale)), 0.40F);
        int addIconWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, addIcon, addIconScale);
        int addIconHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, addIconScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, addLabel, addLabelX, addLabelY, 0xFF007AFF, false, addLabelScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, addIcon,
                addButtonBounds.right() - addIconWidth - Math.max(10, Math.round(12 * screen.scale)),
                addButtonBounds.top + (addButtonBounds.height - addIconHeight) / 2,
                0xFF007AFF, false, addIconScale);

        if (canSaveNumber) {
            guiGraphics.fill(saveButtonBounds.left, saveButtonBounds.top, saveButtonBounds.right(), saveButtonBounds.bottom(), 0xFFEAEBEE);
            guiGraphics.fill(saveButtonBounds.left + 1, saveButtonBounds.top + 1,
                    saveButtonBounds.right() - 1, saveButtonBounds.bottom() - 1, 0xFFFFFFFF);
            Component saveText = Component.translatable("screen.minedevice.phone.call.contacts.save_current", saveCandidateNumber);
            float saveScale = PhoneScreenDraw.textScaleToFit(minecraft.font, saveText,
                    saveButtonBounds.width - Math.round(10 * screen.scale), 0.35F);
            int saveTextWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, saveText, saveScale);
            int saveTextHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, saveScale);
            int saveTextX = saveButtonBounds.left + (saveButtonBounds.width - saveTextWidth) / 2;
            int saveTextY = saveButtonBounds.top + (saveButtonBounds.height - saveTextHeight) / 2;
            PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, saveText, saveTextX, saveTextY, 0xFF007AFF, false, saveScale);
        }

        if (contacts.isEmpty()) {
            Component emptyText = Component.translatable("screen.minedevice.phone.call.contacts.empty");
            Component hintText = canSaveNumber
                    ? Component.translatable("screen.minedevice.phone.call.contacts.hint_save")
                    : Component.translatable("screen.minedevice.phone.call.contacts.hint");
            int textAreaLeft = contentBounds.left + Math.max(7, Math.round(8 * screen.scale));
            int textAreaRight = contentBounds.right() - Math.max(7, Math.round(8 * screen.scale));
            int textAreaWidth = textAreaRight - textAreaLeft;
            int emptyAreaTop = canSaveNumber ? saveButtonBounds.bottom() + Math.max(5, Math.round(6 * screen.scale)) : panelTop;
            int emptyAreaHeight = panelBottom - emptyAreaTop;
            float emptyScale = PhoneScreenDraw.textScaleToFit(minecraft.font, emptyText,
                    textAreaWidth, 0.35F);
            float hintScale = PhoneScreenDraw.textScaleToFit(minecraft.font, hintText,
                    textAreaWidth, 0.30F);
            int emptyWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, emptyText, emptyScale);
            int hintWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, hintText, hintScale);
            int emptyX = textAreaLeft + ((textAreaWidth - emptyWidth) / 2);
            int emptyY = emptyAreaTop + (emptyAreaHeight / 2) - Math.max(8, Math.round(9 * screen.scale));
            int hintX = textAreaLeft + ((textAreaWidth - hintWidth) / 2);
            int hintY = emptyY + PhoneScreenDraw.scaledTextHeight(minecraft.font, emptyScale) + Math.max(4, Math.round(5 * screen.scale));
            PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, emptyText, emptyX, emptyY, 0xFF000000, false, emptyScale);
            PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, hintText, hintX, hintY, 0xFF8E8E93, false, hintScale);
        } else {
            for (int index = 0; index < contacts.size(); index++) {
                PhoneContact contact = contacts.get(index);
                UiRect rowBounds = screen.getContactRowBounds(index, contacts.size());
                UiRect deleteBounds = screen.getContactDeleteButtonBounds(index, contacts.size());
                UiRect editBounds = screen.getContactEditButtonBounds(index, contacts.size());
                guiGraphics.fill(rowBounds.left, rowBounds.top, rowBounds.right(), rowBounds.bottom(), 0xFFEAEBEE);
                guiGraphics.fill(rowBounds.left + 1, rowBounds.top + 1, rowBounds.right() - 1, rowBounds.bottom() - 1, 0xFFEAEBEE);

                Component nameText = Component.literal(contact.displayName());
                Component numberText = Component.literal(contact.number());
                Component lineText = contact.displayName().equals(contact.number())
                        ? numberText : nameText;
                int textLeft = rowBounds.left + Math.max(5, Math.round(6 * screen.scale));
                int textRight = editBounds.left - Math.max(4, Math.round(6 * screen.scale));
                int textMaxWidth = Math.max(18, textRight - textLeft);
                boolean showNumberOnly = contact.displayName().equals(contact.number());
                float lineScale = PhoneScreenDraw.textScaleToFit(minecraft.font, lineText, textMaxWidth, showNumberOnly ? 0.30F : 0.35F);
                int lineHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, lineScale);
                PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, lineText, textLeft,
                        rowBounds.top + (rowBounds.height - lineHeight) / 2,
                        showNumberOnly ? 0xFF8E8E93 : 0xFF000000, false, lineScale);

                guiGraphics.blit(PhoneScreen.DELETE_BUTTON_TEXTURE, deleteBounds.left, deleteBounds.top,
                        deleteBounds.width, deleteBounds.height, 0.0F, 0.0F, 24, 24, 24, 24);

                guiGraphics.fill(editBounds.left, editBounds.top, editBounds.right(), editBounds.bottom(), 0xFF007AFF);
                guiGraphics.fill(editBounds.left + 1, editBounds.top + 1, editBounds.right() - 1, editBounds.bottom() - 1, 0xFF007AFF);
                Component editIcon = Component.literal("✎");
                float editScale = PhoneScreenDraw.textScaleToFit(minecraft.font, editIcon,
                        editBounds.width - Math.max(2, Math.round(3 * screen.scale)), 0.35F);
                int editIconWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, editIcon, editScale);
                int editIconHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, editScale);
                PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, editIcon,
                        editBounds.left + (editBounds.width - editIconWidth) / 2,
                        editBounds.top + (editBounds.height - editIconHeight) / 2,
                        0xFFFFFFFF, false, editScale);
            }
        }

        renderCallMenuTabs(screen, guiGraphics, false);
    }

    static void renderCallSessionSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (screen.activeCallNumber.isEmpty()) {
            screen.endCallSession(true);
            return;
        }

        UiRect contentBounds = screen.getCallSurfaceBounds();
        int numberInsetX = Math.max(7, Math.round(9 * screen.scale));
        
        int statusY = contentBounds.top + Math.max(24, Math.round(32 * screen.scale));
        int nameBlockTop = statusY + Math.max(24, Math.round(28 * screen.scale));
        UiRect sessionNumberBounds = new UiRect(
                contentBounds.left + numberInsetX,
                nameBlockTop,
                Math.max(24, contentBounds.width - (numberInsetX * 2)),
                Math.max(30, Math.round(44 * screen.scale)));
        int subY = sessionNumberBounds.bottom() + Math.max(6, Math.round(8 * screen.scale));

        Component statusText = Component.translatable(screen.activeCallConnected
                ? "screen.minedevice.phone.call.status.connected"
                : screen.activeCallMissed
                ? "screen.minedevice.phone.call.status.no_answer_short"
                : screen.activeCallIncoming
                ? "screen.minedevice.phone.call.status.incoming"
                : "screen.minedevice.phone.call.status.dialing");
        int textHorizontalInset = Math.max(6, Math.round(8 * screen.scale));
        int sessionTextMaxWidth = Math.max(18, contentBounds.width - (textHorizontalInset * 2));
        drawCenteredFittedText(guiGraphics, minecraft.font, statusText,
                contentBounds.left + (contentBounds.width / 2), statusY, sessionTextMaxWidth,
                0xFF000000, false, 0.35F);

        String activeName = screen.activeCallName == null ? "" : screen.activeCallName.strip();
        String activeNumber = screen.activeCallNumber == null ? "" : screen.activeCallNumber.strip();
        boolean showSubNumber = !activeName.isBlank() && !activeName.equals(activeNumber);
        Component numberText = Component.literal(activeName.isBlank() ? activeNumber : activeName);
        float numberScale = PhoneScreenDraw.textScaleToFit(minecraft.font, numberText,
                sessionNumberBounds.width - Math.round(10 * screen.scale), 0.40F);
        int numberWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, numberText, numberScale);
        int numberHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, numberScale);
        int numberX = sessionNumberBounds.left + (sessionNumberBounds.width - numberWidth) / 2;
        int lineGap = Math.max(6, Math.round(8 * screen.scale));
        int subNumberHeight = 0;
        float subNumberScale = 0.0F;
        Component subNumberText = null;
        if (showSubNumber) {
            subNumberText = Component.literal(activeNumber);
            subNumberScale = PhoneScreenDraw.textScaleToFit(minecraft.font, subNumberText,
                    sessionNumberBounds.width - Math.round(12 * screen.scale), 0.35F);
            subNumberHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, subNumberScale);
        }
        int textGroupHeight = numberHeight + (showSubNumber ? lineGap + subNumberHeight : 0);
        int numberY = sessionNumberBounds.top + Math.max(0, (sessionNumberBounds.height - textGroupHeight) / 2);
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, numberText, numberX, numberY, 0xFF000000, false, numberScale);

        if (showSubNumber) {
            int subNumberWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, subNumberText, subNumberScale);
            int subNumberX = sessionNumberBounds.left + (sessionNumberBounds.width - subNumberWidth) / 2;
            int subNumberY = numberY + numberHeight + lineGap;
            PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, subNumberText, subNumberX, subNumberY,
                    0xFF8E8E93, false, subNumberScale);
        }

        Component subText = screen.activeCallConnected
                ? Component.literal(screen.formatCallDuration())
                : screen.activeCallMissed
                ? Component.translatable("screen.minedevice.phone.call.status.no_answer_detail")
                : screen.activeCallIncoming
                ? Component.translatable("screen.minedevice.phone.call.status.tap_connect")
                : Component.translatable("screen.minedevice.phone.call.status.waiting_answer");
        drawCenteredFittedText(guiGraphics, minecraft.font, subText,
                contentBounds.left + (contentBounds.width / 2), subY, sessionTextMaxWidth,
                0xFF8E8E93, false, 0.35F);

        UiRect connectButtonBounds = screen.getCallConnectButtonBounds();
        UiRect hangupButtonBounds = screen.getCallHangupButtonBounds();

        if (screen.activeCallIncoming && !screen.activeCallConnected) {
            int gap = Math.max(5, Math.round(6 * screen.scale));
            int incomingHangupX = connectButtonBounds.right() + gap;
            guiGraphics.blit(PhoneScreen.ANSWER_BUTTON_TEXTURE, 
                    connectButtonBounds.left, connectButtonBounds.top,
                    connectButtonBounds.width, connectButtonBounds.height, 
                    0.0F, 0.0F, 24, 24, 24, 24);
            guiGraphics.blit(PhoneScreen.HANGUP_BUTTON_TEXTURE, 
                    incomingHangupX, connectButtonBounds.top,
                    connectButtonBounds.width, connectButtonBounds.height, 
                    0.0F, 0.0F, 24, 24, 24, 24);
        } else {
            guiGraphics.blit(PhoneScreen.HANGUP_BUTTON_TEXTURE, 
                    hangupButtonBounds.left, hangupButtonBounds.top,
                    hangupButtonBounds.width, hangupButtonBounds.height, 
                    0.0F, 0.0F, 24, 24, 24, 24);
        }

        if (screen.activeCallConnected) {
            UiRect muteBounds = screen.getCallMuteButtonBounds();
            UiRect speakerBounds = screen.getCallSpeakerButtonBounds();
            renderCallActionButton(guiGraphics, muteBounds, "M",
                    screen.callMuted ? 0xFFFF3B30 : 0xFFF2F2F7,
                    screen.callMuted ? 0xFFE6352B : 0xFFEAEBEE,
                    screen.callMuted ? 0xFFFFFFFF : 0xFF8E8E93);
            renderCallActionButton(guiGraphics, speakerBounds, "S",
                    screen.callSpeakerEnabled ? 0xFF007AFF : 0xFFF2F2F7,
                    screen.callSpeakerEnabled ? 0xFF006AD6 : 0xFFEAEBEE,
                    screen.callSpeakerEnabled ? 0xFFFFFFFF : 0xFF8E8E93);
        }
    }

    private static void renderCallActionButton(GuiGraphics guiGraphics, UiRect bounds, String label,
                                                 int fillColor, int borderColor, int textColor) {
        guiGraphics.fill(bounds.left, bounds.top, bounds.right(), bounds.bottom(), borderColor);
        guiGraphics.fill(bounds.left + 1, bounds.top + 1, bounds.right() - 1, bounds.bottom() - 1, fillColor);
        Font font = Minecraft.getInstance().font;
        int textX = bounds.left + (bounds.width - font.width(label)) / 2;
        int textY = bounds.top + (bounds.height - font.lineHeight) / 2;
        guiGraphics.drawString(font, label, textX, textY, textColor, false);
    }

    static void renderCallBackdrop(PhoneScreen screen, GuiGraphics guiGraphics) {
        UiRect backdropBounds = screen.getCallBackdropBounds();
        if (screen.callSessionMode) {
            guiGraphics.blit(PhoneScreen.DEFAULT_BACKGROUND_TEXTURE, backdropBounds.left, backdropBounds.top,
                    0.0F, 0.0F, backdropBounds.width, backdropBounds.height,
                    PhoneScreen.DISPLAY_WIDTH, PhoneScreen.DISPLAY_HEIGHT);
            guiGraphics.fill(backdropBounds.left, backdropBounds.top, backdropBounds.right(), backdropBounds.bottom(), 0xE6FFFFFF);
            return;
        }

        UiRect numberBounds = screen.getCallNumberDisplayBounds();
        int topBandBottom = numberBounds.top - Math.max(6, Math.round(8 * screen.scale));
        int accentInset = Math.max(10, Math.round(12 * screen.scale));
        int accentTop = backdropBounds.top + Math.max(10, Math.round(12 * screen.scale));
        int accentBottom = accentTop + Math.max(24, Math.round(30 * screen.scale));
        int footerShadeHeight = Math.max(48, Math.round(58 * screen.scale));

        guiGraphics.blit(PhoneScreen.DEFAULT_BACKGROUND_TEXTURE, backdropBounds.left, backdropBounds.top,
                0.0F, 0.0F, backdropBounds.width, backdropBounds.height,
                PhoneScreen.DISPLAY_WIDTH, PhoneScreen.DISPLAY_HEIGHT);
        guiGraphics.fill(backdropBounds.left, backdropBounds.top, backdropBounds.right(), backdropBounds.bottom(), 0xE6FFFFFF);
        guiGraphics.fill(backdropBounds.left, backdropBounds.top, backdropBounds.right(), topBandBottom, 0xA7ECECF1);
        guiGraphics.fill(backdropBounds.left + accentInset, accentTop,
                backdropBounds.right() - accentInset, accentBottom, 0x11000000);
        guiGraphics.fill(backdropBounds.left, backdropBounds.bottom() - footerShadeHeight,
                backdropBounds.right(), backdropBounds.bottom(), 0xAAFFFFFF);
    }

    static void renderMediaBackdrop(PhoneScreen screen, GuiGraphics guiGraphics) {
        UiRect backdropBounds = screen.getMediaBackdropBounds();
        if (screen.photoViewerMode) {
            guiGraphics.fill(backdropBounds.left, backdropBounds.top, backdropBounds.right(), backdropBounds.bottom(), 0xFF000000);
            return;
        }
        int topBandHeight = Math.max(34, Math.round(40 * screen.scale));
        int bottomBandHeight = Math.max(24, Math.round(30 * screen.scale));
        int accentInset = Math.max(8, Math.round(10 * screen.scale));
        int accentTop = backdropBounds.top + Math.max(10, Math.round(12 * screen.scale));
        int accentBottom = accentTop + Math.max(24, Math.round(28 * screen.scale));

        guiGraphics.blit(PhoneScreen.DEFAULT_BACKGROUND_TEXTURE, backdropBounds.left, backdropBounds.top,
                0.0F, 0.0F, backdropBounds.width, backdropBounds.height,
                PhoneScreen.DISPLAY_WIDTH, PhoneScreen.DISPLAY_HEIGHT);
        guiGraphics.fill(backdropBounds.left, backdropBounds.top, backdropBounds.right(), backdropBounds.bottom(), 0xDCFFFFFF);
        guiGraphics.fill(backdropBounds.left, backdropBounds.top,
                backdropBounds.right(), backdropBounds.top + topBandHeight, 0xB7ECECF1);
        guiGraphics.fill(backdropBounds.left, backdropBounds.bottom() - bottomBandHeight,
                backdropBounds.right(), backdropBounds.bottom(), 0xEEFFFFFF);
        guiGraphics.fill(backdropBounds.left + accentInset, accentTop,
                backdropBounds.right() - accentInset, accentBottom, 0x11000000);
    }

    private static void renderCallMenuTabs(PhoneScreen screen, GuiGraphics guiGraphics, boolean dialPageActive) {
        UiRect dialBounds = screen.getCallDialMenuBounds();
        UiRect listBounds = screen.getCallListMenuBounds();
        renderCallMenuTab(screen, guiGraphics, dialBounds, PhoneScreen.CALL_MENU_TEXTURE, dialPageActive);
        renderCallMenuTab(screen, guiGraphics, listBounds, PhoneScreen.LIST_MENU_TEXTURE, !dialPageActive);
    }

    private static void renderCallMenuTab(PhoneScreen screen, GuiGraphics guiGraphics,
                                          UiRect bounds, ResourceLocation texture, boolean active) {
        guiGraphics.blit(texture, bounds.left, bounds.top, bounds.width, bounds.height,
                0.0F, 0.0F, 24, 24, 24, 24);
        if (active) {
            int underlineInset = Math.max(2, Math.round(4 * screen.scale));
            int underlineHeight = Math.max(1, Math.round(2 * screen.scale));
            int underlineY = bounds.bottom() + Math.max(1, Math.round(2 * screen.scale));
            guiGraphics.fill(bounds.left + underlineInset, underlineY,
                    bounds.right() - underlineInset, underlineY + underlineHeight, 0xFF007AFF);
        }
    }

    static void renderAddContactSurface(PhoneScreen screen, GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        UiRect contentBounds = screen.getCallSurfaceBounds();
        UiRect popupBounds = screen.getAddContactPopupBounds();
        UiRect nameBounds = screen.getAddContactNameBounds();
        UiRect numberBounds = screen.getAddContactNumberBounds();
        UiRect saveBounds = screen.getAddContactSaveButtonBounds();
        UiRect cancelBounds = screen.getAddContactCancelButtonBounds();

        UiRect backdropBounds = screen.getCallBackdropBounds();
        guiGraphics.fill(backdropBounds.left, backdropBounds.top, backdropBounds.right(), backdropBounds.bottom(), 0x80000000);

        int popupCornerRadius = Math.max(8, Math.round(10 * screen.scale));
        guiGraphics.fill(popupBounds.left, popupBounds.top, popupBounds.right(), popupBounds.bottom(), 0xFFEAEBEE);
        guiGraphics.fill(popupBounds.left + 1, popupBounds.top + 1, popupBounds.right() - 1, popupBounds.bottom() - 1, 0xFFFFFFFF);

        int innerInset = Math.max(8, Math.round(10 * screen.scale));
        int titleHeight = Math.max(18, Math.round(22 * screen.scale));
        int topPadding = Math.max(12, Math.round(14 * screen.scale));
        int titleX = popupBounds.left + innerInset;
        int titleY = popupBounds.top + topPadding;
        Component titleText = screen.editingContactOriginalNumber != null
                ? Component.translatable("screen.minedevice.phone.call.contacts.edit_title")
                : Component.translatable("screen.minedevice.phone.call.contacts.add_title");
        float titleScale = PhoneScreenDraw.textScaleToFit(minecraft.font, titleText,
                popupBounds.width - (innerInset * 2), 0.40F);
        int titleTextHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, titleScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, titleText, titleX, titleY + (titleHeight - titleTextHeight) / 2, 0xFF000000, false, titleScale);

        guiGraphics.fill(nameBounds.left, nameBounds.top, nameBounds.right(), nameBounds.bottom(), 0xFFC7C7CC);
        guiGraphics.fill(nameBounds.left + 1, nameBounds.top + 1, nameBounds.right() - 1, nameBounds.bottom() - 1, 0xFFFFFFFF);
        String displayName = screen.addContactName.isEmpty() ? "" : screen.addContactName;
        boolean nameActive = screen.addContactNameFieldFocused;
        Component nameText = nameActive ? Component.literal(displayName + "_") : (displayName.isEmpty() ? Component.translatable("screen.minedevice.phone.call.contacts.name_label") : Component.literal(displayName));
        int nameFieldInset = Math.max(4, Math.round(5 * screen.scale));
        float nameFieldScale = PhoneScreenDraw.textScaleToFit(minecraft.font, nameText,
                nameBounds.width - (nameFieldInset * 2), 0.30F);
        int nameTextHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, nameFieldScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, nameText,
                nameBounds.left + nameFieldInset, nameBounds.top + (nameBounds.height - nameTextHeight) / 2,
                displayName.isEmpty() && !nameActive ? 0xFF8E8E93 : 0xFF000000, false, nameFieldScale);

        guiGraphics.fill(numberBounds.left, numberBounds.top, numberBounds.right(), numberBounds.bottom(), 0xFFC7C7CC);
        guiGraphics.fill(numberBounds.left + 1, numberBounds.top + 1, numberBounds.right() - 1, numberBounds.bottom() - 1, 0xFFFFFFFF);
        String displayNumber = screen.addContactNumber.isEmpty() ? "" : screen.addContactNumber;
        boolean numberActive = !nameActive;
        Component numberText = numberActive ? Component.literal(displayNumber + "_") : (displayNumber.isEmpty() ? Component.translatable("screen.minedevice.phone.call.contacts.number_label") : Component.literal(displayNumber));
        float numberFieldScale = PhoneScreenDraw.textScaleToFit(minecraft.font, numberText,
                numberBounds.width - (nameFieldInset * 2), 0.30F);
        int numberTextHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, numberFieldScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, numberText,
                numberBounds.left + nameFieldInset, numberBounds.top + (numberBounds.height - numberTextHeight) / 2,
                displayNumber.isEmpty() && !numberActive ? 0xFF8E8E93 : 0xFF000000, false, numberFieldScale);

        boolean canSave = !screen.addContactNumber.isEmpty();
        int saveFill = canSave ? 0xFF34C759 : 0xFFD1D1D6;
        int saveBorder = canSave ? 0xFF4CD964 : 0xFFE5E5EA;
        guiGraphics.fill(saveBounds.left, saveBounds.top, saveBounds.right(), saveBounds.bottom(), saveBorder);
        guiGraphics.fill(saveBounds.left + 1, saveBounds.top + 1, saveBounds.right() - 1, saveBounds.bottom() - 1, saveFill);
        Component saveText = Component.translatable("screen.minedevice.phone.call.contacts.save_button");
        float saveScale = PhoneScreenDraw.textScaleToFit(minecraft.font, saveText,
                saveBounds.width - Math.round(8 * screen.scale), 0.35F);
        int saveTextWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, saveText, saveScale);
        int saveTextHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, saveScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, saveText,
                saveBounds.left + (saveBounds.width - saveTextWidth) / 2,
                saveBounds.top + (saveBounds.height - saveTextHeight) / 2,
                0xFFFFFFFF, false, saveScale);

        guiGraphics.fill(cancelBounds.left, cancelBounds.top, cancelBounds.right(), cancelBounds.bottom(), 0xFFEAEBEE);
        guiGraphics.fill(cancelBounds.left + 1, cancelBounds.top + 1, cancelBounds.right() - 1, cancelBounds.bottom() - 1, 0xFFFFFFFF);
        Component cancelText = Component.translatable("screen.minedevice.phone.call.contacts.cancel_button");
        float cancelScale = PhoneScreenDraw.textScaleToFit(minecraft.font, cancelText,
                cancelBounds.width - Math.round(8 * screen.scale), 0.35F);
        int cancelTextWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, cancelText, cancelScale);
        int cancelTextHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, cancelScale);
        PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, cancelText,
                cancelBounds.left + (cancelBounds.width - cancelTextWidth) / 2,
                cancelBounds.top + (cancelBounds.height - cancelTextHeight) / 2,
                0xFF007AFF, false, cancelScale);
    }

    private static void renderDialPadButton(GuiGraphics guiGraphics, Font font, UiRect bounds, Component text,
                                            int fillColor, int borderColor, int textColor, float scale) {
        guiGraphics.fill(bounds.left, bounds.top, bounds.right(), bounds.bottom(), borderColor);
        guiGraphics.fill(bounds.left + 1, bounds.top + 1, bounds.right() - 1, bounds.bottom() - 1, fillColor);

        float textScale = PhoneScreenDraw.textScaleToFit(font, text, bounds.width - Math.round(8 * scale), 0.30F);
        int textWidth = PhoneScreenDraw.scaledTextWidth(font, text, textScale);
        int textHeight = PhoneScreenDraw.scaledTextHeight(font, textScale);
        int textX = bounds.left + (bounds.width - textWidth) / 2;
        int textY = bounds.top + (bounds.height - textHeight) / 2;
        PhoneScreenDraw.drawScaledText(guiGraphics, font, text, textX, textY, textColor, false, textScale);
    }

    private static int getOwnNumberChipWidth(Font font, String ownNumber, int chipPadding, float scale) {
        return Math.max(Math.round(30 * scale), font.width(ownNumber) + (chipPadding * 2));
    }

    private static void renderOwnNumberChip(GuiGraphics guiGraphics, Font font, String ownNumber,
                                            int chipX, int chipY, int chipWidth, int chipHeight, float scale) {
        Component numberText = Component.literal(ownNumber);
        int innerPaddingX = Math.max(2, Math.round(3 * scale));
        int availableWidth = Math.max(1, chipWidth - (innerPaddingX * 2));
        int availableHeight = Math.max(1, chipHeight);
        float textScale = Math.min(
                PhoneScreenDraw.textScaleToFit(font, numberText, availableWidth, 0.35F),
                Math.min(1.0F, (float) availableHeight / Math.max(1, font.lineHeight))
        );
        int textWidth = PhoneScreenDraw.scaledTextWidth(font, numberText, textScale);
        int textHeight = PhoneScreenDraw.scaledTextHeight(font, textScale);
        int textX = chipX + (chipWidth - textWidth) / 2;
        int textY = chipY + (chipHeight - textHeight) / 2;
        PhoneScreenDraw.drawScaledText(guiGraphics, font, numberText, textX, textY, 0xFF007AFF, false, textScale);
    }

    private static void drawCenteredFittedText(GuiGraphics guiGraphics, Font font, Component text,
                                               int centerX, int topY, int maxWidth, int color,
                                               boolean shadow, float minScale) {
        drawCenteredFittedText(guiGraphics, font, text, centerX, topY, maxWidth, color, shadow, minScale, 1.0F);
    }

    private static void drawCenteredFittedText(GuiGraphics guiGraphics, Font font, Component text,
                                               int centerX, int topY, int maxWidth, int color,
                                               boolean shadow, float minScale, float maxScale) {
        if (maxWidth <= 0) {
            return;
        }

        float textScale = Mth.clamp(PhoneScreenDraw.textScaleToFit(font, text, maxWidth, minScale), minScale, maxScale);
        int maxUnscaledWidth = Math.max(1, Mth.floor(maxWidth / Math.max(0.01F, textScale)));
        Component fittedText = clipToWidth(font, text, maxUnscaledWidth);
        int textWidth = PhoneScreenDraw.scaledTextWidth(font, fittedText, textScale);
        int textX = centerX - (textWidth / 2);
        PhoneScreenDraw.drawScaledText(guiGraphics, font, fittedText, textX, topY, color, shadow, textScale);
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
}
