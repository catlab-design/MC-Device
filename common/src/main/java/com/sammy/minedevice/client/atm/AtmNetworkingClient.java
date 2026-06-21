package com.sammy.minedevice.client.atm;

import com.sammy.minedevice.atm.AtmNetworking;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public final class AtmNetworkingClient {
    private static boolean initialized;

    private AtmNetworkingClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.s2c(), AtmNetworking.BANK_OPEN_SCREEN, (buf, context) -> {
            context.queue(() -> Minecraft.getInstance().setScreen(new BankScreen()));
        });
        NetworkManager.registerReceiver(NetworkManager.s2c(), AtmNetworking.HISTORY_RESPONSE, (buf, context) -> {
            int total = buf.readVarInt();
            int offset = buf.readVarInt();
            int count = buf.readVarInt();
            java.util.List<HistoryEntry> entries = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                HistoryEntry e = new HistoryEntry();
                e.type = buf.readByte();
                e.amount = buf.readLong();
                e.counterparty = buf.readUtf();
                e.time = buf.readLong();
                e.balanceAfter = buf.readLong();
                entries.add(e);
            }
            context.queue(() -> {
                if (Minecraft.getInstance().screen instanceof AtmScreen screen) {
                    screen.setHistory(total, offset, entries);
                }
            });
        });
    }

    public static final class HistoryEntry {
        public byte type;
        public long amount;
        public String counterparty;
        public long time;
        public long balanceAfter;
    }

    public static void requestDepositAll(BlockPos atmPos) {
        if (atmPos == null) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(atmPos);
        NetworkManager.sendToServer(AtmNetworking.DEPOSIT_ALL, buf);
    }

    public static void requestDeposit(BlockPos atmPos, int value) {
        if (atmPos == null) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(atmPos);
        buf.writeVarInt(value);
        NetworkManager.sendToServer(AtmNetworking.DEPOSIT, buf);
    }

    public static void requestOpen(BlockPos atmPos) {
        if (atmPos == null) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(atmPos);
        NetworkManager.sendToServer(AtmNetworking.OPEN_REQUEST, buf);
    }

    public static void requestWithdraw(BlockPos atmPos, int value) {
        if (atmPos == null) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(atmPos);
        buf.writeVarInt(value);
        NetworkManager.sendToServer(AtmNetworking.WITHDRAW, buf);
    }

    public static void requestWithdrawAll(BlockPos atmPos) {
        if (atmPos == null) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(atmPos);
        NetworkManager.sendToServer(AtmNetworking.WITHDRAW_ALL, buf);
    }

    public static void requestTransfer(BlockPos atmPos, String recipientName, long amount) {
        if (atmPos == null || recipientName == null || recipientName.isEmpty() || amount <= 0L) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(atmPos);
        buf.writeUtf(recipientName);
        buf.writeLong(amount);
        NetworkManager.sendToServer(AtmNetworking.TRANSFER, buf);
    }

    public static void sendSetPin(int containerId, String pin) {
        if (pin == null || pin.isEmpty()) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(containerId);
        buf.writeUtf(pin);
        NetworkManager.sendToServer(AtmNetworking.SET_PIN, buf);
    }

    public static void sendVerifyPin(int containerId, String pin) {
        if (pin == null || pin.isEmpty()) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(containerId);
        buf.writeUtf(pin);
        NetworkManager.sendToServer(AtmNetworking.VERIFY_PIN, buf);
    }

    public static void sendEject(int containerId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(containerId);
        NetworkManager.sendToServer(AtmNetworking.EJECT, buf);
    }

    public static void sendMenuWithdraw(int containerId, int value) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(containerId);
        buf.writeVarInt(value);
        NetworkManager.sendToServer(AtmNetworking.MENU_WITHDRAW, buf);
    }

    public static void sendMenuDeposit(int containerId, int value) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(containerId);
        buf.writeVarInt(value);
        NetworkManager.sendToServer(AtmNetworking.MENU_DEPOSIT, buf);
    }

    public static void sendMenuDepositAll(int containerId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(containerId);
        NetworkManager.sendToServer(AtmNetworking.MENU_DEPOSIT_ALL, buf);
    }

    public static void sendMenuTransfer(int containerId, String recipientName, long amount) {
        if (recipientName == null || recipientName.isEmpty() || amount <= 0L) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(containerId);
        buf.writeUtf(recipientName);
        buf.writeLong(amount);
        NetworkManager.sendToServer(AtmNetworking.MENU_TRANSFER, buf);
    }

    public static void sendTakeAll(int containerId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(containerId);
        NetworkManager.sendToServer(AtmNetworking.MENU_TAKE_ALL, buf);
    }

    public static void sendHistoryRequest(int containerId, int offset) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(containerId);
        buf.writeVarInt(offset);
        NetworkManager.sendToServer(AtmNetworking.HISTORY_REQUEST, buf);
    }

    public static void sendBankUnlock(String newPin) {
        if (newPin == null || newPin.isEmpty()) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(newPin);
        NetworkManager.sendToServer(AtmNetworking.BANK_UNLOCK, buf);
    }
}
