package com.sammy.minedevice.client;

import com.sammy.minedevice.Minedevice;
import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.block.HomePhoneBlock;
import com.sammy.minedevice.client.phone.PhoneClientHooks;
import com.sammy.minedevice.client.phone.PhoneNetworkingClient;
import com.sammy.minedevice.item.HomePhoneHandsetItem;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

public final class MinedeviceClient {
    private static boolean initialized;
    private static boolean propertyRegistered;
    private static boolean homePhoneSneakUseDown;

    private MinedeviceClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        registerModelProperties();
        PhoneNetworkingClient.init();
        ClientTickEvent.CLIENT_POST.register(MinedeviceClient::onClientTick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register((player) -> {
            homePhoneSneakUseDown = false;
            PhoneNetworkingClient.clearClientState();
        });
        ClientLifecycleEvent.CLIENT_STOPPING.register((client) -> {
            homePhoneSneakUseDown = false;
            PhoneNetworkingClient.clearClientState();
        });
    }

    private static void onClientTick(Minecraft minecraft) {
        PhoneNetworkingClient.tick(minecraft);
        handleHomePhoneSneakUse(minecraft);
    }

    private static void handleHomePhoneSneakUse(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            homePhoneSneakUseDown = false;
            return;
        }

        boolean useDown = minecraft.options.keyUse.isDown();
        if (!useDown || !minecraft.player.isShiftKeyDown()) {
            homePhoneSneakUseDown = false;
            return;
        }

        if (homePhoneSneakUseDown) {
            return;
        }

        homePhoneSneakUseDown = true;
        BlockPos targetPos = resolveTargetedHeldHomePhone(minecraft, minecraft.player);
        if (targetPos != null) {
            PhoneNetworkingClient.requestPutDownHomePhoneHandset(targetPos);
        }
    }

    private static BlockPos resolveTargetedHeldHomePhone(Minecraft minecraft, LocalPlayer player) {
        if (!(minecraft.hitResult instanceof BlockHitResult blockHitResult)) {
            return null;
        }

        BlockPos blockPos = blockHitResult.getBlockPos();
        if (!(minecraft.level.getBlockState(blockPos).getBlock() instanceof HomePhoneBlock)) {
            return null;
        }

        if (isHandsetBoundTo(player.getMainHandItem(), minecraft, blockPos)
                || isHandsetBoundTo(player.getOffhandItem(), minecraft, blockPos)) {
            return blockPos;
        }

        return null;
    }

    private static boolean isHandsetBoundTo(ItemStack stack, Minecraft minecraft, BlockPos blockPos) {
        var address = HomePhoneHandsetItem.getBoundAddress(stack);
        return address != null
                && minecraft.level != null
                && address.dimension().equals(minecraft.level.dimension())
                && address.blockPos().equals(blockPos);
    }

    private static void registerModelProperties() {
        if (propertyRegistered) {
            return;
        }

        propertyRegistered = true;
        ResourceLocation phoneCallPoseProperty = new ResourceLocation(Minedevice.MOD_ID, "call_pose");
        ItemPropertiesRegistry.register(ModItems.PHONE.get(), phoneCallPoseProperty,
                (stack, level, entity, seed) -> entity != null && PhoneClientHooks.shouldUsePhoneCallPose(entity)
                        ? 1.0F
                        : 0.0F);
    }
}
