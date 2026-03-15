package com.sammy.minedevice.client.phone;

import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.client.homephone.HomePhoneScreen;
import com.sammy.minedevice.phone.PhoneCallState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PhoneClientHooks {
    private PhoneClientHooks() {
    }

    public static void openScreen(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.setScreen(new PhoneScreen(hand));
        }
    }

    public static void openHomePhoneScreen(BlockPos blockPos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.setScreen(new HomePhoneScreen(blockPos));
        }
    }

    public static boolean shouldHideCrosshair() {
        PhoneScreen phoneScreen = getOpenPhoneScreen();
        return phoneScreen != null && phoneScreen.isCameraModeActive();
    }

    public static boolean shouldRaisePhoneArm(LivingEntity entity) {
        LocalPlayer localPlayer = getLocalPlayer();
        if (localPlayer == null || entity != localPlayer) {
            return false;
        }

        ensurePhoneHeldForCall(localPlayer);
        if (isMobileCallActive()) {
            return isPhone(localPlayer.getMainHandItem()) || isPhone(localPlayer.getOffhandItem());
        }

        PhoneScreen phoneScreen = getOpenPhoneScreen();
        if (phoneScreen == null || phoneScreen.isHomePhoneMode()) {
            return false;
        }

        // Keep phone pose active for the whole camera app session,
        // even if the currently held stack changes while the screen is open.
        if (phoneScreen.isCameraModeActive()) {
            return true;
        }

        return resolvePhoneHand(localPlayer, phoneScreen) != null;
    }

    public static boolean shouldUseSelfieCamera(LivingEntity entity) {
        LocalPlayer localPlayer = getLocalPlayer();
        if (localPlayer == null || entity != localPlayer) {
            return false;
        }

        PhoneScreen phoneScreen = getOpenPhoneScreen();
        if (phoneScreen != null && phoneScreen.isHomePhoneMode()) {
            return false;
        }
        return phoneScreen != null && phoneScreen.isSelfieCameraModeActive();
    }

    public static float getActivePhoneCameraZoomFactor() {
        PhoneScreen phoneScreen = getOpenPhoneScreen();
        if (phoneScreen == null || phoneScreen.isHomePhoneMode() || !phoneScreen.isCameraModeActive()) {
            return 1.0F;
        }

        return phoneScreen.getCameraZoomFactor();
    }

    public static HumanoidArm getRaisedPhoneArm(Player player) {
        if (player instanceof LocalPlayer localPlayer) {
            ensurePhoneHeldForCall(localPlayer);
            if (isMobileCallActive()) {
                if (isPhone(localPlayer.getOffhandItem())) {
                    return localPlayer.getMainArm().getOpposite();
                }
                return localPlayer.getMainArm();
            }
        }

        PhoneScreen phoneScreen = getOpenPhoneScreen();
        if (phoneScreen == null) {
            return player.getMainArm();
        }
        if (phoneScreen.isHomePhoneMode()) {
            return player.getMainArm();
        }

        InteractionHand phoneHand = resolvePhoneHand(player, phoneScreen);
        if (phoneHand == null) {
            return player.getMainArm();
        }

        return phoneHand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
    }

    public static HumanoidArm getRaisedPhoneArm(LivingEntity entity) {
        if (entity instanceof Player player) {
            return getRaisedPhoneArm(player);
        }

        return HumanoidArm.RIGHT;
    }

    public static boolean shouldUsePhoneCallPose(LivingEntity entity) {
        LocalPlayer localPlayer = getLocalPlayer();
        if (localPlayer == null || entity != localPlayer) {
            return false;
        }

        ensurePhoneHeldForCall(localPlayer);
        return isMobileCallActive()
                && (isPhone(localPlayer.getMainHandItem()) || isPhone(localPlayer.getOffhandItem()));
    }

    private static LocalPlayer getLocalPlayer() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.player;
    }

    private static PhoneScreen getOpenPhoneScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || !(minecraft.screen instanceof PhoneScreen phoneScreen)) {
            return null;
        }

        return phoneScreen;
    }

    private static InteractionHand resolvePhoneHand(Player player, PhoneScreen phoneScreen) {
        if (isPhone(player.getItemInHand(phoneScreen.getOpenHand()))) {
            return phoneScreen.getOpenHand();
        }

        if (isPhone(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }

        if (isPhone(player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    private static boolean isPhone(ItemStack stack) {
        return stack.is(ModItems.PHONE.get());
    }

    private static boolean isMobileCallActive() {
        PhoneCallState state = PhoneClientCallState.getState();
        return state == PhoneCallState.OUTGOING_RINGING
                || state == PhoneCallState.CONNECTED;
    }

    private static void ensurePhoneHeldForCall(LocalPlayer player) {
        if (!isMobileCallActive()) {
            return;
        }
        if (isPhone(player.getMainHandItem()) || isPhone(player.getOffhandItem())) {
            return;
        }

        for (int slot = 0; slot < 9; slot++) {
            if (isPhone(player.getInventory().getItem(slot))) {
                player.getInventory().selected = slot;
                return;
            }
        }
    }
}
