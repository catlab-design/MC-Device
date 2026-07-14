package com.sammy.minedevice.block;

import com.sammy.minedevice.Minedevice;
import com.sammy.minedevice.ModBlockEntities;
import com.sammy.minedevice.block.entity.LabtopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class LabtopBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty MONITOR_OPEN = BooleanProperty.create("monitor_open");
    public static final BooleanProperty IS_SCREEN = BooleanProperty.create("is_screen");

    private static final double[] CLOSED_BOX = new double[]{2, 0, 3, 14, 1.35, 12.2};
    private static final double[] BASE_BOX = new double[]{2, 0, 3, 14, 1.15, 12.2};
    
    private static final double[] SCREEN_STEP_1 = new double[]{2, 0.2, 12.0, 14, 2.3, 12.8};
    private static final double[] SCREEN_STEP_2 = new double[]{2, 2.3, 12.8, 14, 4.4, 13.5};
    private static final double[] SCREEN_STEP_3 = new double[]{2, 4.4, 13.5, 14, 6.5, 14.3};
    private static final double[] SCREEN_STEP_4 = new double[]{2, 6.5, 14.3, 14, 8.65, 15.0};

    private static final VoxelShape NORTH_CLOSED = rotateBox(Direction.NORTH, CLOSED_BOX);
    private static final VoxelShape EAST_CLOSED = rotateBox(Direction.EAST, CLOSED_BOX);
    private static final VoxelShape SOUTH_CLOSED = rotateBox(Direction.SOUTH, CLOSED_BOX);
    private static final VoxelShape WEST_CLOSED = rotateBox(Direction.WEST, CLOSED_BOX);

    private static final VoxelShape NORTH_OPEN = Shapes.or(
            rotateBox(Direction.NORTH, BASE_BOX),
            rotateBox(Direction.NORTH, SCREEN_STEP_1),
            rotateBox(Direction.NORTH, SCREEN_STEP_2),
            rotateBox(Direction.NORTH, SCREEN_STEP_3),
            rotateBox(Direction.NORTH, SCREEN_STEP_4)
    );
    private static final VoxelShape EAST_OPEN = Shapes.or(
            rotateBox(Direction.EAST, BASE_BOX),
            rotateBox(Direction.EAST, SCREEN_STEP_1),
            rotateBox(Direction.EAST, SCREEN_STEP_2),
            rotateBox(Direction.EAST, SCREEN_STEP_3),
            rotateBox(Direction.EAST, SCREEN_STEP_4)
    );
    private static final VoxelShape SOUTH_OPEN = Shapes.or(
            rotateBox(Direction.SOUTH, BASE_BOX),
            rotateBox(Direction.SOUTH, SCREEN_STEP_1),
            rotateBox(Direction.SOUTH, SCREEN_STEP_2),
            rotateBox(Direction.SOUTH, SCREEN_STEP_3),
            rotateBox(Direction.SOUTH, SCREEN_STEP_4)
    );
    private static final VoxelShape WEST_OPEN = Shapes.or(
            rotateBox(Direction.WEST, BASE_BOX),
            rotateBox(Direction.WEST, SCREEN_STEP_1),
            rotateBox(Direction.WEST, SCREEN_STEP_2),
            rotateBox(Direction.WEST, SCREEN_STEP_3),
            rotateBox(Direction.WEST, SCREEN_STEP_4)
    );

    public LabtopBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(MONITOR_OPEN, false)
                .setValue(IS_SCREEN, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        boolean open = state.getValue(MONITOR_OPEN);
        if (open) {
            return switch (facing) {
                case EAST -> EAST_OPEN;
                case SOUTH -> SOUTH_OPEN;
                case WEST -> WEST_OPEN;
                default -> NORTH_OPEN;
            };
        } else {
            return switch (facing) {
                case EAST -> EAST_CLOSED;
                case SOUTH -> SOUTH_CLOSED;
                case WEST -> WEST_CLOSED;
                default -> NORTH_CLOSED;
            };
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(MONITOR_OPEN, false)
                .setValue(IS_SCREEN, false);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            boolean open = state.getValue(MONITOR_OPEN);
            level.setBlock(pos, state.setValue(MONITOR_OPEN, !open), 3);
            level.playSound(player, pos, open ? SoundEvents.WOODEN_TRAPDOOR_CLOSE : SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.2F);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level.isClientSide) {
            openLabtopScreen(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void openLabtopScreen(BlockPos pos) {
        try {
            Class<?> hookClass = Class.forName("com.sammy.minedevice.client.phone.PhoneClientHooks");
            java.lang.reflect.Method method = hookClass.getDeclaredMethod("openLabtopScreen", BlockPos.class);
            method.invoke(null, pos);
        } catch (ReflectiveOperationException e) {
            Minedevice.LOGGER.debug("Failed to open labtop screen", e);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LabtopBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? (lvl, pos, st, be) -> {
            if (be instanceof LabtopBlockEntity labtop) {
                labtop.clientTick();
            }
        } : null;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative()) {
            popResource(level, pos, new ItemStack(this));
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MONITOR_OPEN, IS_SCREEN);
    }

    private static VoxelShape rotateBox(Direction direction, double[] box) {
        double minX = box[0];
        double minY = box[1];
        double minZ = box[2];
        double maxX = box[3];
        double maxY = box[4];
        double maxZ = box[5];
        return switch (direction) {
            case EAST -> Block.box(16.0D - maxZ, minY, minX, 16.0D - minZ, maxY, maxX);
            case SOUTH -> Block.box(16.0D - maxX, minY, 16.0D - maxZ, 16.0D - minX, maxY, 16.0D - minZ);
            case WEST -> Block.box(minZ, minY, 16.0D - maxX, maxZ, maxY, 16.0D - minX);
            default -> Block.box(minX, minY, minZ, maxX, maxY, maxZ);
        };
    }
}
