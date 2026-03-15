package com.sammy.minedevice;

import com.sammy.minedevice.item.MegaphoneItem;
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
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MegaphoneMod.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> PHONE = ITEMS.register("phone",
            () -> new PhoneItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> HOME_PHONE = ITEMS.register("home_phone",
            () -> new BlockItem(ModBlocks.HOME_PHONE.get(), new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> HOME_PHONE_HANDSET = ITEMS.register("home_phone_handset",
            () -> new HomePhoneHandsetItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> MEGAPHONE = ITEMS.register("megaphone",
            () -> new MegaphoneItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> MEGAPHONE_RED = ITEMS.register("megaphone_red",
            () -> new MegaphoneItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> MEGAPHONE_YELLOW = ITEMS.register("megaphone_yellow",
            () -> new MegaphoneItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> MEGAPHONE_LIGHT_BLUE = ITEMS.register("megaphone_light_blue",
            () -> new MegaphoneItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void init() {
        ITEMS.register();
        CreativeTabRegistry.append(CreativeModeTabs.FUNCTIONAL_BLOCKS, HOME_PHONE);
        CreativeTabRegistry.append(CreativeModeTabs.TOOLS_AND_UTILITIES, PHONE);
        CreativeTabRegistry.append(CreativeModeTabs.TOOLS_AND_UTILITIES, MEGAPHONE);
        CreativeTabRegistry.append(CreativeModeTabs.TOOLS_AND_UTILITIES, MEGAPHONE_RED);
        CreativeTabRegistry.append(CreativeModeTabs.TOOLS_AND_UTILITIES, MEGAPHONE_YELLOW);
        CreativeTabRegistry.append(CreativeModeTabs.TOOLS_AND_UTILITIES, MEGAPHONE_LIGHT_BLUE);
    }
}
