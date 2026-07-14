package com.sammy.minedevice.phone;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PhoneChatStatePayload(String ownNickname, List<FriendEntry> friends, List<ConversationEntry> conversations) {
    private static final int MAX_SYNCED_FRIENDS = 64;
    private static final int MAX_SYNCED_CONVERSATIONS = 64;

    public PhoneChatStatePayload {
        ownNickname = ownNickname == null ? "" : ownNickname;
        friends = friends == null ? List.of() : List.copyOf(friends);
        conversations = conversations == null ? List.of() : List.copyOf(conversations);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(ownNickname, PhoneData.MAX_CONTACT_NAME_LENGTH);
        List<FriendEntry> safeFriends = friends.size() > MAX_SYNCED_FRIENDS
                ? friends.subList(0, MAX_SYNCED_FRIENDS)
                : friends;
        List<ConversationEntry> safeConversations = conversations.size() > MAX_SYNCED_CONVERSATIONS
                ? conversations.subList(0, MAX_SYNCED_CONVERSATIONS)
                : conversations;

        buf.writeVarInt(safeFriends.size());
        for (FriendEntry friend : safeFriends) {
            friend.write(buf);
        }

        buf.writeVarInt(safeConversations.size());
        for (ConversationEntry conversation : safeConversations) {
            conversation.write(buf);
        }
    }

    public static PhoneChatStatePayload read(FriendlyByteBuf buf) {
        String ownNickname = buf.readUtf(PhoneData.MAX_CONTACT_NAME_LENGTH);
        int friendCount = Math.min(buf.readVarInt(), MAX_SYNCED_FRIENDS);
        List<FriendEntry> friends = new ArrayList<>(friendCount);
        for (int i = 0; i < friendCount; i++) {
            friends.add(FriendEntry.read(buf));
        }

        int conversationCount = Math.min(buf.readVarInt(), MAX_SYNCED_CONVERSATIONS);
        List<ConversationEntry> conversations = new ArrayList<>(conversationCount);
        for (int i = 0; i < conversationCount; i++) {
            conversations.add(ConversationEntry.read(buf));
        }

        return new PhoneChatStatePayload(ownNickname, friends, conversations);
    }

    public record FriendEntry(String displayName, String number, UUID profileId) {
        public FriendEntry {
            displayName = displayName == null ? "" : displayName;
            number = PhoneData.normalizePhoneNumber(number);
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUtf(displayName, PhoneData.MAX_CONTACT_NAME_LENGTH);
            buf.writeUtf(number, PhoneData.PHONE_NUMBER_LENGTH);
            buf.writeBoolean(profileId != null);
            if (profileId != null) {
                buf.writeUUID(profileId);
            }
        }

        private static FriendEntry read(FriendlyByteBuf buf) {
            String displayName = buf.readUtf(PhoneData.MAX_CONTACT_NAME_LENGTH);
            String number = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            UUID profileId = buf.readBoolean() ? buf.readUUID() : null;
            return new FriendEntry(displayName, number, profileId);
        }
    }

    public record ConversationEntry(String number, String displayName, UUID profileId, List<PhoneChatMessage> messages) {
        private static final int MAX_SYNCED_MESSAGES = PhoneChatData.MAX_MESSAGES_PER_THREAD;

        public ConversationEntry {
            number = PhoneData.normalizePhoneNumber(number);
            displayName = displayName == null ? "" : displayName;
            messages = messages == null ? List.of() : List.copyOf(messages);
        }

        private void write(FriendlyByteBuf buf) {
            List<PhoneChatMessage> safeMessages = messages.size() > MAX_SYNCED_MESSAGES
                    ? messages.subList(messages.size() - MAX_SYNCED_MESSAGES, messages.size())
                    : messages;

            buf.writeUtf(number, PhoneData.PHONE_NUMBER_LENGTH);
            buf.writeUtf(displayName, PhoneData.MAX_CONTACT_NAME_LENGTH);
            buf.writeBoolean(profileId != null);
            if (profileId != null) {
                buf.writeUUID(profileId);
            }

            buf.writeVarInt(safeMessages.size());
            for (PhoneChatMessage message : safeMessages) {
                buf.writeUtf(PhoneChatData.sanitizeMessage(message.text()), PhoneChatData.MAX_MESSAGE_LENGTH);
                buf.writeBoolean(message.incoming());
            }
        }

        private static ConversationEntry read(FriendlyByteBuf buf) {
            String number = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            String displayName = buf.readUtf(PhoneData.MAX_CONTACT_NAME_LENGTH);
            UUID profileId = buf.readBoolean() ? buf.readUUID() : null;
            int messageCount = Math.min(buf.readVarInt(), MAX_SYNCED_MESSAGES);
            List<PhoneChatMessage> messages = new ArrayList<>(messageCount);
            for (int i = 0; i < messageCount; i++) {
                messages.add(new PhoneChatMessage(
                        buf.readUtf(PhoneChatData.MAX_MESSAGE_LENGTH),
                        buf.readBoolean()
                ));
            }
            return new ConversationEntry(number, displayName, profileId, messages);
        }
    }
}
