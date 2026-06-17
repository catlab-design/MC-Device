package com.sammy.minedevice.item;

import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CardItem extends Item implements DyeableLeatherItem {
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;

    public CardItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getColor(ItemStack stack) {
        if (stack != null && stack.hasTag()) {
            var displayTag = stack.getTagElement("display");
            if (displayTag != null && displayTag.contains("color", 99)) {
                return displayTag.getInt("color");
            }
        }
        return DEFAULT_COLOR;
    }
}
