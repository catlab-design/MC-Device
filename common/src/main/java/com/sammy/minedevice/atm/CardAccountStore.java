package com.sammy.minedevice.atm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CardAccountStore extends SavedData {
    private static final String DATA_NAME = "minedevice_card_accounts";
    private static final String ACCOUNTS_TAG = "card_accounts";
    private static final String CARD_ID_TAG = "card_id";
    private static final String OWNER_TAG = "owner";
    private static final String PIN_TAG = "pin";
    private static final String CARD_NUMBER_TAG = "card_number";
    private static final String LOCKED_TAG = "locked";
    private static final String ATTEMPTS_TAG = "attempts";
    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final Map<UUID, CardAccount> accounts = new HashMap<>();

    public static CardAccountStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                CardAccountStore::load,
                CardAccountStore::new,
                DATA_NAME
        );
    }

    public static CardAccountStore load(CompoundTag tag) {
        CardAccountStore store = new CardAccountStore();
        if (tag == null || !tag.contains(ACCOUNTS_TAG, Tag.TAG_LIST)) {
            return store;
        }

        ListTag list = tag.getList(ACCOUNTS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID(CARD_ID_TAG)) {
                continue;
            }

            UUID cardId = entry.getUUID(CARD_ID_TAG);
            CardAccount account = new CardAccount();
            account.owner = entry.hasUUID(OWNER_TAG) ? entry.getUUID(OWNER_TAG) : null;
            account.pinHash = entry.getString(PIN_TAG);
            account.cardNumber = entry.getString(CARD_NUMBER_TAG);
            account.locked = entry.getBoolean(LOCKED_TAG);
            account.failedAttempts = entry.getInt(ATTEMPTS_TAG);
            store.accounts.put(cardId, account);
        }

        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, CardAccount> e : accounts.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(CARD_ID_TAG, e.getKey());
            CardAccount a = e.getValue();
            if (a.owner != null) {
                entry.putUUID(OWNER_TAG, a.owner);
            }
            entry.putString(PIN_TAG, a.pinHash);
            entry.putString(CARD_NUMBER_TAG, a.cardNumber);
            entry.putBoolean(LOCKED_TAG, a.locked);
            entry.putInt(ATTEMPTS_TAG, a.failedAttempts);
            list.add(entry);
        }

        tag.put(ACCOUNTS_TAG, list);
        return tag;
    }

    public CardAccount getOrCreate(UUID cardId, UUID owner) {
        CardAccount account = accounts.get(cardId);
        if (account == null) {
            account = new CardAccount();
            account.owner = owner;
            account.cardNumber = generateCardNumber(cardId);
            accounts.put(cardId, account);
            setDirty();
        }
        return account;
    }

    public CardAccount get(UUID cardId) {
        return accounts.get(cardId);
    }

    public void setPin(UUID cardId, String pin) {
        CardAccount a = accounts.get(cardId);
        if (a == null) {
            return;
        }
        a.pinHash = hashPin(pin);
        a.failedAttempts = 0;
        a.locked = false;
        setDirty();
    }

    public PinResult verifyPin(UUID cardId, String pin) {
        CardAccount a = accounts.get(cardId);
        if (a == null) {
            return PinResult.NO_CARD;
        }
        if (a.locked) {
            return PinResult.LOCKED;
        }
        if (a.pinHash.isEmpty()) {
            return PinResult.NOT_SET;
        }
        if (a.pinHash.equals(hashPin(pin))) {
            a.failedAttempts = 0;
            setDirty();
            return PinResult.OK;
        }

        a.failedAttempts++;
        if (a.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            a.locked = true;
            setDirty();
            return PinResult.LOCKED;
        }
        setDirty();
        return PinResult.WRONG;
    }

    public void unlock(UUID cardId) {
        CardAccount a = accounts.get(cardId);
        if (a == null) {
            return;
        }
        a.locked = false;
        a.failedAttempts = 0;
        setDirty();
    }

    public boolean hasPin(UUID cardId) {
        CardAccount a = accounts.get(cardId);
        return a != null && !a.pinHash.isEmpty();
    }

    public boolean isLocked(UUID cardId) {
        CardAccount a = accounts.get(cardId);
        return a != null && a.locked;
    }

    public int getFailedAttempts(UUID cardId) {
        CardAccount a = accounts.get(cardId);
        return a == null ? 0 : a.failedAttempts;
    }

    public String getCardNumber(UUID cardId) {
        CardAccount a = accounts.get(cardId);
        return a == null ? "" : a.cardNumber;
    }

    public UUID getOwner(UUID cardId) {
        CardAccount a = accounts.get(cardId);
        return a == null ? null : a.owner;
    }

    private static String generateCardNumber(UUID cardId) {
        return com.sammy.minedevice.item.CardItem.generateCardNumber(cardId);
    }

    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "**** **** ****";
        }
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + last4;
    }

    private static String hashPin(String pin) {
        if (pin == null || pin.isEmpty()) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(("minedevice$" + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return pin;
        }
    }

    public enum PinResult { OK, WRONG, LOCKED, NOT_SET, NO_CARD }

    public static final class CardAccount {
        UUID owner;
        String pinHash = "";
        String cardNumber = "";
        boolean locked = false;
        int failedAttempts = 0;

        public UUID owner() {
            return owner;
        }

        public String cardNumber() {
            return cardNumber;
        }

        public boolean locked() {
            return locked;
        }

        public int failedAttempts() {
            return failedAttempts;
        }
    }
}
