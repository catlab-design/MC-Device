package com.sammy.minedevice.phone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Manages lock-screen appearance and clock settings stored in the phone's NBT tag:
 * the accent color used for the unlock icon and lock-screen time text, and whether
 * the clock shows in-game or real-life time, in 12- or 24-hour format.
 */
public final class PhoneAppearanceData {

    public static final String SETTINGS_TAG = "device_settings";
    public static final String LOCK_ACCENT_COLOR_TAG = "lock_accent_color";
    public static final String TIME_REAL_LIFE_TAG = "time_real_life";
    public static final String TIME_12_HOUR_TAG = "time_12_hour";

    /** Default lock-screen accent color: black. */
    public static final int DEFAULT_LOCK_ACCENT_COLOR = 0xFF000000;

    /** Selectable accent colors offered in the Appearance color picker. */
    public static final int[] ACCENT_COLOR_CHOICES = {
            0xFFFFFFFF, // white
            0xFF111827, // near-black
            0xFF3B82F6, // blue
            0xFF10B981, // green
            0xFFF97316, // orange
            0xFFEF4444, // red
            0xFFA855F7, // purple
            0xFFFACC15, // yellow
    };

    private PhoneAppearanceData() {
    }

    private static CompoundTag getSettingsTag(ItemStack phoneStack, boolean create) {
        if (phoneStack.isEmpty()) {
            return null;
        }
        CompoundTag tag = create ? phoneStack.getOrCreateTag() : phoneStack.getTag();
        if (tag == null) {
            return null;
        }
        if (create) {
            CompoundTag settings = tag.contains(SETTINGS_TAG) ? tag.getCompound(SETTINGS_TAG) : new CompoundTag();
            tag.put(SETTINGS_TAG, settings);
            return settings;
        }
        return tag.contains(SETTINGS_TAG) ? tag.getCompound(SETTINGS_TAG) : null;
    }

    // ── Lock accent color ───────────────────────────────────────────────────────

    public static int getLockAccentColor(ItemStack phoneStack) {
        CompoundTag settings = getSettingsTag(phoneStack, false);
        if (settings == null || !settings.contains(LOCK_ACCENT_COLOR_TAG)) {
            return DEFAULT_LOCK_ACCENT_COLOR;
        }
        return settings.getInt(LOCK_ACCENT_COLOR_TAG);
    }

    public static void setLockAccentColor(ItemStack phoneStack, int argbColor) {
        CompoundTag settings = getSettingsTag(phoneStack, true);
        if (settings == null) {
            return;
        }
        settings.putInt(LOCK_ACCENT_COLOR_TAG, argbColor);
    }

    // ── Clock source / format ────────────────────────────────────────────────────

    /** True = show the player's real-life system clock; false (default) = in-game time. */
    public static boolean isRealLifeTime(ItemStack phoneStack) {
        CompoundTag settings = getSettingsTag(phoneStack, false);
        return settings != null && settings.getBoolean(TIME_REAL_LIFE_TAG);
    }

    public static void setRealLifeTime(ItemStack phoneStack, boolean realLife) {
        CompoundTag settings = getSettingsTag(phoneStack, true);
        if (settings == null) {
            return;
        }
        settings.putBoolean(TIME_REAL_LIFE_TAG, realLife);
    }

    /** True = 12-hour clock with AM/PM; false (default) = 24-hour clock. */
    public static boolean is12HourFormat(ItemStack phoneStack) {
        CompoundTag settings = getSettingsTag(phoneStack, false);
        return settings != null && settings.getBoolean(TIME_12_HOUR_TAG);
    }

    public static void set12HourFormat(ItemStack phoneStack, boolean use12Hour) {
        CompoundTag settings = getSettingsTag(phoneStack, true);
        if (settings == null) {
            return;
        }
        settings.putBoolean(TIME_12_HOUR_TAG, use12Hour);
    }
}
