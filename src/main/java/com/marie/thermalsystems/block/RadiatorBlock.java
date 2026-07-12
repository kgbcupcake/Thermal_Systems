package com.marie.thermalsystems.block;

import com.marie.thermalsystems.blockentity.RadiatorBlockEntity;
import com.marie.thermalsystems.steam.SteamNetworkManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Receives a share of its steam network's heat and, when bound to a
 * ClimateZone, contributes it as an {@link com.marie.thermalsystems.api.heating.IHeatSource}.
 * Unbinding on removal is handled by {@link RadiatorBlockEntity#setRemoved()}.
 */
public class RadiatorBlock extends BaseEntityBlock {

    public static final MapCodec<RadiatorBlock> CODEC = simpleCodec(RadiatorBlock::new);

    public RadiatorBlock(Properties properties) {
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
        return new RadiatorBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            SteamNetworkManager.get().markDirty(level.dimension(), pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            SteamNetworkManager.get().markDirty(level.dimension(), pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
