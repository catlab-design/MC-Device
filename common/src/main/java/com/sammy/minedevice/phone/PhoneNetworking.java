package com.sammy.minedevice.phone;

import com.sammy.minedevice.network.NetworkBufferUtils;

import com.sammy.minedevice.Minedevice;
import com.sammy.minedevice.atm.AtmAccountStore;
import com.sammy.minedevice.block.entity.HomePhoneBlockEntity;
import com.sammy.minedevice.block.entity.HomePhoneRegistry;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PhoneNetworking {
    public static final ResourceLocation CALL_STATE_SYNC = id("phone_call_state_sync");
    public static final ResourceLocation PHONE_TOAST = id("phone_toast");
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
    public static final ResourceLocation CHAT_STATE_SYNC = id("phone_chat_state_sync");
    public static final ResourceLocation CHAT_TOAST = id("phone_chat_toast");
    public static final ResourceLocation CHAT_STATUS_TOAST = id("phone_chat_status_toast");
    public static final ResourceLocation CHAT_CONVERSATION_DELETED = id("phone_chat_conversation_deleted");
    public static final ResourceLocation CONTACT_SAVE = id("phone_contact_save");
    public static final ResourceLocation CONTACT_DELETE = id("phone_contact_delete");
    public static final ResourceLocation PHOTO_APPEND = id("phone_photo_append");
    public static final ResourceLocation PHOTO_DELETE = id("phone_photo_delete");
    public static final ResourceLocation CAMERA_POSE_UPDATE = id("phone_camera_pose_update");
    public static final ResourceLocation SCREEN_ON_UPDATE = id("phone_screen_on_update");
    public static final ResourceLocation BANK_SYNC_REQUEST = id("phone_bank_sync_request");
    public static final ResourceLocation BANK_STATE_SYNC = id("phone_bank_state_sync");
    public static final ResourceLocation BANK_TRANSFER = id("phone_bank_transfer");
    public static final ResourceLocation BANK_RECEIVE_STATE = id("phone_bank_receive_state");
    public static final ResourceLocation BANK_SCAN_TARGET = id("phone_bank_scan_target");
    public static final ResourceLocation BANK_SCAN_RESULT = id("phone_bank_scan_result");
    public static final ResourceLocation BANK_TRANSFER_RECEIPT = id("phone_bank_transfer_receipt");
    private static final int CHAT_STATUS_TOAST_LABEL_LENGTH = PhoneData.MAX_CONTACT_NAME_LENGTH + PhoneData.PHONE_NUMBER_LENGTH + 4;
    private static final double BANK_SCAN_MAX_DISTANCE_SQR = 12.0D * 12.0D;
    private static final Set<UUID> ACTIVE_BANK_RECEIVERS = ConcurrentHashMap.newKeySet();
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

        NetworkManager.registerReceiver(NetworkManager.c2s(), PHOTO_APPEND, (buf, context) -> {
            InteractionHand preferredHand = buf.readEnum(InteractionHand.class);
            String fileName = buf.readUtf(PhonePhotoData.MAX_PHOTO_FILE_NAME_LENGTH);
            ItemStack captureStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            context.queue(() -> appendPhonePhoto((ServerPlayer) context.getPlayer(), preferredHand, fileName, captureStack));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), PHOTO_DELETE, (buf, context) -> {
            InteractionHand preferredHand = buf.readEnum(InteractionHand.class);
            String fileName = buf.readUtf(PhonePhotoData.MAX_PHOTO_FILE_NAME_LENGTH);
            context.queue(() -> deletePhonePhoto((ServerPlayer) context.getPlayer(), preferredHand, fileName));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), CAMERA_POSE_UPDATE, (buf, context) -> {
            boolean active = buf.readBoolean();
            boolean selfie = buf.readBoolean();
            context.queue(() -> updateCameraPose((ServerPlayer) context.getPlayer(), active, selfie));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), SCREEN_ON_UPDATE, (buf, context) -> {
            boolean active = buf.readBoolean();
            context.queue(() -> updateScreenOnState((ServerPlayer) context.getPlayer(), active));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), BANK_SYNC_REQUEST, (buf, context) ->
                context.queue(() -> sendBankState((ServerPlayer) context.getPlayer())));

        NetworkManager.registerReceiver(NetworkManager.c2s(), BANK_TRANSFER, (buf, context) -> {
            String number = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            long amount = buf.readVarLong();
            context.queue(() -> transferBankMoney((ServerPlayer) context.getPlayer(), number, amount));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), BANK_RECEIVE_STATE, (buf, context) -> {
            boolean active = buf.readBoolean();
            context.queue(() -> setBankReceiveState((ServerPlayer) context.getPlayer(), active));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), BANK_SCAN_TARGET, (buf, context) -> {
            UUID targetId = buf.readUUID();
            context.queue(() -> scanBankPaymentTarget((ServerPlayer) context.getPlayer(), targetId));
        });

        PlayerEvent.PLAYER_JOIN.register(player -> {
            try {
                PhoneCallManager.syncPlayer(player);
            } catch (Throwable throwable) {
                Minedevice.LOGGER.error("Failed to sync phone call state for {}", player.getGameProfile().getName(), throwable);
            }
            scheduleChatStateSync(player);
        });
        TickEvent.SERVER_PRE.register(server -> {
            pruneBankReceivers(server);
            deliverPendingConversationDeletes(server);
            deliverPendingChatMessages(server);
        });
    }

    public static void sendCallState(ServerPlayer player, PhoneCallState state, String otherNumber, String otherName) {
        sendCallState(player, state, otherNumber, otherName, null);
    }

    public static void sendCallState(ServerPlayer player, PhoneCallState state, String otherNumber, String otherName,
                                     UUID otherProfileId) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        writeHomePhoneContext(buf, null);
        buf.writeEnum(state);
        buf.writeUtf(PhoneData.normalizePhoneNumber(otherNumber), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeUtf(otherName == null ? "" : otherName, PhoneData.MAX_CONTACT_NAME_LENGTH);
        writeOptionalUuid(buf, otherProfileId);
        NetworkManager.sendToPlayer(player, CALL_STATE_SYNC, buf);
    }

    public static void sendHomePhoneCallState(ServerPlayer player, BlockPos homePhonePos,
                                              PhoneCallState state, String otherNumber, String otherName) {
        sendHomePhoneCallState(player, homePhonePos, state, otherNumber, otherName, null);
    }

    public static void sendHomePhoneCallState(ServerPlayer player, BlockPos homePhonePos,
                                              PhoneCallState state, String otherNumber, String otherName,
                                              UUID otherProfileId) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        writeHomePhoneContext(buf, homePhonePos);
        buf.writeEnum(state);
        buf.writeUtf(PhoneData.normalizePhoneNumber(otherNumber), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeUtf(otherName == null ? "" : otherName, PhoneData.MAX_CONTACT_NAME_LENGTH);
        writeOptionalUuid(buf, otherProfileId);
        NetworkManager.sendToPlayer(player, CALL_STATE_SYNC, buf);
    }

    public static void openHomePhoneScreen(ServerPlayer player, BlockPos homePhonePos) {
        if (player == null || homePhonePos == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeBlockPos(homePhonePos);
        NetworkManager.sendToPlayer(player, HOME_PHONE_SCREEN_OPEN, buf);
    }

    public static void sendChatToast(ServerPlayer player, String senderName, String senderNumber, String messagePreview) {
        if (player == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeUtf(senderName == null ? "" : senderName, PhoneData.MAX_CONTACT_NAME_LENGTH);
        buf.writeUtf(PhoneData.normalizePhoneNumber(senderNumber), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeUtf(PhoneChatData.sanitizeMessage(messagePreview), PhoneChatData.MAX_MESSAGE_LENGTH);
        NetworkManager.sendToPlayer(player, CHAT_TOAST, buf);
    }

    public static void sendChatStatusToast(ServerPlayer player, boolean success, String number, String displayLabel) {
        if (player == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeBoolean(success);
        buf.writeUtf(PhoneData.normalizePhoneNumber(number), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeUtf(displayLabel == null ? "" : displayLabel, CHAT_STATUS_TOAST_LABEL_LENGTH);
        NetworkManager.sendToPlayer(player, CHAT_STATUS_TOAST, buf);
    }

    public static void sendPhoneToast(ServerPlayer player, Component title, Component message) {
        if (player == null || title == null || message == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create(player);
        ComponentSerialization.STREAM_CODEC.encode(buf, title);
        ComponentSerialization.STREAM_CODEC.encode(buf, message);
        NetworkManager.sendToPlayer(player, PHONE_TOAST, buf);
    }

    public static void sendChatConversationDeleted(ServerPlayer player, String number) {
        if (player == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeUtf(PhoneData.normalizePhoneNumber(number), PhoneData.PHONE_NUMBER_LENGTH);
        NetworkManager.sendToPlayer(player, CHAT_CONVERSATION_DELETED, buf);
    }

    public static void syncChatState(ServerPlayer player) {
        if (player == null) {
            return;
        }

        sendChatStatePayload(player, loadChatPayload(player.getUUID()));
    }

    private static void scheduleChatStateSync(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return;
        }

        UUID ownerUuid = player.getUUID();
        MinecraftServer server = player.getServer();
        CompletableFuture.supplyAsync(() -> loadChatPayload(ownerUuid))
                .whenCompleteAsync((payload, error) -> {
                    if (error != null) {
                        Minedevice.LOGGER.error("Failed to load chat state for {}", ownerUuid, error);
                        return;
                    }
                    ServerPlayer current = server.getPlayerList().getPlayer(ownerUuid);
                    if (current == null || current.connection == null) {
                        return;
                    }
                    try {
                        sendChatStatePayload(current, payload);
                    } catch (Throwable throwable) {
                        Minedevice.LOGGER.error("Failed to send chat state to {}", ownerUuid, throwable);
                    }
                }, server);
    }

    private static PhoneChatStatePayload loadChatPayload(UUID ownerUuid) {
        try {
            return ChatStorageManager.isAvailable()
                    ? ChatStorageManager.getInstance().createPayload(ownerUuid)
                    : new PhoneChatStatePayload(List.of(), List.of());
        } catch (Throwable throwable) {
            Minedevice.LOGGER.error("Failed to load chat payload for {}", ownerUuid, throwable);
            return new PhoneChatStatePayload(List.of(), List.of());
        }
    }

    private static void sendChatStatePayload(ServerPlayer player, PhoneChatStatePayload payload) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create(player);
        payload.write(buf);
        NetworkManager.sendToPlayer(player, CHAT_STATE_SYNC, buf);
    }

    public static void sendBankState(ServerPlayer player) {
        sendBankState(player, null);
    }

    private static void sendBankState(ServerPlayer player, Component status) {
        if (player == null || player.getServer() == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create(player);
        buf.writeLong(AtmAccountStore.get(player.getServer()).getBalance(player.getUUID()));
        buf.writeBoolean(status != null);
        if (status != null) {
            ComponentSerialization.STREAM_CODEC.encode(buf, status);
        }
        NetworkManager.sendToPlayer(player, BANK_STATE_SYNC, buf);
    }

    private static void sendBankStatus(ServerPlayer player, Component status) {
        if (player == null || status == null) {
            return;
        }

        player.displayClientMessage(status, true);
        sendBankState(player, status);
    }

    private static void sendBankScanResult(ServerPlayer scanner, ServerPlayer target) {
        if (scanner == null || target == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeUtf(PhoneData.getPhoneNumber(target), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeUtf(target.getGameProfile().getName(), PhoneData.MAX_CONTACT_NAME_LENGTH);
        NetworkManager.sendToPlayer(scanner, BANK_SCAN_RESULT, buf);
    }

    private static void sendBankTransferReceipt(ServerPlayer sender, ServerPlayer recipient, long amount, long senderBalance) {
        if (sender == null || recipient == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeUtf(PhoneData.getPhoneNumber(recipient), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeUtf(recipient.getGameProfile().getName(), PhoneData.MAX_CONTACT_NAME_LENGTH);
        buf.writeVarLong(Math.max(0L, amount));
        buf.writeVarLong(Math.max(0L, senderBalance));
        NetworkManager.sendToPlayer(sender, BANK_TRANSFER_RECEIPT, buf);
    }

    private static void setBankReceiveState(ServerPlayer player, boolean active) {
        if (player == null) {
            return;
        }

        if (active && canUseMobileBank(player)) {
            ACTIVE_BANK_RECEIVERS.add(player.getUUID());
            PhoneCallPoseAccess.setPhoneBankQrPoseActive(player, true);
        } else {
            ACTIVE_BANK_RECEIVERS.remove(player.getUUID());
            PhoneCallPoseAccess.setPhoneBankQrPoseActive(player, false);
        }
    }

    private static void scanBankPaymentTarget(ServerPlayer scanner, UUID targetId) {
        if (!canUseMobileBank(scanner) || scanner.getServer() == null || targetId == null) {
            return;
        }

        if (scanner.getUUID().equals(targetId)) {
            sendBankStatus(scanner, Component.translatable("screen.minedevice.phone.bank.status.self"));
            return;
        }

        ServerPlayer target = scanner.getServer().getPlayerList().getPlayer(targetId);
        if (target == null || !ACTIVE_BANK_RECEIVERS.contains(target.getUUID()) || !PhoneData.hasPhone(target)
                || scanner.distanceToSqr(target) > BANK_SCAN_MAX_DISTANCE_SQR) {
            sendBankStatus(scanner, Component.translatable("screen.minedevice.phone.bank.status.scan_failed"));
            return;
        }

        sendBankScanResult(scanner, target);
        sendBankStatus(scanner, Component.translatable(
                "screen.minedevice.phone.bank.status.scan_found",
                formatBankPlayerLabel(target, PhoneData.getPhoneNumber(target))));
    }

    private static void pruneBankReceivers(MinecraftServer server) {
        if (server == null || ACTIVE_BANK_RECEIVERS.isEmpty()) {
            return;
        }

        ACTIVE_BANK_RECEIVERS.removeIf(uuid -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                return true;
            }

            if (!canUseMobileBank(player)) {
                PhoneCallPoseAccess.setPhoneBankQrPoseActive(player, false);
                return true;
            }

            return false;
        });
    }

    private static void transferBankMoney(ServerPlayer sender, String rawNumber, long amount) {
        if (!canUseMobileBank(sender)) {
            return;
        }

        String number = PhoneData.normalizePhoneNumber(rawNumber);
        if (amount <= 0L) {
            sendBankStatus(sender, Component.translatable("screen.minedevice.phone.bank.status.invalid_amount"));
            return;
        }

        if (!PhoneData.isValidPhoneNumber(number) || !PhoneData.isMobilePhoneNumber(number)) {
            sendBankStatus(sender, Component.translatable("screen.minedevice.phone.bank.status.unavailable", number));
            return;
        }

        if (PhoneData.getPhoneNumber(sender).equals(number)) {
            sendBankStatus(sender, Component.translatable("screen.minedevice.phone.bank.status.self"));
            return;
        }

        ServerPlayer recipient = PhoneCallManager.findOnlineByNumber(sender.getServer(), number);
        if (recipient == null || !PhoneData.hasPhone(recipient)) {
            sendBankStatus(sender, Component.translatable("screen.minedevice.phone.bank.status.unavailable", number));
            return;
        }

        AtmAccountStore store = AtmAccountStore.get(sender.getServer());
        long balance = store.getBalance(sender.getUUID());
        if (balance < amount) {
            sendBankStatus(sender, Component.translatable("screen.minedevice.phone.bank.status.insufficient"));
            return;
        }

        if (!store.withdraw(sender.getUUID(), amount)) {
            sendBankStatus(sender, Component.translatable("screen.minedevice.phone.bank.status.insufficient"));
            return;
        }

        long recipientBalance = store.deposit(recipient.getUUID(), amount);
        long senderBalance = store.getBalance(sender.getUUID());
        String recipientLabel = formatBankPlayerLabel(recipient, number);
        String senderLabel = formatBankPlayerLabel(sender, PhoneData.getPhoneNumber(sender));
        sendBankStatus(sender, Component.translatable(
                "screen.minedevice.phone.bank.status.transfer_sent", amount, recipientLabel, senderBalance));
        sendBankTransferReceipt(sender, recipient, amount, senderBalance);
        sendPhoneToast(sender,
                Component.translatable("toast.minedevice.phone.bank.sent.title"),
                Component.translatable("toast.minedevice.phone.bank.sent.body", amount, recipientLabel));
        sendBankStatus(recipient, Component.translatable(
                "screen.minedevice.phone.bank.status.transfer_received", amount, senderLabel, recipientBalance));
        sendPhoneToast(recipient,
                Component.translatable("toast.minedevice.phone.bank.received.title"),
                Component.translatable("toast.minedevice.phone.bank.received.body", amount, senderLabel));
    }

    private static boolean canUseMobileBank(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return false;
        }

        if (PhoneData.hasPhone(player)) {
            return true;
        }

        sendBankStatus(player, Component.translatable("screen.minedevice.phone.call.error.no_phone"));
        return false;
    }

    private static String formatBankPlayerLabel(ServerPlayer player, String number) {
        String name = player == null ? "" : player.getGameProfile().getName();
        if (name == null || name.isBlank()) {
            return number;
        }
        return name + " (" + number + ")";
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

        if (!PhoneData.hasPhone(player)) {
            player.sendSystemMessage(Component.translatable("screen.minedevice.phone.call.error.no_phone"));
            return;
        }

        ItemStack phoneStack = PhoneData.findPhoneStack(player);
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

    private static void appendPhonePhoto(ServerPlayer player, InteractionHand preferredHand,
                                         String photoFileName, ItemStack captureStack) {
        if (player == null || photoFileName == null || photoFileName.isBlank()) {
            return;
        }

        ItemStack phoneStack = PhoneData.findPhoneStack(player, preferredHand);
        if (phoneStack.isEmpty()) {
            return;
        }

        if (PhonePhotoData.appendPhoto(phoneStack, photoFileName, captureStack)) {
            PhoneData.markDirty(player);
        }
    }

    private static void deletePhonePhoto(ServerPlayer player, InteractionHand preferredHand, String photoFileName) {
        if (player == null || photoFileName == null || photoFileName.isBlank()) {
            return;
        }

        ItemStack phoneStack = PhoneData.findPhoneStack(player, preferredHand);
        if (phoneStack.isEmpty()) {
            return;
        }

        if (PhonePhotoData.removePhotoByFileName(phoneStack, photoFileName)) {
            PhoneData.markDirty(player);
        }
    }

    private static void updateCameraPose(ServerPlayer player, boolean active, boolean selfie) {
        if (player == null) {
            return;
        }

        PhoneCallPoseAccess.setPhoneCameraPoseActive(player, active);
        PhoneCallPoseAccess.setPhoneCameraSelfieActive(player, active && selfie);
    }

    private static void updateScreenOnState(ServerPlayer player, boolean active) {
        if (player == null) {
            return;
        }

        PhoneCallPoseAccess.setPhoneScreenOnActive(player, active && PhoneData.hasPhone(player));
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
        if (PhoneChatData.addFriend(ItemStack.EMPTY, resolvedName, number, targetUuid, player)) {
            syncChatState(player);
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

        if (!PhoneData.hasPhone(sender)) {
            sender.sendSystemMessage(Component.translatable("screen.minedevice.phone.call.error.no_phone"));
            return;
        }

        ServerPlayer receiver = PhoneCallManager.findOnlineByNumber(sender.getServer(), number);
        UUID receiverProfileId = receiver == null ? PhoneChatData.getFriendProfileId(ItemStack.EMPTY, number, sender) : receiver.getUUID();
        if (receiverProfileId == null) {
            sender.sendSystemMessage(Component.translatable("screen.minedevice.phone.chat.error.unavailable", number));
            return;
        }

        String receiverName = receiver == null
                ? PhoneChatData.getFriendName(ItemStack.EMPTY, number, sender)
                : receiver.getGameProfile().getName();
        if (receiverName == null || receiverName.isBlank()) {
            receiverName = number;
        }
        String senderName = sender.getGameProfile().getName();
        boolean senderSaved = PhoneChatData.appendMessage(ItemStack.EMPTY, receiverName, number, message, false, receiverProfileId, sender);
        boolean receiverSaved = storeOrDeliverChatMessage(sender.getServer(), receiver, receiverProfileId, senderName, ownNumber, sender.getUUID(), message);
        if (!senderSaved || !receiverSaved) {
            sender.sendSystemMessage(Component.translatable("screen.minedevice.phone.chat.error.send_failed"));
            return;
        }

        syncChatState(sender);
        sender.sendSystemMessage(Component.translatable("screen.minedevice.phone.status.chat_sent"));
        sendPhoneToast(sender,
                Component.translatable("toast.minedevice.phone.chat.sent.title"),
                Component.translatable("toast.minedevice.phone.chat.sent.body",
                        Component.literal(formatChatLabel(receiverName, number)),
                        message));
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

        if (!PhoneData.hasPhone(player)) {
            player.sendSystemMessage(Component.translatable("screen.minedevice.phone.call.error.no_phone"));
            return;
        }

        UUID otherProfileId = PhoneChatData.getFriendProfileId(ItemStack.EMPTY, number, player);
        PhoneChatData.removeConversation(ItemStack.EMPTY, number, player);
        sendChatConversationDeleted(player, number);
        syncChatState(player);

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
            if (PhoneData.hasPhone(receiver)) {
                boolean receiverSaved = PhoneChatData.appendMessage(ItemStack.EMPTY, senderName, senderNumber, message, true, senderProfileId, receiver);
                if (receiverSaved) {
                    syncChatState(receiver);
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

        if (!PhoneData.hasPhone(player)) {
            return;
        }

        if (!PhoneChatData.removeConversation(ItemStack.EMPTY, otherNumber, player)) {
            return;
        }

        syncChatState(player);
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

        if (!PhoneData.hasPhone(player)) {
            return;
        }

        List<String> pendingDeletes = store.getPendingDeletes(player.getUUID());
        if (pendingDeletes.isEmpty()) {
            return;
        }

        for (String number : pendingDeletes) {
            PhoneChatData.removeConversation(ItemStack.EMPTY, number, player);
            sendChatConversationDeleted(player, number);
        }

        syncChatState(player);
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

        if (!PhoneData.hasPhone(player)) {
            return;
        }

        List<PhonePendingMessageStore.PendingMessage> pendingMessages = store.getPendingMessages(player.getUUID());
        if (pendingMessages.isEmpty()) {
            return;
        }

        for (PhonePendingMessageStore.PendingMessage pendingMessage : pendingMessages) {
            boolean delivered = PhoneChatData.appendMessage(
                    ItemStack.EMPTY,
                    pendingMessage.senderName(),
                    pendingMessage.senderNumber(),
                    pendingMessage.messageText(),
                    true,
                    pendingMessage.senderProfileId(),
                    player
            );
            if (!delivered) {
                return;
            }
        }

        store.clearPendingMessages(player.getUUID());
        syncChatState(player);
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

        return homePhone;
    }

    private static BlockPos readHomePhoneContext(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readBlockPos() : null;
    }

    private static void writeHomePhoneContext(RegistryFriendlyByteBuf buf, BlockPos homePhonePos) {
        boolean homePhone = homePhonePos != null;
        buf.writeBoolean(homePhone);
        if (homePhone) {
            buf.writeBlockPos(homePhonePos);
        }
    }

    private static void writeOptionalUuid(RegistryFriendlyByteBuf buf, UUID uuid) {
        buf.writeBoolean(uuid != null);
        if (uuid != null) {
            buf.writeUUID(uuid);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Minedevice.MOD_ID, path);
    }
}
