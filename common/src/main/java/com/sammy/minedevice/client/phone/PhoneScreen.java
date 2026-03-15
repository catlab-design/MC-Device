package com.sammy.minedevice.client.phone;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.sammy.minedevice.Minedevice;
import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.block.entity.HomePhoneBlockEntity;
import com.sammy.minedevice.phone.PhoneCallState;
import com.sammy.minedevice.phone.PhoneChatData;
import com.sammy.minedevice.phone.PhoneChatMessage;
import com.sammy.minedevice.phone.PhoneContact;
import com.sammy.minedevice.phone.PhoneData;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PhoneScreen extends Screen {
    private static final ResourceLocation FRAME_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_frame.png");
    private static final ResourceLocation UNLOCK_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_unlock.png");
    private static final ResourceLocation LEFT_NAV_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_nav_left.png");
    private static final ResourceLocation RIGHT_NAV_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_nav_right.png");
    private static final ResourceLocation HOME_NAV_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_nav_home.png");
    private static final ResourceLocation APP_CALL_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_app_call.png");
    private static final ResourceLocation APP_CHAT_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_app_chat.png");
    private static final ResourceLocation APP_SHOP_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_app_shop.png");
    private static final ResourceLocation APP_GOOGLE_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_app_setting.png");
    private static final ResourceLocation APP_GALLERY_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_app_gallery.png");
    private static final ResourceLocation APP_BANK_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_app_bank.png");
    private static final ResourceLocation APP_CAMERA_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_app_camera.png");
    static final ResourceLocation CALL_MENU_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/call_menu.png");
    static final ResourceLocation LIST_MENU_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/list_menu.png");
    private static final ResourceLocation SHUTTER_BUTTON_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/shutter_button.png");
    private static final ResourceLocation CAMFLIP_BUTTON_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/camflip_button.png");
    static final ResourceLocation DELETE_BUTTON_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/delete_button.png");
    static final ResourceLocation DEFAULT_BACKGROUND_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/wallpaper/default.png");
    private static final int FRAME_WIDTH = 160;
    private static final int FRAME_HEIGHT = 336;
    private static final float FRAME_SCALE = 0.85F;
    private static final int DISPLAY_X = 12;
    private static final int DISPLAY_Y = 24;
    static final int DISPLAY_WIDTH = 136;
    static final int DISPLAY_HEIGHT = 292;
    private static final int CAMERA_VIEW_X = 9;
    private static final int CAMERA_VIEW_Y = 27;
    private static final int CAMERA_VIEW_WIDTH = 142;
    private static final int CAMERA_VIEW_HEIGHT = 266;
    private static final int CAMERA_VIEW_SIDE_TRIM = 3;
    private static final int ICON_SIZE = 30;
    private static final int ICON_TEXTURE_SIZE = 30;
    static final int GALLERY_COLUMNS = 3;
    private static final int GALLERY_ROWS = 4;
    static final int GALLERY_PAGE_SIZE = GALLERY_COLUMNS * GALLERY_ROWS;
    private static final int CALL_DIAL_COLUMNS = 3;
    private static final int CALL_DIAL_ROWS = 4;
    private static final int CALL_MAX_NUMBER_LENGTH = PhoneData.PHONE_NUMBER_LENGTH;
    private static final int CAMERA_DEFAULT_MIN_FOV = 18;
    private static final int CAMERA_DEFAULT_MAX_FOV = 110;
    private static final int CAMERA_REAR_MIN_ZOOM_LEVEL = -4;
    private static final int CAMERA_REAR_MAX_ZOOM_LEVEL = 4;
    private static final int CAMERA_SELFIE_MIN_ZOOM_LEVEL = -3;
    private static final int CAMERA_SELFIE_MAX_ZOOM_LEVEL = 3;
    private static final float CAMERA_REAR_MAX_ZOOM_FACTOR = 4.0F;
    private static final float CAMERA_SELFIE_MAX_ZOOM_FACTOR = 3.0F;
    private static final int CAMERA_ZOOM_INDICATOR_TICKS = 30;
    static final String[] CALL_DIAL_DIGITS = {
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            "", "0", ""
    };

    float scale;
    int frameX;
    int frameY;
    int frameWidth;
    private int frameHeight;
    int displayX;
    int displayY;
    int displayWidth;
    int displayHeight;
    private boolean unlocked;
    boolean cameraMode;
    boolean galleryMode;
    boolean photoViewerMode;
    boolean callAppMode;
    boolean callContactsMode;
    boolean callSessionMode;
    boolean chatAppMode;
    boolean chatThreadMode;
    boolean capturePending;
    private int captureFlashTicks;
    int galleryPage;
    int viewerPhotoIndex = -1;
    String dialedNumber = "";
    String activeCallNumber = "";
    String activeCallName = "";
    String chatFriendNumber = "";
    String chatDraft = "";
    String activeChatNumber = "";
    String activeChatName = "";
    String chatDeleteTargetNumber = "";
    boolean activeCallIncoming;
    boolean activeCallConnected;
    boolean activeCallMissed;
    int activeCallTicks;
    private long observedCallStateRevision = Long.MIN_VALUE;
    private boolean selfieCameraMode;
    private CameraType savedCameraType;
    private int savedCameraFov = -1;
    private int cameraZoomLevel;
    private float cameraZoomFactor = 1.0F;
    private int cameraZoomIndicatorTicks;
    private boolean cameraMoveMode;
    private double lastCameraMoveMouseX;
    private double lastCameraMoveMouseY;
    private final InteractionHand openHand;
    private final BlockPos homePhonePos;
    private final boolean homePhoneMode;
    private final PhonePhotoStore photoStore = new PhonePhotoStore(Minedevice.MOD_ID);
    private PhoneScreenLayout layoutState;
    private int callSyncCooldown;

    public PhoneScreen() {
        this(InteractionHand.MAIN_HAND);
    }

    public PhoneScreen(InteractionHand openHand) {
        super(Component.translatable("item.minedevice.phone"));
        this.openHand = openHand;
        this.homePhonePos = null;
        this.homePhoneMode = false;
    }

    public PhoneScreen(BlockPos homePhonePos) {
        super(Component.translatable("block.minedevice.home_phone"));
        this.openHand = InteractionHand.MAIN_HAND;
        this.homePhonePos = homePhonePos == null ? null : homePhonePos.immutable();
        this.homePhoneMode = this.homePhonePos != null;
        this.unlocked = true;
        this.callAppMode = true;
    }

    @Override
    protected void init() {
        updateLayout();
        rebuildWidgets();
        requestCallSync();
        applyCallStateFromServer();
    }

    @Override
    public void onClose() {
        closeCamera();
        galleryMode = false;
        photoViewerMode = false;
        callAppMode = false;
        callContactsMode = false;
        callSessionMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        capturePending = false;
        captureFlashTicks = 0;
        galleryPage = 0;
        viewerPhotoIndex = -1;
        dialedNumber = "";
        activeCallNumber = "";
        activeCallName = "";
        chatFriendNumber = "";
        chatDraft = "";
        activeChatNumber = "";
        activeChatName = "";
        chatDeleteTargetNumber = "";
        activeCallIncoming = false;
        activeCallConnected = false;
        activeCallMissed = false;
        activeCallTicks = 0;
        observedCallStateRevision = PhoneClientCallState.getRevision(homePhonePos);
        selfieCameraMode = false;
        savedCameraType = null;
        savedCameraFov = -1;
        cameraZoomLevel = 0;
        cameraZoomFactor = 1.0F;
        cameraZoomIndicatorTicks = 0;
        callSyncCooldown = 0;
        setCameraMoveMode(false, 0.0D, 0.0D);
        releasePhotoTextures();
        super.onClose();
    }

    @Override
    protected void rebuildWidgets() {
        updateLayout();
        clearWidgets();
        if (photoViewerMode) {
            addPhotoViewerWidgets();
        } else if (callSessionMode) {
            addCallSessionWidgets();
        } else if (callContactsMode) {
            addCallContactsWidgets();
        } else if (callAppMode) {
            addCallAppWidgets();
        } else if (chatThreadMode) {
            addChatThreadWidgets();
        } else if (chatAppMode) {
            addChatAppWidgets();
        } else if (cameraMode) {
            addCameraWidgets();
        } else if (galleryMode) {
            addGalleryWidgets();
        } else if (unlocked) {
            addHomeWidgets();
        } else {
            addLockWidgets();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (cameraMode) {
            // Camera mode leaves the game world visible behind the phone frame.
            renderSurface(guiGraphics, partialTick);
            renderPhoneFrame(guiGraphics);
        } else {
            if (callSessionMode || callAppMode || callContactsMode || chatAppMode || chatThreadMode) {
                renderCallBackdrop(guiGraphics);
            } else if (galleryMode || photoViewerMode) {
                renderMediaBackdrop(guiGraphics);
            } else {
                int bgX = displayX - Math.round(6 * scale);
                int bgY = displayY - Math.round(6 * scale);
                int bgWidth = displayWidth + Math.round(12 * scale);
                int bgHeight = displayHeight + Math.round(12 * scale);

                guiGraphics.blit(DEFAULT_BACKGROUND_TEXTURE, bgX, bgY, 0, 0, bgWidth, bgHeight,
                        DISPLAY_WIDTH, DISPLAY_HEIGHT);
            }

            renderSurface(guiGraphics, partialTick);
            renderPhoneFrame(guiGraphics);
        }
        boolean hideWidgetsForCapture = cameraMode && capturePending;
        if (!hideWidgetsForCapture) {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (cameraMode) {
            renderCameraOverlayHints(guiGraphics);
        }

        if (hideWidgetsForCapture) {
            completePendingCapture();
        }

        if (cameraMode && captureFlashTicks > 0) {
            renderCaptureFlash(guiGraphics);
            renderPhoneFrame(guiGraphics);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (homePhoneMode) {
            if (callSyncCooldown <= 0) {
                requestCallSync();
                callSyncCooldown = 10;
            } else {
                callSyncCooldown--;
            }
        }
        applyCallStateFromServer();
        if (isCallScreenLocked()
                && (cameraMode || galleryMode || photoViewerMode || callAppMode || callContactsMode
                || chatAppMode || chatThreadMode || !callSessionMode)) {
            closeCamera();
            cameraMode = false;
            galleryMode = false;
            photoViewerMode = false;
            callAppMode = false;
            callContactsMode = false;
            chatAppMode = false;
            chatThreadMode = false;
            callSessionMode = true;
            rebuildWidgets();
        }
        if (captureFlashTicks > 0) {
            captureFlashTicks--;
        }

        if (cameraZoomIndicatorTicks > 0) {
            cameraZoomIndicatorTicks--;
        }

        if (activeCallConnected) {
            activeCallTicks = PhoneClientCallState.getConnectedDurationTicks(homePhonePos);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (cameraMode && cameraMoveMode && handleMovementKey(keyCode, scanCode, true)) {
            return true;
        }

        if (cameraMode && keyCode == InputConstants.KEY_ESCAPE) {
            if (cameraMoveMode) {
                setCameraMoveMode(false, lastCameraMoveMouseX, lastCameraMoveMouseY);
                return true;
            }

            closeCamera();
            capturePending = false;
            captureFlashTicks = 0;
            unlocked = true;
            rebuildWidgets();
            return true;
        }

        if (cameraMode && (keyCode == InputConstants.KEY_SPACE
                || keyCode == InputConstants.KEY_RETURN
                || keyCode == InputConstants.KEY_NUMPADENTER)) {
            requestPhotoCapture();
            return true;
        }

        if (callSessionMode && keyCode == InputConstants.KEY_ESCAPE) {
            if (isCallScreenLocked()) {
                closeScreenKeepCall();
                return true;
            }
            endCallSession(true);
            return true;
        }

        if (callAppMode && keyCode == InputConstants.KEY_ESCAPE) {
            if (isCallScreenLocked()) {
                closeScreenKeepCall();
                return true;
            }
            closeCallApp();
            return true;
        }

        if (callContactsMode && keyCode == InputConstants.KEY_ESCAPE) {
            if (isCallScreenLocked()) {
                closeScreenKeepCall();
                return true;
            }
            openCallDialPage();
            return true;
        }

        if (chatThreadMode && keyCode == InputConstants.KEY_ESCAPE) {
            openChatApp();
            return true;
        }

        if (chatAppMode && keyCode == InputConstants.KEY_ESCAPE) {
            if (hasChatDeleteMenuOpen()) {
                clearChatDeleteMenu();
                return true;
            }
            closeChatApp();
            return true;
        }

        if (callAppMode) {
            String digit = getDigitForKey(keyCode);
            if (digit != null) {
                appendDialDigit(digit);
                return true;
            }

            if (keyCode == InputConstants.KEY_BACKSPACE) {
                removeLastDialDigit();
                return true;
            }

            if ((keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) && !dialedNumber.isEmpty()) {
                startCallSession(dialedNumber);
                return true;
            }
        }

        if (chatAppMode) {
            String digit = getDigitForKey(keyCode);
            if (digit != null) {
                appendChatFriendDigit(digit);
                return true;
            }

            if (keyCode == InputConstants.KEY_BACKSPACE) {
                removeLastChatFriendDigit();
                return true;
            }

            if ((keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)
                    && canAddChatFriend(chatFriendNumber)) {
                addChatFriend(chatFriendNumber);
                return true;
            }
        }

        if (chatThreadMode) {
            if (keyCode == InputConstants.KEY_BACKSPACE) {
                removeLastChatDraftCharacter();
                return true;
            }

            if ((keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)
                    && canSendChatMessage()) {
                sendChatMessage();
                return true;
            }
        }

        if (callSessionMode && activeCallIncoming && !activeCallConnected
                && (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)) {
            connectActiveCall();
            return true;
        }

        if (callSessionMode && activeCallMissed
                && (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)) {
            endCallSession(true);
            return true;
        }

        if (photoViewerMode && keyCode == InputConstants.KEY_ESCAPE) {
            photoViewerMode = false;
            galleryMode = true;
            rebuildWidgets();
            return true;
        }

        if (photoViewerMode && keyCode == InputConstants.KEY_LEFT) {
            stepViewer(-1);
            return true;
        }

        if (photoViewerMode && keyCode == InputConstants.KEY_RIGHT) {
            stepViewer(1);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (cameraMode && cameraMoveMode && handleMovementKey(keyCode, scanCode, false)) {
            return true;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (chatAppMode) {
            if (Character.isDigit(codePoint)) {
                appendChatFriendDigit(String.valueOf(codePoint));
                return true;
            }
            return false;
        }

        if (chatThreadMode && isAcceptedChatCharacter(codePoint)) {
            appendChatDraftCharacter(codePoint);
            return true;
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (cameraMode && button == 1 && !capturePending) {
            requestPhotoCapture();
            return true;
        }

        if (cameraMode && cameraMoveMode && button == 0) {
            setCameraMoveMode(false, mouseX, mouseY);
            return true;
        }

        if (button == 0 && cameraMode && !cameraMoveMode && !capturePending && getCameraPreviewBounds().contains(mouseX, mouseY)) {
            openGallery();
            return true;
        }

        if (button == 0 && callSessionMode) {
            if (activeCallIncoming && !activeCallConnected && getCallConnectButtonBounds().contains(mouseX, mouseY)) {
                connectActiveCall();
                return true;
            }

            if (activeCallMissed && getCallConnectButtonBounds().contains(mouseX, mouseY)) {
                endCallSession(true);
                return true;
            }

            if (getCallHangupButtonBounds().contains(mouseX, mouseY)) {
                endCallSession(true);
                return true;
            }
        }

        if (button == 0 && callAppMode) {
            if (getCallListMenuBounds().contains(mouseX, mouseY)) {
                openCallContactsPage();
                return true;
            }

            if (getCallDialMenuBounds().contains(mouseX, mouseY)) {
                return true;
            }

            int clickedDigitIndex = getDialPadDigitIndexAt(mouseX, mouseY);
            if (clickedDigitIndex >= 0) {
                appendDialDigit(CALL_DIAL_DIGITS[clickedDigitIndex]);
                return true;
            }

            if (getDialDeleteButtonBounds().contains(mouseX, mouseY)) {
                removeLastDialDigit();
                return true;
            }

            if (!dialedNumber.isEmpty() && getDialCallButtonBounds().contains(mouseX, mouseY)) {
                startCallSession(dialedNumber);
                return true;
            }
        }

        if (button == 0 && callContactsMode) {
            if (getCallDialMenuBounds().contains(mouseX, mouseY)) {
                openCallDialPage();
                return true;
            }

            if (getCallListMenuBounds().contains(mouseX, mouseY)) {
                return true;
            }

            String saveCandidateNumber = getContactSaveCandidateNumber();
            if (!saveCandidateNumber.isEmpty() && !hasSavedContact(saveCandidateNumber)
                    && getContactSaveButtonBounds().contains(mouseX, mouseY)) {
                requestSaveContact(saveCandidateNumber, "");
                return true;
            }

            List<PhoneContact> contacts = getPhoneContacts();
            int contactIndex = getContactIndexAt(mouseX, mouseY, contacts);
            if (contactIndex >= 0 && contactIndex < contacts.size()) {
                PhoneContact contact = contacts.get(contactIndex);
                if (getContactDeleteButtonBounds(contactIndex, contacts.size()).contains(mouseX, mouseY)) {
                    requestDeleteContact(contact.number());
                } else {
                    startCallSession(contact.number());
                }
                return true;
            }
        }

        if (chatAppMode) {
            List<PhoneContact> friends = getChatFriends();
            int friendIndex = getChatFriendIndexAt(mouseX, mouseY, friends);

            if (button == 1) {
                if (friendIndex >= 0 && friendIndex < friends.size()) {
                    toggleChatDeleteMenu(friends.get(friendIndex).number());
                    return true;
                }
                if (hasChatDeleteMenuOpen()) {
                    clearChatDeleteMenu();
                    return true;
                }
            }

            if (button == 0) {
                if (hasChatDeleteMenuOpen()) {
                    int deleteTargetIndex = getChatDeleteTargetIndex(friends);
                    if (deleteTargetIndex >= 0
                            && getChatDeleteButtonBounds(deleteTargetIndex, friends.size()).contains(mouseX, mouseY)) {
                        requestDeleteChatConversation(chatDeleteTargetNumber);
                        clearChatDeleteMenu();
                        return true;
                    }
                }

                if (getChatAddButtonBounds().contains(mouseX, mouseY) && canAddChatFriend(chatFriendNumber)) {
                    addChatFriend(chatFriendNumber);
                    return true;
                }

                if (friendIndex >= 0 && friendIndex < friends.size()) {
                    if (hasChatDeleteMenuOpen()) {
                        clearChatDeleteMenu();
                        return true;
                    }
                    openChatThread(friends.get(friendIndex));
                    return true;
                }

                if (hasChatDeleteMenuOpen()) {
                    clearChatDeleteMenu();
                    return true;
                }
            }
        }

        if (button == 0 && chatThreadMode) {
            if (getChatSendButtonBounds().contains(mouseX, mouseY) && canSendChatMessage()) {
                sendChatMessage();
                return true;
            }
        }

        if (button == 0 && galleryMode) {
            int clickedPhotoIndex = getGalleryPhotoIndexAt(mouseX, mouseY, getPhotos());
            if (clickedPhotoIndex >= 0) {
                openPhotoViewer(clickedPhotoIndex);
                return true;
            }
        }

        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) {
            return true;
        }

        if (cameraMode && cameraMoveMode && button == 0) {
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (cameraMode && cameraMoveMode) {
            lastCameraMoveMouseX = mouseX;
            lastCameraMoveMouseY = mouseY;
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (cameraMode && cameraMoveMode) {
            rotateCameraPlayer(mouseX - lastCameraMoveMouseX, mouseY - lastCameraMoveMouseY);
            lastCameraMoveMouseX = mouseX;
            lastCameraMoveMouseY = mouseY;
            return;
        }

        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (cameraMode && cameraMoveMode && button == 0) {
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (cameraMode && scrollDelta != 0.0D) {
            adjustCameraZoom(scrollDelta);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    private void updateLayout() {
        float fitScale = Math.min((float) width / FRAME_WIDTH, (float) height / FRAME_HEIGHT);
        scale = Math.min(FRAME_SCALE, fitScale * FRAME_SCALE);

        frameWidth = Math.round(FRAME_WIDTH * scale);
        frameHeight = Math.round(FRAME_HEIGHT * scale);
        int baseFrameX = (width - frameWidth) / 2;

        int hotbarOffset = Math.round(height * 0.1F);
        int baseFrameY = Math.min((height - frameHeight) / 2, height - frameHeight - hotbarOffset);

        frameX = baseFrameX;
        frameY = baseFrameY;

        displayX = frameX + Math.round(DISPLAY_X * scale);
        displayY = frameY + Math.round(DISPLAY_Y * scale);
        displayWidth = Math.round(DISPLAY_WIDTH * scale);
        displayHeight = Math.round(DISPLAY_HEIGHT * scale);
        layoutState = new PhoneScreenLayout(scale, frameX, frameY, displayX, displayY, displayWidth, displayHeight);
    }

    private void addLockWidgets() {
        int scaledIconSize = Math.round(ICON_SIZE * scale);
        int x = displayX + (displayWidth - scaledIconSize) / 2;
        int y = displayY + displayHeight - scaledIconSize - Math.round(32 * scale);

        addRenderableWidget(new ImageButton(
                x, y, scaledIconSize, scaledIconSize,
                0, 0, 0, UNLOCK_TEXTURE, ICON_SIZE, ICON_SIZE,
                button -> {
                    unlocked = true;
                    rebuildWidgets();
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.width, this.height, 0.0F, 0.0F,
                        ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
            }
        });

        addNavigationButtons();
    }

    private void addHomeWidgets() {
        int scaledIconSize = Math.round(ICON_SIZE * scale);
        int appsPerRow = 4;
        int usableWidth = Math.max(0, displayWidth - (scaledIconSize * appsPerRow));
        int space = Math.max(2, usableWidth / (appsPerRow + 1));
        int leftover = usableWidth - (space * (appsPerRow + 1));
        int leftInset = space + (leftover / 2);
        int iconGap = space;
        int rowGap = Math.round(15 * scale);

        int iconX1 = displayX + leftInset;
        int iconX2 = iconX1 + scaledIconSize + iconGap;
        int iconX3 = iconX2 + scaledIconSize + iconGap;
        int iconX4 = iconX3 + scaledIconSize + iconGap;
        int iconY1 = displayY + Math.round(displayHeight * 0.40F);
        int iconY2 = iconY1 + scaledIconSize + rowGap;

        addRenderableWidget(createAppIcon(iconX1, iconY1, APP_CALL_TEXTURE, this::openCallApp));
        addRenderableWidget(createAppIcon(iconX2, iconY1, APP_CHAT_TEXTURE, this::openChatApp));
        addRenderableWidget(createAppIcon(iconX3, iconY1, APP_SHOP_TEXTURE, () -> {
        }));
        addRenderableWidget(createAppIcon(iconX4, iconY1, APP_GOOGLE_TEXTURE, () -> {
        }));

        addRenderableWidget(createAppIcon(iconX1, iconY2, APP_GALLERY_TEXTURE, this::openGallery));
        addRenderableWidget(createAppIcon(iconX2, iconY2, APP_BANK_TEXTURE, () -> {
        }));
        addRenderableWidget(createAppIcon(iconX3, iconY2, APP_CAMERA_TEXTURE, () -> {
            openCamera();
        }));

        addNavigationButtons();
    }

    private ImageButton createAppIcon(int x, int y, ResourceLocation texture, Runnable clickAction) {
        int scaledIconSize = Math.round(ICON_SIZE * scale);
        return new ImageButton(
                x, y, scaledIconSize, scaledIconSize,
                0, 0, 0, texture, ICON_SIZE, ICON_SIZE,
                button -> clickAction.run()) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.width, this.height, 0.0F, 0.0F,
                        ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
            }
        };
    }

    private void addGalleryWidgets() {
        addNavigationButtons();
    }

    private void addCallAppWidgets() {
        addNavigationButtons();
    }

    private void addCallContactsWidgets() {
        addNavigationButtons();
    }

    private void addCallSessionWidgets() {
        addNavigationButtons();
    }

    private void addChatAppWidgets() {
        addNavigationButtons();
    }

    private void addChatThreadWidgets() {
        addNavigationButtons();
    }

    private void addPhotoViewerWidgets() {
        UiRect contentBounds = getMediaSurfaceBounds();
        int deleteButtonSize = Math.round(24 * scale);
        int deleteButtonX = contentBounds.right() - deleteButtonSize - Math.round(6 * scale);
        int deleteButtonY = contentBounds.top + Math.round(8 * scale);

        addRenderableWidget(new ImageButton(
                deleteButtonX, deleteButtonY, deleteButtonSize, deleteButtonSize,
                0, 0, 0, DELETE_BUTTON_TEXTURE, 24, 24,
                button -> deleteCurrentPhoto()) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.width, this.height,
                        0.0F, 0.0F, 24, 24, 24, 24);
            }
        });

        addNavigationButtons();
    }

    private void addCameraWidgets() {
        UiRect shutterBounds = getCameraShutterButtonBounds();
        UiRect flipButtonBounds = getCameraFlipButtonBounds();

        addRenderableWidget(new ImageButton(
                flipButtonBounds.left, flipButtonBounds.top, flipButtonBounds.width, flipButtonBounds.height,
                0, 0, 0, CAMFLIP_BUTTON_TEXTURE, 24, 24,
                button -> toggleCameraFlip()) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.width, this.height, 0.0F, 0.0F,
                        24, 24, 24, 24);
            }
        });

        addRenderableWidget(new ImageButton(
                shutterBounds.left, shutterBounds.top, shutterBounds.width, shutterBounds.height,
                0, 0, 0, SHUTTER_BUTTON_TEXTURE, 60, 60,
                button -> setCameraMoveMode(true,
                        button.getX() + (button.getWidth() / 2.0D),
                        button.getY() + (button.getHeight() / 2.0D))) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.width, this.height, 0.0F, 0.0F,
                        60, 60, 60, 60);
            }
        });

        addNavigationButtons();
    }

    private void addNavigationButtons() {
        int scaledIconSize = Math.round((ICON_SIZE * 0.85F) * scale);
        int navY = displayY + displayHeight - scaledIconSize - Math.round(-7 * scale);
        int navLeftX = displayX + Math.round(8 * scale);
        int navCenterX = displayX + (displayWidth - scaledIconSize) / 2;
        int navRightX = displayX + displayWidth - scaledIconSize - Math.round(8 * scale);

        addRenderableWidget(new ImageButton(
                navLeftX, navY, scaledIconSize, scaledIconSize,
                0, 0, 0, LEFT_NAV_TEXTURE, ICON_SIZE, ICON_SIZE,
                button -> {
                    if (photoViewerMode) {
                        stepViewer(-1);
                        return;
                    }

                    if (callSessionMode) {
                        endCallSession(true);
                        return;
                    }

                    if (chatThreadMode) {
                        openChatApp();
                        return;
                    }

                    if (chatAppMode) {
                        closeChatApp();
                        return;
                    }

                    if (callContactsMode) {
                        if (isCallScreenLocked()) {
                            return;
                        }
                        openCallDialPage();
                        return;
                    }

                    if (callAppMode) {
                        if (isCallScreenLocked()) {
                            return;
                        }
                        closeCallApp();
                        return;
                    }

                    if (cameraMode || galleryMode) {
                        closeCamera();
                        galleryMode = false;
                        galleryPage = 0;
                        rebuildWidgets();
                    }
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.width, this.height, 0.0F, 0.0F,
                        ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
            }
        });

        addRenderableWidget(new ImageButton(
                navCenterX, navY, scaledIconSize, scaledIconSize,
                0, 0, 0, HOME_NAV_TEXTURE, ICON_SIZE, ICON_SIZE,
                button -> {
                    if (photoViewerMode) {
                        photoViewerMode = false;
                        galleryMode = true;
                        rebuildWidgets();
                        return;
                    }

                    if (callSessionMode || callAppMode || callContactsMode) {
                        if (isCallScreenLocked()) {
                            closeScreenKeepCall();
                            return;
                        }
                        closeCallApp();
                        return;
                    }

                    if (chatThreadMode || chatAppMode) {
                        closeChatApp();
                        return;
                    }

                    if (cameraMode || galleryMode) {
                        closeCamera();
                        galleryMode = false;
                        galleryPage = 0;
                        unlocked = true;
                        rebuildWidgets();
                    } else {
                        unlocked = false;
                        rebuildWidgets();
                    }
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.width, this.height, 0.0F, 0.0F,
                        ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
            }
        });

        addRenderableWidget(new ImageButton(
                navRightX, navY, scaledIconSize, scaledIconSize,
                0, 0, 0, RIGHT_NAV_TEXTURE, ICON_SIZE, ICON_SIZE,
                button -> {
                    if (photoViewerMode) {
                        stepViewer(1);
                        return;
                    }

                    if (callSessionMode || callAppMode || callContactsMode || chatThreadMode || chatAppMode) {
                        return;
                    }

                    if (galleryMode) {
                        int totalPages = getGalleryPageCount();
                        if (totalPages > 1) {
                            galleryPage = (galleryPage + 1) % totalPages;
                            rebuildWidgets();
                        }
                    }
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.width, this.height, 0.0F, 0.0F,
                        ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
            }
        });
    }

    private void renderSurface(GuiGraphics guiGraphics, float partialTick) {
        if (photoViewerMode) {
            renderPhotoViewerSurface(guiGraphics);
            return;
        }

        if (callSessionMode) {
            renderCallSessionSurface(guiGraphics);
            return;
        }

        if (callContactsMode) {
            renderCallContactsSurface(guiGraphics);
            return;
        }

        if (callAppMode) {
            renderCallAppSurface(guiGraphics);
            return;
        }

        if (chatThreadMode) {
            renderChatThreadSurface(guiGraphics);
            return;
        }

        if (chatAppMode) {
            renderChatAppSurface(guiGraphics);
            return;
        }

        if (galleryMode) {
            renderGallerySurface(guiGraphics);
            return;
        }

        if (cameraMode) {
            renderCameraSurface(guiGraphics, partialTick);
            return;
        }

        if (!unlocked) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                long gameTime = minecraft.level.getDayTime() % 24000L;
                int hours = (int) ((gameTime + 6000) / 1000) % 24;
                int minutes = (int) ((gameTime % 1000) * 60 / 1000);

                String timeString = String.format("%02d:%02d", hours, minutes);
                int textWidth = minecraft.font.width(timeString);
                int textX = displayX + (displayWidth - textWidth) / 2;
                int textY = displayY + Math.round(displayHeight * 0.25F);

                guiGraphics.drawString(minecraft.font, timeString, textX, textY, 0xFF000000, true);
            }
        }
    }

    private void renderGallerySurface(GuiGraphics guiGraphics) {
        PhoneMediaSurfaceRenderer.renderGallerySurface(this, guiGraphics);
    }

    private void renderPhotoViewerSurface(GuiGraphics guiGraphics) {
        PhoneMediaSurfaceRenderer.renderPhotoViewerSurface(this, guiGraphics);
    }

    private void renderCallAppSurface(GuiGraphics guiGraphics) {
        PhoneCallSurfaceRenderer.renderCallAppSurface(this, guiGraphics);
    }

    private void renderCallContactsSurface(GuiGraphics guiGraphics) {
        PhoneCallSurfaceRenderer.renderCallContactsSurface(this, guiGraphics);
    }

    private void renderCallSessionSurface(GuiGraphics guiGraphics) {
        PhoneCallSurfaceRenderer.renderCallSessionSurface(this, guiGraphics);
    }

    private void renderChatAppSurface(GuiGraphics guiGraphics) {
        PhoneChatSurfaceRenderer.renderChatAppSurface(this, guiGraphics);
    }

    private void renderChatThreadSurface(GuiGraphics guiGraphics) {
        PhoneChatSurfaceRenderer.renderChatThreadSurface(this, guiGraphics);
    }

    private void renderCallBackdrop(GuiGraphics guiGraphics) {
        PhoneCallSurfaceRenderer.renderCallBackdrop(this, guiGraphics);
    }

    private void renderMediaBackdrop(GuiGraphics guiGraphics) {
        PhoneCallSurfaceRenderer.renderMediaBackdrop(this, guiGraphics);
    }

    private void renderCameraSurface(GuiGraphics guiGraphics, float partialTick) {
        PhoneMediaSurfaceRenderer.renderCameraSurface(this, guiGraphics);
    }

    private void renderCameraOverlayHints(GuiGraphics guiGraphics) {
        PhoneMediaSurfaceRenderer.renderCameraOverlayHints(this, guiGraphics);
    }

    ViewerLayout getViewerLayout() {
        return new ViewerLayout(
                displayX + Math.round(6 * scale),
                displayY + Math.round(34 * scale),
                displayWidth - Math.round(12 * scale),
                displayHeight - Math.round(82 * scale));
    }

    private PhoneScreenLayout getLayoutState() {
        if (layoutState == null) {
            updateLayout();
        }
        return layoutState;
    }

    GalleryLayout computeGalleryLayout() {
        return getLayoutState().galleryLayout();
    }

    int getGalleryHeaderHeight() {
        return getLayoutState().galleryHeaderHeight();
    }

    private int getGalleryFooterHeight() {
        return getLayoutState().galleryFooterHeight();
    }

    int getGalleryPageCount() {
        return getLayoutState().galleryPageCount(getPhotos().size());
    }

    UiRect getGallerySlotBounds(int localIndex, int pageItemCount) {
        return getLayoutState().gallerySlotBounds(localIndex, pageItemCount);
    }

    private int getGalleryPhotoIndexAt(double mouseX, double mouseY, List<PhotoEntry> photos) {
        return getLayoutState().galleryPhotoIndexAt(mouseX, mouseY, galleryPage, photos.size());
    }

    UiRect getCallSurfaceBounds() {
        return getLayoutState().callSurfaceBounds();
    }

    UiRect getCallBackdropBounds() {
        return getLayoutState().callBackdropBounds();
    }

    UiRect getMediaBackdropBounds() {
        return getLayoutState().mediaBackdropBounds();
    }

    UiRect getMediaSurfaceBounds() {
        return getLayoutState().mediaSurfaceBounds();
    }

    UiRect getChatSurfaceBounds() {
        return getMediaSurfaceBounds();
    }

    int getCallHeaderHeight() {
        return getLayoutState().callHeaderHeight();
    }

    UiRect getCallDialMenuBounds() {
        return getLayoutState().callDialMenuBounds();
    }

    UiRect getCallListMenuBounds() {
        return getLayoutState().callListMenuBounds();
    }

    UiRect getCallNumberDisplayBounds() {
        return getLayoutState().callNumberDisplayBounds();
    }

    private UiRect getCallDialPadBounds() {
        return getLayoutState().callDialPadBounds();
    }

    int getCallMenuBottomReserve() {
        return getLayoutState().callMenuBottomReserve();
    }

    UiRect getDialPadCellBounds(int index) {
        return getLayoutState().dialPadCellBounds(index);
    }

    private int getDialPadDigitIndexAt(double mouseX, double mouseY) {
        for (int i = 0; i < CALL_DIAL_DIGITS.length; i++) {
            if (!CALL_DIAL_DIGITS[i].isEmpty() && getDialPadCellBounds(i).contains(mouseX, mouseY)) {
                return i;
            }
        }

        return -1;
    }

    UiRect getDialDeleteButtonBounds() {
        return getLayoutState().dialDeleteButtonBounds();
    }

    UiRect getDialCallButtonBounds() {
        return getLayoutState().dialCallButtonBounds();
    }

    private UiRect getContactPanelBounds() {
        return getLayoutState().contactPanelBounds();
    }

    UiRect getContactSaveButtonBounds() {
        return getLayoutState().contactSaveButtonBounds();
    }

    private UiRect getContactRowsBounds() {
        String saveCandidateNumber = getContactSaveCandidateNumber();
        boolean canSaveNumber = !saveCandidateNumber.isEmpty() && !hasSavedContact(saveCandidateNumber);
        return getLayoutState().contactRowsBounds(canSaveNumber);
    }

    UiRect getContactRowBounds(int index, int contactCount) {
        String saveCandidateNumber = getContactSaveCandidateNumber();
        boolean canSaveNumber = !saveCandidateNumber.isEmpty() && !hasSavedContact(saveCandidateNumber);
        return getLayoutState().contactRowBounds(index, contactCount, canSaveNumber);
    }

    UiRect getContactDeleteButtonBounds(int index, int contactCount) {
        String saveCandidateNumber = getContactSaveCandidateNumber();
        boolean canSaveNumber = !saveCandidateNumber.isEmpty() && !hasSavedContact(saveCandidateNumber);
        return getLayoutState().contactDeleteButtonBounds(index, contactCount, canSaveNumber);
    }

    private int getContactIndexAt(double mouseX, double mouseY, List<PhoneContact> contacts) {
        String saveCandidateNumber = getContactSaveCandidateNumber();
        boolean canSaveNumber = !saveCandidateNumber.isEmpty() && !hasSavedContact(saveCandidateNumber);
        return getLayoutState().contactIndexAt(mouseX, mouseY, contacts.size(), canSaveNumber);
    }

    UiRect getCallConnectButtonBounds() {
        return getLayoutState().callConnectButtonBounds();
    }

    UiRect getCallHangupButtonBounds() {
        if (activeCallConnected) {
            UiRect connectBounds = getLayoutState().callConnectButtonBounds();
            return new UiRect(
                    displayX + ((displayWidth - connectBounds.width) / 2),
                    connectBounds.top,
                    connectBounds.width,
                    connectBounds.height
            );
        }
        return getLayoutState().callHangupButtonBounds();
    }

    UiRect getChatHeaderBounds() {
        UiRect contentBounds = getChatSurfaceBounds();
        int height = Math.max(getCallHeaderHeight(), Math.round(28 * scale));
        return new UiRect(contentBounds.left, contentBounds.top, contentBounds.width, height);
    }

    private int getChatSidePadding() {
        return Math.max(1, Math.round(2 * scale));
    }

    UiRect getChatFriendInputBounds() {
        UiRect contentBounds = getChatSurfaceBounds();
        int top = getChatHeaderBounds().bottom() + Math.max(5, Math.round(6 * scale));
        int height = Math.max(16, Math.round(18 * scale));
        int sidePadding = getChatSidePadding();
        UiRect addButtonBounds = getChatAddButtonBounds();
        return new UiRect(contentBounds.left + sidePadding, top,
                Math.max(28, addButtonBounds.left - contentBounds.left - (sidePadding * 2)), height);
    }

    UiRect getChatAddButtonBounds() {
        UiRect contentBounds = getChatSurfaceBounds();
        int top = getChatHeaderBounds().bottom() + Math.max(5, Math.round(6 * scale));
        int width = Math.max(26, Math.round(32 * scale));
        int height = Math.max(16, Math.round(18 * scale));
        int sidePadding = getChatSidePadding();
        return new UiRect(contentBounds.right() - width - sidePadding, top, width, height);
    }

    UiRect getChatFriendRowsBounds() {
        UiRect contentBounds = getChatSurfaceBounds();
        int sidePadding = getChatSidePadding();
        int top = getChatFriendInputBounds().bottom() + Math.max(5, Math.round(6 * scale));
        int bottom = contentBounds.bottom() - Math.max(2, Math.round(3 * scale));
        return new UiRect(contentBounds.left + sidePadding, top,
                contentBounds.width - (sidePadding * 2), Math.max(24, bottom - top));
    }

    UiRect getChatFriendRowBounds(int index, int friendCount) {
        UiRect rowsBounds = getChatFriendRowsBounds();
        int rowGap = Math.max(2, Math.round(3 * scale));
        int safeCount = Math.max(1, friendCount);
        int availableHeight = rowsBounds.height - (rowGap * (safeCount - 1));
        int preferredRowHeight = Math.max(22, Math.round(30 * scale));
        int rowHeight = Math.max(20, Math.min(preferredRowHeight, Math.max(20, availableHeight / safeCount)));
        int top = rowsBounds.top + index * (rowHeight + rowGap);
        return new UiRect(rowsBounds.left, top, rowsBounds.width, rowHeight);
    }

    UiRect getChatDeleteButtonBounds(int index, int friendCount) {
        UiRect rowBounds = getChatFriendRowBounds(index, friendCount);
        int width = Math.max(26, Math.round(30 * scale));
        int height = Math.max(10, Math.round(12 * scale));
        int inset = Math.max(3, Math.round(4 * scale));
        return new UiRect(rowBounds.right() - width - inset,
                rowBounds.top + (rowBounds.height - height) / 2,
                width,
                height);
    }

    UiRect getChatThreadTopBounds() {
        UiRect contentBounds = getChatSurfaceBounds();
        int sidePadding = getChatSidePadding();
        int top = contentBounds.top + Math.max(4, Math.round(5 * scale));
        int height = Math.max(18, Math.round(20 * scale));
        return new UiRect(contentBounds.left + sidePadding, top,
                contentBounds.width - (sidePadding * 2), height);
    }

    UiRect getChatMessagesBounds() {
        UiRect contentBounds = getChatSurfaceBounds();
        int sidePadding = getChatSidePadding();
        int top = getChatThreadTopBounds().bottom() + Math.max(5, Math.round(6 * scale));
        int bottom = getChatComposerBounds().top - Math.max(5, Math.round(6 * scale));
        return new UiRect(contentBounds.left + sidePadding, top,
                contentBounds.width - (sidePadding * 2), Math.max(30, bottom - top));
    }

    UiRect getChatComposerBounds() {
        UiRect contentBounds = getChatSurfaceBounds();
        int sidePadding = getChatSidePadding();
        int height = Math.max(13, Math.round(16 * scale));
        int bottom = contentBounds.bottom() - Math.max(18, Math.round(20 * scale));
        return new UiRect(contentBounds.left + sidePadding, bottom - height,
                contentBounds.width - (sidePadding * 2), height);
    }

    UiRect getChatDraftBounds() {
        UiRect composerBounds = getChatComposerBounds();
        UiRect sendBounds = getChatSendButtonBounds();
        int gap = Math.max(4, Math.round(5 * scale));
        return new UiRect(composerBounds.left, composerBounds.top,
                Math.max(28, sendBounds.left - composerBounds.left - gap), composerBounds.height);
    }

    UiRect getChatSendButtonBounds() {
        UiRect composerBounds = getChatComposerBounds();
        int width = Math.max(26, Math.round(30 * scale));
        return new UiRect(composerBounds.right() - width, composerBounds.top, width, composerBounds.height);
    }

    List<PhoneContact> getPhoneContacts() {
        if (homePhoneMode) {
            HomePhoneBlockEntity homePhone = getClientHomePhone();
            return homePhone == null ? List.of() : homePhone.getContacts();
        }
        return PhoneData.getContacts(getOpenPhoneStack());
    }

    List<PhoneContact> getChatFriends() {
        return homePhoneMode ? List.of() : PhoneChatData.getFriends(getOpenPhoneStack());
    }

    List<PhoneChatMessage> getActiveChatMessages() {
        if (activeChatNumber.isEmpty()) {
            return List.of();
        }
        return PhoneChatData.getMessages(getOpenPhoneStack(), activeChatNumber);
    }

    String getActiveChatDisplayName() {
        if (activeChatNumber.isEmpty()) {
            return "";
        }

        String savedName = PhoneChatData.getFriendName(getOpenPhoneStack(), activeChatNumber);
        if (!savedName.isBlank()) {
            return savedName;
        }

        return activeChatName.isBlank() ? activeChatNumber : activeChatName;
    }

    String getActiveChatPreview() {
        if (activeChatNumber.isEmpty()) {
            return "";
        }
        return PhoneChatData.getLastMessagePreview(getOpenPhoneStack(), activeChatNumber);
    }

    String getChatPreview(String number) {
        return homePhoneMode ? "" : PhoneChatData.getLastMessagePreview(getOpenPhoneStack(), number);
    }

    PhoneChatMessage getChatPreviewMessage(String number) {
        return homePhoneMode ? null : PhoneChatData.getLastMessage(getOpenPhoneStack(), number);
    }

    ResourceLocation getChatProfileTexture(String number) {
        if (homePhoneMode) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.player != null) {
                return DefaultPlayerSkin.getDefaultSkin(minecraft.player.getUUID());
            }
            return DefaultPlayerSkin.getDefaultSkin();
        }

        ItemStack phoneStack = getOpenPhoneStack();
        UUID profileId = PhoneChatData.getFriendProfileId(phoneStack, number);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.getConnection() != null) {
            if (profileId != null) {
                PlayerInfo info = minecraft.getConnection().getPlayerInfo(profileId);
                if (info != null) {
                    return info.getSkinLocation();
                }
            }

            String friendName = PhoneChatData.getFriendName(phoneStack, number);
            if (!friendName.isBlank()) {
                PlayerInfo info = minecraft.getConnection().getPlayerInfo(friendName);
                if (info != null) {
                    return info.getSkinLocation();
                }
            }
        }

        if (profileId != null) {
            return DefaultPlayerSkin.getDefaultSkin(profileId);
        }

        if (minecraft != null && minecraft.player != null) {
            return DefaultPlayerSkin.getDefaultSkin(minecraft.player.getUUID());
        }

        return DefaultPlayerSkin.getDefaultSkin();
    }

    boolean hasSavedContact(String rawNumber) {
        String normalized = PhoneData.normalizePhoneNumber(rawNumber);
        if (!PhoneData.isValidPhoneNumber(normalized)) {
            return false;
        }
        if (homePhoneMode) {
            HomePhoneBlockEntity homePhone = getClientHomePhone();
            return homePhone != null && homePhone.hasContact(normalized);
        }
        return PhoneData.hasContact(getOpenPhoneStack(), normalized);
    }

    String getContactSaveCandidateNumber() {
        String normalized = PhoneData.normalizePhoneNumber(dialedNumber);
        if (!PhoneData.isValidPhoneNumber(normalized)) {
            return "";
        }
        return normalized.equals(getOwnPhoneNumber()) ? "" : normalized;
    }

    private String getSavedContactName(String rawNumber) {
        String normalized = PhoneData.normalizePhoneNumber(rawNumber);
        for (PhoneContact contact : getPhoneContacts()) {
            if (contact.number().equals(normalized)) {
                return contact.displayName();
            }
        }
        return "";
    }

    UiRect getCameraPreviewBounds() {
        return getLayoutState().cameraPreviewBounds();
    }

    UiRect getCameraShutterButtonBounds() {
        return getLayoutState().cameraShutterButtonBounds();
    }

    private UiRect getCameraFlipButtonBounds() {
        return getLayoutState().cameraFlipButtonBounds();
    }

    private UiRect getCameraViewBounds() {
        return getLayoutState().cameraViewBounds();
    }

    private void rotateCameraPlayer(double dragX, double dragY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        float yaw = minecraft.player.getYRot() + (float) (dragX * 0.45F);
        float pitch = Mth.clamp(minecraft.player.getXRot() + (float) (dragY * 0.45F), -90.0F, 90.0F);

        minecraft.player.setYRot(yaw);
        minecraft.player.setXRot(pitch);
        minecraft.player.setYHeadRot(yaw);
        minecraft.player.setYBodyRot(yaw);
    }

    private void applyCallStateFromServer() {
        long nextRevision = PhoneClientCallState.getRevision(homePhonePos);
        if (nextRevision == observedCallStateRevision) {
            return;
        }

        observedCallStateRevision = nextRevision;
        PhoneCallState state = PhoneClientCallState.getState(homePhonePos);
        String otherNumber = PhoneClientCallState.getOtherNumber(homePhonePos);
        String otherName = PhoneClientCallState.getOtherName(homePhonePos);
        String savedContactName = getSavedContactName(otherNumber);
        if (!savedContactName.isBlank()) {
            otherName = savedContactName;
        }
        boolean shouldRebuild = false;

        if (state == PhoneCallState.IDLE) {
            boolean hadCallState = !activeCallNumber.isEmpty() || activeCallConnected || activeCallIncoming
                    || activeCallMissed || !activeCallName.isEmpty();
            if (callSessionMode) {
                callSessionMode = false;
                callContactsMode = false;
                callAppMode = true;
                shouldRebuild = true;
            }

            if (hadCallState) {
                activeCallNumber = "";
                activeCallName = "";
                activeCallIncoming = false;
                activeCallConnected = false;
                activeCallMissed = false;
                activeCallTicks = 0;
                shouldRebuild = true;
            }
        } else {
            boolean nextIncoming = state == PhoneCallState.INCOMING_RINGING;
            boolean nextConnected = state == PhoneCallState.CONNECTED;
            boolean nextMissed = state == PhoneCallState.MISSED;
            boolean stateChanged = !Objects.equals(activeCallNumber, otherNumber)
                    || !Objects.equals(activeCallName, otherName)
                    || activeCallIncoming != nextIncoming
                    || activeCallConnected != nextConnected
                    || activeCallMissed != nextMissed
                    || !callSessionMode;

            activeCallNumber = otherNumber;
            activeCallName = otherName;
            activeCallIncoming = nextIncoming;
            activeCallConnected = nextConnected;
            activeCallMissed = nextMissed;
            activeCallTicks = nextConnected ? PhoneClientCallState.getConnectedDurationTicks(homePhonePos) : 0;

            if (!cameraMode && !galleryMode && !photoViewerMode) {
                callAppMode = false;
                callContactsMode = false;
                callSessionMode = true;
                shouldRebuild = true;
            } else if (stateChanged) {
                shouldRebuild = true;
            }
        }

        if (shouldRebuild) {
            rebuildWidgets();
        }
    }

    private boolean handleMovementKey(int keyCode, int scanCode, boolean pressed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }

        return setMovementKeyState(minecraft.options.keyUp, keyCode, scanCode, pressed)
                || setMovementKeyState(minecraft.options.keyLeft, keyCode, scanCode, pressed)
                || setMovementKeyState(minecraft.options.keyDown, keyCode, scanCode, pressed)
                || setMovementKeyState(minecraft.options.keyRight, keyCode, scanCode, pressed)
                || setMovementKeyState(minecraft.options.keyJump, keyCode, scanCode, pressed)
                || setMovementKeyState(minecraft.options.keySprint, keyCode, scanCode, pressed)
                || setMovementKeyState(minecraft.options.keyShift, keyCode, scanCode, pressed);
    }

    private boolean setMovementKeyState(KeyMapping keyMapping, int keyCode, int scanCode, boolean pressed) {
        if (!keyMapping.matches(keyCode, scanCode)) {
            return false;
        }

        keyMapping.setDown(pressed);
        return true;
    }

    private void setCameraMoveMode(boolean enabled, double mouseX, double mouseY) {
        cameraMoveMode = enabled;
        lastCameraMoveMouseX = mouseX;
        lastCameraMoveMouseY = mouseY;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        if (!enabled) {
            minecraft.options.keyUp.setDown(false);
            minecraft.options.keyLeft.setDown(false);
            minecraft.options.keyDown.setDown(false);
            minecraft.options.keyRight.setDown(false);
            minecraft.options.keyJump.setDown(false);
            minecraft.options.keySprint.setDown(false);
            minecraft.options.keyShift.setDown(false);
        }

        long windowHandle = minecraft.getWindow().getWindow();
        GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR,
                enabled ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
        if (GLFW.glfwRawMouseMotionSupported()) {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_RAW_MOUSE_MOTION,
                    enabled ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        }
    }

    PhotoEntry getLatestPhoto() {
        List<PhotoEntry> photos = getPhotos();
        if (photos.isEmpty()) {
            return null;
        }

        return photos.get(0);
    }

    private void openCallApp() {
        closeCamera();
        photoViewerMode = false;
        galleryMode = false;
        unlocked = true;
        callAppMode = true;
        callContactsMode = false;
        callSessionMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        observedCallStateRevision = Long.MIN_VALUE;
        requestCallSync();
        applyCallStateFromServer();
        rebuildWidgets();
    }

    private void closeCallApp() {
        if (homePhoneMode) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                minecraft.setScreen(null);
            }
            return;
        }
        callAppMode = false;
        callContactsMode = false;
        callSessionMode = false;
        unlocked = true;
        rebuildWidgets();
    }

    private void openChatApp() {
        closeCamera();
        photoViewerMode = false;
        galleryMode = false;
        unlocked = true;
        callAppMode = false;
        callContactsMode = false;
        callSessionMode = false;
        chatAppMode = true;
        chatThreadMode = false;
        clearChatDeleteMenu();
        if (!activeChatNumber.isEmpty() && activeChatName.isBlank()) {
            activeChatName = PhoneChatData.getFriendName(getOpenPhoneStack(), activeChatNumber);
        }
        rebuildWidgets();
    }

    private void closeChatApp() {
        chatAppMode = false;
        chatThreadMode = false;
        chatFriendNumber = "";
        chatDraft = "";
        activeChatNumber = "";
        activeChatName = "";
        clearChatDeleteMenu();
        unlocked = true;
        rebuildWidgets();
    }

    private void openChatThread(PhoneContact contact) {
        if (contact == null) {
            return;
        }

        activeChatNumber = contact.number();
        activeChatName = contact.displayName();
        chatDraft = "";
        clearChatDeleteMenu();
        chatAppMode = false;
        chatThreadMode = true;
        rebuildWidgets();
    }

    private void openCallContactsPage() {
        if (isCallScreenLocked()) {
            callAppMode = false;
            callContactsMode = false;
            callSessionMode = true;
            rebuildWidgets();
            return;
        }
        callAppMode = false;
        callContactsMode = true;
        callSessionMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        observedCallStateRevision = Long.MIN_VALUE;
        requestCallSync();
        applyCallStateFromServer();
        rebuildWidgets();
    }

    private void openCallDialPage() {
        if (isCallScreenLocked()) {
            callAppMode = false;
            callContactsMode = false;
            callSessionMode = true;
            rebuildWidgets();
            return;
        }
        callAppMode = true;
        callContactsMode = false;
        callSessionMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        observedCallStateRevision = Long.MIN_VALUE;
        requestCallSync();
        applyCallStateFromServer();
        rebuildWidgets();
    }

    private void startCallSession(String number) {
        String normalized = PhoneData.normalizePhoneNumber(number);
        if (!PhoneData.isValidPhoneNumber(normalized)) {
            return;
        }

        dialedNumber = normalized;
        activeCallNumber = normalized;
        activeCallName = getSavedContactName(normalized);
        activeCallIncoming = false;
        activeCallConnected = false;
        activeCallMissed = false;
        activeCallTicks = 0;
        callAppMode = false;
        callContactsMode = false;
        callSessionMode = true;
        chatAppMode = false;
        chatThreadMode = false;
        if (homePhoneMode) {
            PhoneNetworkingClient.requestCall(normalized, homePhonePos);
        } else {
            PhoneNetworkingClient.requestCall(normalized);
        }
        rebuildWidgets();
    }

    private void connectActiveCall() {
        if (!callSessionMode || activeCallNumber.isEmpty() || activeCallConnected || !activeCallIncoming) {
            return;
        }

        if (homePhoneMode) {
            PhoneNetworkingClient.requestAnswer(homePhonePos);
        } else {
            PhoneNetworkingClient.requestAnswer();
        }
    }

    void endCallSession(boolean returnToList) {
        if (!activeCallNumber.isEmpty() || PhoneClientCallState.getState(homePhonePos) != PhoneCallState.IDLE) {
            if (homePhoneMode) {
                PhoneNetworkingClient.requestEndCall(homePhonePos);
            } else {
                PhoneNetworkingClient.requestEndCall();
            }
        }
        if (!activeCallNumber.isEmpty()) {
            dialedNumber = activeCallNumber;
        }
        activeCallName = "";
        activeCallNumber = "";
        activeCallIncoming = false;
        activeCallConnected = false;
        activeCallMissed = false;
        activeCallTicks = 0;
        callSessionMode = false;
        callContactsMode = false;
        callAppMode = returnToList;
        if (!returnToList) {
            unlocked = true;
        }
        rebuildWidgets();
    }

    String formatCallDuration() {
        int totalSeconds = Math.max(0, activeCallTicks / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void appendDialDigit(String digit) {
        if (digit == null || digit.isEmpty() || dialedNumber.length() >= CALL_MAX_NUMBER_LENGTH) {
            return;
        }

        dialedNumber = dialedNumber + digit;
    }

    private void removeLastDialDigit() {
        if (dialedNumber.isEmpty()) {
            return;
        }

        dialedNumber = dialedNumber.substring(0, dialedNumber.length() - 1);
    }

    private void appendChatFriendDigit(String digit) {
        if (digit == null || digit.isEmpty() || chatFriendNumber.length() >= PhoneData.PHONE_NUMBER_LENGTH) {
            return;
        }

        chatFriendNumber = chatFriendNumber + digit;
    }

    private void removeLastChatFriendDigit() {
        if (chatFriendNumber.isEmpty()) {
            return;
        }

        chatFriendNumber = chatFriendNumber.substring(0, chatFriendNumber.length() - 1);
    }

    private void appendChatDraftCharacter(char codePoint) {
        if (chatDraft.length() >= PhoneChatData.MAX_MESSAGE_LENGTH) {
            return;
        }

        chatDraft = chatDraft + codePoint;
    }

    private void removeLastChatDraftCharacter() {
        if (chatDraft.isEmpty()) {
            return;
        }

        chatDraft = chatDraft.substring(0, chatDraft.length() - 1);
    }

    private String getDigitForKey(int keyCode) {
        return switch (keyCode) {
            case InputConstants.KEY_0, InputConstants.KEY_NUMPAD0 -> "0";
            case InputConstants.KEY_1, InputConstants.KEY_NUMPAD1 -> "1";
            case InputConstants.KEY_2, InputConstants.KEY_NUMPAD2 -> "2";
            case InputConstants.KEY_3, InputConstants.KEY_NUMPAD3 -> "3";
            case InputConstants.KEY_4, InputConstants.KEY_NUMPAD4 -> "4";
            case InputConstants.KEY_5, InputConstants.KEY_NUMPAD5 -> "5";
            case InputConstants.KEY_6, InputConstants.KEY_NUMPAD6 -> "6";
            case InputConstants.KEY_7, InputConstants.KEY_NUMPAD7 -> "7";
            case InputConstants.KEY_8, InputConstants.KEY_NUMPAD8 -> "8";
            case InputConstants.KEY_9, InputConstants.KEY_NUMPAD9 -> "9";
            default -> null;
        };
    }

    boolean canAddChatFriend(String rawNumber) {
        String normalized = PhoneData.normalizePhoneNumber(rawNumber);
        return !homePhoneMode
                && PhoneData.isValidPhoneNumber(normalized)
                && PhoneData.isMobilePhoneNumber(normalized)
                && !normalized.equals(getOwnPhoneNumber())
                && !PhoneChatData.hasFriend(getOpenPhoneStack(), normalized);
    }

    private void addChatFriend(String rawNumber) {
        String normalized = PhoneData.normalizePhoneNumber(rawNumber);
        if (!canAddChatFriend(normalized)) {
            return;
        }

        PhoneNetworkingClient.requestAddChatFriend(normalized);
    }

    void handleChatFriendAddSuccess(String number) {
        if (PhoneData.normalizePhoneNumber(chatFriendNumber).equals(PhoneData.normalizePhoneNumber(number))) {
            chatFriendNumber = "";
        }
    }

    void handleChatConversationDeleted(String number) {
        String normalized = PhoneData.normalizePhoneNumber(number);
        if (normalized.isEmpty()) {
            return;
        }

        if (normalized.equals(chatDeleteTargetNumber)) {
            clearChatDeleteMenu();
        }

        if (normalized.equals(activeChatNumber)) {
            activeChatNumber = "";
            activeChatName = "";
            chatDraft = "";
            chatThreadMode = false;
            chatAppMode = true;
            rebuildWidgets();
        }
    }

    boolean isChatDeleteMenuOpenFor(String number) {
        return PhoneData.normalizePhoneNumber(number).equals(chatDeleteTargetNumber);
    }

    private boolean hasChatDeleteMenuOpen() {
        return !chatDeleteTargetNumber.isBlank();
    }

    private void toggleChatDeleteMenu(String number) {
        String normalized = PhoneData.normalizePhoneNumber(number);
        if (normalized.isEmpty()) {
            clearChatDeleteMenu();
            return;
        }

        chatDeleteTargetNumber = normalized.equals(chatDeleteTargetNumber) ? "" : normalized;
    }

    private void clearChatDeleteMenu() {
        chatDeleteTargetNumber = "";
    }

    private int getChatDeleteTargetIndex(List<PhoneContact> friends) {
        if (!hasChatDeleteMenuOpen()) {
            return -1;
        }

        for (int index = 0; index < friends.size(); index++) {
            if (friends.get(index).number().equals(chatDeleteTargetNumber)) {
                return index;
            }
        }
        return -1;
    }

    private int getChatFriendIndexAt(double mouseX, double mouseY, List<PhoneContact> friends) {
        for (int index = 0; index < friends.size(); index++) {
            if (getChatFriendRowBounds(index, friends.size()).contains(mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }

    boolean canSendChatMessage() {
        return !homePhoneMode
                && !activeChatNumber.isBlank()
                && PhoneData.isValidPhoneNumber(activeChatNumber)
                && !PhoneChatData.sanitizeMessage(chatDraft).isEmpty();
    }

    private void sendChatMessage() {
        if (!canSendChatMessage()) {
            return;
        }

        String nextDraft = PhoneChatData.sanitizeMessage(chatDraft);
        PhoneNetworkingClient.requestSendChatMessage(activeChatNumber, nextDraft);
        chatDraft = "";
    }

    private void requestDeleteChatConversation(String rawNumber) {
        String normalized = PhoneData.normalizePhoneNumber(rawNumber);
        if (!homePhoneMode && PhoneData.isValidPhoneNumber(normalized) && PhoneData.isMobilePhoneNumber(normalized)) {
            PhoneNetworkingClient.requestDeleteChatConversation(normalized);
        }
    }

    private boolean isAcceptedChatCharacter(char codePoint) {
        return !Character.isISOControl(codePoint);
    }

    String getOwnPhoneNumber() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return "00000";
        }
        if (!homePhoneMode) {
            return PhoneData.getPhoneNumber(minecraft.player);
        }

        HomePhoneBlockEntity homePhone = getClientHomePhone();
        if (homePhone != null) {
            return homePhone.getPhoneNumber();
        }
        if (minecraft.level != null && homePhonePos != null) {
            return PhoneData.getHomePhoneNumber(minecraft.level.dimension(), homePhonePos);
        }
        return "00000";
    }

    private void openGallery() {
        galleryMode = true;
        closeCamera();
        callAppMode = false;
        callContactsMode = false;
        callSessionMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        photoViewerMode = false;
        galleryPage = 0;
        rebuildWidgets();
    }

    private void openCamera() {
        Minecraft minecraft = Minecraft.getInstance();
        cameraMode = true;
        galleryMode = false;
        photoViewerMode = false;
        callAppMode = false;
        callContactsMode = false;
        callSessionMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        setCameraMoveMode(false, 0.0D, 0.0D);
        if (minecraft != null) {
            if (savedCameraType == null) {
                savedCameraType = minecraft.options.getCameraType();
            }
            if (savedCameraFov < 0) {
                savedCameraFov = getCurrentCameraFov(minecraft);
            }
            cameraZoomLevel = 0;
            cameraZoomFactor = 1.0F;
            cameraZoomIndicatorTicks = 0;
            minecraft.options.setCameraType(selfieCameraMode ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON);
            applyCameraZoom(minecraft);
        }
        rebuildWidgets();
    }

    private void closeCamera() {
        if (!cameraMode) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        cameraMode = false;
        setCameraMoveMode(false, 0.0D, 0.0D);
        if (minecraft != null && savedCameraType != null) {
            minecraft.options.setCameraType(savedCameraType);
        }
        restoreCameraZoom(minecraft);
        savedCameraType = null;
    }

    private void toggleCameraFlip() {
        Minecraft minecraft = Minecraft.getInstance();
        selfieCameraMode = !selfieCameraMode;
        cameraZoomLevel = Mth.clamp(cameraZoomLevel, getMinCameraZoomLevel(), getMaxCameraZoomLevel());
        cameraZoomFactor = getCameraZoomFactorForLevel(cameraZoomLevel);
        if (cameraMode && minecraft != null) {
            minecraft.options.setCameraType(selfieCameraMode ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON);
            applyCameraZoom(minecraft);
        }
    }

    private void adjustCameraZoom(double scrollDelta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!cameraMode || minecraft == null) {
            return;
        }

        if (savedCameraFov < 0) {
            savedCameraFov = getCurrentCameraFov(minecraft);
        }

        int minZoomLevel = getMinCameraZoomLevel();
        int maxZoomLevel = getMaxCameraZoomLevel();
        if (maxZoomLevel <= minZoomLevel) {
            cameraZoomLevel = 0;
            cameraZoomFactor = 1.0F;
            cameraZoomIndicatorTicks = CAMERA_ZOOM_INDICATOR_TICKS;
            applyCameraZoom(minecraft);
            return;
        }

        int zoomDelta = scrollDelta > 0.0D ? 1 : -1;
        cameraZoomLevel = Mth.clamp(cameraZoomLevel + zoomDelta, minZoomLevel, maxZoomLevel);
        cameraZoomFactor = getCameraZoomFactorForLevel(cameraZoomLevel);
        cameraZoomIndicatorTicks = CAMERA_ZOOM_INDICATOR_TICKS;
        applyCameraZoom(minecraft);
    }

    private void applyCameraZoom(Minecraft minecraft) {
        cameraZoomFactor = getCameraZoomFactorForLevel(
                Mth.clamp(cameraZoomLevel, getMinCameraZoomLevel(), getMaxCameraZoomLevel()));
    }

    private void restoreCameraZoom(Minecraft minecraft) {
        savedCameraFov = -1;
        cameraZoomLevel = 0;
        cameraZoomFactor = 1.0F;
        cameraZoomIndicatorTicks = 0;
    }

    private int getCurrentCameraFov(Minecraft minecraft) {
        return minecraft.options.fov().get();
    }

    float getCameraZoomFactor() {
        return cameraZoomFactor;
    }

    private float getMinCameraZoomFactor() {
        Minecraft minecraft = Minecraft.getInstance();
        int baseFov = minecraft == null
                ? Math.max(CAMERA_DEFAULT_MIN_FOV, savedCameraFov > 0 ? savedCameraFov : 70)
                : Math.max(CAMERA_DEFAULT_MIN_FOV, savedCameraFov > 0 ? savedCameraFov : getCurrentCameraFov(minecraft));
        return Math.min(1.0F, (float) baseFov / CAMERA_DEFAULT_MAX_FOV);
    }

    private float getMaxCameraZoomFactor() {
        return selfieCameraMode ? CAMERA_SELFIE_MAX_ZOOM_FACTOR : CAMERA_REAR_MAX_ZOOM_FACTOR;
    }

    int getMinCameraZoomLevel() {
        return selfieCameraMode ? CAMERA_SELFIE_MIN_ZOOM_LEVEL : CAMERA_REAR_MIN_ZOOM_LEVEL;
    }

    int getMaxCameraZoomLevel() {
        return selfieCameraMode ? CAMERA_SELFIE_MAX_ZOOM_LEVEL : CAMERA_REAR_MAX_ZOOM_LEVEL;
    }

    int getCameraZoomLevel() {
        return cameraZoomLevel;
    }

    private float getCameraZoomFactorForLevel(int zoomLevel) {
        if (zoomLevel == 0) {
            return 1.0F;
        }

        if (zoomLevel > 0) {
            float zoomProgress = (float) zoomLevel / Math.max(1, getMaxCameraZoomLevel());
            return 1.0F + ((getMaxCameraZoomFactor() - 1.0F) * zoomProgress);
        }

        float zoomProgress = (float) Math.abs(zoomLevel) / Math.max(1, Math.abs(getMinCameraZoomLevel()));
        return 1.0F - ((1.0F - getMinCameraZoomFactor()) * zoomProgress);
    }

    boolean shouldShowCameraZoomIndicator() {
        return cameraMode && cameraZoomIndicatorTicks > 0;
    }

    String getCameraZoomLabel() {
        return "x" + cameraZoomLevel;
    }

    private void openPhotoViewer(int photoIndex) {
        List<PhotoEntry> photos = getPhotos();
        if (photos.isEmpty()) {
            return;
        }

        photoViewerMode = true;
        closeCamera();
        galleryMode = false;
        callAppMode = false;
        callContactsMode = false;
        callSessionMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        viewerPhotoIndex = Mth.clamp(photoIndex, 0, photos.size() - 1);
        rebuildWidgets();
    }

    private void stepViewer(int delta) {
        List<PhotoEntry> photos = getPhotos();
        if (photos.isEmpty()) {
            return;
        }

        int photoCount = photos.size();
        viewerPhotoIndex = Math.floorMod(viewerPhotoIndex + delta, photoCount);
        rebuildWidgets();
    }

    private void deleteCurrentPhoto() {
        List<PhotoEntry> photos = getPhotos();
        if (photos.isEmpty()) {
            photoViewerMode = false;
            galleryMode = true;
            viewerPhotoIndex = -1;
            rebuildWidgets();
            return;
        }

        viewerPhotoIndex = Mth.clamp(viewerPhotoIndex, 0, photos.size() - 1);
        PhotoEntry photoEntry = photos.get(viewerPhotoIndex);
        if (!removePhotoFromPhone(viewerPhotoIndex, photoEntry.fileName)) {
            return;
        }

        int remainingPhotos = photos.size() - 1;
        if (remainingPhotos <= 0) {
            photoViewerMode = false;
            galleryMode = true;
            viewerPhotoIndex = -1;
        } else {
            viewerPhotoIndex = Math.min(viewerPhotoIndex, remainingPhotos - 1);
        }
        rebuildWidgets();
    }

    private void captureAndSavePhoto() {
        String photoFileName = capturePhotoToFile();
        if (photoFileName == null) {
            return;
        }

        ItemStack captureStack = captureTargetItem();
        if (!appendPhotoToPhone(photoFileName, captureStack)) {
            deletePhotoFile(photoFileName);
            return;
        }
    }

    private void requestPhotoCapture() {
        if (!cameraMode || capturePending || isPhotoStorageFull()) {
            return;
        }

        capturePending = true;
    }

    private void completePendingCapture() {
        if (!capturePending) {
            return;
        }

        capturePending = false;
        captureAndSavePhoto();
        captureFlashTicks = 3;
    }

    private String capturePhotoToFile() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return null;
        }

        var renderTarget = minecraft.getMainRenderTarget();
        if (renderTarget == null || renderTarget.width <= 0 || renderTarget.height <= 0) {
            return null;
        }

        NativeImage framebufferImage = null;
        NativeImage croppedImage = null;
        try {
            framebufferImage = Screenshot.takeScreenshot(renderTarget);
            int sourceWidth = framebufferImage.getWidth();
            int sourceHeight = framebufferImage.getHeight();
            if (sourceWidth <= 0 || sourceHeight <= 0) {
                return null;
            }

            croppedImage = cropCameraFeedImage(framebufferImage);
            return photoStore.writePhoto(croppedImage);
        } finally {
            if (framebufferImage != null) {
                framebufferImage.close();
            }
            if (croppedImage != null) {
                croppedImage.close();
            }
        }
    }

    private NativeImage cropCameraFeedImage(NativeImage source) {
        UiRect cameraBounds = getCameraViewBounds();
        Minecraft minecraft = Minecraft.getInstance();
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        if (minecraft == null) {
            return cropImage(source, 0, 0, sourceWidth, sourceHeight);
        }

        int guiWidth = Math.max(1, minecraft.getWindow().getGuiScaledWidth());
        int guiHeight = Math.max(1, minecraft.getWindow().getGuiScaledHeight());
        float scaleX = (float) sourceWidth / guiWidth;
        float scaleY = (float) sourceHeight / guiHeight;

        int cropX = Math.max(0, Math.round(cameraBounds.left * scaleX));
        int cropY = Math.max(0, Math.round(cameraBounds.top * scaleY));
        int cropWidth = Math.max(1, Math.round(cameraBounds.width * scaleX));
        int cropHeight = Math.max(1, Math.round(cameraBounds.height * scaleY));

        cropWidth = Math.min(cropWidth, sourceWidth - cropX);
        cropHeight = Math.min(cropHeight, sourceHeight - cropY);
        return cropImage(source, cropX, cropY, cropWidth, cropHeight);
    }

    private NativeImage cropImage(NativeImage source, int x, int y, int width, int height) {
        NativeImage croppedImage = new NativeImage(width, height, false);
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                croppedImage.setPixelRGBA(column, row, source.getPixelRGBA(x + column, y + row));
            }
        }
        return croppedImage;
    }

    private ItemStack captureTargetItem() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return ItemStack.EMPTY;
        }

        HitResult hitResult = minecraft.hitResult;
        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();

            if (entity instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getItem();
                if (!stack.isEmpty()) {
                    return stack.copy();
                }
            }

            if (entity instanceof ItemFrame itemFrame) {
                ItemStack stack = itemFrame.getItem();
                if (!stack.isEmpty()) {
                    return stack.copy();
                }
            }

            if (entity instanceof LivingEntity livingEntity) {
                ItemStack mainHand = livingEntity.getMainHandItem();
                if (!mainHand.isEmpty()) {
                    return mainHand.copy();
                }

                ItemStack offHand = livingEntity.getOffhandItem();
                if (!offHand.isEmpty()) {
                    return offHand.copy();
                }
            }
        }

        if (hitResult instanceof BlockHitResult blockHitResult) {
            BlockState blockState = minecraft.level.getBlockState(blockHitResult.getBlockPos());
            Item item = blockState.getBlock().asItem();
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }

        return ItemStack.EMPTY;
    }

    private boolean appendPhotoToPhone(String photoFileName, ItemStack captureStack) {
        return photoStore.appendPhotoToStack(getOpenPhoneStack(), photoFileName, captureStack);
    }

    private boolean removePhotoFromPhone(int viewerIndex, String expectedFileName) {
        return photoStore.removePhotoFromStack(getOpenPhoneStack(), viewerIndex, expectedFileName);
    }

    List<PhotoEntry> getPhotos() {
        return photoStore.getPhotos(getOpenPhoneStack());
    }

    int getPhotoCount() {
        return photoStore.getPhotoCount(getOpenPhoneStack());
    }

    int getMaxPhotoCount() {
        return photoStore.maxPhotos();
    }

    boolean isPhotoStorageFull() {
        return photoStore.isPhotoLimitReached(getOpenPhoneStack());
    }

    PhotoTexture getOrLoadPhotoTexture(String fileName, boolean previewMode) {
        return photoStore.getOrLoadPhotoTexture(fileName, previewMode);
    }

    private void releasePhotoTextures() {
        photoStore.releaseTextures();
    }

    private void unloadPhotoTexture(String fileName) {
        photoStore.unloadTexture(fileName);
    }

    private void deletePhotoFile(String fileName) {
        photoStore.deletePhotoFile(fileName);
    }

    private ItemStack getOpenPhoneStack() {
        if (homePhoneMode) {
            return ItemStack.EMPTY;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceHandStack = minecraft.player.getItemInHand(openHand);
        if (sourceHandStack.is(ModItems.PHONE.get())) {
            return sourceHandStack;
        }

        ItemStack mainHandStack = minecraft.player.getMainHandItem();
        if (mainHandStack.is(ModItems.PHONE.get())) {
            return mainHandStack;
        }

        ItemStack offHandStack = minecraft.player.getOffhandItem();
        if (offHandStack.is(ModItems.PHONE.get())) {
            return offHandStack;
        }

        return ItemStack.EMPTY;
    }

    private void requestCallSync() {
        if (homePhoneMode) {
            PhoneNetworkingClient.requestSync(homePhonePos);
        } else {
            PhoneNetworkingClient.requestSync();
        }
    }

    private void requestSaveContact(String number, String suggestedName) {
        if (homePhoneMode) {
            PhoneNetworkingClient.requestSaveContact(number, suggestedName, homePhonePos);
        } else {
            PhoneNetworkingClient.requestSaveContact(number, suggestedName);
        }
    }

    private void requestDeleteContact(String number) {
        if (homePhoneMode) {
            PhoneNetworkingClient.requestDeleteContact(number, homePhonePos);
        } else {
            PhoneNetworkingClient.requestDeleteContact(number);
        }
    }

    private HomePhoneBlockEntity getClientHomePhone() {
        if (!homePhoneMode || homePhonePos == null) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return null;
        }

        return minecraft.level.getBlockEntity(homePhonePos) instanceof HomePhoneBlockEntity homePhone ? homePhone : null;
    }

    private boolean isCallScreenLocked() {
        if (homePhoneMode) {
            return false;
        }

        PhoneCallState state = PhoneClientCallState.getState();
        if (state == PhoneCallState.OUTGOING_RINGING
                || state == PhoneCallState.INCOMING_RINGING
                || state == PhoneCallState.CONNECTED) {
            return true;
        }

        return callSessionMode && !activeCallNumber.isEmpty() && !activeCallMissed;
    }

    private void closeScreenKeepCall() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private void showStatus(String translationKey, Object... args) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(translationKey, args), true);
        }
    }

    private void renderCaptureFlash(GuiGraphics guiGraphics) {
        int flashX = displayX - Math.round(6 * scale);
        int flashY = displayY - Math.round(6 * scale);
        int flashWidth = displayWidth + Math.round(12 * scale);
        int flashHeight = displayHeight + Math.round(12 * scale);
        int alpha = switch (captureFlashTicks) {
            case 3 -> 220;
            case 2 -> 132;
            default -> 72;
        };

        guiGraphics.fill(flashX, flashY, flashX + flashWidth, flashY + flashHeight,
                (alpha << 24) | 0x00FFFFFF);
    }

    private void renderPhoneFrame(GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(frameX, frameY, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.blit(FRAME_TEXTURE, 0, 0, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);
        guiGraphics.pose().popPose();
    }

    public boolean isCameraModeActive() {
        return cameraMode;
    }

    public boolean isSelfieCameraModeActive() {
        return cameraMode && selfieCameraMode;
    }

    public boolean isHomePhoneMode() {
        return homePhoneMode;
    }

    InteractionHand getOpenHand() {
        return openHand;
    }

    Font getScreenFont() {
        return font;
    }

}
