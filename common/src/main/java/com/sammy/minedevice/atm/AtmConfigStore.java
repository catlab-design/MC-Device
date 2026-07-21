package com.sammy.minedevice.atm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class AtmConfigStore extends SavedData {
    private static final String DATA_NAME = "minedevice_atm_config";
    private static final String CARD_REQUIRED_TAG = "card_required";

    private boolean cardRequired = true;

    public static AtmConfigStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                AtmConfigStore::load,
                AtmConfigStore::new,
                DATA_NAME
        );
    }

    public static AtmConfigStore load(CompoundTag tag) {
        AtmConfigStore store = new AtmConfigStore();
        if (tag != null && tag.contains(CARD_REQUIRED_TAG)) {
            store.cardRequired = tag.getBoolean(CARD_REQUIRED_TAG);
        }
        return store;
    }

    public boolean isCardRequired() {
        return cardRequired;
    }

    public void setCardRequired(boolean required) {
        this.cardRequired = required;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean(CARD_REQUIRED_TAG, cardRequired);
        return tag;
    }
}
