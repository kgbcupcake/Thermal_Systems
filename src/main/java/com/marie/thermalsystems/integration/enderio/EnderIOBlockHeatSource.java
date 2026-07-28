package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.api.cooling.ICoolingSource;
import com.marie.thermalsystems.api.heating.IHeatSource;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Adapts a real Ender IO Stirling Generator's own, standard NeoForge
 * {@link IEnergyStorage} capability so it can be bound to a zone the same
 * way a RadiatorBlockEntity is. Holds a position, not a live capability/
 * BlockEntity reference; resolves {@link Capabilities.EnergyStorage#BLOCK}
 * fresh via {@link IEnergyStorage#getEnergyStored()} on every evaluation and
 * treats a missing storage as zero output rather than throwing, since the
 * block may have been broken or the chunk unloaded since binding. Never owns
 * or simulates energy state of its own - Ender IO's own machine is the only
 * generation involved.
 *
 * <p>Unlike Mekanism's temperature-delta split, the generator's converted
 * output has no natural heat/cool sign of its own - it only ever produces
 * energy - so the split here is mode-gated instead, via
 * {@link EnderIOAdapterModeRegistry}: the converted value flows to
 * {@link #getHeatOutput()} in {@link ThermalMode#HEAT} and to
 * {@link #getCoolingOutput()} in {@link ThermalMode#COOL}, never both at
 * once.
 */
final class EnderIOBlockHeatSource implements IHeatSource, ICoolingSource {

    private final Level level;
    private final BlockPos pos;

    EnderIOBlockHeatSource(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Override
    public double getHeatOutput() {
        return EnderIOAdapterModeRegistry.get(level, pos) == ThermalMode.HEAT ? convert() : 0.0;
    }

    @Override
    public double getCoolingOutput() {
        return EnderIOAdapterModeRegistry.get(level, pos) == ThermalMode.COOL ? convert() : 0.0;
    }

    private double convert() {
        IEnergyStorage storage = Capabilities.EnergyStorage.BLOCK.getCapability(level, pos, null, null, null);
        if (storage == null) {
            return 0.0;
        }
        return convert(storage);
    }

    static double convert(IEnergyStorage storage) {
        double coefficient = ThermalConfig.ENDERIO_ENERGY_TO_HEAT_COEFFICIENT.get() * ThermalConfig.ENDERIO_OUTPUT_MULTIPLIER.get();
        return EnderIOConversion.energyToHeat(storage.getEnergyStored(), coefficient);
    }
}
