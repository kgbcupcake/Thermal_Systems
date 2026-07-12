package com.marie.thermalsystems.block;

import com.marie.thermalsystems.blockentity.SteamPipeBlockEntity;
import com.marie.thermalsystems.steam.SteamNetworkManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Connectivity node for steam networks. Carries no behavior of its own
 * beyond registering/unregistering itself with {@link SteamNetworkManager}
 * on placement and removal.
 */
public class SteamPipeBlock extends BaseEntityBlock {

    public static final MapCodec<SteamPipeBlock> CODEC = simpleCodec(SteamPipeBlock::new);

    public SteamPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamPipeBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            SteamNetworkManager.get().registerPipe(level.dimension(), pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            SteamNetworkManager.get().unregisterPipe(level.dimension(), pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
