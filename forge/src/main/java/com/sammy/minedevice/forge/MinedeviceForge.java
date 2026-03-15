package com.sammy.minedevice.forge;

import com.sammy.minedevice.MegaphoneMod;
import com.sammy.minedevice.forge.client.MinedeviceForgeClient;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;

@Mod(MegaphoneMod.MOD_ID)
public final class MinedeviceForge {
    public MinedeviceForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(MegaphoneMod.MOD_ID, modEventBus);
        MegaphoneMod.init();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> MinedeviceForgeClient::register);
    }
}
