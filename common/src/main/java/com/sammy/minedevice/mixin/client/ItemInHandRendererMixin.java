package com.sammy.minedevice.mixin.client;

import com.sammy.minedevice.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Redirect(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/UseAnim;"))
    private UseAnim minedevice$showCrossbowUseAnimInFirstPerson(ItemStack stack) {
        if (isMegaphone(stack)) {
            return UseAnim.CROSSBOW;
        }

        return stack.getUseAnimation();
    }

    @Redirect(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z"))
    private boolean minedevice$skipCrossbowChargeForMegaphone(AbstractClientPlayer player) {
        if (isMegaphone(player.getUseItem())) {
            return false;
        }

        return player.isUsingItem();
    }

    @Redirect(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/CrossbowItem;isCharged(Lnet/minecraft/world/item/ItemStack;)Z"),
            require = 0)
    private boolean minedevice$showLoadedCrossbowForMegaphone(ItemStack stack) {
        if (!isMegaphone(stack)) {
            return CrossbowItem.isCharged(stack);
        }

        AbstractClientPlayer player = Minecraft.getInstance().player;
        return player != null && player.isUsingItem() && player.getUseItem() == stack;
    }

    @Inject(method = "isChargedCrossbow", at = @At("HEAD"), cancellable = true, require = 0)
    private static void minedevice$considerMegaphoneCharged(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!isMegaphone(stack)) {
            return;
        }

        AbstractClientPlayer player = Minecraft.getInstance().player;
        cir.setReturnValue(player != null && player.isUsingItem() && player.getUseItem() == stack);
    }

    @Redirect(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean minedevice$treatMegaphoneAsCrossbowInFirstPerson(ItemStack stack, Item item) {
        if (item == Items.CROSSBOW && isMegaphone(stack)) {
            return true;
        }

        return stack.is(item);
    }

    @Unique
    private static boolean isMegaphone(ItemStack stack) {
        return stack.is(ModItems.MEGAPHONE.get())
                || stack.is(ModItems.MEGAPHONE_RED.get())
                || stack.is(ModItems.MEGAPHONE_YELLOW.get())
                || stack.is(ModItems.MEGAPHONE_LIGHT_BLUE.get());
    }
}
