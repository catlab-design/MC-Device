package com.sammy.minedevice.block;

import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.atm.AtmNetworking;
import com.sammy.minedevice.atm.CardAccountStore;
import com.sammy.minedevice.item.CardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.UUID;

public final class BankBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public BankBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 net.minecraft.world.phys.BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.CARD.get())) {
                player.displayClientMessage(
                        Component.translatable("screen.minedevice.bank.need_card"), true);
                return InteractionResult.sidedSuccess(false);
            }
            UUID cardId = CardItem.getCardUUID(stack);
            if (cardId == null) {
                player.displayClientMessage(
                        Component.translatable("screen.minedevice.bank.not_locked"), true);
                return InteractionResult.sidedSuccess(false);
            }
            CardAccountStore store = CardAccountStore.get(serverPlayer.getServer());
            if (!store.isLocked(cardId)) {
                player.displayClientMessage(
                        Component.translatable("screen.minedevice.bank.not_locked"), true);
                return InteractionResult.sidedSuccess(false);
            }
            AtmNetworking.openBankScreen(serverPlayer);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
