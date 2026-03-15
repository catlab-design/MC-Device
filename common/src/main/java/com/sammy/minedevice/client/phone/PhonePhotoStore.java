package com.sammy.minedevice.client.phone;

import com.mojang.blaze3d.platform.NativeImage;
import com.sammy.minedevice.Minedevice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class PhonePhotoStore {
    private static final int MAX_PHOTOS = 302;
    private static final int PHOTO_PREVIEW_SIZE = 200;
    private static final String PHOTO_DIRECTORY_NAME = "minedevice_phone";
    private static final String PHOTOS_TAG = "photos";
    private static final String PHOTO_FILE_TAG = "file";
    private static final String PHOTO_ITEM_TAG = "item";
    private static final String PHOTO_COUNT_TAG = "count";
    private static final String PHOTO_NAME_TAG = "name";
    private static final String PHOTO_TIME_TAG = "time";

    private final String modId;
    private final Map<String, PhotoTexture> previewPhotoTextures = new HashMap<>();
    private final Map<String, PhotoTexture> fullPhotoTextures = new HashMap<>();

    PhonePhotoStore(String modId) {
        this.modId = Objects.requireNonNull(modId, "modId");
    }

    int maxPhotos() {
        return MAX_PHOTOS;
    }

    int getPhotoCount(ItemStack phoneStack) {
        if (phoneStack.isEmpty() || !phoneStack.hasTag()) {
            return 0;
        }

        CompoundTag phoneTag = phoneStack.getTag();
        if (phoneTag == null || !phoneTag.contains(PHOTOS_TAG, Tag.TAG_LIST)) {
            return 0;
        }

        return phoneTag.getList(PHOTOS_TAG, Tag.TAG_COMPOUND).size();
    }

    boolean isPhotoLimitReached(ItemStack phoneStack) {
        return getPhotoCount(phoneStack) >= MAX_PHOTOS;
    }

    String writePhoto(NativeImage image) {
        if (image == null) {
            return null;
        }

        try {
            Path photoDirectory = getPhotoDirectory();
            Files.createDirectories(photoDirectory);
            String fileName = "phone_" + System.currentTimeMillis() + ".png";
            Path photoPath = photoDirectory.resolve(fileName);
            image.writeToFile(photoPath.toFile());
            return fileName;
        } catch (IOException exception) {
            Minedevice.LOGGER.debug("Failed to write phone photo", exception);
            return null;
        }
    }

    boolean appendPhotoToStack(ItemStack phoneStack, String photoFileName, ItemStack captureStack) {
        if (phoneStack.isEmpty()) {
            return false;
        }

        CompoundTag phoneTag = phoneStack.getOrCreateTag();
        ListTag photos = phoneTag.contains(PHOTOS_TAG, Tag.TAG_LIST)
                ? phoneTag.getList(PHOTOS_TAG, Tag.TAG_COMPOUND)
                : new ListTag();

        if (photos.size() >= MAX_PHOTOS) {
            return false;
        }

        CompoundTag photoTag = new CompoundTag();
        photoTag.putString(PHOTO_FILE_TAG, photoFileName);
        photoTag.putLong(PHOTO_TIME_TAG, System.currentTimeMillis());

        if (!captureStack.isEmpty()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(captureStack.getItem());
            photoTag.putString(PHOTO_ITEM_TAG, itemId.toString());
            photoTag.putInt(PHOTO_COUNT_TAG, Math.max(1, captureStack.getCount()));

            if (captureStack.hasCustomHoverName()) {
                photoTag.putString(PHOTO_NAME_TAG, captureStack.getHoverName().getString());
            }
        }

        photos.add(photoTag);
        phoneTag.put(PHOTOS_TAG, photos);
        return true;
    }

    boolean removePhotoFromStack(ItemStack phoneStack, int viewerIndex, String expectedFileName) {
        if (phoneStack.isEmpty() || !phoneStack.hasTag()) {
            return false;
        }

        CompoundTag phoneTag = phoneStack.getTag();
        if (phoneTag == null || !phoneTag.contains(PHOTOS_TAG, Tag.TAG_LIST)) {
            return false;
        }

        ListTag photosTag = phoneTag.getList(PHOTOS_TAG, Tag.TAG_COMPOUND);
        if (photosTag.isEmpty()) {
            return false;
        }

        int nbtIndex = photosTag.size() - 1 - viewerIndex;
        if (nbtIndex < 0 || nbtIndex >= photosTag.size()) {
            return false;
        }

        CompoundTag removedPhoto = photosTag.getCompound(nbtIndex);
        String removedFileName = removedPhoto.getString(PHOTO_FILE_TAG);
        photosTag.remove(nbtIndex);

        if (photosTag.isEmpty()) {
            phoneTag.remove(PHOTOS_TAG);
        } else {
            phoneTag.put(PHOTOS_TAG, photosTag);
        }

        String fileNameToDelete = removedFileName.isEmpty() ? expectedFileName : removedFileName;
        unloadTexture(fileNameToDelete);
        deletePhotoFile(fileNameToDelete);
        return true;
    }

    List<PhotoEntry> getPhotos(ItemStack phoneStack) {
        if (phoneStack.isEmpty() || !phoneStack.hasTag()) {
            return List.of();
        }

        CompoundTag phoneTag = phoneStack.getTag();
        if (phoneTag == null || !phoneTag.contains(PHOTOS_TAG, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag photosTag = phoneTag.getList(PHOTOS_TAG, Tag.TAG_COMPOUND);
        if (photosTag.isEmpty()) {
            return List.of();
        }

        List<PhotoEntry> photos = new ArrayList<>(photosTag.size());
        for (int index = photosTag.size() - 1; index >= 0; index--) {
            CompoundTag photoTag = photosTag.getCompound(index);
            String fileName = photoTag.contains(PHOTO_FILE_TAG, Tag.TAG_STRING)
                    ? photoTag.getString(PHOTO_FILE_TAG)
                    : "";
            photos.add(new PhotoEntry(fileName, readPhotoItemStack(photoTag)));
        }

        return photos;
    }

    PhotoTexture getOrLoadPhotoTexture(String fileName, boolean previewMode) {
        if (fileName.isEmpty()) {
            return null;
        }

        Map<String, PhotoTexture> textureCache = previewMode ? previewPhotoTextures : fullPhotoTextures;
        PhotoTexture cached = textureCache.get(fileName);
        if (cached != null) {
            return cached;
        }

        Path filePath = getPhotoDirectory().resolve(fileName);
        if (!Files.exists(filePath)) {
            return null;
        }

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            NativeImage sourceImage = NativeImage.read(inputStream);
            DynamicTexture texture;
            int textureWidth;
            int textureHeight;

            if (previewMode) {
                NativeImage previewImage = resizeToSquare(sourceImage, PHOTO_PREVIEW_SIZE);
                sourceImage.close();
                texture = new DynamicTexture(previewImage);
                textureWidth = PHOTO_PREVIEW_SIZE;
                textureHeight = PHOTO_PREVIEW_SIZE;
            } else {
                textureWidth = sourceImage.getWidth();
                textureHeight = sourceImage.getHeight();
                texture = new DynamicTexture(sourceImage);
            }

            ResourceLocation textureId = new ResourceLocation(
                    modId,
                    (previewMode ? "phone_photo/preview_" : "phone_photo/full_")
                            + Integer.toUnsignedString(fileName.hashCode())
            );
            Minecraft.getInstance().getTextureManager().register(textureId, texture);

            PhotoTexture photoTexture = new PhotoTexture(textureId, textureWidth, textureHeight);
            textureCache.put(fileName, photoTexture);
            return photoTexture;
        } catch (IOException exception) {
            Minedevice.LOGGER.debug("Failed to load phone photo texture: {}", fileName, exception);
            return null;
        }
    }

    void releaseTextures() {
        releaseTextureMap(previewPhotoTextures);
        releaseTextureMap(fullPhotoTextures);
    }

    void unloadTexture(String fileName) {
        if (fileName.isEmpty()) {
            return;
        }

        releaseTexture(fileName, previewPhotoTextures);
        releaseTexture(fileName, fullPhotoTextures);
    }

    void deletePhotoFile(String fileName) {
        if (fileName.isEmpty()) {
            return;
        }

        try {
            Files.deleteIfExists(getPhotoDirectory().resolve(fileName));
        } catch (IOException ignored) {
            // If cleanup fails we just leave the file in screenshots folder.
        }
    }

    private ItemStack readPhotoItemStack(CompoundTag photoTag) {
        String itemIdRaw = photoTag.getString(PHOTO_ITEM_TAG);
        if (itemIdRaw.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ResourceLocation itemId = ResourceLocation.tryParse(itemIdRaw);
        if (itemId == null) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        int count = Math.max(1, photoTag.getInt(PHOTO_COUNT_TAG));
        ItemStack stack = new ItemStack(item, Math.min(count, item.getMaxStackSize()));
        if (photoTag.contains(PHOTO_NAME_TAG, Tag.TAG_STRING)) {
            stack.setHoverName(Component.literal(photoTag.getString(PHOTO_NAME_TAG)));
        }
        return stack;
    }

    private NativeImage resizeToSquare(NativeImage sourceImage, int size) {
        NativeImage resized = new NativeImage(size, size, false);
        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();
        int cropSize = Math.min(sourceWidth, sourceHeight);
        int cropX = (sourceWidth - cropSize) / 2;
        int cropY = (sourceHeight - cropSize) / 2;

        for (int y = 0; y < size; y++) {
            int sourceY = cropY + Math.min(cropSize - 1, (int) ((long) y * cropSize / size));
            for (int x = 0; x < size; x++) {
                int sourceX = cropX + Math.min(cropSize - 1, (int) ((long) x * cropSize / size));
                resized.setPixelRGBA(x, y, sourceImage.getPixelRGBA(sourceX, sourceY));
            }
        }
        return resized;
    }

    private void releaseTextureMap(Map<String, PhotoTexture> textureMap) {
        Minecraft minecraft = Minecraft.getInstance();
        for (PhotoTexture photoTexture : textureMap.values()) {
            minecraft.getTextureManager().release(photoTexture.textureId);
        }
        textureMap.clear();
    }

    private void releaseTexture(String fileName, Map<String, PhotoTexture> textureMap) {
        PhotoTexture removed = textureMap.remove(fileName);
        if (removed != null) {
            Minecraft.getInstance().getTextureManager().release(removed.textureId);
        }
    }

    private Path getPhotoDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("screenshots")
                .resolve(PHOTO_DIRECTORY_NAME);
    }
}
