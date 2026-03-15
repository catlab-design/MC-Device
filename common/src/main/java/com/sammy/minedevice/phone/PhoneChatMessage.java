package com.sammy.minedevice.phone;

public record PhoneChatMessage(String text, boolean incoming) {
    public PhoneChatMessage {
        text = text == null ? "" : text;
    }
}
