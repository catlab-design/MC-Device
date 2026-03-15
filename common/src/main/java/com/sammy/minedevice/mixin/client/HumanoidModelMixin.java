package com.sammy.minedevice.mixin.client;

import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.client.phone.PhoneClientHooks;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    private static final float SELFIE_ARM_X = 3.25F;
    private static final float SELFIE_ARM_FORWARD_Z = -2.25F;
    private static final float SELFIE_ARM_PITCH = -1.00F;
    private static final float SELFIE_ARM_YAW = 0.12F;
    private static final float SELFIE_ARM_ROLL = 0.12F;
    private static final float SELFIE_ARM_HEAD_PITCH_FOLLOW = 0.80F;
    private static final float SELFIE_ARM_HEAD_YAW_FOLLOW = 0.92F;
    private static final float CAMERA_ARM_X = 4.25F;
    private static final float CAMERA_ARM_FORWARD_Z = -1.0F;
    private static final float CAMERA_ARM_PITCH = -1.15F;
    private static final float CAMERA_ARM_YAW = 0.22F;
    private static final float CAMERA_ARM_ROLL = 0.08F;
    private static final float HANDSET_ARM_PITCH = -(134.0F * ((float) Math.PI / 180.0F));
    private static final float HANDSET_ARM_YAW = 0.08F;
    private static final float HANDSET_ARM_ROLL = 0.04F;

    @Shadow
    public ModelPart head;

    @Shadow
    public ModelPart rightArm;

    @Shadow
    public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void minedevice$raisePhoneArm(T entity, float limbSwing, float limbSwingAmount,
                                          float ageInTicks, float netHeadYaw, float headPitch,
                                          CallbackInfo ci) {
        if (!PhoneClientHooks.shouldRaisePhoneArm(entity)) {
            HumanoidArm handsetArm = minedevice$getHandsetArm(entity);
            if (handsetArm == null) {
                return;
            }

            ModelPart raisedArm = handsetArm == HumanoidArm.RIGHT ? rightArm : leftArm;
            float side = handsetArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            minedevice$applyHandsetPose(raisedArm, side);
            return;
        }

        HumanoidArm phoneArm = PhoneClientHooks.getRaisedPhoneArm(entity);
        ModelPart raisedArm = phoneArm == HumanoidArm.RIGHT ? rightArm : leftArm;
        float side = phoneArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        if (PhoneClientHooks.shouldUsePhoneCallPose(entity)) {
            minedevice$applyHandsetPose(raisedArm, side);
            return;
        }

        boolean selfieMode = PhoneClientHooks.shouldUseSelfieCamera(entity);

        if (selfieMode) {
            raisedArm.x = -SELFIE_ARM_X * side;
            raisedArm.z = SELFIE_ARM_FORWARD_Z;
            raisedArm.xRot = Mth.clamp((head.xRot * SELFIE_ARM_HEAD_PITCH_FOLLOW) + SELFIE_ARM_PITCH, -1.55F, 0.35F);
            raisedArm.yRot = (head.yRot * SELFIE_ARM_HEAD_YAW_FOLLOW) - (SELFIE_ARM_YAW * side);
            raisedArm.zRot = (SELFIE_ARM_ROLL * side) - (head.yRot * 0.08F * side);
            return;
        }

        raisedArm.x = -CAMERA_ARM_X * side;
        raisedArm.z = CAMERA_ARM_FORWARD_Z;
        raisedArm.xRot = Mth.clamp(head.xRot + CAMERA_ARM_PITCH, -1.80F, 0.35F);
        raisedArm.yRot = head.yRot - (CAMERA_ARM_YAW * side);
        raisedArm.zRot = CAMERA_ARM_ROLL * side;
    }

    @Unique
    private static HumanoidArm minedevice$getHandsetArm(LivingEntity entity) {
        if (entity.getMainHandItem().is(ModItems.HOME_PHONE_HANDSET.get())) {
            return entity.getMainArm();
        }

        if (entity.getOffhandItem().is(ModItems.HOME_PHONE_HANDSET.get())) {
            return entity.getMainArm().getOpposite();
        }

        return null;
    }

    @Unique
    private static void minedevice$applyHandsetPose(ModelPart raisedArm, float side) {
        raisedArm.xRot = HANDSET_ARM_PITCH;
        raisedArm.yRot = HANDSET_ARM_YAW * side;
        raisedArm.zRot = HANDSET_ARM_ROLL * side;
    }
}
