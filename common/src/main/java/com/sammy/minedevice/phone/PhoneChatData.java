package com.sammy.minedevice.phone;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public final class PhoneChatData {
    public static final int MAX_CHAT_FRIENDS = 8;
    public static final int MAX_CHAT_FRIEND_ID_LENGTH = 16;
    public static final int MAX_MESSAGES_PER_THREAD = 40;
    public static final int MAX_MESSAGE_LENGTH = 96;

    private PhoneChatData() {
    }

    public static List<PhoneContact> getFriends(ItemStack phoneStack) {
        return List.of();
    }

    public static List<PhoneContact> getFriends(ItemStack phoneStack, Player player) {
        if (player == null || !ChatStorageManager.isAvailable()) {
            return List.of();
        }

        return ChatStorageManager.getInstance().getFriends(player.getUUID());
    }

    public static boolean hasFriend(ItemStack phoneStack, String rawNumber) {
        return false;
    }

    public static boolean hasFriend(ItemStack phoneStack, String rawNumber, Player player) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (player == null || !PhoneData.isValidPhoneNumber(number) || !ChatStorageManager.isAvailable()) {
            return false;
        }

        for (PhoneContact friend : ChatStorageManager.getInstance().getFriends(player.getUUID())) {
            if (friend.number().equals(number)) {
                return true;
            }
        }
        return false;
    }

    public static boolean addFriend(ItemStack phoneStack, String desiredName, String rawNumber) {
        return false;
    }

    public static boolean addFriend(ItemStack phoneStack, String desiredName, String rawNumber, UUID profileId) {
        return false;
    }

    public static boolean addFriend(ItemStack phoneStack, String desiredName, String rawNumber, UUID profileId, Player player) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (player == null || !PhoneData.isValidPhoneNumber(number) || !ChatStorageManager.isAvailable()) {
            return false;
        }

        ChatStorageManager.getInstance().addFriend(player.getUUID(), number, desiredName, profileId);
        return true;
    }

    public static boolean removeConversation(ItemStack phoneStack, String rawNumber) {
        return false;
    }

    public static boolean removeConversation(ItemStack phoneStack, String rawNumber, Player player) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (player == null || !PhoneData.isValidPhoneNumber(number) || !ChatStorageManager.isAvailable()) {
            return false;
        }

        ChatStorageManager.getInstance().removeConversation(player.getUUID(), number);
        return true;
    }

    public static List<PhoneChatMessage> getMessages(ItemStack phoneStack, String rawNumber) {
        return List.of();
    }

    public static List<PhoneChatMessage> getMessages(ItemStack phoneStack, String rawNumber, Player player) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (player == null || !PhoneData.isValidPhoneNumber(number) || !ChatStorageManager.isAvailable()) {
            return List.of();
        }

        return ChatStorageManager.getInstance().getMessages(player.getUUID(), number, MAX_MESSAGES_PER_THREAD);
    }

    public static String getLastMessagePreview(ItemStack phoneStack, String rawNumber) {
        return "";
    }

    public static String getLastMessagePreview(ItemStack phoneStack, String rawNumber, Player player) {
        PhoneChatMessage lastMessage = getLastMessage(phoneStack, rawNumber, player);
        return lastMessage == null ? "" : lastMessage.text();
    }

    public static PhoneChatMessage getLastMessage(ItemStack phoneStack, String rawNumber) {
        return null;
    }

    public static PhoneChatMessage getLastMessage(ItemStack phoneStack, String rawNumber, Player player) {
        List<PhoneChatMessage> messages = getMessages(phoneStack, rawNumber, player);
        if (messages.isEmpty()) {
            return null;
        }

        return messages.get(messages.size() - 1);
    }

    public static boolean appendMessage(ItemStack phoneStack, String otherName, String rawNumber,
                                        String rawMessage, boolean incoming) {
        return false;
    }

    public static boolean appendMessage(ItemStack phoneStack, String otherName, String rawNumber,
                                        String rawMessage, boolean incoming, UUID profileId) {
        return false;
    }

    public static boolean appendMessage(ItemStack phoneStack, String otherName, String rawNumber,
                                        String rawMessage, boolean incoming, UUID profileId, Player player) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        String message = sanitizeMessage(rawMessage);
        if (player == null || !PhoneData.isValidPhoneNumber(number) || message.isEmpty() || !ChatStorageManager.isAvailable()) {
            return false;
        }

        ChatStorageManager.getInstance().addMessage(player.getUUID(), number, message, incoming, otherName, profileId);
        return true;
    }

    public static String getFriendName(ItemStack phoneStack, String rawNumber) {
        return "";
    }

    public static String getFriendName(ItemStack phoneStack, String rawNumber, Player player) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (player == null || !PhoneData.isValidPhoneNumber(number) || !ChatStorageManager.isAvailable()) {
            return "";
        }

        return ChatStorageManager.getInstance().getFriendName(player.getUUID(), number);
    }

    public static UUID getFriendProfileId(ItemStack phoneStack, String rawNumber) {
        return null;
    }

    public static UUID getFriendProfileId(ItemStack phoneStack, String rawNumber, Player player) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (player == null || !PhoneData.isValidPhoneNumber(number) || !ChatStorageManager.isAvailable()) {
            return null;
        }

        return ChatStorageManager.getInstance().getFriendProfileId(player.getUUID(), number);
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
}
