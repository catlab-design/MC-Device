package com.sammy.minedevice.client.phone;

import com.sammy.minedevice.network.NetworkBufferUtils;

import com.sammy.minedevice.ModSounds;
import com.sammy.minedevice.block.entity.HomePhoneBlockEntity;
import com.sammy.minedevice.client.homephone.HomePhoneDialingSound;
import com.sammy.minedevice.client.homephone.HomePhoneRingSound;
import com.sammy.minedevice.homephone.HomePhoneRingState;
import com.sammy.minedevice.phone.PhoneCallState;
import com.sammy.minedevice.phone.PhoneData;
import com.sammy.minedevice.phone.PhoneNetworking;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class PhoneNetworkingClient {
    private static boolean initialized;
    private static final Map<BlockPos, HomePhoneRingSound> activeRings = new HashMap<>();
    private static final Map<BlockPos, HomePhoneDialingSound> activeHomeDialingRings = new HashMap<>();
    private static MobilePhoneRingSound mobileIncomingRing;
    private static LoopingPhoneUiSound mobileDialingRing;
    private static boolean lastCameraPoseActive;
    private static boolean lastCameraPoseSelfie;
    private static boolean cameraPoseInitialized;
    private static boolean lastScreenOnActive;
    private static boolean screenOnInitialized;

    private PhoneNetworkingClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.s2c(), PhoneNetworking.CALL_STATE_SYNC, (buf, context) -> {
            boolean homePhone = buf.readBoolean();
            BlockPos blockPos = homePhone ? buf.readBlockPos() : null;
            PhoneCallState state = buf.readEnum(PhoneCallState.class);
            String otherNumber = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            String otherName = buf.readUtf(PhoneData.MAX_CONTACT_NAME_LENGTH);
            UUID otherProfileId = buf.readBoolean() ? buf.readUUID() : null;
            context.queue(() -> {
                PhoneCallState previousState = PhoneClientCallState.getState(blockPos);
                if (homePhone) {
                    PhoneClientCallState.applyHomePhone(blockPos, state, otherNumber, otherName, otherProfileId);
                } else {
                    PhoneClientCallState.apply(state, otherNumber, otherName, otherProfileId);
                }

                if (state == PhoneCallState.INCOMING_RINGING && previousState != PhoneCallState.INCOMING_RINGING) {
                    Component callerLabel = otherName.isBlank()
                            ? Component.literal(otherNumber)
                            : Component.literal(otherName + " (" + otherNumber + ")");
                    showPhoneToast(
                            Component.translatable("toast.minedevice.phone.call.title"),
                            Component.translatable("toast.minedevice.phone.call.body", callerLabel)
                    );
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), PhoneNetworking.CHAT_TOAST, (buf, context) -> {
            String senderName = buf.readUtf(PhoneData.MAX_CONTACT_NAME_LENGTH);
            String senderNumber = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            String messagePreview = buf.readUtf(com.sammy.minedevice.phone.PhoneChatData.MAX_MESSAGE_LENGTH);
            context.queue(() -> {
                Component senderLabel = senderName.isBlank()
                        ? Component.literal(senderNumber)
                        : Component.literal(senderName);
                showPhoneToast(
                        Component.translatable("toast.minedevice.phone.chat.title"),
                        Component.translatable("toast.minedevice.phone.chat.body", senderLabel, messagePreview)
                );
            });
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), PhoneNetworking.CHAT_STATE_SYNC, (buf, context) -> {
            var payload = com.sammy.minedevice.phone.PhoneChatStatePayload.read(buf);
            context.queue(() -> PhoneClientChatState.apply(payload));
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), PhoneNetworking.CHAT_STATUS_TOAST, (buf, context) -> {
            boolean success = buf.readBoolean();
            String number = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            String displayLabel = buf.readUtf(PhoneData.MAX_CONTACT_NAME_LENGTH + PhoneData.PHONE_NUMBER_LENGTH + 4);
            context.queue(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (success && minecraft != null && minecraft.screen instanceof PhoneScreen phoneScreen) {
                    phoneScreen.handleChatFriendAddSuccess(number);
                }

                showPhoneToast(
                        Component.translatable("toast.minedevice.phone.chat.friend.title"),
                        Component.translatable(
                                success
                                        ? "toast.minedevice.phone.chat.friend.added"
                                        : "toast.minedevice.phone.chat.friend.offline",
                                Component.literal(displayLabel.isBlank() ? number : displayLabel)
                        )
                );
            });
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), PhoneNetworking.PHONE_TOAST, (buf, context) -> {
            Component title = ComponentSerialization.STREAM_CODEC.decode(buf);
            Component message = ComponentSerialization.STREAM_CODEC.decode(buf);
            context.queue(() -> showPhoneToast(title, message));
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), PhoneNetworking.CHAT_CONVERSATION_DELETED, (buf, context) -> {
            String number = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            context.queue(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft != null && minecraft.screen instanceof PhoneScreen phoneScreen) {
                    phoneScreen.handleChatConversationDeleted(number);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), PhoneNetworking.HOME_PHONE_SCREEN_OPEN, (buf, context) -> {
            BlockPos blockPos = buf.readBlockPos();
            context.queue(() -> PhoneClientHooks.openHomePhoneScreen(blockPos));
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), PhoneNetworking.BANK_STATE_SYNC, (buf, context) -> {
            long balance = buf.readLong();
            Component status = buf.readBoolean() ? ComponentSerialization.STREAM_CODEC.decode(buf) : Component.empty();
            context.queue(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft != null && minecraft.screen instanceof PhoneScreen phoneScreen) {
                    phoneScreen.handleBankState(balance, status);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), PhoneNetworking.BANK_SCAN_RESULT, (buf, context) -> {
            String targetNumber = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            String targetName = buf.readUtf(PhoneData.MAX_CONTACT_NAME_LENGTH);
            context.queue(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft != null && minecraft.screen instanceof PhoneScreen phoneScreen) {
                    phoneScreen.handleBankScanResult(targetNumber, targetName);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.s2c(), PhoneNetworking.BANK_TRANSFER_RECEIPT, (buf, context) -> {
            String targetNumber = buf.readUtf(PhoneData.PHONE_NUMBER_LENGTH);
            String targetName = buf.readUtf(PhoneData.MAX_CONTACT_NAME_LENGTH);
            long amount = buf.readVarLong();
            long balance = buf.readVarLong();
            context.queue(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft != null && minecraft.screen instanceof PhoneScreen phoneScreen) {
                    phoneScreen.handleBankTransferReceipt(targetNumber, targetName, amount, balance);
                }
            });
        });

        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            clearClientState();
        });
    }

    public static void tick(Minecraft minecraft) {
        syncCameraPoseState(minecraft);
        syncScreenOnState(minecraft);

        if (minecraft == null || minecraft.level == null) {
            stopAllActiveRings();
            stopAllHomeDialingRings();
            stopMobileIncomingRing();
            stopMobileDialingRing();
            return;
        }

        Set<BlockPos> ringingPhones = HomePhoneRingState.snapshot(minecraft.level.dimension());
        activeRings.entrySet().removeIf(entry -> {
            boolean keepRinging = ringingPhones.contains(entry.getKey()) && canPlayHomePhoneRingSound(minecraft, entry.getKey());
            if (!keepRinging) {
                entry.getValue().stopRinging();
            }
            return !keepRinging;
        });

        for (BlockPos blockPos : ringingPhones) {
            if (activeRings.containsKey(blockPos) || !canPlayHomePhoneRingSound(minecraft, blockPos)) {
                continue;
            }

            HomePhoneRingSound ringSound = new HomePhoneRingSound(blockPos);
            activeRings.put(blockPos.immutable(), ringSound);
            minecraft.getSoundManager().play(ringSound);
        }

        updateMobileIncomingRing(minecraft);
        updateMobileDialingRing(minecraft);
        updateHomePhoneDialingRings(minecraft);
    }

    public static void clearClientState() {
        PhoneClientCallState.clear();
        PhoneClientChatState.clear();
        stopAllActiveRings();
        stopAllHomeDialingRings();
        stopMobileIncomingRing();
        stopMobileDialingRing();
        HomePhoneRingState.clear();
        cameraPoseInitialized = false;
        lastCameraPoseActive = false;
        lastCameraPoseSelfie = false;
        screenOnInitialized = false;
        lastScreenOnActive = false;
        PhoneClientHooks.clearBankReceiveWorldState(false);
    }

    public static void requestSync() {
        sendWithoutPayload(PhoneNetworking.CALL_SYNC_REQUEST, null);
    }

    public static void requestSync(BlockPos homePhonePos) {
        sendWithoutPayload(PhoneNetworking.CALL_SYNC_REQUEST, homePhonePos);
    }

    public static void requestCall(String number) {
        sendNumberOnly(PhoneNetworking.CALL_REQUEST, number, null);
    }

    public static void requestCall(String number, BlockPos homePhonePos) {
        sendNumberOnly(PhoneNetworking.CALL_REQUEST, number, homePhonePos);
    }

    public static void requestAnswer() {
        sendWithoutPayload(PhoneNetworking.CALL_ANSWER, null);
    }

    public static void requestAnswer(BlockPos homePhonePos) {
        sendWithoutPayload(PhoneNetworking.CALL_ANSWER, homePhonePos);
    }

    public static void requestEndCall() {
        sendWithoutPayload(PhoneNetworking.CALL_END, null);
    }

    public static void requestEndCall(BlockPos homePhonePos) {
        sendWithoutPayload(PhoneNetworking.CALL_END, homePhonePos);
    }

    public static void requestPutDownHomePhoneHandset(BlockPos homePhonePos) {
        sendWithoutPayload(PhoneNetworking.HOME_PHONE_HANDSET_PUT_DOWN, homePhonePos);
    }

    public static void requestToggleSpeaker(BlockPos homePhonePos) {
        sendWithoutPayload(PhoneNetworking.CALL_SPEAKER_TOGGLE, homePhonePos);
    }

    public static void requestSaveContact(String number, String suggestedName) {
        sendContactMutation(PhoneNetworking.CONTACT_SAVE, number, suggestedName, null);
    }

    public static void requestSaveContact(String number, String suggestedName, BlockPos homePhonePos) {
        sendContactMutation(PhoneNetworking.CONTACT_SAVE, number, suggestedName, homePhonePos);
    }

    public static void requestDeleteContact(String number) {
        sendContactMutation(PhoneNetworking.CONTACT_DELETE, number, "", null);
    }

    public static void requestDeleteContact(String number, BlockPos homePhonePos) {
        sendContactMutation(PhoneNetworking.CONTACT_DELETE, number, "", homePhonePos);
    }

    public static void requestAddChatFriend(String number) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeUtf(PhoneData.normalizePhoneNumber(number), PhoneData.PHONE_NUMBER_LENGTH);
        NetworkManager.sendToServer(PhoneNetworking.CHAT_FRIEND_ADD, buf);
    }

    public static void requestDeleteChatConversation(String number) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeUtf(PhoneData.normalizePhoneNumber(number), PhoneData.PHONE_NUMBER_LENGTH);
        NetworkManager.sendToServer(PhoneNetworking.CHAT_DELETE, buf);
    }

    public static void requestSendChatMessage(String number, String message) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeUtf(PhoneData.normalizePhoneNumber(number), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeUtf(message == null ? "" : message, com.sammy.minedevice.phone.PhoneChatData.MAX_MESSAGE_LENGTH);
        NetworkManager.sendToServer(PhoneNetworking.CHAT_MESSAGE_SEND, buf);
    }

    public static void requestAppendPhotoMetadata(InteractionHand preferredHand, String photoFileName, ItemStack captureStack) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create(clientRegistryAccess());
        buf.writeEnum(preferredHand == null ? InteractionHand.MAIN_HAND : preferredHand);
        buf.writeUtf(photoFileName == null ? "" : photoFileName, com.sammy.minedevice.phone.PhonePhotoData.MAX_PHOTO_FILE_NAME_LENGTH);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, captureStack == null ? ItemStack.EMPTY : captureStack);
        NetworkManager.sendToServer(PhoneNetworking.PHOTO_APPEND, buf);
    }

    private static net.minecraft.core.RegistryAccess clientRegistryAccess() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.level != null) {
            return minecraft.level.registryAccess();
        }
        return net.minecraft.core.RegistryAccess.EMPTY;
    }

    public static void requestDeletePhotoMetadata(InteractionHand preferredHand, String photoFileName) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeEnum(preferredHand == null ? InteractionHand.MAIN_HAND : preferredHand);
        buf.writeUtf(photoFileName == null ? "" : photoFileName, com.sammy.minedevice.phone.PhonePhotoData.MAX_PHOTO_FILE_NAME_LENGTH);
        NetworkManager.sendToServer(PhoneNetworking.PHOTO_DELETE, buf);
    }

    public static void requestBankSync() {
        NetworkManager.sendToServer(PhoneNetworking.BANK_SYNC_REQUEST, NetworkBufferUtils.create());
    }

    public static void requestBankTransfer(String number, long amount) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeUtf(PhoneData.normalizePhoneNumber(number), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeVarLong(Math.max(0L, amount));
        NetworkManager.sendToServer(PhoneNetworking.BANK_TRANSFER, buf);
    }

    public static void requestBankReceiveState(boolean active) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeBoolean(active);
        NetworkManager.sendToServer(PhoneNetworking.BANK_RECEIVE_STATE, buf);
    }

    public static void requestBankScanTarget(UUID targetId) {
        if (targetId == null) {
            return;
        }

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeUUID(targetId);
        NetworkManager.sendToServer(PhoneNetworking.BANK_SCAN_TARGET, buf);
    }

    private static void syncCameraPoseState(Minecraft minecraft) {
        if (minecraft == null || minecraft.getConnection() == null) {
            cameraPoseInitialized = false;
            lastCameraPoseActive = false;
            lastCameraPoseSelfie = false;
            return;
        }

        boolean active = minecraft.player != null
                && PhoneClientHooks.isLocalPhoneCameraPoseActive();
        boolean selfie = active && PhoneClientHooks.isLocalPhoneCameraSelfieActive();

        if (cameraPoseInitialized && active == lastCameraPoseActive && selfie == lastCameraPoseSelfie) {
            return;
        }

        cameraPoseInitialized = true;
        lastCameraPoseActive = active;
        lastCameraPoseSelfie = selfie;

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeBoolean(active);
        buf.writeBoolean(selfie);
        NetworkManager.sendToServer(PhoneNetworking.CAMERA_POSE_UPDATE, buf);
    }

    private static void syncScreenOnState(Minecraft minecraft) {
        if (minecraft == null || minecraft.getConnection() == null) {
            screenOnInitialized = false;
            lastScreenOnActive = false;
            return;
        }

        boolean active = minecraft.player != null && PhoneClientHooks.isLocalPhoneScreenOnActive();

        if (screenOnInitialized && active == lastScreenOnActive) {
            return;
        }

        screenOnInitialized = true;
        lastScreenOnActive = active;

        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        buf.writeBoolean(active);
        NetworkManager.sendToServer(PhoneNetworking.SCREEN_ON_UPDATE, buf);
    }

    private static void sendWithoutPayload(net.minecraft.resources.ResourceLocation packetId, BlockPos homePhonePos) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        writeHomePhoneContext(buf, homePhonePos);
        NetworkManager.sendToServer(packetId, buf);
    }

    private static void sendNumberOnly(net.minecraft.resources.ResourceLocation packetId, String number, BlockPos homePhonePos) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        writeHomePhoneContext(buf, homePhonePos);
        buf.writeUtf(PhoneData.normalizePhoneNumber(number), PhoneData.PHONE_NUMBER_LENGTH);
        NetworkManager.sendToServer(packetId, buf);
    }

    private static void sendContactMutation(net.minecraft.resources.ResourceLocation packetId, String number, String suggestedName,
                                            BlockPos homePhonePos) {
        RegistryFriendlyByteBuf buf = NetworkBufferUtils.create();
        writeHomePhoneContext(buf, homePhonePos);
        buf.writeUtf(PhoneData.normalizePhoneNumber(number), PhoneData.PHONE_NUMBER_LENGTH);
        buf.writeUtf(suggestedName == null ? "" : suggestedName, PhoneData.MAX_CONTACT_NAME_LENGTH);
        NetworkManager.sendToServer(packetId, buf);
    }

    private static void writeHomePhoneContext(RegistryFriendlyByteBuf buf, BlockPos homePhonePos) {
        boolean homePhone = homePhonePos != null;
        buf.writeBoolean(homePhone);
        if (homePhone) {
            buf.writeBlockPos(homePhonePos);
        }
    }

    private static boolean canPlayHomePhoneRingSound(Minecraft minecraft, BlockPos blockPos) {
        HomePhoneBlockEntity homePhone = getClientHomePhone(minecraft, blockPos);
        return homePhone != null && homePhone.isRinging();
    }

    private static boolean canPlayHomePhoneDialingSound(Minecraft minecraft, BlockPos blockPos) {
        if (PhoneClientCallState.getState(blockPos) != PhoneCallState.OUTGOING_RINGING) {
            return false;
        }

        HomePhoneBlockEntity homePhone = getClientHomePhone(minecraft, blockPos);
        return homePhone != null && hasHomePhoneSoundOutput(minecraft, homePhone);
    }

    private static HomePhoneBlockEntity getClientHomePhone(Minecraft minecraft, BlockPos blockPos) {
        if (minecraft == null || minecraft.level == null || blockPos == null || !minecraft.level.hasChunkAt(blockPos)) {
            return null;
        }

        if (!(minecraft.level.getBlockEntity(blockPos) instanceof HomePhoneBlockEntity homePhone)) {
            return null;
        }

        return homePhone;
    }

    private static boolean hasHomePhoneSoundOutput(Minecraft minecraft, HomePhoneBlockEntity homePhone) {
        if (minecraft == null || minecraft.level == null || homePhone == null) {
            return false;
        }

        UUID handsetHolderId = homePhone.getHandsetHolderId();
        if (handsetHolderId != null) {
            Player holder = minecraft.level.getPlayerByUUID(handsetHolderId);
            if (holder != null) {
                return true;
            }
        }

        return homePhone.isSpeakerEnabled();
    }

    private static void stopAllActiveRings() {
        for (HomePhoneRingSound ring : activeRings.values()) {
            ring.stopRinging();
        }
        activeRings.clear();
    }

    private static void stopAllHomeDialingRings() {
        for (HomePhoneDialingSound dialingSound : activeHomeDialingRings.values()) {
            dialingSound.stopDialing();
        }
        activeHomeDialingRings.clear();
    }

    private static void updateMobileIncomingRing(Minecraft minecraft) {
        boolean shouldPlay = PhoneClientCallState.getState() == PhoneCallState.INCOMING_RINGING;
        if (!shouldPlay) {
            stopMobileIncomingRing();
            return;
        }

        if (mobileIncomingRing == null) {
            mobileIncomingRing = new MobilePhoneRingSound();
            minecraft.getSoundManager().play(mobileIncomingRing);
        }
    }

    private static void updateMobileDialingRing(Minecraft minecraft) {
        boolean shouldPlay = PhoneClientCallState.getState() == PhoneCallState.OUTGOING_RINGING;
        if (!shouldPlay) {
            stopMobileDialingRing();
            return;
        }

        if (mobileDialingRing == null) {
            mobileDialingRing = new LoopingPhoneUiSound(ModSounds.PHONE_DIALING.get(), 0.60F);
            minecraft.getSoundManager().play(mobileDialingRing);
        }
    }

    private static void updateHomePhoneDialingRings(Minecraft minecraft) {
        Set<BlockPos> outgoingHomePhones = PhoneClientCallState.getHomePhonesInState(PhoneCallState.OUTGOING_RINGING);
        activeHomeDialingRings.entrySet().removeIf(entry -> {
            boolean keepDialing = outgoingHomePhones.contains(entry.getKey())
                    && canPlayHomePhoneDialingSound(minecraft, entry.getKey());
            if (!keepDialing) {
                entry.getValue().stopDialing();
            }
            return !keepDialing;
        });

        for (BlockPos blockPos : outgoingHomePhones) {
            if (activeHomeDialingRings.containsKey(blockPos) || !canPlayHomePhoneDialingSound(minecraft, blockPos)) {
                continue;
            }

            HomePhoneDialingSound dialingSound = new HomePhoneDialingSound(blockPos);
            activeHomeDialingRings.put(blockPos.immutable(), dialingSound);
            minecraft.getSoundManager().play(dialingSound);
        }
    }

    private static void stopMobileIncomingRing() {
        if (mobileIncomingRing == null) {
            return;
        }

        mobileIncomingRing.stopRinging();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.getSoundManager().stop(mobileIncomingRing);
        }
        mobileIncomingRing = null;
    }

    private static void stopMobileDialingRing() {
        if (mobileDialingRing == null) {
            return;
        }

        mobileDialingRing.stopLooping();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.getSoundManager().stop(mobileDialingRing);
        }
        mobileDialingRing = null;
    }

    private static void showPhoneToast(Component title, Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        SystemToast.add(minecraft.getToasts(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                title,
                message);
    }

    private static final class LoopingPhoneUiSound extends AbstractTickableSoundInstance {
        private boolean shouldStop;

        private LoopingPhoneUiSound(net.minecraft.sounds.SoundEvent soundEvent, float volume) {
            super(soundEvent, net.minecraft.sounds.SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = volume;
            this.pitch = 1.0F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            if (shouldStop) {
                this.stop();
            }
        }

        private void stopLooping() {
            this.shouldStop = true;
        }
    }
}
