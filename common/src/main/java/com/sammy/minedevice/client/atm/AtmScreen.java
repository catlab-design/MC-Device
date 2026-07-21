package com.sammy.minedevice.client.atm;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sammy.minedevice.Minedevice;
import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.atm.AtmMenu;
import com.sammy.minedevice.atm.TransactionHistoryStore;
import com.sammy.minedevice.client.atm.AtmNetworkingClient.HistoryEntry;
import com.sammy.minedevice.item.CardItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.text.NumberFormat;
import java.util.Locale;

public final class AtmScreen extends AbstractContainerScreen<AtmMenu> {
    // ─── panel dimensions ───
    private static final int PANEL_WIDTH = 256;
    private static final int PANEL_HEIGHT = 166;
    private static final int SCREEN_X = 76;
    private static final int SCREEN_Y = 8;
    private static final int SCREEN_W = 116;
    private static final int SCREEN_H = 150;

    // ─── dark theme colours ───
    private static final int COLOR_BACKDROP = 0x99000000;
    private static final int COLOR_PANEL_EDGE = 0xFF25262B;
    private static final int COLOR_PANEL = 0xFF16171A;
    private static final int COLOR_SCREEN_EDGE = 0xFF1E2638;
    private static final int COLOR_SCREEN = 0xFF0A0A0C;
    private static final int COLOR_ACCENT = 0xFF00D2FF;
    private static final int COLOR_ACCENT_DIM = 0xFF0088CC;
    private static final int COLOR_TEXT = 0xFFE2E8F0;
    private static final int COLOR_TEXT_DIM = 0xFF94A3B8;
    private static final int COLOR_HINT = 0xFF475569;
    private static final int COLOR_BUTTON_EDGE = 0xFF2D3748;
    private static final int COLOR_BUTTON = 0xFF13151A;
    private static final int COLOR_BUTTON_HOVER = 0xFF00D2FF;
    private static final int COLOR_BUTTON_PRESSED = 0xFF0088CC;
    private static final int COLOR_BUTTON_TEXT = 0xFFFFFFFF;
    private static final int COLOR_DANGER = 0xFFEF4444;
    private static final int COLOR_SUCCESS = 0xFF10B981;

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.ROOT);
    private static final ResourceLocation CARD_FRONT_TEXTURE = new ResourceLocation(Minedevice.MOD_ID,
            "textures/gui/atm/card_front.png");

    private static final int CARD_INSERT_TICKS = 15;
    private static final int CARD_EJECT_TICKS = 20;
    private static final int CASH_DISPENSE_TICKS = 12;

    private static float easeOutQuad(float t) {
        return t * (2.0F - t);
    }
    private static float easeInQuad(float t) {
        return t * t;
    }
    private static float easeOutCubic(float t) {
        float r = 1.0F - t;
        return 1.0F - r * r * r;
    }

    public enum Page {
        WELCOME, SET_PIN, ENTER_PIN, MAIN, WITHDRAW, DISPENSE, DEPOSIT, TRANSFER, BALANCE, HISTORY, LOCKED, EJECT
    }

    private Page currentPage = Page.WELCOME;
    private int lastCardState = AtmMenu.STATE_NO_CARD;
    private String statusMessage = "";
    private int statusColor = COLOR_TEXT_DIM;
    private final StringBuilder pinBuffer = new StringBuilder();
    private EditBox recipientInput;
    private EditBox amountInput;
    private java.util.List<HistoryEntry> historyEntries = java.util.Collections.emptyList();
    private int historyTotal = 0;
    private int historyOffset = 0;
    private static final int HISTORY_PAGE_SIZE = 3;

    // animations
    private int cardAnimTicks = 0;
    private boolean cardAnimPlaying = false;
    private boolean cardIsInserting = true; // true = inserting, false = ejecting
    private int cashAnimTicks = 0;
    private boolean cashAnimPlaying = false;

    private boolean hasInsertedCard = false;

    private float screenScale = 1.0F;
    private int actualLeft = 0;
    private int actualTop = 0;

    public AtmScreen(AtmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    private void updateActualPosition() {
        this.actualLeft = (this.width - PANEL_WIDTH) / 2;
        this.actualTop = (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    protected void init() {
        this.screenScale = 1.0F;
        updateActualPosition();

        super.init();

        // Under the translated PoseStack, the panel starts at (0, 0)
        this.leftPos = 0;
        this.topPos = 0;

        rebuildPageWidgets();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        int state = menu.getCardState();
        if (state != lastCardState) {
            int oldState = lastCardState;
            lastCardState = state;

            if (oldState == AtmMenu.STATE_NO_CARD && state != AtmMenu.STATE_NO_CARD) {
                if (!menu.isCardlessMode()) {
                    hasInsertedCard = true;
                    startCardInsert();
                }
            }

            onCardStateChanged(state);
        }

        if (cardAnimPlaying) {
            cardAnimTicks--;
            if (cardAnimTicks <= 0) {
                cardAnimPlaying = false;
                if (!cardIsInserting) {
                    AtmNetworkingClient.sendEject(menu.containerId);
                }
            }
        }

        if (currentPage == Page.WITHDRAW && menu.hasDispenserItems()) {
            setCurrentPage(Page.DISPENSE);
            startCashDispense();
        }

        if (cashAnimPlaying) {
            cashAnimTicks--;
            if (cashAnimTicks <= 0) {
                cashAnimPlaying = false;
                menu.setDispenserActive(true);
            }
        }

        if (currentPage == Page.DISPENSE && !menu.hasDispenserItems() && !cashAnimPlaying) {
            setCurrentPage(Page.MAIN);
        }
    }

    private void startCardInsert() {
        cardAnimTicks = CARD_INSERT_TICKS;
        cardAnimPlaying = true;
        cardIsInserting = true;
    }

    private void startCardEject() {
        cardAnimTicks = CARD_EJECT_TICKS;
        cardAnimPlaying = true;
        cardIsInserting = false;
    }

    private void startCashDispense() {
        cashAnimTicks = CASH_DISPENSE_TICKS;
        cashAnimPlaying = true;
        menu.setDispenserActive(false);
    }

    private void onCardStateChanged(int state) {
        switch (state) {
            case AtmMenu.STATE_NO_CARD -> {
                hasInsertedCard = false;
                setCurrentPage(Page.WELCOME);
            }
            case AtmMenu.STATE_NEEDS_PIN -> setCurrentPage(Page.SET_PIN);
            case AtmMenu.STATE_AWAIT_PIN -> setCurrentPage(Page.ENTER_PIN);
            case AtmMenu.STATE_PIN_OK -> setCurrentPage(Page.MAIN);
            case AtmMenu.STATE_LOCKED -> setCurrentPage(Page.LOCKED);
        }
    }

    private void setCurrentPage(Page page) {
        if (currentPage == page) {
            return;
        }
        currentPage = page;
        statusMessage = "";
        pinBuffer.setLength(0);
        boolean showInventory = page != Page.WELCOME && page != Page.SET_PIN && page != Page.ENTER_PIN
                && page != Page.LOCKED && page != Page.EJECT;
        menu.setInventoryActive(showInventory);
        if (page == Page.HISTORY) {
            historyOffset = 0;
            historyEntries = java.util.Collections.emptyList();
            AtmNetworkingClient.sendHistoryRequest(menu.containerId, 0);
        }
        rebuildPageWidgets();
    }

    private void rebuildPageWidgets() {
        clearWidgets();
        int screenLeft = leftPos + SCREEN_X;
        int screenTop = topPos + SCREEN_Y;
        int btnW = 104;
        int btnH = 15;

        switch (currentPage) {
            case WELCOME, SET_PIN, ENTER_PIN, DISPENSE, EJECT -> {
            }
            case MAIN -> addMainMenuWidgets(screenLeft, screenTop, btnW, btnH);
            case WITHDRAW -> addWithdrawWidgets(screenLeft, screenTop, btnW, btnH);
            case DEPOSIT -> addDepositWidgets(screenLeft, screenTop, btnW, btnH);
            case TRANSFER -> addTransferWidgets(screenLeft, screenTop, btnW, btnH);
            case BALANCE -> addBackOnlyWidgets(screenLeft, screenTop, btnW, btnH);
            case HISTORY -> addHistoryWidgets(screenLeft, screenTop, btnW, btnH);
            case LOCKED -> addEjectOnlyWidgets(screenLeft, screenTop, btnW, btnH);
        }
        addCasingKeypad(leftPos, topPos);
    }

    private void addCasingKeypad(int left, int top) {
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK"};
        for (int i = 0; i < keys.length; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = left + 10 + col * 22;
            int y = top + 25 + row * 22;
            String key = keys[i];
            addRenderableWidget(new CasingKeypadButton(x, y, 18, 18, key, () -> onKeyPress(key)));
        }
    }

    private void onBezelPress(int index) {
        switch (currentPage) {
            case MAIN -> {
                if (index == 0) setCurrentPage(Page.WITHDRAW);
                else if (index == 1) setCurrentPage(Page.TRANSFER);
                else if (index == 2) setCurrentPage(Page.HISTORY);
            }
            case WITHDRAW -> {
                if (index == 0) AtmNetworkingClient.sendMenuWithdraw(menu.containerId, 100);
                else if (index == 1) AtmNetworkingClient.sendMenuWithdraw(menu.containerId, 1000);
                else if (index == 2) AtmNetworkingClient.sendMenuWithdraw(menu.containerId, 500);
            }
            case DEPOSIT -> {
                if (index == 0) AtmNetworkingClient.sendMenuDepositAll(menu.containerId);
                else if (index == 1) AtmNetworkingClient.sendMenuDeposit(menu.containerId, 500);
                else if (index == 2) AtmNetworkingClient.sendMenuDeposit(menu.containerId, 20);
            }
            case DISPENSE -> {
                if (index == 0) AtmNetworkingClient.sendTakeAll(menu.containerId);
            }
            case HISTORY -> {
                if (index == 0) {
                    int newOffset = Math.max(0, historyOffset - HISTORY_PAGE_SIZE);
                    if (newOffset != historyOffset) {
                        historyOffset = newOffset;
                        AtmNetworkingClient.sendHistoryRequest(menu.containerId, historyOffset);
                    }
                } else if (index == 1) {
                    int maxOffset = Math.max(0, historyTotal - HISTORY_PAGE_SIZE);
                    int newOffset = Math.min(maxOffset, historyOffset + HISTORY_PAGE_SIZE);
                    if (newOffset != historyOffset) {
                        historyOffset = newOffset;
                        AtmNetworkingClient.sendHistoryRequest(menu.containerId, historyOffset);
                    }
                }
            }
        }
    }

    private void onKeypadPress(String key) {
        if (currentPage == Page.SET_PIN || currentPage == Page.ENTER_PIN) {
            onKeyPress(key);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentPage == Page.SET_PIN || currentPage == Page.ENTER_PIN) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                onKeyPress("OK");
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (pinBuffer.length() > 0) {
                    pinBuffer.setLength(pinBuffer.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.onClose();
                return true;
            }
            String digit = charForDigit(keyCode);
            if (digit != null) {
                onKeyPress(digit);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    private static String charForDigit(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return String.valueOf(keyCode - GLFW.GLFW_KEY_0);
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return String.valueOf(keyCode - GLFW.GLFW_KEY_KP_0);
        }
        return null;
    }

    private void onKeyPress(String key) {
        if (currentPage != Page.SET_PIN && currentPage != Page.ENTER_PIN) {
            return;
        }
        if (key.equals("C")) {
            pinBuffer.setLength(0);
            return;
        }
        if (key.equals("OK")) {
            if (pinBuffer.length() == 4) {
                String pin = pinBuffer.toString();
                if (currentPage == Page.SET_PIN) {
                    AtmNetworkingClient.sendSetPin(menu.containerId, pin);
                } else {
                    AtmNetworkingClient.sendVerifyPin(menu.containerId, pin);
                }
                pinBuffer.setLength(0);
            }
            return;
        }
        if (pinBuffer.length() < 4) {
            pinBuffer.append(key);
        }
    }

    // ──────────────── WIDGET HELPERS ────────────────

    // ──────────────── WIDGET HELP─ RERS ────────────────

    private void addMainMenuWidgets(int screenLeft, int screenTop, int btnW, int btnH) {
        int x = screenLeft + 6;
        int startY = screenTop + 30;
        int gap = 19; // btnH + 4

        addRenderableWidget(makeNavButton(x, startY, btnW, btnH, "screen.minedevice.atm.withdraw_menu_btn", Page.WITHDRAW));
        addRenderableWidget(makeNavButton(x, startY + gap, btnW, btnH, "screen.minedevice.atm.deposit_menu_btn", Page.DEPOSIT));
        addRenderableWidget(makeNavButton(x, startY + gap * 2, btnW, btnH, "screen.minedevice.atm.transfer", Page.TRANSFER));
        addRenderableWidget(makeNavButton(x, startY + gap * 3, btnW, btnH, "screen.minedevice.atm.balance", Page.BALANCE));
        addRenderableWidget(makeNavButton(x, startY + gap * 4, btnW, btnH, "screen.minedevice.atm.history", Page.HISTORY));
        addRenderableWidget(makeEjectButton(x, startY + gap * 5, btnW, btnH));
    }

    private void addWithdrawWidgets(int screenLeft, int screenTop, int btnW, int btnH) {
        int x = screenLeft + 6;
        int startY = screenTop + 30;
        int gap = 19;

        addRenderableWidget(makeWithdrawButton(x, startY, btnW, btnH, 20));
        addRenderableWidget(makeWithdrawButton(x, startY + gap, btnW, btnH, 100));
        addRenderableWidget(makeWithdrawButton(x, startY + gap * 2, btnW, btnH, 500));
        addRenderableWidget(makeWithdrawButton(x, startY + gap * 3, btnW, btnH, 1000));
        addRenderableWidget(makeBackButton(x, startY + gap * 4, btnW, btnH));
    }

    private void addDepositWidgets(int screenLeft, int screenTop, int btnW, int btnH) {
        int x = screenLeft + 6;
        int startY = screenTop + 30;
        int gap = 19;

        addRenderableWidget(makeDepositButton(x, startY, btnW, btnH, "screen.minedevice.atm.deposit_all", -1));
        addRenderableWidget(makeDepositButton(x, startY + gap, btnW, btnH, "screen.minedevice.atm.deposit", 20));
        addRenderableWidget(makeDepositButton(x, startY + gap * 2, btnW, btnH, "screen.minedevice.atm.deposit", 100));
        addRenderableWidget(makeDepositButton(x, startY + gap * 3, btnW, btnH, "screen.minedevice.atm.deposit", 500));
        addRenderableWidget(makeDepositButton(x, startY + gap * 4, btnW, btnH, "screen.minedevice.atm.deposit", 1000));
        addRenderableWidget(makeBackButton(x, startY + gap * 5, btnW, btnH));
    }

    private void addBackOnlyWidgets(int screenLeft, int screenTop, int btnW, int btnH) {
        int x = screenLeft + 6;
        int y = screenTop + 108;
        addRenderableWidget(makeBackButton(x, y, btnW, btnH));
    }

    private void addEjectOnlyWidgets(int screenLeft, int screenTop, int btnW, int btnH) {
        int x = screenLeft + 6;
        int y = screenTop + 108;
        addRenderableWidget(makeEjectButton(x, y, btnW, btnH));
    }

    private void addDispenseWidgets(int screenLeft, int screenTop, int btnW, int btnH) {
        int x = screenLeft + 6;
        int y = screenTop + 108;
        addRenderableWidget(makeBackButton(x, y, btnW, btnH));
    }

    private void addTransferWidgets(int screenLeft, int screenTop, int btnW, int btnH) {
        int fieldWidth = SCREEN_W - 24;
        int fieldX = screenLeft + 12;

        recipientInput = new EditBox(font, fieldX, screenTop + 30, fieldWidth, 14,
                Component.translatable("screen.minedevice.atm.transfer.recipient"));
        recipientInput.setMaxLength(16);
        recipientInput.setHint(Component.translatable("screen.minedevice.atm.transfer.recipient"));
        addRenderableWidget(recipientInput);

        amountInput = new EditBox(font, fieldX, screenTop + 65, fieldWidth, 14,
                Component.translatable("screen.minedevice.atm.transfer.amount"));
        amountInput.setMaxLength(15);
        amountInput.setFilter(s -> s == null || s.matches("\\d*"));
        amountInput.setHint(Component.translatable("screen.minedevice.atm.transfer.amount"));
        addRenderableWidget(amountInput);

        int x = screenLeft + 6;
        addRenderableWidget(new AtmTextButton(x, screenTop + 100, btnW, btnH,
                Component.translatable("screen.minedevice.atm.transfer_confirm"),
                this::onConfirmTransfer));
        addRenderableWidget(makeBackButton(x, screenTop + 120, btnW, btnH));
    }

    private void onConfirmTransfer() {
        if (recipientInput == null || amountInput == null) {
            return;
        }
        String recipient = recipientInput.getValue().trim();
        String amountStr = amountInput.getValue().trim();
        if (recipient.isEmpty() || amountStr.isEmpty()) {
            return;
        }
        try {
            long amount = Long.parseLong(amountStr);
            if (amount > 0) {
                AtmNetworkingClient.sendMenuTransfer(menu.containerId, recipient, amount);
            }
        } catch (NumberFormatException ignored) {
        }
    }



    private void addHistoryWidgets(int screenLeft, int screenTop, int btnW, int btnH) {
        int xPrev = screenLeft + 6;
        int xNext = screenLeft + 60;
        int xBack = screenLeft + 6;
        
        addRenderableWidget(new AtmTextButton(xPrev, screenTop + 100, 50, btnH,
                Component.translatable("screen.minedevice.atm.previous"),
                () -> {
                    int newOffset = Math.max(0, historyOffset - HISTORY_PAGE_SIZE);
                    if (newOffset != historyOffset) {
                        historyOffset = newOffset;
                        AtmNetworkingClient.sendHistoryRequest(menu.containerId, historyOffset);
                    }
                }));
        addRenderableWidget(new AtmTextButton(xNext, screenTop + 100, 50, btnH,
                Component.translatable("screen.minedevice.atm.next"),
                () -> {
                    int maxOffset = Math.max(0, historyTotal - HISTORY_PAGE_SIZE);
                    int newOffset = Math.min(maxOffset, historyOffset + HISTORY_PAGE_SIZE);
                    if (newOffset != historyOffset) {
                        historyOffset = newOffset;
                        AtmNetworkingClient.sendHistoryRequest(menu.containerId, historyOffset);
                    }
                }));
        addRenderableWidget(makeBackButton(xBack, screenTop + 120, btnW, btnH));
    }

    public void setHistory(int total, int offset, java.util.List<HistoryEntry> entries) {
        this.historyTotal = total;
        this.historyOffset = offset;
        this.historyEntries = entries;
    }

    private AbstractWidget makeNavButton(int x, int y, int w, int h, String key, Page target) {
        return new AtmTextButton(x, y, w, h, Component.translatable(key), () -> setCurrentPage(target));
    }

    private AbstractWidget makeBackButton(int x, int y, int w, int h) {
        return new AtmTextButton(x, y, w, h, Component.translatable("screen.minedevice.atm.back"),
                () -> setCurrentPage(Page.MAIN));
    }

    private AbstractWidget makeEjectButton(int x, int y, int w, int h) {
        return new AtmTextButton(x, y, w, h, Component.translatable("screen.minedevice.atm.eject"),
                this::onEjectPressed);
    }

    private void onEjectPressed() {
        if (menu.getCardState() != AtmMenu.STATE_NO_CARD) {
            startCardEject();
            setCurrentPage(Page.EJECT);
        }
    }

    private AbstractWidget makeWithdrawButton(int x, int y, int w, int h, int value) {
        return new AtmTextButton(x, y, w, h, Component.translatable("screen.minedevice.atm.withdraw", value),
                () -> AtmNetworkingClient.sendMenuWithdraw(menu.containerId, value));
    }

    private AbstractWidget makeDepositButton(int x, int y, int w, int h, String labelKey, int value) {
        Component label = value < 0
                ? Component.translatable(labelKey)
                : Component.translatable(labelKey, value);
        return new AtmTextButton(x, y, w, h, label,
                () -> {
                    if (value < 0) {
                        AtmNetworkingClient.sendMenuDepositAll(menu.containerId);
                    } else {
                        AtmNetworkingClient.sendMenuDeposit(menu.containerId, value);
                    }
                });
    }

    // ──────────────── RENDERING ────────────────

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int left = leftPos;
        int top = topPos;

        // Main panel (Sleek Space Gray border and background)
        drawPanel(guiGraphics, left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT);



        // Screen area
        int sx = left + SCREEN_X;
        int sy = top + SCREEN_Y;
        int sex = sx + SCREEN_W;
        int sey = sy + SCREEN_H;
        guiGraphics.fill(sx - 1, sy - 1, sex + 1, sey + 1, COLOR_SCREEN_EDGE);
        guiGraphics.fill(sx, sy, sex, sey, COLOR_SCREEN);

        // Hardware Bezel Area on the Right
        // 1. CARD Capsule Slot
        int cardCapsuleX = left + 200;
        int cardCapsuleY = top + 24;
        drawCapsule(guiGraphics, cardCapsuleX, cardCapsuleY, 48, 24, 0xFF0D0D10, 0xFF2D3139);
        // Slit line
        guiGraphics.fill(left + 208, top + 35, left + 240, top + 37, 0xFF050506);
        // CARD hardware label
        guiGraphics.drawCenteredString(font, Component.literal("CARD"), left + 224, top + 14, COLOR_TEXT_DIM);

        // LED Indicator Light next to Card slit
        int lightColor = COLOR_SUCCESS;
        if (menu.getCardState() == AtmMenu.STATE_LOCKED) {
            lightColor = COLOR_DANGER;
        } else if (menu.getCardState() == AtmMenu.STATE_NO_CARD) {
            if (Minecraft.getInstance().level != null && (Minecraft.getInstance().level.getGameTime() / 10) % 2 == 0) {
                lightColor = COLOR_SUCCESS;
            } else {
                lightColor = 0xFF065F46; // Dim/Off green
            }
        } else {
            lightColor = COLOR_ACCENT;
        }
        guiGraphics.fill(left + 242, top + 35, left + 244, top + 37, lightColor);

        // 2. CASH Capsule Slot
        int cashCapsuleX = left + 200;
        int cashCapsuleY = top + 98;
        drawCapsule(guiGraphics, cashCapsuleX, cashCapsuleY, 48, 24, 0xFF0D0D10, 0xFF2D3139);
        // Slit line
        guiGraphics.fill(left + 208, top + 109, left + 240, top + 111, 0xFF050506);
        // CASH hardware label
        guiGraphics.drawCenteredString(font, Component.literal("CASH"), left + 224, top + 88, COLOR_TEXT_DIM);

        // Card number if inserted, drawn in top-right of screen
        String maskedNumber = getMaskedCardNumber();
        if (!maskedNumber.isEmpty() && currentPage != Page.WELCOME) {
            Component text = Component.literal(maskedNumber);
            int textWidth = font.width(maskedNumber);
            int maxW = SCREEN_W - 12;
            float scale = 1.0F;
            if (textWidth > maxW) {
                scale = (float) maxW / textWidth;
            }
            int drawnW = (int) (textWidth * scale);
            int x = sex - drawnW - 6;
            int y = sy + 6;
            if (scale < 1.0F) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(x, y + (8.0F - 8.0F * scale) / 2.0F, 0.0F);
                guiGraphics.pose().scale(scale, scale, 1.0F);
                guiGraphics.drawString(font, text, 0, 0, COLOR_TEXT_DIM, false);
                guiGraphics.pose().popPose();
            } else {
                guiGraphics.drawString(font, text, x, y, COLOR_TEXT_DIM, false);
            }
        }

        // ─── render card animation in hardware zone ───
        renderCardInSlot(guiGraphics, left, top);

        // ─── render cash in cash slot ───
        renderCashInSlot(guiGraphics, left, top);
    }

    private void drawCapsule(GuiGraphics guiGraphics, int x, int y, int w, int h, int fillColor, int borderColor) {
        drawRoundedRect(guiGraphics, x, y, w, h, borderColor);
        drawRoundedRect(guiGraphics, x + 1, y + 1, w - 2, h - 2, fillColor);
    }

    private void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        guiGraphics.fill(x + 4, y, x + w - 4, y + h, color);
        guiGraphics.fill(x, y + 4, x + w, y + h - 4, color);
        guiGraphics.fill(x + 2, y + 1, x + w - 2, y + 2, color);
        guiGraphics.fill(x + 1, y + 2, x + w - 1, y + 4, color);
        guiGraphics.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, color);
        guiGraphics.fill(x + 1, y + h - 4, x + w - 1, y + h - 2, color);
    }

    private void renderCardInSlot(GuiGraphics guiGraphics, int left, int top) {
        int slitY = top + 35; // centered slit Y

        RenderSystem.enableBlend();
        if (cardAnimPlaying) {
            float raw = 1.0F - (float) cardAnimTicks / (cardIsInserting ? CARD_INSERT_TICKS : CARD_EJECT_TICKS);
            float eased = cardIsInserting ? easeOutCubic(raw) : easeInQuad(raw);
            
            if (cardIsInserting) {
                // Moving UP into the slit. Top is cut off.
                int dy = (int) (eased * 34);
                int h = 34 - dy;
                if (h > 0) {
                    int vOffset = (int) (dy * (56.0F / 34.0F));
                    int vHeight = 56 - vOffset;
                    guiGraphics.blit(CARD_FRONT_TEXTURE, left + 212, slitY, 24, h, 0, vOffset, 32, vHeight, 32, 56);
                }
            } else {
                // Moving DOWN out of the slit. Top emerges last, bottom first.
                int dy = (int) (eased * 34);
                int h = dy;
                if (h > 0) {
                    int vHeight = (int) (dy * (56.0F / 34.0F));
                    int vOffset = 56 - vHeight;
                    guiGraphics.blit(CARD_FRONT_TEXTURE, left + 212, slitY, 24, h, 0, vOffset, 32, vHeight, 32, 56);
                }
            }
        }
        RenderSystem.disableBlend();
    }

    private static final ResourceLocation BILL20_TEX = new ResourceLocation(Minedevice.MOD_ID, "textures/item/bill20.png");
    private static final ResourceLocation BILL100_TEX = new ResourceLocation(Minedevice.MOD_ID, "textures/item/bill100.png");
    private static final ResourceLocation BILL500_TEX = new ResourceLocation(Minedevice.MOD_ID, "textures/item/bill500.png");
    private static final ResourceLocation BILL1000_TEX = new ResourceLocation(Minedevice.MOD_ID, "textures/item/bill1000.png");

    private void renderCashInSlot(GuiGraphics guiGraphics, int left, int top) {
        int slitY = top + 109; // centered cash slit Y

        boolean hasCash = menu.hasDispenserItems();
        ResourceLocation billTex = BILL20_TEX;
        int billCount = 0;
        if (hasCash) {
            for (int i = 0; i < AtmMenu.DISPENSER_SLOTS; i++) {
                ItemStack s = menu.getDispenserItem(i);
                if (!s.isEmpty()) {
                    if (s.is(ModItems.BILL1000.get())) billTex = BILL1000_TEX;
                    else if (s.is(ModItems.BILL500.get())) billTex = BILL500_TEX;
                    else if (s.is(ModItems.BILL100.get())) billTex = BILL100_TEX;
                    billCount++;
                }
            }
        }

        if (cashAnimPlaying || hasCash) {
            float raw = 1.0F;
            if (cashAnimPlaying) {
                raw = 1.0F - (float) cashAnimTicks / CASH_DISPENSE_TICKS;
            }
            float eased = easeOutQuad(raw);
            int dy = (int) (eased * 18);
            if (dy > 0) {
                int stackCount = Math.min(billCount, 4);
                if (stackCount == 0) stackCount = 2;
                RenderSystem.enableBlend();
                for (int i = 0; i < stackCount; i++) {
                    int bx = left + 206 + i * 2;
                    int by = slitY + i * 2;
                    int vHeight = (int) (dy * (38.0F / 18.0F));
                    int vOffset = 38 - vHeight;
                    guiGraphics.blit(billTex, bx, by, 36, dy, 0, vOffset, 76, vHeight, 76, 38);
                }
                RenderSystem.disableBlend();
            }
        }
    }


    private void drawCenteredScaledString(GuiGraphics guiGraphics, Component text, int centerX, int y, int maxW, int color) {
        String s = text.getString();
        int textWidth = font.width(s);
        float scale = 1.0F;
        if (textWidth > maxW) {
            scale = (float) maxW / textWidth;
        }
        if (scale < 1.0F) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, y + (8.0F - 8.0F * scale) / 2.0F, 0.0F);
            guiGraphics.pose().scale(scale, scale, 1.0F);
            guiGraphics.drawCenteredString(font, text, 0, 0, color);
            guiGraphics.pose().popPose();
        } else {
            guiGraphics.drawCenteredString(font, text, centerX, y, color);
        }
    }

    private void drawCenteredScaledString(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
        drawCenteredScaledString(guiGraphics, text, centerX, y, SCREEN_W - 8, color);
    }

    private void drawScaledString(GuiGraphics guiGraphics, Component text, int x, int y, int maxW, int color) {
        String s = text.getString();
        int textWidth = font.width(s);
        float scale = 1.0F;
        if (textWidth > maxW) {
            scale = (float) maxW / textWidth;
        }
        if (scale < 1.0F) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y + (8.0F - 8.0F * scale) / 2.0F, 0.0F);
            guiGraphics.pose().scale(scale, scale, 1.0F);
            guiGraphics.drawString(font, text, 0, 0, color, false);
            guiGraphics.pose().popPose();
        } else {
            guiGraphics.drawString(font, text, x, y, color, false);
        }
    }

    private void drawScaledString(GuiGraphics guiGraphics, Component text, int x, int y, int color) {
        drawScaledString(guiGraphics, text, x, y, SCREEN_W - 12, color);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        updateActualPosition();

        // Draw dark backdrop across the entire screen in standard coordinates
        guiGraphics.fill(0, 0, this.width, this.height, COLOR_BACKDROP);

        // Convert mouse coordinates to local coordinate space
        int localMouseX = mouseX - actualLeft;
        int localMouseY = mouseY - actualTop;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(actualLeft, actualTop, 0);

        renderBg(guiGraphics, partialTick, localMouseX, localMouseY);
        renderPageContent(guiGraphics, localMouseX, localMouseY, partialTick);
        super.render(guiGraphics, localMouseX, localMouseY, partialTick);
        renderStatus(guiGraphics);

        if (currentPage == Page.TRANSFER) {
            renderTransferLabels(guiGraphics);
        }

        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            this.renderTooltip(guiGraphics, localMouseX, localMouseY);
        }

        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        updateActualPosition();
        double localMouseX = mouseX - actualLeft;
        double localMouseY = mouseY - actualTop;

        if (menu.hasDispenserItems() && !cashAnimPlaying) {
            int left = leftPos;
            int top = topPos;
            if (localMouseX >= left + 200 && localMouseX <= left + 248 && localMouseY >= top + 109 && localMouseY <= top + 135) {
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                AtmNetworkingClient.sendTakeAll(menu.containerId);
                return true;
            }
        }
        return super.mouseClicked(localMouseX, localMouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        updateActualPosition();
        double localMouseX = mouseX - actualLeft;
        double localMouseY = mouseY - actualTop;
        return super.mouseReleased(localMouseX, localMouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        updateActualPosition();
        double localMouseX = mouseX - actualLeft;
        double localMouseY = mouseY - actualTop;
        return super.mouseDragged(localMouseX, localMouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        updateActualPosition();
        double localMouseX = mouseX - actualLeft;
        double localMouseY = mouseY - actualTop;
        return super.mouseScrolled(localMouseX, localMouseY, delta);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        updateActualPosition();
        double localMouseX = mouseX - actualLeft;
        double localMouseY = mouseY - actualTop;
        super.mouseMoved(localMouseX, localMouseY);
    }

    private void renderTransferLabels(GuiGraphics guiGraphics) {
        int sx = leftPos + SCREEN_X;
        int sy = topPos + SCREEN_Y;
        drawScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.transfer.recipient"),
                sx + 12, sy + 20, SCREEN_W - 24, COLOR_TEXT_DIM);
        drawScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.transfer.amount"),
                sx + 12, sy + 58, SCREEN_W - 24, COLOR_TEXT_DIM);
    }

    // ──────────────── PAGE CONTENT ────────────────

    private void renderPageContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int sx = leftPos + SCREEN_X;
        int sy = topPos + SCREEN_Y;

        switch (currentPage) {
            case WELCOME -> renderWelcome(guiGraphics, sx, sy);
            case SET_PIN -> renderPinPrompt(guiGraphics, sx, sy, "screen.minedevice.atm.set_pin");
            case ENTER_PIN -> renderPinPrompt(guiGraphics, sx, sy, "screen.minedevice.atm.enter_pin");
            case MAIN -> renderPageTitle(guiGraphics, sx, sy, "screen.minedevice.atm.main_menu");
            case WITHDRAW -> renderPageTitle(guiGraphics, sx, sy, "screen.minedevice.atm.withdraw_menu");
            case DEPOSIT -> renderPageTitle(guiGraphics, sx, sy, "screen.minedevice.atm.deposit_menu");
            case TRANSFER -> renderPageTitle(guiGraphics, sx, sy, "screen.minedevice.atm.transfer");
            case BALANCE -> renderBalancePage(guiGraphics, sx, sy);
            case HISTORY -> renderHistoryPage(guiGraphics, sx, sy);
            case DISPENSE -> renderDispensePage(guiGraphics, sx, sy);
            case LOCKED -> renderLockedPage(guiGraphics, sx, sy);
            case EJECT -> renderEjectPage(guiGraphics, sx, sy);
        }
    }

    private void renderWelcome(GuiGraphics guiGraphics, int sx, int sy) {
        int cx = sx + SCREEN_W / 2;

        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.welcome"),
                cx, sy + 20, COLOR_ACCENT);
        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.insert_card"),
                cx, sy + 40, COLOR_TEXT);

        // Card insertion graphic
        int iconX = cx - 16;
        int iconY = sy + 65;
        guiGraphics.fill(iconX, iconY, iconX + 32, iconY + 20, 0xFF1F2937);
        guiGraphics.fill(iconX + 1, iconY + 1, iconX + 31, iconY + 19, 0xFF0A0A0C);
        guiGraphics.drawCenteredString(font, Component.literal("→"), cx, iconY + 6, COLOR_ACCENT);

        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.drag_card"),
                cx, sy + 95, COLOR_TEXT_DIM);

        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.accepted_cards"),
                cx, sy + 115, COLOR_HINT);
    }

    private void renderPinPrompt(GuiGraphics guiGraphics, int sx, int sy, String titleKey) {
        int cx = sx + SCREEN_W / 2;

        drawCenteredScaledString(guiGraphics,
                Component.translatable(titleKey),
                cx, sy + 25, COLOR_TEXT);

        int dots = pinBuffer.length();
        StringBuilder dotsText = new StringBuilder();
        for (int i = 0; i < dots; i++) {
            dotsText.append("\u25CF ");
        }
        for (int i = dots; i < 4; i++) {
            dotsText.append("\u25CB ");
        }
        guiGraphics.drawCenteredString(font,
                Component.literal(dotsText.toString().trim()),
                cx, sy + 55, COLOR_TEXT);

        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.pin_hint"),
                cx, sy + 80, COLOR_TEXT_DIM);

        if (menu.getFailedAttempts() > 0) {
            drawCenteredScaledString(guiGraphics,
                    Component.translatable("screen.minedevice.atm.pin_attempts", menu.getFailedAttempts()),
                    cx, sy + 105, COLOR_DANGER);
        }
    }

    private void renderPageTitle(GuiGraphics guiGraphics, int sx, int sy, String key) {
        drawCenteredScaledString(guiGraphics,
                Component.translatable(key),
                sx + SCREEN_W / 2, sy + 12, COLOR_TEXT);
    }

    private void renderBalancePage(GuiGraphics guiGraphics, int sx, int sy) {
        int cx = sx + SCREEN_W / 2;

        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.balance_title"),
                cx, sy + 20, COLOR_TEXT);
        drawCenteredScaledString(guiGraphics,
                Component.literal("$" + NUMBER_FORMAT.format(menu.getBalance())),
                cx, sy + 40, COLOR_SUCCESS);

        String masked = getMaskedCardNumber();
        if (!masked.isEmpty()) {
            drawCenteredScaledString(guiGraphics,
                    Component.translatable("screen.minedevice.atm.card_number"),
                    cx, sy + 68, COLOR_TEXT_DIM);
            drawCenteredScaledString(guiGraphics, Component.literal(masked),
                    cx, sy + 80, COLOR_TEXT);
        }
    }

    private void renderLockedPage(GuiGraphics guiGraphics, int sx, int sy) {
        int cx = sx + SCREEN_W / 2;

        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.locked_title"),
                cx, sy + 25, COLOR_DANGER);
        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.locked_desc"),
                cx, sy + 50, COLOR_TEXT_DIM);
        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.locked_hint"),
                cx, sy + 70, COLOR_TEXT_DIM);
    }

    private void renderEjectPage(GuiGraphics guiGraphics, int sx, int sy) {
        int cx = sx + SCREEN_W / 2;

        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.thank_you"),
                cx, sy + 35, COLOR_TEXT);
        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.take_card"),
                cx, sy + 60, COLOR_TEXT_DIM);
    }

    private void renderDispensePage(GuiGraphics guiGraphics, int sx, int sy) {
        int cx = sx + SCREEN_W / 2;

        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.dispense"),
                cx, sy + 20, COLOR_TEXT);
        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.dispense_hint"),
                cx, sy + 40, COLOR_TEXT_DIM);

        // count dispensed items
        int count = 0;
        for (int i = 0; i < AtmMenu.DISPENSER_SLOTS; i++) {
            if (!menu.getDispenserItem(i).isEmpty()) {
                count++;
            }
        }
        if (count > 0) {
            drawCenteredScaledString(guiGraphics,
                    Component.translatable("screen.minedevice.atm.items_dispensed", count),
                    cx, sy + 65, COLOR_SUCCESS);
        }
    }

    private void renderHistoryPage(GuiGraphics guiGraphics, int sx, int sy) {
        int cx = sx + SCREEN_W / 2;

        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.history"),
                cx, sy + 10, COLOR_TEXT);

        if (historyEntries.isEmpty()) {
            drawCenteredScaledString(guiGraphics,
                    Component.translatable("screen.minedevice.atm.history_empty"),
                    cx, sy + 45, COLOR_TEXT_DIM);
            return;
        }

        int y = sy + 26;
        for (HistoryEntry e : historyEntries) {
            String sign = (e.type == TransactionHistoryStore.TYPE_DEPOSIT
                    || e.type == TransactionHistoryStore.TYPE_TRANSFER_IN) ? "+" : "-";
            String typeLabel = switch (e.type) {
                case TransactionHistoryStore.TYPE_DEPOSIT -> "Deposit";
                case TransactionHistoryStore.TYPE_WITHDRAW -> "Withdraw";
                case TransactionHistoryStore.TYPE_TRANSFER_OUT -> "Transfer Out";
                case TransactionHistoryStore.TYPE_TRANSFER_IN -> "Transfer In";
                default -> "Unknown";
            };
            int color = sign.equals("+") ? COLOR_SUCCESS : COLOR_TEXT;
            drawScaledString(guiGraphics, Component.literal(sign + " " + typeLabel),
                    sx + 6, y, 64, color);
            drawScaledString(guiGraphics, Component.literal("$" + NUMBER_FORMAT.format(e.amount)),
                    sx + 74, y, 36, color);
            y += 14;
        }

        drawCenteredScaledString(guiGraphics,
                Component.translatable("screen.minedevice.atm.history_page", historyOffset + 1, historyTotal),
                cx, sy + 74, COLOR_TEXT_DIM);
    }

    private void renderStatus(GuiGraphics guiGraphics) {
        if (statusMessage == null || statusMessage.isEmpty()) {
            return;
        }
        drawCenteredScaledString(guiGraphics, Component.literal(statusMessage),
                leftPos + PANEL_WIDTH / 2, topPos + PANEL_HEIGHT + 10, PANEL_WIDTH - 20, statusColor);
    }

    private void drawPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        guiGraphics.fill(left, top, right, bottom, COLOR_PANEL_EDGE);
        guiGraphics.fill(left + 2, top + 2, right - 2, bottom - 2, COLOR_PANEL);
    }

    private String getMaskedCardNumber() {
        ItemStack card = menu.getCardStack();
        if (card.isEmpty()) {
            return "";
        }
        String number = CardItem.getCardNumber(card);
        if (number == null || number.isEmpty()) {
            return "";
        }
        if (number.length() < 4) {
            return number;
        }
        return "**** **** **** " + number.substring(number.length() - 4);
    }

    public void setStatus(String message, int color) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = color;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (!menu.isCardlessMode() && menu.getCardState() != AtmMenu.STATE_NO_CARD && currentPage != Page.EJECT && currentPage != Page.WELCOME) {
            onEjectPressed();
        } else {
            super.onClose();
        }
    }

    // ──────────────── INNER CLASSES ────────────────

    private final class AtmTextButton extends AbstractWidget {
        private final Component label;
        private final Runnable onPress;

        AtmTextButton(int x, int y, int w, int h, Component label, Runnable onPress) {
            super(x, y, w, h, label);
            this.label = label;
            this.onPress = onPress;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int fill = isHoveredOrFocused() ? COLOR_BUTTON_HOVER : COLOR_BUTTON;
            if (isFocused()) {
                fill = COLOR_BUTTON_PRESSED;
            }
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, COLOR_BUTTON_EDGE);
            guiGraphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, fill);
            
            String s = label.getString();
            int textWidth = font.width(s);
            int maxW = width - 4;
            float scale = 1.0F;
            if (textWidth > maxW) {
                scale = (float) maxW / textWidth;
            }
            if (scale < 1.0F) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(getX() + width / 2.0F, getY() + (height - 8.0F * scale) / 2.0F, 0.0F);
                guiGraphics.pose().scale(scale, scale, 1.0F);
                guiGraphics.drawCenteredString(font, label, 0, 0, COLOR_BUTTON_TEXT);
                guiGraphics.pose().popPose();
            } else {
                guiGraphics.drawCenteredString(font, label, getX() + width / 2, getY() + (height - 8) / 2, COLOR_BUTTON_TEXT);
            }
        }

        @Override
        public void playDownSound(net.minecraft.client.sounds.SoundManager soundManager) {
            soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            onPress.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }


    private final class CasingKeypadButton extends AbstractWidget {
        private final String label;
        private final Runnable onPress;

        CasingKeypadButton(int x, int y, int w, int h, String label, Runnable onPress) {
            super(x, y, w, h, Component.literal(label));
            this.label = label;
            this.onPress = onPress;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int fill;
            if (label.equals("C")) {
                fill = isHoveredOrFocused() ? 0xFFDC2626 : COLOR_DANGER;
            } else if (label.equals("OK")) {
                fill = isHoveredOrFocused() ? 0xFF059669 : COLOR_SUCCESS;
            } else {
                fill = isHoveredOrFocused() ? COLOR_BUTTON_HOVER : 0xFF33353F;
            }
            int textCol = (isHoveredOrFocused() && !label.equals("C") && !label.equals("OK")) ? COLOR_SCREEN : COLOR_TEXT;
            
            // Draw 3D border/shadow
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF1E2026);
            guiGraphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, fill);
            
            guiGraphics.drawCenteredString(font, label, getX() + width / 2, getY() + (height - 8) / 2, textCol);
        }

        @Override
        public void playDownSound(net.minecraft.client.sounds.SoundManager soundManager) {
            soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            onPress.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }
}
