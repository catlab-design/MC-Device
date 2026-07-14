package com.sammy.minedevice.phone;

public record CallLogEntry(long id, String otherNumber, String otherName, String callType, long timestamp, int durationTicks) {
}
