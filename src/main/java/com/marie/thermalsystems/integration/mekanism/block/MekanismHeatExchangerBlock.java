package com.marie.thermalsystems.integration.mekanism.block;

import com.marie.thermalsystems.integration.mekanism.MekanismIntegration;
import com.marie.thermalsystems.integration.mekanism.blockentity.MekanismHeatExchangerBlockEntity;
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
 * Translation boundary between a Mekanism heat network and a Thermal Systems
 * climate zone. Not a machine - see {@link MekanismHeatExchangerBlockEntity}
 * for the conversion logic. Requires a ticker only to roll the adapter's
 * capacitor temperature into heat/cooling output; it holds no machine state,
 * inventory, fuel, recipes, upgrades, or GUI of its own.
 */
public class MekanismHeatExchangerBlock extends BaseEntityBlock {

    public static final MapCodec<MekanismHeatExchangerBlock> CODEC = simpleCodec(MekanismHeatExchangerBlock::new);

    public MekanismHeatExchangerBlock(Properties properties) {
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
        return new MekanismHeatExchangerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, MekanismIntegration.HEAT_EXCHANGER_BLOCK_ENTITY.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }
}
