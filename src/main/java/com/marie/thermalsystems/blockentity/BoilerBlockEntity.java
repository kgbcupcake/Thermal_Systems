package com.marie.thermalsystems.blockentity;

import com.marie.thermalsystems.data.config.ThermalConfig;
import com.marie.thermalsystems.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds the configured heat output of a placed Boiler. Fuel/combustion
 * simulation is out of scope for Phase 2 - heat output is set directly via
 * {@code /thermal boiler setOutput} or defaults to {@code defaultBoilerHeatOutput}.
 */
public class BoilerBlockEntity extends BlockEntity {

    private double heatOutput;

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOILER.get(), pos, state);
        this.heatOutput = ThermalConfig.DEFAULT_BOILER_HEAT_OUTPUT.get();
    }

    public double getHeatOutput() {
        return heatOutput;
    }

    /**
     * @throws IllegalArgumentException if heatOutput is NaN, infinite, or negative
     */
    public void setHeatOutput(double heatOutput) {
        if (Double.isNaN(heatOutput) || Double.isInfinite(heatOutput) || heatOutput < 0) {
            throw new IllegalArgumentException("heatOutput must be a finite, non-negative number, was: " + heatOutput);
        }
        this.heatOutput = heatOutput;
    }
}
