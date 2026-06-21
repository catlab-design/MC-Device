package com.sammy.minedevice.atm;

import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.ModMenus;
import com.sammy.minedevice.item.CardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.UUID;

public final class AtmMenu extends AbstractContainerMenu {
    public static final int CARD_SLOT = 0;
    public static final int DISPENSER_SLOTS = 4;
    public static final int STATE_NO_CARD = 0;
    public static final int STATE_AWAIT_PIN = 1;
    public static final int STATE_NEEDS_PIN = 2;
    public static final int STATE_PIN_OK = 3;
    public static final int STATE_LOCKED = 4;

    public static final int CARD_SLOT_X = 216;
    public static final int CARD_SLOT_Y = 28;
    public static final int DISP_SLOT_X = 216;
    public static final int DISP_SLOT_Y = 102;

    private static final int BILL20 = 20;
    private static final int BILL100 = 100;
    private static final int BILL500 = 500;
    private static final int BILL1000 = 1000;

    private final BlockPos atmPos;
    private final Player player;
    private final SimpleContainer cardContainer;
    private final SimpleContainer dispenserContainer;
    private final DataSlot cardState = DataSlot.standalone();
    private final DataSlot balanceHigh = DataSlot.standalone();
    private final DataSlot balanceLow = DataSlot.standalone();
    private final DataSlot failedAttempts = DataSlot.standalone();
    private final DataSlot cardSerial = DataSlot.standalone();

    private UUID cardId;
    private boolean pinVerified;
    private boolean dispenserActive = true;

