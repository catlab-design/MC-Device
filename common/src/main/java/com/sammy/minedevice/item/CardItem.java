package com.sammy.minedevice.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public final class CardItem extends Item {
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;

    public CardItem(Properties properties) {
        super(properties);
    }

    public int getColor(ItemStack stack) {
        return stack == null ? DEFAULT_COLOR : DyedItemColor.getOrDefault(stack, DEFAULT_COLOR);
    }
}
