package com.sammy.minedevice.fabric;

import com.sammy.minedevice.MegaphoneMod;
import net.fabricmc.api.ModInitializer;

public final class MinedeviceFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MegaphoneMod.init();
    }
}
