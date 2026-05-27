package com.sammy.minedevice.forge.client;

import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.ModParticles;
import com.sammy.minedevice.client.MinedeviceClient;
import com.sammy.minedevice.client.particle.MegaphoneWaveParticle;
import com.sammy.minedevice.item.MegaphoneItem;
import com.sammy.minedevice.item.PhoneItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class MinedeviceForgeClient {
    private MinedeviceForgeClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MinedeviceForgeClient::onClientSetup);
        modEventBus.addListener(MinedeviceForgeClient::onRegisterItemColors);
        modEventBus.addListener(MinedeviceForgeClient::onRegisterParticleProviders);
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(MinedeviceClient::init);
    }

    private static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.MEGAPHONE_WAVE.get(), MegaphoneWaveParticle.Provider::new);
    }

    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) ->
                        tintIndex == 0 && stack.getItem() instanceof MegaphoneItem megaphoneItem
                                ? megaphoneItem.getColor(stack)
                                : -1,
                ModItems.MEGAPHONE.get());
        event.register((stack, tintIndex) ->
                        tintIndex == 0 && stack.getItem() instanceof PhoneItem phoneItem
                                ? phoneItem.getColor(stack)
                                : -1,
                ModItems.PHONE.get());
    }
}
