package com.sammy.minedevice;

import com.sammy.minedevice.item.AirstrikeRadioItem;
import com.sammy.minedevice.item.HomePhoneHandsetItem;
import com.sammy.minedevice.item.MegaphoneItem;
import com.sammy.minedevice.item.PhoneItem;
import com.sammy.minedevice.item.WalkieRadioItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Minedevice.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> PHONE = ITEMS.register("phone",
            () -> new PhoneItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> HOME_PHONE = ITEMS.register("home_phone",
            () -> new BlockItem(ModBlocks.HOME_PHONE.get(), new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> HOME_PHONE_HANDSET = ITEMS.register("home_phone_handset",
            () -> new HomePhoneHandsetItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> MEGAPHONE = ITEMS.register("megaphone",
            () -> new MegaphoneItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> AIRSTRIKE_RADIO = ITEMS.register("airstrike_radio",
            () -> new AirstrikeRadioItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> WALKIE = ITEMS.register("walkie",
            () -> new WalkieRadioItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> BILL20 = ITEMS.register("bill20",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BILL100 = ITEMS.register("bill100",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BILL500 = ITEMS.register("bill500",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BILL1000 = ITEMS.register("bill1000",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ATM = ITEMS.register("atm",
            () -> new BlockItem(ModBlocks.ATM.get(), new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void init() {
        ITEMS.register();
    }
}
