package com.sammy.minedevice.phone;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ChatStorageManager {
    private static final int BATCH_SIZE_THRESHOLD = 50;
    private static final int SAVE_INTERVAL_SECONDS = 15;

    private static final Object LOCK = new Object();
    private static ChatStorageManager instance;

    private final ChatDatabase database;
    private final ChatCache cache;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running;

    private ChatStorageManager(String dbPath) {
        this.database = new ChatDatabase(dbPath);
        this.cache = new ChatCache();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "MineDevice-ChatStorage");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static ChatStorageManager getInstance() {
        synchronized (LOCK) {
            if (instance == null) {
                throw new IllegalStateException("ChatStorageManager not initialized. Call init() first.");
            }
            return instance;
        }
    }

    public static ChatStorageManager getInstanceOrNull() {
        synchronized (LOCK) {
            return instance;
        }
    }

    public static boolean isAvailable() {
        synchronized (LOCK) {
            return instance != null;
        }
    }

    public static void init(String dbPath) {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new ChatStorageManager(dbPath);
            }
        }
    }

    public static void shutdown() {
        synchronized (LOCK) {
            if (instance == null) {
                return;
            }

            instance.stop();
            instance.database.close();
            instance.cache.clearAllCache();
            instance = null;
        }
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;
        scheduler.scheduleAtFixedRate(this::flushAndPrune, SAVE_INTERVAL_SECONDS, SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        scheduler.shutdown();
        try {
            flushAndPrune();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void addMessage(UUID ownerUuid, String otherNumber, String messageText,
                           boolean isIncoming, String otherName, UUID otherProfileId) {
        cache.cacheMessage(ownerUuid, otherNumber, messageText, isIncoming, otherName, otherProfileId);
        if (cache.getPendingMessageCount() >= BATCH_SIZE_THRESHOLD) {
            saveBatch();
        }
    }

    public void addFriend(UUID ownerUuid, String friendNumber, String friendName, UUID profileId) {
        cache.cacheFriend(ownerUuid, friendNumber, friendName, profileId);
    }

    public List<PhoneChatMessage> getMessages(UUID ownerUuid, String otherNumber, int limit) {
        if (cache.hasCachedMessages(ownerUuid, otherNumber)) {
            return cache.getCachedMessages(ownerUuid, otherNumber, limit);
        }

        UUID profileId = database.getFriendProfileId(ownerUuid, otherNumber);
        String friendName = database.getFriendName(ownerUuid, otherNumber);
        List<PhoneChatMessage> dbMessages = database.getMessages(ownerUuid, otherNumber, limit);
        cache.cacheConversation(ownerUuid, otherNumber, dbMessages, friendName, profileId);
        return cache.getCachedMessages(ownerUuid, otherNumber, limit);
    }

    public List<PhoneContact> getFriends(UUID ownerUuid) {
        if (cache.hasCachedFriends(ownerUuid)) {
            return cache.getCachedFriends(ownerUuid);
        }

        List<ChatDatabase.StoredFriend> dbFriends = database.getFriends(ownerUuid);
        cache.cacheFriends(ownerUuid, dbFriends);
        return cache.getCachedFriends(ownerUuid);
    }

    public String getFriendName(UUID ownerUuid, String friendNumber) {
        String cached = cache.getCachedFriendName(ownerUuid, friendNumber);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        String dbValue = database.getFriendName(ownerUuid, friendNumber);
        if (dbValue != null && !dbValue.isBlank()) {
            cache.rememberFriend(ownerUuid, friendNumber, dbValue, database.getFriendProfileId(ownerUuid, friendNumber));
            return dbValue;
        }

        return "";
    }

    public UUID getFriendProfileId(UUID ownerUuid, String friendNumber) {
        UUID cached = cache.getCachedFriendProfileId(ownerUuid, friendNumber);
        if (cached != null) {
            return cached;
        }

        UUID dbValue = database.getFriendProfileId(ownerUuid, friendNumber);
        if (dbValue != null) {
            cache.rememberFriend(ownerUuid, friendNumber, database.getFriendName(ownerUuid, friendNumber), dbValue);
        }
        return dbValue;
    }

    public void removeFriend(UUID ownerUuid, String friendNumber) {
        cache.removeCachedFriend(ownerUuid, friendNumber);
        database.removeFriend(ownerUuid, friendNumber);
    }

    public void removeConversation(UUID ownerUuid, String otherNumber) {
        cache.removeCachedConversation(ownerUuid, otherNumber);
        database.removeConversation(ownerUuid, otherNumber);
    }

    public void clearCacheForPlayer(UUID ownerUuid) {
        cache.clearCacheForPlayer(ownerUuid);
    }

    public PhoneChatStatePayload createPayload(UUID ownerUuid) {
        List<PhoneContact> friends = getFriends(ownerUuid);
        List<PhoneChatStatePayload.FriendEntry> friendEntries = new ArrayList<>(friends.size());
        List<PhoneChatStatePayload.ConversationEntry> conversationEntries = new ArrayList<>(friends.size());

        for (PhoneContact friend : friends) {
            String number = friend.number();
            String displayName = friend.displayName();
            UUID profileId = getFriendProfileId(ownerUuid, number);
            friendEntries.add(new PhoneChatStatePayload.FriendEntry(displayName, number, profileId));
            conversationEntries.add(new PhoneChatStatePayload.ConversationEntry(
                    number,
                    displayName,
                    profileId,
                    getMessages(ownerUuid, number, PhoneChatData.MAX_MESSAGES_PER_THREAD)
            ));
        }

        return new PhoneChatStatePayload(getOwnNickname(ownerUuid), friendEntries, conversationEntries);
    }

    public boolean isRunning() {
        return running;
    }

    private void flushAndPrune() {
        saveBatch();
        cache.pruneExpiredPlayers();
    }

    private void saveBatch() {
        if (!cache.hasPendingOperations()) {
            return;
        }

        List<ChatCache.BatchOperation> operations = cache.getAndClearPendingOperations();
        if (operations.isEmpty()) {
            return;
        }

        try {
            database.applyBatch(operations);
        } catch (RuntimeException exception) {
            cache.restorePendingOperations(operations);
            System.err.println("Error saving chat batch: " + exception.getMessage());
        }
    }

    public String getOwnNickname(UUID ownerUuid) {
        return database.getNickname(ownerUuid);
    }

    public void setOwnNickname(UUID ownerUuid, String nickname) {
        database.setNickname(ownerUuid, nickname);
    }

    public void updateFriendProfileId(UUID playerUuid, String phoneNumber) {
        database.updateFriendProfileId(playerUuid, phoneNumber);
        cache.updateCachedFriendProfileId(playerUuid, phoneNumber);
    }
}
