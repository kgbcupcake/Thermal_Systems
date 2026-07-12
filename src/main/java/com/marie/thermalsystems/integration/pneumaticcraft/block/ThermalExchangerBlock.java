package com.marie.thermalsystems.integration.pneumaticcraft.block;

import com.marie.thermalsystems.integration.pneumaticcraft.PneumaticCraftIntegration;
import com.marie.thermalsystems.integration.pneumaticcraft.blockentity.ThermalExchangerBlockEntity;
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
 * Translation boundary between a PneumaticCraft: Repressurized heat network
 * and a Thermal Systems climate zone. Not an independent heat source and not
 * a machine - see {@link ThermalExchangerBlockEntity} for the conversion
 * logic. Requires a ticker only to drive PNC:R's own heat exchanger logic
 * ({@code IHeatExchangerLogic.tick()}); it holds no machine state of its own.
 */
public class ThermalExchangerBlock extends BaseEntityBlock {

    public static final MapCodec<ThermalExchangerBlock> CODEC = simpleCodec(ThermalExchangerBlock::new);

    public ThermalExchangerBlock(Properties properties) {
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
        return new ThermalExchangerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, PneumaticCraftIntegration.THERMAL_EXCHANGER_BLOCK_ENTITY.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }
}
