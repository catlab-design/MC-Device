package com.sammy.minedevice.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class CardItem extends Item implements DyeableLeatherItem {
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private static final String CARD_ID_TAG = "card_id";
    private static final String CARD_NUMBER_TAG = "card_number";

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

    public static boolean hasCardUUID(ItemStack stack) {
        return stack != null && stack.hasTag() && stack.getTag().hasUUID(CARD_ID_TAG);
    }

    public static UUID getCardUUID(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        return tag.hasUUID(CARD_ID_TAG) ? tag.getUUID(CARD_ID_TAG) : null;
    }

    public static UUID getOrCreateCardUUID(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.hasUUID(CARD_ID_TAG)) {
            return tag.getUUID(CARD_ID_TAG);
        }
        UUID id = UUID.randomUUID();
        tag.putUUID(CARD_ID_TAG, id);
        String number = generateCardNumber(id);
        tag.putString(CARD_NUMBER_TAG, number);
        stack.setTag(tag);
        return id;
    }

    public static String getCardNumber(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return "";
        }
        CompoundTag tag = stack.getTag();
        return tag.contains(CARD_NUMBER_TAG, 8) ? tag.getString(CARD_NUMBER_TAG) : "";
    }

    public static String generateCardNumber(UUID cardId) {
        if (cardId == null) {
            return "0000000000000000";
        }
        long least = Math.abs(cardId.getLeastSignificantBits());
        long most = Math.abs(cardId.getMostSignificantBits());
        long upper = least % 100000000L;
        long lower = most % 100000000L;
        long combined = upper * 100000000L + lower;
        return String.format("%016d", combined);
    }
}
