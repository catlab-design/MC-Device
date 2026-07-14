package com.sammy.minedevice.client.phone;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;

/**
 * Native (OS) file dialogs for importing/exporting phone images, backed by
 * LWJGL's TinyFileDialogs. Must be called on the render thread.
 */
final class PhoneImageIo {
    private PhoneImageIo() {
    }

    /** Opens a native "open file" dialog for a single image and returns its path, or null if cancelled. */
    static Path openImportDialog() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(4);
            filters.put(stack.UTF8("*.png"));
            filters.put(stack.UTF8("*.jpg"));
            filters.put(stack.UTF8("*.jpeg"));
            filters.put(stack.UTF8("*.bmp"));
            filters.flip();

            String result = TinyFileDialogs.tinyfd_openFileDialog(
                    "Select an image", "", filters, "Image files", false);
            return result == null || result.isBlank() ? null : Path.of(result.trim());
        } catch (Throwable throwable) {
            return null;
        }
    }

    /** Opens a native "save file" dialog and returns the chosen path, or null if cancelled. */
    static Path exportDialog(String defaultName) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();

            String suggested = (defaultName == null || defaultName.isBlank()) ? "photo.png" : defaultName;
            String result = TinyFileDialogs.tinyfd_saveFileDialog(
                    "Save photo", suggested, filters, "PNG image");
            if (result == null || result.isBlank()) {
                return null;
            }
            String trimmed = result.trim();
            if (!trimmed.toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
                trimmed = trimmed + ".png";
            }
            return Path.of(trimmed);
        } catch (Throwable throwable) {
            return null;
        }
    }

    /** True when the path looks like an image file we can import. */
    static boolean isSupportedImage(Path path) {
        if (path == null) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".bmp");
    }
}
