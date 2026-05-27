package com.sammy.minedevice.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.client.phone.PhoneClientHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(
            method = "applyItemArmTransform",
            at = @At("HEAD"),
            cancellable = true
    )
    private void minedevice$applyCameraArmTransform(PoseStack poseStack, HumanoidArm arm,
                                                    float equipProgress, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        // ซ่อนมือถือในมุมมอง first-person เมื่อกำลังถ่ายรูปเซลฟี่
        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offhandItem = player.getOffhandItem();
        if (mainHandItem.is(ModItems.PHONE.get()) || offhandItem.is(ModItems.PHONE.get())) {
            if (PhoneClientHooks.isLocalPhoneCameraSelfieActive()) {
                // ซ่อนมือถือทั้งคู่โดยการย้ายออกจากมุมมอง
                poseStack.translate(0.0F, -100.0F, 0.0F);
                ci.cancel();
                return;
            }
        }

        if (!PhoneClientHooks.isLocalPhoneCameraPoseActive()) {
            return;
        }

        HumanoidArm phoneArm = PhoneClientHooks.getRaisedPhoneArm(player);

        if (arm != phoneArm) {
            // ซ่อนแขนที่ไม่ได้ถือมือถือในโหมดกล้อง
            poseStack.translate(0.0F, -100.0F, 0.0F);
            ci.cancel();
            return;
        }

        // ท่าถือกล้อง: ยกแขนขึ้นในมุมมอง first-person
        // Y เปลี่ยนจาก -0.52 → 0.0 เพื่อยกไหล่ขึ้นระดับสายตา
        // rotation เล็กน้อยเพื่อให้แขนเอียงมาด้านหน้าเล็กน้อย
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate(side * 0.40F, 0.0F + equipProgress * -0.6F, -0.72F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -5.0F));
        ci.cancel();
    }
}
