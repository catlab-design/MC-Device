package com.sammy.minedevice.client.walkie;

import com.sammy.minedevice.walkie.WalkieBand;
import com.sammy.minedevice.walkie.WalkieNetworking;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

public final class WalkieNetworkingClient {
    private WalkieNetworkingClient() {
    }

    public static void requestTune(InteractionHand hand, WalkieBand band, int frequency) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeEnum(hand);
        buf.writeEnum(band);
        buf.writeVarInt(frequency);
        NetworkManager.sendToServer(WalkieNetworking.TUNE, buf);
    }

    public static void requestTransmit(InteractionHand hand) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeEnum(hand);
        NetworkManager.sendToServer(WalkieNetworking.TRANSMIT, buf);
    }

    public static void requestTalkState(InteractionHand hand, boolean talking) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeEnum(hand);
        buf.writeBoolean(talking);
        NetworkManager.sendToServer(WalkieNetworking.TALK_STATE, buf);
    }
}
