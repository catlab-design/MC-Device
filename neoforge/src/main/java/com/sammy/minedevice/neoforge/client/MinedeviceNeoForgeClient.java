package com.sammy.minedevice.neoforge.client;

import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.ModParticles;
import com.sammy.minedevice.client.MinedeviceClient;
import com.sammy.minedevice.client.particle.MegaphoneWaveParticle;
import com.sammy.minedevice.item.CardItem;
import com.sammy.minedevice.item.MegaphoneItem;
import com.sammy.minedevice.item.PhoneItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public final class MinedeviceNeoForgeClient {
    private MinedeviceNeoForgeClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MinedeviceNeoForgeClient::onClientSetup);
        modEventBus.addListener(MinedeviceNeoForgeClient::onRegisterItemColors);
        modEventBus.addListener(MinedeviceNeoForgeClient::onRegisterParticleProviders);
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
        event.register((stack, tintIndex) ->
                        tintIndex == 0 && stack.getItem() instanceof CardItem cardItem
                                ? cardItem.getColor(stack)
                                : -1,
                ModItems.CARD.get());
    }
}
