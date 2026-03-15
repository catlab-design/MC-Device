package com.sammy.minedevice.phone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public final class PhonePendingConversationDeleteStore extends SavedData {
    private static final String DATA_NAME = "minedevice_phone_pending_conversation_deletes";
    private static final String RECIPIENTS_TAG = "recipients";
    private static final String RECIPIENT_ID_TAG = "recipient_id";
    private static final String NUMBERS_TAG = "numbers";

    private final Map<UUID, Set<String>> pendingDeletesByRecipient = new HashMap<>();

    public static PhonePendingConversationDeleteStore get(MinecraftServer server) {
        if (server == null) {
            return new PhonePendingConversationDeleteStore();
        }

        return server.overworld().getDataStorage().computeIfAbsent(
                PhonePendingConversationDeleteStore::load,
                PhonePendingConversationDeleteStore::new,
                DATA_NAME
        );
    }

    public static PhonePendingConversationDeleteStore load(CompoundTag tag) {
        PhonePendingConversationDeleteStore store = new PhonePendingConversationDeleteStore();
        if (tag == null || !tag.contains(RECIPIENTS_TAG, Tag.TAG_LIST)) {
            return store;
        }

        ListTag recipientsTag = tag.getList(RECIPIENTS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < recipientsTag.size(); i++) {
            CompoundTag recipientTag = recipientsTag.getCompound(i);
            if (!recipientTag.hasUUID(RECIPIENT_ID_TAG) || !recipientTag.contains(NUMBERS_TAG, Tag.TAG_LIST)) {
                continue;
            }

            UUID recipientId = recipientTag.getUUID(RECIPIENT_ID_TAG);
            ListTag numbersTag = recipientTag.getList(NUMBERS_TAG, Tag.TAG_STRING);
            Set<String> numbers = new LinkedHashSet<>();
            for (int numberIndex = 0; numberIndex < numbersTag.size(); numberIndex++) {
                String number = PhoneData.normalizePhoneNumber(numbersTag.getString(numberIndex));
                if (PhoneData.isValidPhoneNumber(number)) {
                    numbers.add(number);
                }
            }

            if (!numbers.isEmpty()) {
                store.pendingDeletesByRecipient.put(recipientId, numbers);
            }
        }

        return store;
    }

    public boolean hasPendingDeletes() {
        return !pendingDeletesByRecipient.isEmpty();
    }

    public boolean hasPendingDeletes(UUID recipientId) {
        if (recipientId == null) {
            return false;
        }

        Set<String> numbers = pendingDeletesByRecipient.get(recipientId);
        return numbers != null && !numbers.isEmpty();
    }

    public boolean queueDeletion(UUID recipientId, String targetNumber) {
        String normalizedNumber = PhoneData.normalizePhoneNumber(targetNumber);
        if (recipientId == null || !PhoneData.isValidPhoneNumber(normalizedNumber)) {
            return false;
        }

        Set<String> numbers = pendingDeletesByRecipient.computeIfAbsent(recipientId, ignored -> new LinkedHashSet<>());
        boolean added = numbers.add(normalizedNumber);
        if (added) {
            setDirty();
        }
        return added;
    }

    public List<String> getPendingDeletes(UUID recipientId) {
        if (recipientId == null) {
            return List.of();
        }

        Set<String> numbers = pendingDeletesByRecipient.get(recipientId);
        return numbers == null || numbers.isEmpty() ? List.of() : new ArrayList<>(numbers);
    }

    public void clearPendingDeletes(UUID recipientId) {
        if (recipientId == null) {
            return;
        }

        if (pendingDeletesByRecipient.remove(recipientId) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag recipientsTag = new ListTag();
        for (Map.Entry<UUID, Set<String>> entry : pendingDeletesByRecipient.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            CompoundTag recipientTag = new CompoundTag();
            recipientTag.putUUID(RECIPIENT_ID_TAG, entry.getKey());
            ListTag numbersTag = new ListTag();
            for (String number : entry.getValue()) {
                if (PhoneData.isValidPhoneNumber(number)) {
                    numbersTag.add(StringTag.valueOf(number));
                }
            }

            if (!numbersTag.isEmpty()) {
                recipientTag.put(NUMBERS_TAG, numbersTag);
                recipientsTag.add(recipientTag);
            }
        }

        tag.put(RECIPIENTS_TAG, recipientsTag);
        return tag;
    }
}
