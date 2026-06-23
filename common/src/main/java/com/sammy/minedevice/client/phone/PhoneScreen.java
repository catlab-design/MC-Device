package com.sammy.minedevice.client.phone;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.sammy.minedevice.Minedevice;
import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.block.entity.HomePhoneBlockEntity;
import com.sammy.minedevice.item.PhoneItem;
import com.sammy.minedevice.phone.PhoneCallState;
import com.sammy.minedevice.phone.PhoneChatData;
import com.sammy.minedevice.phone.PhoneChatMessage;
import com.sammy.minedevice.phone.PhoneContact;
import com.sammy.minedevice.phone.PhoneData;
import com.sammy.minedevice.phone.PhoneNetworking;
import com.sammy.minedevice.phone.PhoneWallpaperData;
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
import net.minecraft.world.entity.player.Player;
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
            "textures/gui/phone_gui/phone_frame.png");
    private static final ResourceLocation FRAME_BASE_TINT_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_frame_base_tint.png");
    private static final ResourceLocation FRAME_EDGE_TINT_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_frame_edge_tint.png");
    private static final ResourceLocation FRAME_HIGHLIGHT_TINT_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_frame_highlight_tint.png");
    private static final ResourceLocation UNLOCK_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_unlock.png");
    private static final ResourceLocation LEFT_NAV_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_nav_left.png");
    private static final ResourceLocation RIGHT_NAV_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_nav_right.png");
    private static final ResourceLocation HOME_NAV_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_nav_home.png");
    private static final ResourceLocation APP_CALL_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_app_call.png");
    private static final ResourceLocation APP_CHAT_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_app_chat.png");
    private static final ResourceLocation APP_SHOP_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_app_shop.png");
    private static final ResourceLocation APP_GOOGLE_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_app_setting.png");
    private static final ResourceLocation APP_GALLERY_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_app_gallery.png");
    private static final ResourceLocation APP_BANK_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_app_bank.png");
    private static final ResourceLocation APP_CAMERA_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/phone_app_camera.png");
    static final ResourceLocation CALL_MENU_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/call_menu.png");
    static final ResourceLocation LIST_MENU_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/list_menu.png");
    private static final ResourceLocation SHUTTER_BUTTON_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/shutter_button.png");
    private static final ResourceLocation CAMFLIP_BUTTON_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/camflip_button.png");
    private static final ResourceLocation ROTATE_BUTTON_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/rotate_button.png");
    static final ResourceLocation DELETE_BUTTON_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/delete_button.png");
    static final ResourceLocation ANSWER_BUTTON_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/answer_button.png");
    static final ResourceLocation HANGUP_BUTTON_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/phone_gui/hangup_button.png");
    static final ResourceLocation DEFAULT_BACKGROUND_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/wallpaper/default.png");
    private static final int FRAME_WIDTH = 160;
    private static final int FRAME_HEIGHT = 336;
    private static final float FRAME_SCALE = 0.85F;
    private static final int DISPLAY_X = 12;
    private static final int DISPLAY_Y = 24;
    static final int DISPLAY_WIDTH = 136;
    static final int DISPLAY_HEIGHT = 292;
    private static final int UNLOCK_TEXTURE_SIZE = 24;
    private static final int CAMERA_VIEW_X = 9;
    private static final int CAMERA_VIEW_Y = 27;
    private static final int CAMERA_VIEW_WIDTH = 142;
    private static final int CAMERA_VIEW_HEIGHT = 266;
    private static final int CAMERA_VIEW_SIDE_TRIM = 3;
    private static final int ICON_SIZE = 30;
    private static final int ICON_TEXTURE_SIZE = 30;
    private static final int NAV_LEFT_TEXTURE_WIDTH = 22;
    private static final int NAV_LEFT_TEXTURE_HEIGHT = 21;
    private static final int NAV_RIGHT_TEXTURE_WIDTH = 22;
    private static final int NAV_RIGHT_TEXTURE_HEIGHT = 21;
    private static final int NAV_HOME_TEXTURE_WIDTH = 27;
    private static final int NAV_HOME_TEXTURE_HEIGHT = 24;
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
    private static final int BANK_SCAN_HOLD_TICKS = 28;
    static final int BANK_PAGE_HOME = 0;
    static final int BANK_PAGE_TRANSFER = 1;
    static final int BANK_PAGE_SCAN = 2;
    static final int BANK_PAGE_PAYMENT = 3;
    static final int BANK_PAGE_RECEIVE = 4;
    static final int BANK_PAGE_SLIP = 5;
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
    boolean landscapeMode;
    boolean cameraMode;
    boolean galleryMode;
    boolean photoViewerMode;
    boolean fullScreenPhotoMode;
    float photoZoom = 1.0F;
    boolean callAppMode;
    boolean callContactsMode;
    boolean callSessionMode;
    boolean addContactMode;
    String addContactName = "";
    String addContactNumber = "";
    boolean addContactNameFieldFocused = true;
    String editingContactOriginalNumber = null;
    boolean chatAppMode;
    boolean chatThreadMode;
    boolean bankAppMode;
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
    String bankTransferNumber = "";
    String bankTransferAmount = "";
    String bankPaymentName = "";
    String bankSlipTargetNumber = "";
    String bankSlipTargetName = "";
    String bankSlipTime = "";
    Component bankStatus = Component.empty();
    long bankBalance;
    long bankSlipAmount;
    long bankSlipBalance;
    int bankPage = BANK_PAGE_HOME;
    private boolean bankReceiveActive;
    private boolean closingForBankReceiveWorld;
    private UUID bankScanHoverTargetId;
    private int bankScanHoverTicks;
    private int bankScanRequestCooldown;
    boolean activeCallIncoming;
    boolean activeCallConnected;
    boolean activeCallMissed;
    int activeCallTicks;
    boolean callMuted;
    boolean callSpeakerEnabled;
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
    private boolean savedRawMouseMotion;
    private boolean rawMouseMotionChanged;
    private final InteractionHand openHand;
    private final BlockPos homePhonePos;
    private final boolean homePhoneMode;
    private final boolean startBankReceivePage;
    private final PhonePhotoStore photoStore = new PhonePhotoStore(Minedevice.MOD_ID);
    private PhoneScreenLayout layoutState;
    private int callSyncCooldown;
    private int bankSyncCooldown;
    boolean settingsAppMode;
    private double lastMouseX = -1;
    private double lastMouseY = -1;
    // Wallpaper context menu state (gallery right-click)
    int wallpaperContextPhotoIndex = -1;
    boolean wallpaperContextMenuOpen;
    // Settings wallpaper picker state
    int settingsWallpaperPage; // 0 = main, 1 = pick lock, 2 = pick home

    public PhoneScreen() {
        this(InteractionHand.MAIN_HAND);
    }

    public PhoneScreen(InteractionHand openHand) {
        this(openHand, false);
    }

    private PhoneScreen(InteractionHand openHand, boolean startBankReceivePage) {
        super(Component.translatable("item.minedevice.phone"));
        this.openHand = openHand;
        this.homePhonePos = null;
        this.homePhoneMode = false;
        this.startBankReceivePage = startBankReceivePage;
    }

    public PhoneScreen(BlockPos homePhonePos) {
        super(Component.translatable("block.minedevice.home_phone"));
        this.openHand = InteractionHand.MAIN_HAND;
        this.homePhonePos = homePhonePos == null ? null : homePhonePos.immutable();
        this.homePhoneMode = this.homePhonePos != null;
        this.startBankReceivePage = false;
        this.unlocked = true;
        this.callAppMode = true;
    }

    static PhoneScreen bankReceiveScreen(InteractionHand openHand) {
        return new PhoneScreen(openHand == null ? InteractionHand.MAIN_HAND : openHand, true);
    }

    @Override
    protected void init() {
        updateLayout();
        rebuildWidgets();
        requestCallSync();
        applyCallStateFromServer();
        if (startBankReceivePage && !homePhoneMode) {
            openBankReceivePage();
        }
    }

    @Override
    public void onClose() {
        if (!closingForBankReceiveWorld) {
            setBankReceiveActive(false);
        }
        closeCamera();
        galleryMode = false;
        photoViewerMode = false;
        callAppMode = false;
        callContactsMode = false;
        
        callSessionMode = false;
        addContactMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        bankAppMode = false;
        capturePending = false;
        captureFlashTicks = 0;
        galleryPage = 0;
        viewerPhotoIndex = -1;
        settingsAppMode = false;
        wallpaperContextMenuOpen = false;
        wallpaperContextPhotoIndex = -1;
        settingsWallpaperPage = 0;
        dialedNumber = "";
        activeCallNumber = "";
        activeCallName = "";
        chatFriendNumber = "";
        chatDraft = "";
        activeChatNumber = "";
        activeChatName = "";
        chatDeleteTargetNumber = "";
        bankTransferNumber = "";
        bankTransferAmount = "";
        bankPaymentName = "";
        bankSlipTargetNumber = "";
        bankSlipTargetName = "";
        bankSlipTime = "";
        bankStatus = Component.empty();
        bankBalance = 0L;
        addContactName = "";
        addContactNumber = "";
        addContactNameFieldFocused = true;
        bankSlipAmount = 0L;
        bankSlipBalance = 0L;
        bankPage = BANK_PAGE_HOME;
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
        bankSyncCooldown = 0;
        bankScanHoverTargetId = null;
        bankScanHoverTicks = 0;
        bankScanRequestCooldown = 0;
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
        } else if (bankAppMode) {
            addBankWidgets();
        } else if (cameraMode && !isBankScanCameraMode()) {
            addCameraWidgets();
        } else if (galleryMode) {
            addGalleryWidgets();
        } else if (settingsAppMode) {
            addSettingsWidgets();
        } else if (addContactMode) {
            // no widgets for add contact page
        } else if (unlocked) {
            addHomeWidgets();
        } else {
            addLockWidgets();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (photoViewerMode && fullScreenPhotoMode) {
            guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
            List<PhotoEntry> photos = getPhotos();
            if (!photos.isEmpty()) {
                viewerPhotoIndex = Mth.clamp(viewerPhotoIndex, 0, photos.size() - 1);
                PhotoEntry photoEntry = photos.get(viewerPhotoIndex);
                boolean renderedImage = false;
                if (photoEntry.hasFile()) {
                    PhotoTexture photoTexture = getOrLoadPhotoTexture(photoEntry.fileName, false);
                    if (photoTexture != null) {
                        PhoneScreenDraw.drawTextureInArea(guiGraphics, photoTexture.textureId, photoTexture.width, photoTexture.height,
                                0, 0, this.width, this.height, photoZoom, true);
                        renderedImage = true;
                    }
                }
                if (!renderedImage) {
                    if (!photoEntry.itemStack.isEmpty()) {
                        int itemX = (this.width - 16) / 2;
                        int itemY = (this.height - 16) / 2;
                        guiGraphics.renderItem(photoEntry.itemStack, itemX, itemY);
                        guiGraphics.renderItemDecorations(this.font, photoEntry.itemStack, itemX, itemY);
                    } else {
                        Component missingText = Component.translatable("screen.minedevice.phone.viewer.missing");
                        int missingMaxWidth = Math.max(28, this.width - 20);
                        float missingScale = PhoneScreenDraw.textScaleToFit(this.font, missingText, missingMaxWidth);
                        int missingWidth = PhoneScreenDraw.scaledTextWidth(this.font, missingText, missingScale);
                        int missingHeight = PhoneScreenDraw.scaledTextHeight(this.font, missingScale);
                        int missingX = (this.width - missingWidth) / 2;
                        int missingY = (this.height - missingHeight) / 2;
                        PhoneScreenDraw.drawScaledText(guiGraphics, this.font, missingText, missingX, missingY,
                                0xFF8E8E93, false, missingScale);
                    }
                }
            }
            return;
        }

        boolean isLandscape = landscapeMode;
        double renderMouseX = mouseX;
        double renderMouseY = mouseY;
        if (isLandscape) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(frameX, frameY + Math.round(FRAME_WIDTH * scale), 0.0F);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-90.0F));
            guiGraphics.pose().translate(-frameX, -frameY, 0.0F);

            int frameWidthPortrait = Math.round(FRAME_WIDTH * scale);
            renderMouseX = frameX + frameY + frameWidthPortrait - mouseY;
            renderMouseY = mouseX - frameX + frameY;
        }

        boolean bankScanCameraMode = isBankScanCameraMode();
        if (cameraMode) {
            renderSurface(guiGraphics, partialTick);
            if (bankScanCameraMode) {
                renderBankCameraOverlay(guiGraphics);
            }
            renderPhoneFrame(guiGraphics);
        } else {
            if (callSessionMode || callAppMode || callContactsMode || addContactMode || chatAppMode || chatThreadMode || bankAppMode) {
                renderCallBackdrop(guiGraphics);
            } else if (galleryMode || photoViewerMode) {
                renderMediaBackdrop(guiGraphics);
            } else if (settingsAppMode) {
                renderSettingsBackdrop(guiGraphics);
            } else {
                int bgX = displayX - Math.round(6 * scale);
                int bgY = displayY - Math.round(6 * scale);
                int bgWidth = displayWidth + Math.round(12 * scale);
                int bgHeight = displayHeight + Math.round(12 * scale);

                ResourceLocation wallpaperTexture = resolveWallpaperTexture(false);
                if (wallpaperTexture != null) {
                    guiGraphics.blit(wallpaperTexture, bgX, bgY, 0, 0, bgWidth, bgHeight,
                            DISPLAY_WIDTH, DISPLAY_HEIGHT);
                } else {
                    // Photo wallpaper — rendered in renderSurface
                    guiGraphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0xFF000000);
                }
            }

            renderSurface(guiGraphics, partialTick);
            renderPhoneFrame(guiGraphics);
        }
        boolean hideWidgetsForCapture = cameraMode && capturePending;
        if (!hideWidgetsForCapture) {
            super.render(guiGraphics, (int) renderMouseX, (int) renderMouseY, partialTick);
        }

        if (hideWidgetsForCapture) {
            completePendingCapture();
        }

        if (cameraMode && captureFlashTicks > 0) {
            renderCaptureFlash(guiGraphics);
            renderPhoneFrame(guiGraphics);
        }

        if (isLandscape) {
            guiGraphics.pose().popPose();
        }

        if (cameraMode && !bankScanCameraMode) {
            renderCameraOverlayHints(guiGraphics);
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
        if (bankAppMode) {
            if (bankSyncCooldown <= 0) {
                PhoneNetworkingClient.requestBankSync();
                bankSyncCooldown = 20;
            } else {
                bankSyncCooldown--;
            }
        }
        applyCallStateFromServer();
        if (isCallScreenLocked()
                && (cameraMode || galleryMode || photoViewerMode || callAppMode || callContactsMode
                || chatAppMode || chatThreadMode || bankAppMode || !callSessionMode)) {
            setBankReceiveActive(false);
            closeCamera();
            cameraMode = false;
            galleryMode = false;
            photoViewerMode = false;
            callAppMode = false;
            callContactsMode = false;
            chatAppMode = false;
            chatThreadMode = false;
            bankAppMode = false;
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

        if (cameraMode && cameraMoveMode) {
            syncCameraMovementKeys();
        }

        if (bankScanRequestCooldown > 0) {
            bankScanRequestCooldown--;
        }

        if (isBankScanCameraMode()) {
            tickBankScanHover();
        } else {
            resetBankScanHover();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (cameraMode && cameraMoveMode && handleMovementKey(keyCode, scanCode, true)) {
            return false;
        }

        if (isBankScanCameraMode() && keyCode == InputConstants.KEY_ESCAPE) {
            if (cameraMoveMode) {
                setCameraMoveMode(false, lastCameraMoveMouseX, lastCameraMoveMouseY);
                return true;
            }
            closeBankScanCamera();
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

        if (isBankScanCameraMode() && !cameraMoveMode && (keyCode == InputConstants.KEY_SPACE
                || keyCode == InputConstants.KEY_RETURN
                || keyCode == InputConstants.KEY_NUMPADENTER)) {
            tryScanBankPaymentTarget();
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

        if (addContactMode && keyCode == InputConstants.KEY_ESCAPE) {
            editingContactOriginalNumber = null;
            addContactMode = false;
            callContactsMode = true;
            rebuildWidgets();
            return true;
        }

        if (addContactMode && (keyCode == InputConstants.KEY_BACKSPACE || keyCode == InputConstants.KEY_DELETE)) {
            if (addContactNameFieldFocused && !addContactName.isEmpty()) {
                addContactName = addContactName.substring(0, addContactName.length() - 1);
            } else if (!addContactNameFieldFocused && !addContactNumber.isEmpty()) {
                addContactNumber = addContactNumber.substring(0, addContactNumber.length() - 1);
            }
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

        if (bankAppMode && keyCode == InputConstants.KEY_ESCAPE) {
            if (bankPage == BANK_PAGE_TRANSFER || bankPage == BANK_PAGE_PAYMENT
                    || bankPage == BANK_PAGE_RECEIVE || bankPage == BANK_PAGE_SLIP) {
                openBankHomePage();
                return true;
            }
            closeBankApp();
            return true;
        }

        if (settingsAppMode && keyCode == InputConstants.KEY_ESCAPE) {
            if (settingsWallpaperPage != 0) {
                settingsWallpaperPage = 0;
                rebuildWidgets();
                return true;
            }
            closeSettingsApp();
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

        if (bankAppMode && acceptsBankNumericInput()) {
            String digit = getDigitForKey(keyCode);
            if (digit != null) {
                appendBankDigit(digit);
                return true;
            }

            if (keyCode == InputConstants.KEY_BACKSPACE) {
                removeLastBankDigit();
                return true;
            }

            if ((keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)
                    && canSendBankTransfer()) {
                sendBankTransfer();
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
            if (fullScreenPhotoMode) {
                fullScreenPhotoMode = false;
                photoZoom = 1.0F;
                rebuildWidgets();
                return true;
            }
            photoViewerMode = false;
            galleryMode = true;
            rebuildWidgets();
            return true;
        }

        if (photoViewerMode && !fullScreenPhotoMode && keyCode == InputConstants.KEY_LEFT) {
            stepViewer(-1);
            return true;
        }

        if (photoViewerMode && !fullScreenPhotoMode && keyCode == InputConstants.KEY_RIGHT) {
            stepViewer(1);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (cameraMode && cameraMoveMode && handleMovementKey(keyCode, scanCode, false)) {
            return false;
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

        if (bankAppMode && acceptsBankNumericInput() && Character.isDigit(codePoint)) {
            appendBankDigit(String.valueOf(codePoint));
            return true;
        }

        if (addContactMode) {
            if (Character.isISOControl(codePoint)) {
                return false;
            }
            if (addContactNameFieldFocused) {
                if (addContactName.length() < PhoneData.MAX_CONTACT_NAME_LENGTH) {
                    addContactName += codePoint;
                }
            } else {
                if (addContactNumber.length() < PhoneData.PHONE_NUMBER_LENGTH && Character.isDigit(codePoint)) {
                    addContactNumber += codePoint;
                }
            }
            return true;
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && photoViewerMode && fullScreenPhotoMode) {
            fullScreenPhotoMode = false;
            photoZoom = 1.0F;
            rebuildWidgets();
            return true;
        }

        double rawMouseX = mouseX;
        double rawMouseY = mouseY;
        if (landscapeMode) {
            int frameWidthPortrait = Math.round(FRAME_WIDTH * scale);
            double rotatedMouseX = frameX + frameY + frameWidthPortrait - mouseY;
            double rotatedMouseY = mouseX - frameX + frameY;
            mouseX = rotatedMouseX;
            mouseY = rotatedMouseY;
        }

        if (isBankScanCameraMode()) {
            if (button == 0 && getBankScanReceiveButtonBounds().contains(mouseX, mouseY)) {
                openBankReceivePage();
                return true;
            }

            if (button == 0 && getBankScanEyeButtonBounds().contains(mouseX, mouseY)) {
                setCameraMoveModeRotated(true, rawMouseX, rawMouseY);
                return true;
            }

            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            if (handled) {
                return true;
            }

            if ((button == 0 || button == 1) && getCameraViewBounds().contains(mouseX, mouseY) && !cameraMoveMode) {
                tryScanBankPaymentTarget();
                return true;
            }

            return false;
        }

        if (cameraMode && button == 1 && !capturePending) {
            requestPhotoCapture();
            return true;
        }

        if (cameraMode && cameraMoveMode && button == 0) {
            setCameraMoveModeRotated(false, rawMouseX, rawMouseY);
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

            if (activeCallIncoming && !activeCallConnected) {
                UiRect connectBounds = getCallConnectButtonBounds();
                int gap = Math.max(5, Math.round(6 * scale));
                UiRect incomingHangupBounds = new UiRect(connectBounds.right() + gap, connectBounds.top,
                        connectBounds.width, connectBounds.height);
                if (incomingHangupBounds.contains(mouseX, mouseY)) {
                    endCallSession(true);
                    return true;
                }
            } else if (getCallHangupButtonBounds().contains(mouseX, mouseY)) {
                endCallSession(true);
                return true;
            }

            if (getCallMuteButtonBounds().contains(mouseX, mouseY)) {
                callMuted = !callMuted;
                if (homePhoneMode) {
                    PhoneNetworkingClient.requestToggleMute(homePhonePos);
                } else {
                    PhoneNetworkingClient.requestToggleMute();
                }
                return true;
            }

            if (getCallSpeakerButtonBounds().contains(mouseX, mouseY)) {
                callSpeakerEnabled = !callSpeakerEnabled;
                if (homePhoneMode) {
                    PhoneNetworkingClient.requestToggleSpeaker(homePhonePos);
                } else {
                    PhoneNetworkingClient.requestToggleSpeaker();
                }
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

        if (button == 0 && callContactsMode && !addContactMode) {
            if (getCallDialMenuBounds().contains(mouseX, mouseY)) {
                openCallDialPage();
                return true;
            }

            if (getCallListMenuBounds().contains(mouseX, mouseY)) {
                return true;
            }

            if (getAddContactHeaderButtonBounds().contains(mouseX, mouseY)) {
                openAddContactPage();
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
                } else if (getContactEditButtonBounds(contactIndex, contacts.size()).contains(mouseX, mouseY)) {
                    openEditContactPage(contact);
                } else {
                    startCallSession(contact.number());
                }
                return true;
            }
        }

        if (button == 0 && addContactMode) {
            if (getAddContactNameBounds().contains(mouseX, mouseY)) {
                addContactNameFieldFocused = true;
                return true;
            }

            if (getAddContactNumberBounds().contains(mouseX, mouseY)) {
                addContactNameFieldFocused = false;
                return true;
            }

            if (getAddContactCancelButtonBounds().contains(mouseX, mouseY)) {
                editingContactOriginalNumber = null;
                addContactMode = false;
                callContactsMode = true;
                rebuildWidgets();
                return true;
            }

            if (getAddContactSaveButtonBounds().contains(mouseX, mouseY)) {
                String number = PhoneData.normalizePhoneNumber(addContactNumber);
                if (!number.isEmpty()) {
                    if (editingContactOriginalNumber != null && !editingContactOriginalNumber.equals(number)) {
                        requestDeleteContact(editingContactOriginalNumber);
                    }
                    String displayName = addContactName.isBlank() ? number : addContactName;
                    requestSaveContact(number, displayName);
                    editingContactOriginalNumber = null;
                    addContactMode = false;
                    callContactsMode = true;
                    rebuildWidgets();
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

        if (button == 0 && bankAppMode) {
            if (bankPage == BANK_PAGE_HOME && getBankHomeTransferButtonBounds().contains(mouseX, mouseY)) {
                openBankTransferPage();
                return true;
            }

            if (bankPage == BANK_PAGE_HOME && getBankHomeScanButtonBounds().contains(mouseX, mouseY)) {
                openBankScanCamera();
                return true;
            }

            if (bankPage == BANK_PAGE_TRANSFER && getBankTransferButtonBounds().contains(mouseX, mouseY)
                    && canSendBankTransfer()) {
                sendBankTransfer();
                return true;
            }

            if (bankPage == BANK_PAGE_PAYMENT && getBankTransferButtonBounds().contains(mouseX, mouseY)
                    && canSendBankTransfer()) {
                sendBankTransfer();
                return true;
            }

            if (bankPage == BANK_PAGE_RECEIVE && getBankReceiveEyeButtonBounds().contains(mouseX, mouseY)) {
                showBankReceiveQrToWorld();
                return true;
            }

            if ((bankPage == BANK_PAGE_TRANSFER || bankPage == BANK_PAGE_PAYMENT
                    || bankPage == BANK_PAGE_RECEIVE || bankPage == BANK_PAGE_SLIP)
                    && getBankCancelButtonBounds().contains(mouseX, mouseY)) {
                openBankHomePage();
                return true;
            }
        }

        if (button == 0 && galleryMode) {
            // Close context menu if open and clicked outside
            if (wallpaperContextMenuOpen) {
                wallpaperContextMenuOpen = false;
                wallpaperContextPhotoIndex = -1;
                rebuildWidgets();
                return true;
            }
            int clickedPhotoIndex = getGalleryPhotoIndexAt(mouseX, mouseY, getPhotos());
            if (clickedPhotoIndex >= 0) {
                openPhotoViewer(clickedPhotoIndex);
                return true;
            }
        }

        // Right-click in gallery → open "Set as wallpaper" context menu
        if (button == 1 && galleryMode) {
            int clickedPhotoIndex = getGalleryPhotoIndexAt(mouseX, mouseY, getPhotos());
            if (clickedPhotoIndex >= 0) {
                wallpaperContextPhotoIndex = clickedPhotoIndex;
                wallpaperContextMenuOpen = true;
                rebuildWidgets();
                return true;
            }
        }

        // Settings app clicks
        if (button == 0 && settingsAppMode) {
            return PhoneSettingsSurfaceRenderer.handleClick(this, mouseX, mouseY);
        }

        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) {
            return true;
        }

        if (button == 0 && photoViewerMode && !fullScreenPhotoMode) {
            ViewerLayout viewerLayout = getViewerLayout();
            if (mouseX >= viewerLayout.areaX && mouseX <= viewerLayout.areaX + viewerLayout.areaWidth
                    && mouseY >= viewerLayout.areaY && mouseY <= viewerLayout.areaY + viewerLayout.areaHeight) {
                fullScreenPhotoMode = true;
                photoZoom = 1.0F;
                rebuildWidgets();
                return true;
            }
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
        if (landscapeMode) {
            int frameWidthPortrait = Math.round(FRAME_WIDTH * scale);
            double rotatedMouseX = frameX + frameY + frameWidthPortrait - mouseY;
            double rotatedMouseY = mouseX - frameX + frameY;
            double rotatedDragX = -dragY;
            double rotatedDragY = dragX;
            return super.mouseDragged(rotatedMouseX, rotatedMouseY, button, rotatedDragX, rotatedDragY);
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

        if (landscapeMode) {
            int frameWidthPortrait = Math.round(FRAME_WIDTH * scale);
            double rotatedMouseX = frameX + frameY + frameWidthPortrait - mouseY;
            double rotatedMouseY = mouseX - frameX + frameY;
            mouseX = rotatedMouseX;
            mouseY = rotatedMouseY;
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (cameraMode && cameraMoveMode && button == 0) {
            return true;
        }
        if (landscapeMode) {
            int frameWidthPortrait = Math.round(FRAME_WIDTH * scale);
            double rotatedMouseX = frameX + frameY + frameWidthPortrait - mouseY;
            double rotatedMouseY = mouseX - frameX + frameY;
            mouseX = rotatedMouseX;
            mouseY = rotatedMouseY;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (landscapeMode) {
            int frameWidthPortrait = Math.round(FRAME_WIDTH * scale);
            double rotatedMouseX = frameX + frameY + frameWidthPortrait - mouseY;
            double rotatedMouseY = mouseX - frameX + frameY;
            mouseX = rotatedMouseX;
            mouseY = rotatedMouseY;
        }
        if (photoViewerMode && fullScreenPhotoMode && scrollDelta != 0.0D) {
            photoZoom = Mth.clamp(photoZoom + (float) scrollDelta * 0.2F, 1.0F, 5.0F);
            return true;
        }
        if (cameraMode && scrollDelta != 0.0D) {
            if (isBankScanCameraMode()) {
                return true;
            }
            adjustCameraZoom(scrollDelta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    private void updateLayout() {
        if (!cameraMode) {
            landscapeMode = false;
        }
        float fitScale;
        if (landscapeMode) {
            fitScale = Math.min((float) width / FRAME_HEIGHT, (float) height / FRAME_WIDTH);
            scale = Math.min(FRAME_SCALE, fitScale * FRAME_SCALE);
            frameWidth = Math.round(FRAME_HEIGHT * scale);
            frameHeight = Math.round(FRAME_WIDTH * scale);
        } else {
            fitScale = Math.min((float) width / FRAME_WIDTH, (float) height / FRAME_HEIGHT);
            scale = Math.min(FRAME_SCALE, fitScale * FRAME_SCALE);
            frameWidth = Math.round(FRAME_WIDTH * scale);
            frameHeight = Math.round(FRAME_HEIGHT * scale);
        }
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
        int y = displayY + displayHeight - scaledIconSize - Math.round(44 * scale);

        addRenderableWidget(new ImageButton(
                x, y, scaledIconSize, scaledIconSize,
                0, 0, 0, UNLOCK_TEXTURE, UNLOCK_TEXTURE_SIZE, UNLOCK_TEXTURE_SIZE,
                button -> {
                    unlocked = true;
                    rebuildWidgets();
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.width, this.height, 0.0F, 0.0F,
                        UNLOCK_TEXTURE_SIZE, UNLOCK_TEXTURE_SIZE, UNLOCK_TEXTURE_SIZE, UNLOCK_TEXTURE_SIZE);
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
        int rowGap = Math.round(23 * scale);

        int iconX1 = displayX + leftInset;
        int iconX2 = iconX1 + scaledIconSize + iconGap;
        int iconX3 = iconX2 + scaledIconSize + iconGap;
        int iconX4 = iconX3 + scaledIconSize + iconGap;
        int iconY1 = displayY + Math.round(displayHeight * 0.40F);
        int iconY2 = iconY1 + scaledIconSize + rowGap;

        addRenderableWidget(createAppIcon(iconX1, iconY1, APP_CALL_TEXTURE, Component.literal("Call"), this::openCallApp));
        addRenderableWidget(createAppIcon(iconX2, iconY1, APP_CHAT_TEXTURE, Component.literal("Chat"), this::openChatApp));
        addRenderableWidget(createAppIcon(iconX3, iconY1, APP_SHOP_TEXTURE, Component.literal("Shop"), () -> {
        }));
        addRenderableWidget(createAppIcon(iconX4, iconY1, APP_GOOGLE_TEXTURE, Component.literal("Settings"), this::openSettingsApp));

        addRenderableWidget(createAppIcon(iconX1, iconY2, APP_GALLERY_TEXTURE, Component.literal("Gallery"), this::openGallery));
        addRenderableWidget(createAppIcon(iconX2, iconY2, APP_BANK_TEXTURE, Component.literal("Bank"), this::openBankApp));
        addRenderableWidget(createAppIcon(iconX3, iconY2, APP_CAMERA_TEXTURE, Component.literal("Camera"), () -> {
            openCamera();
        }));

        addNavigationButtons();
    }

    private ImageButton createAppIcon(int x, int y, ResourceLocation texture, Component label, Runnable clickAction) {
        int scaledIconSize = Math.round(ICON_SIZE * scale);
        return new ImageButton(
                x, y, scaledIconSize, scaledIconSize,
                0, 0, 0, texture, ICON_SIZE, ICON_SIZE,
                button -> clickAction.run()) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.width, this.height, 0.0F, 0.0F,
                        ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft == null) {
                    return;
                }

                Font font = minecraft.font;
                int labelWidth = Math.max(this.width + Math.round(18 * scale), Math.round(46 * scale));
                float labelScale = Math.min(0.46F, (float) labelWidth / Math.max(1, font.width(label)));
                int labelTextWidth = PhoneScreenDraw.scaledTextWidth(font, label, labelScale);
                int labelX = this.getX() + (this.width - labelTextWidth) / 2;
                int labelY = this.getY() + this.height + Math.max(2, Math.round(3 * scale));
                int labelHeight = PhoneScreenDraw.scaledTextHeight(font, labelScale);
                int paddingX = Math.max(2, Math.round(3 * scale));
                int paddingY = Math.max(1, Math.round(2 * scale));
                guiGraphics.fill(labelX - paddingX, labelY - paddingY,
                        labelX + labelTextWidth + paddingX, labelY + labelHeight + paddingY,
                        0x80000000);
                PhoneScreenDraw.drawScaledText(guiGraphics, font, label, labelX, labelY, 0xFFFFFFFF, false, labelScale);
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

    private void addBankWidgets() {
        addNavigationButtons();
    }

    private void addSettingsWidgets() {
        addNavigationButtons();
    }

    private void addPhotoViewerWidgets() {
        if (fullScreenPhotoMode) {
            return;
        }
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
                button -> setCameraMoveModeRotated(true,
                        button.getX() + (button.getWidth() / 2.0D),
                        button.getY() + (button.getHeight() / 2.0D))) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.width, this.height, 0.0F, 0.0F,
                        60, 60, 60, 60);
            }
        });

        addRotateButton();
        addNavigationButtons();
    }

    private void addNavigationButtons() {
        int navSlotSize = Math.round((ICON_SIZE * 0.85F) * scale);
        int navY = displayY + displayHeight - navSlotSize + Math.round(6 * scale);
        int navLeftX = displayX + Math.round(8 * scale);
        int navCenterX = displayX + (displayWidth - navSlotSize) / 2;
        int navRightX = displayX + displayWidth - navSlotSize - Math.round(8 * scale);

        addRenderableWidget(new ImageButton(
                navLeftX, navY, navSlotSize, navSlotSize,
                0, 0, 0, LEFT_NAV_TEXTURE, NAV_LEFT_TEXTURE_WIDTH, NAV_LEFT_TEXTURE_HEIGHT,
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

                    if (bankAppMode) {
                        closeBankApp();
                        return;
                    }

                    if (settingsAppMode) {
                        closeSettingsApp();
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
                renderNavButton(guiGraphics, this.getX(), this.getY(), this.width,
                        LEFT_NAV_TEXTURE, NAV_LEFT_TEXTURE_WIDTH, NAV_LEFT_TEXTURE_HEIGHT);
            }
        });

        addRenderableWidget(new ImageButton(
                navCenterX, navY, navSlotSize, navSlotSize,
                0, 0, 0, HOME_NAV_TEXTURE, NAV_HOME_TEXTURE_WIDTH, NAV_HOME_TEXTURE_HEIGHT,
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

                    if (bankAppMode) {
                        closeBankApp();
                        return;
                    }

                    if (settingsAppMode) {
                        closeSettingsApp();
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
                renderNavButton(guiGraphics, this.getX(), this.getY(), this.width,
                        HOME_NAV_TEXTURE, NAV_HOME_TEXTURE_WIDTH, NAV_HOME_TEXTURE_HEIGHT);
            }
        });

        addRenderableWidget(new ImageButton(
                navRightX, navY, navSlotSize, navSlotSize,
                0, 0, 0, RIGHT_NAV_TEXTURE, NAV_RIGHT_TEXTURE_WIDTH, NAV_RIGHT_TEXTURE_HEIGHT,
                button -> {
                    if (photoViewerMode) {
                        stepViewer(1);
                        return;
                    }

                    if (callSessionMode || callAppMode || callContactsMode || chatThreadMode || chatAppMode || bankAppMode) {
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
                renderNavButton(guiGraphics, this.getX(), this.getY(), this.width,
                        RIGHT_NAV_TEXTURE, NAV_RIGHT_TEXTURE_WIDTH, NAV_RIGHT_TEXTURE_HEIGHT);
            }
        });
    }

    private void renderNavButton(GuiGraphics guiGraphics, int slotX, int slotY, int slotSize,
                                 ResourceLocation texture, int textureWidth, int textureHeight) {
        float pixelScale = slotSize / (float) textureHeight;
        int drawWidth = Math.round(textureWidth * pixelScale);
        int drawHeight = Math.round(textureHeight * pixelScale);
        int drawX = slotX + (slotSize - drawWidth) / 2;
        int drawY = slotY + (slotSize - drawHeight) / 2;
        
        // Render with proper texture coordinates
        int color = getOpenPhoneColor();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        guiGraphics.blit(texture, drawX, drawY, drawWidth, drawHeight, 
                        0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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
            if (addContactMode) {
                renderAddContactSurface(guiGraphics);
            }
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

        if (cameraMode) {
            renderCameraSurface(guiGraphics, partialTick);
            return;
        }

        if (bankAppMode) {
            renderBankSurface(guiGraphics);
            return;
        }

        if (galleryMode) {
            renderGallerySurface(guiGraphics);
            return;
        }

        if (settingsAppMode) {
            PhoneSettingsSurfaceRenderer.renderSettingsSurface(this, guiGraphics);
            return;
        }

        if (!unlocked) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                long gameTime = minecraft.level.getDayTime() % 24000L;
                int hours = (int) ((gameTime + 6000) / 1000) % 24;
                int minutes = (int) ((gameTime % 1000) * 60 / 1000);

                Component timeText = Component.literal(String.format("%02d:%02d", hours, minutes));
                float timeScale = Math.max(1.55F, scale * 1.95F);
                int textWidth = PhoneScreenDraw.scaledTextWidth(minecraft.font, timeText, timeScale);
                int textHeight = PhoneScreenDraw.scaledTextHeight(minecraft.font, timeScale);
                int textX = displayX + (displayWidth - textWidth) / 2;
                int textY = displayY + Math.round(displayHeight * 0.24F) - (textHeight / 2);

                PhoneScreenDraw.drawScaledText(guiGraphics, minecraft.font, timeText, textX, textY, 0xFF404040, false, timeScale);
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

    private void renderAddContactSurface(GuiGraphics guiGraphics) {
        PhoneCallSurfaceRenderer.renderAddContactSurface(this, guiGraphics);
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

    private void renderBankSurface(GuiGraphics guiGraphics) {
        PhoneBankSurfaceRenderer.renderBankSurface(this, guiGraphics);
    }

    private void renderBankCameraOverlay(GuiGraphics guiGraphics) {
        PhoneBankSurfaceRenderer.renderBankCameraOverlay(this, guiGraphics);
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

    UiRect getAddContactHeaderButtonBounds() {
        return getLayoutState().addContactHeaderButtonBounds();
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

    UiRect getContactEditButtonBounds(int index, int contactCount) {
        String saveCandidateNumber = getContactSaveCandidateNumber();
        boolean canSaveNumber = !saveCandidateNumber.isEmpty() && !hasSavedContact(saveCandidateNumber);
        return getLayoutState().contactEditButtonBounds(index, contactCount, canSaveNumber);
    }

    private int getContactIndexAt(double mouseX, double mouseY, List<PhoneContact> contacts) {
        String saveCandidateNumber = getContactSaveCandidateNumber();
        boolean canSaveNumber = !saveCandidateNumber.isEmpty() && !hasSavedContact(saveCandidateNumber);
        return getLayoutState().contactIndexAt(mouseX, mouseY, contacts.size(), canSaveNumber);
    }

    UiRect getAddContactPopupBounds() {
        return getLayoutState().addContactPopupBounds();
    }

    UiRect getAddContactNameBounds() {
        return getLayoutState().addContactNameBounds();
    }

    UiRect getAddContactNumberBounds() {
        return getLayoutState().addContactNumberBounds();
    }

    UiRect getAddContactSaveButtonBounds() {
        return getLayoutState().addContactSaveButtonBounds();
    }

    UiRect getAddContactCancelButtonBounds() {
        return getLayoutState().addContactCancelButtonBounds();
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

    UiRect getCallMuteButtonBounds() {
        return getLayoutState().callMuteButtonBounds();
    }

    UiRect getCallSpeakerButtonBounds() {
        return getLayoutState().callSpeakerButtonBounds();
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

    UiRect getBankSurfaceBounds() {
        return new UiRect(displayX, displayY, displayWidth, displayHeight);
    }

    UiRect getBankHeaderBounds() {
        UiRect contentBounds = getBankSurfaceBounds();
        int height = Math.max(28, Math.round(32 * scale));
        return new UiRect(contentBounds.left, contentBounds.top, contentBounds.width, height);
    }

    UiRect getBankAccountBounds() {
        UiRect contentBounds = getBankSurfaceBounds();
        int sidePadding = Math.max(5, Math.round(6 * scale));
        int top = getBankHeaderBounds().bottom() + Math.max(5, Math.round(6 * scale));
        int height = Math.max(32, Math.round(38 * scale)); // Increased height for player head
        return new UiRect(contentBounds.left + sidePadding, top, contentBounds.width - (sidePadding * 2), height);
    }

    UiRect getBankBalanceBounds() {
        UiRect contentBounds = getBankSurfaceBounds();
        int sidePadding = Math.max(5, Math.round(6 * scale));
        int top = getBankAccountBounds().bottom() + Math.max(5, Math.round(6 * scale));
        int height = Math.max(36, Math.round(43 * scale));
        return new UiRect(contentBounds.left + sidePadding, top, contentBounds.width - (sidePadding * 2), height);
    }

    UiRect getBankHomeTransferButtonBounds() {
        UiRect contentBounds = getBankSurfaceBounds();
        int footerHeight = Math.max(58, Math.round(contentBounds.height * 0.24F));
        int sidePadding = Math.max(8, Math.round(10 * scale));
        int buttonGap = Math.max(5, Math.round(6 * scale));
        int height = Math.max(17, Math.round(19 * scale));
        int top = contentBounds.bottom() - footerHeight + Math.max(9, Math.round(10 * scale));
        int width = (contentBounds.width - (sidePadding * 2) - buttonGap) / 2;
        return new UiRect(contentBounds.left + sidePadding, top, width, height);
    }

    UiRect getBankHomeScanButtonBounds() {
        UiRect transferBounds = getBankHomeTransferButtonBounds();
        int buttonGap = Math.max(5, Math.round(6 * scale));
        return new UiRect(transferBounds.right() + buttonGap, transferBounds.top,
                transferBounds.width, transferBounds.height);
    }

    UiRect getBankPaymentTargetBounds() {
        UiRect contentBounds = getBankSurfaceBounds();
        int sidePadding = Math.max(5, Math.round(6 * scale));
        int top = getBankPaymentBalanceBounds().bottom() + Math.max(5, Math.round(6 * scale));
        int height = Math.max(32, Math.round(38 * scale));
        return new UiRect(contentBounds.left + sidePadding, top, contentBounds.width - (sidePadding * 2), height);
    }

    UiRect getBankPaymentBalanceBounds() {
        UiRect contentBounds = getBankSurfaceBounds();
        int sidePadding = Math.max(5, Math.round(6 * scale));
        int top = getBankHeaderBounds().bottom() + Math.max(6, Math.round(7 * scale));
        int height = Math.max(24, Math.round(28 * scale));
        return new UiRect(contentBounds.left + sidePadding, top, contentBounds.width - (sidePadding * 2), height);
    }

    UiRect getBankTransferNumberBounds() {
        UiRect contentBounds = getBankSurfaceBounds();
        int sidePadding = Math.max(5, Math.round(6 * scale));
        int top = getBankHeaderBounds().bottom() + Math.max(8, Math.round(9 * scale));
        int height = Math.max(24, Math.round(28 * scale));
        return new UiRect(contentBounds.left + sidePadding, top, contentBounds.width - (sidePadding * 2), height);
    }

    UiRect getBankTransferAmountBounds() {
        UiRect previousBounds = bankPage == BANK_PAGE_PAYMENT ? getBankPaymentTargetBounds() : getBankTransferNumberBounds();
        int top = previousBounds.bottom() + Math.max(5, Math.round(6 * scale));
        return new UiRect(previousBounds.left, top, previousBounds.width, previousBounds.height);
    }

    UiRect getBankTransferButtonBounds() {
        UiRect contentBounds = getBankSurfaceBounds();
        int sidePadding = Math.max(5, Math.round(6 * scale));
        int top = getBankTransferAmountBounds().bottom() + Math.max(7, Math.round(8 * scale));
        int height = Math.max(18, Math.round(21 * scale));
        if (bankPage == BANK_PAGE_PAYMENT || bankPage == BANK_PAGE_TRANSFER) {
            int buttonGap = Math.max(4, Math.round(5 * scale));
            int width = (contentBounds.width - (sidePadding * 2) - buttonGap) / 2;
            return new UiRect(contentBounds.left + sidePadding + width + buttonGap, top, width, height);
        }
        return new UiRect(contentBounds.left + sidePadding, top, contentBounds.width - (sidePadding * 2), height);
    }

    UiRect getBankCancelButtonBounds() {
        UiRect transferBounds = getBankTransferButtonBounds();
        if (bankPage == BANK_PAGE_PAYMENT || bankPage == BANK_PAGE_TRANSFER) {
            UiRect contentBounds = getBankSurfaceBounds();
            int sidePadding = Math.max(5, Math.round(6 * scale));
            return new UiRect(contentBounds.left + sidePadding, transferBounds.top,
                    transferBounds.width, transferBounds.height);
        }

        if (bankPage == BANK_PAGE_RECEIVE || bankPage == BANK_PAGE_SLIP) {
            UiRect contentBounds = getBankSurfaceBounds();
            int sidePadding = Math.max(8, Math.round(10 * scale));
            int bottomInset = Math.max(24, Math.round(28 * scale));
            int buttonHeight = bankPage == BANK_PAGE_RECEIVE
                    ? Math.max(20, Math.round(24 * scale))
                    : Math.max(18, Math.round(21 * scale));
            int top = contentBounds.bottom() - bottomInset - buttonHeight;
            
            if (bankPage == BANK_PAGE_SLIP) {
                int buttonWidth = Math.min(contentBounds.width - (sidePadding * 2),
                        Math.max(68, Math.round(84 * scale)));
                return new UiRect(contentBounds.left + (contentBounds.width - buttonWidth) / 2,
                        top, buttonWidth, buttonHeight);
            }

            int buttonGap = Math.max(6, Math.round(8 * scale));
            int eyeButtonSize = buttonHeight;
            int eyeButtonX = contentBounds.right() - sidePadding - eyeButtonSize;
            
            int cancelButtonWidth = eyeButtonX - contentBounds.left - sidePadding - buttonGap;
            return new UiRect(contentBounds.left + sidePadding, top, cancelButtonWidth, buttonHeight);
        }
        int top = transferBounds.bottom() + Math.max(5, Math.round(6 * scale));
        return new UiRect(transferBounds.left, top, transferBounds.width, transferBounds.height);
    }

    UiRect getBankReceiveEyeButtonBounds() {
        UiRect contentBounds = getBankSurfaceBounds();
        int sidePadding = Math.max(8, Math.round(10 * scale));
        int bottomInset = Math.max(24, Math.round(28 * scale));
        int buttonHeight = Math.max(20, Math.round(24 * scale));
        int top = contentBounds.bottom() - bottomInset - buttonHeight;
        
        // Eye button (square, right side)
        int eyeButtonSize = buttonHeight;
        int eyeButtonX = contentBounds.right() - sidePadding - eyeButtonSize;
        return new UiRect(eyeButtonX, top, eyeButtonSize, eyeButtonSize);
    }

    UiRect getBankReceiveCardBounds() {
        UiRect contentBounds = getBankSurfaceBounds();
        int sidePadding = Math.max(5, Math.round(6 * scale));
        int top = getBankHeaderBounds().bottom() + Math.max(7, Math.round(8 * scale));
        int bottom = getBankCancelButtonBounds().top - Math.max(6, Math.round(7 * scale));
        return new UiRect(contentBounds.left + sidePadding, top, contentBounds.width - (sidePadding * 2),
                Math.max(76, bottom - top));
    }

    UiRect getBankReceiveQrBounds() {
        UiRect cardBounds = getBankReceiveCardBounds();
        int maxByWidth = cardBounds.width - Math.max(22, Math.round(26 * scale));
        int maxByHeight = Math.round(cardBounds.height * 0.46F);
        int size = Math.min(Math.max(40, maxByWidth), Math.max(42, Math.min(maxByHeight, Math.round(52 * scale))));
        return new UiRect(cardBounds.left + (cardBounds.width - size) / 2,
                cardBounds.top + Math.max(20, Math.round(24 * scale)),
                size,
                size);
    }

    UiRect getBankScanReceiveButtonBounds() {
        UiRect cameraBounds = getCameraViewBounds();
        int sidePadding = Math.max(8, Math.round(10 * scale));
        int buttonGap = Math.max(6, Math.round(8 * scale));
        int bottomInset = Math.max(24, Math.round(28 * scale));
        int buttonHeight = Math.max(20, Math.round(24 * scale));
        int buttonY = cameraBounds.bottom() - bottomInset - buttonHeight;
        int eyeButtonSize = buttonHeight;
        int eyeButtonX = cameraBounds.right() - sidePadding - eyeButtonSize;
        int receiveButtonWidth = eyeButtonX - cameraBounds.left - sidePadding - buttonGap;
        return new UiRect(cameraBounds.left + sidePadding, buttonY, receiveButtonWidth, buttonHeight);
    }

    UiRect getBankScanEyeButtonBounds() {
        UiRect cameraBounds = getCameraViewBounds();
        int sidePadding = Math.max(8, Math.round(10 * scale));
        int bottomInset = Math.max(24, Math.round(28 * scale));
        int buttonHeight = Math.max(20, Math.round(24 * scale));
        int buttonY = cameraBounds.bottom() - bottomInset - buttonHeight;
        int eyeButtonSize = buttonHeight;
        int eyeButtonX = cameraBounds.right() - sidePadding - eyeButtonSize;
        return new UiRect(eyeButtonX, buttonY, eyeButtonSize, eyeButtonSize);
    }

    UiRect getBankReceiveButtonBounds() {
        // For QR Receive page
        UiRect cameraBounds = getCameraViewBounds();
        int width = Math.max(68, Math.round(82 * scale));
        int height = Math.max(18, Math.round(21 * scale));
        int bottomInset = Math.max(24, Math.round(28 * scale));
        return new UiRect(cameraBounds.left + (cameraBounds.width - width) / 2,
                cameraBounds.bottom() - bottomInset - height,
                width,
                height);
    }

    UiRect getBankSlipCardBounds() {
        UiRect contentBounds = getBankSurfaceBounds();
        int sidePadding = Math.max(5, Math.round(6 * scale));
        int top = getBankHeaderBounds().bottom() + Math.max(7, Math.round(8 * scale));
        int bottom = getBankCancelButtonBounds().top - Math.max(7, Math.round(8 * scale));
        return new UiRect(contentBounds.left + sidePadding, top,
                contentBounds.width - (sidePadding * 2), Math.max(104, bottom - top));
    }

    List<PhoneContact> getPhoneContacts() {
        if (homePhoneMode) {
            HomePhoneBlockEntity homePhone = getClientHomePhone();
            return homePhone == null ? List.of() : homePhone.getContacts();
        }
        return PhoneData.getContacts(getOpenPhoneStack());
    }

    List<PhoneContact> getChatFriends() {
        if (homePhoneMode) {
            return List.of();
        }
        return PhoneClientChatState.getFriends();
    }

    List<PhoneChatMessage> getActiveChatMessages() {
        if (activeChatNumber.isEmpty()) {
            return List.of();
        }
        return PhoneClientChatState.getMessages(activeChatNumber);
    }

    String getActiveChatDisplayName() {
        if (activeChatNumber.isEmpty()) {
            return "";
        }

        String savedName = PhoneClientChatState.getFriendName(activeChatNumber);
        if (!savedName.isBlank()) {
            return savedName;
        }

        return activeChatName.isBlank() ? activeChatNumber : activeChatName;
    }

    String getActiveChatPreview() {
        if (activeChatNumber.isEmpty()) {
            return "";
        }
        return PhoneClientChatState.getLastMessagePreview(activeChatNumber);
    }

    String getChatPreview(String number) {
        return homePhoneMode ? "" : PhoneClientChatState.getLastMessagePreview(number);
    }

    PhoneChatMessage getChatPreviewMessage(String number) {
        return homePhoneMode ? null : PhoneClientChatState.getLastMessage(number);
    }

    ResourceLocation getChatProfileTexture(String number) {
        if (homePhoneMode) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.player != null) {
                return DefaultPlayerSkin.getDefaultSkin(minecraft.player.getUUID());
            }
            return DefaultPlayerSkin.getDefaultSkin();
        }

        Minecraft minecraft = Minecraft.getInstance();
        UUID profileId = PhoneClientChatState.getFriendProfileId(number);
        if (minecraft != null && minecraft.getConnection() != null) {
            if (profileId != null) {
                PlayerInfo info = minecraft.getConnection().getPlayerInfo(profileId);
                if (info != null) {
                    return info.getSkinLocation();
                }
            }

            String friendName = PhoneClientChatState.getFriendName(number);
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

    UiRect getCameraViewBounds() {
        return getLayoutState().cameraViewBounds();
    }

    private void rotateCameraPlayer(double dragX, double dragY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        double sensitivity = minecraft.options.sensitivity().get();
        double multiplier = sensitivity * 1.5;

        float yaw = minecraft.player.getYRot() + (float) (dragX * multiplier);
        float pitch = Mth.clamp(minecraft.player.getXRot() + (float) (dragY * multiplier), -90.0F, 90.0F);

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
                addContactMode = false;
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
                addContactMode = false;
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

        long windowHandle = minecraft.getWindow().getWindow();
        if (enabled) {
            if (GLFW.glfwRawMouseMotionSupported() && !rawMouseMotionChanged) {
                savedRawMouseMotion = GLFW.glfwGetInputMode(windowHandle, GLFW.GLFW_RAW_MOUSE_MOTION) == GLFW.GLFW_TRUE;
                GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_TRUE);
                rawMouseMotionChanged = true;
            }
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        } else {
            minecraft.options.keyUp.setDown(false);
            minecraft.options.keyLeft.setDown(false);
            minecraft.options.keyDown.setDown(false);
            minecraft.options.keyRight.setDown(false);
            minecraft.options.keyJump.setDown(false);
            minecraft.options.keySprint.setDown(false);
            minecraft.options.keyShift.setDown(false);
            if (rawMouseMotionChanged && GLFW.glfwRawMouseMotionSupported()) {
                GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_RAW_MOUSE_MOTION,
                        savedRawMouseMotion ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
                rawMouseMotionChanged = false;
            }
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    private void setCameraMoveModeRotated(boolean enabled, double mouseX, double mouseY) {
        if (landscapeMode && enabled) {
            int frameWidthPortrait = Math.round(FRAME_WIDTH * scale);
            double screenX = frameX + mouseY - frameY;
            double screenY = frameY + frameWidthPortrait - mouseX + frameX;
            setCameraMoveMode(enabled, screenX, screenY);
        } else {
            setCameraMoveMode(enabled, mouseX, mouseY);
        }
    }

    private void syncCameraMovementKeys() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        long windowHandle = minecraft.getWindow().getWindow();
        syncMovementKey(windowHandle, minecraft.options.keyUp);
        syncMovementKey(windowHandle, minecraft.options.keyLeft);
        syncMovementKey(windowHandle, minecraft.options.keyDown);
        syncMovementKey(windowHandle, minecraft.options.keyRight);
        syncMovementKey(windowHandle, minecraft.options.keyJump);
        syncMovementKey(windowHandle, minecraft.options.keySprint);
        syncMovementKey(windowHandle, minecraft.options.keyShift);
    }

    private void syncMovementKey(long windowHandle, KeyMapping keyMapping) {
        if (keyMapping == null) {
            return;
        }

        InputConstants.Key inputKey = InputConstants.getKey(keyMapping.saveString());
        if (inputKey.getType() != InputConstants.Type.KEYSYM) {
            return;
        }

        keyMapping.setDown(InputConstants.isKeyDown(windowHandle, inputKey.getValue()));
    }

    PhotoEntry getLatestPhoto() {
        List<PhotoEntry> photos = getPhotos();
        if (photos.isEmpty()) {
            return null;
        }

        return photos.get(0);
    }

    private void openCallApp() {
        setBankReceiveActive(false);
        closeCamera();
        photoViewerMode = false;
        galleryMode = false;
        unlocked = true;
        callAppMode = true;
        callContactsMode = false;
        callSessionMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        bankAppMode = false;
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
        addContactMode = false;
        unlocked = true;
        rebuildWidgets();
    }

    private void openChatApp() {
        setBankReceiveActive(false);
        closeCamera();
        photoViewerMode = false;
        galleryMode = false;
        unlocked = true;
        callAppMode = false;
        callContactsMode = false;
        
        callSessionMode = false;
        addContactMode = false;
        chatAppMode = true;
        chatThreadMode = false;
        bankAppMode = false;
        clearChatDeleteMenu();
        if (!activeChatNumber.isEmpty() && activeChatName.isBlank()) {
            activeChatName = PhoneClientChatState.getFriendName(activeChatNumber);
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

    private void openBankApp() {
        closeCamera();
        photoViewerMode = false;
        galleryMode = false;
        unlocked = true;
        callAppMode = false;
        callContactsMode = false;
        
        callSessionMode = false;
        addContactMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        bankAppMode = true;
        bankPage = BANK_PAGE_HOME;
        bankTransferNumber = "";
        bankTransferAmount = "";
        bankPaymentName = "";
        clearBankSlip();
        resetBankScanHover();
        bankStatus = Component.translatable("screen.minedevice.phone.bank.status.syncing");
        bankSyncCooldown = 0;
        PhoneNetworkingClient.requestBankSync();
        rebuildWidgets();
    }

    private void closeBankApp() {
        setBankReceiveActive(false);
        closeCamera();
        bankAppMode = false;
        bankTransferNumber = "";
        bankTransferAmount = "";
        bankPaymentName = "";
        clearBankSlip();
        resetBankScanHover();
        bankStatus = Component.empty();
        bankPage = BANK_PAGE_HOME;
        bankSyncCooldown = 0;
        unlocked = true;
        rebuildWidgets();
    }

    private void openBankHomePage() {
        setBankReceiveActive(false);
        closeCamera();
        bankPage = BANK_PAGE_HOME;
        bankAppMode = true;
        bankTransferNumber = "";
        bankTransferAmount = "";
        bankPaymentName = "";
        clearBankSlip();
        resetBankScanHover();
        bankStatus = Component.empty();
        bankSyncCooldown = 0;
        PhoneNetworkingClient.requestBankSync();
        rebuildWidgets();
    }

    private void openBankTransferPage() {
        setBankReceiveActive(false);
        closeCamera();
        bankPage = BANK_PAGE_TRANSFER;
        bankAppMode = true;
        bankTransferNumber = "";
        bankTransferAmount = "";
        bankPaymentName = "";
        clearBankSlip();
        resetBankScanHover();
        bankStatus = Component.translatable("screen.minedevice.phone.bank.transfer_hint");
        rebuildWidgets();
    }

    private void openBankScanCamera() {
        setBankReceiveActive(false);
        Minecraft minecraft = Minecraft.getInstance();
        cameraMode = true;
        bankAppMode = true;
        bankPage = BANK_PAGE_SCAN;
        galleryMode = false;
        photoViewerMode = false;
        callAppMode = false;
        callContactsMode = false;
        callSessionMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        bankTransferNumber = "";
        bankTransferAmount = "";
        bankPaymentName = "";
        clearBankSlip();
        resetBankScanHover();
        bankStatus = Component.translatable("screen.minedevice.phone.bank.status.scan_ready");
        setCameraMoveMode(false, 0.0D, 0.0D);
        if (minecraft != null) {
            if (savedCameraType == null) {
                savedCameraType = minecraft.options.getCameraType();
            }
            if (savedCameraFov < 0) {
                savedCameraFov = getCurrentCameraFov(minecraft);
            }
            selfieCameraMode = false;
            cameraZoomLevel = 0;
            cameraZoomFactor = 1.0F;
            cameraZoomIndicatorTicks = 0;
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            applyCameraZoom(minecraft);
        }
        rebuildWidgets();
    }

    private void closeBankScanCamera() {
        closeCamera();
        cameraMode = false;
        bankAppMode = true;
        bankPage = BANK_PAGE_HOME;
        bankStatus = Component.empty();
        resetBankScanHover();
        rebuildWidgets();
    }

    private void openBankReceivePage() {
        closeCamera();
        cameraMode = false;
        bankAppMode = true;
        bankPage = BANK_PAGE_RECEIVE;
        bankTransferNumber = "";
        bankTransferAmount = "";
        bankPaymentName = "";
        clearBankSlip();
        resetBankScanHover();
        bankStatus = Component.translatable("screen.minedevice.phone.bank.status.receive_ready");
        setBankReceiveActive(true);
        rebuildWidgets();
    }

    private void showBankReceiveQrToWorld() {
        setBankReceiveActive(true);
        closingForBankReceiveWorld = true;
        PhoneClientHooks.showBankReceiveQrToWorld(openHand);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private void openBankPaymentPage(String targetNumber, String targetName) {
        setBankReceiveActive(false);
        closeCamera();
        bankAppMode = true;
        bankPage = BANK_PAGE_PAYMENT;
        bankTransferNumber = PhoneData.normalizePhoneNumber(targetNumber);
        bankTransferAmount = "";
        bankPaymentName = targetName == null ? "" : targetName;
        clearBankSlip();
        resetBankScanHover();
        bankStatus = Component.translatable("screen.minedevice.phone.bank.status.enter_amount");
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
        setBankReceiveActive(false);
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
        addContactMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        bankAppMode = false;
        observedCallStateRevision = Long.MIN_VALUE;
        requestCallSync();
        applyCallStateFromServer();
        rebuildWidgets();
    }

    private void openAddContactPage() {
        setBankReceiveActive(false);
        addContactMode = true;
        callAppMode = false;
        
        callSessionMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        bankAppMode = false;
        addContactName = "";
        addContactNumber = "";
        addContactNameFieldFocused = true;
        editingContactOriginalNumber = null;
        rebuildWidgets();
    }

    private void openEditContactPage(PhoneContact contact) {
        setBankReceiveActive(false);
        addContactMode = true;
        callAppMode = false;
        
        callSessionMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        bankAppMode = false;
        addContactName = contact.displayName();
        addContactNumber = contact.number();
        addContactNameFieldFocused = true;
        editingContactOriginalNumber = contact.number();
        rebuildWidgets();
    }

    private void openCallDialPage() {
        setBankReceiveActive(false);
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
        addContactMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        bankAppMode = false;
        observedCallStateRevision = Long.MIN_VALUE;
        requestCallSync();
        applyCallStateFromServer();
        rebuildWidgets();
    }

    private void startCallSession(String number) {
        setBankReceiveActive(false);
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
        addContactMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        bankAppMode = false;
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
        callMuted = false;
        callSpeakerEnabled = false;
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
                && !PhoneClientChatState.hasFriend(normalized);
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

    private void appendBankDigit(String digit) {
        if (digit == null || digit.isEmpty()) {
            return;
        }

        if (bankPage == BANK_PAGE_PAYMENT) {
            if (bankTransferAmount.length() < 9) {
                bankTransferAmount = bankTransferAmount + digit;
            }
            return;
        }

        if (bankPage == BANK_PAGE_TRANSFER && bankTransferNumber.length() < PhoneData.PHONE_NUMBER_LENGTH) {
            bankTransferNumber = bankTransferNumber + digit;
            return;
        }

        if (bankPage == BANK_PAGE_TRANSFER && bankTransferAmount.length() < 9) {
            bankTransferAmount = bankTransferAmount + digit;
        }
    }

    private void removeLastBankDigit() {
        if (!bankTransferAmount.isEmpty()) {
            bankTransferAmount = bankTransferAmount.substring(0, bankTransferAmount.length() - 1);
            return;
        }

        if (bankPage == BANK_PAGE_TRANSFER && !bankTransferNumber.isEmpty()) {
            bankTransferNumber = bankTransferNumber.substring(0, bankTransferNumber.length() - 1);
        }
    }

    boolean canSendBankTransfer() {
        return (bankPage == BANK_PAGE_TRANSFER || bankPage == BANK_PAGE_PAYMENT)
                && PhoneData.isValidPhoneNumber(bankTransferNumber)
                && getBankTransferAmount() > 0L;
    }

    long getBankTransferAmount() {
        if (bankTransferAmount.isBlank()) {
            return 0L;
        }

        try {
            return Math.max(0L, Long.parseLong(bankTransferAmount));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void sendBankTransfer() {
        if (!canSendBankTransfer()) {
            return;
        }

        long amount = getBankTransferAmount();
        PhoneNetworkingClient.requestBankTransfer(bankTransferNumber, getBankTransferAmount());
        bankTransferAmount = "";
        bankStatus = Component.translatable("screen.minedevice.phone.bank.status.processing", amount);
    }

    void handleBankState(long balance, Component status) {
        bankBalance = Math.max(0L, balance);
        if (status != null && !status.getString().isBlank()) {
            bankStatus = status;
        }
    }

    void handleBankScanResult(String targetNumber, String targetName) {
        openBankPaymentPage(targetNumber, targetName);
    }

    void handleBankTransferReceipt(String targetNumber, String targetName, long amount, long balance) {
        bankSlipTargetNumber = PhoneData.normalizePhoneNumber(targetNumber);
        bankSlipTargetName = targetName == null ? "" : targetName;
        bankSlipAmount = Math.max(0L, amount);
        bankSlipBalance = Math.max(0L, balance);
        bankSlipTime = java.time.LocalTime.now().withNano(0).toString();
        closeCamera();
        bankPage = BANK_PAGE_SLIP;
        bankAppMode = true;
        cameraMode = false;
        bankTransferNumber = "";
        bankTransferAmount = "";
        bankPaymentName = "";
        bankStatus = Component.translatable("screen.minedevice.phone.bank.status.transfer_done");
        rebuildWidgets();
    }

    private boolean acceptsBankNumericInput() {
        return bankPage == BANK_PAGE_TRANSFER || bankPage == BANK_PAGE_PAYMENT;
    }

    private boolean isBankScanCameraMode() {
        return bankAppMode && cameraMode && bankPage == BANK_PAGE_SCAN;
    }

    boolean isBankScanCameraActive() {
        return isBankScanCameraMode();
    }

    private void tickBankScanHover() {
        Player targetPlayer = getBankScanTargetPlayer();
        if (targetPlayer == null) {
            resetBankScanHover();
            return;
        }

        UUID targetId = targetPlayer.getUUID();
        if (!targetId.equals(bankScanHoverTargetId)) {
            bankScanHoverTargetId = targetId;
            bankScanHoverTicks = 1;
            return;
        }

        if (bankScanHoverTicks < BANK_SCAN_HOLD_TICKS) {
            bankScanHoverTicks++;
            return;
        }

        if (bankScanRequestCooldown <= 0) {
            requestBankScanTarget(targetPlayer);
        }
    }

    private void resetBankScanHover() {
        bankScanHoverTargetId = null;
        bankScanHoverTicks = 0;
    }

    private void tryScanBankPaymentTarget() {
        Player targetPlayer = getBankScanTargetPlayer();
        if (targetPlayer == null) {
            bankStatus = Component.translatable("screen.minedevice.phone.bank.status.scan_failed");
            resetBankScanHover();
            return;
        }

        requestBankScanTarget(targetPlayer);
    }

    private Player getBankScanTargetPlayer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || !(minecraft.hitResult instanceof EntityHitResult entityHit)
                || !(entityHit.getEntity() instanceof Player targetPlayer)) {
            return null;
        }

        if (targetPlayer == minecraft.player || !isHoldingPhone(targetPlayer)) {
            return null;
        }

        return targetPlayer;
    }

    private void requestBankScanTarget(Player targetPlayer) {
        if (targetPlayer == null) {
            return;
        }

        bankStatus = Component.translatable("screen.minedevice.phone.bank.status.scanning");
        PhoneNetworkingClient.requestBankScanTarget(targetPlayer.getUUID());
        bankScanRequestCooldown = Math.max(20, BANK_SCAN_HOLD_TICKS);
    }

    private boolean isHoldingPhone(Player player) {
        return player != null
                && (player.getMainHandItem().is(ModItems.PHONE.get())
                || player.getOffhandItem().is(ModItems.PHONE.get()));
    }

    private void clearBankSlip() {
        bankSlipTargetNumber = "";
        bankSlipTargetName = "";
        bankSlipTime = "";
        bankSlipAmount = 0L;
        bankSlipBalance = 0L;
    }

    private void setBankReceiveActive(boolean active) {
        if (bankReceiveActive == active) {
            return;
        }

        bankReceiveActive = active;
        if (!homePhoneMode) {
            PhoneNetworkingClient.requestBankReceiveState(active);
        }
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
        setBankReceiveActive(false);
        galleryMode = true;
        closeCamera();
        callAppMode = false;
        callContactsMode = false;
        
        callSessionMode = false;
        addContactMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        bankAppMode = false;
        photoViewerMode = false;
        galleryPage = 0;
        rebuildWidgets();
    }

    private void openCamera() {
        setBankReceiveActive(false);
        Minecraft minecraft = Minecraft.getInstance();
        cameraMode = true;
        galleryMode = false;
        photoViewerMode = false;
        callAppMode = false;
        callContactsMode = false;
        
        callSessionMode = false;
        addContactMode = false;
        chatAppMode = false;
        chatThreadMode = false;
        bankAppMode = false;
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
        setBankReceiveActive(false);
        List<PhotoEntry> photos = getPhotos();
        if (photos.isEmpty()) {
            return;
        }

        photoViewerMode = true;
        fullScreenPhotoMode = false;
        photoZoom = 1.0F;
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

        ItemStack captureStack = sanitizePhotoCaptureStack(captureTargetItem());
        if (!appendPhotoToPhone(photoFileName, captureStack)) {
            deletePhotoFile(photoFileName);
            return;
        }

        PhoneNetworkingClient.requestAppendPhotoMetadata(openHand, photoFileName, captureStack);
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
            if (croppedImage == null) {
                return null;
            }
            return photoStore.writePhoto(croppedImage);
        } catch (Exception exception) {
            Minedevice.LOGGER.warn("Failed to capture phone photo", exception);
            return null;
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
        if (source == null) {
            return null;
        }

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return null;
        }

        UiRect cameraBounds = getCameraViewBounds();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return cropImage(source, 0, 0, sourceWidth, sourceHeight);
        }

        int guiWidth = Math.max(1, minecraft.getWindow().getGuiScaledWidth());
        int guiHeight = Math.max(1, minecraft.getWindow().getGuiScaledHeight());
        float scaleX = (float) sourceWidth / guiWidth;
        float scaleY = (float) sourceHeight / guiHeight;

        int cropX, cropY, cropWidth, cropHeight;
        if (landscapeMode) {
            int frameWidthPortrait = Math.round(FRAME_WIDTH * scale);
            int sxMin = displayY - frameY + frameX;
            int syMin = frameX + frameY + frameWidthPortrait - displayX - displayWidth;
            int rawCropWidth = Math.round((getCameraShutterButtonBounds().top - displayY) * scaleX);
            int rawCropHeight = Math.round(rawCropWidth * (9.0F / 21.0F));
            int fullCropHeight = Math.round(displayWidth * scaleY);
            int yOffset = (fullCropHeight - rawCropHeight) / 2;

            cropX = Mth.clamp(Math.round(sxMin * scaleX), 0, Math.max(0, sourceWidth - 1));
            cropY = Mth.clamp(Math.round(syMin * scaleY) + yOffset, 0, Math.max(0, sourceHeight - 1));
            cropWidth = Mth.clamp(rawCropWidth, 1, sourceWidth - cropX);
            cropHeight = Mth.clamp(rawCropHeight, 1, sourceHeight - cropY);
        } else {
            cropX = Mth.clamp(Math.round(cameraBounds.left * scaleX), 0, Math.max(0, sourceWidth - 1));
            cropY = Mth.clamp(Math.round(cameraBounds.top * scaleY), 0, Math.max(0, sourceHeight - 1));
            cropWidth = Mth.clamp(Math.round(cameraBounds.width * scaleX), 1, sourceWidth - cropX);
            cropHeight = Mth.clamp(Math.round((getCameraShutterButtonBounds().top - displayY) * scaleY), 1, sourceHeight - cropY);
        }

        return cropImage(source, cropX, cropY, cropWidth, cropHeight);
    }

    private NativeImage cropImage(NativeImage source, int x, int y, int width, int height) {
        if (source == null) {
            return null;
        }

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0 || width <= 0 || height <= 0) {
            return null;
        }

        int safeX = Mth.clamp(x, 0, Math.max(0, sourceWidth - 1));
        int safeY = Mth.clamp(y, 0, Math.max(0, sourceHeight - 1));
        int safeWidth = Math.min(width, sourceWidth - safeX);
        int safeHeight = Math.min(height, sourceHeight - safeY);
        if (safeWidth <= 0 || safeHeight <= 0) {
            return null;
        }

        NativeImage croppedImage = new NativeImage(safeWidth, safeHeight, false);
        for (int row = 0; row < safeHeight; row++) {
            for (int column = 0; column < safeWidth; column++) {
                croppedImage.setPixelRGBA(column, row, source.getPixelRGBA(safeX + column, safeY + row));
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

    private ItemStack sanitizePhotoCaptureStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int count = Mth.clamp(stack.getCount(), 1, Math.max(1, stack.getMaxStackSize()));
        ItemStack snapshot = new ItemStack(stack.getItem(), count);
        if (stack.hasCustomHoverName()) {
            snapshot.setHoverName(stack.getHoverName());
        }
        return snapshot;
    }

    private boolean appendPhotoToPhone(String photoFileName, ItemStack captureStack) {
        return photoStore.appendPhotoToStack(getOpenPhoneStack(), photoFileName, captureStack);
    }

    private boolean removePhotoFromPhone(int viewerIndex, String expectedFileName) {
        boolean removed = photoStore.removePhotoFromStack(getOpenPhoneStack(), viewerIndex, expectedFileName);
        if (removed && expectedFileName != null && !expectedFileName.isBlank()) {
            PhoneNetworkingClient.requestDeletePhotoMetadata(openHand, expectedFileName);
        }
        return removed;
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

    // ── Settings App ──────────────────────────────────────────────────────────

    void openSettingsApp() {
        settingsAppMode = true;
        settingsWallpaperPage = 0;
        rebuildWidgets();
    }

    void closeSettingsApp() {
        settingsAppMode = false;
        settingsWallpaperPage = 0;
        rebuildWidgets();
    }

    // ── Wallpaper ─────────────────────────────────────────────────────────────

    /**
     * Returns the ResourceLocation for a built-in wallpaper, or null if it's a photo wallpaper.
     * @param lockScreen true = lock screen wallpaper, false = home screen wallpaper
     */
    ResourceLocation resolveWallpaperTexture(boolean lockScreen) {
        ItemStack stack = getOpenPhoneStack();
        String key = lockScreen
                ? PhoneWallpaperData.getLockWallpaper(stack)
                : PhoneWallpaperData.getHomeWallpaper(stack);
        if (PhoneWallpaperData.isBuiltin(key)) {
            return PhoneWallpaperData.resolveBuiltinTexture(key);
        }
        return null;
    }

    /**
     * Returns the photo file name for a photo wallpaper, or null if it's built-in.
     * @param lockScreen true = lock screen wallpaper, false = home screen wallpaper
     */
    String getWallpaperPhotoFileName(boolean lockScreen) {
        ItemStack stack = getOpenPhoneStack();
        String key = lockScreen
                ? PhoneWallpaperData.getLockWallpaper(stack)
                : PhoneWallpaperData.getHomeWallpaper(stack);
        return PhoneWallpaperData.getPhotoFileName(key);
    }

    void setWallpaperFromPhoto(String photoFileName, boolean lockScreen, boolean homeScreen) {
        ItemStack stack = getOpenPhoneStack();
        if (stack.isEmpty()) return;
        String key = PhoneWallpaperData.photoKey(photoFileName);
        if (lockScreen) PhoneWallpaperData.setLockWallpaper(stack, key);
        if (homeScreen) PhoneWallpaperData.setHomeWallpaper(stack, key);
        PhoneData.markDirty(Minecraft.getInstance().player);
    }

    void setWallpaperBuiltin(String builtinName, boolean lockScreen, boolean homeScreen) {
        ItemStack stack = getOpenPhoneStack();
        if (stack.isEmpty()) return;
        String key = PhoneWallpaperData.BUILTIN_PREFIX + builtinName;
        if (lockScreen) PhoneWallpaperData.setLockWallpaper(stack, key);
        if (homeScreen) PhoneWallpaperData.setHomeWallpaper(stack, key);
        PhoneData.markDirty(Minecraft.getInstance().player);
    }

    private void renderSettingsBackdrop(GuiGraphics guiGraphics) {
        int bgX = displayX - Math.round(6 * scale);
        int bgY = displayY - Math.round(6 * scale);
        int bgWidth = displayWidth + Math.round(12 * scale);
        int bgHeight = displayHeight + Math.round(12 * scale);
        guiGraphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0xFFF2F2F7);
        
        // Render settings surface with hover effects
        PhoneSettingsSurfaceRenderer.renderSettingsSurface(this, guiGraphics, lastMouseX, lastMouseY);
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
        renderPhoneTintedTexture(guiGraphics, FRAME_BASE_TINT_TEXTURE, 0, 0,
                FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT, getOpenPhoneColor());
        renderPhoneTintedTexture(guiGraphics, FRAME_EDGE_TINT_TEXTURE, 0, 0,
                FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT, phoneTintEdgeColor());
        renderPhoneTintedTexture(guiGraphics, FRAME_HIGHLIGHT_TINT_TEXTURE, 0, 0,
                FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT, phoneTintHighlightColor());
        guiGraphics.pose().popPose();
    }

    private void renderPhoneTintedTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y,
                                          int width, int height, int textureWidth, int textureHeight, int color) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        guiGraphics.blit(texture, x, y, 0, 0, width, height, textureWidth, textureHeight);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private int phoneTintEdgeColor() {
        int color = getOpenPhoneColor();
        if (color == PhoneItem.DEFAULT_COLOR) {
            return 0x000000;
        }
        return mixWithBlack(color, 0.14F);
    }

    private int phoneTintHighlightColor() {
        return mixWithWhite(getOpenPhoneColor(), 0.30F);
    }

    private static int mixWithWhite(int color, float amount) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int mixedRed = Mth.clamp(Math.round(red + (255 - red) * amount), 0, 255);
        int mixedGreen = Mth.clamp(Math.round(green + (255 - green) * amount), 0, 255);
        int mixedBlue = Mth.clamp(Math.round(blue + (255 - blue) * amount), 0, 255);
        return (mixedRed << 16) | (mixedGreen << 8) | mixedBlue;
    }

    private static int mixWithBlack(int color, float amount) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int mixedRed = Mth.clamp(Math.round(red * (1.0F - amount)), 0, 255);
        int mixedGreen = Mth.clamp(Math.round(green * (1.0F - amount)), 0, 255);
        int mixedBlue = Mth.clamp(Math.round(blue * (1.0F - amount)), 0, 255);
        return (mixedRed << 16) | (mixedGreen << 8) | mixedBlue;
    }

    private int getOpenPhoneColor() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || openHand == null) {
            return PhoneItem.DEFAULT_COLOR;
        }

        ItemStack stack = minecraft.player.getItemInHand(openHand);
        if (stack.getItem() instanceof PhoneItem phoneItem) {
            return phoneItem.getColor(stack);
        }

        return PhoneItem.DEFAULT_COLOR;
    }

    public boolean isCameraModeActive() {
        return cameraMode;
    }

    public boolean isBankReceivePoseActive() {
        return bankReceiveActive;
    }

    public boolean isCameraMoveModeActive() {
        return cameraMode && cameraMoveMode;
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

    private void addRotateButton() {
        int buttonSize = Math.round(24 * scale);
        int buttonX = frameX + Math.round(FRAME_WIDTH * scale) + Math.round(4 * scale);
        int buttonY = frameY + Math.round(FRAME_HEIGHT * scale) - buttonSize - Math.round(4 * scale);
        addRenderableWidget(new ImageButton(
                buttonX, buttonY, buttonSize, buttonSize,
                0, 0, 0, ROTATE_BUTTON_TEXTURE, 24, 24,
                button -> toggleLandscapeMode()) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), this.width, this.height,
                        0.0F, 0.0F, 24, 24, 24, 24);
            }
        });
    }

    private void toggleLandscapeMode() {
        landscapeMode = !landscapeMode;
        rebuildWidgets();
    }

}
