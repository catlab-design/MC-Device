package com.sammy.minedevice.client.phone;

import com.sammy.minedevice.phone.PhoneCallState;
import com.sammy.minedevice.phone.PhoneData;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PhoneClientCallState {
    private static CallSnapshot mobileState = CallSnapshot.idle();
    private static final Map<BlockPos, CallSnapshot> homePhoneStates = new HashMap<>();

    private PhoneClientCallState() {
    }

    public static void apply(PhoneCallState nextState, String nextNumber, String nextName) {
        mobileState = mobileState.next(nextState, nextNumber, nextName);
    }

    public static void applyHomePhone(BlockPos blockPos, PhoneCallState nextState, String nextNumber, String nextName) {
        if (blockPos == null) {
            apply(nextState, nextNumber, nextName);
            return;
        }

        BlockPos key = blockPos.immutable();
        CallSnapshot previous = homePhoneStates.getOrDefault(key, CallSnapshot.idle());
        homePhoneStates.put(key, previous.next(nextState, nextNumber, nextName));
    }

    public static void clear() {
        mobileState = mobileState.next(PhoneCallState.IDLE, "", "");
        homePhoneStates.clear();
    }

    public static void clearHomePhone(BlockPos blockPos) {
        if (blockPos != null) {
            homePhoneStates.remove(blockPos);
        }
    }

    public static PhoneCallState getState() {
        return mobileState.state();
    }

    public static PhoneCallState getState(BlockPos blockPos) {
        return snapshot(blockPos).state();
    }

    public static String getOtherNumber() {
        return mobileState.otherNumber();
    }

    public static String getOtherNumber(BlockPos blockPos) {
        return snapshot(blockPos).otherNumber();
    }

    public static String getOtherName() {
        return mobileState.otherName();
    }

    public static String getOtherName(BlockPos blockPos) {
        return snapshot(blockPos).otherName();
    }

    public static long getRevision() {
        return mobileState.revision();
    }

    public static long getRevision(BlockPos blockPos) {
        return snapshot(blockPos).revision();
    }

    public static int getConnectedDurationTicks() {
        return connectedDurationTicks(mobileState);
    }

    public static int getConnectedDurationTicks(BlockPos blockPos) {
        return connectedDurationTicks(snapshot(blockPos));
    }

    public static boolean hasHomePhoneState(PhoneCallState state) {
        return !getHomePhonesInState(state).isEmpty();
    }

    public static Set<BlockPos> getHomePhonesInState(PhoneCallState state) {
        if (state == null || homePhoneStates.isEmpty()) {
            return Set.of();
        }

        Set<BlockPos> matching = new HashSet<>();
        for (Map.Entry<BlockPos, CallSnapshot> entry : homePhoneStates.entrySet()) {
            if (entry.getValue().state() == state) {
                matching.add(entry.getKey().immutable());
            }
        }

        return matching.isEmpty() ? Set.of() : Set.copyOf(matching);
    }

    private static CallSnapshot snapshot(BlockPos blockPos) {
        if (blockPos == null) {
            return mobileState;
        }
        return homePhoneStates.getOrDefault(blockPos, CallSnapshot.idle());
    }

    private static int connectedDurationTicks(CallSnapshot snapshot) {
        if (snapshot.state() != PhoneCallState.CONNECTED || snapshot.connectedSinceMillis() < 0L) {
            return 0;
        }

        long elapsedMillis = Math.max(0L, System.currentTimeMillis() - snapshot.connectedSinceMillis());
        return (int) Math.min(Integer.MAX_VALUE, (elapsedMillis * 20L) / 1000L);
    }

    private record CallSnapshot(PhoneCallState state, String otherNumber, String otherName,
                                long revision, long connectedSinceMillis) {
        private static CallSnapshot idle() {
            return new CallSnapshot(PhoneCallState.IDLE, "", "", 0L, -1L);
        }

        private CallSnapshot next(PhoneCallState nextState, String nextNumber, String nextName) {
            PhoneCallState normalizedState = nextState == null ? PhoneCallState.IDLE : nextState;
            String normalizedNumber = PhoneData.normalizePhoneNumber(nextNumber);
            String normalizedName = nextName == null ? "" : nextName;

            long nextConnectedSinceMillis = connectedSinceMillis;
            if (normalizedState != PhoneCallState.CONNECTED) {
                nextConnectedSinceMillis = -1L;
            } else {
                boolean enteringConnected = state != PhoneCallState.CONNECTED;
                boolean peerChanged = !otherNumber.equals(normalizedNumber) || !otherName.equals(normalizedName);
                if (enteringConnected || peerChanged || connectedSinceMillis < 0L) {
                    nextConnectedSinceMillis = System.currentTimeMillis();
                }
            }

            return new CallSnapshot(
                    normalizedState,
                    normalizedNumber,
                    normalizedName,
                    revision + 1L,
                    nextConnectedSinceMillis);
        }
    }
}
