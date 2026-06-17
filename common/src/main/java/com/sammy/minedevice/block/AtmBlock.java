package com.sammy.minedevice.block;

import com.sammy.minedevice.ModItems;
import com.sammy.minedevice.atm.AtmNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

public final class AtmBlock extends HorizontalDirectionalBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 1);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_TOP = Block.box(0, 0, 0, 16, 16, 16);

    public AtmBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == 0 ? SHAPE : SHAPE_TOP;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == 0 ? SHAPE : SHAPE_TOP;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1
                || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }

        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(PART, 0);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.CARD.get())) {
            if (level.isClientSide()) {
                player.displayClientMessage(Component.translatable("screen.minedevice.atm.insert_card"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockPos basePos = state.getValue(PART) == 0 ? pos : pos.below();
            AtmNetworking.openScreen(serverPlayer, basePos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (state.getValue(PART) == 0 && !level.isClientSide()) {
            BlockPos posAbove = pos.above();
            if (level.getBlockState(posAbove).canBeReplaced()) {
                level.setBlock(posAbove, state.setValue(PART, 1), 3);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (state.getValue(PART) == 0) {
                BlockPos posAbove = pos.above();
                BlockState stateAbove = level.getBlockState(posAbove);
                if (isMatchingAtmPart(state, stateAbove, 1)) {
                    level.setBlock(posAbove, Blocks.AIR.defaultBlockState(), 3);
                }
            } else {
                BlockPos posBelow = pos.below();
                BlockState stateBelow = level.getBlockState(posBelow);
                if (isMatchingAtmPart(state, stateBelow, 0)) {
                    level.setBlock(posBelow, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        int part = state.getValue(PART);
        Direction requiredDirection = part == 0 ? Direction.UP : Direction.DOWN;
        if (direction == requiredDirection && !isMatchingAtmPart(state, neighborState, 1 - part)) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private static boolean isMatchingAtmPart(BlockState state, BlockState otherState, int expectedPart) {
        return otherState.is(state.getBlock())
                && otherState.getValue(PART) == expectedPart
                && otherState.getValue(FACING) == state.getValue(FACING);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}