    public AtmMenu(int containerId, Inventory playerInventory, BlockPos atmPos) {
        super(ModMenus.ATM.get(), containerId);
        this.atmPos = atmPos != null ? atmPos.immutable() : BlockPos.ZERO;
        this.player = playerInventory.player;
        this.cardContainer = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                onCardSlotChanged();
            }
        };
        this.dispenserContainer = new SimpleContainer(DISPENSER_SLOTS);

        addSlot(new CardSlot(cardContainer, 0, CARD_SLOT_X, CARD_SLOT_Y));
        for (int i = 0; i < DISPENSER_SLOTS; i++) {
            int col = i % 2;
            int row = i / 2;
            addSlot(new DispenserSlot(this, dispenserContainer, i,
                    DISP_SLOT_X + col * 18, DISP_SLOT_Y + row * 18));
        }

        // addPlayerInventory(playerInventory);

        addDataSlot(cardState);
        addDataSlot(balanceHigh);
        addDataSlot(balanceLow);
        addDataSlot(failedAttempts);
        addDataSlot(cardSerial);
    }

    public static AtmMenu fromNetwork(int containerId, Inventory playerInventory) {
        return new AtmMenu(containerId, playerInventory, BlockPos.ZERO);
    }

    public BlockPos getAtmPos() {
        return atmPos;
    }

    public int getCardState() {
        return cardState.get();
    }

    public long getBalance() {
        return ((long) balanceHigh.get() << 32) | (balanceLow.get() & 0xFFFFFFFFL);
    }

    public int getFailedAttempts() {
        return failedAttempts.get();
    }

    public int getCardSerial() {
        return cardSerial.get();
    }

    public ItemStack getCardStack() {
        return cardContainer.getItem(0);
    }

    public boolean isCardInserted() {
        return !cardContainer.getItem(0).isEmpty();
    }

    public void preInsertCard(ItemStack card) {
        cardContainer.setItem(0, card);
    }

    public boolean isPinVerified() {
        return pinVerified;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int invLeft = 36;
        int invTop = 272;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new PlayerSlot(playerInventory, col + row * 9 + 9, invLeft + col * 18, invTop + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new PlayerSlot(playerInventory, col, invLeft + col * 18, invTop + 58));
        }
    }

    public void setInventoryActive(boolean active) {
        for (Slot slot : slots) {
            if (slot instanceof PlayerSlot playerSlot) {
                playerSlot.active = active;
            }
        }
    }

    private void onCardSlotChanged() {
        ItemStack card = cardContainer.getItem(0);
        if (card.isEmpty()) {
            cardId = null;
            pinVerified = false;
            cardState.set(STATE_NO_CARD);
            failedAttempts.set(0);
            cardSerial.set(0);
            return;
        }

        if (!card.is(ModItems.CARD.get())) {
            return;
        }

        ServerPlayer player = getServerPlayer();
        if (player == null) {
            return;
        }

        UUID id = CardItem.getOrCreateCardUUID(card);
        cardId = id;
        MinecraftServer server = player.getServer();
        CardAccountStore store = CardAccountStore.get(server);
        CardAccountStore.CardAccount account = store.getOrCreate(id, player.getUUID());

        pinVerified = false;
        if (account.locked()) {
            cardState.set(STATE_LOCKED);
        } else if (store.hasPin(id)) {
            cardState.set(STATE_AWAIT_PIN);
        } else {
            cardState.set(STATE_NEEDS_PIN);
        }
        failedAttempts.set(account.failedAttempts());
        cardSerial.set((int) (Math.abs(id.getLeastSignificantBits()) % 100000));
        updateBalanceSlot();
    }

    private void updateBalanceSlot() {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return;
        }
        long balance = AtmAccountStore.get(serverPlayer.getServer()).getBalance(serverPlayer.getUUID());
        balanceHigh.set((int) (balance >>> 32));
        balanceLow.set((int) balance);
    }

    public void setDispenserActive(boolean active) {
        this.dispenserActive = active;
    }

    public boolean isDispenserActive() {
        return dispenserActive;
    }

    private ServerPlayer getServerPlayer() {
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    public void handleSetPin(String pin) {
        ServerPlayer player = getServerPlayer();
        if (player == null || cardId == null) {
            return;
        }
        if (!isValidPin(pin)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
            return;
        }
        CardAccountStore.get(player.getServer()).setPin(cardId, pin);
        pinVerified = true;
        cardState.set(STATE_PIN_OK);
        failedAttempts.set(0);
        updateBalanceSlot();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public void handleVerifyPin(String pin) {
        ServerPlayer player = getServerPlayer();
        if (player == null || cardId == null) {
            return;
        }
        CardAccountStore store = CardAccountStore.get(player.getServer());
        CardAccountStore.PinResult result = store.verifyPin(cardId, pin);
        switch (result) {
            case OK -> {
                pinVerified = true;
                cardState.set(STATE_PIN_OK);
                failedAttempts.set(0);
                updateBalanceSlot();
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            case WRONG -> {
                pinVerified = false;
                cardState.set(STATE_AWAIT_PIN);
                failedAttempts.set(store.getFailedAttempts(cardId));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            case LOCKED -> {
                pinVerified = false;
                cardState.set(STATE_LOCKED);
                failedAttempts.set(store.getFailedAttempts(cardId));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            default -> {
                pinVerified = false;
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
            }
        }
    }

    public boolean handleWithdraw(int value) {
        ServerPlayer player = getServerPlayer();
        if (player == null || !pinVerified || cardId == null) {
            return false;
        }
        Item billItem = billItem(value);
        if (billItem == null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
            return false;
        }
        AtmAccountStore store = AtmAccountStore.get(player.getServer());
        long balance = store.getBalance(player.getUUID());
        if (balance < value) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
            return false;
        }
        ItemStack bill = new ItemStack(billItem);
        if (!dispenserContainer.canAddItem(bill) && !hasEmptyDispenserSlot()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
            return false;
        }
        store.withdraw(player.getUUID(), value);
        dispenserContainer.addItem(bill);
        updateBalanceSlot();
        long newBalance = store.getBalance(player.getUUID());
        TransactionHistoryStore.get(player.getServer()).addTransaction(
                player.getUUID(), TransactionHistoryStore.TYPE_WITHDRAW, value, "", newBalance);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public boolean handleDeposit(int value) {
        ServerPlayer player = getServerPlayer();
        if (player == null || !pinVerified || cardId == null) {
            return false;
        }
        Item billItem = billItem(value);
        if (billItem == null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
            return false;
        }
        if (!removeOneBillFromPlayer(player, billItem)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
            return false;
        }
        long balance = AtmAccountStore.get(player.getServer()).deposit(player.getUUID(), value);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        balanceHigh.set((int) (balance >>> 32));
        balanceLow.set((int) balance);
        TransactionHistoryStore.get(player.getServer()).addTransaction(
                player.getUUID(), TransactionHistoryStore.TYPE_DEPOSIT, value, "", balance);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public boolean handleDepositAll() {
        ServerPlayer player = getServerPlayer();
        if (player == null || !pinVerified || cardId == null) {
            return false;
        }
        long total = removeAllBillsFromPlayer(player);
        if (total <= 0L) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
            return false;
        }
        long balance = AtmAccountStore.get(player.getServer()).deposit(player.getUUID(), total);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        balanceHigh.set((int) (balance >>> 32));
        balanceLow.set((int) balance);
        TransactionHistoryStore.get(player.getServer()).addTransaction(
                player.getUUID(), TransactionHistoryStore.TYPE_DEPOSIT, total, "", balance);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public boolean handleTransfer(String recipientName, long amount) {
        ServerPlayer player = getServerPlayer();
        if (player == null || !pinVerified || cardId == null || amount <= 0L) {
            return false;
        }
        UUID recipientUuid = resolveRecipient(player, recipientName);
        if (recipientUuid == null || recipientUuid.equals(player.getUUID())) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
            return false;
        }
        AtmAccountStore store = AtmAccountStore.get(player.getServer());
        long balance = store.getBalance(player.getUUID());
        if (balance < amount) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
            return false;
        }
        store.withdraw(player.getUUID(), amount);
        store.deposit(recipientUuid, amount);
        updateBalanceSlot();
        long newBalance = store.getBalance(player.getUUID());
        TransactionHistoryStore.get(player.getServer()).addTransaction(
                player.getUUID(), TransactionHistoryStore.TYPE_TRANSFER_OUT, amount, recipientName, newBalance);
        TransactionHistoryStore.get(player.getServer()).addTransaction(
                recipientUuid, TransactionHistoryStore.TYPE_TRANSFER_IN, amount, player.getGameProfile().getName(),
                store.getBalance(recipientUuid));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public void handleEjectCard() {
        ServerPlayer player = getServerPlayer();
        if (player == null) {
            return;
        }
        ItemStack card = cardContainer.removeItemNoUpdate(0);
        if (!card.isEmpty()) {
            if (!player.getInventory().add(card)) {
                player.drop(card, false);
            }
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        cardId = null;
        pinVerified = false;
        cardState.set(STATE_NO_CARD);
    }

    public void handleTakeAll() {
        ServerPlayer player = getServerPlayer();
        if (player == null) {
            return;
        }
        for (int i = 0; i < dispenserContainer.getContainerSize(); i++) {
            ItemStack stack = dispenserContainer.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        broadcastChanges();
    }

    public boolean hasDispenserItems() {
        for (int i = 0; i < dispenserContainer.getContainerSize(); i++) {
            if (!dispenserContainer.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public ItemStack getDispenserItem(int index) {
        if (index < 0 || index >= dispenserContainer.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return dispenserContainer.getItem(index);
    }

    private UUID resolveRecipient(ServerPlayer player, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        ServerPlayer online = player.getServer().getPlayerList().getPlayerByName(name);
        if (online != null) {
            return online.getUUID();
        }
        var profile = player.getServer().getProfileCache().get(name);
        return profile != null && profile.isPresent() ? profile.get().getId() : null;
    }

    private boolean hasEmptyDispenserSlot() {
        for (int i = 0; i < dispenserContainer.getContainerSize(); i++) {
            if (dispenserContainer.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean removeOneBillFromPlayer(ServerPlayer player, Item billItem) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (stack.is(billItem) && !stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().items.set(slot, ItemStack.EMPTY);
                }
                return true;
            }
        }
        for (int slot = 0; slot < player.getInventory().offhand.size(); slot++) {
            ItemStack stack = player.getInventory().offhand.get(slot);
            if (stack.is(billItem) && !stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().offhand.set(slot, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    private long removeAllBillsFromPlayer(ServerPlayer player) {
        long total = 0L;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            int value = billValue(stack);
            if (value > 0 && !stack.isEmpty()) {
                total += (long) value * stack.getCount();
                player.getInventory().items.set(slot, ItemStack.EMPTY);
            }
        }
        for (int slot = 0; slot < player.getInventory().offhand.size(); slot++) {
            ItemStack stack = player.getInventory().offhand.get(slot);
            int value = billValue(stack);
            if (value > 0 && !stack.isEmpty()) {
                total += (long) value * stack.getCount();
                player.getInventory().offhand.set(slot, ItemStack.EMPTY);
            }
        }
        return total;
    }

    private int billValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(ModItems.BILL20.get())) return BILL20;
        if (stack.is(ModItems.BILL100.get())) return BILL100;
        if (stack.is(ModItems.BILL500.get())) return BILL500;
        if (stack.is(ModItems.BILL1000.get())) return BILL1000;
        return 0;
    }

    private Item billItem(int value) {
        return switch (value) {
            case BILL20 -> ModItems.BILL20.get();
            case BILL100 -> ModItems.BILL100.get();
            case BILL500 -> ModItems.BILL500.get();
            case BILL1000 -> ModItems.BILL1000.get();
            default -> null;
        };
    }

    private static boolean isValidPin(String pin) {
        return pin != null && pin.length() == 4 && pin.matches("\\d+");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index == CARD_SLOT) {
            return ItemStack.EMPTY;
        } else if (index >= 1 && index <= DISPENSER_SLOTS) {
            if (!moveItemStackTo(original, DISPENSER_SLOTS + 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (original.is(ModItems.CARD.get())) {
                if (!moveItemStackTo(original, CARD_SLOT, CARD_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (original.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack card = cardContainer.removeItemNoUpdate(0);
            if (!card.isEmpty()) {
                if (!player.getInventory().add(card)) {
                    player.drop(card, false);
                }
            }
            for (int i = 0; i < dispenserContainer.getContainerSize(); i++) {
                ItemStack bill = dispenserContainer.removeItemNoUpdate(i);
                if (!bill.isEmpty()) {
                    if (!player.getInventory().add(bill)) {
                        player.drop(bill, false);
                    }
                }
            }
            serverPlayer.getInventory().setChanged();
            serverPlayer.inventoryMenu.broadcastChanges();
        }
        super.removed(player);
    }

    private static final class CardSlot extends Slot {
        CardSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack != null && stack.is(ModItems.CARD.get());
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    private static final class DispenserSlot extends Slot {
        private final AtmMenu menu;

        DispenserSlot(AtmMenu menu, Container container, int index, int x, int y) {
            super(container, index, x, y);
            this.menu = menu;
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static final class PlayerSlot extends Slot {
        boolean active = true;

        PlayerSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
