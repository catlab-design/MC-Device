package com.sammy.minedevice.item;

import com.sammy.minedevice.MegaphoneMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

public final class PhoneItem extends Item {
    public PhoneItem(Properties properties) {
        super(properties);
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
            MegaphoneMod.LOGGER.debug("Failed to open phone screen", exception);
        }
    }
}
