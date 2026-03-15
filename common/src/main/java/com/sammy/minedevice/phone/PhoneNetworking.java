package com.sammy.minedevice.phone;

import com.sammy.minedevice.Minedevice;
import com.sammy.minedevice.block.entity.HomePhoneBlockEntity;
import com.sammy.minedevice.block.entity.HomePhoneRegistry;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public final class PhoneNetworking {
    public static final ResourceLocation CALL_STATE_SYNC = id("phone_call_state_sync");
    public static final ResourceLocation HOME_PHONE_SCREEN_OPEN = id("home_phone_screen_open");
    public static final ResourceLocation CALL_REQUEST = id("phone_call_request");
    public static final ResourceLocation CALL_ANSWER = id("phone_call_answer");
    public static final ResourceLocation CALL_END = id("phone_call_end");
    public static final ResourceLocation CALL_SPEAKER_TOGGLE = id("phone_call_speaker_toggle");
    public static final ResourceLocation CALL_SYNC_REQUEST = id("phone_call_sync_request");
    public static final ResourceLocation HOME_PHONE_HANDSET_PUT_DOWN = id("home_phone_handset_put_down");
    public static final ResourceLocation CHAT_FRIEND_ADD = id("phone_chat_friend_add");
    public static final ResourceLocation CHAT_MESSAGE_SEND = id("phone_chat_message_send");
    public static final ResourceLocation CHAT_DELETE = id("phone_chat_delete");
    public static final ResourceLocation CHAT_TOAST = id("phone_chat_toast");
    public static final ResourceLocation CHAT_STATUS_TOAST = id("phone_chat_status_toast");
    public static final ResourceLocation CHAT_CONVERSATION_DELETED = id("phone_chat_conversation_deleted");
    public static final ResourceLocation CONTACT_SAVE = id("phone_contact_save");
    public static final ResourceLocation CONTACT_DELETE = id("phone_contact_delete");
    private static final int CHAT_STATUS_TOAST_LABEL_LENGTH = PhoneData.MAX_CONTACT_NAME_LENGTH + PhoneData.PHONE_NUMBER_LENGTH + 4;
    private static boolean initialized;

    private PhoneNetworking() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        PhoneCallManager.init();

        NetworkManager.registerReceiver(NetworkManager.c2s(), CALL_REQUEST, (buf, context) -> {
            BlockPos homePhonePos = readHomePhoneContext(buf);
            String number = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            context.queue(() -> {
                ServerPlayer player = (ServerPlayer) context.getPlayer();
                if (homePhonePos != null) {
                    PhoneCallManager.startHomePhoneCall(player, homePhonePos, number);
                } else {
                    PhoneCallManager.startCall(player, number);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), CALL_ANSWER, (buf, context) -> {
            BlockPos homePhonePos = readHomePhoneContext(buf);
            context.queue(() -> {
                ServerPlayer player = (ServerPlayer) context.getPlayer();
                if (homePhonePos != null) {
                    PhoneCallManager.answerHomePhoneCall(player, homePhonePos);
                } else {
                    PhoneCallManager.answerCall(player);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), CALL_END, (buf, context) -> {
            BlockPos homePhonePos = readHomePhoneContext(buf);
            context.queue(() -> {
                ServerPlayer player = (ServerPlayer) context.getPlayer();
                if (homePhonePos != null) {
                    PhoneCallManager.endHomePhoneCall(player, homePhonePos);
                } else {
                    PhoneCallManager.endCall(player);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), CALL_SPEAKER_TOGGLE, (buf, context) -> {
            BlockPos homePhonePos = readHomePhoneContext(buf);
            context.queue(() -> {
                if (homePhonePos != null) {
                    PhoneCallManager.toggleHomePhoneSpeaker((ServerPlayer) context.getPlayer(), homePhonePos);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), HOME_PHONE_HANDSET_PUT_DOWN, (buf, context) -> {
            BlockPos homePhonePos = readHomePhoneContext(buf);
            context.queue(() -> {
                ServerPlayer player = (ServerPlayer) context.getPlayer();
                if (homePhonePos != null) {
                    PhoneCallManager.putDownHomePhoneHandset(player, player.serverLevel().dimension(), homePhonePos);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), CALL_SYNC_REQUEST, (buf, context) -> {
            BlockPos homePhonePos = readHomePhoneContext(buf);
            context.queue(() -> {
                ServerPlayer player = (ServerPlayer) context.getPlayer();
                if (homePhonePos != null) {
                    PhoneCallManager.syncHomePhone(player, homePhonePos);
                } else {
                    PhoneCallManager.syncPlayer(player);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), CHAT_FRIEND_ADD, (buf, context) -> {
            String number = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            context.queue(() -> addChatFriend((ServerPlayer) context.getPlayer(), number));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), CHAT_MESSAGE_SEND, (buf, context) -> {
            String number = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            String message = buf.readUtf(PhoneChatData.MAX_MESSAGE_LENGTH);
            context.queue(() -> sendChatMessage((ServerPlayer) context.getPlayer(), number, message));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), CHAT_DELETE, (buf, context) -> {
            String number = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            context.queue(() -> deleteChatConversation((ServerPlayer) context.getPlayer(), number));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), CONTACT_SAVE, (buf, context) -> {
            BlockPos homePhonePos = readHomePhoneContext(buf);
            String number = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            String requestedName = buf.readUtf(PhoneData.MAX_CONTACT_NAME_LENGTH);
            context.queue(() -> saveContact((ServerPlayer) context.getPlayer(), homePhonePos, number, requestedName));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), CONTACT_DELETE, (buf, context) -> {
            BlockPos homePhonePos = readHomePhoneContext(buf);
            String number = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            String unusedSuggestedName = buf.readUtf(PhoneData.MAX_CONTACT_NAME_LENGTH);
            context.queue(() -> deleteContact((ServerPlayer) context.getPlayer(), homePhonePos, number));
        });

        PlayerEvent.PLAYER_JOIN.register(PhoneCallManager::syncPlayer);
        TickEvent.SERVER_PRE.register(server -> {
            deliverPendingConversationDeletes(server);
            deliverPendingChatMessages(server);
        });
    }

    public static void sendCallState(ServerPlayer player, PhoneCallState state, String otherNumber, String otherName) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        writeHomePhoneContext(buf, null);
        buf.writeEnum(state);
        buf.writeUtf(PhoneData.normalizePhoneNumber(otherNumber), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeUtf(otherName == null ? "" : otherName, PhoneData.MAX_CONTACT_NAME_LENGTH);
        NetworkManager.sendToPlayer(player, CALL_STATE_SYNC, buf);
    }

    public static void sendHomePhoneCallState(ServerPlayer player, BlockPos homePhonePos,
                                              PhoneCallState state, String otherNumber, String otherName) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        writeHomePhoneContext(buf, homePhonePos);
        buf.writeEnum(state);
        buf.writeUtf(PhoneData.normalizePhoneNumber(otherNumber), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeUtf(otherName == null ? "" : otherName, PhoneData.MAX_CONTACT_NAME_LENGTH);
        NetworkManager.sendToPlayer(player, CALL_STATE_SYNC, buf);
    }

    public static void openHomePhoneScreen(ServerPlayer player, BlockPos homePhonePos) {
        if (player == null || homePhonePos == null) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(homePhonePos);
        NetworkManager.sendToPlayer(player, HOME_PHONE_SCREEN_OPEN, buf);
    }

    public static void sendChatToast(ServerPlayer player, String senderName, String senderNumber, String messagePreview) {
        if (player == null) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(senderName == null ? "" : senderName, PhoneData.MAX_CONTACT_NAME_LENGTH);
        buf.writeUtf(PhoneData.normalizePhoneNumber(senderNumber), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeUtf(PhoneChatData.sanitizeMessage(messagePreview), PhoneChatData.MAX_MESSAGE_LENGTH);
        NetworkManager.sendToPlayer(player, CHAT_TOAST, buf);
    }

    public static void sendChatStatusToast(ServerPlayer player, boolean success, String number, String displayLabel) {
        if (player == null) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(success);
        buf.writeUtf(PhoneData.normalizePhoneNumber(number), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeUtf(displayLabel == null ? "" : displayLabel, CHAT_STATUS_TOAST_LABEL_LENGTH);
        NetworkManager.sendToPlayer(player, CHAT_STATUS_TOAST, buf);
    }

    public static void sendChatConversationDeleted(ServerPlayer player, String number) {
        if (player == null) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(PhoneData.normalizePhoneNumber(number), PhoneData.PHONE_NUMBER_LENGTH);
        NetworkManager.sendToPlayer(player, CHAT_CONVERSATION_DELETED, buf);
    }

    private static void saveContact(ServerPlayer player, BlockPos homePhonePos, String rawNumber, String requestedName) {
        var server = player.getServer();
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (!PhoneData.isValidPhoneNumber(number)) {
            player.sendSystemMessage(Component.translatable("screen.minedevice.phone.call.error.invalid"));
            return;
        }

        String resolvedName = PhoneCallManager.resolveContactName(server, number, requestedName);
        String contactName = resolvedName == null || resolvedName.isBlank() ? number : resolvedName;
        if (homePhonePos != null) {
            HomePhoneBlockEntity homePhone = getAccessibleHomePhone(player, homePhonePos);
            if (homePhone == null) {
                return;
            }

            if (homePhone.saveContact(contactName, number)) {
                player.sendSystemMessage(Component.translatable(
                        "screen.minedevice.phone.call.contacts.saved", contactName));
            }
            return;
        }

        ItemStack phoneStack = PhoneData.findPhoneStack(player);
        if (phoneStack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("screen.minedevice.phone.call.error.no_phone"));
            return;
        }

        if (PhoneData.saveContact(phoneStack, contactName, number)) {
            PhoneData.markDirty(player);
            player.sendSystemMessage(Component.translatable(
                    "screen.minedevice.phone.call.contacts.saved", contactName));
        }
    }

    private static void deleteContact(ServerPlayer player, BlockPos homePhonePos, String rawNumber) {
        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (!PhoneData.isValidPhoneNumber(number)) {
            return;
        }

        if (homePhonePos != null) {
            HomePhoneBlockEntity homePhone = getAccessibleHomePhone(player, homePhonePos);
            if (homePhone != null && homePhone.removeContact(number)) {
                player.sendSystemMessage(Component.translatable(
                        "screen.minedevice.phone.call.contacts.deleted", number));
            }
            return;
        }

        ItemStack phoneStack = PhoneData.findPhoneStack(player);
        if (phoneStack.isEmpty()) {
            return;
        }

        if (PhoneData.removeContact(phoneStack, number)) {
            PhoneData.markDirty(player);
            player.sendSystemMessage(Component.translatable(
                    "screen.minedevice.phone.call.contacts.deleted", number));
        }
    }

    private static void addChatFriend(ServerPlayer player, String rawNumber) {
        if (player == null) {
            return;
        }

        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (!PhoneData.isValidPhoneNumber(number)) {
            player.sendSystemMessage(Component.translatable("screen.minedevice.phone.call.error.invalid"));
            return;
        }

        if (!PhoneData.isMobilePhoneNumber(number)) {
            player.sendSystemMessage(Component.translatable("screen.minedevice.phone.chat.error.unavailable", number));
            return;
        }

        String ownNumber = PhoneData.getPhoneNumber(player);
        if (ownNumber.equals(number)) {
            player.sendSystemMessage(Component.translatable("screen.minedevice.phone.chat.error.self"));
            return;
        }

        ItemStack phoneStack = PhoneData.findPhoneStack(player);
        if (phoneStack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("screen.minedevice.phone.call.error.no_phone"));
            return;
        }

        ServerPlayer target = PhoneCallManager.findOnlineByNumber(player.getServer(), number);
        if (target == null || !PhoneData.hasPhone(target)) {
            sendChatStatusToast(player, false, number, number);
            return;
        }

        String resolvedName = target.getGameProfile().getName();
        UUID targetUuid = target.getUUID();
        if (PhoneChatData.addFriend(phoneStack, resolvedName, number, targetUuid)) {
            PhoneData.markDirty(player);
            sendChatStatusToast(player, true, number, formatChatLabel(resolvedName, number));
        }
    }

    private static void sendChatMessage(ServerPlayer sender, String rawNumber, String rawMessage) {
        if (sender == null) {
            return;
        }

        String number = PhoneData.normalizePhoneNumber(rawNumber);
        String message = PhoneChatData.sanitizeMessage(rawMessage);
        if (!PhoneData.isValidPhoneNumber(number)) {
            sender.sendSystemMessage(Component.translatable("screen.minedevice.phone.call.error.invalid"));
            return;
        }

        if (!PhoneData.isMobilePhoneNumber(number)) {
            sender.sendSystemMessage(Component.translatable("screen.minedevice.phone.chat.error.unavailable", number));
            return;
        }

        if (message.isEmpty()) {
            sender.sendSystemMessage(Component.translatable("screen.minedevice.phone.chat.error.empty"));
            return;
        }

        String ownNumber = PhoneData.getPhoneNumber(sender);
        if (ownNumber.equals(number)) {
            sender.sendSystemMessage(Component.translatable("screen.minedevice.phone.chat.error.self"));
            return;
        }

        ItemStack senderPhone = PhoneData.findPhoneStack(sender);
        if (senderPhone.isEmpty()) {
            sender.sendSystemMessage(Component.translatable("screen.minedevice.phone.call.error.no_phone"));
            return;
        }

        ServerPlayer receiver = PhoneCallManager.findOnlineByNumber(sender.getServer(), number);
        UUID receiverProfileId = receiver == null ? PhoneChatData.getFriendProfileId(senderPhone, number) : receiver.getUUID();
        if (receiverProfileId == null) {
            sender.sendSystemMessage(Component.translatable("screen.minedevice.phone.chat.error.unavailable", number));
            return;
        }

        String receiverName = receiver == null
                ? PhoneChatData.getFriendName(senderPhone, number)
                : receiver.getGameProfile().getName();
        if (receiverName == null || receiverName.isBlank()) {
            receiverName = number;
        }
        String senderName = sender.getGameProfile().getName();
        boolean senderSaved = PhoneChatData.appendMessage(senderPhone, receiverName, number, message, false, receiverProfileId);
        boolean receiverSaved = storeOrDeliverChatMessage(sender.getServer(), receiver, receiverProfileId, senderName, ownNumber, sender.getUUID(), message);
        if (!senderSaved || !receiverSaved) {
            sender.sendSystemMessage(Component.translatable("screen.minedevice.phone.chat.error.send_failed"));
            return;
        }

        PhoneData.markDirty(sender);
        sender.sendSystemMessage(Component.translatable("screen.minedevice.phone.status.chat_sent"));
        if (receiver != null && PhoneData.hasPhone(receiver)) {
            receiver.sendSystemMessage(Component.translatable(
                    "screen.minedevice.phone.chat.status.incoming_from",
                    senderName,
                    ownNumber));
        }
    }

    private static void deleteChatConversation(ServerPlayer player, String rawNumber) {
        if (player == null) {
            return;
        }

        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (!PhoneData.isValidPhoneNumber(number)) {
            player.sendSystemMessage(Component.translatable("screen.minedevice.phone.call.error.invalid"));
            return;
        }

        if (!PhoneData.isMobilePhoneNumber(number)) {
            player.sendSystemMessage(Component.translatable("screen.minedevice.phone.chat.error.unavailable", number));
            return;
        }

        String ownNumber = PhoneData.getPhoneNumber(player);
        if (ownNumber.equals(number)) {
            player.sendSystemMessage(Component.translatable("screen.minedevice.phone.chat.error.self"));
            return;
        }

        ItemStack phoneStack = PhoneData.findPhoneStack(player);
        if (phoneStack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("screen.minedevice.phone.call.error.no_phone"));
            return;
        }

        UUID otherProfileId = PhoneChatData.getFriendProfileId(phoneStack, number);
        PhoneChatData.removeConversation(phoneStack, number);
        PhoneData.markDirty(player);

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        PhonePendingMessageStore pendingMessages = PhonePendingMessageStore.get(server);
        pendingMessages.clearMessagesForSender(player.getUUID(), number);
        if (otherProfileId != null) {
            pendingMessages.clearMessagesForSender(otherProfileId, ownNumber);
        }

        ServerPlayer otherPlayer = PhoneCallManager.findOnlineByNumber(server, number);
        if (otherPlayer != null && PhoneData.hasPhone(otherPlayer)) {
            applyChatConversationDelete(otherPlayer, ownNumber);
            return;
        }

        if (otherProfileId != null) {
            PhonePendingConversationDeleteStore.get(server).queueDeletion(otherProfileId, ownNumber);
        }
    }

    private static boolean storeOrDeliverChatMessage(MinecraftServer server, ServerPlayer receiver, UUID receiverProfileId,
                                                     String senderName, String senderNumber, UUID senderProfileId, String message) {
        if (server == null || receiverProfileId == null) {
            return false;
        }

        if (receiver != null) {
            ItemStack receiverPhone = PhoneData.findPhoneStack(receiver);
            if (!receiverPhone.isEmpty()) {
                boolean receiverSaved = PhoneChatData.appendMessage(receiverPhone, senderName, senderNumber, message, true, senderProfileId);
                if (receiverSaved) {
                    PhoneData.markDirty(receiver);
                    sendChatToast(receiver, senderName, senderNumber, message);
                }
                return receiverSaved;
            }
        }

        return PhonePendingMessageStore.get(server)
                .queueMessage(receiverProfileId, senderName, senderNumber, senderProfileId, message);
    }

    private static void applyChatConversationDelete(ServerPlayer player, String otherNumber) {
        if (player == null) {
            return;
        }

        ItemStack phoneStack = PhoneData.findPhoneStack(player);
        if (phoneStack.isEmpty()) {
            return;
        }

        if (!PhoneChatData.removeConversation(phoneStack, otherNumber)) {
            return;
        }

        PhoneData.markDirty(player);
        sendChatConversationDeleted(player, otherNumber);
    }

    private static void deliverPendingConversationDeletes(MinecraftServer server) {
        if (server == null) {
            return;
        }

        PhonePendingConversationDeleteStore store = PhonePendingConversationDeleteStore.get(server);
        if (!store.hasPendingDeletes()) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            deliverPendingConversationDeletes(player, store);
        }
    }

    private static void deliverPendingConversationDeletes(ServerPlayer player, PhonePendingConversationDeleteStore store) {
        if (player == null || store == null || !store.hasPendingDeletes(player.getUUID())) {
            return;
        }

        ItemStack phoneStack = PhoneData.findPhoneStack(player);
        if (phoneStack.isEmpty()) {
            return;
        }

        List<String> pendingDeletes = store.getPendingDeletes(player.getUUID());
        if (pendingDeletes.isEmpty()) {
            return;
        }

        for (String number : pendingDeletes) {
            PhoneChatData.removeConversation(phoneStack, number);
            sendChatConversationDeleted(player, number);
        }

        PhoneData.markDirty(player);
        store.clearPendingDeletes(player.getUUID());
    }

    private static void deliverPendingChatMessages(MinecraftServer server) {
        if (server == null) {
            return;
        }

        PhonePendingMessageStore store = PhonePendingMessageStore.get(server);
        if (!store.hasPendingMessages()) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            deliverPendingChatMessages(player, store);
        }
    }

    private static void deliverPendingChatMessages(ServerPlayer player, PhonePendingMessageStore store) {
        if (player == null || store == null || !store.hasPendingMessages(player.getUUID())) {
            return;
        }

        ItemStack phoneStack = PhoneData.findPhoneStack(player);
        if (phoneStack.isEmpty()) {
            return;
        }

        List<PhonePendingMessageStore.PendingMessage> pendingMessages = store.getPendingMessages(player.getUUID());
        if (pendingMessages.isEmpty()) {
            return;
        }

        for (PhonePendingMessageStore.PendingMessage pendingMessage : pendingMessages) {
            boolean delivered = PhoneChatData.appendMessage(
                    phoneStack,
                    pendingMessage.senderName(),
                    pendingMessage.senderNumber(),
                    pendingMessage.messageText(),
                    true,
                    pendingMessage.senderProfileId()
            );
            if (!delivered) {
                return;
            }
        }

        PhoneData.markDirty(player);
        store.clearPendingMessages(player.getUUID());
        for (PhonePendingMessageStore.PendingMessage pendingMessage : pendingMessages) {
            sendChatToast(player, pendingMessage.senderName(), pendingMessage.senderNumber(), pendingMessage.messageText());
        }
    }

    private static String formatChatLabel(String displayName, String number) {
        if (displayName == null || displayName.isBlank() || displayName.equals(number)) {
            return number;
        }

        return displayName + " (" + number + ")";
    }

    private static HomePhoneBlockEntity getAccessibleHomePhone(ServerPlayer player, BlockPos homePhonePos) {
        if (player == null || homePhonePos == null) {
            return null;
        }

        ServerLevel level = player.serverLevel();
        HomePhoneBlockEntity homePhone = HomePhoneRegistry.get(level, homePhonePos);
        if (homePhone == null) {
            return null;
        }

        double centerX = homePhonePos.getX() + 0.5D;
        double centerY = homePhonePos.getY() + 0.5D;
        double centerZ = homePhonePos.getZ() + 0.5D;
        return player.distanceToSqr(centerX, centerY, centerZ) <= 64.0D ? homePhone : null;
    }

    private static BlockPos readHomePhoneContext(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readBlockPos() : null;
    }

    private static void writeHomePhoneContext(FriendlyByteBuf buf, BlockPos homePhonePos) {
        boolean homePhone = homePhonePos != null;
        buf.writeBoolean(homePhone);
        if (homePhone) {
            buf.writeBlockPos(homePhonePos);
        }
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Minedevice.MOD_ID, path);
    }
}
