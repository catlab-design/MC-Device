package com.sammy.minedevice;

import com.sammy.minedevice.atm.AtmMenu;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Minedevice.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<AtmMenu>> ATM =
            MENUS.register("atm", () -> new MenuType<>(AtmMenu::fromNetwork, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {
    }

    public static void init() {
        MENUS.register();
    }
}
