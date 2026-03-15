package com.sammy.minedevice.mixin;

import com.sammy.minedevice.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void minedevice$preventDroppingHomePhoneHandset(ItemStack stack, boolean dropAround,
                                                            boolean includeThrowerName,
                                                            CallbackInfoReturnable<ItemEntity> cir) {
        if (stack.is(ModItems.HOME_PHONE_HANDSET.get())) {
            cir.setReturnValue(null);
        }
    }
}
