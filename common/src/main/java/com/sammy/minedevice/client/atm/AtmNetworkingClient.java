package com.sammy.minedevice.client.atm;

import com.sammy.minedevice.network.NetworkBufferUtils;

import com.sammy.minedevice.atm.AtmNetworking;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class AtmNetworkingClient {
    private static boolean initialized;

    private AtmNetworkingClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.s2c(), AtmNetworking.OPEN_SCREEN, (buf, context) -> {
            BlockPos atmPos = buf.readBlockPos();
            long balance = buf.readLong();
            context.queue(() -> Minecraft.getInstance().setScreen(new AtmScreen(atmPos, balance)));
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), AtmNetworking.STATE_SYNC, (buf, context) -> {
            BlockPos atmPos = buf.readBlockPos();
            long balance = buf.readLong();
            context.queue(() -> AtmScreen.updateOpenScreen(atmPos, balance));
        });
    }

    public static void requestDepositAll(BlockPos atmPos) {
        if (atmPos == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeBlockPos(atmPos);
        NetworkManager.sendToServer(AtmNetworking.DEPOSIT_ALL, buf);
    }

    public static void requestDeposit(BlockPos atmPos, int value) {
        if (atmPos == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeBlockPos(atmPos);
        buf.writeVarInt(value);
        NetworkManager.sendToServer(AtmNetworking.DEPOSIT, buf);
    }

    public static void requestOpen(BlockPos atmPos) {
        if (atmPos == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeBlockPos(atmPos);
        NetworkManager.sendToServer(AtmNetworking.OPEN_REQUEST, buf);
    }

    public static void requestWithdraw(BlockPos atmPos, int value) {
        if (atmPos == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeBlockPos(atmPos);
        buf.writeVarInt(value);
        NetworkManager.sendToServer(AtmNetworking.WITHDRAW, buf);
    }

    public static void requestWithdrawAll(BlockPos atmPos) {
        if (atmPos == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeBlockPos(atmPos);
        NetworkManager.sendToServer(AtmNetworking.WITHDRAW_ALL, buf);
    }

    public static void requestTransfer(BlockPos atmPos, String recipientName, long amount) {
        if (atmPos == null || recipientName == null || recipientName.isEmpty() || amount <= 0L) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeBlockPos(atmPos);
        buf.writeUtf(recipientName);
        buf.writeLong(amount);
        NetworkManager.sendToServer(AtmNetworking.TRANSFER, buf);
    }
}
