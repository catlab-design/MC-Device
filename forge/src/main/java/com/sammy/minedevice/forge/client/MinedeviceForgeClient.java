package com.sammy.minedevice.forge.client;

import com.sammy.minedevice.client.MegaphoneClient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class MinedeviceForgeClient {
    private MinedeviceForgeClient() {
    }

    public static void register() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(MinedeviceForgeClient::onClientSetup);
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(MegaphoneClient::init);
    }
}
