package com.sammy.minedevice.voice.svc;

import com.sammy.minedevice.Minedevice;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

public class MinedeviceSvcPlugin implements VoicechatPlugin {

    private static VoicechatServerApi serverApi;

    @Override
    public String getPluginId() {
        return Minedevice.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        Minedevice.LOGGER.info("MineDevice Simple Voice Chat plugin initialized");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        serverApi = event.getVoicechat();
        Minedevice.LOGGER.info("MineDevice Simple Voice Chat server hooks activated");

        SvcWalkieVoiceHook.onServerStarted(serverApi);
        SvcMegaphoneVoiceHook.onServerStarted(serverApi);
        SvcHomePhoneVoiceHook.onServerStarted(serverApi);
    }

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        SvcWalkieVoiceHook.onServerStopped();
        SvcMegaphoneVoiceHook.onServerStopped();
        SvcHomePhoneVoiceHook.onServerStopped();

        serverApi = null;
        Minedevice.LOGGER.info("MineDevice Simple Voice Chat server hooks deactivated");
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        SvcWalkieVoiceHook.onMicrophonePacket(event);
        SvcMegaphoneVoiceHook.onMicrophonePacket(event);
        SvcHomePhoneVoiceHook.onMicrophonePacket(event);
    }

    public static VoicechatServerApi getServerApi() {
        return serverApi;
    }
}
