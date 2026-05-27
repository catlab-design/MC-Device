package com.sammy.minedevice.phone;

import net.minecraft.world.entity.player.Player;

public interface PhoneCallPoseAccess {
    boolean minedevice$isPhoneCallPoseActive();

    void minedevice$setPhoneCallPoseActive(boolean active);

    boolean minedevice$isPhoneCameraPoseActive();

    void minedevice$setPhoneCameraPoseActive(boolean active);

    boolean minedevice$isPhoneCameraSelfieActive();

    void minedevice$setPhoneCameraSelfieActive(boolean active);

    boolean minedevice$isPhoneBankQrPoseActive();

    void minedevice$setPhoneBankQrPoseActive(boolean active);

    boolean minedevice$isPhoneScreenOnActive();

    void minedevice$setPhoneScreenOnActive(boolean active);

    static boolean isPhoneCallPoseActive(Player player) {
        return player instanceof PhoneCallPoseAccess access && access.minedevice$isPhoneCallPoseActive();
    }

    static void setPhoneCallPoseActive(Player player, boolean active) {
        if (player instanceof PhoneCallPoseAccess access) {
            access.minedevice$setPhoneCallPoseActive(active);
        }
    }

    static boolean isPhoneCameraPoseActive(Player player) {
        return player instanceof PhoneCallPoseAccess access && access.minedevice$isPhoneCameraPoseActive();
    }

    static void setPhoneCameraPoseActive(Player player, boolean active) {
        if (player instanceof PhoneCallPoseAccess access) {
            access.minedevice$setPhoneCameraPoseActive(active);
            if (!active) {
                access.minedevice$setPhoneCameraSelfieActive(false);
            }
        }
    }

    static boolean isPhoneCameraSelfieActive(Player player) {
        return player instanceof PhoneCallPoseAccess access
                && access.minedevice$isPhoneCameraPoseActive()
                && access.minedevice$isPhoneCameraSelfieActive();
    }

    static void setPhoneCameraSelfieActive(Player player, boolean active) {
        if (player instanceof PhoneCallPoseAccess access) {
            access.minedevice$setPhoneCameraSelfieActive(active);
        }
    }

    static boolean isPhoneBankQrPoseActive(Player player) {
        return player instanceof PhoneCallPoseAccess access && access.minedevice$isPhoneBankQrPoseActive();
    }

    static void setPhoneBankQrPoseActive(Player player, boolean active) {
        if (player instanceof PhoneCallPoseAccess access) {
            access.minedevice$setPhoneBankQrPoseActive(active);
        }
    }

    static boolean isPhoneScreenOnActive(Player player) {
        return player instanceof PhoneCallPoseAccess access && access.minedevice$isPhoneScreenOnActive();
    }

    static void setPhoneScreenOnActive(Player player, boolean active) {
        if (player instanceof PhoneCallPoseAccess access) {
            access.minedevice$setPhoneScreenOnActive(active);
        }
    }
}
