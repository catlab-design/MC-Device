package com.sammy.minedevice.atm;

import com.sammy.minedevice.Minedevice;
import com.sammy.minedevice.ModBlocks;
import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.block.AtmBlock;
import com.sammy.minedevice.item.CardItem;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public final class AtmNetworking {
    public static final ResourceLocation OPEN_SCREEN = id("atm_open_screen");
    public static final ResourceLocation STATE_SYNC = id("atm_state_sync");
    public static final ResourceLocation OPEN_REQUEST = id("atm_open_request");
    public static final ResourceLocation DEPOSIT = id("atm_deposit");
    public static final ResourceLocation DEPOSIT_ALL = id("atm_deposit_all");
    public static final ResourceLocation WITHDRAW = id("atm_withdraw");
    public static final ResourceLocation WITHDRAW_ALL = id("atm_withdraw_all");
    public static final ResourceLocation TRANSFER = id("atm_transfer");
    public static final ResourceLocation SET_PIN = id("atm_set_pin");
    public static final ResourceLocation VERIFY_PIN = id("atm_verify_pin");
    public static final ResourceLocation EJECT = id("atm_eject");
    public static final ResourceLocation MENU_WITHDRAW = id("atm_menu_withdraw");
    public static final ResourceLocation MENU_DEPOSIT = id("atm_menu_deposit");
    public static final ResourceLocation MENU_DEPOSIT_ALL = id("atm_menu_deposit_all");
    public static final ResourceLocation MENU_TRANSFER = id("atm_menu_transfer");
    public static final ResourceLocation MENU_TAKE_ALL = id("atm_menu_take_all");
    public static final ResourceLocation HISTORY_REQUEST = id("atm_history_request");
    public static final ResourceLocation HISTORY_RESPONSE = id("atm_history_response");
    public static final ResourceLocation BANK_OPEN_SCREEN = id("bank_open_screen");
    public static final ResourceLocation BANK_UNLOCK = id("bank_unlock");
    private static final double ATM_ACCESS_DISTANCE_SQR = 8.0D * 8.0D;
    private static boolean initialized;

    private AtmNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.c2s(), OPEN_REQUEST, (buf, context) -> {
            BlockPos atmPos = buf.readBlockPos();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    openScreen(player, atmPos);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), DEPOSIT_ALL, (buf, context) -> {
            BlockPos atmPos = buf.readBlockPos();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleDepositAll(player, atmPos);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), DEPOSIT, (buf, context) -> {
            BlockPos atmPos = buf.readBlockPos();
            int value = buf.readVarInt();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleDeposit(player, atmPos, value);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), WITHDRAW, (buf, context) -> {
            BlockPos atmPos = buf.readBlockPos();
            int value = buf.readVarInt();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleWithdraw(player, atmPos, value);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), WITHDRAW_ALL, (buf, context) -> {
            BlockPos atmPos = buf.readBlockPos();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleWithdrawAll(player, atmPos);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), TRANSFER, (buf, context) -> {
            BlockPos atmPos = buf.readBlockPos();
            String recipientName = buf.readUtf();
            long amount = buf.readLong();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleTransfer(player, atmPos, recipientName, amount);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), SET_PIN, (buf, context) -> {
            int containerId = buf.readVarInt();
            String pin = buf.readUtf();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player
                        && player.containerMenu instanceof AtmMenu menu
                        && menu.containerId == containerId) {
                    menu.handleSetPin(pin);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), VERIFY_PIN, (buf, context) -> {
            int containerId = buf.readVarInt();
            String pin = buf.readUtf();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player
                        && player.containerMenu instanceof AtmMenu menu
                        && menu.containerId == containerId) {
                    menu.handleVerifyPin(pin);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), EJECT, (buf, context) -> {
            int containerId = buf.readVarInt();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player
                        && player.containerMenu instanceof AtmMenu menu
                        && menu.containerId == containerId) {
                    menu.handleEjectCard();
                    player.closeContainer();
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), MENU_WITHDRAW, (buf, context) -> {
            int containerId = buf.readVarInt();
            int value = buf.readVarInt();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player
                        && player.containerMenu instanceof AtmMenu menu
                        && menu.containerId == containerId) {
                    menu.handleWithdraw(value);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), MENU_DEPOSIT, (buf, context) -> {
            int containerId = buf.readVarInt();
            int value = buf.readVarInt();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player
                        && player.containerMenu instanceof AtmMenu menu
                        && menu.containerId == containerId) {
                    menu.handleDeposit(value);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), MENU_DEPOSIT_ALL, (buf, context) -> {
            int containerId = buf.readVarInt();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player
                        && player.containerMenu instanceof AtmMenu menu
                        && menu.containerId == containerId) {
                    menu.handleDepositAll();
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), MENU_TRANSFER, (buf, context) -> {
            int containerId = buf.readVarInt();
            String recipientName = buf.readUtf();
            long amount = buf.readLong();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player
                        && player.containerMenu instanceof AtmMenu menu
                        && menu.containerId == containerId) {
                    menu.handleTransfer(recipientName, amount);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), MENU_TAKE_ALL, (buf, context) -> {
            int containerId = buf.readVarInt();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player
                        && player.containerMenu instanceof AtmMenu menu
                        && menu.containerId == containerId) {
                    menu.handleTakeAll();
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), HISTORY_REQUEST, (buf, context) -> {
            int containerId = buf.readVarInt();
            int offset = buf.readVarInt();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player
                        && player.containerMenu instanceof AtmMenu menu
                        && menu.containerId == containerId) {
                    sendHistoryResponse(player, offset);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), BANK_UNLOCK, (buf, context) -> {
            String newPin = buf.readUtf();
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    handleBankUnlock(player, newPin);
                }
            });
        });
    }

    private static void sendHistoryResponse(ServerPlayer player, int offset) {
        TransactionHistoryStore store = TransactionHistoryStore.get(player.getServer());
        int total = store.getCount(player.getUUID());
        int pageSize = 6;
        int clampedOffset = Math.max(0, Math.min(offset, total));
        var txns = store.getPage(player.getUUID(), clampedOffset, pageSize);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(total);
        buf.writeVarInt(clampedOffset);
        buf.writeVarInt(txns.size());
        for (var t : txns) {
            buf.writeByte(t.type);
            buf.writeLong(t.amount);
            buf.writeUtf(t.counterparty);
            buf.writeLong(t.time);
            buf.writeLong(t.balanceAfter);
        }
        NetworkManager.sendToPlayer(player, HISTORY_RESPONSE, buf);
    }

    private static void handleTransfer(ServerPlayer player, BlockPos atmPos, String recipientName, long amount) {
        BlockPos basePos = resolveAccessibleAtm(player, atmPos);
        if (basePos == null) {
            return;
        }

        if (amount <= 0L) {
            sendState(player, basePos);
            return;
        }

        UUID recipientUuid = null;
        ServerPlayer recipient = player.getServer().getPlayerList().getPlayerByName(recipientName);
        if (recipient != null) {
            recipientUuid = recipient.getUUID();
        } else {
            var profileOpt = player.getServer().getProfileCache().get(recipientName);
            if (profileOpt != null && profileOpt.isPresent()) {
                recipientUuid = profileOpt.get().getId();
            }
        }

        if (recipientUuid == null) {
            player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.player_not_found"), true);
            sendState(player, basePos);
            return;
        }

        if (recipientUuid.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.transfer_self"), true);
            sendState(player, basePos);
            return;
        }

        AtmAccountStore store = AtmAccountStore.get(player.getServer());
        long balance = store.getBalance(player.getUUID());
        if (balance < amount) {
            player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.insufficient"), true);
            sendState(player, basePos, balance);
            return;
        }

        store.withdraw(player.getUUID(), amount);
        store.deposit(recipientUuid, amount);

        long nextBalance = store.getBalance(player.getUUID());
        player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.transferred", amount, recipientName, nextBalance), true);
        sendState(player, basePos, nextBalance);

        if (recipient != null) {
            recipient.displayClientMessage(Component.translatable("screen.minedevice.atm.status.received", amount, player.getGameProfile().getName()), true);
            sendState(recipient, basePos, store.getBalance(recipientUuid));
        }
    }

    public static void openScreen(ServerPlayer player, BlockPos atmPos) {
        BlockPos basePos = resolveAccessibleAtm(player, atmPos);
        if (basePos == null) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.CARD.get())) {
            return;
        }

        ItemStack card = held.split(1);
        if (card.isEmpty()) {
            return;
        }

        ItemStack cardCopy = card.copy();
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> {
                    AtmMenu menu = new AtmMenu(id, inv, basePos);
                    menu.preInsertCard(cardCopy);
                    return menu;
                },
                Component.translatable("screen.minedevice.atm.title")
        ));
        markInventoryChanged(player);
    }

    public static void openBankScreen(ServerPlayer player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToPlayer(player, BANK_OPEN_SCREEN, buf);
    }

    private static void handleBankUnlock(ServerPlayer player, String newPin) {
        if (newPin == null || newPin.length() != 4 || !newPin.matches("\\d+")) {
            return;
        }
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        ItemStack cardStack = null;
        if (mainHand.is(ModItems.CARD.get())) {
            cardStack = mainHand;
        } else if (offHand.is(ModItems.CARD.get())) {
            cardStack = offHand;
        }
        if (cardStack == null) {
            return;
        }
        UUID cardId = CardItem.getCardUUID(cardStack);
        if (cardId == null) {
            return;
        }
        CardAccountStore store = CardAccountStore.get(player.getServer());
        if (!store.isLocked(cardId)) {
            return;
        }
        store.unlock(cardId);
        store.setPin(cardId, newPin);
        player.displayClientMessage(
                Component.translatable("screen.minedevice.bank.unlocked"), true);
    }

    private static void handleDepositAll(ServerPlayer player, BlockPos atmPos) {
        BlockPos basePos = resolveAccessibleAtm(player, atmPos);
        if (basePos == null) {
            return;
        }

        long deposited = removeAllBills(player);
        if (deposited <= 0L) {
            player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.no_cash"), true);
            sendState(player, basePos);
            return;
        }

        long balance = AtmAccountStore.get(player.getServer()).deposit(player.getUUID(), deposited);
        player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.deposited", deposited, balance), true);
        sendState(player, basePos, balance);
    }

    private static void handleDeposit(ServerPlayer player, BlockPos atmPos, int value) {
        BlockPos basePos = resolveAccessibleAtm(player, atmPos);
        if (basePos == null) {
            return;
        }

        if (billItem(value) == null || !removeOneBill(player, value)) {
            player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.no_cash"), true);
            sendState(player, basePos);
            return;
        }

        long balance = AtmAccountStore.get(player.getServer()).deposit(player.getUUID(), value);
        player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.deposited", value, balance), true);
        sendState(player, basePos, balance);
    }

    private static void handleWithdraw(ServerPlayer player, BlockPos atmPos, int value) {
        BlockPos basePos = resolveAccessibleAtm(player, atmPos);
        if (basePos == null) {
            return;
        }

        Item billItem = billItem(value);
        if (billItem == null) {
            sendState(player, basePos);
            return;
        }

        AtmAccountStore store = AtmAccountStore.get(player.getServer());
        long balance = store.getBalance(player.getUUID());
        if (balance < value) {
            player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.insufficient"), true);
            sendState(player, basePos, balance);
            return;
        }

        ItemStack withdrawnBill = new ItemStack(billItem);
        if (!player.getInventory().add(withdrawnBill)) {
            player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.inventory_full"), true);
            sendState(player, basePos, balance);
            return;
        }

        store.withdraw(player.getUUID(), value);
        markInventoryChanged(player);
        long nextBalance = store.getBalance(player.getUUID());
        player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.withdrawn", value, nextBalance), true);
        sendState(player, basePos, nextBalance);
    }

    private static void handleWithdrawAll(ServerPlayer player, BlockPos atmPos) {
        BlockPos basePos = resolveAccessibleAtm(player, atmPos);
        if (basePos == null) {
            return;
        }

        AtmAccountStore store = AtmAccountStore.get(player.getServer());
        long balance = store.getBalance(player.getUUID());
        long withdrawableAmount = balance - Math.floorMod(balance, 20L);
        if (withdrawableAmount <= 0L) {
            player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.insufficient"), true);
            sendState(player, basePos, balance);
            return;
        }

        long[] billCounts = billCounts(withdrawableAmount);
        if (!canFitBills(player, billCounts)) {
            player.displayClientMessage(Component.translatable("screen.minedevice.atm.status.inventory_full"), true);
            sendState(player, basePos, balance);
            return;
        }

        addBills(player, billCounts);
        store.withdraw(player.getUUID(), withdrawableAmount);
        markInventoryChanged(player);
        long nextBalance = store.getBalance(player.getUUID());
        player.displayClientMessage(Component.translatable(
                "screen.minedevice.atm.status.withdrawn", withdrawableAmount, nextBalance), true);
        sendState(player, basePos, nextBalance);
    }

    private static void sendState(ServerPlayer player, BlockPos atmPos) {
        sendState(player, atmPos, getBalance(player));
    }

    private static void sendState(ServerPlayer player, BlockPos atmPos, long balance) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(atmPos);
        buf.writeLong(balance);
        NetworkManager.sendToPlayer(player, STATE_SYNC, buf);
    }

    private static long getBalance(ServerPlayer player) {
        return AtmAccountStore.get(player.getServer()).getBalance(player.getUUID());
    }

    private static long removeAllBills(ServerPlayer player) {
        long total = 0L;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            long removed = billStackValue(stack);
            if (removed > 0L) {
                total += removed;
                player.getInventory().items.set(slot, ItemStack.EMPTY);
            }
        }

        for (int slot = 0; slot < player.getInventory().offhand.size(); slot++) {
            ItemStack stack = player.getInventory().offhand.get(slot);
            long removed = billStackValue(stack);
            if (removed > 0L) {
                total += removed;
                player.getInventory().offhand.set(slot, ItemStack.EMPTY);
            }
        }

        if (total > 0L) {
            markInventoryChanged(player);
        }
        return total;
    }

    private static boolean removeOneBill(ServerPlayer player, int value) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (billValue(stack) == value && !stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().items.set(slot, ItemStack.EMPTY);
                }
                markInventoryChanged(player);
                return true;
            }
        }

        for (int slot = 0; slot < player.getInventory().offhand.size(); slot++) {
            ItemStack stack = player.getInventory().offhand.get(slot);
            if (billValue(stack) == value && !stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().offhand.set(slot, ItemStack.EMPTY);
                }
                markInventoryChanged(player);
                return true;
            }
        }

        return false;
    }

    private static long billStackValue(ItemStack stack) {
        int value = billValue(stack);
        if (value <= 0 || stack.isEmpty()) {
            return 0L;
        }

        return (long) value * stack.getCount();
    }

    private static int billValue(ItemStack stack) {
        if (stack.is(ModItems.BILL20.get())) {
            return 20;
        }
        if (stack.is(ModItems.BILL100.get())) {
            return 100;
        }
        if (stack.is(ModItems.BILL500.get())) {
            return 500;
        }
        return stack.is(ModItems.BILL1000.get()) ? 1000 : 0;
    }

    private static Item billItem(int value) {
        return switch (value) {
            case 20 -> ModItems.BILL20.get();
            case 100 -> ModItems.BILL100.get();
            case 500 -> ModItems.BILL500.get();
            case 1000 -> ModItems.BILL1000.get();
            default -> null;
        };
    }

    private static long[] billCounts(long amount) {
        long remaining = Math.max(0L, amount);
        long bill1000 = remaining / 1000L;
        remaining %= 1000L;
        long bill500 = remaining / 500L;
        remaining %= 500L;
        long bill100 = remaining / 100L;
        remaining %= 100L;
        long bill20 = remaining / 20L;
        return new long[]{bill1000, bill500, bill100, bill20};
    }

    private static boolean canFitBills(ServerPlayer player, long[] billCounts) {
        long[] existingCapacity = new long[4];
        int emptySlots = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                emptySlots++;
                continue;
            }

            int index = billIndex(stack);
            if (index >= 0) {
                existingCapacity[index] += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            }
        }

        long requiredEmptySlots = 0L;
        for (int index = 0; index < billCounts.length; index++) {
            long remaining = Math.max(0L, billCounts[index] - existingCapacity[index]);
            if (remaining > 0L) {
                requiredEmptySlots += (remaining + 63L) / 64L;
            }
        }

        return requiredEmptySlots <= emptySlots;
    }

    private static void addBills(ServerPlayer player, long[] billCounts) {
        addBills(player, ModItems.BILL1000.get(), billCounts[0]);
        addBills(player, ModItems.BILL500.get(), billCounts[1]);
        addBills(player, ModItems.BILL100.get(), billCounts[2]);
        addBills(player, ModItems.BILL20.get(), billCounts[3]);
    }

    private static void addBills(ServerPlayer player, Item item, long count) {
        long remaining = Math.max(0L, count);
        int maxStackSize = item.getDefaultInstance().getMaxStackSize();
        while (remaining > 0L) {
            int stackCount = (int) Math.min(maxStackSize, remaining);
            player.getInventory().add(new ItemStack(item, stackCount));
            remaining -= stackCount;
        }
    }

    private static int billIndex(ItemStack stack) {
        if (stack.is(ModItems.BILL1000.get())) {
            return 0;
        }
        if (stack.is(ModItems.BILL500.get())) {
            return 1;
        }
        if (stack.is(ModItems.BILL100.get())) {
            return 2;
        }
        return stack.is(ModItems.BILL20.get()) ? 3 : -1;
    }

    public static BlockPos resolveAccessibleAtm(ServerPlayer player, BlockPos atmPos) {
        if (player == null || player.getServer() == null || atmPos == null) {
            return null;
        }

        ServerLevel level = player.serverLevel();
        BlockPos basePos = resolveAtmBase(level, atmPos);
        if (basePos == null) {
            return null;
        }

        double centerX = basePos.getX() + 0.5D;
        double centerY = basePos.getY() + 1.0D;
        double centerZ = basePos.getZ() + 0.5D;
        if (player.distanceToSqr(centerX, centerY, centerZ) > ATM_ACCESS_DISTANCE_SQR) {
            return null;
        }

        return basePos;
    }

    private static BlockPos resolveAtmBase(ServerLevel level, BlockPos atmPos) {
        if (level == null || atmPos == null) {
            return null;
        }

        BlockState state = level.getBlockState(atmPos);
        if (!state.is(ModBlocks.ATM.get()) || !state.hasProperty(AtmBlock.PART)) {
            return null;
        }

        BlockPos basePos = state.getValue(AtmBlock.PART) == 0 ? atmPos : atmPos.below();
        BlockState baseState = level.getBlockState(basePos);
        if (!baseState.is(ModBlocks.ATM.get())
                || !baseState.hasProperty(AtmBlock.PART)
                || baseState.getValue(AtmBlock.PART) != 0) {
            return null;
        }

        return basePos.immutable();
    }

    private static void markInventoryChanged(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Minedevice.MOD_ID, path);
    }
}
