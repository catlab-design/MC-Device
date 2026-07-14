package com.sammy.minedevice.atm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AtmAccountStore extends SavedData {
    private static final String DATA_NAME = "minedevice_atm_accounts";
    private static final String ACCOUNTS_TAG = "accounts";
    private static final String PLAYER_ID_TAG = "player_id";
    private static final String BALANCE_TAG = "balance";

    private final Map<UUID, Long> balances = new HashMap<>();

    public static AtmAccountStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                AtmAccountStore::load,
                AtmAccountStore::new,
                DATA_NAME
        );
    }

    public static AtmAccountStore load(CompoundTag tag) {
        AtmAccountStore store = new AtmAccountStore();
        if (tag == null || !tag.contains(ACCOUNTS_TAG, Tag.TAG_LIST)) {
            return store;
        }

        ListTag accountsTag = tag.getList(ACCOUNTS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < accountsTag.size(); index++) {
            CompoundTag accountTag = accountsTag.getCompound(index);
            if (!accountTag.hasUUID(PLAYER_ID_TAG)) {
                continue;
            }

            long balance = Math.max(0L, accountTag.getLong(BALANCE_TAG));
            if (balance > 0L) {
                store.balances.put(accountTag.getUUID(PLAYER_ID_TAG), balance);
            }
        }

        return store;
    }

    public long getBalance(UUID playerId) {
        if (playerId == null) {
            return 0L;
        }

        return Math.max(0L, balances.getOrDefault(playerId, 0L));
    }

    public long deposit(UUID playerId, long amount) {
        if (playerId == null || amount <= 0L) {
            return getBalance(playerId);
        }

        long currentBalance = getBalance(playerId);
        long nextBalance = Long.MAX_VALUE - currentBalance < amount
                ? Long.MAX_VALUE
                : currentBalance + amount;
        balances.put(playerId, nextBalance);
        setDirty();
        return nextBalance;
    }

    /** Directly sets a player's balance (used by admin commands). Negative values clamp to 0. */
    public long setBalance(UUID playerId, long amount) {
        if (playerId == null) {
            return 0L;
        }

        long clamped = Math.max(0L, amount);
        if (clamped <= 0L) {
            balances.remove(playerId);
        } else {
            balances.put(playerId, clamped);
        }
        setDirty();
        return clamped;
    }

    public boolean withdraw(UUID playerId, long amount) {
        if (playerId == null || amount <= 0L) {
            return false;
        }

        long currentBalance = getBalance(playerId);
        if (currentBalance < amount) {
            return false;
        }

        long nextBalance = currentBalance - amount;
        if (nextBalance <= 0L) {
            balances.remove(playerId);
        } else {
            balances.put(playerId, nextBalance);
        }
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag accountsTag = new ListTag();
        for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
            long balance = Math.max(0L, entry.getValue());
            if (balance <= 0L) {
                continue;
            }

            CompoundTag accountTag = new CompoundTag();
            accountTag.putUUID(PLAYER_ID_TAG, entry.getKey());
            accountTag.putLong(BALANCE_TAG, balance);
            accountsTag.add(accountTag);
        }

        tag.put(ACCOUNTS_TAG, accountsTag);
        return tag;
    }
}
