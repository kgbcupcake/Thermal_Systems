package com.marie.thermalsystems.integration.pneumaticcraft;

import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Adapts a real PNC:R block entity's own {@link IHeatExchangerLogic}
 * capability so it can be bound to a zone the same way a
 * RadiatorBlockEntity is. Holds a position, not a live capability/BlockEntity
 * reference; resolves {@link PNCCapabilities#HEAT_EXCHANGER_BLOCK} fresh via
 * {@link IHeatExchangerLogic#getTemperature()} on every evaluation and
 * treats a missing logic as zero output rather than throwing, since the
 * block may have been broken or the chunk unloaded since binding. Never owns
 * or simulates heat state of its own - PNC:R's own machine is the only heat
 * simulation involved.
 */
final class PneumaticCraftBlockHeatSource implements IHeatSource, ICoolingSource {

    private final Level level;
    private final BlockPos pos;

    PneumaticCraftBlockHeatSource(Level level, BlockPos pos) {
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

    private ThermalExchangerConversion.Output convert() {
        IHeatExchangerLogic logic = PNCCapabilities.HEAT_EXCHANGER_BLOCK.getCapability(level, pos, null, null, null);
        if (logic == null) {
            return new ThermalExchangerConversion.Output(0.0, 0.0);
        }
        return convert(logic);
    }

    static ThermalExchangerConversion.Output convert(IHeatExchangerLogic logic) {
        double referenceTemperature = ThermalConfig.PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN.get();
        double conversionCoefficient = ThermalConfig.PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT.get();
        return ThermalExchangerConversion.convert(logic.getTemperature(), referenceTemperature, conversionCoefficient);
    }
}
