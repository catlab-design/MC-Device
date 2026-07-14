package com.sammy.minedevice.phone;

import com.sammy.minedevice.Minedevice;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side storage for phone photos so that a picture taken on one phone is
 * visible to any player who later holds that phone. Image bytes live in the world
 * save folder (never in item NBT), keyed by the photo's file name; item NBT only
 * references the file name. Uploads/downloads are chunked over the network and
 * reassembled here.
 */
public final class PhonePhotoServerStore {
    private static final String PHOTO_DIRECTORY = "minedevice_photos";

    // Reassembly buffers for in-flight uploads, keyed by "<playerUuid>:<fileName>".
    private static final Map<String, UploadBuffer> UPLOADS = new HashMap<>();

    private PhonePhotoServerStore() {
    }

    /** True if the name is a safe, flat photo file we are willing to read/write. */
    public static boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.length() > PhonePhotoData.MAX_PHOTO_FILE_NAME_LENGTH) {
            return false;
        }
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            return false;
        }
        for (int i = 0; i < fileName.length(); i++) {
            char c = fileName.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.';
            if (!ok) {
                return false;
            }
        }
        return fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".png");
    }

    private static Path directory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(PHOTO_DIRECTORY);
    }

    public static boolean exists(MinecraftServer server, String fileName) {
        if (server == null || !isValidFileName(fileName)) {
            return false;
        }
        return Files.exists(directory(server).resolve(fileName));
    }

    /** Reads a stored photo's raw bytes, or null if absent/invalid. */
    public static byte[] read(MinecraftServer server, String fileName) {
        if (server == null || !isValidFileName(fileName)) {
            return null;
        }
        Path path = directory(server).resolve(fileName);
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return Files.readAllBytes(path);
        } catch (Exception exception) {
            Minedevice.LOGGER.debug("Failed to read server photo {}", fileName, exception);
            return null;
        }
    }

    private static void write(MinecraftServer server, String fileName, byte[] bytes) {
        if (server == null || !isValidFileName(fileName) || bytes == null || bytes.length == 0) {
            return;
        }
        try {
            Path dir = directory(server);
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), bytes);
        } catch (Exception exception) {
            Minedevice.LOGGER.debug("Failed to write server photo {}", fileName, exception);
        }
    }

    public static void delete(MinecraftServer server, String fileName) {
        if (server == null || !isValidFileName(fileName)) {
            return;
        }
        try {
            Files.deleteIfExists(directory(server).resolve(fileName));
        } catch (Exception ignored) {
        }
    }

    /**
     * Accepts one chunk of an upload. When the final chunk arrives the whole image
     * is written to disk. Returns true once the file is complete.
     */
    public static boolean acceptUploadChunk(MinecraftServer server, UUID uploader, String fileName,
                                            int totalChunks, int chunkIndex, byte[] chunk) {
        if (server == null || uploader == null || !isValidFileName(fileName)
                || totalChunks <= 0 || chunkIndex < 0 || chunkIndex >= totalChunks || chunk == null) {
            return false;
        }

        String key = uploader + ":" + fileName;
        UploadBuffer buffer = UPLOADS.computeIfAbsent(key, k -> new UploadBuffer(totalChunks));
        if (buffer.totalChunks != totalChunks || buffer.nextIndex != chunkIndex) {
            // Out-of-order or mismatched upload — discard and start over.
            UPLOADS.remove(key);
            return false;
        }

        buffer.data.writeBytes(chunk);
        buffer.nextIndex++;
        if (buffer.data.size() > PhoneNetworking.PHOTO_MAX_BYTES) {
            UPLOADS.remove(key);
            return false;
        }

        if (buffer.nextIndex >= totalChunks) {
            UPLOADS.remove(key);
            write(server, fileName, buffer.data.toByteArray());
            return true;
        }
        return false;
    }

    public static void clearUploadsFor(UUID uploader) {
        if (uploader == null) {
            return;
        }
        UPLOADS.keySet().removeIf(key -> key.startsWith(uploader + ":"));
    }

    private static final class UploadBuffer {
        private final int totalChunks;
        private final ByteArrayOutputStream data = new ByteArrayOutputStream();
        private int nextIndex;

        private UploadBuffer(int totalChunks) {
            this.totalChunks = totalChunks;
        }
    }
}
