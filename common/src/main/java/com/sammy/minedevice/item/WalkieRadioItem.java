package com.sammy.minedevice.item;

import com.sammy.minedevice.Minedevice;
import com.sammy.minedevice.walkie.WalkieBand;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.List;

public final class WalkieRadioItem extends Item {
    private static final String BAND_TAG = "WalkieBand";
    private static final String FREQUENCY_TAG = "WalkieFrequency";
    private static final int USE_DURATION = 72000;

    public WalkieRadioItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.minedevice.walkie.tooltip",
                getBand(stack).display(getFrequency(stack))).withStyle(ChatFormatting.GRAY));
    }

    public static WalkieBand getBand(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return WalkieBand.AM;
        }

        CompoundTag tag = stack.getTag();
        return tag == null ? WalkieBand.AM : WalkieBand.byName(tag.getString(BAND_TAG));
    }

    public static int getFrequency(ItemStack stack) {
        WalkieBand band = getBand(stack);
        if (stack == null || !stack.hasTag()) {
            return band.defaultFrequency();
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(FREQUENCY_TAG)) {
            return band.defaultFrequency();
        }

        return band.clamp(tag.getInt(FREQUENCY_TAG));
    }

    public static void setTuning(ItemStack stack, WalkieBand band, int frequency) {
        if (stack == null || stack.isEmpty() || band == null) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(BAND_TAG, band.name());
        tag.putInt(FREQUENCY_TAG, band.clamp(frequency));
    }

    public static boolean hasSameTuning(ItemStack first, ItemStack second) {
        WalkieBand firstBand = getBand(first);
        return firstBand == getBand(second) && getFrequency(first) == getFrequency(second);
    }

    public static boolean isUsingWalkie(Player player) {
        return player != null
                && player.isUsingItem()
                && player.getUseItem().getItem() instanceof WalkieRadioItem;
    }

    public static void openWalkieScreenClient(InteractionHand hand) {
        try {
            Class<?> hookClass = Class.forName("com.sammy.minedevice.client.walkie.WalkieClientHooks");
            Method openMethod = hookClass.getDeclaredMethod("openScreen", InteractionHand.class);
            openMethod.invoke(null, hand);
        } catch (ReflectiveOperationException exception) {
            Minedevice.LOGGER.debug("Failed to open walkie screen", exception);
        }
    }
}
