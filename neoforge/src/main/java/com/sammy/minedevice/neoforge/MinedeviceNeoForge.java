package com.sammy.minedevice.neoforge;

import com.sammy.minedevice.Minedevice;
import com.sammy.minedevice.neoforge.client.MinedeviceNeoForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Minedevice.MOD_ID)
public final class MinedeviceNeoForge {
    public MinedeviceNeoForge(FMLModContainer container) {
        IEventBus modEventBus = container.getEventBus();
        Minedevice.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinedeviceNeoForgeClient.register(modEventBus);
        }
    }
}
