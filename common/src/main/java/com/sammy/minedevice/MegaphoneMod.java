package com.sammy.minedevice;

import com.mojang.logging.LogUtils;
import com.sammy.minedevice.phone.PhoneNetworking;
import dev.architectury.platform.Platform;
import org.slf4j.Logger;

public final class MegaphoneMod {
    public static final String MOD_ID = "minedevice";
    public static final Logger LOGGER = LogUtils.getLogger();
    private MegaphoneMod() {
    }
    public static void init() {
        ModSounds.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModItems.init();
        PhoneNetworking.init();
        initOptionalVoiceHooks();
    }

    private static void initOptionalVoiceHooks() {
        if (!Platform.isModLoaded("plasmovoice")) {
            return;
        }

        try {
            Class.forName("com.sammy.minedevice.homephone.voice.HomePhoneVoiceHook")
                    .getMethod("init")
                    .invoke(null);
        } catch (Throwable throwable) {
            LOGGER.warn("Unable to initialize Plasmo Voice home phone hook", throwable);
        }
    }
}
