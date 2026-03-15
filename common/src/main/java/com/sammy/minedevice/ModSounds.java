package com.sammy.minedevice;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final float HOME_PHONE_RING_RANGE = 32.0F;
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(MegaphoneMod.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> HOME_PHONE_RING = SOUNDS.register("home_phone_ring",
            () -> SoundEvent.createFixedRangeEvent(
                    new ResourceLocation(MegaphoneMod.MOD_ID, "home_phone_ring"),
                    HOME_PHONE_RING_RANGE));
    public static final RegistrySupplier<SoundEvent> PHONE_RING = SOUNDS.register("phone_ring",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MegaphoneMod.MOD_ID, "phone_ring")));
    public static final RegistrySupplier<SoundEvent> PHONE_DIALING = SOUNDS.register("phone_dialing",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MegaphoneMod.MOD_ID, "phone_dialing")));

    private ModSounds() {
    }

    public static void init() {
        SOUNDS.register();
    }
}
