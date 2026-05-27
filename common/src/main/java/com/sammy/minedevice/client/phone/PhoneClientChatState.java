package com.sammy.minedevice.client.phone;

import com.sammy.minedevice.phone.PhoneChatMessage;
import com.sammy.minedevice.phone.PhoneChatStatePayload;
import com.sammy.minedevice.phone.PhoneContact;
import com.sammy.minedevice.phone.PhoneData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PhoneClientChatState {
    private static final Map<String, FriendState> FRIENDS = new LinkedHashMap<>();
    private static final Map<String, ConversationState> CONVERSATIONS = new LinkedHashMap<>();

    private PhoneClientChatState() {
    }

    public static synchronized void apply(PhoneChatStatePayload payload) {
        FRIENDS.clear();
        CONVERSATIONS.clear();
        if (payload == null) {
            return;
        }

        for (PhoneChatStatePayload.FriendEntry friend : payload.friends()) {
            String number = PhoneData.normalizePhoneNumber(friend.number());
            if (!PhoneData.isValidPhoneNumber(number)) {
                continue;
            }

            FRIENDS.put(number, new FriendState(friend.displayName(), number, friend.profileId()));
        }

        for (PhoneChatStatePayload.ConversationEntry conversation : payload.conversations()) {
            String number = PhoneData.normalizePhoneNumber(conversation.number());
            if (!PhoneData.isValidPhoneNumber(number)) {
                continue;
            }

            List<PhoneChatMessage> messages = new ArrayList<>();
            for (PhoneChatMessage message : conversation.messages()) {
                String text = message == null ? "" : message.text();
                if (text.isBlank()) {
                    continue;
                }
                messages.add(new PhoneChatMessage(text, message.incoming()));
            }

            CONVERSATIONS.put(number, new ConversationState(
                    conversation.displayName(),
                    number,
                    conversation.profileId(),
                    List.copyOf(messages)
            ));
        }
    }

    public static synchronized void clear() {
        FRIENDS.clear();
        CONVERSATIONS.clear();
    }

    public static synchronized List<PhoneContact> getFriends() {
        List<PhoneContact> contacts = new ArrayList<>(FRIENDS.size());
        for (FriendState friend : FRIENDS.values()) {
            contacts.add(new PhoneContact(friend.displayName(), friend.number()));
        }
        return contacts;
    }

    public static synchronized boolean hasFriend(String rawNumber) {
        return FRIENDS.containsKey(PhoneData.normalizePhoneNumber(rawNumber));
    }

    public static synchronized List<PhoneChatMessage> getMessages(String rawNumber) {
        ConversationState conversation = CONVERSATIONS.get(PhoneData.normalizePhoneNumber(rawNumber));
        return conversation == null ? List.of() : conversation.messages();
    }

    public static synchronized PhoneChatMessage getLastMessage(String rawNumber) {
        List<PhoneChatMessage> messages = getMessages(rawNumber);
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    public static synchronized String getLastMessagePreview(String rawNumber) {
        PhoneChatMessage message = getLastMessage(rawNumber);
        return message == null ? "" : message.text();
    }

    public static synchronized String getFriendName(String rawNumber) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        FriendState friend = FRIENDS.get(number);
        if (friend != null && !friend.displayName().isBlank()) {
            return friend.displayName();
        }

        ConversationState conversation = CONVERSATIONS.get(number);
        if (conversation != null && !conversation.displayName().isBlank()) {
            return conversation.displayName();
        }

        return "";
    }

    public static synchronized UUID getFriendProfileId(String rawNumber) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        FriendState friend = FRIENDS.get(number);
        if (friend != null && friend.profileId() != null) {
            return friend.profileId();
        }

        ConversationState conversation = CONVERSATIONS.get(number);
        return conversation == null ? null : conversation.profileId();
    }

    private record FriendState(String displayName, String number, UUID profileId) {
    }

    private record ConversationState(String displayName, String number, UUID profileId, List<PhoneChatMessage> messages) {
    }
}
