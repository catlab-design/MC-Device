package com.sammy.minedevice;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.Locale;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Minedevice.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeTabRegistry.create(Component.translatable("itemGroup.minedevice.main"),
                    () -> new ItemStack(ModItems.PHONE.get())));

    private ModCreativeTabs() {
    }

    @SuppressWarnings("unchecked")
    public static void init() {
        CREATIVE_MODE_TABS.register();
        CreativeTabRegistry.append(MAIN, ModItems.HOME_PHONE);
        CreativeTabRegistry.append(MAIN, ModItems.PHONE);
        CreativeTabRegistry.append(MAIN, ModItems.MEGAPHONE);
        for (DyeColor color : DyeColor.values()) {
            CreativeTabRegistry.appendStack(CreativeModeTabs.SEARCH, () -> dyedMegaphone(color));
        }
        CreativeTabRegistry.append(MAIN, ModItems.AIRSTRIKE_RADIO);
        CreativeTabRegistry.append(MAIN, ModItems.WALKIE);
        CreativeTabRegistry.append(MAIN, ModItems.BILL20);
        CreativeTabRegistry.append(MAIN, ModItems.BILL100);
        CreativeTabRegistry.append(MAIN, ModItems.BILL500);
        CreativeTabRegistry.append(MAIN, ModItems.BILL1000);
        CreativeTabRegistry.append(MAIN, ModItems.COIN);
        CreativeTabRegistry.append(MAIN, ModItems.CARD);
        CreativeTabRegistry.append(MAIN, ModItems.ATM);
        CreativeTabRegistry.append(MAIN, ModItems.LABTOP);
    }

    private static ItemStack dyedMegaphone(DyeColor color) {
        ItemStack stack = new ItemStack(ModItems.MEGAPHONE.get());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(dyeColorToRgb(color), false));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(formatColorName(color) + " Megaphone"));
        return stack;
    }

    private static String formatColorName(DyeColor color) {
        String[] parts = color.getName().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }

    private static int dyeColorToRgb(DyeColor color) {
        return color.getTextureDiffuseColor();
    }
}
