package com.sammy.minedevice.block.entity;

import com.sammy.minedevice.ModBlockEntities;
import com.sammy.minedevice.block.LabtopBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class LabtopBlockEntity extends BlockEntity {
    public float openProgress;
    public float prevOpenProgress;

    public LabtopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LABTOP.get(), pos, state);
        boolean isOpen = state.getValue(LabtopBlock.MONITOR_OPEN);
        this.openProgress = isOpen ? 1.0F : 0.0F;
        this.prevOpenProgress = this.openProgress;
    }

    public void clientTick() {
        this.prevOpenProgress = this.openProgress;
        BlockState state = this.getBlockState();
        if (state.is(com.sammy.minedevice.ModBlocks.LABTOP.get())) {
            boolean isOpen = state.getValue(LabtopBlock.MONITOR_OPEN);
            if (isOpen) {
                if (this.openProgress < 1.0F) {
                    this.openProgress = Math.min(1.0F, this.openProgress + 0.12F);
                }
            } else {
                if (this.openProgress > 0.0F) {
                    this.openProgress = Math.max(0.0F, this.openProgress - 0.12F);
                }
            }
        }
    }
}
