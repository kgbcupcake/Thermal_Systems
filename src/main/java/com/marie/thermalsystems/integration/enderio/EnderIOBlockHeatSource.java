package com.marie.thermalsystems.integration.enderio;

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
 */
final class EnderIOBlockHeatSource implements IHeatSource {

    private final Level level;
    private final BlockPos pos;

    EnderIOBlockHeatSource(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Override
    public double getHeatOutput() {
        IEnergyStorage storage = Capabilities.EnergyStorage.BLOCK.getCapability(level, pos, null, null, null);
        if (storage == null) {
            return 0.0;
        }
        return convert(storage);
    }

    static double convert(IEnergyStorage storage) {
        double coefficient = ThermalConfig.ENDERIO_ENERGY_TO_HEAT_COEFFICIENT.get();
        return EnderIOConversion.energyToHeat(storage.getEnergyStored(), coefficient);
    }
}
