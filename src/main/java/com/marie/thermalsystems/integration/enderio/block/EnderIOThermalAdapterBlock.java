package com.marie.thermalsystems.integration.enderio.block;

import com.marie.thermalsystems.integration.enderio.EnderIOIntegration;
import com.marie.thermalsystems.integration.enderio.blockentity.EnderIOThermalAdapterBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Translation boundary between an Ender IO Energy Conduit network and a
 * Thermal Systems climate zone. Not a machine - see
 * {@link EnderIOThermalAdapterBlockEntity} for the conversion logic. Requires
 * a ticker only to roll the per-tick received-energy counter into a heat
 * output value; it holds no machine state, fuel, recipes, or GUI of its own.
 */
public class EnderIOThermalAdapterBlock extends BaseEntityBlock {

    public static final MapCodec<EnderIOThermalAdapterBlock> CODEC = simpleCodec(EnderIOThermalAdapterBlock::new);

    public EnderIOThermalAdapterBlock(Properties properties) {
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
        return new EnderIOThermalAdapterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, EnderIOIntegration.THERMAL_ADAPTER_BLOCK_ENTITY.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }
}
