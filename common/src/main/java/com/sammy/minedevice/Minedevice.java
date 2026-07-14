package com.sammy.minedevice;

import com.mojang.logging.LogUtils;
import com.sammy.minedevice.airstrike.AirstrikeManager;
import com.sammy.minedevice.atm.AtmNetworking;
import com.sammy.minedevice.phone.CallLogStorageManager;
import com.sammy.minedevice.phone.ChatStorageManager;
import com.sammy.minedevice.phone.PhoneNetworking;
import com.sammy.minedevice.walkie.WalkieNetworking;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;

public final class Minedevice {
    public static final String MOD_ID = "minedevice";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean chatLifecycleRegistered;
    private static Method megaphoneSpeakingMethod;

    private Minedevice() {
    }

    public static void init() {
        ModSounds.init();
        ModParticles.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModItems.init();
        ModMenus.init();
        ModCreativeTabs.init();
        PhoneNetworking.init();
        WalkieNetworking.init();
        AtmNetworking.init();
        AirstrikeManager.init();
        com.sammy.minedevice.command.MinedeviceCommands.init();

        registerChatStorageLifecycle();

        initOptionalVoiceHooks();
    }

    private static void registerChatStorageLifecycle() {
        if (chatLifecycleRegistered) {
            return;
        }

        chatLifecycleRegistered = true;
        LifecycleEvent.SERVER_STARTED.register(server -> initChatStorage());
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            ChatStorageManager.shutdown();
            CallLogStorageManager.shutdown();
        });
    }

    private static void initChatStorage() {
        if (ChatStorageManager.isAvailable()) {
            return;
        }

        try {
            String gameDir = System.getProperty("user.dir");
            String dbPath = new File(gameDir, "minedevice_chat.db").getAbsolutePath();

            ChatStorageManager.init(dbPath);
            ChatStorageManager.getInstance().start();

            String callLogDbPath = new File(gameDir, "minedevice_call_log.db").getAbsolutePath();
            CallLogStorageManager.init(callLogDbPath);

            LOGGER.info("Chat storage system initialized with SQLite database");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize chat storage system", e);
        }
    }

    private static void initOptionalVoiceHooks() {
        com.sammy.minedevice.voice.VoiceProvider provider = com.sammy.minedevice.voice.VoiceProviderDetector.detect();

        if (provider == com.sammy.minedevice.voice.VoiceProvider.PLASMO_VOICE) {
            try {
                Class.forName("com.sammy.minedevice.homephone.voice.HomePhoneVoiceHook")
                        .getMethod("init")
                        .invoke(null);
            } catch (Throwable throwable) {
                LOGGER.warn("Unable to initialize Plasmo Voice home phone hook", throwable);
            }

            try {
                Class<?> megaphoneHookClass = Class.forName("com.sammy.minedevice.megaphone.voice.MegaphoneVoiceHook");
                megaphoneHookClass.getMethod("init").invoke(null);
                megaphoneSpeakingMethod = megaphoneHookClass.getMethod("isSpeaking", UUID.class);
            } catch (Throwable throwable) {
                megaphoneSpeakingMethod = null;
                LOGGER.warn("Unable to initialize Plasmo Voice megaphone hook", throwable);
            }

            try {
                Class.forName("com.sammy.minedevice.walkie.voice.WalkieVoiceHook")
                        .getMethod("init")
                        .invoke(null);
            } catch (Throwable throwable) {
                LOGGER.warn("Unable to initialize Plasmo Voice walkie hook", throwable);
            }
            LOGGER.info("Plasmo Voice server hooks initialized.");
        } else if (provider == com.sammy.minedevice.voice.VoiceProvider.SIMPLE_VOICE_CHAT) {
            LOGGER.info("Simple Voice Chat server hooks will be initialized via the plugin lifecycle.");
        } else {
            LOGGER.info("No compatible voice mod found. Voice features will be disabled.");
        }
    }

    public static boolean isMegaphoneSpeaking(ServerPlayer player) {
        if (player == null) {
            return false;
        }

        com.sammy.minedevice.voice.VoiceProvider provider = com.sammy.minedevice.voice.VoiceProviderDetector.detect();
        if (provider == com.sammy.minedevice.voice.VoiceProvider.SIMPLE_VOICE_CHAT) {
            return com.sammy.minedevice.voice.svc.SvcMegaphoneVoiceHook.isSpeaking(player.getUUID());
        } else if (provider == com.sammy.minedevice.voice.VoiceProvider.PLASMO_VOICE) {
            if (megaphoneSpeakingMethod == null) {
                return false;
            }
            try {
                Object result = megaphoneSpeakingMethod.invoke(null, player.getUUID());
                return result instanceof Boolean speaking && speaking;
            } catch (Throwable throwable) {
                LOGGER.debug("Unable to query megaphone speaking state", throwable);
                return false;
            }
        }
        
        return false;
    }
}
