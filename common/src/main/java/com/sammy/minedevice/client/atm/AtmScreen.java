package com.sammy.minedevice.client.atm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.Locale;

public final class AtmScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 234;
    private static final int COLOR_BACKDROP = 0x99000000;
    private static final int COLOR_PANEL_EDGE = 0xFF252A31;
    private static final int COLOR_PANEL = 0xFFF2F3F5;
    private static final int COLOR_PANEL_INNER = 0xFFFFFFFF;
    private static final int COLOR_FIELD = 0xFF163D35;
    private static final int COLOR_FIELD_EDGE = 0xFF3D4A46;
    private static final int COLOR_FIELD_TEXT = 0xFFE8FFF1;
    private static final int COLOR_TEXT = 0xFF171C22;
    private static final int COLOR_HINT = 0xFF6A7280;
    private static final int COLOR_BUTTON_EDGE = 0xFF1F242B;
    private static final int COLOR_BUTTON = 0xFF8E939B;
    private static final int COLOR_BUTTON_HOVER = 0xFFA4AAB3;
    private static final int COLOR_BUTTON_PRESSED = 0xFF747A83;
    private static final int COLOR_BUTTON_TEXT = 0xFFFFFFFF;
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.ROOT);

    private final BlockPos atmPos;
    private long balance;

    public AtmScreen(BlockPos atmPos, long balance) {
        super(Component.translatable("screen.minedevice.atm.title"));
        this.atmPos = atmPos.immutable();
        this.balance = Math.max(0L, balance);
    }

    public static void updateOpenScreen(BlockPos atmPos, long balance) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || !(minecraft.screen instanceof AtmScreen screen)) {
            return;
        }

        if (!screen.atmPos.equals(atmPos)) {
            return;
        }

        screen.setBalance(balance);
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // The ATM draws its own backdrop before widgets; vanilla 1.21 would blur the custom UI.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, COLOR_BACKDROP);

        int left = panelLeft();
        int top = panelTop();
        drawPanel(guiGraphics, left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT);

        guiGraphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 12, COLOR_TEXT);

        int screenLeft = left + 22;
        int screenTop = top + 30;
        int screenWidth = PANEL_WIDTH - 44;
        int screenHeight = 42;
        drawField(guiGraphics, screenLeft, screenTop, screenLeft + screenWidth, screenTop + screenHeight);

        guiGraphics.drawString(font,
                Component.translatable("screen.minedevice.atm.balance"),
                screenLeft + 10,
                screenTop + 8,
                COLOR_FIELD_TEXT,
                false);
        guiGraphics.drawString(font,
                Component.literal(NUMBER_FORMAT.format(balance)),
                screenLeft + 10,
                screenTop + 24,
                COLOR_FIELD_TEXT,
                false);

        guiGraphics.drawString(font,
                Component.translatable("screen.minedevice.atm.hint"),
                left + 22,
                top + 82,
                COLOR_HINT,
                false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        int left = panelLeft();
        int top = panelTop();
        int buttonWidth = 92;
        int buttonHeight = 20;
        int gap = 8;
        int leftColumn = left + 22;
        int rightColumn = left + PANEL_WIDTH - 22 - buttonWidth;
        int rowOne = top + 96;
        int rowTwo = rowOne + buttonHeight + gap;
        int rowThree = rowTwo + buttonHeight + gap;
        int rowFour = rowThree + buttonHeight + gap;
        int rowFive = rowFour + buttonHeight + gap;

        addRenderableWidget(new AtmButton(
                leftColumn,
                rowOne,
                buttonWidth,
                buttonHeight,
                Component.translatable("screen.minedevice.atm.deposit_all"),
                () -> AtmNetworkingClient.requestDepositAll(atmPos)
        ));

        addRenderableWidget(new AtmButton(
                rightColumn,
                rowOne,
                buttonWidth,
                buttonHeight,
                Component.translatable("screen.minedevice.atm.withdraw_all"),
                () -> AtmNetworkingClient.requestWithdrawAll(atmPos)
        ));

        addRenderableWidget(new AtmButton(
                leftColumn,
                rowTwo,
                buttonWidth,
                buttonHeight,
                Component.translatable("screen.minedevice.atm.deposit", 20),
                () -> AtmNetworkingClient.requestDeposit(atmPos, 20)
        ));

        addRenderableWidget(new AtmButton(
                rightColumn,
                rowTwo,
                buttonWidth,
                buttonHeight,
                Component.translatable("screen.minedevice.atm.deposit", 100),
                () -> AtmNetworkingClient.requestDeposit(atmPos, 100)
        ));

        addRenderableWidget(new AtmButton(
                leftColumn,
                rowThree,
                buttonWidth,
                buttonHeight,
                Component.translatable("screen.minedevice.atm.deposit", 500),
                () -> AtmNetworkingClient.requestDeposit(atmPos, 500)
        ));

        addRenderableWidget(new AtmButton(
                rightColumn,
                rowThree,
                buttonWidth,
                buttonHeight,
                Component.translatable("screen.minedevice.atm.deposit", 1000),
                () -> AtmNetworkingClient.requestDeposit(atmPos, 1000)
        ));

        addRenderableWidget(new AtmButton(
                leftColumn,
                rowFour,
                buttonWidth,
                buttonHeight,
                Component.translatable("screen.minedevice.atm.withdraw", 20),
                () -> AtmNetworkingClient.requestWithdraw(atmPos, 20)
        ));

        addRenderableWidget(new AtmButton(
                rightColumn,
                rowFour,
                buttonWidth,
                buttonHeight,
                Component.translatable("screen.minedevice.atm.withdraw", 100),
                () -> AtmNetworkingClient.requestWithdraw(atmPos, 100)
        ));

        addRenderableWidget(new AtmButton(
                leftColumn,
                rowFive,
                buttonWidth,
                buttonHeight,
                Component.translatable("screen.minedevice.atm.withdraw", 500),
                () -> AtmNetworkingClient.requestWithdraw(atmPos, 500)
        ));

        addRenderableWidget(new AtmButton(
                rightColumn,
                rowFive,
                buttonWidth,
                buttonHeight,
                Component.translatable("screen.minedevice.atm.withdraw", 1000),
                () -> AtmNetworkingClient.requestWithdraw(atmPos, 1000)
        ));
    }

    private void setBalance(long balance) {
        this.balance = Math.max(0L, balance);
        rebuildWidgets();
    }

    private int panelLeft() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private static void drawPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        guiGraphics.fill(left, top, right, bottom, COLOR_PANEL_EDGE);
        guiGraphics.fill(left + 2, top + 2, right - 2, bottom - 2, COLOR_PANEL);
        guiGraphics.fill(left + 4, top + 4, right - 4, bottom - 4, COLOR_PANEL_INNER);
    }

    private static void drawField(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        guiGraphics.fill(left, top, right, bottom, COLOR_FIELD_EDGE);
        guiGraphics.fill(left + 2, top + 2, right - 2, bottom - 2, COLOR_FIELD);
    }

    private final class AtmButton extends AbstractWidget {
        private final Runnable onPress;

        private AtmButton(int x, int y, int width, int height, Component message, Runnable onPress) {
            super(x, y, width, height, message);
            this.onPress = onPress;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int fill = isHoveredOrFocused() ? COLOR_BUTTON_HOVER : COLOR_BUTTON;
            if (isFocused()) {
                fill = COLOR_BUTTON_PRESSED;
            }

            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, COLOR_BUTTON_EDGE);
            guiGraphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, fill);
            guiGraphics.fill(getX() + 3, getY() + 3, getX() + width - 3, getY() + 5, 0x55FFFFFF);
            guiGraphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, COLOR_BUTTON_TEXT);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            onPress.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
