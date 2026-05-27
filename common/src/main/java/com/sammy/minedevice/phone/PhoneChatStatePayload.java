package com.sammy.minedevice.phone;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PhoneChatStatePayload(List<FriendEntry> friends, List<ConversationEntry> conversations) {
    private static final int MAX_SYNCED_FRIENDS = 64;
    private static final int MAX_SYNCED_CONVERSATIONS = 64;

    public PhoneChatStatePayload {
        friends = friends == null ? List.of() : List.copyOf(friends);
        conversations = conversations == null ? List.of() : List.copyOf(conversations);
    }

    public void write(RegistryFriendlyByteBuf buf) {
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

    public static PhoneChatStatePayload read(RegistryFriendlyByteBuf buf) {
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

        return new PhoneChatStatePayload(friends, conversations);
    }

    public record FriendEntry(String displayName, String number, UUID profileId) {
        public FriendEntry {
            displayName = displayName == null ? "" : displayName;
            number = PhoneData.normalizePhoneNumber(number);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(displayName, PhoneData.MAX_CONTACT_NAME_LENGTH);
            buf.writeUtf(number, PhoneData.PHONE_NUMBER_LENGTH);
            buf.writeBoolean(profileId != null);
            if (profileId != null) {
                buf.writeUUID(profileId);
            }
        }

        private static FriendEntry read(RegistryFriendlyByteBuf buf) {
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

        private void write(RegistryFriendlyByteBuf buf) {
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

        private static ConversationEntry read(RegistryFriendlyByteBuf buf) {
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
