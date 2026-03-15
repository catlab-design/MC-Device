package com.sammy.minedevice.phone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PhoneChatData {
    public static final int MAX_CHAT_FRIENDS = 8;
    public static final int MAX_CHAT_FRIEND_ID_LENGTH = 16;
    public static final int MAX_MESSAGES_PER_THREAD = 40;
    public static final int MAX_MESSAGE_LENGTH = 96;
    public static final String CHAT_FRIENDS_TAG = "chat_friends";
    private static final String CHAT_THREADS_TAG = "chat_threads";
    private static final String THREAD_NUMBER_TAG = "number";
    private static final String THREAD_NAME_TAG = "name";
    private static final String THREAD_PROFILE_ID_TAG = "profile_id";
    private static final String THREAD_MESSAGES_TAG = "messages";
    private static final String MESSAGE_TEXT_TAG = "text";
    private static final String MESSAGE_INCOMING_TAG = "incoming";
    private static final String FRIEND_PROFILE_ID_TAG = "profile_id";

    private PhoneChatData() {
    }

    public static List<PhoneContact> getFriends(ItemStack phoneStack) {
        if (phoneStack.isEmpty() || !phoneStack.hasTag()) {
            return List.of();
        }

        CompoundTag tag = phoneStack.getTag();
        return tag == null ? List.of() : getFriends(tag);
    }

    public static List<PhoneContact> getFriends(CompoundTag tag) {
        if (tag == null || !tag.contains(CHAT_FRIENDS_TAG, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag friendsTag = tag.getList(CHAT_FRIENDS_TAG, Tag.TAG_COMPOUND);
        if (friendsTag.isEmpty()) {
            return List.of();
        }

        List<PhoneContact> friends = new ArrayList<>(friendsTag.size());
        for (int i = friendsTag.size() - 1; i >= 0; i--) {
            CompoundTag friendTag = friendsTag.getCompound(i);
            String number = PhoneData.normalizePhoneNumber(friendTag.getString(PhoneData.CONTACT_NUMBER_TAG));
            if (!PhoneData.isValidPhoneNumber(number)) {
                continue;
            }

            String name = sanitizeFriendName(friendTag.getString(PhoneData.CONTACT_NAME_TAG), number);
            friends.add(new PhoneContact(name, number));
        }
        return friends;
    }

    public static boolean hasFriend(ItemStack phoneStack, String rawNumber) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (phoneStack.isEmpty() || !PhoneData.isValidPhoneNumber(number)) {
            return false;
        }

        for (PhoneContact friend : getFriends(phoneStack)) {
            if (friend.number().equals(number)) {
                return true;
            }
        }
        return false;
    }

    public static boolean addFriend(ItemStack phoneStack, String desiredName, String rawNumber) {
        return addFriend(phoneStack, desiredName, rawNumber, null);
    }

    public static boolean addFriend(ItemStack phoneStack, String desiredName, String rawNumber, UUID profileId) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (phoneStack.isEmpty() || !PhoneData.isValidPhoneNumber(number)) {
            return false;
        }

        return addFriend(phoneStack.getOrCreateTag(), desiredName, number, profileId);
    }

    public static boolean addFriend(CompoundTag tag, String desiredName, String rawNumber) {
        return addFriend(tag, desiredName, rawNumber, null);
    }

    public static boolean addFriend(CompoundTag tag, String desiredName, String rawNumber, UUID profileId) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (tag == null || !PhoneData.isValidPhoneNumber(number)) {
            return false;
        }

        ListTag friendsTag = tag.contains(CHAT_FRIENDS_TAG, Tag.TAG_LIST)
                ? tag.getList(CHAT_FRIENDS_TAG, Tag.TAG_COMPOUND)
                : new ListTag();

        for (int i = friendsTag.size() - 1; i >= 0; i--) {
            CompoundTag existing = friendsTag.getCompound(i);
            if (number.equals(PhoneData.normalizePhoneNumber(existing.getString(PhoneData.CONTACT_NUMBER_TAG)))) {
                if (profileId == null && existing.hasUUID(FRIEND_PROFILE_ID_TAG)) {
                    profileId = existing.getUUID(FRIEND_PROFILE_ID_TAG);
                }
                friendsTag.remove(i);
            }
        }

        CompoundTag friendTag = new CompoundTag();
        friendTag.putString(PhoneData.CONTACT_NAME_TAG, sanitizeFriendName(desiredName, number));
        friendTag.putString(PhoneData.CONTACT_NUMBER_TAG, number);
        if (profileId != null) {
            friendTag.putUUID(FRIEND_PROFILE_ID_TAG, profileId);
        }
        friendsTag.add(friendTag);

        while (friendsTag.size() > MAX_CHAT_FRIENDS) {
            friendsTag.remove(0);
        }

        tag.put(CHAT_FRIENDS_TAG, friendsTag);
        return true;
    }

    public static boolean removeConversation(ItemStack phoneStack, String rawNumber) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (phoneStack.isEmpty() || !PhoneData.isValidPhoneNumber(number) || !phoneStack.hasTag()) {
            return false;
        }

        CompoundTag tag = phoneStack.getTag();
        return tag != null && removeConversation(tag, number);
    }

    public static boolean removeConversation(CompoundTag tag, String rawNumber) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (tag == null || !PhoneData.isValidPhoneNumber(number)) {
            return false;
        }

        boolean removed = false;
        if (tag.contains(CHAT_FRIENDS_TAG, Tag.TAG_LIST)) {
            ListTag friendsTag = tag.getList(CHAT_FRIENDS_TAG, Tag.TAG_COMPOUND);
            for (int i = friendsTag.size() - 1; i >= 0; i--) {
                CompoundTag friendTag = friendsTag.getCompound(i);
                if (number.equals(PhoneData.normalizePhoneNumber(friendTag.getString(PhoneData.CONTACT_NUMBER_TAG)))) {
                    friendsTag.remove(i);
                    removed = true;
                }
            }

            if (friendsTag.isEmpty()) {
                tag.remove(CHAT_FRIENDS_TAG);
            } else {
                tag.put(CHAT_FRIENDS_TAG, friendsTag);
            }
        }

        if (tag.contains(CHAT_THREADS_TAG, Tag.TAG_LIST)) {
            ListTag threadsTag = tag.getList(CHAT_THREADS_TAG, Tag.TAG_COMPOUND);
            for (int i = threadsTag.size() - 1; i >= 0; i--) {
                CompoundTag threadTag = threadsTag.getCompound(i);
                if (number.equals(PhoneData.normalizePhoneNumber(threadTag.getString(THREAD_NUMBER_TAG)))) {
                    threadsTag.remove(i);
                    removed = true;
                }
            }

            if (threadsTag.isEmpty()) {
                tag.remove(CHAT_THREADS_TAG);
            } else {
                tag.put(CHAT_THREADS_TAG, threadsTag);
            }
        }

        return removed;
    }

    public static List<PhoneChatMessage> getMessages(ItemStack phoneStack, String rawNumber) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (phoneStack.isEmpty() || !phoneStack.hasTag() || !PhoneData.isValidPhoneNumber(number)) {
            return List.of();
        }

        CompoundTag tag = phoneStack.getTag();
        return tag == null ? List.of() : getMessages(tag, number);
    }

    public static List<PhoneChatMessage> getMessages(CompoundTag tag, String rawNumber) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (tag == null || !PhoneData.isValidPhoneNumber(number)) {
            return List.of();
        }

        CompoundTag threadTag = findThreadTag(tag, number);
        if (threadTag == null || !threadTag.contains(THREAD_MESSAGES_TAG, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag messagesTag = threadTag.getList(THREAD_MESSAGES_TAG, Tag.TAG_COMPOUND);
        List<PhoneChatMessage> messages = new ArrayList<>(messagesTag.size());
        for (int i = 0; i < messagesTag.size(); i++) {
            CompoundTag messageTag = messagesTag.getCompound(i);
            String text = sanitizeMessage(messageTag.getString(MESSAGE_TEXT_TAG));
            if (text.isEmpty()) {
                continue;
            }

            messages.add(new PhoneChatMessage(text, messageTag.getBoolean(MESSAGE_INCOMING_TAG)));
        }
        return messages;
    }

    public static String getLastMessagePreview(ItemStack phoneStack, String rawNumber) {
        PhoneChatMessage lastMessage = getLastMessage(phoneStack, rawNumber);
        return lastMessage == null ? "" : lastMessage.text();
    }

    public static PhoneChatMessage getLastMessage(ItemStack phoneStack, String rawNumber) {
        List<PhoneChatMessage> messages = getMessages(phoneStack, rawNumber);
        if (messages.isEmpty()) {
            return null;
        }

        return messages.get(messages.size() - 1);
    }

    public static boolean appendMessage(ItemStack phoneStack, String otherName, String rawNumber,
                                        String rawMessage, boolean incoming) {
        return appendMessage(phoneStack, otherName, rawNumber, rawMessage, incoming, null);
    }

    public static boolean appendMessage(ItemStack phoneStack, String otherName, String rawNumber,
                                        String rawMessage, boolean incoming, UUID profileId) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        String message = sanitizeMessage(rawMessage);
        if (phoneStack.isEmpty() || !PhoneData.isValidPhoneNumber(number) || message.isEmpty()) {
            return false;
        }

        CompoundTag tag = phoneStack.getOrCreateTag();
        addFriend(tag, otherName, number, profileId);

        ListTag threadsTag = tag.contains(CHAT_THREADS_TAG, Tag.TAG_LIST)
                ? tag.getList(CHAT_THREADS_TAG, Tag.TAG_COMPOUND)
                : new ListTag();

        CompoundTag threadTag = null;
        for (int i = threadsTag.size() - 1; i >= 0; i--) {
            CompoundTag existing = threadsTag.getCompound(i);
            if (number.equals(PhoneData.normalizePhoneNumber(existing.getString(THREAD_NUMBER_TAG)))) {
                threadTag = existing.copy();
                threadsTag.remove(i);
                break;
            }
        }

        if (threadTag == null) {
            threadTag = new CompoundTag();
        }

        threadTag.putString(THREAD_NUMBER_TAG, number);
        threadTag.putString(THREAD_NAME_TAG, sanitizeFriendName(otherName, number));
        if (profileId != null) {
            threadTag.putUUID(THREAD_PROFILE_ID_TAG, profileId);
        }

        ListTag messagesTag = threadTag.contains(THREAD_MESSAGES_TAG, Tag.TAG_LIST)
                ? threadTag.getList(THREAD_MESSAGES_TAG, Tag.TAG_COMPOUND)
                : new ListTag();

        CompoundTag messageTag = new CompoundTag();
        messageTag.putString(MESSAGE_TEXT_TAG, message);
        messageTag.putBoolean(MESSAGE_INCOMING_TAG, incoming);
        messagesTag.add(messageTag);

        while (messagesTag.size() > MAX_MESSAGES_PER_THREAD) {
            messagesTag.remove(0);
        }

        threadTag.put(THREAD_MESSAGES_TAG, messagesTag);
        threadsTag.add(threadTag);
        tag.put(CHAT_THREADS_TAG, threadsTag);
        return true;
    }

    public static String getFriendName(ItemStack phoneStack, String rawNumber) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (phoneStack.isEmpty() || !PhoneData.isValidPhoneNumber(number)) {
            return "";
        }

        for (PhoneContact friend : getFriends(phoneStack)) {
            if (friend.number().equals(number)) {
                return friend.displayName();
            }
        }

        CompoundTag tag = phoneStack.getTag();
        CompoundTag threadTag = tag == null ? null : findThreadTag(tag, number);
        if (threadTag == null) {
            return "";
        }

        return sanitizeFriendName(threadTag.getString(THREAD_NAME_TAG), number);
    }

    public static UUID getFriendProfileId(ItemStack phoneStack, String rawNumber) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (phoneStack.isEmpty() || !PhoneData.isValidPhoneNumber(number)) {
            return null;
        }

        CompoundTag tag = phoneStack.getTag();
        if (tag == null) {
            return null;
        }

        if (tag.contains(CHAT_FRIENDS_TAG, Tag.TAG_LIST)) {
            ListTag friendsTag = tag.getList(CHAT_FRIENDS_TAG, Tag.TAG_COMPOUND);
            for (int i = friendsTag.size() - 1; i >= 0; i--) {
                CompoundTag friendTag = friendsTag.getCompound(i);
                if (number.equals(PhoneData.normalizePhoneNumber(friendTag.getString(PhoneData.CONTACT_NUMBER_TAG)))
                        && friendTag.hasUUID(FRIEND_PROFILE_ID_TAG)) {
                    return friendTag.getUUID(FRIEND_PROFILE_ID_TAG);
                }
            }
        }

        CompoundTag threadTag = findThreadTag(tag, number);
        if (threadTag != null && threadTag.hasUUID(THREAD_PROFILE_ID_TAG)) {
            return threadTag.getUUID(THREAD_PROFILE_ID_TAG);
        }

        return null;
    }

    public static String sanitizeMessage(String rawMessage) {
        if (rawMessage == null) {
            return "";
        }

        String compact = rawMessage
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
        if (compact.isEmpty()) {
            return "";
        }

        int end = Math.min(compact.length(), MAX_MESSAGE_LENGTH);
        return compact.substring(0, end);
    }

    public static String sanitizeFriendId(String rawFriendId) {
        if (rawFriendId == null || rawFriendId.isBlank()) {
            return "";
        }

        String trimmed = rawFriendId.trim();
        StringBuilder builder = new StringBuilder(MAX_CHAT_FRIEND_ID_LENGTH);
        for (int i = 0; i < trimmed.length() && builder.length() < MAX_CHAT_FRIEND_ID_LENGTH; i++) {
            char ch = trimmed.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '_') {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    private static CompoundTag findThreadTag(CompoundTag tag, String number) {
        if (tag == null || !tag.contains(CHAT_THREADS_TAG, Tag.TAG_LIST)) {
            return null;
        }

        ListTag threadsTag = tag.getList(CHAT_THREADS_TAG, Tag.TAG_COMPOUND);
        for (int i = threadsTag.size() - 1; i >= 0; i--) {
            CompoundTag existing = threadsTag.getCompound(i);
            if (number.equals(PhoneData.normalizePhoneNumber(existing.getString(THREAD_NUMBER_TAG)))) {
                return existing;
            }
        }

        return null;
    }

    private static String sanitizeFriendName(String desiredName, String fallbackNumber) {
        String base = desiredName == null ? "" : desiredName.trim();
        if (base.isEmpty()) {
            base = fallbackNumber;
        }

        int end = Math.min(base.length(), PhoneData.MAX_CONTACT_NAME_LENGTH);
        return base.substring(0, end);
    }
}
