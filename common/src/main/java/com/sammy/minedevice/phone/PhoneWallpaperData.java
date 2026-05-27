package com.sammy.minedevice.phone;

import com.sammy.minedevice.item.ItemNbt;

import com.sammy.minedevice.Minedevice;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Manages wallpaper settings stored in the phone's NBT tag.
 * Supports separate wallpapers for lock screen and home screen.
 * Wallpaper can be a built-in resource (e.g. "default", "wallpaper1") or a photo file name.
 */
public final class PhoneWallpaperData {

    public static final String WALLPAPER_TAG = "wallpaper";
    public static final String LOCK_WALLPAPER_TAG = "lock_wallpaper";
    public static final String HOME_WALLPAPER_TAG = "home_wallpaper";

    // Prefix for built-in wallpapers stored in assets
    public static final String BUILTIN_PREFIX = "builtin:";
    // Prefix for photo-based wallpapers (file name)
    public static final String PHOTO_PREFIX = "photo:";

    public static final String DEFAULT_WALLPAPER = BUILTIN_PREFIX + "default";

    /** All built-in wallpaper resource names (without prefix) */
    public static final String[] BUILTIN_WALLPAPERS = {
            "default",
            "wallpaper1",
            "wallpaper2"
    };

    private PhoneWallpaperData() {
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public static String getLockWallpaper(ItemStack phoneStack) {
        if (phoneStack.isEmpty() || !ItemNbt.hasTag(phoneStack)) {
            return DEFAULT_WALLPAPER;
        }
        CompoundTag tag = ItemNbt.getTag(phoneStack);
        if (tag == null || !tag.contains(WALLPAPER_TAG)) {
            return DEFAULT_WALLPAPER;
        }
        CompoundTag wallpaperTag = tag.getCompound(WALLPAPER_TAG);
        String value = wallpaperTag.getString(LOCK_WALLPAPER_TAG);
        return value.isEmpty() ? DEFAULT_WALLPAPER : value;
    }

    public static String getHomeWallpaper(ItemStack phoneStack) {
        if (phoneStack.isEmpty() || !ItemNbt.hasTag(phoneStack)) {
            return DEFAULT_WALLPAPER;
        }
        CompoundTag tag = ItemNbt.getTag(phoneStack);
        if (tag == null || !tag.contains(WALLPAPER_TAG)) {
            return DEFAULT_WALLPAPER;
        }
        CompoundTag wallpaperTag = tag.getCompound(WALLPAPER_TAG);
        String value = wallpaperTag.getString(HOME_WALLPAPER_TAG);
        return value.isEmpty() ? DEFAULT_WALLPAPER : value;
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public static void setLockWallpaper(ItemStack phoneStack, String wallpaper) {
        if (phoneStack.isEmpty()) return;
        CompoundTag tag = ItemNbt.getOrCreateTag(phoneStack);
        CompoundTag wallpaperTag = tag.contains(WALLPAPER_TAG)
                ? tag.getCompound(WALLPAPER_TAG)
                : new CompoundTag();
        wallpaperTag.putString(LOCK_WALLPAPER_TAG, wallpaper == null ? DEFAULT_WALLPAPER : wallpaper);
        tag.put(WALLPAPER_TAG, wallpaperTag);
    }

    public static void setHomeWallpaper(ItemStack phoneStack, String wallpaper) {
        if (phoneStack.isEmpty()) return;
        CompoundTag tag = ItemNbt.getOrCreateTag(phoneStack);
        CompoundTag wallpaperTag = tag.contains(WALLPAPER_TAG)
                ? tag.getCompound(WALLPAPER_TAG)
                : new CompoundTag();
        wallpaperTag.putString(HOME_WALLPAPER_TAG, wallpaper == null ? DEFAULT_WALLPAPER : wallpaper);
        tag.put(WALLPAPER_TAG, wallpaperTag);
    }

    public static void setBothWallpapers(ItemStack phoneStack, String wallpaper) {
        setLockWallpaper(phoneStack, wallpaper);
        setHomeWallpaper(phoneStack, wallpaper);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static boolean isBuiltin(String wallpaper) {
        return wallpaper != null && wallpaper.startsWith(BUILTIN_PREFIX);
    }

    public static boolean isPhoto(String wallpaper) {
        return wallpaper != null && wallpaper.startsWith(PHOTO_PREFIX);
    }

    /** Returns the built-in resource name (without prefix), or null if not built-in. */
    public static String getBuiltinName(String wallpaper) {
        if (!isBuiltin(wallpaper)) return null;
        return wallpaper.substring(BUILTIN_PREFIX.length());
    }

    /** Returns the photo file name (without prefix), or null if not a photo. */
    public static String getPhotoFileName(String wallpaper) {
        if (!isPhoto(wallpaper)) return null;
        return wallpaper.substring(PHOTO_PREFIX.length());
    }

    /** Builds a wallpaper key for a built-in wallpaper by index. */
    public static String builtinKey(int index) {
        if (index < 0 || index >= BUILTIN_WALLPAPERS.length) return DEFAULT_WALLPAPER;
        return BUILTIN_PREFIX + BUILTIN_WALLPAPERS[index];
    }

    /** Builds a wallpaper key for a photo file. */
    public static String photoKey(String fileName) {
        return PHOTO_PREFIX + fileName;
    }

    /** Resolves a built-in wallpaper key to its ResourceLocation. */
    public static ResourceLocation resolveBuiltinTexture(String wallpaper) {
        String name = getBuiltinName(wallpaper);
        if (name == null || name.isEmpty()) {
            name = "default";
        }
        return ResourceLocation.fromNamespaceAndPath(Minedevice.MOD_ID, "textures/gui/wallpaper/" + name + ".png");
    }
}
