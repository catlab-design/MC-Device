package com.sammy.minedevice.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class NetworkBufferUtils {
    private NetworkBufferUtils() {
    }

    public static RegistryFriendlyByteBuf create() {
        return create(RegistryAccess.EMPTY);
    }

    public static RegistryFriendlyByteBuf create(ServerPlayer player) {
        RegistryAccess access = player != null && player.level() != null
                ? player.level().registryAccess()
                : RegistryAccess.EMPTY;
        return create(access);
    }

    public static RegistryFriendlyByteBuf create(RegistryAccess registryAccess) {
        return new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                registryAccess != null ? registryAccess : RegistryAccess.EMPTY
        );
    }
}
