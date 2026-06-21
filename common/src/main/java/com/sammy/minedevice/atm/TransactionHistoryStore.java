package com.sammy.minedevice.atm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TransactionHistoryStore extends SavedData {
    private static final String DATA_NAME = "minedevice_atm_history";
    private static final String ENTRIES_TAG = "entries";
    private static final String PLAYER_TAG = "player";
    private static final String TXNS_TAG = "txns";
    private static final String TYPE_TAG = "type";
    private static final String AMOUNT_TAG = "amount";
    private static final String COUNTERPARTY_TAG = "counterparty";
    private static final String TIME_TAG = "time";
    private static final String BALANCE_TAG = "balance";
    private static final int MAX_HISTORY = 50;

    public static final byte TYPE_DEPOSIT = 0;
    public static final byte TYPE_WITHDRAW = 1;
    public static final byte TYPE_TRANSFER_OUT = 2;
    public static final byte TYPE_TRANSFER_IN = 3;

    private final Map<UUID, List<Transaction>> history = new HashMap<>();

    public static TransactionHistoryStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                TransactionHistoryStore::load,
                TransactionHistoryStore::new,
                DATA_NAME
        );
    }

    public static TransactionHistoryStore load(CompoundTag tag) {
        TransactionHistoryStore store = new TransactionHistoryStore();
        if (tag == null || !tag.contains(ENTRIES_TAG, Tag.TAG_LIST)) {
            return store;
        }
        ListTag entries = tag.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (!entry.hasUUID(PLAYER_TAG)) {
                continue;
            }
            UUID player = entry.getUUID(PLAYER_TAG);
            List<Transaction> txns = new ArrayList<>();
            ListTag txnList = entry.getList(TXNS_TAG, Tag.TAG_COMPOUND);
            for (int j = 0; j < txnList.size(); j++) {
                CompoundTag t = txnList.getCompound(j);
                Transaction txn = new Transaction();
                txn.type = t.getByte(TYPE_TAG);
                txn.amount = t.getLong(AMOUNT_TAG);
                txn.counterparty = t.getString(COUNTERPARTY_TAG);
                txn.time = t.getLong(TIME_TAG);
                txn.balanceAfter = t.getLong(BALANCE_TAG);
                txns.add(txn);
            }
            store.history.put(player, txns);
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        for (Map.Entry<UUID, List<Transaction>> e : history.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(PLAYER_TAG, e.getKey());
            ListTag txnList = new ListTag();
            for (Transaction t : e.getValue()) {
                CompoundTag ct = new CompoundTag();
                ct.putByte(TYPE_TAG, t.type);
                ct.putLong(AMOUNT_TAG, t.amount);
                ct.putString(COUNTERPARTY_TAG, t.counterparty);
                ct.putLong(TIME_TAG, t.time);
                ct.putLong(BALANCE_TAG, t.balanceAfter);
                txnList.add(ct);
            }
            entry.put(TXNS_TAG, txnList);
            entries.add(entry);
        }
        tag.put(ENTRIES_TAG, entries);
        return tag;
    }

    public void addTransaction(UUID player, byte type, long amount, String counterparty, long balanceAfter) {
        List<Transaction> txns = history.computeIfAbsent(player, k -> new ArrayList<>());
        Transaction t = new Transaction();
        t.type = type;
        t.amount = amount;
        t.counterparty = counterparty == null ? "" : counterparty;
        t.time = System.currentTimeMillis();
        t.balanceAfter = balanceAfter;
        txns.add(t);
        while (txns.size() > MAX_HISTORY) {
            txns.remove(0);
        }
        setDirty();
    }

    public List<Transaction> getPage(UUID player, int offset, int count) {
        List<Transaction> txns = history.get(player);
        List<Transaction> result = new ArrayList<>();
        if (txns == null || offset < 0 || offset >= txns.size()) {
            return result;
        }
        int end = Math.min(offset + count, txns.size());
        for (int i = offset; i < end; i++) {
            result.add(txns.get(i));
        }
        return result;
    }

    public int getCount(UUID player) {
        List<Transaction> txns = history.get(player);
        return txns == null ? 0 : txns.size();
    }

    public static final class Transaction {
        public byte type;
        public long amount;
        public String counterparty;
        public long time;
        public long balanceAfter;
    }
}
