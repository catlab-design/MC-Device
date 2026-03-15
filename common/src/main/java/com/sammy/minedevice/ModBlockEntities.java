package com.sammy.minedevice;

import com.sammy.minedevice.block.entity.HomePhoneBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            MegaphoneMod.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<HomePhoneBlockEntity>> HOME_PHONE = BLOCK_ENTITY_TYPES.register(
            "home_phone", () -> BlockEntityType.Builder.of(HomePhoneBlockEntity::new, ModBlocks.HOME_PHONE.get()).build(null));

    private ModBlockEntities() {
    }

    public static void init() {
        BLOCK_ENTITY_TYPES.register();
    }
}
