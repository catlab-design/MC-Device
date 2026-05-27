package com.sammy.minedevice.item;

import com.sammy.minedevice.Minedevice;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

public final class PhoneItem extends Item implements DyeableLeatherItem {
    public static final int DEFAULT_COLOR = 0x222323;

    public PhoneItem(Properties properties) {
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (level.isClientSide) {
            openPhoneScreenClient(hand);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }

    private static void openPhoneScreenClient(InteractionHand hand) {
        try {
            Class<?> hookClass = Class.forName("com.sammy.minedevice.client.phone.PhoneClientHooks");
            Method openMethod = hookClass.getDeclaredMethod("openScreen", InteractionHand.class);
            openMethod.invoke(null, hand);
        } catch (ReflectiveOperationException exception) {
            Minedevice.LOGGER.debug("Failed to open phone screen", exception);
        }
    }
}
