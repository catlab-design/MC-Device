package com.sammy.minedevice.fabric.client;

import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.ModParticles;
import com.sammy.minedevice.client.MinedeviceClient;
import com.sammy.minedevice.client.particle.MegaphoneWaveParticle;
import com.sammy.minedevice.item.CardItem;
import com.sammy.minedevice.item.MegaphoneItem;
import com.sammy.minedevice.item.PhoneItem;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.api.ClientModInitializer;

public final class MinedeviceFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MinedeviceClient.init();
        ColorProviderRegistry.ITEM.register((stack, tintIndex) ->
                        tintIndex == 0 && stack.getItem() instanceof MegaphoneItem megaphoneItem
                                ? megaphoneItem.getColor(stack)
                                : -1,
                ModItems.MEGAPHONE.get());
        ColorProviderRegistry.ITEM.register((stack, tintIndex) ->
                        tintIndex == 0 && stack.getItem() instanceof PhoneItem phoneItem
                                ? phoneItem.getColor(stack)
                                : -1,
                ModItems.PHONE.get());
        ColorProviderRegistry.ITEM.register((stack, tintIndex) ->
                        tintIndex == 0 && stack.getItem() instanceof CardItem cardItem
                                ? cardItem.getColor(stack)
                                : -1,
                ModItems.CARD.get());
        ParticleFactoryRegistry.getInstance().register(ModParticles.MEGAPHONE_WAVE.get(), MegaphoneWaveParticle.Provider::new);
    }
}
