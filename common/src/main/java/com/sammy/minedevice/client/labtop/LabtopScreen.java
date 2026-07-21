package com.sammy.minedevice.client.labtop;

import com.sammy.minedevice.Minedevice;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class LabtopScreen extends Screen {
    private static final ResourceLocation FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Minedevice.MOD_ID, "textures/gui/labtop_gui/labtop_frame.png");
    private static final int IMG_W = 224;
    private static final int IMG_H = 139;
    private static final float SCALE = 1.5F;

    private final BlockPos pos;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public LabtopScreen(BlockPos pos) {
        super(Component.translatable("block.minedevice.labtop"));
        this.pos = pos.immutable();
    }

    @Override
    protected void init() {
        panelW = (int)(IMG_W * SCALE);
        panelH = (int)(IMG_H * SCALE);
        this.panelX = (this.width - panelW) / 2;
        this.panelY = (this.height - panelH) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(FRAME_TEXTURE, panelX, panelY, panelW, panelH, 0, 0, IMG_W, IMG_H, IMG_W, IMG_H);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
