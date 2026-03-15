package com.sammy.minedevice.client;

import com.sammy.minedevice.MegaphoneMod;
import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.block.HomePhoneBlock;
import com.sammy.minedevice.client.phone.PhoneNetworkingClient;
import com.sammy.minedevice.client.phone.PhoneClientHooks;
import com.sammy.minedevice.client.voice.MegaphoneVoiceHook;
import com.sammy.minedevice.item.HomePhoneHandsetItem;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

public final class MegaphoneClient {
    private static boolean initialized;
    private static boolean propertyRegistered;
    private static boolean plasmoVoicePresent;
    private static boolean active;
    private static boolean homePhoneSneakUseDown;
    private static net.minecraft.client.KeyMapping toggleKey;

    private MegaphoneClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        registerModelProperties();
        PhoneNetworkingClient.init();

        toggleKey = new net.minecraft.client.KeyMapping(
                "key.minedevice.toggle_megaphone",
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                com.mojang.blaze3d.platform.InputConstants.KEY_M,
                "key.categories.minedevice");
        dev.architectury.registry.client.keymappings.KeyMappingRegistry.register(toggleKey);

        plasmoVoicePresent = Platform.isModLoaded("plasmovoice");
        if (plasmoVoicePresent) {
            MegaphoneVoiceHook.init();
        }

        ClientTickEvent.CLIENT_POST.register(MegaphoneClient::onClientTick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register((player) -> {
            active = false;
            PhoneNetworkingClient.clearClientState();
            if (plasmoVoicePresent) {
                MegaphoneVoiceHook.detach();
            }
        });
        ClientLifecycleEvent.CLIENT_STOPPING.register((client) -> {
            PhoneNetworkingClient.clearClientState();
            if (plasmoVoicePresent) {
                MegaphoneVoiceHook.shutdown();
            }
        });
    }

    private static void onClientTick(Minecraft minecraft) {
        PhoneNetworkingClient.tick(minecraft);
        handleHomePhoneSneakUse(minecraft);
        if (!plasmoVoicePresent) {
            return;
        }

        while (toggleKey != null && toggleKey.consumeClick()) {
            active = !active;
            if (minecraft.player != null) {
                String messageKey = active ? "action.minedevice.megaphone.on" : "action.minedevice.megaphone.off";
                minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(messageKey),
                        true);
            }
        }

        LocalPlayer player = minecraft.player;
        boolean shouldEnableMegaphone = player != null && (isUsingMegaphone(player) || (active && isHoldingMegaphone(player)));
        MegaphoneVoiceHook.tick(shouldEnableMegaphone);
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

    private static boolean isHoldingMegaphone(LocalPlayer player) {
        return isMegaphoneItem(player.getMainHandItem()) || isMegaphoneItem(player.getOffhandItem());
    }

    private static boolean isMegaphoneItem(ItemStack stack) {
        return stack.is(ModItems.MEGAPHONE.get()) || 
               stack.is(ModItems.MEGAPHONE_RED.get()) || 
               stack.is(ModItems.MEGAPHONE_YELLOW.get()) || 
               stack.is(ModItems.MEGAPHONE_LIGHT_BLUE.get());
    }

    private static boolean isUsingMegaphone(LocalPlayer player) {
        if (!player.isUsingItem()) {
            return false;
        }

        ItemStack usingItem = player.getUseItem();
        return !usingItem.isEmpty() && isMegaphoneItem(usingItem);
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
        ResourceLocation phoneCallPoseProperty = new ResourceLocation(MegaphoneMod.MOD_ID, "call_pose");
        ItemPropertiesRegistry.register(ModItems.PHONE.get(), phoneCallPoseProperty,
                (stack, level, entity, seed) -> entity != null && PhoneClientHooks.shouldUsePhoneCallPose(entity) ? 1.0F : 0.0F);

        ResourceLocation tootingProperty = new ResourceLocation(MegaphoneMod.MOD_ID, "tooting");
        
        // Register tooting property for all megaphone colors
        registerTootingProperty(ModItems.MEGAPHONE.get(), tootingProperty);
        registerTootingProperty(ModItems.MEGAPHONE_RED.get(), tootingProperty);
        registerTootingProperty(ModItems.MEGAPHONE_YELLOW.get(), tootingProperty);
        registerTootingProperty(ModItems.MEGAPHONE_LIGHT_BLUE.get(), tootingProperty);

        ResourceLocation colorProperty = new ResourceLocation("minedevice", "color_type");
        ItemPropertiesRegistry.register(ModItems.MEGAPHONE.get(), colorProperty, (stack, level, entity, seed) -> {
            if (!(stack.getItem() instanceof net.minecraft.world.item.DyeableLeatherItem dyeable)) {
                return 0.0F;
            }

            int color = dyeable.getColor(stack);
            if (color == 11546150)
                return 0.1F; // Red
            if (color == 16701501)
                return 0.2F; // Yellow
            if (color == 3847130)
                return 0.3F; // Light Blue
            return 0.0F; // White/Default
        });
    }

    private static void registerTootingProperty(Item item, ResourceLocation property) {
        ItemPropertiesRegistry.register(item, property, (stack, level, entity, seed) -> entity != null
                && entity.isUsingItem()
                && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }
}
