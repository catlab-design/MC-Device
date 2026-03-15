package com.sammy.minedevice.fabric.client;

import com.sammy.minedevice.client.MinedeviceClient;
import net.fabricmc.api.ClientModInitializer;

public final class MinedeviceFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MinedeviceClient.init();
    }
}
