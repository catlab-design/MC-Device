package com.sammy.minedevice.client.atm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class BankScreen extends Screen {
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 200;
    private static final int COLOR_BACKDROP = 0x99000000;
    private static final int COLOR_PANEL_EDGE = 0xFF1B3A5C;
    private static final int COLOR_PANEL = 0xFFE8EDF3;
    private static final int COLOR_SCREEN = 0xFF0F1A2A;
    private static final int COLOR_TEXT = 0xFFE8EDF3;
    private static final int COLOR_TEXT_DIM = 0xFF8FA3B8;
    private static final int COLOR_DANGER = 0xFFDC2626;
    private static final int COLOR_SUCCESS = 0xFF10B981;
    private static final int COLOR_BUTTON_EDGE = 0xFF1F242B;
    private static final int COLOR_BUTTON = 0xFF2D3748;
    private static final int COLOR_BUTTON_HOVER = 0xFF3B82F6;

    private final StringBuilder pinBuffer = new StringBuilder();
    private String statusMessage = "";
    private int statusColor = COLOR_TEXT_DIM;

    public BankScreen() {
        super(Component.translatable("screen.minedevice.bank.title"));
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int keypadX = left + 58;
        int keypadY = top + 90;
        int btnSize = 24;
        int gap = 3;
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK"};
        for (int i = 0; i < keys.length; i++) {
            int row = i / 3;
            int col = i % 3;
            String key = keys[i];
            int x = keypadX + col * (btnSize + gap);
            int y = keypadY + row * (btnSize + gap);
            addRenderableWidget(new KeypadBtn(x, y, btnSize, btnSize, key, () -> onKeyPress(key)));
        }
    }

    private void onKeyPress(String key) {
        if (key.equals("C")) {
            pinBuffer.setLength(0);
            statusMessage = "";
            return;
        }
        if (key.equals("OK")) {
            if (pinBuffer.length() == 4) {
                AtmNetworkingClient.sendBankUnlock(pinBuffer.toString());
                statusMessage = "screen.minedevice.bank.unlocking";
                statusColor = COLOR_SUCCESS;
                pinBuffer.setLength(0);
            }
            return;
        }
        if (pinBuffer.length() < 4) {
            pinBuffer.append(key);
            statusMessage = "";
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            onKeyPress(String.valueOf(keyCode - GLFW.GLFW_KEY_0));
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            onKeyPress(String.valueOf(keyCode - GLFW.GLFW_KEY_KP_0));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint >= '0' && codePoint <= '9') {
            onKeyPress(String.valueOf(codePoint));
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, COLOR_BACKDROP);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_PANEL_EDGE);
        guiGraphics.fill(left + 2, top + 2, left + PANEL_WIDTH - 2, top + PANEL_HEIGHT - 2, COLOR_PANEL);
        guiGraphics.fill(left + 8, top + 28, left + PANEL_WIDTH - 8, top + 170, COLOR_SCREEN);

        guiGraphics.drawCenteredString(font, Component.literal("CAT BANK"),
                left + PANEL_WIDTH / 2, top + 10, 0xFF3B82F6);
        guiGraphics.drawCenteredString(font,
                Component.translatable("screen.minedevice.bank.title"),
                left + PANEL_WIDTH / 2, top + 38, COLOR_TEXT);
        guiGraphics.drawCenteredString(font,
                Component.translatable("screen.minedevice.bank.enter_new_pin"),
                left + PANEL_WIDTH / 2, top + 56, COLOR_TEXT_DIM);

        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < pinBuffer.length(); i++) {
            dots.append("\u25CF ");
        }
        for (int i = pinBuffer.length(); i < 4; i++) {
            dots.append("\u25CB ");
        }
        guiGraphics.drawCenteredString(font, Component.literal(dots.toString().trim()),
                left + PANEL_WIDTH / 2, top + 72, COLOR_TEXT);

        if (!statusMessage.isEmpty()) {
            Component status = statusMessage.startsWith("screen.") || statusMessage.startsWith("bank.")
                    ? Component.translatable(statusMessage) : Component.literal(statusMessage);
            guiGraphics.drawCenteredString(font, status,
                    left + PANEL_WIDTH / 2, top + PANEL_HEIGHT - 14, statusColor);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class KeypadBtn extends AbstractWidget {
        private final String key;
        private final Runnable onPress;

        KeypadBtn(int x, int y, int w, int h, String key, Runnable onPress) {
            super(x, y, w, h, Component.literal(key));
            this.key = key;
            this.onPress = onPress;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int fill = isHoveredOrFocused() ? COLOR_BUTTON_HOVER : COLOR_BUTTON;
            if (key.equals("C")) {
                fill = isHoveredOrFocused() ? 0xFFB91C1C : COLOR_DANGER;
            } else if (key.equals("OK")) {
                fill = isHoveredOrFocused() ? 0xFF059669 : COLOR_SUCCESS;
            }
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, COLOR_BUTTON_EDGE);
            guiGraphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, fill);
            guiGraphics.drawCenteredString(font, Component.literal(key),
                    getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFFFFF);
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
