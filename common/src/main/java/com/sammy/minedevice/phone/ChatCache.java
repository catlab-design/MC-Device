package com.sammy.minedevice.phone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public final class ChatCache {
    private static final int MAX_CONVERSATIONS_PER_PLAYER = 24;
    private static final int MAX_CACHE_SIZE_PER_CONVERSATION = Math.max(64, PhoneChatData.MAX_MESSAGES_PER_THREAD);
    private static final long PLAYER_CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(10);

    private final ConcurrentHashMap<UUID, PlayerCache> playerCaches = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<BatchOperation> pendingOperations = new ConcurrentLinkedQueue<>();
    private final ReentrantLock saveLock = new ReentrantLock();
    private final AtomicInteger pendingMessageCount = new AtomicInteger();

    public void cacheMessage(UUID ownerUuid, String otherNumber, String messageText,
                             boolean isIncoming, String otherName, UUID otherProfileId) {
        String normalizedNumber = PhoneData.normalizePhoneNumber(otherNumber);
        String sanitizedMessage = PhoneChatData.sanitizeMessage(messageText);
        if (ownerUuid == null || !PhoneData.isValidPhoneNumber(normalizedNumber) || sanitizedMessage.isEmpty()) {
            return;
        }

        PlayerCache playerCache = getOrCreatePlayerCache(ownerUuid);
        playerCache.appendMessage(normalizedNumber, new PhoneChatMessage(sanitizedMessage, isIncoming));
        if ((otherName != null && !otherName.isBlank()) || otherProfileId != null) {
            boolean friendChanged = playerCache.upsertFriend(normalizedNumber, otherName, otherProfileId);
            if (friendChanged) {
                pendingOperations.add(new BatchOperation(
                        BatchOperation.Type.ADD_FRIEND,
                        ownerUuid,
                        normalizedNumber,
                        null,
                        false,
                        sanitizeFriendName(otherName, normalizedNumber),
                        otherProfileId
                ));
            }
        }

        pendingOperations.add(new BatchOperation(
                BatchOperation.Type.ADD_MESSAGE,
                ownerUuid,
                normalizedNumber,
                sanitizedMessage,
                isIncoming,
                sanitizeFriendName(otherName, normalizedNumber),
                otherProfileId
        ));
        pendingMessageCount.incrementAndGet();
    }

    public void cacheFriend(UUID ownerUuid, String friendNumber, String friendName, UUID profileId) {
        String normalizedNumber = PhoneData.normalizePhoneNumber(friendNumber);
        if (ownerUuid == null || !PhoneData.isValidPhoneNumber(normalizedNumber)) {
            return;
        }

        PlayerCache playerCache = getOrCreatePlayerCache(ownerUuid);
        if (!playerCache.upsertFriend(normalizedNumber, friendName, profileId)) {
            return;
        }

        pendingOperations.add(new BatchOperation(
                BatchOperation.Type.ADD_FRIEND,
                ownerUuid,
                normalizedNumber,
                null,
                false,
                sanitizeFriendName(friendName, normalizedNumber),
                profileId
        ));
    }

    public void rememberFriend(UUID ownerUuid, String friendNumber, String friendName, UUID profileId) {
        String normalizedNumber = PhoneData.normalizePhoneNumber(friendNumber);
        if (ownerUuid == null || !PhoneData.isValidPhoneNumber(normalizedNumber)) {
            return;
        }

        getOrCreatePlayerCache(ownerUuid).upsertFriend(normalizedNumber, friendName, profileId);
    }

    public void cacheFriends(UUID ownerUuid, List<ChatDatabase.StoredFriend> friends) {
        if (ownerUuid == null) {
            return;
        }

        getOrCreatePlayerCache(ownerUuid).markFriendsLoaded(friends);
    }

    public void cacheConversation(UUID ownerUuid, String otherNumber, List<PhoneChatMessage> messages,
                                  String otherName, UUID profileId) {
        String normalizedNumber = PhoneData.normalizePhoneNumber(otherNumber);
        if (ownerUuid == null || !PhoneData.isValidPhoneNumber(normalizedNumber)) {
            return;
        }

        getOrCreatePlayerCache(ownerUuid).markConversationLoaded(
                normalizedNumber,
                messages == null ? List.of() : messages,
                otherName,
                profileId
        );
    }

    public boolean hasCachedFriends(UUID ownerUuid) {
        PlayerCache cache = playerCaches.get(ownerUuid);
        return cache != null && cache.areFriendsLoaded();
    }

    public boolean hasCachedMessages(UUID ownerUuid, String otherNumber) {
        PlayerCache cache = playerCaches.get(ownerUuid);
        return cache != null && cache.isConversationLoaded(otherNumber);
    }

    public List<PhoneChatMessage> getCachedMessages(UUID ownerUuid, String otherNumber, int limit) {
        PlayerCache cache = playerCaches.get(ownerUuid);
        return cache == null ? List.of() : cache.getConversationMessages(otherNumber, limit);
    }

    public List<PhoneContact> getCachedFriends(UUID ownerUuid) {
        PlayerCache cache = playerCaches.get(ownerUuid);
        return cache == null ? List.of() : cache.getFriends();
    }

    public String getCachedFriendName(UUID ownerUuid, String friendNumber) {
        PlayerCache cache = playerCaches.get(ownerUuid);
        return cache == null ? null : cache.getFriendName(friendNumber);
    }

    public UUID getCachedFriendProfileId(UUID ownerUuid, String friendNumber) {
        PlayerCache cache = playerCaches.get(ownerUuid);
        return cache == null ? null : cache.getFriendProfileId(friendNumber);
    }

    public void removeCachedFriend(UUID ownerUuid, String friendNumber) {
        String normalizedNumber = PhoneData.normalizePhoneNumber(friendNumber);
        if (ownerUuid == null || !PhoneData.isValidPhoneNumber(normalizedNumber)) {
            return;
        }

        PlayerCache cache = playerCaches.get(ownerUuid);
        if (cache != null) {
            cache.removeFriend(normalizedNumber);
        }

        pendingOperations.add(new BatchOperation(
                BatchOperation.Type.REMOVE_FRIEND,
                ownerUuid,
                normalizedNumber,
                null,
                false,
                null,
                null
        ));
    }

    public void removeCachedConversation(UUID ownerUuid, String otherNumber) {
        String normalizedNumber = PhoneData.normalizePhoneNumber(otherNumber);
        if (ownerUuid == null || !PhoneData.isValidPhoneNumber(normalizedNumber)) {
            return;
        }

        PlayerCache cache = playerCaches.get(ownerUuid);
        if (cache != null) {
            cache.removeConversation(normalizedNumber);
            cache.removeFriend(normalizedNumber);
        }

        pendingOperations.add(new BatchOperation(
                BatchOperation.Type.REMOVE_CONVERSATION,
                ownerUuid,
                normalizedNumber,
                null,
                false,
                null,
                null
        ));
    }

    public List<BatchOperation> getAndClearPendingOperations() {
        saveLock.lock();
        try {
            List<BatchOperation> operations = new ArrayList<>(pendingOperations);
            pendingOperations.clear();
            pendingMessageCount.set(0);
            return operations;
        } finally {
            saveLock.unlock();
        }
    }

    public int getPendingMessageCount() {
        return pendingMessageCount.get();
    }

    public boolean hasPendingOperations() {
        return !pendingOperations.isEmpty();
    }

    public void restorePendingOperations(List<BatchOperation> operations) {
        if (operations == null || operations.isEmpty()) {
            return;
        }

        for (BatchOperation operation : operations) {
            pendingOperations.add(operation);
            if (operation.type() == BatchOperation.Type.ADD_MESSAGE) {
                pendingMessageCount.incrementAndGet();
            }
        }
    }

    public void updateCachedFriendProfileId(UUID playerUuid, String phoneNumber) {
        String normNumber = PhoneData.normalizePhoneNumber(phoneNumber);
        for (PlayerCache playerCache : playerCaches.values()) {
            playerCache.updateFriendProfileIdIfNull(normNumber, playerUuid);
        }
    }

    public void clearCacheForPlayer(UUID ownerUuid) {
        if (ownerUuid != null) {
            playerCaches.remove(ownerUuid);
        }
    }

    public void clearAllCache() {
        playerCaches.clear();
        pendingOperations.clear();
        pendingMessageCount.set(0);
    }

    public void pruneExpiredPlayers() {
        long now = System.currentTimeMillis();
        playerCaches.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private PlayerCache getOrCreatePlayerCache(UUID ownerUuid) {
        return playerCaches.computeIfAbsent(ownerUuid, ignored -> new PlayerCache());
    }

    private static String sanitizeFriendName(String desiredName, String fallbackNumber) {
        String base = desiredName == null ? "" : desiredName.trim();
        if (base.isEmpty()) {
            base = fallbackNumber;
        }

        int end = Math.min(base.length(), PhoneData.MAX_CONTACT_NAME_LENGTH);
        return base.substring(0, end);
    }

    public record BatchOperation(Type type, UUID ownerUuid, String otherNumber, String messageText,
                                 boolean isIncoming, String otherName, UUID otherProfileId) {
        public enum Type {
            ADD_MESSAGE,
            ADD_FRIEND,
            REMOVE_FRIEND,
            REMOVE_CONVERSATION
        }
    }

    private static final class PlayerCache {
        private final Map<String, FriendEntry> friendsByNumber = new LinkedHashMap<>();
        private final Map<String, ConversationCache> conversationsByNumber = new LinkedHashMap<>();
        private boolean friendsLoaded;
        private long lastAccessMillis = System.currentTimeMillis();

        synchronized boolean upsertFriend(String otherNumber, String otherName, UUID otherProfileId) {
            touch();
            String normalizedNumber = PhoneData.normalizePhoneNumber(otherNumber);
            String displayName = sanitizeFriendName(otherName, normalizedNumber);
            FriendEntry existing = friendsByNumber.get(normalizedNumber);
            if (existing != null
                    && existing.displayName().equals(displayName)
                    && java.util.Objects.equals(existing.profileId(), otherProfileId)) {
                return false;
            }

            friendsByNumber.put(normalizedNumber, new FriendEntry(displayName, normalizedNumber, otherProfileId));
            return true;
        }

        synchronized void markFriendsLoaded(List<ChatDatabase.StoredFriend> friends) {
            touch();
            Map<String, FriendEntry> merged = new LinkedHashMap<>(friendsByNumber);
            for (ChatDatabase.StoredFriend friend : friends) {
                String normalizedNumber = PhoneData.normalizePhoneNumber(friend.number());
                if (!PhoneData.isValidPhoneNumber(normalizedNumber)) {
                    continue;
                }

                merged.put(normalizedNumber, new FriendEntry(
                        sanitizeFriendName(friend.displayName(), normalizedNumber),
                        normalizedNumber,
                        friend.profileId()
                ));
            }

            friendsByNumber.clear();
            friendsByNumber.putAll(merged);
            friendsLoaded = true;
        }

        synchronized void appendMessage(String otherNumber, PhoneChatMessage message) {
            touch();
            ConversationCache conversation = conversationsByNumber.computeIfAbsent(
                    otherNumber,
                    ignored -> new ConversationCache()
            );
            conversation.append(message);
            ensureConversationLimit();
        }

        synchronized void markConversationLoaded(String otherNumber, List<PhoneChatMessage> storedMessages,
                                                 String otherName, UUID profileId) {
            touch();
            ConversationCache conversation = conversationsByNumber.computeIfAbsent(
                    otherNumber,
                    ignored -> new ConversationCache()
            );
            conversation.markLoaded(storedMessages);
            if ((otherName != null && !otherName.isBlank()) || profileId != null) {
                upsertFriend(otherNumber, otherName, profileId);
            }
            ensureConversationLimit();
        }

        synchronized boolean areFriendsLoaded() {
            touch();
            return friendsLoaded;
        }

        synchronized boolean isConversationLoaded(String otherNumber) {
            touch();
            ConversationCache conversation = conversationsByNumber.get(PhoneData.normalizePhoneNumber(otherNumber));
            return conversation != null && conversation.loaded;
        }

        synchronized List<PhoneContact> getFriends() {
            touch();
            if (!friendsLoaded) {
                return List.of();
            }

            List<PhoneContact> contacts = new ArrayList<>(friendsByNumber.size());
            for (FriendEntry friend : friendsByNumber.values()) {
                contacts.add(new PhoneContact(friend.displayName(), friend.number()));
            }
            return contacts;
        }

        synchronized List<PhoneChatMessage> getConversationMessages(String otherNumber, int limit) {
            touch();
            ConversationCache conversation = conversationsByNumber.get(PhoneData.normalizePhoneNumber(otherNumber));
            if (conversation == null || !conversation.loaded) {
                return List.of();
            }
            return conversation.latest(limit);
        }

        synchronized String getFriendName(String otherNumber) {
            touch();
            String normalizedNumber = PhoneData.normalizePhoneNumber(otherNumber);
            FriendEntry friend = friendsByNumber.get(normalizedNumber);
            return friend == null ? null : friend.displayName();
        }

        synchronized UUID getFriendProfileId(String otherNumber) {
            touch();
            FriendEntry friend = friendsByNumber.get(PhoneData.normalizePhoneNumber(otherNumber));
            return friend == null ? null : friend.profileId();
        }

        synchronized void removeFriend(String otherNumber) {
            touch();
            friendsByNumber.remove(PhoneData.normalizePhoneNumber(otherNumber));
        }

        synchronized void updateFriendProfileIdIfNull(String friendNumber, UUID profileId) {
            touch();
            String normalizedNumber = PhoneData.normalizePhoneNumber(friendNumber);
            FriendEntry existing = friendsByNumber.get(normalizedNumber);
            if (existing != null && existing.profileId() == null) {
                friendsByNumber.put(normalizedNumber, new FriendEntry(existing.displayName(), normalizedNumber, profileId));
            }
        }

        synchronized void removeConversation(String otherNumber) {
            touch();
            conversationsByNumber.remove(PhoneData.normalizePhoneNumber(otherNumber));
        }

        synchronized boolean isExpired(long now) {
            return now - lastAccessMillis > PLAYER_CACHE_TTL_MILLIS;
        }

        private void ensureConversationLimit() {
            while (conversationsByNumber.size() > MAX_CONVERSATIONS_PER_PLAYER) {
                String eldest = conversationsByNumber.keySet().iterator().next();
                conversationsByNumber.remove(eldest);
            }
        }

        private void touch() {
            lastAccessMillis = System.currentTimeMillis();
        }
    }

    private record FriendEntry(String displayName, String number, UUID profileId) {
    }

    private static final class ConversationCache {
        private final List<PhoneChatMessage> messages = new ArrayList<>();
        private boolean loaded;

        void append(PhoneChatMessage message) {
            messages.add(message);
            trim();
        }

        void markLoaded(List<PhoneChatMessage> storedMessages) {
            if (!loaded) {
                List<PhoneChatMessage> pendingMessages = new ArrayList<>(messages);
                messages.clear();
                for (PhoneChatMessage message : storedMessages) {
                    if (message != null && !message.text().isBlank()) {
                        messages.add(new PhoneChatMessage(message.text(), message.incoming()));
                    }
                }
                messages.addAll(pendingMessages);
                loaded = true;
                trim();
                return;
            }

            if (messages.isEmpty()) {
                for (PhoneChatMessage message : storedMessages) {
                    if (message != null && !message.text().isBlank()) {
                        messages.add(new PhoneChatMessage(message.text(), message.incoming()));
                    }
                }
                trim();
            }
        }

        List<PhoneChatMessage> latest(int limit) {
            if (messages.isEmpty()) {
                return List.of();
            }

            int effectiveLimit = Math.max(1, limit);
            int startIndex = Math.max(0, messages.size() - effectiveLimit);
            return List.copyOf(messages.subList(startIndex, messages.size()));
        }

        private void trim() {
            while (messages.size() > MAX_CACHE_SIZE_PER_CONVERSATION) {
                messages.remove(0);
            }
        }
    }
}
