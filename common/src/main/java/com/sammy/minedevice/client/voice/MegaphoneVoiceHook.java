package com.sammy.minedevice.client.voice;

import com.sammy.minedevice.MegaphoneMod;
import com.sammy.minedevice.client.voice.filter.MegaphoneFilter;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.audio.device.InputDevice;
import su.plo.voice.api.client.audio.filter.AudioFilter;
import su.plo.voice.api.client.audio.source.LoopbackSource;
import su.plo.voice.api.client.event.audio.capture.AudioCaptureProcessedEvent;
import su.plo.voice.api.event.EventSubscribe;

import java.util.Optional;

public final class MegaphoneVoiceHook {
    private static final MegaphoneFilter FILTER = new MegaphoneFilter();

    private static MegaphoneAddon addon;
    private static PlasmoVoiceClient voiceClient;
    private static InputDevice attachedDevice;
    private static boolean addonLoaded;

    private MegaphoneVoiceHook() {
    }

    public static void init() {
        if (addonLoaded) {
            return;
        }

        try {
            addon = new MegaphoneAddon();
            PlasmoVoiceClient.getAddonsLoader().load(addon);
            addonLoaded = true;
        } catch (Throwable throwable) {
            addon = null;
            addonLoaded = false;
            MegaphoneMod.LOGGER.warn("Unable to initialize Plasmo Voice megaphone addon", throwable);
        }
    }

    public static void tick(boolean shouldEnable) {
        if (!addonLoaded || voiceClient == null) {
            setRuntimeActive(false);
            detach();
            return;
        }

        Optional<InputDevice> deviceOptional;
        try {
            deviceOptional = voiceClient.getDeviceManager().getInputDevice();
        } catch (Throwable throwable) {
            MegaphoneMod.LOGGER.debug("Unable to access Plasmo Voice input device", throwable);
            setRuntimeActive(false);
            detach();
            return;
        }

        if (deviceOptional.isEmpty()) {
            setRuntimeActive(false);
            detach();
            return;
        }

        InputDevice currentDevice = deviceOptional.get();
        if (shouldEnable) {
            setRuntimeActive(attach(currentDevice));
        } else {
            setRuntimeActive(false);
            detach();
        }
    }

    public static void detach() {
        setRuntimeActive(false);

        if (attachedDevice == null) {
            return;
        }

        try {
            attachedDevice.removeFilter(FILTER);
        } catch (Throwable throwable) {
            MegaphoneMod.LOGGER.debug("Failed to remove megaphone filter", throwable);
        } finally {
            attachedDevice = null;
        }
    }

    public static void shutdown() {
        setRuntimeActive(false);
        detach();
        voiceClient = null;

        if (!addonLoaded || addon == null) {
            return;
        }

        try {
            PlasmoVoiceClient.getAddonsLoader().unload(addon);
        } catch (Throwable throwable) {
            MegaphoneMod.LOGGER.debug("Failed to unload Plasmo Voice megaphone addon", throwable);
        } finally {
            addonLoaded = false;
            addon = null;
        }
    }

    private static boolean attach(InputDevice device) {
        if (attachedDevice == device) {
            return ensureFilterPresent(device);
        }

        detach();
        if (!ensureFilterPresent(device)) {
            return false;
        }

        attachedDevice = device;
        return true;
    }

    private static boolean ensureFilterPresent(InputDevice device) {
        try {
            if (!device.getFilters().contains(FILTER)) {
                device.addFilter(FILTER, AudioFilter.Priority.HIGH);
            }
            return device.getFilters().contains(FILTER);
        } catch (Throwable throwable) {
            MegaphoneMod.LOGGER.debug("Failed to attach megaphone filter", throwable);
            return false;
        }
    }

    private static void setRuntimeActive(boolean active) {
        if (addon != null) {
            addon.setRuntimeActive(active);
        }
    }

    @Addon(
            id = "minedevice_megaphone",
            name = "MineDevice Megaphone",
            scope = AddonLoaderScope.CLIENT,
            version = "1.0.0",
            authors = {"Q Team Studio"}
    )
    private static final class MegaphoneAddon implements AddonInitializer {
        @InjectPlasmoVoice
        private PlasmoVoiceClient client;

        private boolean runtimeActive;
        private LoopbackSource loopbackSource;

        @Override
        public void onAddonInitialize() {
            voiceClient = client;
            client.getAddonConfig(this);
        }

        @Override
        public void onAddonShutdown() {
            setRuntimeActive(false);
            closeLoopbackSource();
            detach();
            voiceClient = null;
        }

        @EventSubscribe
        public void onAudioCaptureProcessed(AudioCaptureProcessedEvent event) {
            if (!runtimeActive || client == null) {
                return;
            }

            LoopbackSource source = ensureLoopbackSource();
            if (source == null || source.isClosed()) {
                return;
            }

            source.write(event.getProcessed().getMono());
        }

        public synchronized void setRuntimeActive(boolean active) {
            if (runtimeActive == active) {
                return;
            }

            runtimeActive = active;
            if (!runtimeActive) {
                closeLoopbackSource();
            }
        }

        private synchronized LoopbackSource ensureLoopbackSource() {
            if (loopbackSource != null && !loopbackSource.isClosed()) {
                return loopbackSource;
            }

            try {
                LoopbackSource source = client.getSourceManager().createLoopbackSource(true);
                source.initialize(false);
                loopbackSource = source;
                return loopbackSource;
            } catch (Throwable throwable) {
                closeLoopbackSource();
                MegaphoneMod.LOGGER.debug("Failed to initialize megaphone loopback source", throwable);
                return null;
            }
        }

        private synchronized void closeLoopbackSource() {
            if (loopbackSource == null) {
                return;
            }

            loopbackSource.close();
            loopbackSource = null;
        }
    }
}
