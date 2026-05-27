package com.sammy.minedevice;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final float HOME_PHONE_RING_RANGE = 32.0F;
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Minedevice.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> HOME_PHONE_RING = SOUNDS.register("home_phone_ring",
            () -> SoundEvent.createFixedRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(Minedevice.MOD_ID, "home_phone_ring"),
                    HOME_PHONE_RING_RANGE));
    public static final RegistrySupplier<SoundEvent> PHONE_RING = SOUNDS.register("phone_ring",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Minedevice.MOD_ID, "phone_ring")));
    public static final RegistrySupplier<SoundEvent> PHONE_DIALING = SOUNDS.register("phone_dialing",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Minedevice.MOD_ID, "phone_dialing")));
    public static final RegistrySupplier<SoundEvent> AIRSTRIKE_TARGET_LOCKED_EN_US = SOUNDS.register("airstrike_target_locked_en_us",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Minedevice.MOD_ID, "airstrike_target_locked_en_us")));
    public static final RegistrySupplier<SoundEvent> AIRSTRIKE_TARGET_LOCKED_TH_TH = SOUNDS.register("airstrike_target_locked_th_th",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Minedevice.MOD_ID, "airstrike_target_locked_th_th")));
    public static final RegistrySupplier<SoundEvent> WALKIE_ON_YOU = SOUNDS.register("walkie_on_you",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Minedevice.MOD_ID, "walkie_on_you")));
    public static final RegistrySupplier<SoundEvent> WALKIE_CLOSE = SOUNDS.register("walkie_close",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Minedevice.MOD_ID, "walkie_close")));
    public static final RegistrySupplier<SoundEvent> WALKIE_TALK = SOUNDS.register("walkie_talk",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Minedevice.MOD_ID, "walkie_talk")));

    private ModSounds() {
    }

    public static void init() {
        SOUNDS.register();
    }
}
