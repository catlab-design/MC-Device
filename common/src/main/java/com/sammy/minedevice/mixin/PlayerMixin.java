package com.sammy.minedevice.mixin;

import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.phone.PhoneCallPoseAccess;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin implements PhoneCallPoseAccess {
    @Unique
    private static final EntityDataAccessor<Boolean> MINDEVICE_PHONE_CALL_POSE = SynchedEntityData.defineId(
            Player.class,
            EntityDataSerializers.BOOLEAN
    );
    @Unique
    private static final EntityDataAccessor<Boolean> MINDEVICE_PHONE_CAMERA_POSE = SynchedEntityData.defineId(
            Player.class,
            EntityDataSerializers.BOOLEAN
    );
    @Unique
    private static final EntityDataAccessor<Boolean> MINDEVICE_PHONE_CAMERA_SELFIE = SynchedEntityData.defineId(
            Player.class,
            EntityDataSerializers.BOOLEAN
    );
    @Unique
    private static final EntityDataAccessor<Boolean> MINDEVICE_PHONE_BANK_QR_POSE = SynchedEntityData.defineId(
            Player.class,
            EntityDataSerializers.BOOLEAN
    );
    @Unique
    private static final EntityDataAccessor<Boolean> MINDEVICE_PHONE_SCREEN_ON = SynchedEntityData.defineId(
            Player.class,
            EntityDataSerializers.BOOLEAN
    );

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void minedevice$definePhoneCallPoseData(CallbackInfo ci) {
        ((Player) (Object) this).getEntityData().define(MINDEVICE_PHONE_CALL_POSE, false);
        ((Player) (Object) this).getEntityData().define(MINDEVICE_PHONE_CAMERA_POSE, false);
        ((Player) (Object) this).getEntityData().define(MINDEVICE_PHONE_CAMERA_SELFIE, false);
        ((Player) (Object) this).getEntityData().define(MINDEVICE_PHONE_BANK_QR_POSE, false);
        ((Player) (Object) this).getEntityData().define(MINDEVICE_PHONE_SCREEN_ON, false);
    }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void minedevice$preventDroppingHomePhoneHandset(ItemStack stack, boolean dropAround,
                                                            boolean includeThrowerName,
                                                            CallbackInfoReturnable<ItemEntity> cir) {
        if (stack.is(ModItems.HOME_PHONE_HANDSET.get())) {
            cir.setReturnValue(null);
        }
    }

    @Override
    public boolean minedevice$isPhoneCallPoseActive() {
        return ((Player) (Object) this).getEntityData().get(MINDEVICE_PHONE_CALL_POSE);
    }

    @Override
    public void minedevice$setPhoneCallPoseActive(boolean active) {
        ((Player) (Object) this).getEntityData().set(MINDEVICE_PHONE_CALL_POSE, active);
    }

    @Override
    public boolean minedevice$isPhoneCameraPoseActive() {
        return ((Player) (Object) this).getEntityData().get(MINDEVICE_PHONE_CAMERA_POSE);
    }

    @Override
    public void minedevice$setPhoneCameraPoseActive(boolean active) {
        ((Player) (Object) this).getEntityData().set(MINDEVICE_PHONE_CAMERA_POSE, active);
        if (!active) {
            ((Player) (Object) this).getEntityData().set(MINDEVICE_PHONE_CAMERA_SELFIE, false);
        }
    }

    @Override
    public boolean minedevice$isPhoneCameraSelfieActive() {
        return ((Player) (Object) this).getEntityData().get(MINDEVICE_PHONE_CAMERA_SELFIE);
    }

    @Override
    public void minedevice$setPhoneCameraSelfieActive(boolean active) {
        ((Player) (Object) this).getEntityData().set(MINDEVICE_PHONE_CAMERA_SELFIE, active);
    }

    @Override
    public boolean minedevice$isPhoneBankQrPoseActive() {
        return ((Player) (Object) this).getEntityData().get(MINDEVICE_PHONE_BANK_QR_POSE);
    }

    @Override
    public void minedevice$setPhoneBankQrPoseActive(boolean active) {
        ((Player) (Object) this).getEntityData().set(MINDEVICE_PHONE_BANK_QR_POSE, active);
    }

    @Override
    public boolean minedevice$isPhoneScreenOnActive() {
        return ((Player) (Object) this).getEntityData().get(MINDEVICE_PHONE_SCREEN_ON);
    }

    @Override
    public void minedevice$setPhoneScreenOnActive(boolean active) {
        ((Player) (Object) this).getEntityData().set(MINDEVICE_PHONE_SCREEN_ON, active);
    }
}
