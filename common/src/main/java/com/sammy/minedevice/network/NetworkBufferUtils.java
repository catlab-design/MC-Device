package com.sammy.minedevice.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class NetworkBufferUtils {
    private NetworkBufferUtils() {
    }

    public static RegistryFriendlyByteBuf create() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }
}
