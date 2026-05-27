package com.sammy.minedevice.phone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PhonePendingMessageStore extends SavedData {
    private static final String DATA_NAME = "minedevice_phone_pending_messages";
    private static final String RECIPIENTS_TAG = "recipients";
    private static final String RECIPIENT_ID_TAG = "recipient_id";
    private static final String MESSAGES_TAG = "messages";
    private static final String SENDER_NAME_TAG = "sender_name";
    private static final String SENDER_NUMBER_TAG = "sender_number";
    private static final String SENDER_PROFILE_ID_TAG = "sender_profile_id";
    private static final String MESSAGE_TEXT_TAG = "text";
    private static final int MAX_PENDING_PER_RECIPIENT = 64;

    private final Map<UUID, List<PendingMessage>> messagesByRecipient = new ConcurrentHashMap<>();

    public static PhonePendingMessageStore get(MinecraftServer server) {
        if (server == null) {
            return new PhonePendingMessageStore();
        }

        if (ChatStorageManager.isAvailable()) {
            return new PhonePendingMessageStore();
        }

        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PhonePendingMessageStore::new, PhonePendingMessageStore::load, null),
                DATA_NAME
        );
    }

    public static PhonePendingMessageStore load(CompoundTag tag, HolderLookup.Provider registries) {
        PhonePendingMessageStore store = new PhonePendingMessageStore();
        if (tag == null || !tag.contains(RECIPIENTS_TAG, Tag.TAG_LIST)) {
            return store;
        }

        ListTag recipientsTag = tag.getList(RECIPIENTS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < recipientsTag.size(); i++) {
            CompoundTag recipientTag = recipientsTag.getCompound(i);
            if (!recipientTag.hasUUID(RECIPIENT_ID_TAG) || !recipientTag.contains(MESSAGES_TAG, Tag.TAG_LIST)) {
                continue;
            }

            UUID recipientId = recipientTag.getUUID(RECIPIENT_ID_TAG);
            ListTag messagesTag = recipientTag.getList(MESSAGES_TAG, Tag.TAG_COMPOUND);
            List<PendingMessage> messages = new ArrayList<>(messagesTag.size());
            for (int messageIndex = 0; messageIndex < messagesTag.size(); messageIndex++) {
                CompoundTag messageTag = messagesTag.getCompound(messageIndex);
                String senderNumber = PhoneData.normalizePhoneNumber(messageTag.getString(SENDER_NUMBER_TAG));
                String messageText = PhoneChatData.sanitizeMessage(messageTag.getString(MESSAGE_TEXT_TAG));
                if (!PhoneData.isValidPhoneNumber(senderNumber) || messageText.isEmpty()) {
                    continue;
                }

                String senderName = sanitizeSenderName(messageTag.getString(SENDER_NAME_TAG), senderNumber);
                UUID senderProfileId = messageTag.hasUUID(SENDER_PROFILE_ID_TAG)
                        ? messageTag.getUUID(SENDER_PROFILE_ID_TAG)
                        : null;
                messages.add(new PendingMessage(senderName, senderNumber, senderProfileId, messageText));
            }

            if (!messages.isEmpty()) {
                store.messagesByRecipient.put(recipientId, messages);
            }
        }

        return store;
    }

    public boolean hasPendingMessages() {
        return !messagesByRecipient.isEmpty();
    }

    public boolean hasPendingMessages(UUID recipientId) {
        if (recipientId == null) {
            return false;
        }

        List<PendingMessage> messages = messagesByRecipient.get(recipientId);
        return messages != null && !messages.isEmpty();
    }

    public boolean queueMessage(UUID recipientId, String senderName, String senderNumber,
                                UUID senderProfileId, String messageText) {
        String normalizedNumber = PhoneData.normalizePhoneNumber(senderNumber);
        String normalizedMessage = PhoneChatData.sanitizeMessage(messageText);
        if (recipientId == null || !PhoneData.isValidPhoneNumber(normalizedNumber) || normalizedMessage.isEmpty()) {
            return false;
        }

        if (ChatStorageManager.isAvailable()) {
            ChatStorageManager.getInstance().addMessage(
                recipientId, normalizedNumber, normalizedMessage, true,
                sanitizeSenderName(senderName, normalizedNumber), senderProfileId
            );
            return true;
        }

        List<PendingMessage> messages = messagesByRecipient.computeIfAbsent(recipientId, ignored -> new ArrayList<>());
        messages.add(new PendingMessage(
                sanitizeSenderName(senderName, normalizedNumber),
                normalizedNumber,
                senderProfileId,
                normalizedMessage
        ));
        while (messages.size() > MAX_PENDING_PER_RECIPIENT) {
            messages.remove(0);
        }

        setDirty();
        return true;
    }

    public List<PendingMessage> getPendingMessages(UUID recipientId) {
        if (recipientId == null) {
            return List.of();
        }

        List<PendingMessage> messages = messagesByRecipient.get(recipientId);
        return messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
    }

    public boolean clearMessagesForSender(UUID recipientId, String senderNumber) {
        String normalizedNumber = PhoneData.normalizePhoneNumber(senderNumber);
        if (recipientId == null || !PhoneData.isValidPhoneNumber(normalizedNumber)) {
            return false;
        }

        if (ChatStorageManager.isAvailable()) {
            ChatStorageManager.getInstance().removeConversation(recipientId, normalizedNumber);
            return true;
        }

        List<PendingMessage> messages = messagesByRecipient.get(recipientId);
        if (messages == null || messages.isEmpty()) {
            return false;
        }

        boolean removed = messages.removeIf(message -> normalizedNumber.equals(message.senderNumber()));
        if (!removed) {
            return false;
        }

        if (messages.isEmpty()) {
            messagesByRecipient.remove(recipientId);
        }
        setDirty();
        return true;
    }

    public void clearPendingMessages(UUID recipientId) {
        if (recipientId == null) {
            return;
        }

        if (ChatStorageManager.isAvailable()) {
            ChatStorageManager.getInstance().clearCacheForPlayer(recipientId);
            return;
        }

        if (messagesByRecipient.remove(recipientId) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag recipientsTag = new ListTag();
        for (Map.Entry<UUID, List<PendingMessage>> entry : messagesByRecipient.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            CompoundTag recipientTag = new CompoundTag();
            recipientTag.putUUID(RECIPIENT_ID_TAG, entry.getKey());

            ListTag messagesTag = new ListTag();
            for (PendingMessage message : entry.getValue()) {
                if (!PhoneData.isValidPhoneNumber(message.senderNumber()) || message.messageText().isEmpty()) {
                    continue;
                }

                CompoundTag messageTag = new CompoundTag();
                messageTag.putString(SENDER_NAME_TAG, sanitizeSenderName(message.senderName(), message.senderNumber()));
                messageTag.putString(SENDER_NUMBER_TAG, message.senderNumber());
                if (message.senderProfileId() != null) {
                    messageTag.putUUID(SENDER_PROFILE_ID_TAG, message.senderProfileId());
                }
                messageTag.putString(MESSAGE_TEXT_TAG, message.messageText());
                messagesTag.add(messageTag);
            }

            if (!messagesTag.isEmpty()) {
                recipientTag.put(MESSAGES_TAG, messagesTag);
                recipientsTag.add(recipientTag);
            }
        }

        tag.put(RECIPIENTS_TAG, recipientsTag);
        return tag;
    }

    private static String sanitizeSenderName(String senderName, String fallbackNumber) {
        String base = senderName == null ? "" : senderName.trim();
        if (base.isEmpty()) {
            base = fallbackNumber;
        }

        int end = Math.min(base.length(), PhoneData.MAX_CONTACT_NAME_LENGTH);
        return base.substring(0, end);
    }

    public record PendingMessage(String senderName, String senderNumber, UUID senderProfileId, String messageText) {
    }
}
