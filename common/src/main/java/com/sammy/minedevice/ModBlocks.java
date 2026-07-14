package com.sammy.minedevice;

import com.sammy.minedevice.block.AtmBlock;
import com.sammy.minedevice.block.HomePhoneBlock;
import com.sammy.minedevice.block.LabtopBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Minedevice.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<Block> HOME_PHONE = BLOCKS.register("home_phone",
            () -> new HomePhoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistrySupplier<Block> ATM = BLOCKS.register("atm",
            () -> new AtmBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistrySupplier<Block> LABTOP = BLOCKS.register("labtop",
            () -> new LabtopBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    private ModBlocks() {
    }

    public static void init() {
        BLOCKS.register();
    }
}
