package com.sammy.minedevice;

import com.sammy.minedevice.item.HomePhoneHandsetItem;
import com.sammy.minedevice.item.PhoneItem;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Minedevice.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> PHONE = ITEMS.register("phone",
            () -> new PhoneItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> HOME_PHONE = ITEMS.register("home_phone",
            () -> new BlockItem(ModBlocks.HOME_PHONE.get(), new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> HOME_PHONE_HANDSET = ITEMS.register("home_phone_handset",
            () -> new HomePhoneHandsetItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void init() {
        ITEMS.register();
        CreativeTabRegistry.append(CreativeModeTabs.FUNCTIONAL_BLOCKS, HOME_PHONE);
        CreativeTabRegistry.append(CreativeModeTabs.TOOLS_AND_UTILITIES, PHONE);
    }
}
