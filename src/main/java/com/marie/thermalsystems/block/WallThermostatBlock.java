package com.marie.thermalsystems.block;

import com.marie.thermalsystems.controller.ClimateController;
import com.marie.thermalsystems.controller.ThermalScreens;
import com.marie.thermalsystems.registry.ThermalRegistries;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * A thermostat tablet mounted flat on a wall. It controls whichever zone contains its own position,
 * or the closest one a few blocks away, looked up live by {@link WallThermostatBlockEntity}, so it
 * needs no binding and follows the room if the zone is resized. Tapping the screen opens the thermostat app for that room; tapping the
 * bottom strip's minus/mode/plus buttons changes the room directly (see {@link WallThermostatTouch}).
 * Emits a redstone signal - strongly into the wall it hangs on, like a lever - while the room is
 * calling for heat or cooling, so it can switch machines Thermal Systems doesn't integrate with.
 */
public class WallThermostatBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<WallThermostatBlock> CODEC = simpleCodec(WallThermostatBlock::new);

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Map.of(
            Direction.NORTH, box(2, 1, 14, 14, 15, 16),
            Direction.SOUTH, box(2, 1, 0, 14, 15, 2),
            Direction.WEST, box(14, 1, 2, 16, 15, 14),
            Direction.EAST, box(0, 1, 2, 2, 15, 14)));

    public WallThermostatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            return null;
        }
        BlockState state = defaultBlockState().setValue(FACING, face);
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WallThermostatBlockEntity thermostat)) {
            return InteractionResult.PASS;
        }
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        WallThermostatTouch.Region region = WallThermostatTouch.regionOf(
                state.getValue(FACING), local.x, local.y, local.z);
        UUID zoneId = thermostat.zoneId();

        if (region == WallThermostatTouch.Region.SCREEN) {
            if (level.isClientSide) {
                ThermalScreens.openThermostat(zoneId, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (zoneId == null) {
                serverPlayer.displayClientMessage(Component.translatable("message.thermalsystems.thermostat.no_zone"), true);
            } else {
                ClimateController.Result result = switch (region) {
                    case MINUS -> ClimateController.stepTarget(serverPlayer, zoneId, -1);
                    case PLUS -> ClimateController.stepTarget(serverPlayer, zoneId, 1);
                    default -> ClimateController.cycleMode(serverPlayer, zoneId);
                };
                serverPlayer.displayClientMessage(result.message(), true);
                if (result.success()) {
                    thermostat.refresh();
                    level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 1.6F);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            updatePowerNeighbors(state, level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    static void updatePowerNeighbors(BlockState state, Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), state.getBlock());
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof WallThermostatBlockEntity thermostat && thermostat.isCalling() ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == state.getValue(FACING) ? getSignal(state, level, pos, direction) : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WallThermostatBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ThermalRegistries.WALL_THERMOSTAT_ENTITY.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<WallThermostatBlockEntity>)
                (tickLevel, tickPos, tickState, thermostat) -> thermostat.serverTick();
    }
}
