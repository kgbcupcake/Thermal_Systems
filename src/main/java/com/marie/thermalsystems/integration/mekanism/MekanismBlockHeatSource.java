package com.marie.thermalsystems.integration.mekanism;

import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import mekanism.api.heat.IHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Adapts a real Mekanism block entity's own {@link IHeatHandler} capability
 * so it can be bound to a zone the same way a RadiatorBlockEntity is. Holds
 * a position, not a live capability/BlockEntity reference; resolves
 * {@link MekanismIntegration#HEAT_HANDLER} fresh via
 * {@link IHeatHandler#getTotalTemperature()} on every evaluation and treats
 * a missing handler as zero output rather than throwing, since the block may
 * have been broken or the chunk unloaded since binding. Never owns or
 * simulates heat state of its own - Mekanism's own machine is the only heat
 * simulation involved.
 */
final class MekanismBlockHeatSource implements IHeatSource, ICoolingSource {

    private final Level level;
    private final BlockPos pos;

    MekanismBlockHeatSource(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Override
    public double getHeatOutput() {
        return convert().heatOutput();
    }

    @Override
    public double getCoolingOutput() {
        return convert().coolingOutput();
    }

    private MekanismHeatConversion.Output convert() {
        IHeatHandler handler = MekanismIntegration.HEAT_HANDLER.getCapability(level, pos, null, null, null);
        if (handler == null) {
            return new MekanismHeatConversion.Output(0.0, 0.0);
        }
        return convert(handler);
    }

    static MekanismHeatConversion.Output convert(IHeatHandler handler) {
        double referenceTemperature = ThermalConfig.MEKANISM_REFERENCE_TEMPERATURE_KELVIN.get();
        double conversionCoefficient = ThermalConfig.MEKANISM_CONVERSION_COEFFICIENT.get();
        return MekanismHeatConversion.convert(handler.getTotalTemperature(), referenceTemperature, conversionCoefficient);
    }
}
